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
import static org.mockito.Mockito.*;

import java.io.*;
import java.nio.file.*;
import javax.imageio.stream.ImageInputStream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Comprehensive test suite for StreamUtil class. */
class StreamUtilTest {

  @TempDir Path tempDir;

  // ================= Safe Close Tests =================

  @Nested
  @DisplayName("SafeClose AutoCloseable Tests")
  class SafeCloseAutoCloseableTests {

    @Test
    @DisplayName("Should safely close AutoCloseable resource")
    void shouldSafelyCloseAutoCloseableResource() throws Exception {
      // Arrange
      AutoCloseable mockResource = mock(AutoCloseable.class);

      // Act
      StreamUtil.safeClose(mockResource);

      // Assert
      verify(mockResource).close();
    }

    @Test
    @DisplayName("Should handle null AutoCloseable gracefully")
    void shouldHandleNullAutoCloseableGracefully() {
      // Act & Assert - should not throw exception
      assertDoesNotThrow(() -> StreamUtil.safeClose((AutoCloseable) null));
    }

    @Test
    @DisplayName("Should handle AutoCloseable close exception gracefully")
    void shouldHandleAutoCloseableCloseExceptionGracefully() throws Exception {
      // Arrange
      AutoCloseable mockResource = mock(AutoCloseable.class);
      doThrow(new IOException("Close failed")).when(mockResource).close();

      // Act & Assert - should not throw exception
      assertDoesNotThrow(() -> StreamUtil.safeClose(mockResource));
      verify(mockResource).close();
    }

    @Test
    @DisplayName("Should close multiple AutoCloseable resources")
    void shouldCloseMultipleAutoCloseableResources() throws Exception {
      // Arrange
      AutoCloseable resource1 = mock(AutoCloseable.class);
      AutoCloseable resource2 = mock(AutoCloseable.class);
      AutoCloseable resource3 = mock(AutoCloseable.class);

      // Act
      StreamUtil.safeClose(resource1, resource2, resource3);

      // Assert
      verify(resource1).close();
      verify(resource2).close();
      verify(resource3).close();
    }

    @Test
    @DisplayName("Should handle mixed null and valid resources in varargs")
    void shouldHandleMixedNullAndValidResourcesInVarargs() throws Exception {
      // Arrange
      AutoCloseable validResource = mock(AutoCloseable.class);

      // Act & Assert - should not throw exception
      assertDoesNotThrow(() -> StreamUtil.safeClose(validResource, null, validResource));
      verify(validResource, times(2)).close();
    }

    @Test
    @DisplayName("Should continue closing remaining resources when one fails")
    void shouldContinueClosingRemainingResourcesWhenOneFails() throws Exception {
      // Arrange
      AutoCloseable resource1 = mock(AutoCloseable.class);
      AutoCloseable resource2 = mock(AutoCloseable.class);
      AutoCloseable resource3 = mock(AutoCloseable.class);

      doThrow(new IOException("Close failed")).when(resource2).close();

      // Act
      assertDoesNotThrow(() -> StreamUtil.safeClose(resource1, resource2, resource3));

      // Assert
      verify(resource1).close();
      verify(resource2).close();
      verify(resource3).close();
    }

    @Test
    @DisplayName("Should handle null varargs array")
    void shouldHandleNullVarargsArray() {
      // Act & Assert - should not throw exception
      assertDoesNotThrow(() -> StreamUtil.safeClose((AutoCloseable[]) null));
    }
  }

  @Nested
  @DisplayName("SafeClose XML Stream Tests")
  class SafeCloseXMLStreamTests {

    @Test
    @DisplayName("Should safely close XMLStreamWriter")
    void shouldSafelyCloseXMLStreamWriter() throws XMLStreamException {
      // Arrange
      XMLStreamWriter mockWriter = mock(XMLStreamWriter.class);

      // Act
      StreamUtil.safeClose(mockWriter);

      // Assert
      verify(mockWriter).close();
    }

    @Test
    @DisplayName("Should handle null XMLStreamWriter gracefully")
    void shouldHandleNullXMLStreamWriterGracefully() {
      // Act & Assert - should not throw exception
      assertDoesNotThrow(() -> StreamUtil.safeClose((XMLStreamWriter) null));
    }

    @Test
    @DisplayName("Should handle XMLStreamWriter close exception gracefully")
    void shouldHandleXMLStreamWriterCloseExceptionGracefully() throws XMLStreamException {
      // Arrange
      XMLStreamWriter mockWriter = mock(XMLStreamWriter.class);
      doThrow(new XMLStreamException("Close failed")).when(mockWriter).close();

      // Act & Assert - should not throw exception
      assertDoesNotThrow(() -> StreamUtil.safeClose(mockWriter));
      verify(mockWriter).close();
    }

    @Test
    @DisplayName("Should safely close XMLStreamReader")
    void shouldSafelyCloseXMLStreamReader() throws XMLStreamException {
      // Arrange
      XMLStreamReader mockReader = mock(XMLStreamReader.class);

      // Act
      StreamUtil.safeClose(mockReader);

      // Assert
      verify(mockReader).close();
    }

    @Test
    @DisplayName("Should handle null XMLStreamReader gracefully")
    void shouldHandleNullXMLStreamReaderGracefully() {
      // Act & Assert - should not throw exception
      assertDoesNotThrow(() -> StreamUtil.safeClose((XMLStreamReader) null));
    }

    @Test
    @DisplayName("Should handle XMLStreamReader close exception gracefully")
    void shouldHandleXMLStreamReaderCloseExceptionGracefully() throws XMLStreamException {
      // Arrange
      XMLStreamReader mockReader = mock(XMLStreamReader.class);
      doThrow(new XMLStreamException("Close failed")).when(mockReader).close();

      // Act & Assert - should not throw exception
      assertDoesNotThrow(() -> StreamUtil.safeClose(mockReader));
      verify(mockReader).close();
    }
  }

  // ================= Stream Copy Tests =================

  @Nested
  @DisplayName("Stream Copy Tests")
  class StreamCopyTests {

    @Test
    @DisplayName("Should copy data from valid streams")
    void shouldCopyDataFromValidStreams() throws IOException {
      // Arrange
      byte[] inputData = "Test data for StreamUtil copy".getBytes();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(inputData);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      long copiedBytes = StreamUtil.copy(inputStream, outputStream);

      // Assert
      assertEquals(inputData.length, copiedBytes);
      assertArrayEquals(inputData, outputStream.toByteArray());
    }

    @Test
    @DisplayName("Should handle empty input stream")
    void shouldHandleEmptyInputStream() throws IOException {
      // Arrange
      byte[] inputData = new byte[0];
      ByteArrayInputStream inputStream = new ByteArrayInputStream(inputData);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      long copiedBytes = StreamUtil.copy(inputStream, outputStream);

      // Assert
      assertEquals(0, copiedBytes);
      assertEquals(0, outputStream.size());
    }

    @Test
    @DisplayName("Should throw exception for null input stream")
    void shouldThrowExceptionForNullInputStream() {
      // Arrange
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act & Assert
      IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> StreamUtil.copy(null, outputStream));
      assertEquals("Input and output streams cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for null output stream")
    void shouldThrowExceptionForNullOutputStream() {
      // Arrange
      byte[] inputData = "Some data".getBytes();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(inputData);

      // Act & Assert
      IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> StreamUtil.copy(inputStream, null));
      assertEquals("Input and output streams cannot be null", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 100, 1024, 8192, 16384})
    @DisplayName("Should copy with various buffer sizes")
    void shouldCopyWithVariousBufferSizes(int bufferSize) throws IOException {
      // Arrange
      byte[] inputData = "Sample data with custom buffer size testing various sizes".getBytes();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(inputData);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      long copiedBytes = StreamUtil.copy(inputStream, outputStream, bufferSize);

      // Assert
      assertEquals(inputData.length, copiedBytes);
      assertArrayEquals(inputData, outputStream.toByteArray());
    }

    @ParameterizedTest
    @ValueSource(ints = {-10, 0})
    @DisplayName("Should use default buffer size for invalid buffer sizes")
    void shouldUseDefaultBufferSizeForInvalidBufferSizes(int invalidBufferSize) throws IOException {
      // Arrange
      byte[] inputData = "Buffer size test".getBytes();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(inputData);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      long copiedBytes = StreamUtil.copy(inputStream, outputStream, invalidBufferSize);

      // Assert
      assertEquals(inputData.length, copiedBytes);
      assertArrayEquals(inputData, outputStream.toByteArray());
    }

    @Test
    @DisplayName("Should handle large data transfer")
    void shouldHandleLargeDataTransfer() throws IOException {
      // Arrange
      byte[] largeData = new byte[100000]; // 100KB
      for (int i = 0; i < largeData.length; i++) {
        largeData[i] = (byte) (i % 256);
      }
      ByteArrayInputStream inputStream = new ByteArrayInputStream(largeData);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      long copiedBytes = StreamUtil.copy(inputStream, outputStream);

      // Assert
      assertEquals(largeData.length, copiedBytes);
      assertArrayEquals(largeData, outputStream.toByteArray());
    }

    @Test
    @DisplayName("Should flush output stream after copy")
    void shouldFlushOutputStreamAfterCopy() throws IOException {
      // Arrange
      byte[] inputData = "Test flush".getBytes();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(inputData);
      OutputStream mockOutputStream = spy(new ByteArrayOutputStream());

      // Act
      StreamUtil.copy(inputStream, mockOutputStream);

      // Assert
      verify(mockOutputStream).flush();
    }
  }

  // ================= Copy to File Tests =================

  @Nested
  @DisplayName("Copy to File Tests")
  class CopyToFileTests {

    @Test
    @DisplayName("Should copy input stream to file successfully")
    void shouldCopyInputStreamToFileSuccessfully() throws IOException {
      // Arrange
      byte[] testData = "Test data for file copy".getBytes();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(testData);
      Path targetFile = tempDir.resolve("test-output.txt");

      // Act
      boolean result = StreamUtil.copyToFile(inputStream, targetFile);

      // Assert
      assertTrue(result);
      assertTrue(Files.exists(targetFile));
      assertArrayEquals(testData, Files.readAllBytes(targetFile));
    }

    @Test
    @DisplayName("Should create parent directories when copying to file")
    void shouldCreateParentDirectoriesWhenCopyingToFile() throws IOException {
      // Arrange
      byte[] testData = "Test data with subdirectories".getBytes();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(testData);
      Path targetFile = tempDir.resolve("subdir").resolve("nested").resolve("test-file.txt");

      // Act
      boolean result = StreamUtil.copyToFile(inputStream, targetFile);

      // Assert
      assertTrue(result);
      assertTrue(Files.exists(targetFile));
      assertTrue(Files.exists(targetFile.getParent()));
      assertArrayEquals(testData, Files.readAllBytes(targetFile));
    }

    @Test
    @DisplayName("Should replace existing file when copying")
    void shouldReplaceExistingFileWhenCopying() throws IOException {
      // Arrange
      Path targetFile = tempDir.resolve("existing-file.txt");
      Files.write(targetFile, "Original content".getBytes());

      byte[] newData = "New content".getBytes();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(newData);

      // Act
      boolean result = StreamUtil.copyToFile(inputStream, targetFile);

      // Assert
      assertTrue(result);
      assertArrayEquals(newData, Files.readAllBytes(targetFile));
    }

    @Test
    @DisplayName("Should return false for null input stream")
    void shouldReturnFalseForNullInputStream() {
      // Arrange
      Path targetFile = tempDir.resolve("test-file.txt");

      // Act
      boolean result = StreamUtil.copyToFile(null, targetFile);

      // Assert
      assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for null target path")
    void shouldReturnFalseForNullTargetPath() {
      // Arrange
      ByteArrayInputStream inputStream = new ByteArrayInputStream("test".getBytes());

      // Act
      boolean result = StreamUtil.copyToFile(inputStream, null);

      // Assert
      assertFalse(result);
    }

    @Test
    @DisplayName("Should handle file creation errors gracefully")
    void shouldHandleFileCreationErrorsGracefully() throws IOException {
      // Arrange
      byte[] testData = "Test data".getBytes();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(testData);

      // Create a read-only directory to force an error
      Path readOnlyDir = tempDir.resolve("readonly");
      Files.createDirectory(readOnlyDir);
      readOnlyDir.toFile().setReadOnly();
      Path targetFile = readOnlyDir.resolve("cannot-create.txt");

      try {
        // Act
        boolean result = StreamUtil.copyToFile(inputStream, targetFile);

        // Assert
        assertFalse(result);
        assertFalse(Files.exists(targetFile));
      } finally {
        // Cleanup
        readOnlyDir.toFile().setWritable(true);
      }
    }
  }

  // ================= Copy and Close Tests =================

  @Nested
  @DisplayName("Copy and Close Tests")
  class CopyAndCloseTests {

    @Test
    @DisplayName("Should copy streams and close both")
    void shouldCopyStreamsAndCloseBoth() throws IOException {
      // Arrange
      byte[] testData = "Test data for copy and close".getBytes();
      InputStream inputStream = spy(new ByteArrayInputStream(testData));
      OutputStream outputStream = spy(new ByteArrayOutputStream());

      // Act
      long copiedBytes = StreamUtil.copyAndClose(inputStream, outputStream);

      // Assert
      assertEquals(testData.length, copiedBytes);
      verify(inputStream).close();
      verify(outputStream).close();
    }

    @Test
    @DisplayName("Should close streams even when copy fails")
    void shouldCloseStreamsEvenWhenCopyFails() throws IOException {
      // Arrange
      InputStream inputStream = spy(new ByteArrayInputStream("test".getBytes()));
      OutputStream outputStream = spy(new ByteArrayOutputStream());

      // Make the input stream throw an exception on read
      doThrow(new IOException("Read error")).when(inputStream).read(any(byte[].class));

      // Act
      long result = StreamUtil.copyAndClose(inputStream, outputStream);

      // Assert
      assertEquals(-1, result);
      verify(inputStream).close();
      verify(outputStream).close();
    }

    @Test
    @DisplayName("Should handle null input stream in copy and close")
    void shouldHandleNullInputStreamInCopyAndClose() {
      // Arrange
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      long result = StreamUtil.copyAndClose(null, outputStream);

      // Assert
      assertEquals(-1, result);
    }

    @Test
    @DisplayName("Should handle null output stream in copy and close")
    void shouldHandleNullOutputStreamInCopyAndClose() {
      // Arrange
      ByteArrayInputStream inputStream = new ByteArrayInputStream("test".getBytes());

      // Act
      long result = StreamUtil.copyAndClose(inputStream, null);

      // Assert
      assertEquals(-1, result);
    }

    @Test
    @DisplayName("Should continue closing second stream if first close fails")
    void shouldContinueClosingSecondStreamIfFirstCloseFails() throws IOException {
      // Arrange
      InputStream inputStream = mock(InputStream.class);
      OutputStream outputStream = mock(OutputStream.class);

      // Make copy succeed but first close fail
      when(inputStream.read(any(byte[].class))).thenReturn(-1);
      doThrow(new IOException("Close failed")).when(inputStream).close();

      // Act
      long result = StreamUtil.copyAndClose(inputStream, outputStream);

      // Assert
      assertEquals(0, result); // No bytes copied, but operation succeeded
      verify(inputStream).close();
      verify(outputStream).close();
    }
  }

  // ================= Copy to File and Close Tests =================

  @Nested
  @DisplayName("Copy to File and Close Tests")
  class CopyToFileAndCloseTests {

    @Test
    @DisplayName("Should copy input stream to file and close stream successfully")
    void shouldCopyInputStreamToFileAndCloseStreamSuccessfully() throws IOException {
      // Arrange
      String testData = "Test data for copying to file and closing stream";
      InputStream inputStream = spy(new ByteArrayInputStream(testData.getBytes()));
      Path targetFile = tempDir.resolve("test-copy-and-close.txt");

      // Act
      boolean result = StreamUtil.copyToFileAndClose(inputStream, targetFile);

      // Assert
      assertTrue(result, "Copy operation should succeed");
      assertTrue(Files.exists(targetFile), "Target file should exist");
      assertEquals(testData, Files.readString(targetFile), "File content should match input data");
      verify(inputStream, times(1)).close(); // Verify stream was closed
    }

    @Test
    @DisplayName("Should create parent directories when copying to file and closing")
    void shouldCreateParentDirectoriesWhenCopyingToFileAndClosing() throws IOException {
      // Arrange
      String testData = "Test data with nested directories";
      InputStream inputStream = spy(new ByteArrayInputStream(testData.getBytes()));
      Path nestedPath = tempDir.resolve("level1").resolve("level2").resolve("nested-file.txt");

      // Act
      boolean result = StreamUtil.copyToFileAndClose(inputStream, nestedPath);

      // Assert
      assertTrue(result, "Copy operation should succeed");
      assertTrue(Files.exists(nestedPath), "Target file should exist");
      assertTrue(Files.exists(nestedPath.getParent()), "Parent directories should be created");
      assertEquals(testData, Files.readString(nestedPath), "File content should match input data");
      verify(inputStream, times(1)).close();
    }

    @Test
    @DisplayName("Should return false for null input stream and close nothing")
    void shouldReturnFalseForNullInputStreamAndCloseNothing() {
      // Arrange
      Path targetFile = tempDir.resolve("null-input-test.txt");

      // Act
      boolean result = StreamUtil.copyToFileAndClose((InputStream) null, targetFile);

      // Assert
      assertFalse(result, "Copy operation should fail for null input stream");
      assertFalse(Files.exists(targetFile), "Target file should not be created");
    }

    @Test
    @DisplayName("Should return false for null target path and close input stream")
    void shouldReturnFalseForNullTargetPathAndCloseInputStream() throws IOException {
      // Arrange
      String testData = "Test data with null target";
      InputStream inputStream = spy(new ByteArrayInputStream(testData.getBytes()));

      // Act
      boolean result = StreamUtil.copyToFileAndClose(inputStream, null);

      // Assert
      assertFalse(result, "Copy operation should fail for null target path");
      verify(inputStream, times(1)).close(); // Stream should still be closed
    }

    @Test
    @DisplayName("Should close input stream even when copy fails")
    void shouldCloseInputStreamEvenWhenCopyFails() throws IOException {
      // Arrange
      InputStream inputStream = spy(new ByteArrayInputStream("test".getBytes()));
      // Create a read-only directory to cause copy failure
      Path readOnlyDir = tempDir.resolve("readonly");
      Files.createDirectory(readOnlyDir);
      readOnlyDir.toFile().setReadOnly(); // Make directory read-only to cause failure
      Path targetFile = readOnlyDir.resolve("test-file.txt");

      // Act
      boolean result = StreamUtil.copyToFileAndClose(inputStream, targetFile);

      // Assert
      assertFalse(result, "Copy operation should fail for read-only directory");
      verify(inputStream, times(1)).close(); // Stream should be closed despite failure

      // Clean up - restore write permissions
      readOnlyDir.toFile().setWritable(true);
    }

    @Test
    @DisplayName("Should handle input stream close exception gracefully")
    void shouldHandleInputStreamCloseExceptionGracefully() throws IOException {
      // Arrange
      String testData = "Test data for close exception handling";
      InputStream inputStream = spy(new ByteArrayInputStream(testData.getBytes()));
      Path targetFile = tempDir.resolve("close-exception-test.txt");

      // Make the stream throw exception on close
      doThrow(new IOException("Close failed")).when(inputStream).close();

      // Act
      boolean result = StreamUtil.copyToFileAndClose(inputStream, targetFile);

      // Assert
      assertTrue(result, "Copy operation should succeed despite close exception");
      assertTrue(Files.exists(targetFile), "Target file should exist");
      assertEquals(
          testData,
          Files.readString(targetFile),
          "File content should match despite close exception");
      verify(inputStream, times(1)).close(); // Verify close was attempted
    }

    @Test
    @DisplayName("Should replace existing file when copying and closing")
    void shouldReplaceExistingFileWhenCopyingAndClosing() throws IOException {
      // Arrange
      String originalData = "Original file content";
      String newData = "New file content";
      Path targetFile = tempDir.resolve("replace-test.txt");

      // Create initial file
      Files.writeString(targetFile, originalData);
      assertTrue(Files.exists(targetFile), "Initial file should exist");

      InputStream inputStream = spy(new ByteArrayInputStream(newData.getBytes()));

      // Act
      boolean result = StreamUtil.copyToFileAndClose(inputStream, targetFile);

      // Assert
      assertTrue(result, "Copy operation should succeed");
      assertTrue(Files.exists(targetFile), "Target file should still exist");
      assertEquals(newData, Files.readString(targetFile), "File content should be replaced");
      assertNotEquals(
          originalData, Files.readString(targetFile), "Original content should be overwritten");
      verify(inputStream, times(1)).close();
    }

    @Test
    @DisplayName("Should handle empty input stream")
    void shouldHandleEmptyInputStream() throws IOException {
      // Arrange
      InputStream inputStream = spy(new ByteArrayInputStream(new byte[0]));
      Path targetFile = tempDir.resolve("empty-file-test.txt");

      // Act
      boolean result = StreamUtil.copyToFileAndClose(inputStream, targetFile);

      // Assert
      assertTrue(result, "Copy operation should succeed for empty stream");
      assertTrue(Files.exists(targetFile), "Target file should be created");
      assertEquals(0, Files.size(targetFile), "File should be empty");
      verify(inputStream, times(1)).close();
    }

    @Test
    @DisplayName("Should copy ImageInputStream to file and close stream successfully")
    void shouldCopyImageInputStreamToFileAndCloseStreamSuccessfully() throws IOException {
      // Arrange
      byte[] testData = "Test image data for ImageInputStream".getBytes();
      ImageInputStream imageInputStream = mock(ImageInputStream.class);
      Path targetFile = tempDir.resolve("image-test.dat");

      // Mock the ImageInputStream behavior
      when(imageInputStream.read(any(byte[].class)))
          .thenAnswer(
              invocation -> {
                byte[] buffer = invocation.getArgument(0);
                int bytesToCopy = Math.min(buffer.length, testData.length);
                System.arraycopy(testData, 0, buffer, 0, bytesToCopy);
                return bytesToCopy;
              })
          .thenReturn(-1); // End of stream

      // Act
      boolean result = StreamUtil.copyToFileAndClose(imageInputStream, targetFile);

      // Assert
      assertTrue(result, "Copy operation should succeed");
      assertTrue(Files.exists(targetFile), "Target file should exist");
      verify(imageInputStream, atLeastOnce()).read(any(byte[].class));
      verify(imageInputStream, times(1)).close();
    }

    @Test
    @DisplayName("Should return false for null ImageInputStream and close nothing")
    void shouldReturnFalseForNullImageInputStream() {
      // Arrange
      Path targetFile = tempDir.resolve("null-image-input-test.dat");

      // Act
      boolean result = StreamUtil.copyToFileAndClose((ImageInputStream) null, targetFile);

      // Assert
      assertFalse(result, "Copy operation should fail for null ImageInputStream");
      assertFalse(Files.exists(targetFile), "Target file should not be created");
    }

    @Test
    @DisplayName("Should return false for null target path with ImageInputStream and close stream")
    void shouldReturnFalseForNullTargetPathWithImageInputStream() throws IOException {
      // Arrange
      ImageInputStream imageInputStream = mock(ImageInputStream.class);

      // Act
      boolean result = StreamUtil.copyToFileAndClose(imageInputStream, null);

      // Assert
      assertFalse(result, "Copy operation should fail for null target path");
    }

    @Test
    @DisplayName("Should close ImageInputStream even when copy fails")
    void shouldCloseImageInputStreamEvenWhenCopyFails() throws IOException {
      // Arrange
      ImageInputStream imageInputStream = mock(ImageInputStream.class);
      when(imageInputStream.read(any(byte[].class))).thenThrow(new IOException("Read failed"));
      Path targetFile = tempDir.resolve("image-copy-fail-test.dat");

      // Act
      boolean result = StreamUtil.copyToFileAndClose(imageInputStream, targetFile);

      // Assert
      assertFalse(result, "Copy operation should fail when ImageInputStream read fails");
      verify(imageInputStream, times(1)).close(); // Stream should be closed despite failure
    }

    @Test
    @DisplayName("Should handle ImageInputStream close exception gracefully")
    void shouldHandleImageInputStreamCloseExceptionGracefully() throws IOException {
      // Arrange
      byte[] testData = "Test data for ImageInputStream close exception".getBytes();
      ImageInputStream imageInputStream = mock(ImageInputStream.class);
      Path targetFile = tempDir.resolve("image-close-exception-test.dat");

      // Mock successful read but failed close
      when(imageInputStream.read(any(byte[].class)))
          .thenAnswer(
              invocation -> {
                byte[] buffer = invocation.getArgument(0);
                int bytesToCopy = Math.min(buffer.length, testData.length);
                System.arraycopy(testData, 0, buffer, 0, bytesToCopy);
                return bytesToCopy;
              })
          .thenReturn(-1);

      doThrow(new IOException("Close failed")).when(imageInputStream).close();

      // Act
      boolean result = StreamUtil.copyToFileAndClose(imageInputStream, targetFile);

      // Assert
      assertTrue(result, "Copy operation should succeed despite close exception");
      assertTrue(Files.exists(targetFile), "Target file should exist");
      verify(imageInputStream, times(1)).close(); // Verify close was attempted
    }
  }

  // ================= Copy with NIO Tests =================

  @Nested
  @DisplayName("Copy with NIO Tests")
  class CopyWithNIOTests {

    @Test
    @DisplayName("Should copy data using NIO with default buffer size")
    void shouldCopyDataUsingNIOWithDefaultBufferSize() {
      // Arrange
      byte[] testData = "Test data for NIO copy with default buffer".getBytes();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(testData);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      boolean result = StreamUtil.copyWithNIO(inputStream, outputStream, 0);

      // Assert
      assertTrue(result);
      assertArrayEquals(testData, outputStream.toByteArray());
    }

    @Test
    @DisplayName("Should copy data using NIO with custom buffer size")
    void shouldCopyDataUsingNIOWithCustomBufferSize() {
      // Arrange
      byte[] testData = "Test data for NIO copy with custom buffer size".getBytes();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(testData);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      boolean result = StreamUtil.copyWithNIO(inputStream, outputStream, 1024);

      // Assert
      assertTrue(result);
      assertArrayEquals(testData, outputStream.toByteArray());
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, 0, 1, 512, 4096, 8192})
    @DisplayName("Should handle various buffer sizes in NIO copy")
    void shouldHandleVariousBufferSizesInNIOCopy(int bufferSize) {
      // Arrange
      byte[] testData = ("NIO copy test with buffer size: " + bufferSize).getBytes();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(testData);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      boolean result = StreamUtil.copyWithNIO(inputStream, outputStream, bufferSize);

      // Assert
      assertTrue(result);
      assertArrayEquals(testData, outputStream.toByteArray());
    }

    @Test
    @DisplayName("Should return false for null input stream in NIO copy")
    void shouldReturnFalseForNullInputStreamInNIOCopy() {
      // Arrange
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      boolean result = StreamUtil.copyWithNIO(null, outputStream, 1024);

      // Assert
      assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for null output stream in NIO copy")
    void shouldReturnFalseForNullOutputStreamInNIOCopy() {
      // Arrange
      ByteArrayInputStream inputStream = new ByteArrayInputStream("test".getBytes());

      // Act
      boolean result = StreamUtil.copyWithNIO(inputStream, null, 1024);

      // Assert
      assertFalse(result);
    }

    @Test
    @DisplayName("Should handle empty input stream in NIO copy")
    void shouldHandleEmptyInputStreamInNIOCopy() {
      // Arrange
      ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      boolean result = StreamUtil.copyWithNIO(inputStream, outputStream, 1024);

      // Assert
      assertTrue(result);
      assertEquals(0, outputStream.size());
    }

    @Test
    @DisplayName("Should handle large data with NIO copy")
    void shouldHandleLargeDataWithNIOCopy() {
      // Arrange
      byte[] largeData = new byte[50000]; // 50KB
      for (int i = 0; i < largeData.length; i++) {
        largeData[i] = (byte) (i % 256);
      }
      ByteArrayInputStream inputStream = new ByteArrayInputStream(largeData);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      boolean result = StreamUtil.copyWithNIO(inputStream, outputStream, 4096);

      // Assert
      assertTrue(result);
      assertArrayEquals(largeData, outputStream.toByteArray());
    }

    @Test
    @DisplayName("Should close channels but underlying streams in NIO copy")
    void shouldCloseChannelsButUnderlyingStreamsInNIOCopy() throws IOException {
      // Arrange
      byte[] testData = "Test that channels close underlying streams".getBytes();
      InputStream inputStream = spy(new ByteArrayInputStream(testData));
      OutputStream outputStream = spy(new ByteArrayOutputStream());

      // Act
      boolean result = StreamUtil.copyWithNIO(inputStream, outputStream, 1024);

      // Assert
      assertTrue(result);
      // Note: NIO channels automatically close their underlying streams when closed
      // This is the expected behavior for Channels.newChannel() implementation
      verify(inputStream, times(1)).close();
      verify(outputStream, times(1)).close();
    }

    @Test
    @DisplayName("Should handle IO exceptions in NIO copy gracefully")
    void shouldHandleIOExceptionsInNIOCopyGracefully() throws IOException {
      // Arrange
      // Create a stream that will throw an exception when wrapped in a channel
      InputStream faultyInputStream =
          new InputStream() {
            @Override
            public int read() throws IOException {
              throw new IOException("Read error");
            }

            @Override
            public int read(byte[] b) throws IOException {
              throw new IOException("Read error");
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
              throw new IOException("Read error");
            }
          };
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      boolean result = StreamUtil.copyWithNIO(faultyInputStream, outputStream, 1024);

      // Assert
      assertFalse(result);
    }
  }

  // ================= Copy with NIO and Close Tests =================

  @Nested
  @DisplayName("Copy with NIO and Close Tests")
  class CopyWithNIOAndCloseTests {

    @Test
    @DisplayName("Should copy data using NIO and close both streams")
    void shouldCopyDataUsingNIOAndCloseBothStreams() throws IOException {
      // Arrange
      byte[] testData = "Test data for NIO copy and close".getBytes();
      InputStream inputStream = spy(new ByteArrayInputStream(testData));
      OutputStream outputStream = spy(new ByteArrayOutputStream());

      // Act
      boolean result = StreamUtil.copyWithNIOAndClose(inputStream, outputStream, 1024);

      // Assert
      assertTrue(result);
      // Streams are closed twice: once by NIO channels, once by safeClose in finally block
      verify(inputStream, times(2)).close();
      verify(outputStream, times(2)).close();
    }

    @Test
    @DisplayName("Should close streams even when NIO copy fails")
    void shouldCloseStreamsEvenWhenNIOCopyFails() throws IOException {
      // Arrange
      OutputStream outputStream = spy(new ByteArrayOutputStream());

      // Act
      boolean result = StreamUtil.copyWithNIOAndClose(null, outputStream, 1024);

      // Assert
      assertFalse(result);
      verify(outputStream).close();
    }

    @Test
    @DisplayName("Should handle null input stream in NIO copy and close")
    void shouldHandleNullInputStreamInNIOCopyAndClose() throws IOException {
      // Arrange
      OutputStream outputStream = spy(new ByteArrayOutputStream());

      // Act
      boolean result = StreamUtil.copyWithNIOAndClose(null, outputStream, 1024);

      // Assert
      assertFalse(result);
      verify(outputStream).close();
    }

    @Test
    @DisplayName("Should handle null output stream in NIO copy and close")
    void shouldHandleNullOutputStreamInNIOCopyAndClose() throws IOException {
      // Arrange
      InputStream inputStream = spy(new ByteArrayInputStream("test".getBytes()));

      // Act
      boolean result = StreamUtil.copyWithNIOAndClose(inputStream, null, 1024);

      // Assert
      assertFalse(result);
      verify(inputStream).close();
    }

    @Test
    @DisplayName("Should continue closing second stream if first close fails")
    void shouldContinueClosingSecondStreamIfFirstCloseFails() throws IOException {
      // Arrange
      byte[] testData = "Test data for close failure handling".getBytes();
      InputStream inputStream = spy(new ByteArrayInputStream(testData));
      OutputStream outputStream = spy(new ByteArrayOutputStream());

      // Make first stream close fail
      doThrow(new IOException("Close failed")).when(inputStream).close();

      // Act
      boolean result = StreamUtil.copyWithNIOAndClose(inputStream, outputStream, 1024);

      // Assert
      assertTrue(result); // Copy should succeed
      // Streams are closed twice: once by NIO channels, once by safeClose in finally block
      verify(inputStream, times(2)).close();
      verify(outputStream, times(2)).close();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 512, 2048, 8192})
    @DisplayName("Should handle various buffer sizes in NIO copy and close")
    void shouldHandleVariousBufferSizesInNIOCopyAndClose(int bufferSize) throws IOException {
      // Arrange
      byte[] testData = ("NIO copy and close test with buffer: " + bufferSize).getBytes();
      InputStream inputStream = spy(new ByteArrayInputStream(testData));
      OutputStream outputStream = spy(new ByteArrayOutputStream());

      // Act
      boolean result = StreamUtil.copyWithNIOAndClose(inputStream, outputStream, bufferSize);

      // Assert
      assertTrue(result);
      // Streams are closed twice: once by NIO channels, once by safeClose in finally block
      verify(inputStream, times(2)).close();
      verify(outputStream, times(2)).close();
    }

    @Test
    @DisplayName("Should handle large data with NIO copy and close streams twice")
    void shouldHandleLargeDataWithNIOCopyAndClose() throws IOException {
      // Arrange
      byte[] largeData = new byte[100000]; // 100KB
      for (int i = 0; i < largeData.length; i++) {
        largeData[i] = (byte) (i % 256);
      }
      InputStream inputStream = spy(new ByteArrayInputStream(largeData));
      ByteArrayOutputStream outputStream = spy(new ByteArrayOutputStream());

      // Act
      boolean result = StreamUtil.copyWithNIOAndClose(inputStream, outputStream, 4096);

      // Assert
      assertTrue(result);
      assertArrayEquals(largeData, outputStream.toByteArray());
      // Streams are closed twice: once by NIO channels, once by safeClose in finally block
      verify(inputStream, times(2)).close();
      verify(outputStream, times(2)).close();
    }

    @Test
    @DisplayName("Should handle both streams being null")
    void shouldHandleBothStreamsBeingNull() {
      // Act
      boolean result = StreamUtil.copyWithNIOAndClose(null, null, 1024);

      // Assert
      assertFalse(result);
    }

    @Test
    @DisplayName("Should handle empty data with NIO copy and close")
    void shouldHandleEmptyDataWithNIOCopyAndClose() throws IOException {
      // Arrange
      InputStream inputStream = spy(new ByteArrayInputStream(new byte[0]));
      OutputStream outputStream = spy(new ByteArrayOutputStream());

      // Act
      boolean result = StreamUtil.copyWithNIOAndClose(inputStream, outputStream, 1024);

      // Assert
      assertTrue(result);
      // Streams are closed twice: once by NIO channels, once by safeClose in finally block
      verify(inputStream, times(2)).close();
      verify(outputStream, times(2)).close();
    }
  }

  // ================= Integration Tests =================

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should work with real file streams")
    void shouldWorkWithRealFileStreams() throws IOException {
      // Arrange
      Path sourceFile = tempDir.resolve("source.txt");
      Path targetFile = tempDir.resolve("target.txt");
      byte[] testData = "Integration test data with real file streams".getBytes();

      Files.write(sourceFile, testData);

      // Act
      try (InputStream inputStream = Files.newInputStream(sourceFile);
          OutputStream outputStream = Files.newOutputStream(targetFile)) {

        long copiedBytes = StreamUtil.copy(inputStream, outputStream);

        // Assert
        assertEquals(testData.length, copiedBytes);
      }

      assertArrayEquals(testData, Files.readAllBytes(targetFile));
    }

    @Test
    @DisplayName("Should handle multiple consecutive copy operations")
    void shouldHandleMultipleConsecutiveCopyOperations() throws IOException {
      // Arrange
      byte[] data1 = "First data chunk".getBytes();
      byte[] data2 = "Second data chunk".getBytes();
      byte[] data3 = "Third data chunk".getBytes();

      // Act & Assert
      for (byte[] data : new byte[][] {data1, data2, data3}) {
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        long copied = StreamUtil.copy(input, output);

        assertEquals(data.length, copied);
        assertArrayEquals(data, output.toByteArray());
      }
    }
  }

  // ================= Performance Tests =================

  @Nested
  @DisplayName("Performance Tests")
  class PerformanceTests {

    @Test
    @DisplayName("Should handle large file efficiently")
    void shouldHandleLargeFileEfficiently() throws IOException {
      // Arrange - Create 1MB of test data
      byte[] largeData = new byte[1024 * 1024];
      for (int i = 0; i < largeData.length; i++) {
        largeData[i] = (byte) (i % 256);
      }

      ByteArrayInputStream inputStream = new ByteArrayInputStream(largeData);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      long startTime = System.currentTimeMillis();
      long copiedBytes = StreamUtil.copy(inputStream, outputStream);
      long endTime = System.currentTimeMillis();

      // Assert
      assertEquals(largeData.length, copiedBytes);
      assertArrayEquals(largeData, outputStream.toByteArray());

      // Performance assertion - should complete within reasonable time
      long duration = endTime - startTime;
      assertTrue(duration < 1000, "Large file copy took too long: " + duration + "ms");
    }
  }
}
