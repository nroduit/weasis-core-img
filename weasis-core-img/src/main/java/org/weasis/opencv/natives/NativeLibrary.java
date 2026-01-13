/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.natives;

import java.nio.file.Path;
import java.util.Map;
import org.opencv.core.Core;

/**
 * Utility class for determining native library specifications based on the current operating system
 * and architecture. This class follows OSGi naming conventions as defined by <a
 * href="https://docs.osgi.org/reference/osnames.html">OSGI</a>
 *
 * <p>The class provides thread-safe caching of the native library specification to avoid repeated
 * system property lookups.
 */
public final class NativeLibrary {
  private static volatile boolean libraryLoaded = false;
  private static final Object LIBRARY_LOCK = new Object();

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

  // Architecture Mappings - Using Map.of for immutability
  private static final Map<String, String> ARCH_MAPPINGS =
      Map.ofEntries(
          // x86-64 variants
          Map.entry(ARCH_X86_64, ARCH_X86_64),
          Map.entry("amd64", ARCH_X86_64),
          Map.entry("em64t", ARCH_X86_64),
          Map.entry("x86_64", ARCH_X86_64),
          // ARM64 variants
          Map.entry(ARCH_AARCH64, ARCH_AARCH64),
          Map.entry("arm64", ARCH_AARCH64),
          // ARM variants
          Map.entry("arm", ARCH_ARMV7A),
          // x86 variants
          Map.entry("pentium", ARCH_X86),
          Map.entry("i386", ARCH_X86),
          Map.entry("i486", ARCH_X86),
          Map.entry("i586", ARCH_X86),
          Map.entry("i686", ARCH_X86),
          // Other architectures
          Map.entry("power ppc", ARCH_POWERPC),
          Map.entry("psc1k", ARCH_IGNITE));

  private static volatile String cachedSpecification;

  private NativeLibrary() {}

  /**
   * Gets the native library specification string in the format "osname-architecture". This method
   * is thread-safe and caches the result for improved performance.
   *
   * @return the native library specification string (e.g., "windows-x86-64", "linux-aarch64")
   * @throws IllegalStateException if system properties cannot be determined
   */
  public static String getNativeLibSpecification() {
    // Double-checked locking pattern with volatile field
    var result = cachedSpecification;
    if (result == null) {
      synchronized (NativeLibrary.class) {
        result = cachedSpecification;
        if (result == null) {
          cachedSpecification = result = buildNativeLibSpecification();
        }
      }
    }
    return result;
  }

  /**
   * Loads a native library from the specified absolute path. Use the classloader's default library.
   *
   * @param absolutePath the absolute path to the native library
   * @throws UnsatisfiedLinkError if the library cannot be loaded
   */
  public static void loadLibraryFromAbsolutePath(Path absolutePath) {
    if (libraryLoaded) {
      return;
    }

    synchronized (LIBRARY_LOCK) {
      if (libraryLoaded) {
        return;
      }

      try {
        System.load(absolutePath.toAbsolutePath().toString());
        libraryLoaded = true;
      } catch (Throwable e) {
        System.err.println("Cannot load OpenCV native library: " + e.getMessage());
      }
    }
  }

  /**
   * Loads the OpenCV native library using the standard library name. The library must be available
   * in the system's library path.
   *
   * @throws UnsatisfiedLinkError if the library cannot be loaded
   */
  public static void loadLibraryFromLibraryName() {
    if (libraryLoaded) {
      return;
    }

    synchronized (LIBRARY_LOCK) {
      if (libraryLoaded) {
        return;
      }

      try {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        libraryLoaded = true;
      } catch (Throwable e) {
        System.err.println("Cannot load OpenCV native library: " + e.getMessage());
      }
    }
  }

  private static String buildNativeLibSpecification() {
    var rawOsName = System.getProperty(PROP_OS_NAME, "");
    var rawOsArch = System.getProperty(PROP_OS_ARCH, "");

    var normalizedOsName = normalizeOsName(rawOsName);
    var normalizedOsArch = normalizeArchitecture(rawOsArch);

    return normalizedOsName + "-" + normalizedOsArch;
  }

  private static String normalizeOsName(String rawOsName) {
    if (rawOsName.isBlank()) {
      throw new IllegalStateException("OS name system property is null or empty");
    }

    var osName = rawOsName.toLowerCase();

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

  private static String normalizeArchitecture(String rawOsArch) {
    if (rawOsArch.isBlank()) {
      throw new IllegalStateException("OS architecture system property is null or empty");
    }

    return ARCH_MAPPINGS.getOrDefault(rawOsArch.toLowerCase(), rawOsArch.toLowerCase());
  }

  /** Clears the cached native library specification for testing purposes. */
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
