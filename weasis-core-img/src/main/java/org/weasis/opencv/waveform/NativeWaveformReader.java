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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import org.opencv.core.Mat;

/**
 * Reader of native (uncompressed) Waveform Data (5400,1010): sample-interleaved integers as
 * described by Waveform Sample Interpretation and Waveform Bits Allocated. The data comes either
 * from memory or from a file region that is memory-mapped per window, so a multi-gigabyte recording
 * is never loaded as a whole.
 */
public final class NativeWaveformReader implements WaveformReader {

  /** Source of the raw bytes of the window [offset, offset + length). */
  private interface ByteSource extends AutoCloseable {
    ByteBuffer slice(long offset, int length);

    long size();

    @Override
    void close();
  }

  private final ByteSource source;
  private final int channels;
  private final WaveformSampleInterpretation interpretation;
  private final ByteOrder byteOrder;
  private final double samplingFrequency;
  private final long length;
  private final int frameBytes;
  private boolean closed;

  private NativeWaveformReader(
      ByteSource source,
      int channels,
      WaveformSampleInterpretation interpretation,
      boolean bigEndian,
      double samplingFrequency) {
    if (channels <= 0) {
      throw new IllegalArgumentException("The number of channels must be positive");
    }
    this.source = Objects.requireNonNull(source);
    this.channels = channels;
    this.interpretation = Objects.requireNonNull(interpretation);
    this.byteOrder = bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    this.samplingFrequency = Math.max(0.0, samplingFrequency);
    this.frameBytes = channels * interpretation.bytesPerSample();
    this.length = source.size() / frameBytes;
  }

  /**
   * Reader over Waveform Data held in memory.
   *
   * @param data the value of (5400,1010); the buffer is read from its position to its limit and is
   *     not modified
   * @param channels Number of Waveform Channels (003A,0005)
   * @param interpretation Waveform Sample Interpretation (5400,1006)
   * @param bigEndian byte order of the samples (true only for the big-endian transfer syntax)
   * @param samplingFrequency Sampling Frequency (003A,001A), 0 if unknown
   */
  public static NativeWaveformReader ofMemory(
      ByteBuffer data,
      int channels,
      WaveformSampleInterpretation interpretation,
      boolean bigEndian,
      double samplingFrequency) {
    ByteBuffer view = Objects.requireNonNull(data).slice();
    ByteSource source =
        new ByteSource() {
          @Override
          public ByteBuffer slice(long offset, int length) {
            return view.duplicate().position((int) offset).limit((int) offset + length).slice();
          }

          @Override
          public long size() {
            return view.remaining();
          }

          @Override
          public void close() {
            // nothing to release
          }
        };
    return new NativeWaveformReader(source, channels, interpretation, bigEndian, samplingFrequency);
  }

  /**
   * Reader over Waveform Data stored in a file, memory-mapped window by window.
   *
   * @param file the DICOM file
   * @param offset position of the first byte of the value of (5400,1010)
   * @param length length of the value in bytes
   */
  public static NativeWaveformReader ofFile(
      Path file,
      long offset,
      long length,
      int channels,
      WaveformSampleInterpretation interpretation,
      boolean bigEndian,
      double samplingFrequency) {
    if (offset < 0 || length < 0) {
      throw new IllegalArgumentException("Negative file region");
    }
    // The channel belongs to the source, which the returned reader closes. If the reader cannot be
    // built (invalid arguments), the source is closed here so the file handle does not leak.
    FileByteSource source = new FileByteSource(file, offset, length);
    try {
      return new NativeWaveformReader(
          source, channels, interpretation, bigEndian, samplingFrequency);
    } catch (RuntimeException e) {
      source.close();
      throw e;
    }
  }

  /** File region memory-mapped on demand; owns the channel until {@link #close()}. */
  private static final class FileByteSource implements ByteSource {
    private final Path file;
    private final long offset;
    private final long length;
    private final FileChannel channel;

    FileByteSource(Path file, long offset, long length) {
      this.file = file;
      this.offset = offset;
      this.length = length;
      try {
        this.channel = FileChannel.open(file, StandardOpenOption.READ);
      } catch (IOException e) {
        throw new UncheckedIOException("Cannot open " + file, e);
      }
    }

    @Override
    public ByteBuffer slice(long regionOffset, int sliceLength) {
      try {
        return channel.map(MapMode.READ_ONLY, offset + regionOffset, sliceLength);
      } catch (IOException e) {
        throw new UncheckedIOException("Cannot map " + file, e);
      }
    }

    @Override
    public long size() {
      return length;
    }

    @Override
    public void close() {
      try {
        channel.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  @Override
  public int channels() {
    return channels;
  }

  @Override
  public double samplingFrequency() {
    return samplingFrequency;
  }

  @Override
  public long length() {
    return length;
  }

  @Override
  public int type() {
    return interpretation.cvType();
  }

  public WaveformSampleInterpretation interpretation() {
    return interpretation;
  }

  /** Native data has no structure: any window size is as cheap as another. */
  @Override
  public int preferredWindow() {
    return 4096;
  }

  @Override
  public Mat read(long start, int count) {
    if (closed) {
      throw new IllegalStateException("Reader is closed");
    }
    if (start < 0 || start >= length || count <= 0) {
      throw new IllegalArgumentException(
          "Window [" + start + ", +" + count + ") outside the recording of " + length + " samples");
    }
    int rows = (int) Math.min(count, length - start);
    ByteBuffer bytes = source.slice(start * frameBytes, rows * frameBytes).order(byteOrder);
    Mat window = new Mat(rows, channels, interpretation.cvType());
    int values = rows * channels;
    switch (interpretation.bytesPerSample()) {
      case 1 -> {
        byte[] data = new byte[values];
        bytes.get(data);
        window.put(0, 0, data);
      }
      case 2 -> {
        short[] data = new short[values];
        bytes.asShortBuffer().get(data);
        window.put(0, 0, data);
      }
      default -> {
        int[] data = new int[values];
        bytes.asIntBuffer().get(data);
        window.put(0, 0, data);
      }
    }
    return window;
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      source.close();
    }
  }
}
