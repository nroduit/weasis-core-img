/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File and path utilities built on NIO.2. For stream operations, see {@link StreamUtil}.
 *
 * @author Nicolas Roduit
 */
public final class FileUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

  /** Buffer size used by the copy loops of this package. */
  public static final int FILE_BUFFER = 64 * 1024;

  private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");

  // ASCII characters allowed in a file name: printable, without " * / : < > ? \ |
  private static final boolean[] VALID_ASCII = buildValidAsciiTable();

  private FileUtil() {}

  private static boolean[] buildValidAsciiTable() {
    var table = new boolean[128];
    for (int c = ' '; c < 127; c++) {
      table[c] = true;
    }
    for (char c : "\"*/:<>?\\|".toCharArray()) {
      table[c] = false;
    }
    return table;
  }

  private static boolean isValidFileNameChar(char c) {
    return c < 128 ? VALID_ASCII[c] : c >= '\u00a0';
  }

  /**
   * Transform a fileName into a valid fileName for all operating systems. All special and control
   * characters are excluded.
   *
   * @param fileName a filename or directory name
   * @return a valid filename, empty string if input is null
   */
  public static String getValidFileName(String fileName) {
    if (fileName == null) {
      return "";
    }
    int length = fileName.length();
    StringBuilder result = null;
    for (int i = 0; i < length; i++) {
      char c = fileName.charAt(i);
      if (isValidFileNameChar(c)) {
        if (result != null) {
          result.append(c);
        }
      } else if (result == null) {
        result = new StringBuilder(length).append(fileName, 0, i);
      }
    }
    return (result == null ? fileName : result.toString()).trim();
  }

  /**
   * Transform a fileName into a valid fileName for all operating systems. HTML/XML tags and special
   * characters are removed.
   *
   * @param fileName a file name
   * @return a valid file name, empty string if input is null
   */
  public static String getValidFileNameWithoutHTML(String fileName) {
    if (fileName == null) {
      return "";
    }
    return getValidFileName(HTML_TAG.matcher(fileName).replaceAll(""));
  }

  /**
   * Create a unique temporary directory in the specified directory.
   *
   * @param baseDir the base directory where the temporary directory is created
   * @return the temporary directory path
   * @throws IllegalArgumentException if baseDir is null or not a directory
   * @throws IllegalStateException if the directory cannot be created after 1000 attempts
   */
  public static Path createTempDir(Path baseDir) {
    if (baseDir == null || !Files.isDirectory(baseDir)) {
      throw new IllegalArgumentException("Base directory must exist and be a directory");
    }
    var baseName = String.valueOf(System.currentTimeMillis());
    for (int counter = 0; counter < 1000; counter++) {
      var tempDir = baseDir.resolve(baseName + counter);
      try {
        return Files.createDirectory(tempDir);
      } catch (FileAlreadyExistsException ignored) {
        // Continue to next counter
      } catch (IOException e) {
        throw new IllegalStateException("Failed to create temporary directory", e);
      }
    }
    throw new IllegalStateException("Failed to create directory after 1000 attempts");
  }

  /**
   * Get all files in a directory and its subdirectories.
   *
   * @param directory the directory path
   * @param files the list of paths to populate
   */
  public static void getAllFilesInDirectory(Path directory, List<Path> files) {
    getAllFilesInDirectory(directory, files, true);
  }

  /**
   * Get all files in a directory with optional recursion. Symbolic links are followed.
   *
   * @param directory the directory path
   * @param files the list of paths to populate
   * @param recursive true to include subdirectories
   */
  public static void getAllFilesInDirectory(Path directory, List<Path> files, boolean recursive) {
    if (files == null || isNotDirectory(directory)) {
      return;
    }
    // The visitor receives the attributes of the listing: no extra stat per entry
    var visitor =
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (attrs.isRegularFile()) {
              files.add(file);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFileFailed(Path file, IOException exc) {
            LOGGER.warn("Cannot read {}: {}", file, exc.getMessage());
            return FileVisitResult.CONTINUE;
          }
        };
    try {
      Files.walkFileTree(
          directory,
          EnumSet.of(FileVisitOption.FOLLOW_LINKS),
          recursive ? Integer.MAX_VALUE : 1,
          visitor);
    } catch (IOException e) {
      LOGGER.warn("Failed to list directory contents: {}", directory, e);
    }
  }

  /**
   * Delete a file, a symbolic link, or a directory and all its contents. Symbolic links are not
   * followed.
   *
   * @param path the file or directory to delete
   * @return true if successfully deleted; false otherwise
   */
  public static boolean delete(Path path) {
    if (path == null) {
      return false;
    }
    try {
      return Files.deleteIfExists(path);
    } catch (DirectoryNotEmptyException e) {
      return deleteTree(path, 0, false);
    } catch (IOException e) {
      logDelete(e, path);
      return false;
    }
  }

  /**
   * Delete all files below a directory, and the directories whose level is at least deleteDirLevel.
   * Symbolic links are followed.
   *
   * @param directory the directory path
   * @param deleteDirLevel the level from which directories are deleted
   * @param level the level of the given directory
   */
  public static void deleteDirectoryContents(Path directory, int deleteDirLevel, int level) {
    if (isNotDirectory(directory)) {
      return;
    }
    deleteTree(directory, deleteDirLevel - level, true);
  }

  /**
   * Delete all files and subdirectories of a directory.
   *
   * @param rootDir the root directory to delete
   */
  public static void recursiveDelete(Path rootDir) {
    recursiveDelete(rootDir, true);
  }

  /**
   * Delete all files and subdirectories of a directory. Symbolic links are followed.
   *
   * @param rootDir the root directory to delete
   * @param deleteRoot true to delete the root directory, false to keep it
   */
  public static void recursiveDelete(Path rootDir, boolean deleteRoot) {
    if (isNotDirectory(rootDir)) {
      return;
    }
    deleteTree(rootDir, deleteRoot ? 0 : 1, true);
  }

  // Deletes every file below root and every directory at depth >= minDirDepth (root is depth 0)
  private static boolean deleteTree(Path root, int minDirDepth, boolean followLinks) {
    var visitor = new DeleteVisitor(minDirDepth);
    var options =
        followLinks
            ? EnumSet.of(FileVisitOption.FOLLOW_LINKS)
            : EnumSet.noneOf(FileVisitOption.class);
    try {
      Files.walkFileTree(root, options, Integer.MAX_VALUE, visitor);
    } catch (IOException e) {
      logDelete(e, root);
      return false;
    }
    return visitor.success;
  }

  private static boolean deleteQuietly(Path path) {
    try {
      return Files.deleteIfExists(path);
    } catch (IOException e) {
      logDelete(e, path);
      return false;
    }
  }

  private static void logDelete(Exception e, Path path) {
    LOGGER.error("Cannot delete: {}", path, e);
  }

  private static boolean isNotDirectory(Path directory) {
    return directory == null || !Files.isDirectory(directory);
  }

  /**
   * Prepare a file to be written by creating parent directories if necessary.
   *
   * @param path the target file path
   * @throws IOException if an I/O error occurs
   */
  public static void prepareToWriteFile(Path path) throws IOException {
    var parent = path.getParent();
    if (parent != null) {
      // createDirectories is a no-op when the directory exists: no separate existence check
      Files.createDirectories(parent);
    }
  }

  /**
   * Get the name of a file without the extension.
   *
   * @param filename the file name
   * @return the name without extension, null if input is null
   */
  public static String nameWithoutExtension(String filename) {
    if (filename == null) {
      return null;
    }
    int dotIndex = filename.lastIndexOf('.');
    return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
  }

  /**
   * Get the extension of a file name.
   *
   * @param filename the file name
   * @return the extension starting with '.' or empty string if none
   */
  public static String getExtension(String filename) {
    if (filename == null) {
      return "";
    }
    int dotIndex = filename.lastIndexOf('.');
    return dotIndex > 0 ? filename.substring(dotIndex) : "";
  }

  /**
   * Check if file extension matches any of the provided extensions (case-insensitive).
   *
   * @param path the file path
   * @param extensions the extensions array (with or without dots)
   * @return true if extension matches
   */
  public static boolean isFileExtensionMatching(Path path, String[] extensions) {
    if (path == null || extensions == null || path.getFileName() == null) {
      return false;
    }
    var fileExtension = getExtension(path.getFileName().toString());
    if (fileExtension.isEmpty()) {
      return false;
    }
    for (String extension : extensions) {
      if (StringUtil.hasText(extension) && matchesExtension(fileExtension, extension)) {
        return true;
      }
    }
    return false;
  }

  // fileExtension always starts with '.', candidate may or may not
  private static boolean matchesExtension(String fileExtension, String candidate) {
    int offset = candidate.charAt(0) == '.' ? 0 : 1;
    return fileExtension.length() == candidate.length() + offset
        && fileExtension.regionMatches(true, offset, candidate, 0, candidate.length());
  }

  /**
   * Write inputStream content to a file.
   *
   * @param inputStream the input stream
   * @param outPath the output file path
   * @param closeInputStream true to close the input stream
   * @return -1 on success, or the number of bytes written before an interruption
   * @throws StreamIOException if an I/O error occurs
   */
  public static int writeStream(InputStream inputStream, Path outPath, boolean closeInputStream)
      throws StreamIOException {
    try {
      return writeToFile(outPath, out -> StreamUtil.copy(inputStream, out));
    } finally {
      if (closeInputStream) {
        StreamUtil.safeClose(inputStream);
      }
    }
  }

  /**
   * Write inputStream content to a file and close the input stream.
   *
   * @param inputStream the input stream
   * @param outPath the output file path
   * @return -1 on success, or the number of bytes written before an interruption
   * @throws StreamIOException if an I/O error occurs
   */
  public static int writeStream(InputStream inputStream, Path outPath) throws StreamIOException {
    return writeStream(inputStream, outPath, true);
  }

  /**
   * Write inputStream content to a file, failing when nothing could be written.
   *
   * @param inputStream the input stream
   * @param outPath the output file path
   * @throws StreamIOException if an I/O error occurs or write fails
   */
  public static void writeStreamWithIOException(InputStream inputStream, Path outPath)
      throws StreamIOException {
    int result = writeStream(inputStream, outPath, true);
    if (result == 0) {
      throw new StreamIOException("Failed to write stream to file: " + outPath);
    }
  }

  /**
   * Write ImageInputStream content to a file and close the input stream.
   *
   * @param imageInputStream the input stream
   * @param outPath the output file path
   * @return -1 on success, or the number of bytes written before an interruption
   * @throws StreamIOException if an I/O error occurs
   */
  public static int writeFile(ImageInputStream imageInputStream, Path outPath)
      throws StreamIOException {
    try {
      return writeToFile(outPath, out -> StreamUtil.copyImageInputStream(imageInputStream, out));
    } finally {
      StreamUtil.safeClose(imageInputStream);
    }
  }

  @FunctionalInterface
  private interface StreamCopier {
    void copyTo(OutputStream out) throws IOException;
  }

  // Removes the partially written file on failure
  private static int writeToFile(Path outPath, StreamCopier copier) throws StreamIOException {
    try {
      prepareToWriteFile(outPath);
      try (var out = Files.newOutputStream(outPath)) {
        copier.copyTo(out);
      }
      return -1;
    } catch (SocketTimeoutException e) {
      delete(outPath);
      throw new StreamIOException(e);
    } catch (InterruptedIOException e) {
      delete(outPath);
      LOGGER.error("Interruption when writing file: {}", e.getMessage());
      return e.bytesTransferred;
    } catch (IOException e) {
      delete(outPath);
      throw new StreamIOException(e);
    }
  }

  /**
   * Format byte count in human-readable format.
   *
   * @see <a href="https://programming.guide/worlds-most-copied-so-snippet.html">World's most copied
   *     StackOverflow snippet</a>
   * @param bytes number of bytes
   * @param si true for SI units (1000), false for binary units (1024)
   * @return human-readable size string
   */
  public static String humanReadableByte(long bytes, boolean si) {
    int unit = si ? 1000 : 1024;
    long absBytes = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
    if (absBytes < unit) {
      return bytes + " B";
    }
    int exp = (int) (Math.log(absBytes) / Math.log(unit));
    long threshold = (long) Math.ceil(Math.pow(unit, exp) * (unit - 0.05));
    if (exp < 6 && absBytes >= threshold - ((threshold & 0xFFF) == 0xD00 ? 51 : 0)) {
      exp++;
    }

    var unitChars = si ? "kMGTPE" : "KMGTPE";
    var prefix = unitChars.charAt(exp - 1) + (si ? "" : "i");
    if (exp > 4) {
      bytes /= unit;
      exp -= 1;
    }
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), prefix);
  }

  /**
   * Copy a folder and its contents to another folder.
   *
   * @param source the source folder
   * @param target the target folder
   * @param options the copy options
   * @throws IOException if an I/O error occurs
   */
  public static void copyFolder(Path source, Path target, CopyOption... options)
      throws IOException {
    Files.walkFileTree(source, new FolderCopyVisitor(source, target, options));
  }

  /**
   * Get the output path, resolving filename if target is a directory.
   *
   * @param input the input path
   * @param output the output path
   * @return the resolved output path
   */
  public static Path getOutputPath(Path input, Path output) {
    return Files.isDirectory(output) ? output.resolve(input.getFileName()) : output;
  }

  /**
   * Add an index to a filename before the extension.
   *
   * @param path the file path
   * @param index the index to add
   * @param indexSize the minimum number of digits (zero-padded)
   * @return the path with index added
   */
  public static Path addFileIndex(Path path, int index, int indexSize) {
    if (indexSize < 1) {
      return path;
    }
    var fileName = path.getFileName().toString();
    var indexStr = String.format("-%0" + indexSize + "d", index);
    return path.resolveSibling(nameWithoutExtension(fileName) + indexStr + getExtension(fileName));
  }

  private static final class DeleteVisitor extends SimpleFileVisitor<Path> {
    private final int minDirDepth;
    private int depth;
    private boolean success = true;

    DeleteVisitor(int minDirDepth) {
      this.minDirDepth = minDirDepth;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
      depth++;
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
      success &= deleteQuietly(file);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
      logDelete(exc, file);
      success = false;
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
      depth--;
      if (exc != null) {
        logDelete(exc, dir);
        success = false;
      } else if (depth >= minDirDepth) {
        success &= deleteQuietly(dir);
      }
      return FileVisitResult.CONTINUE;
    }
  }

  private static final class FolderCopyVisitor extends SimpleFileVisitor<Path> {
    private final Path source;
    private final Path target;
    private final CopyOption[] options;

    FolderCopyVisitor(Path source, Path target, CopyOption[] options) {
      this.source = source;
      this.target = target;
      this.options = options;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        throws IOException {
      Files.createDirectories(target.resolve(source.relativize(dir)));
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      Files.copy(file, target.resolve(source.relativize(file)), options);
      return FileVisitResult.CONTINUE;
    }
  }
}
