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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import javax.imageio.stream.ImageInputStream;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.util.annotations.Generated;

/**
 * Utility class for file operations and path management.
 *
 * <p>This class provides methods for file manipulation, validation, and path operations using
 * {@link Path} as the primary API. For stream operations, use {@link StreamUtil}.
 *
 * <p>All {@link File}-based methods are deprecated in favor of {@link Path}-based equivalents.
 *
 * @author Nicolas Roduit
 */
public final class FileUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);
  public static final int FILE_BUFFER = 4096;

  private static final int[] ILLEGAL_CHARS = {
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
    26, 27, 28, 29, 30, 31, 34, 42, 47, 58, 60, 62, 63, 92, 124
  };
  private static final String CANNOT_DELETE = "Cannot delete";

  private FileUtil() {
    // Prevent instantiation
  }

  /**
   * Transform a fileName into a writable fileName for all operating systems. All special and
   * control characters are excluded.
   *
   * @param fileName a filename or directory name
   * @return a writable filename
   */
  public static String getValidFileName(String fileName) {
    if (fileName == null) {
      return "";
    }

    StringBuilder cleanName = new StringBuilder(fileName.length());
    for (int i = 0; i < fileName.length(); i++) {
      char c = fileName.charAt(i);
      if (isValidFileNameChar(c)) {
        cleanName.append(c);
      }
    }
    return cleanName.toString().trim();
  }

  private static boolean isValidFileNameChar(char c) {
    return Arrays.binarySearch(ILLEGAL_CHARS, c) < 0 && c >= ' ' && (c <= '~' || c >= '\u00a0');
  }

  /**
   * Transform a fileName into a fileName for all operating systems. All special and control
   * characters are excluded. HTML or XML tags are also removed.
   *
   * @param fileName a file name
   * @return a writable file name
   */
  public static String getValidFileNameWithoutHTML(String fileName) {
    if (fileName == null) {
      return "";
    }
    // Remove HTML tags first
    String withoutHtml = fileName.replaceAll("<[^>]*>", "");
    return getValidFileName(withoutHtml);
  }

  /**
   * Create a unique temporary directory in the specified directory.
   *
   * @param baseDir the base directory where the temporary directory is created
   * @return the temporary directory
   * @throws IllegalArgumentException if baseDir is null or not a directory
   * @throws IllegalStateException if the directory cannot be created
   */
  public static Path createTempDir(Path baseDir) {
    if (baseDir == null || !Files.isDirectory(baseDir)) {
      throw new IllegalArgumentException("Base directory must exist and be a directory");
    }
    String baseName = String.valueOf(System.currentTimeMillis());
    for (int counter = 0; counter < 1000; counter++) {
      Path tempDir = baseDir.resolve(baseName + counter);
      try {
        return Files.createDirectory(tempDir);
      } catch (FileAlreadyExistsException ignored) {
        // Try next counter
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
   * @param files the list of paths to fill
   */
  public static void getAllFilesInDirectory(Path directory, List<Path> files) {
    getAllFilesInDirectory(directory, files, true);
  }

  /**
   * Get all files in a directory.
   *
   * @param directory the directory path
   * @param files the list of paths to fill
   * @param recursive true to get files in subdirectories
   */
  public static void getAllFilesInDirectory(Path directory, List<Path> files, boolean recursive) {
    if (directory == null || !Files.isDirectory(directory) || files == null) {
      return;
    }

    try (Stream<Path> stream = Files.list(directory)) {
      stream.forEach(
          path -> {
            if (Files.isRegularFile(path)) {
              files.add(path);
            } else if (recursive && Files.isDirectory(path)) {
              getAllFilesInDirectory(path, files, true);
            }
          });
    } catch (IOException e) {
      LOGGER.warn("Failed to list directory contents: {}", directory, e);
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

    if (Files.isDirectory(path)) {
      try (Stream<Path> walk = Files.walk(path)) {
        walk.sorted(Comparator.reverseOrder()) // Reverse order for depth-first deletion
            .forEach(FileUtil::deleteQuietly);
      } catch (IOException e) {
        LOGGER.error(CANNOT_DELETE + ": {}", path, e);
        return false;
      }
    } else {
      return deleteQuietly(path);
    }
    return !Files.exists(path);
  }

  /**
   * Delete the content of a directory and optionally the directory itself.
   *
   * @param directory the directory path
   * @param deleteDirLevel the level of subdirectories to delete
   * @param level the current level
   */
  public static void deleteDirectoryContents(Path directory, int deleteDirLevel, int level) {
    if (directory == null || !Files.isDirectory(directory)) {
      return;
    }
    try (Stream<Path> stream = Files.list(directory)) {
      stream.forEach(
          path -> {
            if (Files.isDirectory(path)) {
              deleteDirectoryContents(path, deleteDirLevel, level + 1);
            } else {
              deleteQuietly(path);
            }
          });
    } catch (IOException e) {
      LOGGER.warn("Failed to delete directory contents: {}", directory, e);
    }
    if (level >= deleteDirLevel) {
      deleteQuietly(directory);
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
   * @param deleteRoot true to delete the root directory at the end, false to keep it
   */
  public static void recursiveDelete(Path rootDir, boolean deleteRoot) {
    if (rootDir == null || !Files.isDirectory(rootDir)) {
      return;
    }
    try (Stream<Path> stream = Files.list(rootDir)) {
      stream.forEach(
          path -> {
            if (Files.isDirectory(path)) {
              recursiveDelete(path, true);
            } else {
              deleteQuietly(path);
            }
          });
    } catch (IOException e) {
      LOGGER.warn("Failed to delete directory contents: {}", rootDir, e);
    }
    if (deleteRoot) {
      deleteQuietly(rootDir);
    }
  }

  private static boolean deleteQuietly(Path path) {
    try {
      return Files.deleteIfExists(path);
    } catch (IOException e) {
      LOGGER.error(CANNOT_DELETE + ": {}", path, e);
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
    Path parent = path.getParent();
    if (parent != null && !Files.exists(parent)) {
      Files.createDirectories(parent);
    }
  }

  /**
   * Get the name of a file without the extension (e.g. convert image.png to image).
   *
   * @param filename The file name.
   * @return The name of the file without extension.
   */
  public static String nameWithoutExtension(String filename) {
    if (filename == null) {
      return null;
    }
    int dotIndex = filename.lastIndexOf('.');
    return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
  }

  /**
   * Get the extension of a file name, like ".png" or ".jpg".
   *
   * @param filename The file name to retrieve the extension of.
   * @return The extension of the file name starting with '.' or empty string if none.
   */
  public static String getExtension(String filename) {
    if (filename == null) {
      return "";
    }
    int dotIndex = filename.lastIndexOf('.');
    return dotIndex > 0 ? filename.substring(dotIndex) : "";
  }

  /**
   * Check if the extension of the file name matches one of the extensions in the array. The
   * extension can start with '.' or not and is case-insensitive.
   *
   * @param path the file path
   * @param extensions the extensions array, e.g. {"png", "jpg"} or {".png", ".jpg"}
   * @return true if the file extension matches one of the provided extensions
   */
  public static boolean isFileExtensionMatching(Path path, String[] extensions) {
    if (path == null || extensions == null) {
      return false;
    }

    String filename = path.getFileName() != null ? path.getFileName().toString() : "";
    String fileExtension = getExtension(filename);
    if (!StringUtil.hasLength(fileExtension)) {
      return false;
    }

    return Arrays.stream(extensions)
        .filter(StringUtil::hasText)
        .map(ext -> ext.startsWith(".") ? ext : "." + ext)
        .anyMatch(fileExtension::equalsIgnoreCase);
  }

  /**
   * Write inputStream content into a file and close the input stream if closeInputStream is true.
   *
   * @param inputStream the input stream
   * @param outPath the output file path
   * @param closeInputStream true to close the input stream
   * @return the number of written bytes. 0 = error, -1 = all bytes has been written, other = bytes
   *     written before interruption
   * @throws StreamIOException if an I/O error occurs
   */
  public static int writeStream(InputStream inputStream, Path outPath, boolean closeInputStream)
      throws StreamIOException {
    try {
      prepareToWriteFile(outPath);
    } catch (IOException e) {
      throw new StreamIOException(e);
    }
    try (OutputStream outputStream = Files.newOutputStream(outPath)) {
      byte[] buf = new byte[FILE_BUFFER];
      int offset;
      while ((offset = inputStream.read(buf)) > 0) {
        outputStream.write(buf, 0, offset);
      }
      outputStream.flush();
      return -1;
    } catch (SocketTimeoutException e) {
      delete(outPath);
      throw new StreamIOException(e);
    } catch (InterruptedIOException e) {
      delete(outPath);
      // Specific for ProgressMonitor
      LOGGER.error("Interruption when writing file: {}", e.getMessage());
      return e.bytesTransferred;
    } catch (IOException e) {
      delete(outPath);
      throw new StreamIOException(e);
    } finally {
      if (closeInputStream) {
        StreamUtil.safeClose(inputStream);
      }
    }
  }

  /**
   * Write inputStream content into a file.
   *
   * @param inputStream the input stream
   * @param outPath the output file path
   * @return the number of written bytes. 0 = error, -1 = all bytes has been written, other = bytes
   *     written before interruption
   * @throws StreamIOException if an I/O error occurs
   */
  public static int writeStream(InputStream inputStream, Path outPath) throws StreamIOException {
    return writeStream(inputStream, outPath, true);
  }

  /**
   * Write inputStream content into a file using StreamUtil.
   *
   * @param inputStream the input stream
   * @param outPath the output file path
   * @throws StreamIOException if an I/O error occurs
   */
  public static void writeStreamWithIOException(InputStream inputStream, Path outPath)
      throws StreamIOException {
    int result = writeStream(inputStream, outPath, true);
    if (result == 0) {
      throw new StreamIOException("Failed to write stream to file: " + outPath);
    }
  }

  /**
   * Write ImageInputStream content into a file.
   *
   * @param imageInputStream the input stream
   * @param outPath the output file path
   * @return the number of written bytes. 0 = error, -1 = all bytes has been written, other = bytes
   *     written before interruption
   * @throws StreamIOException if an I/O error occurs
   */
  public static int writeFile(ImageInputStream imageInputStream, Path outPath)
      throws StreamIOException {
    try {
      prepareToWriteFile(outPath);
    } catch (IOException e) {
      throw new StreamIOException(e);
    }
    try (OutputStream outputStream = Files.newOutputStream(outPath)) {
      byte[] buf = new byte[FILE_BUFFER];
      int offset;
      while ((offset = imageInputStream.read(buf)) > 0) {
        outputStream.write(buf, 0, offset);
      }
      outputStream.flush();
      return -1;
    } catch (SocketTimeoutException e) {
      delete(outPath);
      throw new StreamIOException(e);
    } catch (InterruptedIOException e) {
      delete(outPath);
      // Specific for SeriesProgressMonitor
      LOGGER.error("Interruption when writing image {}", e.getMessage());
      return e.bytesTransferred;
    } catch (IOException e) {
      delete(outPath);
      throw new StreamIOException(e);
    } finally {
      StreamUtil.safeClose(imageInputStream);
    }
  }

  /**
   * Print a byte count in a human-readable format.
   *
   * @see <a href="https://programming.guide/worlds-most-copied-so-snippet.html">World's most copied
   *     StackOverflow snippet</a>
   * @param bytes number of bytes
   * @param si true for SI units (powers of 1000), false for binary units (powers of 1024)
   * @return the human-readable size of the byte count
   */
  public static String humanReadableByte(long bytes, boolean si) {
    int unit = si ? 1000 : 1024;
    long absBytes = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
    if (absBytes < unit) return bytes + " B";
    int exp = (int) (Math.log(absBytes) / Math.log(unit));
    long th = (long) Math.ceil(Math.pow(unit, exp) * (unit - 0.05));
    if (exp < 6 && absBytes >= th - ((th & 0xFFF) == 0xD00 ? 51 : 0)) exp++;
    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
    if (exp > 4) {
      bytes /= unit;
      exp -= 1;
    }
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }

  /**
   * Copy a folder and its content to another folder with copy options.
   *
   * @param source the source folder
   * @param target the target folder
   * @param options the copy options
   * @throws IOException if an I/O error occurs
   */
  public static void copyFolder(Path source, Path target, CopyOption... options)
      throws IOException {
    Files.walkFileTree(
        source,
        new SimpleFileVisitor<Path>() {

          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            Files.createDirectories(target.resolve(source.relativize(dir)));
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Files.copy(file, target.resolve(source.relativize(file)), options);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  /**
   * Get the combined path. If output is a file, it is returned as is. If output is a directory, the
   * input filename is added to the output path.
   *
   * @param input The input filename
   * @param output The output path
   * @return The combined output path
   */
  public static Path getOutputPath(Path input, Path output) {
    return Files.isDirectory(output) ? output.resolve(input.getFileName()) : output;
  }

  /**
   * Add a file index to the file name. The index number is added before the file extension.
   *
   * @param path the file path
   * @param index the index to add to the filename
   * @param indexSize the minimal number of digits of the index (0 padding)
   * @return the new path
   */
  public static Path addFileIndex(Path path, int index, int indexSize) {
    if (indexSize < 1) {
      return path;
    }
    String pattern = "$1-%0" + indexSize + "d$2";
    String insert = String.format(pattern, index);
    return path.resolveSibling(
        path.getFileName().toString().replaceFirst("(.*?)(\\.[^.]+)?$", insert));
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
