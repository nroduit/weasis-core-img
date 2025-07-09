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
 * Interface for planar image objects that wrap OpenCV Mat functionality. Extends ImageSize and
 * AutoCloseable for proper resource management.
 */
@Generated
public interface PlanarImage extends ImageSize, AutoCloseable {

  // Core Mat properties
  int channels();

  int dims();

  int depth();

  long elemSize();

  long elemSize1();

  Size size();

  int type();

  int height();

  int width();

  // Data access methods
  double[] get(int row, int column);

  int get(int i, int j, byte[] pixelData);

  int get(int i, int j, short[] data);

  int get(int i, int j, int[] data);

  int get(int i, int j, float[] data);

  int get(int i, int j, double[] data);

  // Image operations
  void assignTo(Mat dstImg);

  void release();

  // Resource management state
  boolean isReleased();

  boolean isReleasedAfterProcessing();

  void setReleasedAfterProcessing(boolean releasedAfterProcessing);

  @Override
  void close();

  /**
   * Converts this PlanarImage to a Mat instance.
   *
   * @return Mat instance
   * @throws UnsupportedOperationException if conversion is not supported
   */
  default Mat toMat() {
    if (this instanceof Mat mat) {
      return mat;
    }
    throw new UnsupportedOperationException(
        "Conversion to Mat not supported for this implementation");
  }

  /**
   * Converts this PlanarImage to an ImageCV instance.
   *
   * @return ImageCV instance
   * @throws UnsupportedOperationException if conversion is not supported
   */
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
   * @deprecated Use {@link #isReleased()} instead. This method name was inconsistent with naming
   *     conventions.
   */
  @Deprecated(since = "4.12", forRemoval = true)
  boolean isHasBeenReleased();
}
