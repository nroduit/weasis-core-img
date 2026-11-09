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

import java.util.Objects;
import java.util.function.Consumer;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.DicomWaveformWriter;

/**
 * Streaming T.261 encoder (DICOM Supplement 253) backed by the native {@link DicomWaveformWriter}:
 * samples are pushed in chunks of any size, each completed frame sequence comes out as one byte
 * array suited to be one Item of Waveform Data (5400,1010). Memory stays at one frame sequence of
 * samples plus the encoder state, whatever the recording length. The concatenation of the items is
 * byte-identical to the one-shot {@code Imgcodecs.dicomWaveformWrite}.
 */
public final class T261WaveformWriter implements AutoCloseable {

  private final DicomWaveformWriter writer;
  private final T261Parameters parameters;
  private boolean finished;

  /**
   * Starts a stream.
   *
   * @throws IllegalArgumentException when the parameters are rejected by the codec
   */
  public T261WaveformWriter(T261Parameters parameters) {
    this.parameters = Objects.requireNonNull(parameters);
    this.writer = new DicomWaveformWriter();
    MatOfInt params = parameters.toMatOfInt();
    try {
      if (!writer.open(params)) {
        throw new IllegalArgumentException("Cannot open the T.261 encoder with " + parameters);
      }
    } finally {
      params.release();
    }
  }

  public T261Parameters parameters() {
    return parameters;
  }

  /** Frame sequence length used by the encoder (the random-access granularity). */
  public int frameSequenceLength() {
    return writer.frameSequenceLength();
  }

  public long samplesWritten() {
    return writer.samplesWritten();
  }

  public long samplesPending() {
    return writer.samplesPending();
  }

  /**
   * Appends samples: rows = samples, cols = channels, CV_8S, CV_16S or CV_32S.
   *
   * @throws IllegalStateException after {@link #finish()}
   */
  public void write(Mat samples) {
    checkNotFinished();
    if (!writer.write(samples)) {
      throw new IllegalStateException("The T.261 encoder refused the samples");
    }
  }

  /**
   * Encodes the next complete frame sequence if enough samples are pending.
   *
   * @return its packets, or null when fewer than a frame sequence is pending
   */
  public byte[] nextItem() {
    checkNotFinished();
    Mat packets = new Mat();
    try {
      return writer.nextItem(packets) ? toBytes(packets) : null;
    } finally {
      packets.release();
    }
  }

  /**
   * Encodes the remaining samples and terminates the stream.
   *
   * @return the last item: remaining frames (if any), the segment metadata and the WSS_END packet
   * @throws IllegalStateException when nothing was written
   */
  public byte[] finish() {
    checkNotFinished();
    finished = true;
    Mat packets = new Mat();
    try {
      if (!writer.finish(packets)) {
        throw new IllegalStateException("Nothing to finish: no sample was written");
      }
      return toBytes(packets);
    } finally {
      packets.release();
    }
  }

  @Override
  public void close() {
    writer.close();
  }

  /**
   * Encodes a whole recording window by window, bounded in memory by one window plus one frame
   * sequence: the typical transcoding loop (native to T.261, T.261 to T.261 with other parameters).
   *
   * @param source the samples; windows of {@code source.preferredWindow()} are read
   * @param parameters encoder parameters; its channel count must match the source
   * @param itemSink receives each item in order (write it as one DICOM Item)
   * @return the number of items produced
   */
  public static int encode(
      WaveformReader source, T261Parameters parameters, Consumer<byte[]> itemSink) {
    if (source.channels() != parameters.channels()) {
      throw new IllegalArgumentException(
          "Source has " + source.channels() + " channels, parameters say " + parameters.channels());
    }
    int items = 0;
    try (T261WaveformWriter writer = new T261WaveformWriter(parameters)) {
      int window = Math.max(1, source.preferredWindow());
      long length = source.length();
      for (long start = 0; start < length; start += window) {
        Mat chunk = source.read(start, (int) Math.min(window, length - start));
        try {
          writer.write(chunk);
        } finally {
          chunk.release();
        }
        byte[] item;
        while ((item = writer.nextItem()) != null) {
          itemSink.accept(item);
          items++;
        }
      }
      itemSink.accept(writer.finish());
      items++;
    }
    return items;
  }

  private void checkNotFinished() {
    if (finished) {
      throw new IllegalStateException("The stream is finished");
    }
  }

  private static byte[] toBytes(Mat packets) {
    byte[] bytes = new byte[(int) packets.total()];
    packets.get(0, 0, bytes);
    return bytes;
  }
}
