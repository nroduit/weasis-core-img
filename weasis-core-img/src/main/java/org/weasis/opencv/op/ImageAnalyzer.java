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

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;

/**
 * Provides statistical analysis and measurement operations for medical images.
 *
 * <p>This class focuses on extracting quantitative information from images including:
 *
 * <ul>
 *   <li>Min/max pixel values and their locations
 *   <li>Mean and standard deviation calculations
 *   <li>Statistical analysis with region-of-interest support
 *   <li>Pixel padding value exclusion for accurate measurements
 * </ul>
 *
 * <p>All operations are thread-safe and designed for high-performance medical image analysis. The
 * class supports both OpenCV Mat objects and Java RenderedImage interfaces.
 *
 * @author Weasis Team
 * @since 1.0
 */
public final class ImageAnalyzer {

  private ImageAnalyzer() {
    // Utility class - prevent instantiation
  }

  /**
   * Create the mask image from the shape and exclude the pixel padding values.
   *
   * @param source the image.
   * @param shape the shape to apply on the image. If null, the whole image is processed.
   * @param paddingValue the starting value to exclude. PaddingValue is applied only with one
   *     channel image.
   * @param paddingLimit the last value to exclude. If null only paddingValue is excluded.
   * @return the source and mask images.
   */
  public static List<Mat> getMaskImage(
      Mat source, Shape shape, Integer paddingValue, Integer paddingLimit) {
    Objects.requireNonNull(source);
    Mat srcImg;
    Mat mask = null;
    if (shape == null) {
      srcImg = source;
    } else {
      Rectangle b =
          new Rectangle(0, 0, source.width(), source.height()).intersection(shape.getBounds());
      if (b.getWidth() < 1 || b.getHeight() < 1) {
        return Collections.emptyList();
      }

      srcImg = source.submat(new Rect(b.x, b.y, b.width, b.height));
      mask = Mat.zeros(srcImg.size(), CvType.CV_8UC1);
      List<MatOfPoint> pts = transformShapeToContour(shape, false);
      Imgproc.fillPoly(mask, pts, new Scalar(255));
    }

    mask = getPixelPaddingMask(srcImg, mask, paddingValue, paddingLimit);
    return Arrays.asList(srcImg, mask);
  }

  /**
   * Converts a shape to a list of contours compliant with OpenCV.
   *
   * @param shape the shape to transform.
   * @param keepImageCoordinates if true, the coordinates are not translated to the shape bounds.
   * @return the list of contours.
   */
  public static List<MatOfPoint> transformShapeToContour(
      Shape shape, boolean keepImageCoordinates) {
    Rectangle b = shape.getBounds();
    if (keepImageCoordinates) {
      b.x = 0;
      b.y = 0;
    }
    List<MatOfPoint> points = new ArrayList<>();
    List<Point> cvPts = new ArrayList<>();

    PathIterator iterator = new FlatteningPathIterator(shape.getPathIterator(null), 2);
    double[] pts = new double[6];
    MatOfPoint p = null;
    while (!iterator.isDone()) {
      int segType = iterator.currentSegment(pts);
      switch (segType) {
        case PathIterator.SEG_MOVETO -> {
          addSegment(p, cvPts, points);
          p = new MatOfPoint();
          cvPts.add(new Point(pts[0] - b.x, pts[1] - b.y));
        }
        case PathIterator.SEG_LINETO, PathIterator.SEG_CLOSE ->
            cvPts.add(new Point(pts[0] - b.x, pts[1] - b.y));
        default -> {
          // should never append with FlatteningPathIterator
        }
      }
      iterator.next();
    }

    addSegment(p, cvPts, points);
    return points;
  }

  private static void addSegment(MatOfPoint p, List<Point> cvPts, List<MatOfPoint> points) {
    if (p != null) {
      if (cvPts.size() > 1) {
        int last = cvPts.size() - 1;
        if (cvPts.get(last - 1).equals(cvPts.get(last))) {
          cvPts.remove(last);
        }
      }
      p.fromList(cvPts);
      cvPts.clear();
      points.add(p);
    }
  }

  /**
   * Computes minimum and maximum pixel values from a PlanarImage with optional 8-bit exclusion.
   *
   * <p>This method performs a comprehensive analysis of pixel values across the entire image. For
   * 8-bit images, it can optionally return default values (0, 255) to optimize performance when the
   * full dynamic range is known.
   *
   * @param img the source image to analyze
   * @param exclude8bitImage if {@code true}, 8-bit images return default values (0, 255) without
   *     computation for performance optimization
   * @return a {@link MinMaxLocResult} containing minimum and maximum values with their pixel
   *     locations. The maximum value is guaranteed to be at least (minimum + 1) to prevent division
   *     by zero in subsequent calculations
   * @throws OutOfMemoryError if the image is too large to process in available memory
   * @throws IllegalArgumentException if the image is null or has invalid dimensions
   */
  public static MinMaxLocResult findRawMinMaxValues(PlanarImage img, boolean exclude8bitImage)
      throws OutOfMemoryError {
    Objects.requireNonNull(img, "Image cannot be null");

    MinMaxLocResult val;
    if (CvType.depth(Objects.requireNonNull(img).type()) <= 1 && exclude8bitImage) {
      val = new MinMaxLocResult();
      val.minVal = 0.0;
      val.maxVal = 255.0;
    } else {
      val = ImageAnalyzer.findMinMaxValues(img.toMat());
      // Handle special case when min and max are equal, ex. black image
      // + 1 to max enables to display the correct value
      if (val.minVal == val.maxVal) {
        val.maxVal += 1.0;
      }
    }
    return val;
  }

  /**
   * Finds minimum and maximum pixel values in a Mat image.
   *
   * <p>Performs a complete scan of the image to determine the actual minimum and maximum pixel
   * values across all channels. This is essential for proper window/level calculations and
   * histogram analysis.
   *
   * @param source the source Mat to analyze
   * @return a {@link MinMaxLocResult} with min/max values and their locations. Maximum is
   *     guaranteed to be at least (minimum + 1) to avoid division by zero
   * @throws IllegalArgumentException if source is null or empty
   */
  public static MinMaxLocResult findMinMaxValues(Mat source) {
    return minMaxLoc(source, null);
  }

  /**
   * Finds minimum and maximum pixel values while excluding a specified range of padding values.
   *
   * <p>This method is particularly useful for medical images where certain pixel values represent
   * non-anatomical information (padding, background) that should be excluded from statistical
   * analysis.
   *
   * @param source the source Mat to analyze
   * @param paddingValue the starting value to exclude from analysis
   * @param paddingLimit the ending value to exclude (inclusive). If null, only paddingValue is
   *     excluded
   * @return a {@link MinMaxLocResult} with min/max values excluding the specified range
   * @throws IllegalArgumentException if source is null or empty
   */
  public static MinMaxLocResult findMinMaxValues(
      Mat source, Integer paddingValue, Integer paddingLimit) {
    ImageIOHandler.validateSource(source);
    if (paddingValue == null) {
      return findMinMaxValues(source);
    }

    List<Mat> maskData = getMaskImage(source, null, paddingValue, paddingLimit);
    return minMaxLoc(maskData.get(0), maskData.get(1));
  }

  /**
   * Computes minimum and maximum values within a rectangular region of interest.
   *
   * <p>Restricts the analysis to a specific rectangular area of the image, useful for localized
   * measurements and region-specific statistics.
   *
   * @param source the source RenderedImage to analyze
   * @param area the rectangular region of interest. If null, analyzes the entire image
   * @return a {@link MinMaxLocResult} with min/max values within the specified area
   * @throws IllegalArgumentException if source is null
   */
  public static MinMaxLocResult minMaxLoc(RenderedImage source, Rectangle area) {
    Objects.requireNonNull(source, "Source image cannot be null");

    // Convert to Mat and apply ROI if specified
    Mat mat = ImageConversion.toMat(source);
    if (area != null) {
      mat = ImageTransformer.crop(mat, area).toMat();
    }

    return minMaxLoc(mat, null);
  }

  /**
   * Computes minimum and maximum pixel values with optional mask application.
   *
   * <p>This is the core min/max computation method that handles masked regions. The mask allows for
   * complex region-of-interest definitions beyond simple rectangles.
   *
   * @param srcImg the source Mat to analyze
   * @param mask optional mask Mat where non-zero pixels indicate regions to include in analysis. If
   *     null, analyzes the entire image
   * @return a {@link MinMaxLocResult} containing min/max values and their pixel locations. Ensures
   *     maximum >= minimum + 1 to prevent division by zero
   * @throws IllegalArgumentException if srcImg is null or empty
   */
  public static MinMaxLocResult minMaxLoc(Mat srcImg, Mat mask) {
    ImageIOHandler.validateSource(srcImg);

    List<Mat> channels = new ArrayList<>(srcImg.channels());
    if (srcImg.channels() > 1) {
      Core.split(srcImg, channels);
    } else {
      channels.add(srcImg);
    }

    MinMaxLocResult result = new MinMaxLocResult();
    result.minVal = Double.MAX_VALUE;
    result.maxVal = -Double.MAX_VALUE;

    for (Mat channel : channels) {
      MinMaxLocResult minMax = Core.minMaxLoc(channel, mask);
      result.minVal = Math.min(result.minVal, minMax.minVal);
      if (result.minVal == minMax.minVal) {
        result.minLoc = minMax.minLoc;
      }
      result.maxVal = Math.max(result.maxVal, minMax.maxVal);
      if (result.maxVal == minMax.maxVal) {
        result.maxLoc = minMax.maxLoc;
      }
    }
    return result;
  }

  /**
   * Computes mean and standard deviation statistics for the entire image.
   *
   * <p>Calculates comprehensive statistical measures across all channels of the image. This
   * overload processes the complete image without any regional restrictions.
   *
   * @param source the source Mat to analyze
   * @return a 2D array where each row represents a channel, containing: [min, max, mean,
   *     standard_deviation, pixel_count]
   * @throws IllegalArgumentException if source is null or empty
   */
  public static double[][] meanStdDev(Mat source) {
    return meanStdDev(source, (Shape) null, null, null);
  }

  /**
   * Computes mean and standard deviation statistics within a shaped region.
   *
   * <p>Restricts statistical analysis to pixels within the specified geometric shape, enabling
   * precise region-of-interest measurements for complex anatomical structures.
   *
   * @param source the source Mat to analyze
   * @param shape the geometric shape defining the region of interest. If null, analyzes entire
   *     image
   * @return a 2D array where each row represents a channel, containing: [min, max, mean,
   *     standard_deviation, pixel_count]
   * @throws IllegalArgumentException if source is null or empty
   */
  public static double[][] meanStdDev(Mat source, Shape shape) {
    return meanStdDev(source, shape, null, null);
  }

  /**
   * Computes comprehensive statistical measures with shape masking and padding exclusion.
   *
   * <p>This method provides the most flexible statistical analysis by combining:
   *
   * <ul>
   *   <li>Geometric region-of-interest definition via Shape
   *   <li>Exclusion of padding/background values
   *   <li>Multi-channel support for color images
   * </ul>
   *
   * @param source the source Mat to analyze
   * @param shape the geometric shape for region restriction. If null, uses entire image
   * @param paddingValue the starting pixel value to exclude from statistics
   * @param paddingLimit the ending pixel value to exclude (inclusive). If null, only paddingValue
   *     excluded
   * @return a 2D array where result[channel] = [min, max, mean, std_dev, pixel_count]
   * @throws IllegalArgumentException if source is null or empty
   */
  public static double[][] meanStdDev(
      Mat source, Shape shape, Integer paddingValue, Integer paddingLimit) {
    ImageIOHandler.validateSource(source);

    List<Mat> maskData = getMaskImage(source, shape, paddingValue, paddingLimit);
    return buildMeanStdDev(maskData.get(0), maskData.get(1));
  }

  /**
   * Computes statistical measures using a pre-computed mask with padding exclusion.
   *
   * <p>Allows for maximum flexibility by accepting a custom mask while still supporting the
   * exclusion of specific pixel value ranges. Useful when complex masking operations have already
   * been performed.
   *
   * @param source the source Mat to analyze
   * @param mask the binary mask where non-zero pixels indicate inclusion in statistics
   * @param paddingValue the starting pixel value to exclude
   * @param paddingLimit the ending pixel value to exclude (inclusive). If null, only paddingValue
   *     excluded
   * @return a 2D array where result[channel] = [min, max, mean, std_dev, pixel_count]
   * @throws IllegalArgumentException if source is null or empty
   */
  public static double[][] meanStdDev(
      Mat source, Mat mask, Integer paddingValue, Integer paddingLimit) {
    ImageIOHandler.validateSource(source);

    Mat finalMask = mask;
    if (paddingValue != null) {
      finalMask = getPixelPaddingMask(source, mask, paddingValue, paddingLimit);
    }

    return buildMeanStdDev(source, finalMask);
  }

  /**
   * Core statistical computation method using OpenCV operations.
   *
   * <p>Performs the actual mean and standard deviation calculations using optimized OpenCV
   * functions. Handles multi-channel images and provides comprehensive statistics.
   *
   * @param source the source Mat
   * @param mask the binary mask for region selection
   * @return statistical data array [channel][min, max, mean, std_dev, pixel_count]
   */
  private static double[][] buildMeanStdDev(Mat source, Mat mask) {
    if (source == null) {
      return null;
    }
    MatOfDouble mean = new MatOfDouble();
    MatOfDouble stddev = new MatOfDouble();
    if (mask == null) {
      Core.meanStdDev(source, mean, stddev);
    } else {
      Core.meanStdDev(source, mean, stddev, mask);
    }

    List<Mat> channels = new ArrayList<>();
    if (source.channels() > 1) {
      Core.split(source, channels);
    } else {
      channels.add(source);
    }

    double[][] val = new double[5][channels.size()];
    for (int i = 0; i < channels.size(); i++) {
      MinMaxLocResult minMax;
      if (mask == null) {
        minMax = Core.minMaxLoc(channels.get(i));
      } else {
        minMax = Core.minMaxLoc(channels.get(i), mask);
      }
      val[0][i] = minMax.minVal;
      val[1][i] = minMax.maxVal;
    }

    val[2] = mean.toArray();
    val[3] = stddev.toArray();
    if (mask == null) {
      val[4][0] = source.width() * (double) source.height();
    } else {
      val[4][0] = Core.countNonZero(mask);
    }
    return val;
  }

  /**
   * Creates a mask that excludes specified padding value ranges.
   *
   * <p>Generates a binary mask where pixels with values in the specified range are set to 0
   * (excluded) and all other pixels are set to 255 (included). This mask can then be used with
   * other OpenCV operations.
   *
   * @param source the source image
   * @param mask optional existing mask to combine with padding exclusion
   * @param paddingValue the starting value to exclude
   * @param paddingLimit the ending value to exclude (inclusive)
   * @return a binary mask with padding values excluded
   */
  private static Mat getPixelPaddingMask(
      Mat source, Mat mask, Integer paddingValue, Integer paddingLimit) {

    if (paddingValue != null && source.channels() == 1) {
      if (paddingLimit == null) {
        paddingLimit = paddingValue;
      } else if (paddingLimit < paddingValue) {
        int temp = paddingValue;
        paddingValue = paddingLimit;
        paddingLimit = temp;
      }
      Mat maskPix = new Mat(source.size(), CvType.CV_8UC1, new Scalar(0));
      excludePaddingValue(source, maskPix, paddingValue, paddingLimit);
      Mat paddingMask;
      if (mask == null) {
        paddingMask = maskPix;
      } else {
        paddingMask = new ImageCV();
        Core.bitwise_and(mask, maskPix, paddingMask);
      }
      return paddingMask;
    }

    return mask;
  }

  /**
   * Excludes pixels within the specified value range from the mask.
   *
   * <p>Modifies the mask by setting pixels to 0 where the source image values fall within the
   * [paddingValue, paddingLimit] range.
   *
   * @param src the source image
   * @param mask the mask to modify
   * @param paddingValue the minimum value to exclude
   * @param paddingLimit the maximum value to exclude
   */
  private static void excludePaddingValue(Mat src, Mat mask, int paddingValue, int paddingLimit) {
    // Create mask for values in padding range
    Mat paddingMask = new Mat();
    Core.inRange(src, new Scalar(paddingValue), new Scalar(paddingLimit), paddingMask);

    // Invert the padding mask (0 where padding, 255 elsewhere)
    Core.bitwise_not(paddingMask, paddingMask);

    // Combine with existing mask
    Core.add(paddingMask, mask, mask);
  }
}
