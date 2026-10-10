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

import org.opencv.core.Mat;

/**
 * Random access to the samples of one DICOM multiplex group without holding the whole recording in
 * memory. Samples are returned as a single-channel {@link Mat} with one row per sample and one
 * column per channel, the order of native DICOM Waveform Data, so a window is also the input of the
 * T.261 encoder and of the usual OpenCV operations.
 *
 * <p>Implementations: {@link NativeWaveformReader} for uncompressed Waveform Data in memory or
 * memory-mapped from a file, {@link T261WaveformReader} for the T.261 transfer syntaxes of
 * Supplement 253 (decodes only the frame sequences covering a window). Readers are not thread safe;
 * use one per thread or synchronize externally.
 */
public interface WaveformReader extends AutoCloseable {

  /** Number of channels of the multiplex group. */
  int channels();

  /** Sampling frequency in Hz, 0 when unknown. */
  double samplingFrequency();

  /** Number of samples per channel. */
  long length();

  /** OpenCV type of the Mats returned by {@link #read(long, int)} (single channel). */
  int type();

  /**
   * Window size (in samples) at which reads are cheapest: the frame sequence length of a compressed
   * stream, or any value for native data. Callers scrolling through a recording should request
   * windows aligned on multiples of this size.
   */
  int preferredWindow();

  /**
   * Reads a window of samples.
   *
   * @param start first sample (per channel), 0 to {@code length() - 1}
   * @param count number of samples; clipped to the end of the recording
   * @return a new Mat (rows = samples, cols = channels) owned by the caller
   * @throws IllegalArgumentException when the window is outside the recording
   * @throws IllegalStateException when the reader is closed or the data cannot be read
   */
  Mat read(long start, int count);

  /**
   * Reads one channel of a window as doubles, a convenience for plotting and statistics.
   *
   * @param channel channel index, 0 to {@code channels() - 1}
   */
  default double[] readChannel(long start, int count, int channel) {
    if (channel < 0 || channel >= channels()) {
      throw new IllegalArgumentException("Channel out of range: " + channel);
    }
    Mat window = read(start, count);
    try {
      double[] values = new double[window.rows()];
      Mat column = window.col(channel);
      try {
        Mat asDouble = new Mat();
        try {
          column.convertTo(asDouble, org.opencv.core.CvType.CV_64F);
          asDouble.get(0, 0, values);
        } finally {
          asDouble.release();
        }
      } finally {
        column.release();
      }
      return values;
    } finally {
      window.release();
    }
  }

  @Override
  void close();
}
