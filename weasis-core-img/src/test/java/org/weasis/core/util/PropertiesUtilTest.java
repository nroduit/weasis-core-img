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

  @Nested
  class Load_Properties_Tests {

    @Test
    void should_load_properties_from_existing_file_with_default_UTF8_encoding() throws IOException {
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test properties");

      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      assertAll(
          () -> assertNotNull(loadedProperties),
          () -> assertEquals(testProperties.size(), loadedProperties.size()),
          () -> assertEquals("value1", loadedProperties.getProperty("key1")),
          () -> assertEquals("value2", loadedProperties.getProperty("key2")),
          () -> assertEquals("héllo wörld", loadedProperties.getProperty("unicode.key")));
    }

    @Test
    void should_load_properties_with_custom_charset_encoding() throws IOException {
      var charset = StandardCharsets.ISO_8859_1;
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test properties", charset);

      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile, null, charset);

      assertAll(
          () -> assertEquals(testProperties.size(), loadedProperties.size()),
          () -> assertEquals("value1", loadedProperties.getProperty("key1")),
          () -> assertEquals("value2", loadedProperties.getProperty("key2")));
    }

    @Test
    void should_return_empty_properties_when_file_does_not_exist() {
      var nonExistentFile = tempDir.resolve("nonexistent.properties");

      var loadedProperties = PropertiesUtil.loadProperties(nonExistentFile);

      assertAll(() -> assertNotNull(loadedProperties), () -> assertTrue(loadedProperties.isEmpty()));
    }

    @Test
    void should_handle_empty_properties_file() throws IOException {
      Files.createFile(propertiesFile);

      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      assertAll(() -> assertNotNull(loadedProperties), () -> assertTrue(loadedProperties.isEmpty()));
    }

    @Test
    void should_throw_exception_for_null_path() {
      var exception =
          assertThrows(NullPointerException.class, () -> PropertiesUtil.loadProperties(null));
      assertEquals("Path cannot be null", exception.getMessage());
    }

    @Test
    void should_throw_exception_for_null_charset() {
      var exception =
          assertThrows(
              NullPointerException.class,
              () -> PropertiesUtil.loadProperties(propertiesFile, null, null));
      assertEquals("Charset cannot be null", exception.getMessage());
    }

    @Test
    void should_load_properties_into_existing_properties_object() throws IOException {
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test properties");
      var existingProperties = new Properties();
      existingProperties.setProperty("existing.key", "existing.value");

      var result = PropertiesUtil.loadProperties(propertiesFile, existingProperties);

      assertAll(
          () -> assertSame(existingProperties, result),
          () -> assertEquals(testProperties.size() + 1, result.size()),
          () -> assertEquals("existing.value", result.getProperty("existing.key")),
          () -> assertEquals("value1", result.getProperty("key1")));
    }

    @Test
    void should_create_new_properties_object_when_target_is_null() throws IOException {
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test properties");

      var result = PropertiesUtil.loadProperties(propertiesFile, null);

      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(testProperties.size(), result.size()),
          () -> assertEquals("value1", result.getProperty("key1")));
    }

    @Test
    void should_handle_properties_with_special_characters_in_values() throws IOException {
      var specialProperties = createSpecialCharProperties();
      PropertiesUtil.storeProperties(propertiesFile, specialProperties, null);

      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      assertAll(
          () -> assertEquals("key=value", loadedProperties.getProperty("equals.test")),
          () -> assertEquals("key:value", loadedProperties.getProperty("colon.test")),
          () -> assertEquals("key#comment", loadedProperties.getProperty("hash.test")),
          () -> assertEquals("key!value", loadedProperties.getProperty("exclamation.test")));
    }
  }

  @Nested
  class Store_Properties_Tests {

    @Test
    void should_store_properties_to_file_with_default_UTF8_encoding() throws IOException {
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test comment");

      assertTrue(Files.exists(propertiesFile));
      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);
      assertAll(
          () -> assertEquals(testProperties.size(), loadedProperties.size()),
          () -> assertEquals("value1", loadedProperties.getProperty("key1")));
    }

    @Test
    void should_store_properties_with_custom_charset_encoding() throws IOException {
      var charset = StandardCharsets.ISO_8859_1;

      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Test comment", charset);

      assertTrue(Files.exists(propertiesFile));
      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile, null, charset);
      assertAll(
          () -> assertEquals(testProperties.size(), loadedProperties.size()),
          () -> assertEquals("value1", loadedProperties.getProperty("key1")));
    }

    @Test
    void should_create_parent_directories_when_storing_properties() throws IOException {
      var nestedFile = tempDir.resolve("nested").resolve("deep").resolve("test.properties");

      PropertiesUtil.storeProperties(nestedFile, testProperties, "Nested file test");

      assertAll(
          () -> assertTrue(Files.exists(nestedFile)),
          () -> assertTrue(Files.exists(nestedFile.getParent())),
          () ->
              assertEquals(
                  "value1", PropertiesUtil.loadProperties(nestedFile).getProperty("key1")));
    }

    @Test
    void should_overwrite_existing_file_when_storing_properties() throws IOException {
      var originalProperties = new Properties();
      originalProperties.setProperty("original.key", "original.value");
      PropertiesUtil.storeProperties(propertiesFile, originalProperties, "Original");

      var newProperties = new Properties();
      newProperties.setProperty("new.key", "new.value");

      PropertiesUtil.storeProperties(propertiesFile, newProperties, "New properties");

      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);
      assertAll(
          () -> assertEquals(1, loadedProperties.size()),
          () -> assertEquals("new.value", loadedProperties.getProperty("new.key")),
          () -> assertNull(loadedProperties.getProperty("original.key")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Simple comment", "Multi\nline\ncomment"})
    void should_handle_various_comment_types(String comment) throws IOException {
      assertDoesNotThrow(
          () -> PropertiesUtil.storeProperties(propertiesFile, testProperties, comment));
      assertTrue(Files.exists(propertiesFile));

      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);
      assertEquals(testProperties.size(), loadedProperties.size());
    }

    @Test
    void should_throw_exception_for_null_path_when_storing() {
      var exception =
          assertThrows(
              NullPointerException.class,
              () -> PropertiesUtil.storeProperties(null, testProperties, "comment"));
      assertEquals("Path cannot be null", exception.getMessage());
    }

    @Test
    void should_throw_exception_for_null_properties_when_storing() {
      var exception =
          assertThrows(
              NullPointerException.class,
              () -> PropertiesUtil.storeProperties(propertiesFile, null, "comment"));
      assertEquals("Properties cannot be null", exception.getMessage());
    }

    @Test
    void should_throw_exception_for_null_charset_when_storing() {
      var exception =
          assertThrows(
              NullPointerException.class,
              () ->
                  PropertiesUtil.storeProperties(propertiesFile, testProperties, "comment", null));
      assertEquals("Charset cannot be null", exception.getMessage());
    }

    @Test
    void should_preserve_unicode_characters_when_storing_and_loading() throws IOException {
      var unicodeProperties = createUnicodeProperties();

      PropertiesUtil.storeProperties(propertiesFile, unicodeProperties, "Unicode test");
      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      assertAll(
          () -> assertEquals("你好世界", loadedProperties.getProperty("chinese")),
          () -> assertEquals("こんにちは世界", loadedProperties.getProperty("japanese")),
          () -> assertEquals("مرحبا بالعالم", loadedProperties.getProperty("arabic")),
          () -> assertEquals("🌍🚀💻", loadedProperties.getProperty("emoji")));
    }
  }

  @Nested
  class Utility_Methods_Tests {

    @Test
    void should_create_Properties_from_Map() {
      var map =
          Map.of(
              "map.key1", "map.value1",
              "map.key2", "map.value2");

      var properties = PropertiesUtil.fromMap(map);

      assertAll(
          () -> assertNotNull(properties),
          () -> assertEquals(2, properties.size()),
          () -> assertEquals("map.value1", properties.getProperty("map.key1")),
          () -> assertEquals("map.value2", properties.getProperty("map.key2")));
    }

    @Test
    void should_handle_null_map_gracefully() {
      var properties = PropertiesUtil.fromMap(null);

      assertAll(() -> assertNotNull(properties), () -> assertTrue(properties.isEmpty()));
    }

    @Test
    void should_handle_empty_map() {
      var emptyMap = Map.<String, String>of();

      var properties = PropertiesUtil.fromMap(emptyMap);

      assertAll(() -> assertNotNull(properties), () -> assertTrue(properties.isEmpty()));
    }

    @Test
    void should_merge_multiple_Properties_objects() {
      var props1 = new Properties();
      props1.setProperty("key1", "value1");
      props1.setProperty("common", "from_props1");

      var props2 = new Properties();
      props2.setProperty("key2", "value2");
      props2.setProperty("common", "from_props2");

      var props3 = new Properties();
      props3.setProperty("key3", "value3");

      var merged = PropertiesUtil.merge(props1, props2, props3);

      assertAll(
          () -> assertEquals(4, merged.size()),
          () -> assertEquals("value1", merged.getProperty("key1")),
          () -> assertEquals("value2", merged.getProperty("key2")),
          () -> assertEquals("value3", merged.getProperty("key3")),
          () ->
              assertEquals(
                  "from_props2",
                  merged.getProperty("common"))); // Later properties override earlier ones
    }

    @Test
    void should_handle_null_properties_in_merge() {
      var props1 = new Properties();
      props1.setProperty("key1", "value1");

      var props3 = new Properties();
      props3.setProperty("key3", "value3");

      var merged = PropertiesUtil.merge(props1, null, props3);

      assertAll(
          () -> assertEquals(2, merged.size()),
          () -> assertEquals("value1", merged.getProperty("key1")),
          () -> assertEquals("value3", merged.getProperty("key3")));
    }

    @Test
    void should_handle_null_array_in_merge() {
      var merged = PropertiesUtil.merge((Properties[]) null);

      assertAll(() -> assertNotNull(merged), () -> assertTrue(merged.isEmpty()));
    }

    @Test
    void should_handle_empty_array_in_merge() {
      var merged = PropertiesUtil.merge();

      assertAll(() -> assertNotNull(merged), () -> assertTrue(merged.isEmpty()));
    }
  }

  @Nested
  class Integration_Tests {

    @Test
    void should_perform_complete_store_and_load_cycle() throws IOException {
      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Complete cycle test");
      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      assertEquals(testProperties, loadedProperties);
    }

    @Test
    void should_handle_concurrent_access_to_different_files() throws IOException {
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

                              assertAll(
                                  () ->
                                      assertEquals(
                                          String.valueOf(i), loadedProps.getProperty("thread.id")),
                                  () ->
                                      assertEquals(
                                          "value-" + i, loadedProps.getProperty("thread.value")));
                            },
                            executor))
                .toArray(CompletableFuture[]::new);

        assertDoesNotThrow(() -> CompletableFuture.allOf(futures).join());
      } finally {
        executor.shutdown();
      }
    }

    @Test
    void should_work_with_very_large_properties_file() throws IOException {
      var largeProperties = new Properties();
      IntStream.range(0, 10000).forEach(i -> largeProperties.setProperty("key" + i, "value" + i));

      PropertiesUtil.storeProperties(propertiesFile, largeProperties, "Large file test");
      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      assertAll(
          () -> assertEquals(largeProperties.size(), loadedProperties.size()),
          () -> assertEquals("value0", loadedProperties.getProperty("key0")),
          () -> assertEquals("value9999", loadedProperties.getProperty("key9999")));
    }

    @Test
    void should_maintain_property_order_consistency() throws IOException {
      var orderedProps = new Properties();
      orderedProps.setProperty("a.first", "1");
      orderedProps.setProperty("b.second", "2");
      orderedProps.setProperty("c.third", "3");

      PropertiesUtil.storeProperties(propertiesFile, orderedProps, "Order test");
      var loadedOnce = PropertiesUtil.loadProperties(propertiesFile);
      PropertiesUtil.storeProperties(propertiesFile, loadedOnce, "Order test again");
      var loadedTwice = PropertiesUtil.loadProperties(propertiesFile);

      assertEquals(loadedOnce, loadedTwice);
    }
  }

  @Nested
  class Error_Handling_Tests {

    @Test
    void should_handle_invalid_file_permissions_gracefully() throws IOException {
      assumeTrue(
          System.getProperty("os.name").toLowerCase().contains("linux")
              || System.getProperty("os.name").toLowerCase().contains("mac"));

      PropertiesUtil.storeProperties(propertiesFile, testProperties, "Permission test");
      var success = propertiesFile.toFile().setReadable(false);
      assumeTrue(success, "Unable to modify file permissions on this system");

      try {
        var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);
        assertTrue(loadedProperties.isEmpty());
      } finally {
        propertiesFile.toFile().setReadable(true);
      }
    }

    @Test
    void should_handle_corrupted_properties_file() throws IOException {
      Files.writeString(
          propertiesFile,
          """
          valid.key=valid.value
          invalid line without equals
          another.valid.key=another.valid.value
          """);

      var loadedProperties = PropertiesUtil.loadProperties(propertiesFile);

      assertAll(
          () -> assertEquals("valid.value", loadedProperties.getProperty("valid.key")),
          () ->
              assertEquals(
                  "another.valid.value", loadedProperties.getProperty("another.valid.key")));
    }
  }
}
