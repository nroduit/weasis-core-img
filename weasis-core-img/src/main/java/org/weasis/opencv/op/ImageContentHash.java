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
  AVERAGE() {
    @Override
    public ImgHashBase getAlgorithm() {
      return AverageHash.create();
    }
  },
  PHASH() {
    @Override
    public ImgHashBase getAlgorithm() {
      return PHash.create();
    }
  },
  MARR_HILDRETH() {
    @Override
    public ImgHashBase getAlgorithm() {
      return MarrHildrethHash.create();
    }
  },
  RADIAL_VARIANCE() {
    @Override
    public ImgHashBase getAlgorithm() {
      return RadialVarianceHash.create();
    }
  },
  BLOCK_MEAN_ZERO() {
    @Override
    public ImgHashBase getAlgorithm() {
      return BlockMeanHash.create(0);
    }
  },
  BLOCK_MEAN_ONE() {
    @Override
    public ImgHashBase getAlgorithm() {
      return BlockMeanHash.create(1);
    }
  },
  COLOR_MOMENT() {
    @Override
    public ImgHashBase getAlgorithm() {
      return ColorMomentHash.create();
    }
  };

  public abstract ImgHashBase getAlgorithm();

  public double compare(Mat imgIn, Mat imgOut) {
    ImgHashBase hashAlgorithm = getAlgorithm();
    Mat inHash = new Mat();
    Mat outHash = new Mat();
    hashAlgorithm.compute(imgIn, inHash);
    hashAlgorithm.compute(imgOut, outHash);
    return hashAlgorithm.compare(inHash, outHash);
  }
}
