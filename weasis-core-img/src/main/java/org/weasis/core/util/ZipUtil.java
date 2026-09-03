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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Zip and unzip directories. Extraction rejects entries escaping the target directory (zip slip)
 * and entries with a suspicious compression ratio (zip bomb).
 */
public final class ZipUtil {

  /** Maximum compression ratio of a single entry before it is rejected as a suspected zip bomb. */
  static final long MAX_COMPRESSION_RATIO = 5000L;

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
    Files.createDirectories(targetDir);

    try (var zFile = new ZipFile(zipFile.toFile())) {
      var entries = zFile.entries();
      while (entries.hasMoreElements()) {
        var entry = entries.nextElement();
        checkEntry(entry);
        try (var entryStream = zFile.getInputStream(entry)) {
          extractEntry(entryStream, entry, targetDir);
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

    try (var zis = new ZipInputStream(new BufferedInputStream(inputStream, FileUtil.FILE_BUFFER))) {
      Files.createDirectories(targetDir);
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        checkEntry(entry);
        extractEntry(zis, entry, targetDir);
      }
    }
  }

  // Package-private so the threshold can be tested without crafting a fraudulent central directory
  static void checkEntry(ZipEntry entry) throws IOException {
    if (entry.getSize() > 0 && entry.getCompressedSize() > 0) {
      long ratio = entry.getSize() / entry.getCompressedSize();
      if (ratio > MAX_COMPRESSION_RATIO) {
        throw new IOException("Entry has suspicious compression ratio: " + entry.getName());
      }
    }
  }

  private static void extractEntry(InputStream inputStream, ZipEntry entry, Path targetDir)
      throws IOException {
    var entryPath = targetDir.resolve(entry.getName()).normalize();
    if (!entryPath.startsWith(targetDir)) {
      throw new IOException("Entry is outside the target directory: " + entry.getName());
    }
    if (entry.isDirectory()) {
      Files.createDirectories(entryPath);
    } else {
      FileUtil.prepareToWriteFile(entryPath);
      Files.copy(inputStream, entryPath, StandardCopyOption.REPLACE_EXISTING);
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
