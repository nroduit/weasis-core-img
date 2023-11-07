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
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

public class ImageCV extends Mat implements PlanarImage {

  private boolean releasedAfterProcessing;
  private boolean hasBeenReleased = false;

  public ImageCV() {
    super();
  }

  public ImageCV(int rows, int cols, int type) {
    super(rows, cols, type);
  }

  public ImageCV(Size size, int type, Scalar s) {
    super(size, type, s);
  }

  public ImageCV(int rows, int cols, int type, Scalar s) {
    super(rows, cols, type, s);
  }

  public ImageCV(Mat m, Range rowRange, Range colRange) {
    super(m, rowRange, colRange);
  }

  public ImageCV(Mat m, Range rowRange) {
    super(m, rowRange);
  }

  public ImageCV(Mat m, Rect roi) {
    super(m, roi);
  }

  public ImageCV(Size size, int type) {
    super(size, type);
  }

  @Override
  public long physicalBytes() {
    return total() * elemSize();
  }

  public static Mat toMat(PlanarImage source) {
    if (source instanceof Mat mat) {
      return mat;
    } else {
      throw new IllegalAccessError("Not implemented yet");
    }
  }

  public static ImageCV toImageCV(Mat source) {
    if (source instanceof ImageCV img) {
      return img;
    }
    ImageCV dstImg = new ImageCV();
    source.assignTo(dstImg);
    return dstImg;
  }

  @Override
  public void release() {
    if (!hasBeenReleased) {
      super.release();
      this.hasBeenReleased = true;
    }
  }

  public boolean isHasBeenReleased() {
    return hasBeenReleased;
  }

  public boolean isReleasedAfterProcessing() {
    return releasedAfterProcessing;
  }

  public void setReleasedAfterProcessing(boolean releasedAfterProcessing) {
    this.releasedAfterProcessing = releasedAfterProcessing;
  }

  @Override
  public void close() {
    release();
  }
}
