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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.imageio.stream.ImageInputStream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for stream operations and safe resource management.
 *
 * <p>This class provides methods for safely closing resources and copying streams with proper
 * exception handling and logging.
 *
 * @author Weasis Team
 */
public final class StreamUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(StreamUtil.class);

  public static final int DEFAULT_BUFFER_SIZE = 8192;

  private StreamUtil() {
    // Prevent instantiation
  }

  /**
   * Safely close any AutoCloseable resource, logging any exceptions that occur. This method handles
   * null values gracefully.
   *
   * @param resource the resource to close (can be null)
   */
  public static void safeClose(AutoCloseable resource) {
    if (resource != null) {
      try {
        resource.close();
      } catch (Exception e) {
        LOGGER.warn("Failed to close resource: {}", resource.getClass().getSimpleName(), e);
      }
    }
  }

  /**
   * Safely close multiple AutoCloseable resources in sequence. Each resource is closed
   * independently, so failure to close one resource doesn't prevent closing others.
   *
   * @param resources the resources to close (can contain null values)
   */
  public static void safeClose(AutoCloseable... resources) {
    if (resources != null) {
      for (AutoCloseable resource : resources) {
        safeClose(resource);
      }
    }
  }

  /**
   * Safely close an XMLStreamWriter, logging any XMLStreamException that occurs. This method
   * handles null values gracefully.
   *
   * @param writer the XMLStreamWriter to close (can be null)
   */
  public static void safeClose(XMLStreamWriter writer) {
    if (writer != null) {
      try {
        writer.close();
      } catch (XMLStreamException e) {
        LOGGER.warn("Failed to close XMLStreamWriter", e);
      }
    }
  }

  /**
   * Safely close an XMLStreamReader, logging any XMLStreamException that occurs. This method
   * handles null values gracefully.
   *
   * @param reader the XMLStreamReader to close (can be null)
   */
  public static void safeClose(XMLStreamReader reader) {
    if (reader != null) {
      try {
        reader.close();
      } catch (XMLStreamException e) {
        LOGGER.warn("Failed to close XMLStreamReader", e);
      }
    }
  }

  /**
   * Copy all data from an InputStream to an OutputStream. Neither stream is closed by this method.
   *
   * @param input the source InputStream
   * @param output the target OutputStream
   * @return the number of bytes copied
   * @throws IOException if an I/O error occurs
   */
  public static long copy(InputStream input, OutputStream output) throws IOException {
    return copy(input, output, DEFAULT_BUFFER_SIZE);
  }

  /**
   * Copy all data from an InputStream to an OutputStream using a specified buffer size. Neither
   * stream is closed by this method.
   *
   * @param input the source InputStream
   * @param output the target OutputStream
   * @param bufferSize the buffer size to use for copying
   * @return the number of bytes copied
   * @throws IOException if an I/O error occurs
   */
  public static long copy(InputStream input, OutputStream output, int bufferSize)
      throws IOException {
    if (input == null || output == null) {
      throw new IllegalArgumentException("Input and output streams cannot be null");
    }

    if (bufferSize <= 0) {
      bufferSize = DEFAULT_BUFFER_SIZE;
    }

    byte[] buffer = new byte[bufferSize];
    long totalBytes = 0;
    int bytesRead;

    while ((bytesRead = input.read(buffer)) != -1) {
      output.write(buffer, 0, bytesRead);
      totalBytes += bytesRead;
    }

    output.flush();
    return totalBytes;
  }

  /**
   * Copy a file from one path to another using NIO. This method replaces the destination file if it
   * already exists.
   *
   * @param source the source file path
   * @param destination the target file path
   * @return true if the copy was successful, false otherwise
   */
  public static boolean copyFile(Path source, Path destination) {
    if (source == null || destination == null) {
      return false;
    }
    try {
      Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch (Exception e) {
      LOGGER.error("Copy file", e);
      return false;
    }
  }

  /**
   * Copy an InputStream to a file using NIO, creating parent directories if necessary. The
   * InputStream is not closed by this method.
   *
   * @param input the source InputStream
   * @param target the target file path
   * @return true if the copy was successful, false otherwise
   */
  public static boolean copyToFile(InputStream input, Path target) {
    if (input == null || target == null) {
      LOGGER.warn("Input stream or target path is null");
      return false;
    }

    try {
      FileUtil.prepareToWriteFile(target);

      Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch (Exception e) {
      LOGGER.error("Failed to copy stream to file: {}", target, e);
      return false;
    }
  }

  /**
   * Copy an InputStream to a file and close the InputStream.
   *
   * @param input the source InputStream (will be closed)
   * @param target the target file path
   * @return true if the copy was successful, false otherwise
   */
  public static boolean copyToFileAndClose(InputStream input, Path target) {
    try {
      return copyToFile(input, target);
    } finally {
      safeClose(input);
    }
  }

  /**
   * Copy an ImageInputStream to a file and close the ImageInputStream.
   *
   * @param input the source ImageInputStream (will be closed)
   * @param target the target file path
   * @return true if the copy was successful, false otherwise
   */
  public static boolean copyToFileAndClose(ImageInputStream input, Path target) {
    if (input == null || target == null) {
      LOGGER.warn("Input stream or target path is null");
      return false;
    }

    try {
      FileUtil.prepareToWriteFile(target);
      try (OutputStream output = Files.newOutputStream(target)) {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
          output.write(buffer, 0, bytesRead);
        }
        output.flush();
        return true;
      }
    } catch (Exception e) {
      LOGGER.error("Failed to copy ImageInputStream to file: {}", target, e);
      return false;
    } finally {
      safeClose(input);
    }
  }

  /**
   * Copy data from an InputStream to an OutputStream, safely closing both streams.
   *
   * @param input the source InputStream (will be closed)
   * @param output the target OutputStream (will be closed)
   * @return the number of bytes copied, or -1 if an error occurred
   */
  public static long copyAndClose(InputStream input, OutputStream output) {
    try {
      return copy(input, output);
    } catch (Exception e) {
      LOGGER.error("Failed to copy streams", e);
      return -1;
    } finally {
      safeClose(input, output);
    }
  }

  /**
   * Copy data from an InputStream to an OutputStream using NIO channels with a default buffer size.
   * Neither stream is closed by this method.
   *
   * @param input the source InputStream
   * @param output the target OutputStream
   * @return true if the copy was successful, false otherwise
   */
  public static boolean copyWithNIO(InputStream input, OutputStream output) {
    return copyWithNIO(input, output, DEFAULT_BUFFER_SIZE);
  }

  /**
   * Copy data from an InputStream to an OutputStream using NIO channels with a custom buffer size.
   * Neither stream is closed by this method.
   *
   * @param input the source InputStream
   * @param output the target OutputStream
   * @param bufferSize the buffer size for NIO operations
   * @return true if the copy was successful, false otherwise
   */
  public static boolean copyWithNIO(InputStream input, OutputStream output, int bufferSize) {
    if (input == null || output == null) {
      LOGGER.warn("Input or output stream is null");
      return false;
    }

    if (bufferSize <= 0) {
      bufferSize = DEFAULT_BUFFER_SIZE;
    }

    ReadableByteChannel readChannel = null;
    WritableByteChannel writeChannel = null;
    try {
      readChannel = Channels.newChannel(input);
      writeChannel = Channels.newChannel(output);

      ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
      while (readChannel.read(buffer) != -1) {
        buffer.flip();
        writeChannel.write(buffer);
        buffer.clear();
      }
      return true;
    } catch (Exception e) {
      LOGGER.error("Failed to copy streams using NIO", e);
      return false;
    } finally {
      // Close channels independently to ensure both are closed even if one fails
      safeClose(readChannel);
      safeClose(writeChannel);
    }
  }

  /**
   * Copy data from an InputStream to an OutputStream using NIO channels and close both streams.
   *
   * @param input the source InputStream (will be closed)
   * @param output the target OutputStream (will be closed)
   * @param bufferSize the buffer size for NIO operations
   * @return true if the copy was successful, false otherwise
   */
  public static boolean copyWithNIOAndClose(
      InputStream input, OutputStream output, int bufferSize) {
    try {
      return copyWithNIO(input, output, bufferSize);
    } finally {
      safeClose(input, output);
    }
  }
}
