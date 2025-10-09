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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;

/**
 * Legacy ImageProcessor class providing backward compatibility.
 *
 * <p><strong>DEPRECATED:</strong> This class has been split into specialized classes for better
 * maintainability:
 *
 * <ul>
 *   <li>{@link ImageAnalyzer} - Statistical analysis and measurements
 *   <li>{@link ImageTransformer} - Geometric and visual transformations
 *   <li>{@link ImageIOHandler} - File input/output operations
 * </ul>
 *
 * <p>This class now delegates to the new specialized classes to maintain backward compatibility.
 * New code should use the specialized classes directly for better performance and clarity.
 *
 * @deprecated Use the specialized classes instead: {@link ImageAnalyzer}, {@link ImageTransformer},
 *     and {@link ImageIOHandler}
 * @author Weasis Team
 */
@Deprecated(since = "4.12", forRemoval = true)
public class ImageProcessor {

  private ImageProcessor() {}

  // ==================== ImageAnalyzer Methods ====================

  /**
   * @deprecated Use {@link ImageAnalyzer#findRawMinMaxValues(PlanarImage, boolean)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static MinMaxLocResult findRawMinMaxValues(PlanarImage img, boolean exclude8bitImage)
      throws OutOfMemoryError {
    return ImageAnalyzer.findRawMinMaxValues(img, exclude8bitImage);
  }

  /**
   * @deprecated Use {@link ImageAnalyzer#findMinMaxValues(Mat)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static MinMaxLocResult findMinMaxValues(Mat source) {
    return ImageAnalyzer.findMinMaxValues(source);
  }

  /**
   * @deprecated Use {@link ImageAnalyzer#findMinMaxValues(Mat, Integer, Integer)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static MinMaxLocResult findMinMaxValues(
      Mat source, Integer paddingValue, Integer paddingLimit) {
    return ImageAnalyzer.findMinMaxValues(source, paddingValue, paddingLimit);
  }

  /**
   * @deprecated Use {@link ImageAnalyzer#minMaxLoc(RenderedImage, Rectangle)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static MinMaxLocResult minMaxLoc(RenderedImage source, Rectangle area) {
    return ImageAnalyzer.minMaxLoc(source, area);
  }

  /**
   * @deprecated Use {@link ImageAnalyzer#minMaxLoc(Mat, Mat)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static MinMaxLocResult minMaxLoc(Mat srcImg, Mat mask) {
    return ImageAnalyzer.minMaxLoc(srcImg, mask);
  }

  /**
   * @deprecated Use {@link ImageAnalyzer#meanStdDev(Mat)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static double[][] meanStdDev(Mat source) {
    return ImageAnalyzer.meanStdDev(source);
  }

  /**
   * @deprecated Use {@link ImageAnalyzer#meanStdDev(Mat, Shape)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static double[][] meanStdDev(Mat source, Shape shape) {
    return ImageAnalyzer.meanStdDev(source, shape);
  }

  /**
   * @deprecated Use {@link ImageAnalyzer#meanStdDev(Mat, Shape, Integer, Integer)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static double[][] meanStdDev(
      Mat source, Shape shape, Integer paddingValue, Integer paddingLimit) {
    return ImageAnalyzer.meanStdDev(source, shape, paddingValue, paddingLimit);
  }

  /**
   * @deprecated Use {@link ImageAnalyzer#meanStdDev(Mat, Mat, Integer, Integer)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static double[][] meanStdDev(
      Mat source, Mat mask, Integer paddingValue, Integer paddingLimit) {
    return ImageAnalyzer.meanStdDev(source, mask, paddingValue, paddingLimit);
  }

  /**
   * @deprecated Use {@link ImageAnalyzer#transformShapeToContour(Shape, boolean)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static List<MatOfPoint> transformShapeToContour(
      Shape shape, boolean keepImageCoordinates) {
    return ImageAnalyzer.transformShapeToContour(shape, keepImageCoordinates);
  }

  /**
   * @deprecated Use {@link ImageAnalyzer#getMaskImage(Mat, Shape, Integer, Integer)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static List<Mat> getMaskImage(
      Mat source, Shape shape, Integer paddingValue, Integer paddingLimit) {
    return ImageAnalyzer.getMaskImage(source, shape, paddingValue, paddingLimit);
  }

  // ==================== ImageTransformer Methods ====================

  /**
   * @deprecated Use {@link ImageTransformer#crop(Mat, Rectangle)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV crop(Mat source, Rectangle area) {
    return ImageTransformer.crop(source, area);
  }

  /**
   * @deprecated Use {@link ImageTransformer#scale(Mat, Dimension)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV scale(Mat source, Dimension dim) {
    return ImageTransformer.scale(source, dim);
  }

  /**
   * @deprecated Use {@link ImageTransformer#scale(Mat, Dimension, Integer)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV scale(Mat source, Dimension dim, Integer interpolation) {
    return ImageTransformer.scale(source, dim, interpolation);
  }

  /**
   * @deprecated Use {@link ImageTransformer#applyLUT(Mat, byte[][])} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV applyLUT(Mat source, byte[][] lut) {
    return ImageTransformer.applyLUT(source, lut);
  }

  /**
   * @deprecated Use {@link ImageTransformer#rescaleToByte(Mat, double, double)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV rescaleToByte(Mat source, double alpha, double beta) {
    return ImageTransformer.rescaleToByte(source, alpha, beta);
  }

  /**
   * @deprecated Use {@link ImageTransformer#invertLUT(ImageCV)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV invertLUT(ImageCV source) {
    return ImageTransformer.invertLUT(source);
  }

  /**
   * @deprecated Use {@link ImageTransformer#bitwiseAnd(Mat, int)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV bitwiseAnd(Mat source, int src2Cst) {
    return ImageTransformer.bitwiseAnd(source, src2Cst);
  }

  /**
   * @deprecated Use {@link ImageTransformer#getRotatedImage(Mat, int)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV getRotatedImage(Mat source, int rotateCvType) {
    return ImageTransformer.getRotatedImage(source, rotateCvType);
  }

  /**
   * @deprecated Use {@link ImageTransformer#flip(Mat, int)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV flip(Mat source, int flipCvType) {
    return ImageTransformer.flip(source, flipCvType);
  }

  /**
   * @deprecated Use {@link ImageTransformer#warpAffine(Mat, Mat, Size, Integer)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV warpAffine(Mat source, Mat matrix, Size boxSize, Integer interpolation) {
    return ImageTransformer.warpAffine(source, matrix, boxSize, interpolation);
  }

  /**
   * @deprecated Use {@link ImageTransformer#mergeImages(Mat, Mat, double, double)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV mergeImages(Mat source1, Mat source2, double opacity1, double opacity2) {
    return ImageTransformer.mergeImages(source1, source2, opacity1, opacity2);
  }

  /**
   * @deprecated Use {@link ImageTransformer#overlay(Mat, Mat, Color)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV overlay(Mat source, Mat imgOverlay, Color color) {
    return ImageTransformer.overlay(source, imgOverlay, color);
  }

  /**
   * @deprecated Use {@link ImageTransformer#overlay(Mat, RenderedImage, Color)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV overlay(Mat source, RenderedImage imgOverlay, Color color) {
    return ImageTransformer.overlay(source, imgOverlay, color);
  }

  /**
   * @deprecated Use {@link ImageTransformer#drawShape(RenderedImage, Shape, Color)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static BufferedImage drawShape(RenderedImage source, Shape shape, Color color) {
    return ImageTransformer.drawShape(source, shape, color);
  }

  /**
   * @deprecated Use {@link ImageTransformer#applyCropMask(Mat, Rectangle, double)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV applyCropMask(Mat source, Rectangle b, double alpha) {
    return ImageTransformer.applyCropMask(source, b, alpha);
  }

  /**
   * @deprecated Use {@link ImageTransformer#applyShutter(Mat, Shape, Color)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV applyShutter(Mat source, Shape shape, Color color) {
    return ImageTransformer.applyShutter(source, shape, color);
  }

  /**
   * @deprecated Use {@link ImageTransformer#applyShutter(Mat, RenderedImage, Color)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV applyShutter(Mat source, RenderedImage imgOverlay, Color color) {
    return ImageTransformer.applyShutter(source, imgOverlay, color);
  }

  // ==================== ImageIO Methods ====================

  /**
   * @deprecated Use {@link ImageIOHandler#readImage(Path, List)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV readImage(File file, List<String> tags) {
    return ImageIOHandler.readImage(file.toPath(), tags);
  }

  /**
   * @deprecated Use {@link ImageIOHandler#readImageWithCvException(Path, List)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV readImageWithCvException(File file, List<String> tags) {
    return ImageIOHandler.readImageWithCvException(file.toPath(), tags);
  }

  /**
   * @deprecated Use {@link ImageIOHandler#writeImage(Mat, Path)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static boolean writeImage(Mat source, File file) {
    return ImageIOHandler.writeImage(source, file.toPath());
  }

  /**
   * @deprecated Use {@link ImageIOHandler#writeImage(RenderedImage, Path)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static boolean writeImage(RenderedImage source, File file) {
    return ImageIOHandler.writeImage(source, file.toPath());
  }

  /**
   * @deprecated Use {@link ImageIOHandler#writeImage(Mat, Path, MatOfInt)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static boolean writeImage(Mat source, File file, MatOfInt params) {
    return ImageIOHandler.writeImage(source, file.toPath(), params);
  }

  /**
   * @deprecated Use {@link ImageIOHandler#writePNG(Mat, Path)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static boolean writePNG(Mat source, File file) {
    return ImageIOHandler.writePNG(source, file.toPath());
  }

  /**
   * @deprecated Use {@link ImageIOHandler#writeThumbnail(Mat, Path, int)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static boolean writeThumbnail(Mat source, File file, int maxSize) {
    return ImageIOHandler.writeThumbnail(source, file.toPath(), maxSize);
  }

  /**
   * @deprecated Use {@link ImageIOHandler#buildThumbnail(PlanarImage, Dimension, boolean)} instead
   */
  @Deprecated(since = "4.12", forRemoval = true)
  public static ImageCV buildThumbnail(PlanarImage source, Dimension iconDim, boolean keepRatio) {
    return ImageIOHandler.buildThumbnail(source, iconDim, keepRatio);
  }
}
