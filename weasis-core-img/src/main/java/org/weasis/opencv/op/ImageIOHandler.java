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
import org.weasis.opencv.data.PlanarImage;

/**
 * Provides comprehensive image input/output operations for medical imaging applications.
 *
 * <p>This class handles all file-based image operations including:
 *
 * <ul>
 *   <li>Reading various image formats with metadata extraction
 *   <li>Writing images with format-specific optimization
 *   <li>Thumbnail generation with aspect ratio preservation
 *   <li>Format conversion and quality control
 * </ul>
 *
 * <p>The implementation leverages OpenCV's optimized codecs for superior performance and supports
 * medical imaging formats commonly used in DICOM workflows. All operations include comprehensive
 * error handling and logging for production environments.
 *
 * @author Weasis Team
 * @since 1.0
 */
public final class ImageIOHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImageIOHandler.class);

  private static final int PNG_COMPRESSION_LEVEL = 9;

  private ImageIOHandler() {
    // Utility class - prevent instantiation
  }

  /**
   * Reads an image from file with optional exif tag extraction.
   *
   * <p>Loads an image using OpenCV's optimized codecs with support for various formats including
   * JPEG, PNG, TIFF, BMP, and medical imaging formats.
   *
   * @param path the image file path to read - must exist and be readable
   * @param tags optional list to populate with extracted metadata tags. Common exif tags are
   *     defined in OpenCV imread function.
   * @return a new {@link ImageCV} containing the loaded image data, or {@code null} if the image
   *     cannot be read due to format incompatibility or file corruption
   * @throws IllegalArgumentException if path is null or does not point to a readable file
   */
  public static ImageCV readImage(Path path, List<String> tags) {
    try {
      return readImageWithCvException(path, tags);
    } catch (OutOfMemoryError | CvException e) {
      LOGGER.error("Reading image", e);
      return null;
    }
  }

  /**
   * Reads an image from file with exception throwing for error handling.
   *
   * <p>Similar to {@link #readImage(Path, List)} but throws exceptions instead of returning null,
   * providing more detailed error information for debugging and error handling in calling code.
   *
   * @param path the image file path to read
   * @param tags optional metadata tag list
   * @return a new {@link ImageCV} with the loaded image
   * @throws CvException if the image cannot be read or is corrupted
   * @throws NullPointerException if path is null or does not point to a readable file
   */
  public static ImageCV readImageWithCvException(Path path, List<String> tags) {
    validateSourcePath(path);

    List<String> exifs = tags != null ? tags : new ArrayList<>();
    Mat mat = Imgcodecs.imread(path.toAbsolutePath().toString(), exifs);

    if (mat.empty()) {
      throw new CvException("Failed to load image from: " + path.toAbsolutePath());
    }
    return ImageCV.fromMat(mat);
  }

  /**
   * Writes an OpenCV Mat image to file with automatic format detection.
   *
   * <p>Saves the image using the format determined by the file extension. The method automatically
   * selects appropriate encoding parameters for optimal quality and file size based on the detected
   * format.
   *
   * @param source the Mat image to write - must not be empty
   * @param path the output file path - parent directories will be created if needed
   * @return {@code true} if the image was successfully written, {@code false} otherwise
   * @throws NullPointerException if source or path is null
   * @throws IllegalArgumentException if source is empty
   */
  public static boolean writeImage(Mat source, Path path) {
    validateParameters(source, path);
    return writeImageInternal(source, path, null);
  }

  /**
   * Writes a Java RenderedImage to file with format conversion.
   *
   * <p>Converts the Java image to OpenCV format before writing, enabling the use of OpenCV's
   * optimized codecs for better performance and broader format support compared to standard Java
   * ImageIO.
   *
   * @param source the RenderedImage to write
   * @param path the output file path
   * @return {@code true} if the image was successfully written, {@code false} otherwise
   * @throws IllegalArgumentException if source is null or path is null
   */
  public static boolean writeImage(RenderedImage source, Path path) {
    Objects.requireNonNull(source, "RenderedImage cannot be null");
    validateOutputParameters(path);
    try {
      Mat mat = ImageConversion.toMat(Objects.requireNonNull(source));
      return writeImageInternal(mat, path, null);
    } catch (Exception e) {
      LOGGER.error("Error converting RenderedImage to Mat for path: {}", path.toAbsolutePath(), e);
      return false;
    }
  }

  /**
   * Writes an image with custom encoding parameters.
   *
   * <p>Provides full control over the encoding process by accepting format-specific parameters.
   * This allows fine-tuning of compression levels, quality settings, and other encoding options.
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
    validateParameters(source, path);
    return writeImageInternal(source, path, params);
  }

  /**
   * Writes an image in PNG format with maximum compression.
   *
   * <p>Specialized method for PNG output with optimized compression settings for minimal file size
   * while maintaining lossless quality. Ideal for medical images where compression artifacts are
   * unacceptable.
   *
   * @param source the Mat image to write
   * @param path the output PNG file path - extension will be enforced as .png
   * @return {@code true} if the PNG was successfully written, {@code false} otherwise
   * @throws IllegalArgumentException if source is null/empty or path is null
   */
  public static boolean writePNG(Mat source, Path path) {
    validateParameters(source, path);

    // Ensure PNG extension
    String fileName = path.getFileName().toString();
    if (!fileName.toLowerCase().endsWith(".png")) {
      path = path.resolveSibling(FileUtil.nameWithoutExtension(fileName) + ".png");
    }

    Mat dstImg = null;
    int type = source.type();
    int elemSize = CvType.ELEM_SIZE(type);
    int channels = CvType.channels(type);
    int bpp = (elemSize * 8) / channels;
    if (bpp > 16 || !CvType.isInteger(type)) {
      dstImg = new Mat();
      source.convertTo(dstImg, CvType.CV_16SC(channels));
      source = dstImg;
    }

    try {
      MatOfInt params = new MatOfInt(Imgcodecs.IMWRITE_PNG_COMPRESSION, PNG_COMPRESSION_LEVEL);
      return writeImageInternal(source, path, params);
    } finally {
      ImageConversion.releaseMat(dstImg);
    }
  }

  /**
   * Creates and writes a thumbnail image with automatic size optimization.
   *
   * <p>Generates a thumbnail by scaling the source image while preserving aspect ratio. The
   * thumbnail size is automatically calculated to fit within the specified maximum dimension while
   * maintaining the original proportions.
   *
   * @param source the source Mat image
   * @param path the output thumbnail file path
   * @param maxSize the maximum dimension (width or height) for the thumbnail. The actual size will
   *     be calculated to preserve aspect ratio while ensuring no dimension exceeds this value
   * @return {@code true} if the thumbnail was successfully created and written, {@code false}
   *     otherwise
   * @throws IllegalArgumentException if source is null/empty, path is null, or maxSize is not
   *     positive
   */
  public static boolean writeThumbnail(Mat source, Path path, int maxSize) {
    validateParameters(source, path);
    if (maxSize <= 0) {
      throw new IllegalArgumentException("Maximum size must be positive: " + maxSize);
    }

    try {
      // Calculate thumbnail dimensions preserving aspect ratio
      Dimension thumbSize = calculateThumbnailSize(source.cols(), source.rows(), maxSize);

      ImageCV thumbnail;
      // Scale the image
      if (thumbSize.width >= source.cols() && thumbSize.height >= source.rows()) {
        thumbnail = ImageCV.fromMat(source);
      } else {
        thumbnail = ImageTransformer.scale(source, thumbSize);
      }

      // Write with JPEG optimization for thumbnails
      MatOfInt params = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 85);
      boolean success = writeImageInternal(thumbnail, path, params);

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
   * <p>Generates a thumbnail version of the source image by scaling it to fit within the specified
   * dimensions. The scaling preserves aspect ratio to prevent distortion and ensures the result is
   * never larger than the original image.
   *
   * @param source the source PlanarImage to thumbnail
   * @param iconDim the target thumbnail dimensions. If keepRatio is true, the smaller dimension is
   *     used as the maximum size constraint
   * @param keepRatio if {@code true}, preserves aspect ratio and uses the minimum of iconDim
   *     width/height as the size constraint. If {@code false}, scales to exact dimensions
   *     potentially distorting the image
   * @return a new {@link ImageCV} containing the thumbnail. Will not be larger than the original
   *     image dimensions even if iconDim is larger
   * @throws IllegalArgumentException if source is null, iconDim is null, or dimensions are not
   *     positive
   */
  public static ImageCV buildThumbnail(PlanarImage source, Dimension iconDim, boolean keepRatio) {
    Objects.requireNonNull(source, "Source image cannot be null");
    Objects.requireNonNull(iconDim, "Icon dimensions cannot be null");

    if (iconDim.width <= 0 || iconDim.height <= 0) {
      throw new IllegalArgumentException("Icon dimensions must be positive: " + iconDim);
    }

    Mat sourceMat = source.toMat();
    if (sourceMat.empty()) {
      throw new IllegalArgumentException("Source image cannot be empty");
    }
    int originalWidth = sourceMat.cols();
    int originalHeight = sourceMat.rows();

    Dimension targetSize;
    if (keepRatio) {
      int maxSize = Math.min(iconDim.width, iconDim.height);
      targetSize = calculateThumbnailSize(originalWidth, originalHeight, maxSize);
    } else {
      targetSize = new Dimension(iconDim.width, iconDim.height);
    }

    ImageCV thumbnail;
    // Scale the image only if the target size is smaller than the original
    if (targetSize.width >= originalWidth && targetSize.height >= originalHeight) {
      thumbnail = ImageCV.fromMat(sourceMat);
    } else {
      thumbnail = ImageTransformer.scale(sourceMat, targetSize);
    }
    return thumbnail;
  }

  private static boolean writeImageInternal(Mat source, Path path, MatOfInt params) {
    try {
      FileUtil.prepareToWriteFile(path);
      if (!Files.exists(path)) {
        Files.createFile(path);
      }
      if (!Files.isWritable(path)) {
        LOGGER.warn("Path is not writable: {}", path);
        return false;
      }

      boolean success;
      String filename = path.toAbsolutePath().toString();
      if (params != null) {
        success = Imgcodecs.imwrite(filename, source, params);
      } else {
        success = Imgcodecs.imwrite(filename, source);
      }

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

  private static Dimension calculateThumbnailSize(
      int originalWidth, int originalHeight, int maxSize) {
    final double scale =
        Math.min(maxSize / (double) originalHeight, (double) maxSize / originalWidth);
    if (scale < 1.0) {
      return new Dimension((int) (scale * originalWidth), (int) (scale * originalHeight));
    }
    return new Dimension(originalWidth, originalHeight);
  }

  public static void validateSourcePath(Path path) {
    Objects.requireNonNull(path, "File path cannot be null");

    if (!Files.isReadable(path)) {
      throw new IllegalArgumentException("File path is not readable: " + path.toAbsolutePath());
    }
  }

  public static void validateSource(Mat source) {
    Objects.requireNonNull(source, "Source image cannot be null");
    if (source.empty()) {
      throw new IllegalArgumentException("Source image cannot be empty");
    }
  }

  private static void validateOutputParameters(Path path) {
    Objects.requireNonNull(path, "Output path cannot be null");
  }

  private static void validateParameters(Mat source, Path path) {
    validateSource(source);
    validateOutputParameters(path);
  }
}
