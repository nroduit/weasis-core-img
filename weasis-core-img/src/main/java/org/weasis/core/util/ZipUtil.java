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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for zipping and unzipping files and directories.
 *
 * <p>Provides methods to create zip files from directories and extract zip files into directories.
 * Uses modern Java NIO.2 Path API for better performance and cross-platform compatibility.
 * Implements security measures to prevent zip slip attacks.
 *
 * <p>All methods validate input parameters and throw appropriate exceptions for invalid arguments.
 * File operations are atomic where possible to ensure data integrity.
 *
 * @since 1.0
 */
public final class ZipUtil {

  private ZipUtil() {
    // Utility class - prevent instantiation
  }

  /**
   * Creates a zip file from a directory.
   *
   * <p>Recursively processes all files and subdirectories. Empty directories are preserved in the
   * zip archive. Parent directories for the zip file are created automatically if needed.
   *
   * @param sourceDir the directory to zip (must exist and be a directory)
   * @param zipFile the zip file to create (parent directories will be created if needed)
   * @throws IOException if an I/O error occurs during zip creation
   * @throws IllegalArgumentException if sourceDir or zipFile is null
   */
  public static void zip(Path sourceDir, Path zipFile) throws IOException {
    validateZipArguments(sourceDir, zipFile);
    validateSourceDirectory(sourceDir);

    createParentDirectories(zipFile);

    try (var zipOut = new ZipOutputStream(Files.newOutputStream(zipFile))) {
      addDirectoryToZip(sourceDir, sourceDir, zipOut);
    }
  }

  /**
   * Extracts a zip file into a directory.
   *
   * <p>Preserves directory structure and handles empty directories. Implements security checks to
   * prevent zip slip attacks.
   *
   * @param zipFile the zip file to extract (must exist)
   * @param targetDir the directory to extract files into (will be created if it doesn't exist)
   * @throws IOException if an I/O error occurs during extraction
   * @throws IllegalArgumentException if zipFile or targetDir is null
   */
  public static void unzip(Path zipFile, Path targetDir) throws IOException {
    validateUnzipArguments(zipFile, targetDir);
    validateZipFile(zipFile);

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

  private static void checkEntry(ZipEntry entry) throws IOException {
    // Verify compression ratio to prevent zip bomb attacks
    if (entry.getSize() > 0 && entry.getCompressedSize() > 0) {
      long ratio = entry.getSize() / entry.getCompressedSize();
      if (ratio > 5000) { // Suspicious compression ratio threshold
        throw new IOException("Entry has suspicious compression ratio: " + entry.getName());
      }
    }
  }

  /**
   * Extracts a zip input stream into a directory.
   *
   * <p>Preserves directory structure and handles empty directories. Implements security checks to
   * prevent zip slip attacks. The input stream is automatically closed after extraction.
   *
   * @param inputStream the zip input stream to extract
   * @param targetDir the directory to extract files into (will be created if it doesn't exist)
   * @throws IOException if an I/O error occurs during extraction
   * @throws IllegalArgumentException if inputStream or targetDir is null
   */
  public static void unzip(InputStream inputStream, Path targetDir) throws IOException {
    Objects.requireNonNull(inputStream, "Input stream cannot be null");
    Objects.requireNonNull(targetDir, "Target directory cannot be null");

    Files.createDirectories(targetDir);

    try (var bufInStream = new BufferedInputStream(inputStream);
        var zis = new ZipInputStream(bufInStream)) {

      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {

        // Verify compression ratio to prevent zip bomb attacks
        checkEntry(entry);
        extractEntry(zis, entry, targetDir);
      }

    } finally {
      StreamUtil.safeClose(inputStream);
    }
  }

  private static void addDirectoryToZip(Path sourceDir, Path basePath, ZipOutputStream zipOut)
      throws IOException {
    try (var stream = Files.walk(sourceDir)) {
      var paths = stream.sorted().toList(); // Sort for consistent ordering

      for (var path : paths) {
        if (path.equals(sourceDir)) {
          continue; // Skip the root directory itself
        }

        var relativePath = basePath.relativize(path).toString().replace('\\', '/');

        if (Files.isDirectory(path)) {
          addDirectoryEntry(path, relativePath, zipOut);
        } else {
          addFileEntry(path, relativePath, zipOut);
        }
      }
    }
  }

  private static void addDirectoryEntry(Path path, String relativePath, ZipOutputStream zipOut)
      throws IOException {
    if (isDirectoryEmpty(path)) {
      zipOut.putNextEntry(new ZipEntry(relativePath + "/"));
      zipOut.closeEntry();
    }
  }

  private static void addFileEntry(Path path, String relativePath, ZipOutputStream zipOut)
      throws IOException {
    zipOut.putNextEntry(new ZipEntry(relativePath));
    Files.copy(path, zipOut);
    zipOut.closeEntry();
  }

  private static boolean isDirectoryEmpty(Path directory) throws IOException {
    try (var stream = Files.list(directory)) {
      return stream.findFirst().isEmpty();
    }
  }

  private static void extractEntry(InputStream inputStream, ZipEntry entry, Path targetPath)
      throws IOException {
    var entryPath = resolveAndValidateEntryPath(entry, targetPath);

    if (entry.isDirectory()) {
      Files.createDirectories(entryPath);
    } else {
      createParentDirectories(entryPath);
      writeEntryToFile(inputStream, entryPath);
    }
  }

  private static Path resolveAndValidateEntryPath(ZipEntry entry, Path targetPath)
      throws IOException {
    var entryPath = targetPath.resolve(entry.getName()).normalize();

    // Security check: prevent zip slip attacks
    if (!entryPath.startsWith(targetPath)) {
      throw new IOException("Entry is outside the target directory: " + entry.getName());
    }
    return entryPath;
  }

  private static void writeEntryToFile(InputStream inputStream, Path filePath) throws IOException {
    try (var out =
        Files.newOutputStream(
            filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      inputStream.transferTo(out);
    }
  }

  private static void createParentDirectories(Path path) throws IOException {
    var parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
  }

  private static void validateZipArguments(Path sourceDir, Path zipFile) {
    Objects.requireNonNull(sourceDir, "Source directory cannot be null");
    Objects.requireNonNull(zipFile, "Zip file cannot be null");
  }

  private static void validateUnzipArguments(Path zipFile, Path targetDir) {
    Objects.requireNonNull(zipFile, "Zip file cannot be null");
    Objects.requireNonNull(targetDir, "Target directory cannot be null");
  }

  private static void validateSourceDirectory(Path sourceDir) throws IOException {
    if (!Files.exists(sourceDir)) {
      throw new IOException("Directory does not exist: " + sourceDir);
    }
    if (!Files.isDirectory(sourceDir)) {
      throw new IOException("Source is not a directory: " + sourceDir);
    }
  }

  private static void validateZipFile(Path zipFile) throws IOException {
    if (!Files.exists(zipFile)) {
      throw new IOException("Zip file does not exist: " + zipFile);
    }
  }
}
