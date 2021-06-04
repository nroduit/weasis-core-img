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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

/** @author Nicolas Roduit */
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
   * Transform a string into a writable string for all the operating system. All the special and
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
            || (c < '\u0020') // ctrls
            || (c > '\u007e' && c < '\u00a0'))) { // ctrls
          cleanName.append(c);
        }
      }
    }
    return cleanName.toString().trim();
  }

  public static String getValidFileNameWithoutHTML(String fileName) {
    String val = null;
    if (fileName != null) {
      // Force to remove html tags
      val = fileName.replaceAll("\\<.*?>", "");
    }
    return getValidFileName(val);
  }

  public static void safeClose(final AutoCloseable object) {
    if (object != null) {
      try {
        object.close();
      } catch (Exception e) {
        LOGGER.error("Cannot close AutoCloseable", e);
      }
    }
  }

  public static File createTempDir(File baseDir) {
    if (baseDir != null) {
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

  public static void getAllFilesInDirectory(File directory, List<File> files) {
    getAllFilesInDirectory(directory, files, true);
  }

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

  private static boolean deleteFile(File fileOrDirectory) {
    try {
      Files.delete(fileOrDirectory.toPath());
    } catch (Exception e) {
      LOGGER.error(CANNOT_DELETE, e);
      return false;
    }
    return true;
  }

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

  public static void recursiveDelete(File rootDir) {
    recursiveDelete(rootDir, true);
  }

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

  public static void safeClose(XMLStreamWriter writer) {
    if (writer != null) {
      try {
        writer.close();
      } catch (XMLStreamException e) {
        LOGGER.error("Cannot close XMLStreamWriter", e);
      }
    }
  }

  public static void safeClose(XMLStreamReader xmler) {
    if (xmler != null) {
      try {
        xmler.close();
      } catch (XMLStreamException e) {
        LOGGER.error("Cannot close XMLStreamException", e);
      }
    }
  }

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

  public static String nameWithoutExtension(String fn) {
    if (fn == null) {
      return null;
    }
    int i = fn.lastIndexOf('.');
    if (i > 0) {
      return fn.substring(0, i);
    }
    return fn;
  }

  public static String getExtension(String fn) {
    if (fn == null) {
      return "";
    }
    int i = fn.lastIndexOf('.');
    if (i > 0) {
      return fn.substring(i);
    }
    return "";
  }

  public static boolean isFileExtensionMatching(File file, String[] extensions) {
    if (file != null && extensions != null) {
      String fileExt = getExtension(file.getName()).toLowerCase();
      if (StringUtil.hasLength(fileExt)) {
        for (String extension : extensions) {
          if (fileExt.endsWith(extension)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Write inputStream content into a file
   *
   * @param inputStream
   * @param outFile
   * @throws StreamIOException
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
   * @param inputStream
   * @param outFile
   * @return bytes transferred. O = error, -1 = all bytes has been transferred, other = bytes
   *     transferred before interruption
   * @throws StreamIOException
   */
  public static int writeStream(InputStream inputStream, File outFile) throws StreamIOException {
    return writeStream(inputStream, outFile, true);
  }

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

  public static int writeFile(ImageInputStream inputStream, File outFile) throws StreamIOException {
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
      LOGGER.error("Interruption when writing image {}", e.getMessage());
      return e.bytesTransferred;
    } catch (IOException e) {
      FileUtil.delete(outFile);
      throw new StreamIOException(e);
    } finally {
      FileUtil.safeClose(inputStream);
    }
  }

  // From: https://programming.guide/worlds-most-copied-so-snippet.html
  public static String humanReadableByte(long bytes, boolean si) {
    int unit = si ? 1000 : 1024;
    long absBytes = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
    if (absBytes < unit) return bytes + " B";
    int exp = (int) (Math.log(absBytes) / Math.log(unit));
    long th = (long) (Math.pow(unit, exp) * (unit - 0.05));
    if (exp < 6 && absBytes >= th - ((th & 0xfff) == 0xd00 ? 52 : 0)) exp++;
    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
    if (exp > 4) {
      bytes /= unit;
      exp -= 1;
    }
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }

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

  public static boolean nioWriteFile(InputStream in, OutputStream out, final int bufferSize) {
    if (in == null || out == null) {
      return false;
    }
    try (ReadableByteChannel readChannel = Channels.newChannel(in);
        WritableByteChannel writeChannel = Channels.newChannel(out)) {

      ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

      while (readChannel.read(buffer) != -1) {
        LangUtil.safeBufferType(buffer).flip();
        writeChannel.write(buffer);
        LangUtil.safeBufferType(buffer).clear();
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

  public static void storeProperties(File propsFile, Properties props, String comments) {
    if (props != null && propsFile != null) {
      try (FileOutputStream fout = new FileOutputStream(propsFile)) {
        props.store(fout, comments);
      } catch (IOException e) {
        LOGGER.error("Error when writing properties", e);
      }
    }
  }

  public static void zip(File directory, File zipfile) throws IOException {
    if (zipfile == null || directory == null) {
      return;
    }
    URI base = directory.toURI();
    Deque<File> queue = new LinkedList<>();
    queue.push(directory);

    // The resources will be closed in reverse order of the order in which they are created in
    // try().
    // Zip stream must be close before out stream.
    try (OutputStream out = new FileOutputStream(zipfile);
        ZipOutputStream zout = new ZipOutputStream(out)) {
      while (!queue.isEmpty()) {
        File dir = queue.pop();
        File[] files = dir.listFiles();
        if (files != null) {
          for (File entry : files) {
            String name = base.relativize(entry.toURI()).getPath();
            if (entry.isDirectory()) {
              queue.push(entry);
              String[] flist = entry.list();
              if (flist == null || flist.length == 0) {
                name = name.endsWith("/") ? name : name + "/";
                zout.putNextEntry(new ZipEntry(name));
              }
            } else {
              zout.putNextEntry(new ZipEntry(name));
              copyZip(entry, zout);
              zout.closeEntry();
            }
          }
        }
      }
    }
  }

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

  public static void unzip(File zipfile, File directory) throws IOException {
    if (zipfile == null || directory == null) {
      return;
    }
    try (ZipFile zfile = new ZipFile(zipfile)) {
      String canonicalDirPath = directory.getCanonicalPath();
      Enumeration<? extends ZipEntry> entries = zfile.entries();
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
          try (InputStream in = zfile.getInputStream(entry)) {
            copyZip(in, file);
          }
        }
      }
    }
  }

  private static void copy(InputStream in, OutputStream out) throws IOException {
    if (in == null || out == null) {
      return;
    }
    byte[] buf = new byte[FILE_BUFFER];
    int offset;
    while ((offset = in.read(buf)) > 0) {
      out.write(buf, 0, offset);
    }
    out.flush();
  }

  private static void copyZip(File file, OutputStream out) throws IOException {
    try (InputStream in = new FileInputStream(file)) {
      copy(in, out);
    }
  }

  private static void copyZip(InputStream in, File file) throws IOException {
    try (OutputStream out = new FileOutputStream(file)) {
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

  private static boolean deleteFile(Path path) {
    try {
      return Files.deleteIfExists(path);
    } catch (IOException e) {
      LOGGER.error(CANNOT_DELETE, e);
    }
    return false;
  }

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

  public static Path getOutputPath(Path src, Path dst) {
    if (Files.isDirectory(dst)) {
      return FileSystems.getDefault().getPath(dst.toString(), src.getFileName().toString());
    } else {
      return dst;
    }
  }

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
