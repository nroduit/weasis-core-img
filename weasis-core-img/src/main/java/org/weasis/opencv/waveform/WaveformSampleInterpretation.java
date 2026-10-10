/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.waveform;

import org.opencv.core.CvType;

/**
 * Waveform Sample Interpretation (5400,1006) of native DICOM Waveform Data, with the OpenCV type
 * used to hold the samples. {@code MB} and {@code AB} (mu-law / A-law) are not supported.
 */
public enum WaveformSampleInterpretation {
  /** Signed 8 bit linear. */
  SB(8, true, CvType.CV_8S),
  /** Unsigned 8 bit linear. */
  UB(8, false, CvType.CV_8U),
  /** Signed 16 bit linear. */
  SS(16, true, CvType.CV_16S),
  /** Unsigned 16 bit linear. */
  US(16, false, CvType.CV_16U),
  /** Signed 32 bit linear. */
  SL(32, true, CvType.CV_32S),
  /**
   * Unsigned 32 bit linear, held in a signed 32-bit Mat: values above 2^31 - 1 wrap around, since
   * OpenCV has no unsigned 32-bit type.
   */
  UL(32, false, CvType.CV_32S);

  private final int bitsAllocated;
  private final boolean signed;
  private final int cvType;

  WaveformSampleInterpretation(int bitsAllocated, boolean signed, int cvType) {
    this.bitsAllocated = bitsAllocated;
    this.signed = signed;
    this.cvType = cvType;
  }

  /** Waveform Bits Allocated (5400,1004) of this interpretation: 8, 16 or 32. */
  public int bitsAllocated() {
    return bitsAllocated;
  }

  public boolean isSigned() {
    return signed;
  }

  /** OpenCV type of a single-channel Mat holding these samples. */
  public int cvType() {
    return cvType;
  }

  public int bytesPerSample() {
    return bitsAllocated / 8;
  }

  /**
   * Returns the interpretation of a DICOM Defined Term.
   *
   * @throws IllegalArgumentException for an unknown or unsupported term (including MB and AB)
   */
  public static WaveformSampleInterpretation fromDicom(String definedTerm) {
    if (definedTerm == null) {
      throw new IllegalArgumentException("Waveform Sample Interpretation is required");
    }
    String term = definedTerm.trim().toUpperCase();
    for (WaveformSampleInterpretation value : values()) {
      if (value.name().equals(term)) {
        return value;
      }
    }
    throw new IllegalArgumentException(
        "Unsupported Waveform Sample Interpretation: " + definedTerm);
  }
}
