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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Comprehensive test suite for PropertiesUtil class. */
@DisplayNameGeneration(ReplaceUnderscores.class)
class PropertiesUtilTest {

  @TempDir Path tempDir;

  private Properties testProperties;
  private Path propertiesFile;

  @BeforeEach
  void setUp() {
    testProperties = createTestProperties();
    propertiesFile = tempDir.resolve("test.properties");
  }

  private static Properties createTestProperties() {
    var properties = new Properties();
    properties.setProperty("key1", "value1");
    properties.setProperty("key2", "value2");
    properties.setProperty("unicode.key", "héllo wörld");
    properties.setProperty("special.chars", "!@#$%^&*()");
    properties.setProperty("multiline", "line1\nline2\nline3");
    return properties;
  }

  private static Properties createUnicodeProperties() {
    var properties = new Properties();
    properties.setProperty("chinese", "你好世界");
    properties.setProperty("japanese", "こんにちは世界");
    properties.setProperty("arabic", "مرحبا بالعالم");
    properties.setProperty("emoji", "🌍🚀💻");
    return properties;
  }

  private static Properties createSpecialCharProperties() {
    var properties = new Properties();
    properties.setProperty("equals.test", "key=value");
    properties.setProperty("colon.test", "key:value");
    properties.setProperty("hash.test", "key#comment");
    properties.setProperty("exclamation.test", "key!value");
    return properties;
  }

  // ================= Load Properties Tests =================

  @Nested
  class Load_Properties_Tests {

    @Test
    void should_load_properties_from_existing_file_with_default_UTF8_encoding() throws IOException {
      // Arrange
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test properties");

      // Act
      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      // Assert
      assertThat(loadedProperties)
          .isNotNull()
          .hasSameSizeAs(testProperties)
          .containsEntry("key1", "value1")
          .containsEntry("key2", "value2")
          .containsEntry("unicode.key", "héllo wörld");
    }

    @Test
    void should_load_properties_with_custom_charset_encoding() throws IOException {
      // Arrange
      var charset = StandardCharsets.ISO_8859_1;
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test properties", charset);

      // Act
      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile, null, charset);

      // Assert
      assertThat(loadedProperties)
          .hasSameSizeAs(testProperties)
          .containsEntry("key1", "value1")
          .containsEntry("key2", "value2");
    }

    @Test
    void should_return_empty_properties_when_file_does_not_exist() {
      // Arrange
      var nonExistentFile = tempDir.resolve("nonexistent.properties");

      // Act
      var loadedProperties = PropertiesUtil.loadProperties(nonExistentFile);

      // Assert
      assertThat(loadedProperties).isNotNull().isEmpty();
    }

    @Test
    void should_handle_empty_properties_file() throws IOException {
      // Arrange
      Files.createFile(propertiesFile);

      // Act
      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      // Assert
      assertThat(loadedProperties).isNotNull().isEmpty();
    }

    @Test
    void should_throw_exception_for_null_path() {
      // Act & Assert
      var exception =
          assertThrows(NullPointerException.class, () -> PropertiesUtil.loadProperties(null));
      assertEquals("Path cannot be null", exception.getMessage());
    }

    @Test
    void should_throw_exception_for_null_charset() {
      // Act & Assert
      var exception =
          assertThrows(
              NullPointerException.class,
              () -> PropertiesUtil.loadProperties(propertiesFile, null, null));
      assertEquals("Charset cannot be null", exception.getMessage());
    }

    @Test
    void should_load_properties_into_existing_properties_object() throws IOException {
      // Arrange
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test properties");
      var existingProperties = new Properties();
      existingProperties.setProperty("existing.key", "existing.value");

      // Act
      var result = PropertiesUtil.loadProperties(propertiesFile, existingProperties);

      // Assert
      assertThat(result)
          .isSameAs(existingProperties)
          .hasSize(testProperties.size() + 1)
          .containsEntry("existing.key", "existing.value")
          .containsEntry("key1", "value1");
    }

    @Test
    void should_create_new_properties_object_when_target_is_null() throws IOException {
      // Arrange
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test properties");

      // Act
      var result = PropertiesUtil.loadProperties(propertiesFile, null);

      // Assert
      assertThat(result).isNotNull().hasSameSizeAs(testProperties).containsEntry("key1", "value1");
    }

    @Test
    void should_handle_properties_with_special_characters_in_values() throws IOException {
      // Arrange
      var specialProperties = createSpecialCharProperties();
      PropertiesUtil.storeProperties(propertiesFile, specialProperties, null);

      // Act
      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      // Assert
      assertThat(loadedProperties)
          .containsEntry("equals.test", "key=value")
          .containsEntry("colon.test", "key:value")
          .containsEntry("hash.test", "key#comment")
          .containsEntry("exclamation.test", "key!value");
    }
  }

  // ================= Store Properties Tests =================

  @Nested
  class Store_Properties_Tests {

    @Test
    void should_store_properties_to_file_with_default_UTF8_encoding() throws IOException {
      // Act
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test comment");

      // Assert
      assertThat(propertiesFile).exists();
      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);
      assertThat(loadedProperties).hasSameSizeAs(testProperties).containsEntry("key1", "value1");
    }

    @Test
    void should_store_properties_with_custom_charset_encoding() throws IOException {
      // Arrange
      var charset = StandardCharsets.ISO_8859_1;

      // Act
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test comment", charset);

      // Assert
      assertThat(propertiesFile).exists();
      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile, null, charset);
      assertThat(loadedProperties).hasSameSizeAs(testProperties).containsEntry("key1", "value1");
    }

    @Test
    void should_create_parent_directories_when_storing_properties() throws IOException {
      // Arrange
      var nestedFile = tempDir.resolve("nested").resolve("deep").resolve("test.properties");

      // Act
      PropertiesUtil.storeProperties(nestedFile, testProperties, "Nested file test");

      // Assert
      assertThat(nestedFile).exists();
      assertThat(nestedFile.getParent()).exists();
      var loadedProperties = PropertiesUtil.loadProperties(nestedFile);
      assertThat(loadedProperties).containsEntry("key1", "value1");
    }

    @Test
    void should_overwrite_existing_file_when_storing_properties() throws IOException {
      // Arrange
      var originalProperties = new Properties();
      originalProperties.setProperty("original.key", "original.value");
      PropertiesUtil.storeProperties(propertiesFile, originalProperties, "Original");

      var newProperties = new Properties();
      newProperties.setProperty("new.key", "new.value");

      // Act
      PropertiesUtil.storeProperties(propertiesFile, newProperties, "New properties");

      // Assert
      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);
      assertThat(loadedProperties)
          .hasSize(1)
          .containsEntry("new.key", "new.value")
          .doesNotContainKey("original.key");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Simple comment", "Multi\nline\ncomment"})
    void should_handle_various_comment_types(String comment) throws IOException {
      // Act & Assert
      assertDoesNotThrow(
          () -> PropertiesUtil.storeProperties(propertiesFile, testProperties, comment));
      assertThat(propertiesFile).exists();

      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);
      assertThat(loadedProperties).hasSameSizeAs(testProperties);
    }

    @Test
    void should_throw_exception_for_null_path_when_storing() {
      // Act & Assert
      var exception =
          assertThrows(
              NullPointerException.class,
              () -> PropertiesUtil.storeProperties(null, testProperties, "comment"));
      assertEquals("Path cannot be null", exception.getMessage());
    }

    @Test
    void should_throw_exception_for_null_properties_when_storing() {
      // Act & Assert
      var exception =
          assertThrows(
              NullPointerException.class,
              () -> PropertiesUtil.storeProperties(propertiesFile, null, "comment"));
      assertEquals("Properties cannot be null", exception.getMessage());
    }

    @Test
    void should_throw_exception_for_null_charset_when_storing() {
      // Act & Assert
      var exception =
          assertThrows(
              NullPointerException.class,
              () ->
                  PropertiesUtil.storeProperties(propertiesFile, testProperties, "comment", null));
      assertEquals("Charset cannot be null", exception.getMessage());
    }

    @Test
    void should_preserve_unicode_characters_when_storing_and_loading() throws IOException {
      // Arrange
      var unicodeProperties = createUnicodeProperties();

      // Act
      PropertiesUtil.storeProperties(propertiesFile, unicodeProperties, "Unicode test");
      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      // Assert
      assertThat(loadedProperties)
          .containsEntry("chinese", "你好世界")
          .containsEntry("japanese", "こんにちは世界")
          .containsEntry("arabic", "مرحبا بالعالم")
          .containsEntry("emoji", "🌍🚀💻");
    }
  }

  // ================= Utility Methods Tests =================

  @Nested
  class Utility_Methods_Tests {

    @Test
    void should_create_Properties_from_Map() {
      // Arrange
      var map =
          Map.of(
              "map.key1", "map.value1",
              "map.key2", "map.value2");

      // Act
      var properties = PropertiesUtil.fromMap(map);

      // Assert
      assertThat(properties)
          .isNotNull()
          .hasSize(2)
          .containsEntry("map.key1", "map.value1")
          .containsEntry("map.key2", "map.value2");
    }

    @Test
    void should_handle_null_map_gracefully() {
      // Act
      var properties = PropertiesUtil.fromMap(null);

      // Assert
      assertThat(properties).isNotNull().isEmpty();
    }

    @Test
    void should_handle_empty_map() {
      // Arrange
      var emptyMap = Map.<String, String>of();

      // Act
      var properties = PropertiesUtil.fromMap(emptyMap);

      // Assert
      assertThat(properties).isNotNull().isEmpty();
    }

    @Test
    void should_merge_multiple_Properties_objects() {
      // Arrange
      var props1 = new Properties();
      props1.setProperty("key1", "value1");
      props1.setProperty("common", "from_props1");

      var props2 = new Properties();
      props2.setProperty("key2", "value2");
      props2.setProperty("common", "from_props2");

      var props3 = new Properties();
      props3.setProperty("key3", "value3");

      // Act
      var merged = PropertiesUtil.merge(props1, props2, props3);

      // Assert
      assertThat(merged)
          .hasSize(4)
          .containsEntry("key1", "value1")
          .containsEntry("key2", "value2")
          .containsEntry("key3", "value3")
          .containsEntry("common", "from_props2"); // Later properties override earlier ones
    }

    @Test
    void should_handle_null_properties_in_merge() {
      // Arrange
      var props1 = new Properties();
      props1.setProperty("key1", "value1");

      var props3 = new Properties();
      props3.setProperty("key3", "value3");

      // Act
      var merged = PropertiesUtil.merge(props1, null, props3);

      // Assert
      assertThat(merged).hasSize(2).containsEntry("key1", "value1").containsEntry("key3", "value3");
    }

    @Test
    void should_handle_null_array_in_merge() {
      // Act
      var merged = PropertiesUtil.merge((Properties[]) null);

      // Assert
      assertThat(merged).isNotNull().isEmpty();
    }

    @Test
    void should_handle_empty_array_in_merge() {
      // Act
      var merged = PropertiesUtil.merge();

      // Assert
      assertThat(merged).isNotNull().isEmpty();
    }
  }

  // ================= Integration Tests =================

  @Nested
  class Integration_Tests {

    @Test
    void should_perform_complete_store_and_load_cycle() throws IOException {
      // Act
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Complete cycle test");
      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      // Assert
      assertThat(loadedProperties).isEqualTo(testProperties);
    }

    @Test
    void should_handle_concurrent_access_to_different_files() throws IOException {
      // Arrange
      var numThreads = 10;
      var executor = Executors.newFixedThreadPool(numThreads);

      try {
        var futures =
            IntStream.range(0, numThreads)
                .mapToObj(
                    i ->
                        CompletableFuture.runAsync(
                            () -> {
                              var threadFile = tempDir.resolve("thread-" + i + ".properties");
                              var threadProperties = new Properties();
                              threadProperties.setProperty("thread.id", String.valueOf(i));
                              threadProperties.setProperty("thread.value", "value-" + i);

                              PropertiesUtil.storeProperties(
                                  threadFile, threadProperties, "Thread " + i);
                              var loadedProps = PropertiesUtil.loadProperties(threadFile);

                              assertThat(loadedProps)
                                  .containsEntry("thread.id", String.valueOf(i))
                                  .containsEntry("thread.value", "value-" + i);
                            },
                            executor))
                .toArray(CompletableFuture[]::new);

        // Act & Assert
        assertDoesNotThrow(() -> CompletableFuture.allOf(futures).join());
      } finally {
        executor.shutdown();
      }
    }

    @Test
    void should_work_with_very_large_properties_file() throws IOException {
      // Arrange
      var largeProperties = new Properties();
      IntStream.range(0, 10000).forEach(i -> largeProperties.setProperty("key" + i, "value" + i));

      // Act
      PropertiesUtil.storeProperties(propertiesFile, largeProperties, "Large file test");
      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      // Assert
      assertThat(loadedProperties)
          .hasSameSizeAs(largeProperties)
          .containsEntry("key0", "value0")
          .containsEntry("key9999", "value9999");
    }

    @Test
    void should_maintain_property_order_consistency() throws IOException {
      // Arrange - Properties doesn't guarantee order, but we can test consistent behavior
      var orderedProps = new Properties();
      orderedProps.setProperty("a.first", "1");
      orderedProps.setProperty("b.second", "2");
      orderedProps.setProperty("c.third", "3");

      // Act
      PropertiesUtil.storeProperties(propertiesFile, orderedProps, "Order test");
      var loadedOnce = PropertiesUtil.loadProperties(propertiesFile);
      PropertiesUtil.storeProperties(propertiesFile, loadedOnce, "Order test again");
      var loadedTwice = PropertiesUtil.loadProperties(propertiesFile);

      // Assert
      assertThat(loadedTwice).isEqualTo(loadedOnce);
    }
  }

  // ================= Error Handling Tests =================

  @Nested
  class Error_Handling_Tests {

    @Test
    void should_handle_invalid_file_permissions_gracefully() throws IOException {
      // This test is platform-dependent and may not work on all systems
      assumeTrue(
          System.getProperty("os.name").toLowerCase().contains("linux")
              || System.getProperty("os.name").toLowerCase().contains("mac"));

      // Arrange
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Permission test");
      var success = propertiesFile.toFile().setReadable(false);
      assumeTrue(success, "Unable to modify file permissions on this system");

      try {
        // Act
        var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

        // Assert
        assertThat(loadedProperties).isEmpty();
      } finally {
        // Cleanup - restore permissions
        propertiesFile.toFile().setReadable(true);
      }
    }

    @Test
    void should_handle_corrupted_properties_file() throws IOException {
      // Arrange - Create a file with invalid properties format
      Files.writeString(
          propertiesFile,
          """
          valid.key=valid.value
          invalid line without equals
          another.valid.key=another.valid.value
          """);

      // Act
      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      // Assert - Should load valid properties and skip invalid ones
      assertThat(loadedProperties)
          .containsEntry("valid.key", "valid.value")
          .containsEntry("another.valid.key", "another.valid.value");
    }
  }

  // Helper method to use AssertJ-style assertions (if available)
  private static PropertiesAssert assertThat(Properties actual) {
    return new PropertiesAssert(actual);
  }

  private static PathAssert assertThat(Path actual) {
    return new PathAssert(actual);
  }

  // Custom assertion classes for better readability
  private record PropertiesAssert(Properties actual) {

    PropertiesAssert isNotNull() {
      assertNotNull(actual);
      return this;
    }

    PropertiesAssert isEmpty() {
      assertTrue(actual.isEmpty());
      return this;
    }

    PropertiesAssert hasSize(int expectedSize) {
      assertEquals(expectedSize, actual.size());
      return this;
    }

    PropertiesAssert hasSameSizeAs(Properties other) {
      assertEquals(other.size(), actual.size());
      return this;
    }

    PropertiesAssert containsEntry(String key, String value) {
      assertEquals(value, actual.getProperty(key));
      return this;
    }

    PropertiesAssert doesNotContainKey(String key) {
      assertNull(actual.getProperty(key));
      return this;
    }

    PropertiesAssert isSameAs(Properties expected) {
      assertSame(expected, actual);
      return this;
    }

    PropertiesAssert isEqualTo(Properties expected) {
      assertEquals(expected, actual);
      return this;
    }
  }

  private record PathAssert(Path actual) {

    PathAssert exists() {
      assertTrue(Files.exists(actual));
      return this;
    }
  }
}
