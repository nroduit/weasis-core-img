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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for zipping and unzipping files and directories. Provides methods to create zip
 * files from directories and extract zip files into directories.
 */
public final class ZipUtil {

  private static final int BUFFER_SIZE = 8192;

  private ZipUtil() {
    // Utility class
  }

  /**
   * Create a zip file from a directory.
   *
   * @param sourceDir the directory to zip
   * @param zipFile the zip file to create
   * @throws IOException if an I/O error occurs
   * @throws IllegalArgumentException if sourceDir or zipFile is null
   */
  public static void zip(Path sourceDir, Path zipFile) throws IOException {
    if (sourceDir == null) {
      throw new IllegalArgumentException("Source directory cannot be null");
    }
    if (zipFile == null) {
      throw new IllegalArgumentException("Zip file cannot be null");
    }
    if (!Files.exists(sourceDir)) {
      throw new IOException("Directory does not exist: " + sourceDir);
    }
    if (!Files.isDirectory(sourceDir)) {
      throw new IOException("Source is not a directory: " + sourceDir);
    }

    // Ensure parent directory exists
    Path parent = zipFile.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipFile))) {
      zipDirectory(sourceDir, sourceDir, zipOut);
    }
  }

  /** Recursively zip a directory and its contents. */
  private static void zipDirectory(Path sourceDir, Path basePath, ZipOutputStream zipOut)
      throws IOException {
    try (var stream = Files.walk(sourceDir)) {
      stream.forEach(
          path -> {
            try {
              String relativePath = basePath.relativize(path).toString().replace('\\', '/');

              if (Files.isDirectory(path)) {
                // Add a directory entry only if it's empty to preserve empty directories
                if (isEmpty(path) && !relativePath.isEmpty()) {
                  zipOut.putNextEntry(new ZipEntry(relativePath + "/"));
                  zipOut.closeEntry();
                }
              } else {
                // Add file entry
                zipOut.putNextEntry(new ZipEntry(relativePath));
                Files.copy(path, zipOut);
                zipOut.closeEntry();
              }
            } catch (IOException e) {
              throw new IllegalStateException("Error processing path: " + path, e);
            }
          });
    } catch (RuntimeException e) {
      if (e.getCause() instanceof IOException ioException) {
        throw ioException;
      }
      throw e;
    }
  }

  /** Check if a directory is empty. */
  private static boolean isEmpty(Path directory) throws IOException {
    if (!Files.isDirectory(directory)) {
      return false;
    }
    try (var stream = Files.list(directory)) {
      return stream.findFirst().isEmpty();
    }
  }

  /**
   * Unzip a zip input stream into a directory.
   *
   * @param inputStream the zip input stream
   * @param targetDir the directory to unzip all files
   * @throws IOException if an I/O error occurs
   * @throws IllegalArgumentException if inputStream or targetDir is null
   */
  public static void unzip(InputStream inputStream, Path targetDir) throws IOException {
    if (inputStream == null) {
      throw new IllegalArgumentException("Input stream cannot be null");
    }
    if (targetDir == null) {
      throw new IllegalArgumentException("Target directory cannot be null");
    }

    Files.createDirectories(targetDir);

    try (BufferedInputStream bufInStream = new BufferedInputStream(inputStream);
        ZipInputStream zis = new ZipInputStream(bufInStream)) {
      extractZipEntries(zis, targetDir);
    } finally {
      StreamUtil.safeClose(inputStream);
    }
  }

  /**
   * Unzip a zip file into a directory.
   *
   * @param zipFile the zip file to unzip
   * @param targetDir the directory to unzip all files
   * @throws IOException if an I/O error occurs
   * @throws IllegalArgumentException if zipFile or targetDir is null
   */
  public static void unzip(Path zipFile, Path targetDir) throws IOException {
    if (zipFile == null) {
      throw new IllegalArgumentException("Zip file cannot be null");
    }
    if (targetDir == null) {
      throw new IllegalArgumentException("Target directory cannot be null");
    }
    if (!Files.exists(zipFile)) {
      throw new IOException("Zip file does not exist: " + zipFile);
    }

    Files.createDirectories(targetDir);

    try (ZipFile zFile = new ZipFile(zipFile.toFile())) {
      Enumeration<? extends ZipEntry> entries = zFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        try (InputStream entryStream = zFile.getInputStream(entry)) {
          extractEntry(entryStream, entry, targetDir);
        }
      }
    }
  }

  /** Extract all entries from a ZipInputStream. */
  private static void extractZipEntries(ZipInputStream zis, Path targetPath) throws IOException {
    ZipEntry entry;
    while ((entry = zis.getNextEntry()) != null) {
      extractEntry(zis, entry, targetPath);
    }
  }

  /** Extract a single zip entry to the target directory with security checks. */
  private static void extractEntry(InputStream inputStream, ZipEntry entry, Path targetPath)
      throws IOException {
    Path entryPath = targetPath.resolve(entry.getName()).normalize();

    // Security check: prevent zip slip attacks
    if (!entryPath.startsWith(targetPath)) {
      throw new IOException("Entry is outside the target directory: " + entry.getName());
    }
    if (entry.isDirectory()) {
      Files.createDirectories(entryPath);
    } else {
      // Ensure parent directory exists
      Path parent = entryPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      // Copy file content
      try (OutputStream out =
          Files.newOutputStream(
              entryPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
        copyStream(inputStream, out);
      }
    }
  }

  /** Copy data from input stream to output stream efficiently. */
  private static void copyStream(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead;
    while ((bytesRead = in.read(buffer)) != -1) {
      out.write(buffer, 0, bytesRead);
    }
  }
}
