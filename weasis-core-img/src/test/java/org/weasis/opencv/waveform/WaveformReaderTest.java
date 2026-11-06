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

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.weasis.opencv.natives.NativeLibrary;

@DisplayNameGeneration(ReplaceUnderscores.class)
class WaveformReaderTest {

  private static final int CHANNELS = 12;
  private static final int SAMPLES = 20000;
  private static final double FS = 500.0;

  @BeforeAll
  static void load_native_lib() {
    NativeLibrary.loadLibraryFromLibraryName();
  }

  /** ECG-like 12-bit signal, rows = samples, cols = channels, CV_16S. */
  static Mat synth(int samples, int channels, int bits, long seed) {
    Mat m = new Mat(samples, channels, CvType.CV_16S);
    Random r = new Random(seed);
    double amp = (1 << (bits - 1)) * 0.6;
    short[] row = new short[channels];
    for (int i = 0; i < samples; i++) {
      double t = i / FS;
      for (int c = 0; c < channels; c++) {
        row[c] =
            (short)
                (amp
                    * (0.7 * Math.sin(2 * Math.PI * 1.2 * t + c * 0.3)
                        + 0.2 * Math.sin(2 * Math.PI * 17 * t)
                        + 0.03 * r.nextGaussian()));
      }
      m.put(i, 0, row);
    }
    return m;
  }

  static byte[] nativeBytes(Mat m16, ByteOrder order) {
    short[] values = new short[(int) m16.total()];
    m16.get(0, 0, values);
    ByteBuffer bb = ByteBuffer.allocate(values.length * 2).order(order);
    bb.asShortBuffer().put(values);
    return bb.array();
  }

  static double maxDiff(Mat a, Mat b) {
    assertEquals(a.rows(), b.rows(), "rows");
    assertEquals(a.cols(), b.cols(), "cols");
    Mat d = new Mat();
    Core.absdiff(a, b, d);
    double max = Core.minMaxLoc(d).maxVal;
    d.release();
    return max;
  }

  static void assertWindow(WaveformReader reader, Mat reference, long start, int count) {
    Mat window = reader.read(start, count);
    try {
      int expected = (int) Math.min(count, reference.rows() - start);
      assertEquals(expected, window.rows());
      assertEquals(CHANNELS, window.cols());
      Mat slice = reference.rowRange((int) start, (int) start + expected);
      assertEquals(0.0, maxDiff(slice, window), "window [" + start + ", +" + count + ")");
      slice.release();
    } finally {
      window.release();
    }
  }

  @Test
  void native_memory_reader_reads_windows_and_clips_at_the_end() {
    Mat signal = synth(SAMPLES, CHANNELS, 12, 1);
    ByteBuffer data = ByteBuffer.wrap(nativeBytes(signal, ByteOrder.LITTLE_ENDIAN));
    try (NativeWaveformReader reader =
        NativeWaveformReader.ofMemory(data, CHANNELS, WaveformSampleInterpretation.SS, false, FS)) {
      assertEquals(CHANNELS, reader.channels());
      assertEquals(SAMPLES, reader.length());
      assertEquals(FS, reader.samplingFrequency());
      assertEquals(CvType.CV_16S, reader.type());
      assertWindow(reader, signal, 0, 100);
      assertWindow(reader, signal, 12345, 4096);
      assertWindow(reader, signal, SAMPLES - 10, 1000);
      assertWindow(reader, signal, 0, SAMPLES);
      double[] channel = reader.readChannel(1000, 5, 3);
      assertEquals(5, channel.length);
      assertEquals(signal.get(1002, 3)[0], channel[2]);
      assertThrows(IllegalArgumentException.class, () -> reader.read(SAMPLES, 10));
      assertThrows(IllegalArgumentException.class, () -> reader.read(-1, 10));
      assertThrows(IllegalArgumentException.class, () -> reader.read(0, 0));
    }
    signal.release();
  }

  @Test
  void native_file_reader_maps_windows_from_a_file_region(@TempDir Path dir) throws IOException {
    Mat signal = synth(SAMPLES, CHANNELS, 12, 2);
    byte[] bytes = nativeBytes(signal, ByteOrder.LITTLE_ENDIAN);
    Path file = dir.resolve("native.dcm");
    byte[] junk = new byte[777];
    try (OutputStream out = Files.newOutputStream(file)) {
      out.write(junk);
      out.write(bytes);
      out.write(junk);
    }
    try (NativeWaveformReader reader =
        NativeWaveformReader.ofFile(
            file,
            junk.length,
            bytes.length,
            CHANNELS,
            WaveformSampleInterpretation.SS,
            false,
            FS)) {
      assertEquals(SAMPLES, reader.length());
      assertWindow(reader, signal, 0, 4096);
      assertWindow(reader, signal, 7777, 333);
      assertWindow(reader, signal, SAMPLES - 1, 50);
    }
    signal.release();
  }

  @Test
  void native_reader_handles_big_endian_unsigned_and_8_bit_interpretations() {
    Mat signal = synth(500, CHANNELS, 12, 3);
    // big-endian signed 16
    ByteBuffer be = ByteBuffer.wrap(nativeBytes(signal, ByteOrder.BIG_ENDIAN));
    try (NativeWaveformReader reader =
        NativeWaveformReader.ofMemory(be, CHANNELS, WaveformSampleInterpretation.SS, true, FS)) {
      assertWindow(reader, signal, 0, 500);
    }
    // unsigned 16: offset the values into the positive range
    Mat unsigned = new Mat();
    Core.add(signal, new org.opencv.core.Scalar(4096), unsigned, new Mat(), CvType.CV_16U);
    short[] raw = new short[(int) unsigned.total()];
    unsigned.get(0, 0, raw);
    ByteBuffer ub = ByteBuffer.allocate(raw.length * 2).order(ByteOrder.LITTLE_ENDIAN);
    ub.asShortBuffer().put(raw);
    ub.rewind();
    try (NativeWaveformReader reader =
        NativeWaveformReader.ofMemory(ub, CHANNELS, WaveformSampleInterpretation.US, false, FS)) {
      assertEquals(CvType.CV_16U, reader.type());
      Mat window = reader.read(0, 500);
      assertEquals(0.0, maxDiff(unsigned, window));
      window.release();
    }
    // signed 8
    Mat s8 = new Mat();
    signal.convertTo(s8, CvType.CV_8S, 1.0 / 16);
    byte[] b8 = new byte[(int) s8.total()];
    s8.get(0, 0, b8);
    try (NativeWaveformReader reader =
        NativeWaveformReader.ofMemory(
            ByteBuffer.wrap(b8), CHANNELS, WaveformSampleInterpretation.SB, false, FS)) {
      assertEquals(CvType.CV_8S, reader.type());
      Mat window = reader.read(100, 100);
      Mat slice = s8.rowRange(100, 200);
      assertEquals(0.0, maxDiff(slice, window));
      slice.release();
      window.release();
    }
    assertEquals(WaveformSampleInterpretation.US, WaveformSampleInterpretation.fromDicom(" us "));
    assertThrows(
        IllegalArgumentException.class, () -> WaveformSampleInterpretation.fromDicom("MB"));
    signal.release();
  }

  @Test
  void t261_writer_items_concatenate_to_the_one_shot_stream_and_the_reader_decodes_windows() {
    Mat signal = synth(SAMPLES, CHANNELS, 12, 4);
    T261Parameters params = T261Parameters.lossless(CHANNELS, 16, 12, FS).withIntraPeriod(2048);
    Mat oneShot = Imgcodecs.dicomWaveformWrite(signal, params.toMatOfInt());
    byte[] reference = new byte[(int) oneShot.total()];
    oneShot.get(0, 0, reference);
    oneShot.release();

    List<byte[]> items = new ArrayList<>();
    ByteArrayOutputStream all = new ByteArrayOutputStream();
    try (T261WaveformWriter writer = new T261WaveformWriter(params)) {
      assertEquals(2048, writer.frameSequenceLength());
      Random r = new Random(5);
      int pos = 0;
      while (pos < SAMPLES) {
        int n = Math.min(SAMPLES - pos, 1 + r.nextInt(3000));
        Mat chunk = signal.rowRange(pos, pos + n);
        writer.write(chunk);
        pos += n;
        byte[] item;
        while ((item = writer.nextItem()) != null) {
          items.add(item);
          all.writeBytes(item);
        }
        assertTrue(writer.samplesPending() < 2048 + 3000);
      }
      byte[] last = writer.finish();
      items.add(last);
      all.writeBytes(last);
      assertThrows(IllegalStateException.class, () -> writer.write(signal.rowRange(0, 10)));
    }
    assertEquals(10, items.size());
    assertArrayEquals(reference, all.toByteArray(), "chunked stream equals the one-shot stream");

    try (T261WaveformReader reader = T261WaveformReader.ofMemory(all.toByteArray(), 0)) {
      assertEquals(CHANNELS, reader.channels());
      assertEquals(SAMPLES, reader.length());
      assertEquals(FS, reader.samplingFrequency(), "frequency carried by the stream");
      assertEquals(12, reader.bitDepth());
      assertEquals(10, reader.frameSequences());
      assertEquals(2048, reader.preferredWindow());
      assertEquals(CvType.CV_16S, reader.type());
      assertWindow(reader, signal, 0, 100);
      assertWindow(reader, signal, 2000, 100);
      assertWindow(reader, signal, 10000, 3000);
      assertWindow(reader, signal, 19000, 5000);
      assertWindow(reader, signal, 0, SAMPLES);
      assertThrows(IllegalArgumentException.class, () -> reader.read(SAMPLES, 1));
    }
    signal.release();
  }

  @Test
  void t261_file_reader_reads_items_lazily(@TempDir Path dir) throws IOException {
    Mat signal = synth(SAMPLES, CHANNELS, 12, 6);
    T261Parameters params = T261Parameters.lossless(CHANNELS, 16, 12, FS).withIntraPeriod(4096);
    List<byte[]> items = new ArrayList<>();
    int count = T261WaveformWriter.encode(nativeReader(signal), params, items::add);
    assertEquals(5, count);
    // write the items separated by junk, as in a DICOM file (Item tags and lengths)
    Path file = dir.resolve("t261.dcm");
    long[] positions = new long[items.size()];
    long[] lengths = new long[items.size()];
    try (OutputStream out = Files.newOutputStream(file)) {
      long pos = 0;
      byte[] junk = new byte[8];
      for (int i = 0; i < items.size(); i++) {
        out.write(junk);
        pos += junk.length;
        positions[i] = pos;
        lengths[i] = items.get(i).length;
        out.write(items.get(i));
        pos += lengths[i];
      }
    }
    try (T261WaveformReader reader = T261WaveformReader.ofFile(file, positions, lengths, 0)) {
      assertEquals(SAMPLES, reader.length());
      assertEquals(5, reader.frameSequences());
      assertWindow(reader, signal, 4000, 200);
      assertWindow(reader, signal, 0, SAMPLES);
      assertWindow(reader, signal, 19500, 500);
    }
    signal.release();
  }

  @Test
  void lossy_transcoding_stays_bounded_and_close_to_the_source() {
    Mat signal = synth(SAMPLES, CHANNELS, 12, 7);
    T261Parameters params =
        T261Parameters.lossy(CHANNELS, 16, 12, FS, 2.0)
            .withSignalType(T261Parameters.SignalType.ECG)
            .withIntraPeriod(4096);
    ByteArrayOutputStream all = new ByteArrayOutputStream();
    T261WaveformWriter.encode(nativeReader(signal), params, all::writeBytes);
    try (T261WaveformReader reader = T261WaveformReader.ofMemory(all.toByteArray(), 0)) {
      Mat window = reader.read(0, SAMPLES);
      assertTrue(maxDiff(signal, window) <= 2.0, "step size 2.0 bounds the error");
      window.release();
    }
    assertTrue(all.size() < SAMPLES * CHANNELS * 2 / 2, "compressed by more than 2x");
    signal.release();
  }

  @Test
  void invalid_streams_and_parameters_are_rejected() {
    assertThrows(
        IllegalArgumentException.class, () -> T261WaveformReader.ofMemory(new byte[64], 0));
    assertThrows(IllegalArgumentException.class, () -> T261Parameters.lossless(0, 16, 12, FS));
    assertThrows(IllegalArgumentException.class, () -> T261Parameters.lossless(12, 12, 12, FS));
    assertThrows(IllegalArgumentException.class, () -> T261Parameters.lossless(12, 32, 28, FS));
    Mat signal = synth(100, CHANNELS, 12, 8);
    T261Parameters wrong = T261Parameters.lossless(CHANNELS + 1, 16, 12, FS);
    assertThrows(
        IllegalArgumentException.class,
        () -> T261WaveformWriter.encode(nativeReader(signal), wrong, b -> {}));
    try (T261WaveformWriter writer =
        new T261WaveformWriter(T261Parameters.lossless(CHANNELS, 16, 12, FS))) {
      assertNull(writer.nextItem());
      assertThrows(IllegalStateException.class, writer::finish);
    }
    signal.release();
  }

  private static NativeWaveformReader nativeReader(Mat signal) {
    return NativeWaveformReader.ofMemory(
        ByteBuffer.wrap(nativeBytes(signal, ByteOrder.LITTLE_ENDIAN)),
        signal.cols(),
        WaveformSampleInterpretation.SS,
        false,
        FS);
  }
}
