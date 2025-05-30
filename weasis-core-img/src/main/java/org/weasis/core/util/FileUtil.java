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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nicolas Roduit
 */
public final class FileUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

  public static final int FILE_BUFFER = 4096;
  private static final int[] ILLEGAL_CHARS = {
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
    26, 27, 28, 29, 30, 31, 34, 42, 47, 58, 60, 62, 63, 92, 124
  };
  public static final String CANNOT_DELETE = "Cannot delete";

  private FileUtil() {}

  /**
   * Transform a fileName into a writable fileName for all the operating system. All the special and
   * control characters are excluded.
   *
   * @param fileName a filename or directory name
   * @return a writable filename
   */
  public static String getValidFileName(String fileName) {
    StringBuilder cleanName = new StringBuilder();
    if (fileName != null) {
      for (int i = 0; i < fileName.length(); i++) {
        char c = fileName.charAt(i);
        if (!(Arrays.binarySearch(ILLEGAL_CHARS, c) >= 0
            || (c < ' ') // control characters
            || (c > '~' && c < '\u00a0'))) { // control characters
          cleanName.append(c);
        }
      }
    }
    return cleanName.toString().trim();
  }

  /**
   * Transform a fileName into a fileName for all the operating system. All the special and control
   * characters are excluded. HTML or XML tags are also removed.
   *
   * @param fileName a file name
   * @return a writable file name
   */
  public static String getValidFileNameWithoutHTML(String fileName) {
    String val = null;
    if (fileName != null) {
      // Force to remove html tags
      val = fileName.replaceAll("<[^>]*>", "");
    }
    return getValidFileName(val);
  }

  /**
   * Close an AutoCloseable object and log the exception if any.
   *
   * @param autoCloseable the object to close
   */
  public static void safeClose(final AutoCloseable autoCloseable) {
    if (autoCloseable != null) {
      try {
        autoCloseable.close();
      } catch (Exception e) {
        LOGGER.error("Cannot close AutoCloseable", e);
      }
    }
  }

  /**
   * Create a unique temporary directory in the specified directory.
   *
   * @param baseDir the base directory where the temporary directory is created
   * @return the temporary directory
   */
  public static File createTempDir(File baseDir) {
    if (baseDir != null && baseDir.isDirectory()) {
      String baseName = String.valueOf(System.currentTimeMillis());
      for (int counter = 0; counter < 1000; counter++) {
        File tempDir = new File(baseDir, baseName + counter);
        if (tempDir.mkdir()) {
          return tempDir;
        }
      }
    }
    throw new IllegalStateException("Failed to create directory");
  }

  /**
   * Delete the content of a directory and the directory itself if the level is reached.
   *
   * @param dir the directory
   * @param deleteDirLevel the level of subdirectories to delete
   * @param level the current level
   */
  public static void deleteDirectoryContents(final File dir, int deleteDirLevel, int level) {
    if ((dir == null) || !dir.isDirectory()) {
      return;
    }
    final File[] files = dir.listFiles();
    if (files != null) {
      for (final File f : files) {
        if (f.isDirectory()) {
          deleteDirectoryContents(f, deleteDirLevel, level + 1);
        } else {
          deleteFile(f);
        }
      }
    }
    if (level >= deleteDirLevel) {
      deleteFile(dir);
    }
  }

  /**
   * Get all files in a directory and its subdirectories.
   *
   * @param directory the directory
   * @param files the list of files to fill
   */
  public static void getAllFilesInDirectory(File directory, List<File> files) {
    getAllFilesInDirectory(directory, files, true);
  }

  /**
   * Get all files in a directory.
   *
   * @param directory the directory
   * @param files the list of files to fill
   * @param recursive true to get files in subdirectories
   */
  public static void getAllFilesInDirectory(File directory, List<File> files, boolean recursive) {
    File[] fList = directory.listFiles();
    if (fList != null) {
      for (File f : fList) {
        if (f.isFile()) {
          files.add(f);
        } else if (recursive && f.isDirectory()) {
          getAllFilesInDirectory(f, files, true);
        }
      }
    }
  }

  static boolean deleteFile(File fileOrDirectory) {
    try {
      Files.delete(fileOrDirectory.toPath());
    } catch (Exception e) {
      LOGGER.error(CANNOT_DELETE, e);
      return false;
    }
    return true;
  }

  /**
   * Delete a file or a directory and all its content.
   *
   * @param fileOrDirectory the file or directory to delete
   * @return true if the file or directory is successfully deleted; false otherwise
   */
  public static boolean delete(File fileOrDirectory) {
    if (fileOrDirectory == null || !fileOrDirectory.exists()) {
      return false;
    }

    if (fileOrDirectory.isDirectory()) {
      final File[] files = fileOrDirectory.listFiles();
      if (files != null) {
        for (File child : files) {
          delete(child);
        }
      }
    }
    return deleteFile(fileOrDirectory);
  }

  /**
   * Delete all files and subdirectories of a specific folder.
   *
   * @param rootDir the root directory to delete
   */
  public static void recursiveDelete(File rootDir) {
    recursiveDelete(rootDir, true);
  }

  /**
   * Delete all files and subdirectories of a specific folder.
   *
   * @param rootDir the root directory to delete
   * @param deleteRoot true to delete the root directory at the end and false to keep it
   */
  public static void recursiveDelete(File rootDir, boolean deleteRoot) {
    if ((rootDir == null) || !rootDir.isDirectory()) {
      return;
    }
    File[] childDirs = rootDir.listFiles();
    if (childDirs != null) {
      for (File f : childDirs) {
        if (f.isDirectory()) {
          // deleteRoot used only for the first level, directory is deleted in next line
          recursiveDelete(f, false);
          deleteFile(f);
        } else {
          deleteFile(f);
        }
      }
    }
    if (deleteRoot) {
      deleteFile(rootDir);
    }
  }

  /**
   * Close an XMLStreamWriter object and log the exception if any.
   *
   * @param writer the object to close
   */
  public static void safeClose(XMLStreamWriter writer) {
    if (writer != null) {
      try {
        writer.close();
      } catch (XMLStreamException e) {
        LOGGER.error("Cannot close XMLStreamWriter", e);
      }
    }
  }

  /**
   * Close an XMLStreamReader object and log the exception if any.
   *
   * @param reader the object to close
   */
  public static void safeClose(XMLStreamReader reader) {
    if (reader != null) {
      try {
        reader.close();
      } catch (XMLStreamException e) {
        LOGGER.error("Cannot close XMLStreamException", e);
      }
    }
  }

  /**
   * Prepare a file to be written by creating the parent directory if necessary.
   *
   * @param file the source file
   * @throws IOException if an I/O error occurs
   */
  public static void prepareToWriteFile(File file) throws IOException {
    if (!file.exists()) {
      // Check the file that doesn't exist yet.
      // Create a new file. The file is writable if the creation succeeds.
      File outputDir = file.getParentFile();
      // necessary to check exists otherwise mkdirs() is false when dir exists
      if (outputDir != null && !outputDir.exists() && !outputDir.mkdirs()) {
        throw new IOException("Cannot write parent directory of " + file.getPath());
      }
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
    int i = filename.lastIndexOf('.');
    if (i > 0) {
      return filename.substring(0, i);
    }
    return filename;
  }

  /**
   * Get the extension of a file name, like ".png" or ".jpg".
   *
   * @param filename The file name to retrieve the extension of.
   * @return The extension of the file name starting by '.' or empty string if none.
   */
  public static String getExtension(String filename) {
    if (filename == null) {
      return "";
    }
    int i = filename.lastIndexOf('.');
    if (i > 0) {
      return filename.substring(i);
    }
    return "";
  }

  /**
   * Check if the extension of the file name is matching one of the extensions in the array. The
   * extension can start with '.' or not and is not case-sensitive.
   *
   * @param file the file
   * @param extensions the extensions array, e.g. {"png", "jpg"} or {".png", ".jpg"}
   * @return The extension of the file name without '.' or empty string if none.
   */
  public static boolean isFileExtensionMatching(File file, String[] extensions) {
    if (file == null || extensions == null) return false;

    String fExt = getExtension(file.getName());
    if (!StringUtil.hasLength(fExt)) return false;

    return Arrays.stream(extensions).anyMatch(ext -> matchesExtension(fExt, ext));
  }

  private static boolean matchesExtension(String fExt, String ext) {
    if (!StringUtil.hasText(ext)) return false;
    String nExt = ext.startsWith(".") ? ext : "." + ext;
    return fExt.equalsIgnoreCase(nExt);
  }


  /**
   * Write inputStream content into a file
   *
   * @param inputStream the input stream
   * @param outFile the output file
   * @throws StreamIOException if an I/O error occurs
   */
  public static void writeStreamWithIOException(InputStream inputStream, File outFile)
      throws StreamIOException {
    try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
      byte[] buf = new byte[FILE_BUFFER];
      int offset;
      while ((offset = inputStream.read(buf)) > 0) {
        outputStream.write(buf, 0, offset);
      }
      outputStream.flush();
    } catch (IOException e) {
      FileUtil.delete(outFile);
      throw new StreamIOException(e);
    } finally {
      FileUtil.safeClose(inputStream);
    }
  }

  /**
   * Write inputStream content into a file
   *
   * @param inputStream the input stream
   * @param outFile the output file
   * @return the number of written bytes. O = error, -1 = all bytes has been written, other = bytes
   *     written before interruption
   * @throws StreamIOException if an I/O error occurs
   */
  public static int writeStream(InputStream inputStream, File outFile) throws StreamIOException {
    return writeStream(inputStream, outFile, true);
  }

  /**
   * Write inputStream content into a file and close the input stream if closeInputStream is true.
   *
   * @param inputStream the input stream
   * @param outFile the output
   * @param closeInputStream true to close the input stream
   * @return the number of written bytes. O = error, -1 = all bytes has been written, other = bytes
   *     written before interruption
   * @throws StreamIOException if an I/O error occurs
   */
  public static int writeStream(InputStream inputStream, File outFile, boolean closeInputStream)
      throws StreamIOException {
    try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
      byte[] buf = new byte[FILE_BUFFER];
      int offset;
      while ((offset = inputStream.read(buf)) > 0) {
        outputStream.write(buf, 0, offset);
      }
      outputStream.flush();
      return -1;
    } catch (SocketTimeoutException e) {
      FileUtil.delete(outFile);
      throw new StreamIOException(e);
    } catch (InterruptedIOException e) {
      FileUtil.delete(outFile);
      // Specific for SeriesProgressMonitor
      LOGGER.error("Interruption when writing file: {}", e.getMessage());
      return e.bytesTransferred;
    } catch (IOException e) {
      FileUtil.delete(outFile);
      throw new StreamIOException(e);
    } finally {
      if (closeInputStream) {
        FileUtil.safeClose(inputStream);
      }
    }
  }

  /**
   * Write inputStream content into a file
   *
   * @param imageInputStream the input stream
   * @param outFile the output file
   * @return the number of written bytes. O = error, -1 = all bytes has been written, other = bytes
   *     written before interruption
   * @throws StreamIOException if an I/O error occurs
   */
  public static int writeFile(ImageInputStream imageInputStream, File outFile)
      throws StreamIOException {
    try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
      byte[] buf = new byte[FILE_BUFFER];
      int offset;
      while ((offset = imageInputStream.read(buf)) > 0) {
        outputStream.write(buf, 0, offset);
      }
      outputStream.flush();
      return -1;
    } catch (SocketTimeoutException e) {
      FileUtil.delete(outFile);
      throw new StreamIOException(e);
    } catch (InterruptedIOException e) {
      FileUtil.delete(outFile);
      // Specific for SeriesProgressMonitor
      LOGGER.error("Interruption when writing image {}", e.getMessage());
      return e.bytesTransferred;
    } catch (IOException e) {
      FileUtil.delete(outFile);
      throw new StreamIOException(e);
    } finally {
      FileUtil.safeClose(imageInputStream);
    }
  }

  /**
   * Print a byte count in a human-readable format
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
   * Write inputStream content into a <code>FileOutputStream</code>
   *
   * @param inputStream the input stream
   * @param out the output stream
   * @return true if the copy is successful
   */
  public static boolean nioWriteFile(FileInputStream inputStream, FileOutputStream out) {
    if (inputStream == null || out == null) {
      return false;
    }
    try (FileChannel fci = inputStream.getChannel();
        FileChannel fco = out.getChannel()) {
      fco.transferFrom(fci, 0, fci.size());
      return true;
    } catch (Exception e) {
      LOGGER.error("Write file", e);
      return false;
    } finally {
      FileUtil.safeClose(inputStream);
      FileUtil.safeClose(out);
    }
  }

  /**
   * Write inputStream content into a <code>OutputStream</code> with a specific buffer size
   *
   * @param in the input stream
   * @param out the output stream
   * @param bufferSize the buffer size when reading the input stream
   * @return true if the copy is successful
   */
  public static boolean nioWriteFile(InputStream in, OutputStream out, final int bufferSize) {
    if (in == null || out == null) {
      return false;
    }
    try (ReadableByteChannel readChannel = Channels.newChannel(in);
        WritableByteChannel writeChannel = Channels.newChannel(out)) {

      ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

      while (readChannel.read(buffer) != -1) {
        buffer.flip();
        writeChannel.write(buffer);
        buffer.clear();
      }
      return true;
    } catch (IOException e) {
      LOGGER.error("Write file", e);
      return false;
    } finally {
      FileUtil.safeClose(in);
      FileUtil.safeClose(out);
    }
  }

  /**
   * Write the source file into the destination file
   *
   * @param source the source file
   * @param destination the destination file
   * @return true if the copy is successful
   */
  public static boolean nioCopyFile(File source, File destination) {
    if (source == null || destination == null) {
      return false;
    }
    try {
      Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch (Exception e) {
      LOGGER.error("Copy file", e);
      return false;
    }
  }

  /**
   * Read the content of a file into <code>Properties</code>
   *
   * @param propsFile the <code>Properties</code> file
   * @param props the properties to copy in. If null, a new <code>Properties</code> is created
   * @return the <code>Properties</code> from the file
   */
  public static Properties readProperties(File propsFile, Properties props) {
    Properties p = props == null ? new Properties() : props;
    if (propsFile != null && propsFile.canRead()) {
      try (FileInputStream fileStream = new FileInputStream(propsFile)) {
        p.load(fileStream);
      } catch (IOException e) {
        LOGGER.error("Error when reading properties", e);
      }
    }
    return p;
  }

  /**
   * Write the content of <code>Properties</code> into a file
   *
   * @param propsFile the properties file
   * @param props the properties to copy in. If null, a new Properties is created
   * @param comments the comments to write at the beginning of the file
   */
  public static void storeProperties(File propsFile, Properties props, String comments) {
    if (props != null && propsFile != null) {
      try (FileOutputStream stream = new FileOutputStream(propsFile)) {
        props.store(stream, comments);
      } catch (IOException e) {
        LOGGER.error("Error when writing properties", e);
      }
    }
  }

  /**
   * Create a zip file from a directory.
   *
   * @param directory the directory to zip.
   * @param zipFile the zip file to create.
   * @throws IOException if an I/O error occurs.
   */
  public static void zip(File directory, File zipFile) throws IOException {
    if (zipFile == null || directory == null) return;

    URI base = directory.toURI();
    Deque<File> dirQ = new LinkedList<>();
    dirQ.push(directory);

    try (OutputStream out = Files.newOutputStream(zipFile.toPath());
        ZipOutputStream zipOut = new ZipOutputStream(out)) {

      while (!dirQ.isEmpty()) {
        File dir = dirQ.pop();
        File[] files = dir.listFiles();

        if (files != null) {
          for (File file : files) {
            processFile(file, base, dirQ, zipOut);
          }
        }
      }
    }
  }

  private static void processFile(File file, URI base, Deque<File> dirQ, ZipOutputStream zipOut) throws IOException {
    String name = base.relativize(file.toURI()).getPath();
    if (file.isDirectory()) {
      dirQ.push(file); // Add a subdirectory to the queue
      String[] list = file.list();
      if (list == null || list.length == 0) {
        name = name.endsWith("/") ? name : name + "/";
        zipOut.putNextEntry(new ZipEntry(name));
        zipOut.closeEntry();
      }
    } else {
      zipOut.putNextEntry(new ZipEntry(name));
      copyZip(file, zipOut);
      zipOut.closeEntry();
    }
  }


  /**
   * Unzip a zip inputStream into a directory
   *
   * @param inputStream the zip input stream
   * @param directory the directory to unzip all files
   * @throws IOException if an I/O error occurs
   */
  public static void unzip(InputStream inputStream, File directory) throws IOException {
    if (inputStream == null || directory == null) {
      return;
    }

    try (BufferedInputStream bufInStream = new BufferedInputStream(inputStream);
        ZipInputStream zis = new ZipInputStream(bufInStream)) {
      String canonicalDirPath = directory.getCanonicalPath();
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        File file = new File(directory, entry.getName());
        String canonicalDestPath = file.getCanonicalPath();
        if (!canonicalDestPath.startsWith(canonicalDirPath + File.separator)) { // Sanitizer
          throw new IOException(
              "Security issue: Entry is trying to leave the target dir: " + entry.getName());
        }
        if (entry.isDirectory()) {
          file.mkdirs();
        } else {
          file.getParentFile().mkdirs();
          copyZip(zis, file);
        }
      }
    } finally {
      FileUtil.safeClose(inputStream);
    }
  }

  /**
   * Unzip a zip file into a directory
   *
   * @param zipFile the zip file to unzip
   * @param directory the directory to unzip all files
   * @throws IOException if an I/O error occurs
   */
  public static void unzip(File zipFile, File directory) throws IOException {
    if (zipFile == null || directory == null) {
      return;
    }
    try (ZipFile zFile = new ZipFile(zipFile)) {
      String canonicalDirPath = directory.getCanonicalPath();
      Enumeration<? extends ZipEntry> entries = zFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        File file = new File(directory, entry.getName());
        String canonicalDestPath = file.getCanonicalPath();
        if (!canonicalDestPath.startsWith(canonicalDirPath + File.separator)) { // Sanitizer
          throw new IOException(
              "Security issue: Entry is trying to leave the target dir: " + entry.getName());
        }
        if (entry.isDirectory()) {
          file.mkdirs();
        } else {
          file.getParentFile().mkdirs();
          try (InputStream in = zFile.getInputStream(entry)) {
            copyZip(in, file);
          }
        }
      }
    }
  }

  private static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] buf = new byte[FILE_BUFFER];
    int offset;
    while ((offset = in.read(buf)) > 0) {
      out.write(buf, 0, offset);
    }
    out.flush();
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

  private static void copyZip(File file, OutputStream out) throws IOException {
    try (InputStream in = Files.newInputStream(file.toPath())) {
      copy(in, out);
    }
  }

  private static void copyZip(InputStream in, File file) throws IOException {
    try (OutputStream out = Files.newOutputStream(file.toPath())) {
      copy(in, out);
    }
  }

  public static boolean writeStream(
      InputStream inputStream, Path outFile, boolean closeInputStream) {
    try (OutputStream outputStream = Files.newOutputStream(outFile)) {
      byte[] buf = new byte[FILE_BUFFER];
      int offset;
      while ((offset = inputStream.read(buf)) > 0) {
        outputStream.write(buf, 0, offset);
      }
      outputStream.flush();
      return true;
    } catch (IOException e) {
      FileUtil.delete(outFile);
      LOGGER.error("Writing file: {}", outFile, e);
      return false;
    } finally {
      if (closeInputStream) {
        FileUtil.safeClose(inputStream);
      }
    }
  }

  static boolean deleteFile(Path path) {
    try {
      return Files.deleteIfExists(path);
    } catch (IOException e) {
      LOGGER.error(CANNOT_DELETE, e);
    }
    return false;
  }

  /**
   * Delete a file or a directory and all its content.
   *
   * @param fileOrDirectory the file or directory to delete
   * @return true if the file or directory is successfully deleted; false otherwise
   */
  public static boolean delete(Path fileOrDirectory) {
    if (!Files.isDirectory(fileOrDirectory)) {
      return deleteFile(fileOrDirectory);
    }

    try (Stream<Path> walk = Files.walk(fileOrDirectory)) {
      walk.sorted(Comparator.reverseOrder()).forEach(FileUtil::deleteFile);
      return true;
    } catch (IOException e) {
      LOGGER.error(CANNOT_DELETE, e);
    }
    return false;
  }

  /**
   * Get the combined path. If output is a file, it is returned as is. If output is a directory, the
   * input filename is added to the output path.
   *
   * @param input The input filename
   * @param output The output path
   * @return The name of the file without extension.
   */
  public static Path getOutputPath(Path input, Path output) {
    if (Files.isDirectory(output)) {
      return FileSystems.getDefault().getPath(output.toString(), input.getFileName().toString());
    } else {
      return output;
    }
  }

  /**
   * Add a file index to the file name. The index number is added before the file extension.
   *
   * @param path the file path
   * @param index the index to add the filename
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
}
