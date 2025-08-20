/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.data;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.opencv.core.CvException;
import org.weasis.core.util.StringUtil;
import org.weasis.opencv.op.ImageIOHandler;

/**
 * Represents a wrapper for a file containing raw image data (not compressed with a specific
 * header), providing methods to read and write image data to and from the file.
 *
 * <p>This record provides a type-safe way to handle raw image files with validation and error
 * handling capabilities using modern Java NIO Path API.
 */
public record FileRawImage(Path path) {

  public static final int HEADER_LENGTH = 46;

  /**
   * Creates a new FileRawImage instance.
   *
   * @param path the path to the file containing raw image data, must not be null
   * @throws NullPointerException if path is null
   */
  public FileRawImage {
    Objects.requireNonNull(path, "Path cannot be null");
  }

  /**
   * Factory method to create a FileRawImage from a File.
   *
   * @param file the file containing image data
   * @return a new FileRawImage instance
   * @throws NullPointerException if file is null
   */
  public static FileRawImage of(File file) {
    Objects.requireNonNull(file, "File cannot be null");
    return new FileRawImage(file.toPath());
  }

  /**
   * Factory method to create a FileRawImage from a file path string.
   *
   * @param filePath the path to the image file as string
   * @return a new FileRawImage instance
   * @throws IllegalArgumentException if filePath is null or empty
   */
  public static FileRawImage of(String filePath) {
    if (!StringUtil.hasText(filePath)) {
      throw new IllegalArgumentException("File path cannot be null or empty");
    }
    return new FileRawImage(Path.of(filePath));
  }

  /**
   * Gets the File representation of the path for compatibility with legacy APIs.
   *
   * @return the File object
   */
  public File file() {
    return path.toFile();
  }

  /**
   * Reads the image from the file.
   *
   * @return the image data as ImageCV
   * @throws CvException if the image cannot be read
   * @throws IllegalStateException if the file is not readable
   */
  public ImageCV read() {
    return ImageIOHandler.readImageWithCvException(path, null);
  }

  /**
   * Safely reads the image from the file, returning an Optional.
   *
   * @return an Optional containing the image if successfully read, empty otherwise
   */
  public Optional<ImageCV> readSafely() {
    try {
      return Optional.ofNullable(ImageIOHandler.readImageWithCvException(path, null));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Writes the image to the file.
   *
   * @param image the image data to write, must not be null
   * @return true if the image was successfully written, false otherwise
   * @throws NullPointerException if image is null
   */
  public boolean write(PlanarImage image) {
    Objects.requireNonNull(image, "Image cannot be null");
    return ImageIOHandler.writeImage(image.toMat(), path);
  }

  @Override
  public String toString() {
    return "FileRawImage[path=" + path + "]";
  }
}
