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

import java.nio.file.Path;
import java.util.Objects;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgcodecs.DicomWaveformReader;

/**
 * Reader of a Waveform Data Stream compressed with the T.261 codec (DICOM Supplement 253, lossless
 * and lossy transfer syntaxes), backed by the native {@link DicomWaveformReader}: the packet
 * headers are indexed once, then only the frame sequences covering a window are decoded (the last
 * ones are cached), so a long recording costs one frame sequence of decoding per jump and a copy
 * otherwise.
 */
public final class T261WaveformReader implements WaveformReader {

  private final DicomWaveformReader reader;
  private final double samplingFrequency;
  private final int preferredWindow;
  private boolean closed;

  private T261WaveformReader(DicomWaveformReader reader, double samplingFrequencyFallback) {
    this.reader = reader;
    double fs = reader.samplingFrequency();
    this.samplingFrequency = fs > 0 ? fs : Math.max(0.0, samplingFrequencyFallback);
    int window = 0;
    for (int i = 0; i < reader.numFrameSequences(); i++) {
      window = Math.max(window, reader.frameSequenceLength(i));
    }
    this.preferredWindow = window > 0 ? window : 4096;
  }

  /**
   * Opens a stream held in memory (the concatenated Item values of the multiplex group).
   *
   * @param stream the T.261 stream packets; copied by the native reader
   * @param samplingFrequency Sampling Frequency (003A,001A), used when the stream does not carry
   *     one (0 if unknown)
   * @throws IllegalArgumentException when the stream cannot be parsed
   */
  public static T261WaveformReader ofMemory(byte[] stream, double samplingFrequency) {
    Objects.requireNonNull(stream);
    Mat buf = new Mat(1, stream.length, CvType.CV_8U);
    try {
      buf.put(0, 0, stream);
      DicomWaveformReader reader = new DicomWaveformReader();
      if (!reader.open(buf)) {
        throw new IllegalArgumentException("Not a decodable T.261 waveform stream");
      }
      return new T261WaveformReader(reader, samplingFrequency);
    } finally {
      buf.release();
    }
  }

  /**
   * Opens a stream stored as Items of Waveform Data (5400,1010) in a DICOM file; the Items are read
   * lazily, never as a whole.
   *
   * @param file the DICOM file
   * @param itemPositions byte position of each Item value (excluding the Item tag and length)
   * @param itemLengths byte length of each Item value
   */
  public static T261WaveformReader ofFile(
      Path file, long[] itemPositions, long[] itemLengths, double samplingFrequency) {
    if (itemPositions.length != itemLengths.length || itemPositions.length == 0) {
      throw new IllegalArgumentException(
          "Item positions and lengths must be non-empty and equal in size");
    }
    double[] positions = new double[itemPositions.length];
    double[] lengths = new double[itemLengths.length];
    for (int i = 0; i < positions.length; i++) {
      positions[i] = itemPositions[i];
      lengths[i] = itemLengths[i];
    }
    DicomWaveformReader reader = new DicomWaveformReader();
    if (!reader.openFile(file.toString(), new MatOfDouble(positions), new MatOfDouble(lengths))) {
      throw new IllegalArgumentException("Not a decodable T.261 waveform stream: " + file);
    }
    return new T261WaveformReader(reader, samplingFrequency);
  }

  @Override
  public int channels() {
    return reader.channels();
  }

  @Override
  public double samplingFrequency() {
    return samplingFrequency;
  }

  @Override
  public long length() {
    return reader.numSamples();
  }

  /** Coded bit depth of the stream (4, 8, 12, 16, 20, 24 or 28). */
  public int bitDepth() {
    return reader.bitDepth();
  }

  @Override
  public int type() {
    return reader.bitDepth() <= 16 ? CvType.CV_16S : CvType.CV_32S;
  }

  /** The longest frame sequence of the stream: windows aligned on it decode exactly once. */
  @Override
  public int preferredWindow() {
    return preferredWindow;
  }

  public int frameSequences() {
    return reader.numFrameSequences();
  }

  public long frameSequenceStart(int index) {
    return reader.frameSequenceStart(index);
  }

  public int frameSequenceLength(int index) {
    return reader.frameSequenceLength(index);
  }

  @Override
  public Mat read(long start, int count) {
    if (closed) {
      throw new IllegalStateException("Reader is closed");
    }
    long length = reader.numSamples();
    if (start < 0 || start >= length || count <= 0) {
      throw new IllegalArgumentException(
          "Window [" + start + ", +" + count + ") outside the recording of " + length + " samples");
    }
    Mat window = new Mat();
    if (!reader.read(start, count, window)) {
      window.release();
      throw new IllegalStateException("T.261 decoding failed at sample " + start);
    }
    return window;
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      reader.close();
    }
  }
}
