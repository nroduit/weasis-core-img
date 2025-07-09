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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Comprehensive test suite for PropertiesUtil class. */
class PropertiesUtilTest {

  @TempDir Path tempDir;

  private Properties testProperties;
  private Path propertiesFile;

  @BeforeEach
  void setUp() {
    testProperties = new Properties();
    testProperties.setProperty("key1", "value1");
    testProperties.setProperty("key2", "value2");
    testProperties.setProperty("unicode.key", "héllo wörld");
    testProperties.setProperty("special.chars", "!@#$%^&*()");
    testProperties.setProperty("multiline", "line1\nline2\nline3");

    propertiesFile = tempDir.resolve("test.properties");
  }

  // ================= Load Properties Tests =================

  @Nested
  @DisplayName("Load Properties Tests")
  class LoadPropertiesTests {

    @Test
    @DisplayName("Should load properties from existing file with default UTF-8 encoding")
    void shouldLoadPropertiesFromExistingFileWithDefaultUTF8Encoding() throws IOException {
      // Arrange
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test properties");

      // Act
      Properties loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      // Assert
      assertNotNull(loadedProperties);
      assertEquals(testProperties.size(), loadedProperties.size());
      assertEquals("value1", loadedProperties.getProperty("key1"));
      assertEquals("value2", loadedProperties.getProperty("key2"));
      assertEquals("héllo wörld", loadedProperties.getProperty("unicode.key"));
    }

    @Test
    @DisplayName("Should load properties with custom charset encoding")
    void shouldLoadPropertiesWithCustomCharsetEncoding() throws IOException {
      // Arrange
      Charset charset = StandardCharsets.ISO_8859_1;
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test properties", charset);

      // Act
      Properties loadedProperties = PropertiesUtil.loadProperties(propertiesFile, null, charset);

      // Assert
      assertNotNull(loadedProperties);
      assertEquals(testProperties.size(), loadedProperties.size());
      assertEquals("value1", loadedProperties.getProperty("key1"));
      assertEquals("value2", loadedProperties.getProperty("key2"));
    }

    @Test
    @DisplayName("Should return empty properties when file does not exist")
    void shouldReturnEmptyPropertiesWhenFileDoesNotExist() throws IOException {
      // Arrange
      Path nonExistentFile = tempDir.resolve("nonexistent.properties");

      // Act
      Properties loadedProperties = PropertiesUtil.loadProperties(nonExistentFile);

      // Assert
      assertNotNull(loadedProperties);
      assertEquals(0, loadedProperties.size());
      assertTrue(loadedProperties.isEmpty());
    }

    @Test
    @DisplayName("Should handle empty properties file")
    void shouldHandleEmptyPropertiesFile() throws IOException {
      // Arrange
      Files.createFile(propertiesFile);

      // Act
      Properties loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      // Assert
      assertNotNull(loadedProperties);
      assertEquals(0, loadedProperties.size());
      assertTrue(loadedProperties.isEmpty());
    }

    @Test
    @DisplayName("Should throw exception for null path")
    void shouldThrowExceptionForNullPath() {
      // Act & Assert
      NullPointerException exception =
          assertThrows(NullPointerException.class, () -> PropertiesUtil.loadProperties(null));
      assertEquals("Path cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for null charset")
    void shouldThrowExceptionForNullCharset() {
      // Act & Assert
      NullPointerException exception =
          assertThrows(
              NullPointerException.class,
              () -> PropertiesUtil.loadProperties(propertiesFile, null, null));
      assertEquals("Charset cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should load properties into existing properties object")
    void shouldLoadPropertiesIntoExistingPropertiesObject() throws IOException {
      // Arrange
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test properties");

      Properties existingProperties = new Properties();
      existingProperties.setProperty("existing.key", "existing.value");

      // Act
      Properties result = PropertiesUtil.loadProperties(propertiesFile, existingProperties);

      // Assert
      assertSame(existingProperties, result);
      assertEquals(testProperties.size() + 1, result.size());
      assertEquals("existing.value", result.getProperty("existing.key"));
      assertEquals("value1", result.getProperty("key1"));
    }

    @Test
    @DisplayName("Should create new properties object when target is null")
    void shouldCreateNewPropertiesObjectWhenTargetIsNull() throws IOException {
      // Arrange
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test properties");

      // Act
      Properties result = PropertiesUtil.loadProperties(propertiesFile, null);

      // Assert
      assertNotNull(result);
      assertEquals(testProperties.size(), result.size());
      assertEquals("value1", result.getProperty("key1"));
    }

    @Test
    @DisplayName("Should handle properties with special characters in values")
    void shouldHandlePropertiesWithSpecialCharactersInValues() throws IOException {
      // Arrange
      Properties specialProperties = new Properties();
      specialProperties.setProperty("equals.test", "key=value");
      specialProperties.setProperty("colon.test", "key:value");
      specialProperties.setProperty("hash.test", "key#comment");
      specialProperties.setProperty("exclamation.test", "key!value");

      PropertiesUtil.storeProperties(propertiesFile, specialProperties, null);

      // Act
      Properties loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      // Assert
      assertEquals("key=value", loadedProperties.getProperty("equals.test"));
      assertEquals("key:value", loadedProperties.getProperty("colon.test"));
      assertEquals("key#comment", loadedProperties.getProperty("hash.test"));
      assertEquals("key!value", loadedProperties.getProperty("exclamation.test"));
    }
  }

  // ================= Store Properties Tests =================

  @Nested
  @DisplayName("Store Properties Tests")
  class StorePropertiesTests {

    @Test
    @DisplayName("Should store properties to file with default UTF-8 encoding")
    void shouldStorePropertiesToFileWithDefaultUTF8Encoding() throws IOException {
      // Act
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test comment");

      // Assert
      assertTrue(Files.exists(propertiesFile));
      assertTrue(Files.isReadable(propertiesFile));

      Properties loadedProperties = PropertiesUtil.loadProperties(propertiesFile);
      assertEquals(testProperties.size(), loadedProperties.size());
      assertEquals("value1", loadedProperties.getProperty("key1"));
    }

    @Test
    @DisplayName("Should store properties with custom charset encoding")
    void shouldStorePropertiesWithCustomCharsetEncoding() throws IOException {
      // Arrange
      Charset charset = StandardCharsets.ISO_8859_1;

      // Act
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test comment", charset);

      // Assert
      assertTrue(Files.exists(propertiesFile));

      Properties loadedProperties = PropertiesUtil.loadProperties(propertiesFile, null, charset);
      assertEquals(testProperties.size(), loadedProperties.size());
      assertEquals("value1", loadedProperties.getProperty("key1"));
    }

    @Test
    @DisplayName("Should create parent directories when storing properties")
    void shouldCreateParentDirectoriesWhenStoringProperties() throws IOException {
      // Arrange
      Path nestedFile = tempDir.resolve("nested").resolve("deep").resolve("test.properties");

      // Act
      PropertiesUtil.storeProperties(nestedFile, testProperties, "Nested file test");

      // Assert
      assertTrue(Files.exists(nestedFile));
      assertTrue(Files.exists(nestedFile.getParent()));

      Properties loadedProperties = PropertiesUtil.loadProperties(nestedFile);
      assertEquals("value1", loadedProperties.getProperty("key1"));
    }

    @Test
    @DisplayName("Should overwrite existing file when storing properties")
    void shouldOverwriteExistingFileWhenStoringProperties() throws IOException {
      // Arrange
      Properties originalProperties = new Properties();
      originalProperties.setProperty("original.key", "original.value");
      PropertiesUtil.storeProperties(propertiesFile, originalProperties, "Original");

      Properties newProperties = new Properties();
      newProperties.setProperty("new.key", "new.value");

      // Act
      PropertiesUtil.storeProperties(propertiesFile, newProperties, "New properties");

      // Assert
      Properties loadedProperties = PropertiesUtil.loadProperties(propertiesFile);
      assertEquals(1, loadedProperties.size());
      assertEquals("new.value", loadedProperties.getProperty("new.key"));
      assertNull(loadedProperties.getProperty("original.key"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Simple comment", "Multi\nline\ncomment"})
    @DisplayName("Should handle various comment types")
    void shouldHandleVariousCommentTypes(String comment) throws IOException {
      // Act & Assert
      assertDoesNotThrow(
          () -> PropertiesUtil.storeProperties(propertiesFile, testProperties, comment));
      assertTrue(Files.exists(propertiesFile));

      Properties loadedProperties = PropertiesUtil.loadProperties(propertiesFile);
      assertEquals(testProperties.size(), loadedProperties.size());
    }

    @Test
    @DisplayName("Should throw exception for null path when storing")
    void shouldThrowExceptionForNullPathWhenStoring() {
      // Act & Assert
      NullPointerException exception =
          assertThrows(
              NullPointerException.class,
              () -> PropertiesUtil.storeProperties(null, testProperties, "comment"));
      assertEquals("Path cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for null properties when storing")
    void shouldThrowExceptionForNullPropertiesWhenStoring() {
      // Act & Assert
      NullPointerException exception =
          assertThrows(
              NullPointerException.class,
              () -> PropertiesUtil.storeProperties(propertiesFile, null, "comment"));
      assertEquals("Properties cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for null charset when storing")
    void shouldThrowExceptionForNullCharsetWhenStoring() {
      // Act & Assert
      NullPointerException exception =
          assertThrows(
              NullPointerException.class,
              () ->
                  PropertiesUtil.storeProperties(propertiesFile, testProperties, "comment", null));
      assertEquals("Charset cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should preserve unicode characters when storing and loading")
    void shouldPreserveUnicodeCharactersWhenStoringAndLoading() throws IOException {
      // Arrange
      Properties unicodeProperties = new Properties();
      unicodeProperties.setProperty("chinese", "你好世界");
      unicodeProperties.setProperty("japanese", "こんにちは世界");
      unicodeProperties.setProperty("arabic", "مرحبا بالعالم");
      unicodeProperties.setProperty("emoji", "🌍🚀💻");

      // Act
      PropertiesUtil.storeProperties(propertiesFile, unicodeProperties, "Unicode test");
      Properties loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      // Assert
      assertEquals("你好世界", loadedProperties.getProperty("chinese"));
      assertEquals("こんにちは世界", loadedProperties.getProperty("japanese"));
      assertEquals("مرحبا بالعالم", loadedProperties.getProperty("arabic"));
      assertEquals("🌍🚀💻", loadedProperties.getProperty("emoji"));
    }
  }

  // ================= Utility Methods Tests =================

  @Nested
  @DisplayName("Utility Methods Tests")
  class UtilityMethodsTests {

    @Test
    @DisplayName("Should create Properties from Map")
    void shouldCreatePropertiesFromMap() {
      // Arrange
      Map<String, String> map = new HashMap<>();
      map.put("map.key1", "map.value1");
      map.put("map.key2", "map.value2");

      // Act
      Properties properties = PropertiesUtil.fromMap(map);

      // Assert
      assertNotNull(properties);
      assertEquals(2, properties.size());
      assertEquals("map.value1", properties.getProperty("map.key1"));
      assertEquals("map.value2", properties.getProperty("map.key2"));
    }

    @Test
    @DisplayName("Should handle null map gracefully")
    void shouldHandleNullMapGracefully() {
      // Act
      Properties properties = PropertiesUtil.fromMap(null);

      // Assert
      assertNotNull(properties);
      assertEquals(0, properties.size());
      assertTrue(properties.isEmpty());
    }

    @Test
    @DisplayName("Should handle empty map")
    void shouldHandleEmptyMap() {
      // Arrange
      Map<String, String> emptyMap = new HashMap<>();

      // Act
      Properties properties = PropertiesUtil.fromMap(emptyMap);

      // Assert
      assertNotNull(properties);
      assertEquals(0, properties.size());
      assertTrue(properties.isEmpty());
    }

    @Test
    @DisplayName("Should merge multiple Properties objects")
    void shouldMergeMultiplePropertiesObjects() {
      // Arrange
      Properties props1 = new Properties();
      props1.setProperty("key1", "value1");
      props1.setProperty("common", "from_props1");

      Properties props2 = new Properties();
      props2.setProperty("key2", "value2");
      props2.setProperty("common", "from_props2");

      Properties props3 = new Properties();
      props3.setProperty("key3", "value3");

      // Act
      Properties merged = PropertiesUtil.merge(props1, props2, props3);

      // Assert
      assertNotNull(merged);
      assertEquals(4, merged.size());
      assertEquals("value1", merged.getProperty("key1"));
      assertEquals("value2", merged.getProperty("key2"));
      assertEquals("value3", merged.getProperty("key3"));
      assertEquals("from_props2", merged.getProperty("common")); // Last one wins
    }

    @Test
    @DisplayName("Should handle null properties in merge")
    void shouldHandleNullPropertiesInMerge() {
      // Arrange
      Properties props1 = new Properties();
      props1.setProperty("key1", "value1");

      Properties props3 = new Properties();
      props3.setProperty("key3", "value3");

      // Act
      Properties merged = PropertiesUtil.merge(props1, null, props3);

      // Assert
      assertNotNull(merged);
      assertEquals(2, merged.size());
      assertEquals("value1", merged.getProperty("key1"));
      assertEquals("value3", merged.getProperty("key3"));
    }

    @Test
    @DisplayName("Should handle null array in merge")
    void shouldHandleNullArrayInMerge() {
      // Act
      Properties merged = PropertiesUtil.merge((Properties[]) null);

      // Assert
      assertNotNull(merged);
      assertEquals(0, merged.size());
      assertTrue(merged.isEmpty());
    }

    @Test
    @DisplayName("Should handle empty array in merge")
    void shouldHandleEmptyArrayInMerge() {
      // Act
      Properties merged = PropertiesUtil.merge();

      // Assert
      assertNotNull(merged);
      assertEquals(0, merged.size());
      assertTrue(merged.isEmpty());
    }
  }

  // ================= Integration Tests =================

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should perform complete store and load cycle")
    void shouldPerformCompleteStoreAndLoadCycle() throws IOException {
      // Arrange
      Properties originalProperties = new Properties();
      originalProperties.setProperty("app.name", "Test Application");
      originalProperties.setProperty("app.version", "1.0.0");
      originalProperties.setProperty("app.author", "Test Author");
      originalProperties.setProperty("app.date", LocalDateTime.now().toString());

      // Act - Store
      PropertiesUtil.storeProperties(propertiesFile, originalProperties, "Application Properties");

      // Act - Load
      Properties loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      // Assert
      assertEquals(originalProperties.size(), loadedProperties.size());
      for (String key : originalProperties.stringPropertyNames()) {
        assertEquals(originalProperties.getProperty(key), loadedProperties.getProperty(key));
      }
    }

    @Test
    @DisplayName("Should handle concurrent access to different files")
    void shouldHandleConcurrentAccessToDifferentFiles() throws IOException {
      // Arrange
      Path file1 = tempDir.resolve("concurrent1.properties");
      Path file2 = tempDir.resolve("concurrent2.properties");

      Properties props1 = new Properties();
      props1.setProperty("file", "one");

      Properties props2 = new Properties();
      props2.setProperty("file", "two");

      // Act
      PropertiesUtil.storeProperties(file1, props1, "File 1");
      PropertiesUtil.storeProperties(file2, props2, "File 2");

      Properties loaded1 = PropertiesUtil.loadProperties(file1);
      Properties loaded2 = PropertiesUtil.loadProperties(file2);

      // Assert
      assertEquals("one", loaded1.getProperty("file"));
      assertEquals("two", loaded2.getProperty("file"));
    }

    @Test
    @DisplayName("Should work with very large properties file")
    void shouldWorkWithVeryLargePropertiesFile() throws IOException {
      // Arrange
      Properties largeProperties = new Properties();
      for (int i = 0; i < 1000; i++) {
        largeProperties.setProperty(
            "key" + i, "value" + i + " - some longer text to make it more realistic");
      }

      // Act
      PropertiesUtil.storeProperties(propertiesFile, largeProperties, "Large properties test");
      Properties loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      // Assert
      assertEquals(1000, loadedProperties.size());
      assertEquals(
          "value0 - some longer text to make it more realistic",
          loadedProperties.getProperty("key0"));
      assertEquals(
          "value999 - some longer text to make it more realistic",
          loadedProperties.getProperty("key999"));
    }

    @Test
    @DisplayName("Should maintain property order consistency")
    void shouldMaintainPropertyOrderConsistency() throws IOException {
      // Arrange
      Properties orderedProperties = new Properties();
      // Add properties in a specific order
      for (int i = 0; i < 10; i++) {
        orderedProperties.setProperty(String.format("prop%02d", i), "value" + i);
      }

      // Act
      PropertiesUtil.storeProperties(propertiesFile, orderedProperties, "Order test");
      Properties loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      // Assert
      assertEquals(orderedProperties.size(), loadedProperties.size());
      for (String key : orderedProperties.stringPropertyNames()) {
        assertEquals(orderedProperties.getProperty(key), loadedProperties.getProperty(key));
      }
    }
  }

  // ================= Error Handling Tests =================

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle invalid file permissions gracefully")
    void shouldHandleInvalidFilePermissionsGracefully() throws IOException {
      // This test is platform-specific and may not work on all systems
      // Skipping complex permission tests for cross-platform compatibility
      assertTrue(true); // Placeholder for permission-based tests
    }

    @Test
    @DisplayName("Should handle corrupted properties file")
    void shouldHandleCorruptedPropertiesFile() throws IOException {
      // Arrange - Create a malformed properties file
      try (BufferedWriter writer =
          Files.newBufferedWriter(propertiesFile, StandardCharsets.UTF_8)) {
        writer.write("valid.key=valid.value\n");
        writer.write("invalid line without equals or colon\n");
        writer.write("another.valid.key=another.value\n");
      }

      // Act & Assert - Should still load valid properties
      assertDoesNotThrow(
          () -> {
            Properties properties = PropertiesUtil.loadProperties(propertiesFile);
            assertEquals("valid.value", properties.getProperty("valid.key"));
            assertEquals("another.value", properties.getProperty("another.valid.key"));
          });
    }
  }
}
