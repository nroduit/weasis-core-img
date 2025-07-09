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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for determining native library specifications based on the current operating system
 * and architecture. This class follows OSGi naming conventions as defined by <a
 * href="https://docs.osgi.org/reference/osnames.html">OSGI</a>
 *
 * <p>The class provides thread-safe caching of the native library specification to avoid repeated
 * system property lookups.
 */
public final class NativeLibrary {

  // OS Name Constants
  private static final String OS_WINDOWS = "windows";
  private static final String OS_MACOSX = "macosx";
  private static final String OS_LINUX = "linux";
  private static final String OS_EPOC32 = "epoc32";
  private static final String OS_HPUX = "hpux";
  private static final String OS_OS2 = "os2";
  private static final String OS_QNX = "qnx";

  // Architecture Constants
  private static final String ARCH_X86_64 = "x86-64";
  private static final String ARCH_AARCH64 = "aarch64";
  private static final String ARCH_ARMV7A = "armv7a";
  private static final String ARCH_X86 = "x86";
  private static final String ARCH_POWERPC = "powerpc";
  private static final String ARCH_IGNITE = "ignite";

  // System Property Keys
  private static final String PROP_OS_NAME = "os.name";
  private static final String PROP_OS_ARCH = "os.arch";

  // OS Name Mappings
  private static final Map<String, String> OS_NAME_MAPPINGS =
      Map.of(
          "symbianos", OS_EPOC32,
          "hp-ux", OS_HPUX,
          "os/2", OS_OS2,
          "procnto", OS_QNX);

  // Architecture Mappings
  private static final Map<String, String> ARCH_MAPPINGS = new ConcurrentHashMap<>();

  static {
    // x86-64 variants
    ARCH_MAPPINGS.put(ARCH_X86_64, ARCH_X86_64);
    ARCH_MAPPINGS.put("amd64", ARCH_X86_64);
    ARCH_MAPPINGS.put("em64t", ARCH_X86_64);
    ARCH_MAPPINGS.put("x86_64", ARCH_X86_64);

    // ARM64 variants
    ARCH_MAPPINGS.put(ARCH_AARCH64, ARCH_AARCH64);
    ARCH_MAPPINGS.put("arm64", ARCH_AARCH64);

    // ARM variants
    ARCH_MAPPINGS.put("arm", ARCH_ARMV7A);

    // x86 variants
    ARCH_MAPPINGS.put("pentium", ARCH_X86);
    ARCH_MAPPINGS.put("i386", ARCH_X86);
    ARCH_MAPPINGS.put("i486", ARCH_X86);
    ARCH_MAPPINGS.put("i586", ARCH_X86);
    ARCH_MAPPINGS.put("i686", ARCH_X86);

    // Other architectures
    ARCH_MAPPINGS.put("power ppc", ARCH_POWERPC);
    ARCH_MAPPINGS.put("psc1k", ARCH_IGNITE);
  }

  // Cache for the native library specification
  private static volatile String cachedSpecification;

  /** Private constructor to prevent instantiation of this utility class. */
  private NativeLibrary() {}

  /**
   * Gets the native library specification string in the format "osname-architecture". This method
   * is thread-safe and caches the result for improved performance.
   *
   * @return the native library specification string (e.g., "windows-x86-64", "linux-aarch64")
   * @throws IllegalStateException if system properties cannot be determined
   */
  public static String getNativeLibSpecification() {
    if (cachedSpecification == null) {
      synchronized (NativeLibrary.class) {
        if (cachedSpecification == null) {
          cachedSpecification = buildNativeLibSpecification();
        }
      }
    }
    return cachedSpecification;
  }

  /**
   * Builds the native library specification by normalizing the OS name and architecture.
   *
   * @return the native library specification string
   * @throws IllegalStateException if system properties cannot be determined
   */
  private static String buildNativeLibSpecification() {
    String rawOsName = System.getProperty(PROP_OS_NAME);
    String rawOsArch = System.getProperty(PROP_OS_ARCH);

    String normalizedOsName = normalizeOsName(rawOsName);
    String normalizedOsArch = normalizeArchitecture(rawOsArch);

    return normalizedOsName + "-" + normalizedOsArch;
  }

  /**
   * Normalizes the operating system name according to OSGi conventions.
   *
   * @param rawOsName the raw OS name from system properties
   * @return the normalized OS name
   */
  private static String normalizeOsName(String rawOsName) {
    if (rawOsName == null || rawOsName.isEmpty()) {
      throw new IllegalStateException("OS name system property is null or empty");
    }

    String osName = rawOsName.toLowerCase();

    // Handle common OS prefixes
    if (osName.startsWith("win")) {
      return OS_WINDOWS;
    } else if (osName.startsWith("mac")) {
      return OS_MACOSX;
    } else if (osName.startsWith(OS_LINUX)) {
      return OS_LINUX;
    }

    // Handle specific OS mappings
    return OS_NAME_MAPPINGS.getOrDefault(osName, osName);
  }

  /**
   * Normalizes the processor architecture according to OSGi conventions.
   *
   * @param rawOsArch the raw architecture from system properties
   * @return the normalized architecture
   */
  private static String normalizeArchitecture(String rawOsArch) {
    if (rawOsArch == null || rawOsArch.isEmpty()) {
      throw new IllegalStateException("OS architecture system property is null or empty");
    }

    String osArch = rawOsArch.toLowerCase();
    return ARCH_MAPPINGS.getOrDefault(osArch, osArch);
  }

  /**
   * Clears the cached native library specification. This method is primarily intended for testing
   * purposes.
   */
  static void clearCache() {
    synchronized (NativeLibrary.class) {
      cachedSpecification = null;
    }
  }

  /**
   * Main method for testing the native library specification determination.
   *
   * @param args command line arguments (not used)
   */
  public static void main(String[] args) {
    try {
      System.out.println(getNativeLibSpecification());
    } catch (Exception e) {
      System.err.println("Error determining native library specification: " + e.getMessage());
      System.exit(1);
    }
  }
}
