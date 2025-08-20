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
   * Creates a mask image from the shape and excludes pixel padding values.
   *
   * @param source the source image
   * @param shape the shape to apply on the image. If null, the whole image is processed
   * @param paddingValue the starting value to exclude (applied only with single channel images)
   * @param paddingLimit the last value to exclude. If null, only paddingValue is excluded
   * @return list containing the source and mask images, or empty list if no intersection
   */
  public static List<Mat> getMaskImage(
      Mat source, Shape shape, Integer paddingValue, Integer paddingLimit) {
    Objects.requireNonNull(source);
    var maskData = createShapeMask(source, shape);
    if (maskData == null) {
      return Collections.emptyList();
    }

    var paddingMask =
        getPixelPaddingMask(maskData.srcImg(), maskData.mask(), paddingValue, paddingLimit);
    return Arrays.asList(maskData.srcImg(), paddingMask);
  }

  /**
   * Converts a shape to OpenCV-compliant contours.
   *
   * @param shape the shape to transform
   * @param keepImageCoordinates if true, coordinates are not translated to shape bounds
   * @return list of contours
   */
  public static List<MatOfPoint> transformShapeToContour(
      Shape shape, boolean keepImageCoordinates) {
    var bounds = shape.getBounds();
    if (keepImageCoordinates) {
      bounds.x = 0;
      bounds.y = 0;
    }

    var contours = new ArrayList<MatOfPoint>();
    var currentPoints = new ArrayList<Point>();
    var pathIterator = new FlatteningPathIterator(shape.getPathIterator(null), 2);
    var coords = new double[6];
    MatOfPoint currentContour = null;

    while (!pathIterator.isDone()) {
      int segType = pathIterator.currentSegment(coords);
      switch (segType) {
        case PathIterator.SEG_MOVETO -> {
          currentContour = finalizePreviousSegment(currentContour, currentPoints, contours);
          currentContour = new MatOfPoint();
          currentPoints.add(new Point(coords[0] - bounds.x, coords[1] - bounds.y));
        }
        case PathIterator.SEG_LINETO, PathIterator.SEG_CLOSE ->
            currentPoints.add(new Point(coords[0] - bounds.x, coords[1] - bounds.y));
      }
      pathIterator.next();
    }

    finalizePreviousSegment(currentContour, currentPoints, contours);
    return contours;
  }

  /**
   * Finds min/max pixel values from a PlanarImage with optional 8-bit optimization.
   *
   * @param img the source image to analyze
   * @param exclude8bitImage if true, 8-bit images return default values (0, 255) for performance
   * @return MinMaxLocResult with min/max values and locations
   * @throws OutOfMemoryError if the image is too large to process
   * @throws IllegalArgumentException if the image is null or invalid
   */
  public static MinMaxLocResult findRawMinMaxValues(PlanarImage img, boolean exclude8bitImage) {
    Objects.requireNonNull(img, "Image cannot be null");

    if (is8BitImage(img) && exclude8bitImage) {
      return create8BitDefaultResult();
    }
    var result = findMinMaxValues(img.toMat());
    return adjustForEqualMinMax(result);
  }

  /**
   * Finds minimum and maximum pixel values in a Mat image.
   *
   * @param source the source Mat to analyze
   * @return MinMaxLocResult with min/max values and locations
   * @throws IllegalArgumentException if source is null or empty
   */
  public static MinMaxLocResult findMinMaxValues(Mat source) {
    return minMaxLoc(source, null);
  }

  /**
   * Finds min/max pixel values while excluding specified padding value ranges.
   *
   * @param source the source Mat to analyze
   * @param paddingValue the starting value to exclude from analysis
   * @param paddingLimit the ending value to exclude (inclusive). If null, only paddingValue
   *     excluded
   * @return MinMaxLocResult with min/max values excluding the specified range
   * @throws IllegalArgumentException if source is null or empty
   */
  public static MinMaxLocResult findMinMaxValues(
      Mat source, Integer paddingValue, Integer paddingLimit) {
    ImageIOHandler.validateSource(source);
    if (paddingValue == null) {
      return findMinMaxValues(source);
    }

    var maskData = getMaskImage(source, null, paddingValue, paddingLimit);
    return maskData.isEmpty() ? new MinMaxLocResult() : minMaxLoc(maskData.get(0), maskData.get(1));
  }

  /**
   * Computes min/max values within a rectangular region of interest.
   *
   * @param source the source RenderedImage to analyze
   * @param area the rectangular region of interest. If null, analyzes entire image
   * @return MinMaxLocResult with min/max values within the specified area
   * @throws IllegalArgumentException if source is null
   */
  public static MinMaxLocResult minMaxLoc(RenderedImage source, Rectangle area) {
    Objects.requireNonNull(source, "Source image cannot be null");

    var mat = ImageConversion.toMat(source);
    if (area != null) {
      mat = ImageTransformer.crop(mat, area);
    }

    return minMaxLoc(mat, null);
  }

  /**
   * Computes min/max pixel values with optional mask application.
   *
   * @param srcImg the source Mat to analyze
   * @param mask optional mask Mat where non-zero pixels indicate regions to include
   * @return MinMaxLocResult containing min/max values and their pixel locations
   * @throws IllegalArgumentException if srcImg is null or empty
   */
  public static MinMaxLocResult minMaxLoc(Mat srcImg, Mat mask) {
    ImageIOHandler.validateSource(srcImg);

    var channels = splitIntoChannels(srcImg);
    var result = new MinMaxLocResult();
    result.minVal = Double.MAX_VALUE;
    result.maxVal = -Double.MAX_VALUE;

    for (var channel : channels) {
      var channelResult = Core.minMaxLoc(channel, mask);
      updateMinMaxResult(result, channelResult);
    }
    return result;
  }

  public static double[][] meanStdDev(Mat source) {
    return meanStdDev(source, (Shape) null, null, null);
  }

  public static double[][] meanStdDev(Mat source, Shape shape) {
    return meanStdDev(source, shape, null, null);
  }

  /**
   * Computes comprehensive statistical measures with shape masking and padding exclusion.
   *
   * @param source the source Mat to analyze
   * @param shape the geometric shape for region restriction. If null, uses entire image
   * @param paddingValue the starting pixel value to exclude from statistics
   * @param paddingLimit the ending pixel value to exclude (inclusive). If null, only paddingValue
   *     excluded
   * @return 2D array where result[measure][channel] = [min, max, mean, std_dev, pixel_count]
   * @throws IllegalArgumentException if source is null or empty
   */
  public static double[][] meanStdDev(
      Mat source, Shape shape, Integer paddingValue, Integer paddingLimit) {
    ImageIOHandler.validateSource(source);

    var maskData = getMaskImage(source, shape, paddingValue, paddingLimit);
    return maskData.isEmpty() ? null : buildMeanStdDev(maskData.get(0), maskData.get(1));
  }

  /**
   * Computes statistical measures using a pre-computed mask with padding exclusion.
   *
   * @param source the source Mat to analyze
   * @param mask the binary mask where non-zero pixels indicate inclusion in statistics
   * @param paddingValue the starting pixel value to exclude
   * @param paddingLimit the ending pixel value to exclude (inclusive). If null, only paddingValue
   *     excluded
   * @return 2D array where result[measure][channel] = [min, max, mean, std_dev, pixel_count]
   * @throws IllegalArgumentException if source is null or empty
   */
  public static double[][] meanStdDev(
      Mat source, Mat mask, Integer paddingValue, Integer paddingLimit) {
    ImageIOHandler.validateSource(source);

    var finalMask =
        (paddingValue != null)
            ? getPixelPaddingMask(source, mask, paddingValue, paddingLimit)
            : mask;

    return buildMeanStdDev(source, finalMask);
  }

  // Private helper methods

  private record MaskData(Mat srcImg, Mat mask) {}

  private static MaskData createShapeMask(Mat source, Shape shape) {
    if (shape == null) {
      return new MaskData(source, null);
    }

    var imageBounds = new Rectangle(0, 0, source.width(), source.height());
    var intersection = imageBounds.intersection(shape.getBounds());

    if (intersection.getWidth() < 1 || intersection.getHeight() < 1) {
      return null;
    }
    var croppedSrc =
        source.submat(
            new Rect(intersection.x, intersection.y, intersection.width, intersection.height));
    var mask = Mat.zeros(croppedSrc.size(), CvType.CV_8UC1);
    var contours = transformShapeToContour(shape, false);
    Imgproc.fillPoly(mask, contours, new Scalar(255));

    return new MaskData(croppedSrc, mask);
  }

  private static MatOfPoint finalizePreviousSegment(
      MatOfPoint currentContour, List<Point> points, List<MatOfPoint> contours) {
    if (currentContour != null && !points.isEmpty()) {
      removeDuplicateLastPoint(points);
      currentContour.fromList(points);
      contours.add(currentContour);
      points.clear();
    }
    return currentContour;
  }

  private static void removeDuplicateLastPoint(List<Point> points) {
    if (points.size() > 1) {
      int lastIndex = points.size() - 1;
      if (points.get(lastIndex - 1).equals(points.get(lastIndex))) {
        points.remove(lastIndex);
      }
    }
  }

  private static boolean is8BitImage(PlanarImage img) {
    return CvType.depth(img.type()) <= 1;
  }

  private static MinMaxLocResult create8BitDefaultResult() {
    var result = new MinMaxLocResult();
    result.minVal = 0.0;
    result.maxVal = 255.0;
    return result;
  }

  private static MinMaxLocResult adjustForEqualMinMax(MinMaxLocResult result) {
    if (result.minVal == result.maxVal) {
      result.maxVal += 1.0; // Prevent division by zero
    }
    return result;
  }

  private static List<Mat> splitIntoChannels(Mat source) {
    var channels = new ArrayList<Mat>(source.channels());
    if (source.channels() > 1) {
      Core.split(source, channels);
    } else {
      channels.add(source);
    }
    return channels;
  }

  private static void updateMinMaxResult(MinMaxLocResult result, MinMaxLocResult channelResult) {
    if (channelResult.minVal < result.minVal) {
      result.minVal = channelResult.minVal;
      result.minLoc = channelResult.minLoc;
    }
    if (channelResult.maxVal > result.maxVal) {
      result.maxVal = channelResult.maxVal;
      result.maxLoc = channelResult.maxLoc;
    }
  }

  private static double[][] buildMeanStdDev(Mat source, Mat mask) {
    if (source == null) {
      return null;
    }

    var statistics = computeBasicStatistics(source, mask);
    var channels = splitIntoChannels(source);
    var results = new double[5][channels.size()];

    populateMinMaxValues(results, channels, mask);
    results[2] = statistics.mean().toArray();
    results[3] = statistics.stdDev().toArray();
    results[4][0] = computePixelCount(source, mask);

    return results;
  }

  private record Statistics(MatOfDouble mean, MatOfDouble stdDev) {}

  private static Statistics computeBasicStatistics(Mat source, Mat mask) {
    var mean = new MatOfDouble();
    var stdDev = new MatOfDouble();
    if (mask == null) {
      Core.meanStdDev(source, mean, stdDev);
    } else {
      Core.meanStdDev(source, mean, stdDev, mask);
    }

    return new Statistics(mean, stdDev);
  }

  private static void populateMinMaxValues(double[][] results, List<Mat> channels, Mat mask) {
    for (int i = 0; i < channels.size(); i++) {
      var minMax =
          (mask == null) ? Core.minMaxLoc(channels.get(i)) : Core.minMaxLoc(channels.get(i), mask);

      results[0][i] = minMax.minVal;
      results[1][i] = minMax.maxVal;
    }
  }

  private static double computePixelCount(Mat source, Mat mask) {
    return (mask == null) ? source.width() * (double) source.height() : Core.countNonZero(mask);
  }

  private static Mat getPixelPaddingMask(
      Mat source, Mat mask, Integer paddingValue, Integer paddingLimit) {
    if (paddingValue == null || source.channels() != 1) {
      return mask;
    }

    var range = normalizePaddingRange(paddingValue, paddingLimit);
    var paddingMask = createPaddingMask(source, range.min(), range.max());

    return combineMasks(mask, paddingMask);
  }

  private record Range(int min, int max) {}

  private static Range normalizePaddingRange(Integer paddingValue, Integer paddingLimit) {
    int limit = (paddingLimit != null) ? paddingLimit : paddingValue;
    return (limit < paddingValue) ? new Range(limit, paddingValue) : new Range(paddingValue, limit);
  }

  private static Mat createPaddingMask(Mat source, int minValue, int maxValue) {
    var paddingMask = new Mat();
    Core.inRange(source, new Scalar(minValue), new Scalar(maxValue), paddingMask);
    Core.bitwise_not(paddingMask, paddingMask); // Invert: 0 for padding, 255 elsewhere
    return paddingMask;
  }

  private static Mat combineMasks(Mat existingMask, Mat paddingMask) {
    if (existingMask == null) {
      return paddingMask;
    }

    var combinedMask = new ImageCV();
    Core.bitwise_and(existingMask, paddingMask, combinedMask);
    return combinedMask;
  }
}
