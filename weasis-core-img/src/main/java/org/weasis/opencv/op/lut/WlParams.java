/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op.lut;

/**
 * Interface extending {@link WlPresentation} with comprehensive window/level parameters.
 *
 * <p>This interface defines all parameters required for window/level transformations in medical
 * image processing, including windowing values, LUT behavior, and display options.
 *
 * @author Weasis Team
 */
public interface WlParams extends WlPresentation {

  /**
   * Gets the window width for the window/level transformation.
   *
   * @return the window width value
   */
  double getWindow();

  /**
   * Gets the window center (level) for the window/level transformation.
   *
   * @return the window center value
   */
  double getLevel();

  /**
   * Gets the minimum level value allowed for this transformation.
   *
   * @return the minimum level value
   */
  double getLevelMin();

  /**
   * Gets the maximum level value allowed for this transformation.
   *
   * @return the maximum level value
   */
  double getLevelMax();

  /**
   * Determines whether the lookup table should be inverted.
   *
   * @return {@code true} if LUT inversion is enabled, {@code false} otherwise
   */
  boolean isInverseLut();

  /**
   * Determines whether values outside the LUT range should be filled.
   *
   * @return {@code true} if outside range filling is enabled, {@code false} otherwise
   */
  boolean isFillOutsideLutRange();

  /**
   * Determines whether window/level adjustments are allowed on color images.
   *
   * @return {@code true} if color image adjustments are allowed, {@code false} otherwise
   */
  boolean isAllowWinLevelOnColorImage();

  /**
   * Gets the lookup table shape function used for the transformation.
   *
   * @return the LUT shape configuration
   */
  LutShape getLutShape();
}
