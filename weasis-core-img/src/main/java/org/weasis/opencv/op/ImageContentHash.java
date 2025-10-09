/*
 * Copyright (c) 2021 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op;

import java.util.Objects;
import org.opencv.core.Mat;
import org.opencv.img_hash.AverageHash;
import org.opencv.img_hash.BlockMeanHash;
import org.opencv.img_hash.ColorMomentHash;
import org.opencv.img_hash.ImgHashBase;
import org.opencv.img_hash.MarrHildrethHash;
import org.opencv.img_hash.PHash;
import org.opencv.img_hash.RadialVarianceHash;

/**
 * Algorithms to compare image content of two images.
 *
 * <p><b>For General Purpose:</b>
 *
 * <ul>
 *   <li><b>PHash:</b> Best starting point for most applications
 *   <li><b>Average Hash:</b> When speed is more important than accuracy
 * </ul>
 *
 * <p><b>For Specific Scenarios:</b>
 *
 * <ul>
 *   <li><b>Structural similarity:</b> Marr-Hildreth Hash
 *   <li><b>Color-based similarity:</b> Color Moment Hash
 *   <li><b>Rotational invariance:</b> Radial Variance Hash
 *   <li><b>Regional analysis:</b> Block Mean Hash
 *   <li><b>Fast duplicate detection:</b> Average Hash
 * </ul>
 *
 * <p><b>Performance Considerations:</b>
 *
 * <ul>
 *   <li><b>Fastest:</b> Average Hash
 *   <li><b>Most robust:</b> PHash
 *   <li><b>Most specialized:</b> Radial Variance and Color Moment
 * </ul>
 *
 * @see <a
 *     href="http://qtandopencv.blogspot.com/2016/06/introduction-to-image-hash-module-of.html">Hash
 *     for pixel data</a>
 * @author Nicolas Roduit
 */
public enum ImageContentHash {
  /** Fast hash algorithm, ideal when speed is prioritized over accuracy. */
  AVERAGE(AverageHash::create),

  /** Perceptual hash, best general-purpose algorithm balancing accuracy and performance. */
  PHASH(PHash::create),

  /** Structural similarity hash using edge detection. */
  MARR_HILDRETH(MarrHildrethHash::create),

  /** Rotation-invariant hash based on radial variance. */
  RADIAL_VARIANCE(RadialVarianceHash::create),

  /** Block-based mean hash with mode 0 for regional analysis. */
  BLOCK_MEAN_ZERO(() -> BlockMeanHash.create(0)),

  /** Block-based mean hash with mode 1 for enhanced regional analysis. */
  BLOCK_MEAN_ONE(() -> BlockMeanHash.create(1)),

  /** Color-based hash using statistical moments. */
  COLOR_MOMENT(ColorMomentHash::create);

  private final AlgorithmFactory algorithmFactory;

  ImageContentHash(AlgorithmFactory algorithmFactory) {
    this.algorithmFactory = algorithmFactory;
  }

  /**
   * Creates a new instance of the hash algorithm.
   *
   * @return a new ImgHashBase instance
   */
  public ImgHashBase getAlgorithm() {
    return algorithmFactory.create();
  }

  /**
   * Compares two images using the hash algorithm and returns similarity score.
   *
   * @param imgIn first image to compare
   * @param imgOut second image to compare
   * @return similarity score (0.0 = identical, higher values = more different)
   * @throws IllegalArgumentException if either image is null or empty
   */
  public double compare(Mat imgIn, Mat imgOut) {
    Objects.requireNonNull(imgIn, "Input image cannot be null");
    Objects.requireNonNull(imgOut, "Output image cannot be null");

    if (imgIn.empty() || imgOut.empty()) {
      throw new IllegalArgumentException("Images cannot be empty");
    }

    var hashAlgorithm = getAlgorithm();
    var inHash = new Mat();
    var outHash = new Mat();
    hashAlgorithm.compute(imgIn, inHash);
    hashAlgorithm.compute(imgOut, outHash);
    return hashAlgorithm.compare(inHash, outHash);
  }

  @FunctionalInterface
  private interface AlgorithmFactory {
    ImgHashBase create();
  }
}
