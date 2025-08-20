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

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.weasis.core.util.annotations.Generated;

/**
 * Represents a planar image with OpenCV Mat functionality. Provides resource management through
 * AutoCloseable and memory size calculation through ImageSize.
 */
@Generated
public interface PlanarImage extends ImageSize, AutoCloseable {

  // Core properties
  int channels();

  int dims();

  int depth();

  long elemSize();

  long elemSize1();

  Size size();

  int type();

  int height();

  int width();

  // Data access
  double[] get(int row, int column);

  int get(int i, int j, byte[] pixelData);

  int get(int i, int j, short[] data);

  int get(int i, int j, int[] data);

  int get(int i, int j, float[] data);

  int get(int i, int j, double[] data);

  // Operations
  void assignTo(Mat dstImg);

  void release();

  // Resource management
  boolean isReleased();

  boolean isReleasedAfterProcessing();

  void setReleasedAfterProcessing(boolean releasedAfterProcessing);

  @Override
  void close();

  /** Converts this PlanarImage to a Mat instance. */
  default Mat toMat() {
    if (this instanceof Mat mat) {
      return mat;
    }
    throw new UnsupportedOperationException(
        "Conversion to Mat not supported for this implementation");
  }

  /** Converts this PlanarImage to an ImageCV instance. */
  default ImageCV toImageCV() {
    if (this instanceof ImageCV imageCV) {
      return imageCV;
    }
    if (this instanceof Mat mat) {
      return ImageCV.fromMat(mat);
    }
    throw new UnsupportedOperationException(
        "Conversion to ImageCV not supported for this implementation");
  }

  // ============================== DEPRECATED FILE-BASED METHODS ==============================

  /**
   * @deprecated Use {@link #isReleased()} instead.
   */
  @Deprecated(since = "4.12", forRemoval = true)
  boolean isHasBeenReleased();
}
