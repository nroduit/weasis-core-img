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

import org.weasis.core.util.annotations.Generated;

/**
 * Immutable record representing comprehensive lookup table transformation parameters.
 *
 * <p>This record encapsulates all parameters required for DICOM-compliant image pixel value
 * transformations, including:
 *
 * <ul>
 *   <li>Linear transformation coefficients (intercept and slope)
 *   <li>Pixel padding configuration and values
 *   <li>Bit depth and signedness specifications for input and output
 *   <li>Modality LUT padding inversion settings
 * </ul>
 *
 * <p>The transformation follows the DICOM standard formula: {@code output = input * slope +
 * intercept}
 *
 * <p>Padding values are handled according to DICOM PS3.3 specifications, where pixels matching the
 * padding range are excluded from display calculations.
 *
 * @param intercept the intercept value for linear transformation (DICOM tag 0028,1052)
 * @param slope the slope value for linear transformation (DICOM tag 0028,1053)
 * @param applyPadding whether pixel padding should be applied during transformation
 * @param paddingMinValue the minimum padding value, or {@code null} if not specified
 * @param paddingMaxValue the maximum padding value, or {@code null} if not specified
 * @param bitsStored the number of bits used to store each pixel value (1-32)
 * @param signed whether the input pixel values are signed
 * @param outputSigned whether the output pixel values should be signed
 * @param bitsOutput the number of bits in the output pixel values (1-32)
 * @param inversePaddingMLUT whether to inverse padding for modality LUT
 * @author Nicolas Roduit
 */
@Generated
public record LutParameters(
    double intercept,
    double slope,
    boolean applyPadding,
    Integer paddingMinValue,
    Integer paddingMaxValue,
    int bitsStored,
    boolean signed,
    boolean outputSigned,
    int bitsOutput,
    boolean inversePaddingMLUT) {

  /** Compact constructor with validation. */
  public LutParameters {
    if (bitsStored < 1 || bitsStored > 32) {
      throw new IllegalArgumentException("bitsStored must be between 1 and 32, got: " + bitsStored);
    }
    if (bitsOutput < 1 || bitsOutput > 32) {
      throw new IllegalArgumentException("bitsOutput must be between 1 and 32, got: " + bitsOutput);
    }
  }
}
