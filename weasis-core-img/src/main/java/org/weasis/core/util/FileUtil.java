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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import javax.imageio.stream.ImageInputStream;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.util.annotations.Generated;

/**
 * Modern utility class for file operations and path management using Java NIO.2.
 *
 * <p>This class provides comprehensive file manipulation, validation, and path operations using
 * {@link Path} as the primary API, leveraging Java 17 features for better performance and
 * maintainability.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Path-based operations with automatic parent directory creation
 *   <li>Safe file and directory deletion with proper error handling
 *   <li>Stream-based file copying with resource management
 *   <li>Filename validation and sanitization
 *   <li>Extension-based file filtering
 * </ul>
 *
 * <p>For stream operations, use {@link StreamUtil}. All {@link File}-based methods are deprecated
 * in favor of {@link Path}-based equivalents.
 *
 * @author Nicolas Roduit
 */
public final class FileUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);
  public static final int FILE_BUFFER = 4096;
  private static final String CANNOT_DELETE = "Cannot delete";

  // Optimized with Set for O(1) lookups instead of binary search
  private static final Set<Integer> ILLEGAL_CHARS =
      Set.of(
          0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
          25, 26, 27, 28, 29, 30, 31, 34, 42, 47, 58, 60, 62, 63, 92, 124);

  private FileUtil() {
    // Prevent instantiation
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

    return fileName
        .chars()
        .filter(FileUtil::isValidFileNameChar)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString()
        .trim();
  }

  private static boolean isValidFileNameChar(int codePoint) {
    return !ILLEGAL_CHARS.contains(codePoint)
        && codePoint >= ' '
        && (codePoint <= '~' || codePoint >= '\u00a0');
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
    return getValidFileName(fileName.replaceAll("<[^>]*>", ""));
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
   * Get all files in a directory with optional recursion.
   *
   * @param directory the directory path
   * @param files the list of paths to populate
   * @param recursive true to include subdirectories
   */
  public static void getAllFilesInDirectory(Path directory, List<Path> files, boolean recursive) {
    if (!isValidDirectory(directory) || files == null) {
      return;
    }

    try (var stream = Files.list(directory)) {
      stream.forEach(path -> processDirectoryEntry(path, files, recursive));
    } catch (IOException e) {
      LOGGER.warn("Failed to list directory contents: {}", directory, e);
    }
  }

  private static boolean isValidDirectory(Path directory) {
    return directory != null && Files.isDirectory(directory);
  }

  private static void processDirectoryEntry(Path path, List<Path> files, boolean recursive) {
    if (Files.isRegularFile(path)) {
      files.add(path);
    } else if (recursive && Files.isDirectory(path)) {
      getAllFilesInDirectory(path, files, true);
    }
  }

  /**
   * Delete a file or directory and all its contents.
   *
   * @param path the file or directory to delete
   * @return true if successfully deleted; false otherwise
   */
  public static boolean delete(Path path) {
    if (path == null || !Files.exists(path)) {
      return false;
    }

    try {
      if (Files.isDirectory(path)) {
        return deleteDirectory(path);
      } else {
        return deleteQuietly(path);
      }
    } catch (Exception e) {
      LOGGER.error("{}: {}", CANNOT_DELETE, path, e);
      return false;
    }
  }

  private static boolean deleteDirectory(Path directory) {
    try (var walk = Files.walk(directory)) {
      walk.sorted(Comparator.reverseOrder()).forEach(FileUtil::deleteQuietly);
      return !Files.exists(directory);
    } catch (IOException e) {
      LOGGER.error("{}: {}", CANNOT_DELETE, directory, e);
      return false;
    }
  }

  /**
   * Delete directory contents based on directory level.
   *
   * @param directory the directory path
   * @param deleteDirLevel the level of subdirectories to delete
   * @param level the current level
   */
  public static void deleteDirectoryContents(Path directory, int deleteDirLevel, int level) {
    if (!isValidDirectory(directory)) {
      return;
    }
    try (var stream = Files.list(directory)) {
      stream.forEach(path -> processDeleteEntry(path, deleteDirLevel, level));
    } catch (IOException e) {
      LOGGER.warn("Failed to delete directory contents: {}", directory, e);
    }
    if (level >= deleteDirLevel) {
      deleteQuietly(directory);
    }
  }

  private static void processDeleteEntry(Path path, int deleteDirLevel, int level) {
    if (Files.isDirectory(path)) {
      deleteDirectoryContents(path, deleteDirLevel, level + 1);
    } else {
      deleteQuietly(path);
    }
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
   * Delete all files and subdirectories of a directory.
   *
   * @param rootDir the root directory to delete
   * @param deleteRoot true to delete the root directory, false to keep it
   */
  public static void recursiveDelete(Path rootDir, boolean deleteRoot) {
    if (!isValidDirectory(rootDir)) {
      return;
    }
    try (var stream = Files.list(rootDir)) {
      stream.forEach(FileUtil::processRecursiveDelete);
    } catch (IOException e) {
      LOGGER.warn("Failed to delete directory contents: {}", rootDir, e);
    }

    if (deleteRoot) {
      deleteQuietly(rootDir);
    }
  }

  private static void processRecursiveDelete(Path path) {
    if (Files.isDirectory(path)) {
      recursiveDelete(path, true);
    } else {
      deleteQuietly(path);
    }
  }

  private static boolean deleteQuietly(Path path) {
    try {
      return Files.deleteIfExists(path);
    } catch (IOException e) {
      LOGGER.error("{}: {}", CANNOT_DELETE, path, e);
      return false;
    }
  }

  /**
   * Prepare a file to be written by creating parent directories if necessary.
   *
   * @param path the target file path
   * @throws IOException if an I/O error occurs
   */
  public static void prepareToWriteFile(Path path) throws IOException {
    var parent = path.getParent();
    if (parent != null && !Files.exists(parent)) {
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
    if (path == null || extensions == null) {
      return false;
    }

    var filename = Optional.ofNullable(path.getFileName()).map(Path::toString).orElse("");
    var fileExtension = getExtension(filename);
    if (!StringUtil.hasLength(fileExtension)) {
      return false;
    }

    return Arrays.stream(extensions)
        .filter(StringUtil::hasText)
        .map(ext -> ext.startsWith(".") ? ext : "." + ext)
        .anyMatch(fileExtension::equalsIgnoreCase);
  }

  /**
   * Write inputStream content to a file.
   *
   * @param inputStream the input stream
   * @param outPath the output file path
   * @param closeInputStream true to close the input stream
   * @return the number of written bytes (-1 = success, 0 = error, other = interrupted bytes)
   * @throws StreamIOException if an I/O error occurs
   */
  public static int writeStream(InputStream inputStream, Path outPath, boolean closeInputStream)
      throws StreamIOException {
    try {
      prepareToWriteFile(outPath);
      return performStreamWrite(inputStream, outPath);
    } catch (IOException e) {
      throw new StreamIOException(e);
    } finally {
      if (closeInputStream) {
        StreamUtil.safeClose(inputStream);
      }
    }
  }

  private static int performStreamWrite(InputStream inputStream, Path outPath)
      throws StreamIOException {
    try (var outputStream = Files.newOutputStream(outPath)) {
      return copyStreamData(inputStream, outputStream, outPath);
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

  private static int copyStreamData(
      InputStream inputStream, OutputStream outputStream, Path outPath) throws IOException {
    var buffer = new byte[FILE_BUFFER];
    int bytesRead;
    while ((bytesRead = inputStream.read(buffer)) > 0) {
      outputStream.write(buffer, 0, bytesRead);
    }
    outputStream.flush();
    return -1; // Success indicator
  }

  /**
   * Write inputStream content to a file.
   *
   * @param inputStream the input stream
   * @param outPath the output file path
   * @return the number of written bytes (-1 = success, 0 = error, other = interrupted bytes)
   * @throws StreamIOException if an I/O error occurs
   */
  public static int writeStream(InputStream inputStream, Path outPath) throws StreamIOException {
    return writeStream(inputStream, outPath, true);
  }

  /**
   * Write inputStream content to a file with exception on failure.
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
   * Write ImageInputStream content to a file.
   *
   * @param imageInputStream the input stream
   * @param outPath the output file path
   * @return the number of written bytes (-1 = success, 0 = error, other = interrupted bytes)
   * @throws StreamIOException if an I/O error occurs
   */
  public static int writeFile(ImageInputStream imageInputStream, Path outPath)
      throws StreamIOException {
    try {
      prepareToWriteFile(outPath);
      return performImageStreamWrite(imageInputStream, outPath);
    } catch (IOException e) {
      throw new StreamIOException(e);
    } finally {
      StreamUtil.safeClose(imageInputStream);
    }
  }

  private static int performImageStreamWrite(ImageInputStream imageInputStream, Path outPath)
      throws StreamIOException {
    try (var outputStream = Files.newOutputStream(outPath)) {
      return copyImageStreamData(imageInputStream, outputStream);
    } catch (SocketTimeoutException e) {
      delete(outPath);
      throw new StreamIOException(e);
    } catch (InterruptedIOException e) {
      delete(outPath);
      LOGGER.error("Interruption when writing image: {}", e.getMessage());
      return e.bytesTransferred;
    } catch (IOException e) {
      delete(outPath);
      throw new StreamIOException(e);
    }
  }

  private static int copyImageStreamData(
      ImageInputStream imageInputStream, OutputStream outputStream) throws IOException {
    var buffer = new byte[FILE_BUFFER];
    int bytesRead;
    while ((bytesRead = imageInputStream.read(buffer)) > 0) {
      outputStream.write(buffer, 0, bytesRead);
    }
    outputStream.flush();
    return -1; // Success indicator
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
    var pattern = String.format("$1-%%0%dd$2", indexSize);
    var replacement = String.format(pattern, index);
    var newName = path.getFileName().toString().replaceFirst("(.*?)(\\.[^.]+)?$", replacement);
    return path.resolveSibling(newName);
  }

  // Inner class for folder copying
  private static class FolderCopyVisitor extends SimpleFileVisitor<Path> {
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

  // ============================== DEPRECATED FILE-BASED METHODS ==============================

  /**
   * Safely closes the provided {@code AutoCloseable} resource, suppressing any exceptions that may
   * occur during the closing operation. This method delegates the closing operation to {@code
   * StreamUtil.safeClose}.
   *
   * @param autoCloseable the resource that needs to be closed; can be null, in which case the
   *     method does nothing
   * @deprecated Use {@link StreamUtil#safeClose(AutoCloseable)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static void safeClose(final AutoCloseable autoCloseable) {
    StreamUtil.safeClose(autoCloseable);
  }

  /**
   * Safely closes the provided {@code XMLStreamWriter} resource, suppressing any exceptions that
   * may occur during the closing operation. This method delegates the closing operation to {@code
   * StreamUtil.safeClose}.
   *
   * @param writer the XMLStreamWriter to close; can be null, in which case the method does nothing
   * @deprecated Use {@link StreamUtil#safeClose(XMLStreamWriter)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static void safeClose(XMLStreamWriter writer) {
    StreamUtil.safeClose(writer);
  }

  /**
   * Safely closes the provided {@code XMLStreamReader} resource, suppressing any exceptions that
   * may occur during the closing operation. This method delegates the closing operation to {@code
   * StreamUtil.safeClose}.
   *
   * @param reader the XMLStreamReader to close; can be null, in which case the method does nothing
   * @deprecated Use {@link StreamUtil#safeClose(XMLStreamReader)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static void safeClose(XMLStreamReader reader) {
    StreamUtil.safeClose(reader);
  }

  /**
   * Write the content of a FileInputStream to a FileOutputStream using NIO.
   *
   * @param inputStream the input stream to read from
   * @param out the output stream to write to
   * @return true if the file was successfully written; false otherwise
   * @deprecated Use {@link StreamUtil#copyWithNIO(InputStream, OutputStream)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static boolean nioWriteFile(FileInputStream inputStream, FileOutputStream out) {
    return StreamUtil.copyWithNIO(inputStream, out);
  }

  /**
   * Write the content of an InputStream to an OutputStream using NIO.
   *
   * @param in the input stream to read from
   * @param out the output stream to write to
   * @param bufferSize the size of the buffer used for copying
   * @return true if the file was successfully written; false otherwise
   * @deprecated Use {@link StreamUtil#copyWithNIO(InputStream, OutputStream, int)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static boolean nioWriteFile(InputStream in, OutputStream out, final int bufferSize) {
    return StreamUtil.copyWithNIO(in, out, bufferSize);
  }

  /**
   * Copy a file using NIO. This method is deprecated and will be removed in future versions.
   *
   * @deprecated Use {@link StreamUtil#copyFile(Path, Path)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static boolean nioCopyFile(File source, File destination) {
    return StreamUtil.copyFile(source.toPath(), destination.toPath());
  }

  /**
   * Read properties from a file and return them as a Properties object.
   *
   * @param propsFile the properties file to read
   * @param props the Properties object to fill, or null to create a new one
   * @return the filled Properties object
   * @deprecated Use {@link PropertiesUtil#loadProperties(Path, Properties)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static Properties readProperties(File propsFile, Properties props) {
    Properties p = props == null ? new Properties() : props;
    PropertiesUtil.loadProperties(propsFile.toPath(), p);
    return p;
  }

  /**
   * Store properties to a file with optional comments.
   *
   * @param propsFile the properties file to write
   * @param props the Properties object to write
   * @param comments optional comments for the properties file
   * @deprecated Use {@link PropertiesUtil#storeProperties(Path, Properties, String)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static void storeProperties(File propsFile, Properties props, String comments) {
    PropertiesUtil.storeProperties(propsFile.toPath(), props, comments);
  }

  /**
   * Zip a directory and its content into a zip file.
   *
   * @param directory the directory to zip
   * @param zipFile the output zip file
   * @throws IOException if an I/O error occurs
   * @deprecated Use {@link ZipUtil#zip(Path, Path)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static void zip(File directory, File zipFile) throws IOException {
    ZipUtil.zip(directory.toPath(), zipFile.toPath());
  }

  /**
   * Unzip a zip file into a directory.
   *
   * @param inputStream the input stream of the zip file
   * @param directory the output directory where the content is extracted
   * @throws IOException if an I/O error occurs
   * @deprecated Use {@link ZipUtil#unzip(InputStream, Path)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static void unzip(InputStream inputStream, File directory) throws IOException {
    ZipUtil.unzip(inputStream, directory.toPath());
  }

  /**
   * Create a unique temporary directory in the specified directory.
   *
   * @param baseDir the base directory where the temporary directory is created
   * @return the temporary directory
   * @deprecated Use {@link #createTempDir(Path)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static File createTempDir(File baseDir) {
    if (baseDir == null) {
      throw new IllegalArgumentException("Base directory cannot be null");
    }
    return createTempDir(baseDir.toPath()).toFile();
  }

  /**
   * Get all files in a directory and its subdirectories.
   *
   * @param directory the directory
   * @param files the list of files to fill
   * @deprecated Use {@link #getAllFilesInDirectory(Path, List)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static void getAllFilesInDirectory(File directory, List<File> files) {
    getAllFilesInDirectory(directory, files, true);
  }

  /**
   * Get all files in a directory.
   *
   * @param directory the directory
   * @param files the list of files to fill
   * @param recursive true to get files in subdirectories
   * @deprecated Use {@link #getAllFilesInDirectory(Path, List, boolean)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static void getAllFilesInDirectory(File directory, List<File> files, boolean recursive) {
    List<Path> paths = new ArrayList<>();
    for (File file : files) {
      paths.add(file.toPath());
    }
    getAllFilesInDirectory(directory.toPath(), paths, recursive);
  }

  /**
   * Delete a file or a directory and all its content.
   *
   * @param fileOrDirectory the file or directory to delete
   * @return true if the file or directory is successfully deleted; false otherwise
   * @deprecated Use {@link #delete(Path)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static boolean delete(File fileOrDirectory) {
    if (fileOrDirectory == null) {
      return false;
    }
    return delete(fileOrDirectory.toPath());
  }

  /**
   * Delete the content of a directory and the directory itself if the level is reached.
   *
   * @param dir the directory
   * @param deleteDirLevel the level of subdirectories to delete
   * @param level the current level
   * @deprecated Use {@link #deleteDirectoryContents(Path, int, int)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static void deleteDirectoryContents(final File dir, int deleteDirLevel, int level) {
    if (dir == null) {
      return;
    }
    deleteDirectoryContents(dir.toPath(), deleteDirLevel, level);
  }

  /**
   * Delete all files and subdirectories of a specific folder.
   *
   * @param rootDir the root directory to delete
   * @deprecated Use {@link #recursiveDelete(Path)} instead
   */
  @Generated
  @Deprecated(since = "4.12", forRemoval = true)
  public static void recursiveDelete(File rootDir) {
    recursiveDelete(rootDir, true);
  }

  /**
   * Delete all files and subdirectories of a specific folder.
   *
   * @param rootDir the root directory to delete
   * @param deleteRoot true to delete the root directory at the end and false to keep it
   * @deprecated Use {@link #recursiveDelete(Path, boolean)} instead
   */
  @Generated
  @Deprecated(since = "4.12", forRemoval = true)
  public static void recursiveDelete(File rootDir, boolean deleteRoot) {
    if (rootDir == null) {
      return;
    }
    recursiveDelete(rootDir.toPath(), deleteRoot);
  }

  /**
   * Prepare a file to be written by creating the parent directory if necessary.
   *
   * @param file the target file
   * @throws IOException if an I/O error occurs
   * @deprecated Use {@link #prepareToWriteFile(Path)} instead
   */
  @Generated
  @Deprecated(since = "4.12", forRemoval = true)
  public static void prepareToWriteFile(File file) throws IOException {
    if (file == null) {
      throw new IllegalArgumentException("File cannot be null");
    }
    prepareToWriteFile(file.toPath());
  }

  /**
   * Check if the extension of the file name matches one of the extensions in the array.
   *
   * @param file the file
   * @param extensions the extensions array, e.g. {"png", "jpg"} or {".png", ".jpg"}
   * @return true if the file extension matches one of the provided extensions
   * @deprecated Use {@link #isFileExtensionMatching(Path, String[])} instead
   */
  @Generated
  @Deprecated(since = "4.12", forRemoval = true)
  public static boolean isFileExtensionMatching(File file, String[] extensions) {
    if (file == null) {
      return false;
    }
    return isFileExtensionMatching(file.toPath(), extensions);
  }

  /**
   * Write inputStream content into a file.
   *
   * @param inputStream the input stream
   * @param outFile the output file
   * @return the number of written bytes. 0 = error, -1 = all bytes has been written, other = bytes
   *     written before interruption
   * @throws StreamIOException if an I/O error occurs
   * @deprecated Use {@link #writeStream(InputStream, Path)} instead
   */
  @Generated
  @Deprecated(since = "4.12", forRemoval = true)
  public static int writeStream(InputStream inputStream, File outFile) throws StreamIOException {
    return writeStream(inputStream, outFile, true);
  }

  /**
   * Write inputStream content into a file and close the input stream if closeInputStream is true.
   *
   * @param inputStream the input stream
   * @param outFile the output
   * @param closeInputStream true to close the input stream
   * @return the number of written bytes. 0 = error, -1 = all bytes has been written, other = bytes
   *     written before interruption
   * @throws StreamIOException if an I/O error occurs
   * @deprecated Use {@link #writeStream(InputStream, Path, boolean)} instead
   */
  @Generated
  @Deprecated(since = "4.12", forRemoval = true)
  public static int writeStream(InputStream inputStream, File outFile, boolean closeInputStream)
      throws StreamIOException {
    return writeStream(inputStream, outFile.toPath(), closeInputStream);
  }

  /**
   * Write inputStream content into a file using StreamUtil.
   *
   * @param inputStream the input stream
   * @param outFile the output file
   * @throws StreamIOException if an I/O error occurs
   * @deprecated Use {@link #writeStreamWithIOException(InputStream, Path)} instead
   */
  @Generated
  @Deprecated(since = "4.12", forRemoval = true)
  public static void writeStreamWithIOException(InputStream inputStream, File outFile)
      throws StreamIOException {
    if (outFile == null) {
      throw new IllegalArgumentException("Output file cannot be null");
    }
    writeStreamWithIOException(inputStream, outFile.toPath());
  }

  /**
   * Write inputStream content into a file
   *
   * @param imageInputStream the input stream
   * @param outFile the output file
   * @return the number of written bytes. 0 = error, -1 = all bytes has been written, other = bytes
   *     written before interruption
   * @throws StreamIOException if an I/O error occurs
   * @deprecated Use {@link #writeFile(ImageInputStream, Path)} instead
   */
  @Generated
  @Deprecated(since = "4.12", forRemoval = true)
  public static int writeFile(ImageInputStream imageInputStream, File outFile)
      throws StreamIOException {
    return writeFile(imageInputStream, outFile.toPath());
  }
}
