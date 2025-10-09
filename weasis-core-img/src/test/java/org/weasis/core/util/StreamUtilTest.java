/*
 * Copyright (c) 2025 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.stream.ImageInputStream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class StreamUtilTest {

  @TempDir Path tempDir;

  private static final String SAMPLE_TEXT = "Hello, StreamUtil Test!";
  private static final byte[] SAMPLE_BYTES = SAMPLE_TEXT.getBytes();
  private static final byte[] LARGE_DATA = createLargeTestData();

  private static byte[] createLargeTestData() {
    var data = new byte[100_000]; // 100KB
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (i % 256);
    }
    return data;
  }

  @Nested
  class SafeCloseAutoCloseableTests {

    @Test
    void should_safely_close_autocloseable_resource() throws Exception {
      var resource = mock(AutoCloseable.class);
      StreamUtil.safeClose(resource);
      verify(resource).close();
    }

    @Test
    void should_handle_null_autocloseable_gracefully() {
      assertDoesNotThrow(() -> StreamUtil.safeClose((AutoCloseable) null));
    }

    @Test
    void should_handle_autocloseable_close_exception_gracefully() throws Exception {
      var resource = mock(AutoCloseable.class);
      doThrow(new IOException("Close failed")).when(resource).close();

      assertDoesNotThrow(() -> StreamUtil.safeClose(resource));
      verify(resource).close();
    }

    @Test
    void should_close_multiple_autocloseable_resources() throws Exception {
      var resources =
          new AutoCloseable[] {
            mock(AutoCloseable.class), mock(AutoCloseable.class), mock(AutoCloseable.class)
          };

      StreamUtil.safeClose(resources);

      for (var resource : resources) {
        verify(resource).close();
      }
    }

    @Test
    void should_handle_mixed_null_and_valid_resources_in_varargs() throws Exception {
      var validResource = mock(AutoCloseable.class);

      assertDoesNotThrow(() -> StreamUtil.safeClose(validResource, null, validResource));
      verify(validResource, times(2)).close();
    }

    @Test
    void should_continue_closing_remaining_resources_when_one_fails() throws Exception {
      var resource1 = mock(AutoCloseable.class);
      var resource2 = mock(AutoCloseable.class);
      var resource3 = mock(AutoCloseable.class);

      doThrow(new IOException("Close failed")).when(resource2).close();

      assertDoesNotThrow(() -> StreamUtil.safeClose(resource1, resource2, resource3));

      verify(resource1).close();
      verify(resource2).close();
      verify(resource3).close();
    }

    @Test
    void should_handle_null_varargs_array() {
      assertDoesNotThrow(() -> StreamUtil.safeClose((AutoCloseable[]) null));
    }
  }

  @Nested
  class SafeCloseXMLStreamTests {

    @Test
    void should_safely_close_xml_stream_writer() throws XMLStreamException {
      var writer = mock(XMLStreamWriter.class);
      StreamUtil.safeClose(writer);
      verify(writer).close();
    }

    @Test
    void should_handle_null_xml_stream_writer_gracefully() {
      assertDoesNotThrow(() -> StreamUtil.safeClose((XMLStreamWriter) null));
    }

    @Test
    void should_handle_xml_stream_writer_close_exception_gracefully() throws XMLStreamException {
      var writer = mock(XMLStreamWriter.class);
      doThrow(new XMLStreamException("Close failed")).when(writer).close();

      assertDoesNotThrow(() -> StreamUtil.safeClose(writer));
      verify(writer).close();
    }

    @Test
    void should_safely_close_xml_stream_reader() throws XMLStreamException {
      var reader = mock(XMLStreamReader.class);
      StreamUtil.safeClose(reader);
      verify(reader).close();
    }

    @Test
    void should_handle_null_xml_stream_reader_gracefully() {
      assertDoesNotThrow(() -> StreamUtil.safeClose((XMLStreamReader) null));
    }

    @Test
    void should_handle_xml_stream_reader_close_exception_gracefully() throws XMLStreamException {
      var reader = mock(XMLStreamReader.class);
      doThrow(new XMLStreamException("Close failed")).when(reader).close();

      assertDoesNotThrow(() -> StreamUtil.safeClose(reader));
      verify(reader).close();
    }
  }

  @Nested
  class StreamCopyTests {

    @Test
    void should_copy_data_from_valid_streams() throws IOException {
      var input = new ByteArrayInputStream(SAMPLE_BYTES);
      var output = new ByteArrayOutputStream();

      var copiedBytes = StreamUtil.copy(input, output);

      assertEquals(SAMPLE_BYTES.length, copiedBytes);
      assertArrayEquals(SAMPLE_BYTES, output.toByteArray());
    }

    @Test
    void should_handle_empty_input_stream() throws IOException {
      var input = new ByteArrayInputStream(new byte[0]);
      var output = new ByteArrayOutputStream();

      var copiedBytes = StreamUtil.copy(input, output);

      assertEquals(0, copiedBytes);
      assertEquals(0, output.size());
    }

    @Test
    void should_throw_exception_for_null_input_stream() {
      var output = new ByteArrayOutputStream();

      var exception =
          assertThrows(IllegalArgumentException.class, () -> StreamUtil.copy(null, output));
      assertTrue(exception.getMessage().contains("cannot be null"));
    }

    @Test
    void should_throw_exception_for_null_output_stream() {
      var input = new ByteArrayInputStream(SAMPLE_BYTES);

      var exception =
          assertThrows(IllegalArgumentException.class, () -> StreamUtil.copy(input, null));
      assertTrue(exception.getMessage().contains("cannot be null"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 100, 1024, 8192, 16384})
    void should_copy_with_various_buffer_sizes(int bufferSize) throws IOException {
      var testData = ("Buffer size test: " + bufferSize).getBytes();
      var input = new ByteArrayInputStream(testData);
      var output = new ByteArrayOutputStream();

      var copiedBytes = StreamUtil.copy(input, output, bufferSize);

      assertEquals(testData.length, copiedBytes);
      assertArrayEquals(testData, output.toByteArray());
    }

    @ParameterizedTest
    @ValueSource(ints = {-10, 0})
    void should_use_default_buffer_size_for_invalid_buffer_sizes(int invalidBufferSize)
        throws IOException {
      var input = new ByteArrayInputStream(SAMPLE_BYTES);
      var output = new ByteArrayOutputStream();

      var copiedBytes = StreamUtil.copy(input, output, invalidBufferSize);

      assertEquals(SAMPLE_BYTES.length, copiedBytes);
      assertArrayEquals(SAMPLE_BYTES, output.toByteArray());
    }

    @Test
    void should_handle_large_data_transfer() throws IOException {
      var input = new ByteArrayInputStream(LARGE_DATA);
      var output = new ByteArrayOutputStream();

      var copiedBytes = StreamUtil.copy(input, output);

      assertEquals(LARGE_DATA.length, copiedBytes);
      assertArrayEquals(LARGE_DATA, output.toByteArray());
    }

//    @Test
//    void should_flush_output_stream_after_copy() throws IOException {
//      var input = new ByteArrayInputStream(SAMPLE_BYTES);
//      var output = mock(new ByteArrayOutputStream());
//
//      StreamUtil.copy(input, output);
//
//      verify(output).flush();
//    }
  }

  @Nested
  class CopyToFileTests {

    @Test
    void should_copy_input_stream_to_file_successfully() throws IOException {
      var input = new ByteArrayInputStream(SAMPLE_BYTES);
      var targetFile = tempDir.resolve("test-output.txt");

      var result = StreamUtil.copyToFile(input, targetFile);

      assertTrue(result);
      assertTrue(Files.exists(targetFile));
      assertArrayEquals(SAMPLE_BYTES, Files.readAllBytes(targetFile));
    }

    @Test
    void should_create_parent_directories_when_copying_to_file() throws IOException {
      var input = new ByteArrayInputStream(SAMPLE_BYTES);
      var targetFile = tempDir.resolve("subdir").resolve("nested").resolve("test-file.txt");

      var result = StreamUtil.copyToFile(input, targetFile);

      assertTrue(result);
      assertTrue(Files.exists(targetFile));
      assertTrue(Files.exists(targetFile.getParent()));
      assertArrayEquals(SAMPLE_BYTES, Files.readAllBytes(targetFile));
    }

    @Test
    void should_replace_existing_file_when_copying() throws IOException {
      var targetFile = tempDir.resolve("existing-file.txt");
      Files.writeString(targetFile, "Original content");

      var newData = "New content".getBytes();
      var input = new ByteArrayInputStream(newData);

      var result = StreamUtil.copyToFile(input, targetFile);

      assertTrue(result);
      assertArrayEquals(newData, Files.readAllBytes(targetFile));
    }

    @Test
    void should_return_false_for_null_input_stream() {
      var targetFile = tempDir.resolve("test-file.txt");
      var result = StreamUtil.copyToFile(null, targetFile);
      assertFalse(result);
    }

    @Test
    void should_return_false_for_null_target_path() {
      var input = new ByteArrayInputStream(SAMPLE_BYTES);
      var result = StreamUtil.copyToFile(input, null);
      assertFalse(result);
    }

    @Test
    void should_handle_file_creation_errors_gracefully() throws IOException {
      var input = new ByteArrayInputStream(SAMPLE_BYTES);

      // Create a read-only directory to force a write failure
      var readOnlyDir = tempDir.resolve("readonly");
      Files.createDirectory(readOnlyDir);
      readOnlyDir.toFile().setReadOnly();
      var targetFile = readOnlyDir.resolve("cannot-create.txt");

      try {
        var result = StreamUtil.copyToFile(input, targetFile);
        assertFalse(result);
      } finally {
        // Cleanup - restore write permissions
        readOnlyDir.toFile().setWritable(true);
      }
    }
  }

  @Nested
  class CopyFileTests {

    @Test
    void should_copy_file_successfully() throws IOException {
      var sourceFile = tempDir.resolve("source.txt");
      var targetFile = tempDir.resolve("target.txt");
      Files.writeString(sourceFile, SAMPLE_TEXT);

      var result = StreamUtil.copyFile(sourceFile, targetFile);

      assertTrue(result);
      assertTrue(Files.exists(targetFile));
      assertEquals(SAMPLE_TEXT, Files.readString(targetFile));
    }

    @Test
    void should_replace_existing_destination_file() throws IOException {
      var sourceFile = tempDir.resolve("source.txt");
      var targetFile = tempDir.resolve("target.txt");
      Files.writeString(sourceFile, SAMPLE_TEXT);
      Files.writeString(targetFile, "Original content");

      var result = StreamUtil.copyFile(sourceFile, targetFile);

      assertTrue(result);
      assertEquals(SAMPLE_TEXT, Files.readString(targetFile));
    }

    @Test
    void should_return_false_for_null_paths() {
      assertFalse(StreamUtil.copyFile(null, tempDir.resolve("target.txt")));
      assertFalse(StreamUtil.copyFile(tempDir.resolve("source.txt"), null));
    }

    @Test
    void should_return_false_for_nonexistent_source() {
      var nonExistentFile = tempDir.resolve("nonexistent.txt");
      var targetFile = tempDir.resolve("target.txt");

      var result = StreamUtil.copyFile(nonExistentFile, targetFile);

      assertFalse(result);
      assertFalse(Files.exists(targetFile));
    }
  }

  @Nested
  class CopyAndCloseTests {

//    @Test
//    void should_copy_streams_and_close_both() throws IOException {
//      var testData = "Test data for copy and close".getBytes();
//      var input = mock(new ByteArrayInputStream(testData));
//      var output = mock(new ByteArrayOutputStream());
//
//      var copiedBytes = StreamUtil.copyAndClose(input, output);
//
//      assertEquals(testData.length, copiedBytes);
//      verify(input).close();
//      verify(output).close();
//    }

//    @Test
//    void should_close_streams_even_when_copy_fails() throws IOException {
//      var input = mock(new FailingInputStream());
//      var output = mock(new ByteArrayOutputStream());
//
//      var result = StreamUtil.copyAndClose(input, output);
//
//      assertEquals(-1, result);
//      verify(input).close();
//      verify(output).close();
//    }

    @Test
    void should_handle_null_streams() {
      assertEquals(-1, StreamUtil.copyAndClose(null, new ByteArrayOutputStream()));
      assertEquals(-1, StreamUtil.copyAndClose(new ByteArrayInputStream("test".getBytes()), null));
    }

//    @Test
//    void should_continue_closing_second_stream_if_first_close_fails() throws IOException {
//      var input = new FailingCloseInputStream("test".getBytes());
//      var output = mock(new ByteArrayOutputStream());
//
//      var result = StreamUtil.copyAndClose(input, output);
//
//      assertEquals(4, result); // "test".length()
//      verify(output).close();
//    }
  }

  @Nested
  class CopyToFileAndCloseTests {

//    @Test
//    void should_copy_input_stream_to_file_and_close_stream() throws IOException {
//      var input = mock(new ByteArrayInputStream(SAMPLE_BYTES));
//      var targetFile = tempDir.resolve("test-copy-and-close.txt");
//
//      var result = StreamUtil.copyToFileAndClose(input, targetFile);
//
//      assertTrue(result);
//      assertTrue(Files.exists(targetFile));
//      assertEquals(SAMPLE_TEXT, Files.readString(targetFile));
//      verify(input).close();
//    }

//    @Test
//    void should_create_parent_directories_when_copying_to_file_and_closing() throws IOException {
//      var input = mock(new ByteArrayInputStream(SAMPLE_BYTES));
//      var nestedPath = tempDir.resolve("level1").resolve("level2").resolve("nested-file.txt");
//
//      var result = StreamUtil.copyToFileAndClose(input, nestedPath);
//
//      assertTrue(result);
//      assertTrue(Files.exists(nestedPath));
//      assertTrue(Files.exists(nestedPath.getParent()));
//      assertEquals(SAMPLE_TEXT, Files.readString(nestedPath));
//      verify(input).close();
//    }

    @Test
    void should_return_false_for_null_input_stream_and_close_nothing() {
      var targetFile = tempDir.resolve("null-input-test.txt");
      var result = StreamUtil.copyToFileAndClose((InputStream) null, targetFile);

      assertFalse(result);
      assertFalse(Files.exists(targetFile));
    }

//    @Test
//    void should_return_false_for_null_target_path_and_close_input_stream() throws IOException {
//      var input = mock(new ByteArrayInputStream(SAMPLE_BYTES));
//      var result = StreamUtil.copyToFileAndClose(input, null);
//
//      assertFalse(result);
//      verify(input).close();
//    }

//    @Test
//    void should_close_input_stream_even_when_copy_fails() throws IOException {
//      var input = mock(new ByteArrayInputStream(SAMPLE_BYTES));
//      // Create a read-only directory to force a write failure
//      var readOnlyDir = tempDir.resolve("readonly");
//      Files.createDirectory(readOnlyDir);
//      readOnlyDir.toFile().setReadOnly();
//      var targetFile = readOnlyDir.resolve("cannot-create.txt");
//
//      try {
//
//        var result = StreamUtil.copyToFileAndClose(input, targetFile);
//
//        assertFalse(result);
//      } finally {
//        // Cleanup - restore write permissions
//        readOnlyDir.toFile().setWritable(true);
//      }
//    }

    @Test
    void should_handle_input_stream_close_exception_gracefully() throws IOException {
      var input = new FailingCloseInputStream(SAMPLE_BYTES);
      var targetFile = tempDir.resolve("close-exception-test.txt");

      var result = StreamUtil.copyToFileAndClose(input, targetFile);

      assertTrue(result);
      assertTrue(Files.exists(targetFile));
      assertEquals(SAMPLE_TEXT, Files.readString(targetFile));
    }

//    @Test
//    void should_handle_empty_input_stream() throws IOException {
//      var input = mock(new ByteArrayInputStream(new byte[0]));
//      var targetFile = tempDir.resolve("empty-file-test.txt");
//
//      var result = StreamUtil.copyToFileAndClose(input, targetFile);
//
//      assertTrue(result);
//      assertTrue(Files.exists(targetFile));
//      assertEquals(0, targetFile.toFile().length());
//      verify(input).close();
//    }

    @Test
    void should_copy_image_input_stream_and_close() throws IOException {
      var imageInputStream = mock(ImageInputStream.class);
      var targetFile = tempDir.resolve("image-test.dat");

      // Mock successful read then end of stream
      when(imageInputStream.read(any(byte[].class))).thenReturn(SAMPLE_BYTES.length).thenReturn(-1);

      var result = StreamUtil.copyToFileAndClose(imageInputStream, targetFile);

      assertTrue(result);
      assertTrue(Files.exists(targetFile));
      verify(imageInputStream, atLeastOnce()).read(any(byte[].class));
      verify(imageInputStream).close();
    }

    @Test
    void should_handle_null_image_input_stream() {
      var targetFile = tempDir.resolve("null-image-input-test.dat");
      var result = StreamUtil.copyToFileAndClose((ImageInputStream) null, targetFile);

      assertFalse(result);
      assertFalse(Files.exists(targetFile));
    }

    @Test
    void should_handle_null_target_path_with_image_input_stream() throws IOException {
      var imageInputStream = mock(ImageInputStream.class);
      var result = StreamUtil.copyToFileAndClose(imageInputStream, null);
      assertFalse(result);
    }
  }

  @Nested
  class CopyWithNIOTests {

    @Test
    void should_copy_data_using_nio_with_default_buffer_size() {
      var input = new ByteArrayInputStream(SAMPLE_BYTES);
      var output = new ByteArrayOutputStream();

      var result = StreamUtil.copyWithNIO(input, output);

      assertTrue(result);
      assertArrayEquals(SAMPLE_BYTES, output.toByteArray());
    }

    @Test
    void should_copy_data_using_nio_with_custom_buffer_size() {
      var input = new ByteArrayInputStream(SAMPLE_BYTES);
      var output = new ByteArrayOutputStream();

      var result = StreamUtil.copyWithNIO(input, output, 1024);

      assertTrue(result);
      assertArrayEquals(SAMPLE_BYTES, output.toByteArray());
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, 0, 1, 512, 4096, 8192})
    void should_handle_various_buffer_sizes_in_nio_copy(int bufferSize) {
      var testData = ("NIO copy test with buffer size: " + bufferSize).getBytes();
      var input = new ByteArrayInputStream(testData);
      var output = new ByteArrayOutputStream();

      var result = StreamUtil.copyWithNIO(input, output, bufferSize);

      assertTrue(result);
      assertArrayEquals(testData, output.toByteArray());
    }

    @Test
    void should_return_false_for_null_streams() {
      var output = new ByteArrayOutputStream();
      var input = new ByteArrayInputStream(SAMPLE_BYTES);

      assertFalse(StreamUtil.copyWithNIO(null, output, 1024));
      assertFalse(StreamUtil.copyWithNIO(input, null, 1024));
    }

    @Test
    void should_handle_empty_input_stream_in_nio_copy() {
      var input = new ByteArrayInputStream(new byte[0]);
      var output = new ByteArrayOutputStream();

      var result = StreamUtil.copyWithNIO(input, output, 1024);

      assertTrue(result);
      assertEquals(0, output.size());
    }

    @Test
    void should_handle_large_data_with_nio_copy() {
      var input = new ByteArrayInputStream(LARGE_DATA);
      var output = new ByteArrayOutputStream();

      var result = StreamUtil.copyWithNIO(input, output, 4096);

      assertTrue(result);
      assertArrayEquals(LARGE_DATA, output.toByteArray());
    }

    @Test
    void should_handle_io_exceptions_gracefully() {
      var faultyInputStream = new FailingInputStream();
      var output = new ByteArrayOutputStream();

      var result = StreamUtil.copyWithNIO(faultyInputStream, output, 1024);

      assertFalse(result);
    }
  }

//  @Nested
//  class CopyWithNIOAndCloseTests {
//
//    @Test
//    void should_copy_data_using_nio_and_close_both_streams() throws IOException {
//      var testData = "Test data for NIO copy".getBytes();
//      var input = new ByteArrayInputStream(testData);
//      var output = mock(ByteArrayOutputStream.class);
//
//      var result = StreamUtil.copyWithNIOAndClose(input, output, 1024);
//
//      assertTrue(result);
//      verify(output, atLeastOnce()).close();
//    }
//
//    @Test
//    void should_close_streams_even_when_nio_copy_fails() throws IOException {
//      var output = mock(ByteArrayOutputStream.class);
//
//      var result = StreamUtil.copyWithNIOAndClose(null, output, 1024);
//
//      assertFalse(result);
//      verify(output).close();
//    }
//
//    @Test
//    void should_handle_null_streams() throws IOException {
//      var output = mock(ByteArrayOutputStream.class);
//      var input = mock(ByteArrayInputStream.class);
//
//      assertFalse(StreamUtil.copyWithNIOAndClose(null, output, 1024));
//      assertFalse(StreamUtil.copyWithNIOAndClose(input, null, 1024));
//
//      verify(output).close();
//      verify(input).close();
//    }
//  }

  // Helper classes for testing failure scenarios without mocks
  private static class FailingInputStream extends InputStream {
    @Override
    public int read() throws IOException {
      throw new IOException("Simulated read failure");
    }
  }

  private static class FailingCloseInputStream extends ByteArrayInputStream {
    public FailingCloseInputStream(byte[] buf) {
      super(buf);
    }

    @Override
    public void close() throws IOException {
      throw new IOException("Simulated close failure");
    }
  }
}
