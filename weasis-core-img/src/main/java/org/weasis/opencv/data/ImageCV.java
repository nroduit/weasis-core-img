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

import java.util.Objects;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.weasis.core.util.annotations.Generated;

/**
 * Enhanced Mat implementation with additional memory management features. Implements PlanarImage
 * for consistent image handling across the application.
 */
public final class ImageCV extends Mat implements PlanarImage {

  private boolean releasedAfterProcessing;
  private boolean released;

  public ImageCV() {
    super();
  }

  public ImageCV(int rows, int cols, int type) {
    super(rows, cols, type);
  }

  public ImageCV(Size size, int type) {
    super(size, type);
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

  @Override
  public long physicalBytes() {
    return total() * elemSize();
  }

  @Override
  public void release() {
    if (!released) {
      super.release();
      this.released = true;
    }
  }

  @Override
  public boolean isReleased() {
    return released;
  }

  @Override
  public boolean isReleasedAfterProcessing() {
    return releasedAfterProcessing;
  }

  @Override
  public void setReleasedAfterProcessing(boolean releasedAfterProcessing) {
    this.releasedAfterProcessing = releasedAfterProcessing;
  }

  @Override
  public void close() {
    release();
  }

  /** Creates ImageCV from Mat. Returns the source directly if already ImageCV. */
  public static ImageCV fromMat(Mat source) {
    Objects.requireNonNull(source, "Source Mat cannot be null");
    if (source instanceof ImageCV imageCV) {
      return imageCV;
    }
    var result = new ImageCV();
    source.assignTo(result);
    return result;
  }

  /** Converts PlanarImage to Mat. */
  public static Mat toMat(PlanarImage source) {
    Objects.requireNonNull(source, "Source PlanarImage cannot be null");
    return source.toMat();
  }

  // ============================== DEPRECATED FILE-BASED METHODS ==============================

  /**
   * Checks if the image has been released. This method is deprecated and should be replaced with
   * {@link #isReleased()}.
   *
   * @return false, as this method is deprecated and does not reflect the current state
   * @deprecated Use {@link #isReleased()} instead.
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  @Override
  public boolean isHasBeenReleased() {
    return isReleased();
  }

  /**
   * Converts a Mat to an ImageCV instance. This method is deprecated and should be replaced with
   * {@link #fromMat(Mat)}.
   *
   * @param source the source Mat
   * @return ImageCV instance
   * @deprecated Use {@link #fromMat(Mat)} instead.
   */
  @Deprecated(since = "4.12", forRemoval = true)
  @Generated
  public static ImageCV toImageCV(Mat source) {
    return fromMat(source);
  }
}
