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

import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;

/**
 * Encoding parameters of the T.261 waveform codec (DICOM Supplement 253), mapped to the {@code
 * DICOM_WF_PARAM_*} vector of the native library.
 *
 * @param channels Number of Waveform Channels (003A,0005)
 * @param bitsAllocated Waveform Bits Allocated (5400,1004): 8, 16 or 32
 * @param bitsStored Waveform Bits Stored (5400,1005), 1 to 24; 0 = derived from bits allocated
 * @param samplingFrequency Sampling Frequency (003A,001A); the integer part goes into the stream
 * @param lossless true for the lossless transfer syntax, false for the lossy one
 * @param stepSize quantization step size of the lossy mode (1.0 = lossless), 0 = preset default
 * @param signalType encoder preset
 * @param intraPeriod frame sequence length in samples (random-access granularity), 0 = preset
 *     default (98304); a few seconds of signal costs under 3% of compression
 * @param profileLevel T.261 Main profile level to enforce (1 to 11), 0 = none
 */
public record T261Parameters(
    int channels,
    int bitsAllocated,
    int bitsStored,
    double samplingFrequency,
    boolean lossless,
    double stepSize,
    SignalType signalType,
    int intraPeriod,
    int profileLevel) {

  /** Encoder presets of the reference software; ECG also serves PPG. */
  public enum SignalType {
    ECG(0),
    EEG(1),
    EMG(2);

    private final int code;

    SignalType(int code) {
      this.code = code;
    }

    public int code() {
      return code;
    }
  }

  public T261Parameters {
    if (channels <= 0 || channels > 1024) {
      throw new IllegalArgumentException("Channels must be 1..1024: " + channels);
    }
    if (bitsAllocated != 8 && bitsAllocated != 16 && bitsAllocated != 32) {
      throw new IllegalArgumentException("Bits allocated must be 8, 16 or 32: " + bitsAllocated);
    }
    if (bitsStored < 0 || bitsStored > 24 || bitsStored > bitsAllocated) {
      throw new IllegalArgumentException("Bits stored must be 1..24 and <= bits allocated");
    }
    if (stepSize < 0) {
      throw new IllegalArgumentException("Negative step size");
    }
    if (intraPeriod < 0 || profileLevel < 0 || profileLevel > 11) {
      throw new IllegalArgumentException("Invalid intra period or profile level");
    }
    if (signalType == null) {
      signalType = SignalType.ECG;
    }
  }

  /** Lossless parameters with the default preset and frame sequence length. */
  public static T261Parameters lossless(
      int channels, int bitsAllocated, int bitsStored, double samplingFrequency) {
    return new T261Parameters(
        channels, bitsAllocated, bitsStored, samplingFrequency, true, 0, SignalType.ECG, 0, 0);
  }

  /** Lossy parameters with the given quantization step size (above 1.0). */
  public static T261Parameters lossy(
      int channels, int bitsAllocated, int bitsStored, double samplingFrequency, double stepSize) {
    return new T261Parameters(
        channels,
        bitsAllocated,
        bitsStored,
        samplingFrequency,
        false,
        stepSize,
        SignalType.ECG,
        0,
        0);
  }

  public T261Parameters withSignalType(SignalType type) {
    return new T261Parameters(
        channels,
        bitsAllocated,
        bitsStored,
        samplingFrequency,
        lossless,
        stepSize,
        type,
        intraPeriod,
        profileLevel);
  }

  public T261Parameters withIntraPeriod(int samples) {
    return new T261Parameters(
        channels,
        bitsAllocated,
        bitsStored,
        samplingFrequency,
        lossless,
        stepSize,
        signalType,
        samples,
        profileLevel);
  }

  /** The parameter vector expected by the native library (see {@code DicomWaveformParams}). */
  public MatOfInt toMatOfInt() {
    int[] values = new int[Imgcodecs.DICOM_WF_PARAM_INTRA_PERIOD + 1];
    values[Imgcodecs.DICOM_WF_PARAM_CHANNELS] = channels;
    values[Imgcodecs.DICOM_WF_PARAM_BITS_ALLOCATED] = bitsAllocated;
    values[Imgcodecs.DICOM_WF_PARAM_BITS_STORED] = bitsStored;
    values[Imgcodecs.DICOM_WF_PARAM_SAMPLING_FREQUENCY] = (int) Math.floor(samplingFrequency);
    values[Imgcodecs.DICOM_WF_PARAM_LOSSLESS] = lossless ? 1 : 0;
    values[Imgcodecs.DICOM_WF_PARAM_STEP_SIZE] = lossless ? 0 : (int) Math.round(stepSize * 1000);
    values[Imgcodecs.DICOM_WF_PARAM_SIGNAL_TYPE] = signalType.code();
    values[Imgcodecs.DICOM_WF_PARAM_PROFILE_LEVEL] = profileLevel;
    values[Imgcodecs.DICOM_WF_PARAM_INTRA_PERIOD] = intraPeriod;
    return new MatOfInt(values);
  }
}
