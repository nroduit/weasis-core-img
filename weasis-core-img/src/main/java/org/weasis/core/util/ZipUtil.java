/*
 * Copyright (c) 2025 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Zip and unzip directories. Extraction rejects entries escaping the target directory (zip slip)
 * and archives whose extracted bytes or entry count exceed safe limits (zip bomb).
 */
public final class ZipUtil {

  /** Maximum ratio between extracted bytes and compressed bytes read from the archive. */
  static final long MAX_COMPRESSION_RATIO = 100L;

  /** Extracted bytes always allowed, so small highly compressible archives are not rejected. */
  static final long RATIO_FREE_BYTES = 16L * 1024 * 1024;

  /** Maximum number of entries in an archive. */
  static final int MAX_ENTRIES = 100_000;

  private ZipUtil() {}

  /**
   * Creates a zip file from a directory. Empty directories are preserved and parent directories of
   * the zip file are created if needed.
   *
   * @param sourceDir the directory to zip
   * @param zipFile the zip file to create
   * @throws IOException if sourceDir is not a directory or an I/O error occurs
   */
  public static void zip(Path sourceDir, Path zipFile) throws IOException {
    Objects.requireNonNull(sourceDir, "Source directory cannot be null");
    Objects.requireNonNull(zipFile, "Zip file cannot be null");
    if (!Files.isDirectory(sourceDir)) {
      throw new IOException(
          Files.exists(sourceDir)
              ? "Source is not a directory: " + sourceDir
              : "Directory does not exist: " + sourceDir);
    }
    FileUtil.prepareToWriteFile(zipFile);

    try (var zipOut =
        new ZipOutputStream(
            new BufferedOutputStream(Files.newOutputStream(zipFile), FileUtil.FILE_BUFFER))) {
      Files.walkFileTree(sourceDir, new ZipVisitor(sourceDir, zipOut));
    }
  }

  /**
   * Extracts a zip file into a directory, which is created if needed.
   *
   * @param zipFile the zip file to extract
   * @param targetDir the directory to extract files into
   * @throws IOException if zipFile does not exist, contains an unsafe entry, or an I/O error occurs
   */
  public static void unzip(Path zipFile, Path targetDir) throws IOException {
    Objects.requireNonNull(zipFile, "Zip file cannot be null");
    Objects.requireNonNull(targetDir, "Target directory cannot be null");
    if (!Files.exists(zipFile)) {
      throw new IOException("Zip file does not exist: " + zipFile);
    }
    var target = prepareTargetDir(targetDir);
    long archiveSize = Files.size(zipFile);
    var guard = new ExtractionGuard(() -> archiveSize);

    try (var zFile = new ZipFile(zipFile.toFile())) {
      if (zFile.size() > MAX_ENTRIES) {
        throw new IOException("Archive has too many entries: " + zipFile);
      }
      var entries = zFile.entries();
      while (entries.hasMoreElements()) {
        var entry = entries.nextElement();
        try (var entryStream = zFile.getInputStream(entry)) {
          extractEntry(entryStream, entry, target, guard);
        }
      }
    }
  }

  /**
   * Extracts a zip stream into a directory, which is created if needed. The stream is closed after
   * extraction.
   *
   * @param inputStream the zip input stream to extract
   * @param targetDir the directory to extract files into
   * @throws IOException if the stream contains an unsafe entry or an I/O error occurs
   */
  public static void unzip(InputStream inputStream, Path targetDir) throws IOException {
    Objects.requireNonNull(inputStream, "Input stream cannot be null");
    Objects.requireNonNull(targetDir, "Target directory cannot be null");

    var counting = new CountingInputStream(inputStream);
    try (var zis = new ZipInputStream(new BufferedInputStream(counting, FileUtil.FILE_BUFFER))) {
      var target = prepareTargetDir(targetDir);
      var guard = new ExtractionGuard(counting::count);
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        extractEntry(zis, entry, target, guard);
      }
    }
  }

  private static Path prepareTargetDir(Path targetDir) throws IOException {
    var target = targetDir.toAbsolutePath().normalize();
    Files.createDirectories(target);
    return target;
  }

  private static void extractEntry(
      InputStream inputStream, ZipEntry entry, Path targetDir, ExtractionGuard guard)
      throws IOException {
    guard.onEntry(entry);
    var entryPath = targetDir.resolve(entry.getName()).normalize();
    if (!entryPath.startsWith(targetDir)) {
      throw new IOException("Entry is outside the target directory: " + entry.getName());
    }
    if (entry.isDirectory()) {
      Files.createDirectories(entryPath);
      return;
    }
    FileUtil.prepareToWriteFile(entryPath);
    try (var out = Files.newOutputStream(entryPath)) {
      guard.copy(inputStream, out, entry);
    } catch (IOException e) {
      Files.deleteIfExists(entryPath);
      throw e;
    }
  }

  // Bounds entry count and extracted bytes relative to compressed bytes actually read
  private static final class ExtractionGuard {
    private final LongSupplier compressedBytes;
    private final byte[] buffer = new byte[FileUtil.FILE_BUFFER];
    private int entryCount;
    private long extractedBytes;

    ExtractionGuard(LongSupplier compressedBytes) {
      this.compressedBytes = compressedBytes;
    }

    void onEntry(ZipEntry entry) throws IOException {
      if (++entryCount > MAX_ENTRIES) {
        throw new IOException("Archive has too many entries: " + entry.getName());
      }
    }

    void copy(InputStream in, OutputStream out, ZipEntry entry) throws IOException {
      int read;
      while ((read = in.read(buffer)) != -1) {
        extractedBytes += read;
        long allowed =
            Math.max(RATIO_FREE_BYTES, MAX_COMPRESSION_RATIO * compressedBytes.getAsLong());
        if (extractedBytes > allowed) {
          throw new IOException("Entry has suspicious compression ratio: " + entry.getName());
        }
        out.write(buffer, 0, read);
      }
    }
  }

  private static final class CountingInputStream extends FilterInputStream {
    private long count;

    CountingInputStream(InputStream in) {
      super(in);
    }

    long count() {
      return count;
    }

    @Override
    public int read() throws IOException {
      int b = super.read();
      if (b != -1) {
        count++;
      }
      return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int n = super.read(b, off, len);
      if (n > 0) {
        count += n;
      }
      return n;
    }

    @Override
    public long skip(long n) throws IOException {
      long skipped = super.skip(n);
      count += skipped;
      return skipped;
    }
  }

  // Single pass over the tree, using the attributes of the directory listing
  private static final class ZipVisitor extends SimpleFileVisitor<Path> {
    private final Path root;
    private final ZipOutputStream zipOut;

    ZipVisitor(Path root, ZipOutputStream zipOut) {
      this.root = root;
      this.zipOut = zipOut;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        throws IOException {
      if (!dir.equals(root)) {
        putEntry(dir, attrs, "/");
        zipOut.closeEntry();
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      if (attrs.isRegularFile() || (attrs.isSymbolicLink() && Files.isRegularFile(file))) {
        putEntry(file, attrs, "");
        Files.copy(file, zipOut);
        zipOut.closeEntry();
      }
      return FileVisitResult.CONTINUE;
    }

    private void putEntry(Path path, BasicFileAttributes attrs, String suffix) throws IOException {
      var name = root.relativize(path).toString().replace('\\', '/') + suffix;
      var entry = new ZipEntry(name);
      entry.setTime(attrs.lastModifiedTime().toMillis());
      zipOut.putNextEntry(entry);
    }
  }
}
