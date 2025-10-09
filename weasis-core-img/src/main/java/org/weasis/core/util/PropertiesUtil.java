/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for handling Java Properties files with modern NIO.2 APIs. Provides methods for
 * loading, storing, and manipulating properties with UTF-8 encoding by default and enhanced error
 * handling.
 *
 * @author Nicolas Roduit
 */
public final class PropertiesUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesUtil.class);

  private PropertiesUtil() {}

  /**
   * Loads properties from a file using UTF-8 encoding.
   *
   * @param path the path to the properties file
   * @return the loaded properties, never null (empty if file doesn't exist)
   */
  public static Properties loadProperties(Path path) {
    return loadProperties(path, null, StandardCharsets.UTF_8);
  }

  /**
   * Loads properties from a file and merges them into an existing Properties object.
   *
   * @param path the path to the properties file
   * @param target the properties object to merge into (if null, creates a new one)
   * @return the target properties object with loaded properties merged in
   */
  public static Properties loadProperties(Path path, Properties target) {
    return loadProperties(path, target, StandardCharsets.UTF_8);
  }

  /**
   * Loads properties from a file with specified charset encoding.
   *
   * @param path the path to the properties file
   * @param target the properties object to merge into (if null, creates a new one)
   * @param charset the charset to use for reading
   * @return the loaded properties, never null (empty if file doesn't exist)
   */
  public static Properties loadProperties(Path path, Properties target, Charset charset) {
    Objects.requireNonNull(path, "Path cannot be null");
    Objects.requireNonNull(charset, "Charset cannot be null");

    var properties = Objects.requireNonNullElseGet(target, Properties::new);

    if (!isReadableFile(path)) {
      LOGGER.debug("Properties file not found or not readable: {}", path);
      return properties;
    }

    try (var reader = Files.newBufferedReader(path, charset)) {
      properties.load(reader);
      LOGGER.trace("Loaded {} properties from: {}", properties.size(), path);
    } catch (IOException e) {
      LOGGER.error("Failed to load properties from file: {}", path, e);
    }

    return properties;
  }

  /**
   * Stores properties to a file using UTF-8 encoding.
   *
   * @param path the path to the properties file
   * @param properties the properties to store
   * @param comments optional comments to write at the beginning of the file
   */
  public static void storeProperties(Path path, Properties properties, String comments) {
    storeProperties(path, properties, comments, StandardCharsets.UTF_8);
  }

  /**
   * Stores properties to a file with specified charset encoding.
   *
   * @param path the path to the properties file
   * @param properties the properties to store
   * @param comments optional comments to write at the beginning of the file
   * @param charset the charset to use for writing
   */
  public static void storeProperties(
      Path path, Properties properties, String comments, Charset charset) {
    Objects.requireNonNull(path, "Path cannot be null");
    Objects.requireNonNull(properties, "Properties cannot be null");
    Objects.requireNonNull(charset, "Charset cannot be null");

    if (!createParentDirectories(path)) {
      return;
    }

    try (var writer =
        Files.newBufferedWriter(
            path,
            charset,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING)) {
      properties.store(writer, comments);
      LOGGER.trace("Stored {} properties to: {}", properties.size(), path);
    } catch (IOException e) {
      LOGGER.error("Failed to store properties to file: {}", path, e);
    }
  }

  /**
   * Creates a Properties object from a Map.
   *
   * @param map the map to convert
   * @return a new Properties object containing the map entries
   */
  public static Properties fromMap(Map<String, String> map) {
    var properties = new Properties();
    if (map != null) {
      properties.putAll(map);
    }
    return properties;
  }

  /**
   * Merges multiple Properties objects into a new one. Later properties override earlier ones for
   * duplicate keys.
   *
   * @param propertiesArray array of Properties objects to merge
   * @return a new Properties object containing merged properties
   */
  public static Properties merge(Properties... propertiesArray) {
    var merged = new Properties();
    if (propertiesArray != null) {
      for (var props : propertiesArray) {
        if (props != null) {
          merged.putAll(props);
        }
      }
    }
    return merged;
  }

  private static boolean isReadableFile(Path path) {
    return Files.exists(path) && Files.isReadable(path);
  }

  private static boolean createParentDirectories(Path path) {
    try {
      FileUtil.prepareToWriteFile(path);
      return true;
    } catch (IOException e) {
      LOGGER.error("Failed to create parent directories for: {}", path, e);
      return false;
    }
  }
}
