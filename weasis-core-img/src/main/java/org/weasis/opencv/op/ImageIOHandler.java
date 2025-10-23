/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op;

import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.util.FileUtil;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.MetadataParser;
import org.weasis.opencv.data.PlanarImage;

/**
 * Provides image I/O operations for medical imaging applications.
 *
 * <p>Handles reading, writing, and thumbnail generation with OpenCV optimized codecs. Supports
 * various image formats including JPEG, PNG, TIFF, BMP, and medical imaging formats.
 *
 * @author Weasis Team
 * @since 1.0
 */
public final class ImageIOHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ImageIOHandler.class);

  private static final int PNG_COMPRESSION_LEVEL = 9;
  private static final int THUMBNAIL_JPEG_QUALITY = 85;

  public static final String NULL_SOURCE_IMAGE_ERROR = "Source image cannot be null";

  private ImageIOHandler() {
    // Utility class - prevent instantiation
  }

  /**
   * Reads an image from file with optional metadata extraction from EXIF.
   *
   * @param path the image file path to read - must exist and be readable
   * @param tags optional list to populate with extracted metadata tags
   * @return a new {@link ImageCV} containing the loaded image data, or {@code null} if failed
   * @throws IllegalArgumentException if path is null or not readable
   */
  public static ImageCV readImage(Path path, List<String> tags) {
    try {
      return readImageWithCvException(path, tags);
    } catch (OutOfMemoryError | CvException e) {
      LOGGER.error("Reading image failed", e);
      return null;
    }
  }

  /**
   * Reads an image from file with exception propagation.
   *
   * @param path the image file path to read
   * @param tags optional metadata tag list
   * @return a new {@link ImageCV} with the loaded image
   * @throws CvException if the image cannot be read or is corrupted
   * @throws IllegalArgumentException if path is null or not readable
   */
  public static ImageCV readImageWithCvException(Path path, List<String> tags) {
    validateReadablePath(path);

    Mat mat;
    if (tags == null) {
      mat = Imgcodecs.imread(path.toAbsolutePath().toString());
      return handleImageConversion(path, mat);
    }
    MatOfInt metadataTypes = new MatOfInt();
    List<Mat> metadataList = new ArrayList<>();
    mat =
        Imgcodecs.imreadWithMetadata(path.toAbsolutePath().toString(), metadataTypes, metadataList);

    List<String> exifTags = MetadataParser.parseExifParseMetadata(metadataList, metadataTypes);
    tags.clear();
    tags.addAll(exifTags);

    return handleImageConversion(path, mat);
  }

  /**
   * Writes an OpenCV Mat image to file with automatic format detection.
   *
   * @param source the Mat image to write - must not be empty
   * @param path the output file path - parent directories will be created if needed
   * @return {@code true} if successfully written
   * @throws IllegalArgumentException if source is null/empty or path is null
   */
  public static boolean writeImage(Mat source, Path path) {
    validateWriteParameters(source, path);
    return writeImageInternal(source, path, null);
  }

  /**
   * Writes a Java RenderedImage to file with format conversion.
   *
   * @param source the RenderedImage to write
   * @param path the output file path
   * @return {@code true} if successfully written
   * @throws IllegalArgumentException if source or path is null
   */
  public static boolean writeImage(RenderedImage source, Path path) {
    Objects.requireNonNull(source, "RenderedImage cannot be null");
    Objects.requireNonNull(path, "Output path cannot be null");
    try {
      var mat = ImageConversion.toMat(source);
      return writeImageInternal(mat, path, null);
    } catch (Exception e) {
      LOGGER.error("Error converting RenderedImage to Mat for path: {}", path.toAbsolutePath(), e);
      return false;
    }
  }

  /**
   * Writes an image with custom encoding parameters.
   *
   * @param source the Mat image to write
   * @param path the output file path
   * @param params the encoding parameters as {@link MatOfInt} containing parameter pairs:
   *     (parameter_id, value, parameter_id, value, ...)
   *     <p>Common parameters include:
   *     <ul>
   *       <li>{@code Imgcodecs.IMWRITE_JPEG_QUALITY} - JPEG quality (0-100)
   *       <li>{@code Imgcodecs.IMWRITE_PNG_COMPRESSION} - PNG compression (0-9)
   *       <li>{@code Imgcodecs.IMWRITE_TIFF_COMPRESSION} - TIFF compression type
   *     </ul>
   *
   * @return {@code true} if the image was successfully written, {@code false} otherwise
   * @throws IllegalArgumentException if source is null/empty or path is null
   */
  public static boolean writeImage(Mat source, Path path, MatOfInt params) {
    validateWriteParameters(source, path);
    return writeImageInternal(source, path, params);
  }

  /**
   * Writes an image in PNG format with maximum compression.
   *
   * @param source the Mat image to write
   * @param path the output file path - extension will be enforced as .png
   * @return {@code true} if successfully written
   * @throws IllegalArgumentException if source is null/empty or path is null
   */
  public static boolean writePNG(Mat source, Path path) {
    validateWriteParameters(source, path);

    var pngPath = ensurePngExtension(path);
    var convertedSource = convertForPngIfNeeded(source);

    try {
      var params = new MatOfInt(Imgcodecs.IMWRITE_PNG_COMPRESSION, PNG_COMPRESSION_LEVEL);
      return writeImageInternal(convertedSource, pngPath, params);
    } finally {
      if (convertedSource != source) {
        ImageConversion.releaseMat(convertedSource);
      }
    }
  }

  /**
   * Creates and writes a thumbnail image with automatic size optimization.
   *
   * @param source the source Mat image
   * @param path the output thumbnail file path
   * @param maxSize the maximum dimension for the thumbnail
   * @return {@code true} if successfully created and written
   * @throws IllegalArgumentException if parameters are invalid
   */
  public static boolean writeThumbnail(Mat source, Path path, int maxSize) {
    validateWriteParameters(source, path);
    if (maxSize <= 0) {
      throw new IllegalArgumentException("Maximum size must be positive: " + maxSize);
    }

    try {
      var thumbnail = createThumbnail(source, maxSize);
      var params = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, THUMBNAIL_JPEG_QUALITY);
      var success = writeImageInternal(thumbnail, path, params);

      thumbnail.release();
      return success;

    } catch (Exception e) {
      LOGGER.error("Error creating thumbnail for path: {}", path.toAbsolutePath(), e);
      return false;
    }
  }

  /**
   * Creates a thumbnail image with aspect ratio preservation.
   *
   * @param source the source PlanarImage to thumbnail
   * @param iconDim the target thumbnail dimensions
   * @param keepRatio if {@code true}, preserves aspect ratio
   * @return a new {@link ImageCV} containing the thumbnail
   * @throws IllegalArgumentException if parameters are invalid
   */
  public static ImageCV buildThumbnail(PlanarImage source, Dimension iconDim, boolean keepRatio) {
    Objects.requireNonNull(source, NULL_SOURCE_IMAGE_ERROR);
    Objects.requireNonNull(iconDim, "Icon dimensions cannot be null");

    if (iconDim.width <= 0 || iconDim.height <= 0) {
      throw new IllegalArgumentException("Icon dimensions must be positive: " + iconDim);
    }

    var sourceMat = source.toMat();
    if (sourceMat.empty()) {
      throw new IllegalArgumentException("Source image cannot be empty");
    }

    var targetSize = calculateTargetSize(sourceMat, iconDim, keepRatio);
    return createThumbnailFromSize(sourceMat, targetSize);
  }

  // Private helper methods with minimal or no documentation

  private static ImageCV handleImageConversion(Path path, Mat mat) {
    if (mat.empty()) {
      throw new CvException("Failed to read image or unsupported format: " + path);
    }
    return ImageCV.fromMat(mat);
  }

  private static boolean writeImageInternal(Mat source, Path path, MatOfInt params) {
    try {
      prepareOutputPath(path);
      var filename = path.toAbsolutePath().toString();
      var success =
          params != null
              ? Imgcodecs.imwrite(filename, source, params)
              : Imgcodecs.imwrite(filename, source);

      if (!success) {
        LOGGER.warn("Failed to write image to: {}", path);
        FileUtil.delete(path);
      }
      return success;

    } catch (Exception | OutOfMemoryError e) {
      LOGGER.error("Error writing image to path: {}", path, e);
      FileUtil.delete(path);
      return false;
    }
  }

  private static void prepareOutputPath(Path path) throws IOException {
    FileUtil.prepareToWriteFile(path);
    if (!Files.exists(path)) {
      Files.createFile(path);
    }
    if (!Files.isWritable(path)) {
      throw new IOException("Path is not writable: " + path);
    }
  }

  private static Path ensurePngExtension(Path path) {
    var fileName = path.getFileName().toString();
    if (!fileName.toLowerCase().endsWith(".png")) {
      return path.resolveSibling(FileUtil.nameWithoutExtension(fileName) + ".png");
    }
    return path;
  }

  private static Mat convertForPngIfNeeded(Mat source) {
    var type = source.type();
    var elemSize = CvType.ELEM_SIZE(type);
    var channels = CvType.channels(type);
    var bpp = (elemSize * 8) / channels;

    if (bpp > 16 || !CvType.isInteger(type)) {
      var dstImg = new Mat();
      source.convertTo(dstImg, CvType.CV_16SC(channels));
      return dstImg;
    }
    return source;
  }

  private static ImageCV createThumbnail(Mat source, int maxSize) {
    var thumbSize = calculateThumbnailSize(source.cols(), source.rows(), maxSize);
    return shouldScale(source, thumbSize)
        ? ImageTransformer.scale(source, thumbSize)
        : ImageCV.fromMat(source);
  }

  private static Dimension calculateTargetSize(
      Mat sourceMat, Dimension iconDim, boolean keepRatio) {
    if (keepRatio) {
      var maxSize = Math.min(iconDim.width, iconDim.height);
      return calculateThumbnailSize(sourceMat.cols(), sourceMat.rows(), maxSize);
    }
    return new Dimension(iconDim.width, iconDim.height);
  }

  private static ImageCV createThumbnailFromSize(Mat sourceMat, Dimension targetSize) {
    return shouldScale(sourceMat, targetSize)
        ? ImageTransformer.scale(sourceMat, targetSize)
        : ImageCV.fromMat(sourceMat);
  }

  private static boolean shouldScale(Mat source, Dimension targetSize) {
    return targetSize.width < source.cols() || targetSize.height < source.rows();
  }

  private static Dimension calculateThumbnailSize(
      int originalWidth, int originalHeight, int maxSize) {
    var scale = Math.min(maxSize / (double) originalHeight, (double) maxSize / originalWidth);
    return scale < 1.0
        ? new Dimension((int) (scale * originalWidth), (int) (scale * originalHeight))
        : new Dimension(originalWidth, originalHeight);
  }

  // ======= Validation methods =======

  public static void validateReadablePath(Path path) {
    Objects.requireNonNull(path, "File path cannot be null");

    if (!Files.isReadable(path)) {
      throw new IllegalArgumentException("File path is not readable: " + path.toAbsolutePath());
    }
  }

  public static void validateSource(Mat source) {
    Objects.requireNonNull(source, NULL_SOURCE_IMAGE_ERROR);
    if (source.empty()) {
      throw new IllegalArgumentException("Source image cannot be empty");
    }
  }

  private static void validateWriteParameters(Mat source, Path path) {
    Objects.requireNonNull(source, NULL_SOURCE_IMAGE_ERROR);
    Objects.requireNonNull(path, "Output path cannot be null");
    if (source.empty()) {
      throw new IllegalArgumentException("Source image cannot be empty");
    }
  }
}
