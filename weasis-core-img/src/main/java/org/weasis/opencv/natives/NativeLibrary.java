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
 * Resolves the "osname-architecture" specification of the running platform, following the <a
 * href="https://docs.osgi.org/reference/osnames.html">OSGi naming conventions</a>, and loads the
 * OpenCV native library once. Only Linux, Windows and macOS on 64-bit architectures are supported.
 */
public final class NativeLibrary {
  private static volatile boolean libraryLoaded = false;
  private static final Object LIBRARY_LOCK = new Object();

  private static final String OS_WINDOWS = "windows";
  private static final String OS_MACOSX = "macosx";
  private static final String OS_LINUX = "linux";

  private static final String ARCH_X86_64 = "x86-64";
  private static final String ARCH_AARCH64 = "aarch64";
  private static final String ARCH_POWERPC_64 = "powerpc-64";

  private static final String PROP_OS_NAME = "os.name";
  private static final String PROP_OS_ARCH = "os.arch";

  // 64-bit values of os.arch reported by JVMs on Linux, Windows and macOS
  private static final Map<String, String> ARCH_MAPPINGS =
      Map.ofEntries(
          Map.entry(ARCH_X86_64, ARCH_X86_64),
          Map.entry("x86_64", ARCH_X86_64),
          Map.entry("amd64", ARCH_X86_64),
          Map.entry("em64t", ARCH_X86_64),
          Map.entry(ARCH_AARCH64, ARCH_AARCH64),
          Map.entry("arm64", ARCH_AARCH64),
          Map.entry("ppc64", ARCH_POWERPC_64),
          Map.entry("ppc64le", ARCH_POWERPC_64),
          Map.entry("powerpc64", ARCH_POWERPC_64),
          Map.entry("riscv64", "riscv64"),
          Map.entry("s390x", "s390x"),
          Map.entry("loongarch64", "loongarch64"));

  private static volatile String cachedSpecification;

  private NativeLibrary() {}

  /**
   * Gets the native library specification of the running platform, cached after the first call.
   *
   * @return the specification (e.g., "windows-x86-64", "linux-aarch64")
   * @throws IllegalStateException if the os.name or os.arch system property is missing
   * @throws UnsupportedOperationException if the operating system is not Linux, Windows or macOS,
   *     or the architecture is not 64-bit
   */
  public static String getNativeLibSpecification() {
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
   * Loads the OpenCV native library from an absolute path. Repeat calls are no-ops.
   *
   * @param absolutePath the absolute path to the native library
   */
  public static void loadLibraryFromAbsolutePath(Path absolutePath) {
    loadLibrary(() -> System.load(absolutePath.toAbsolutePath().toString()));
  }

  /**
   * Loads the OpenCV native library by name from the system library path. Repeat calls are no-ops.
   */
  public static void loadLibraryFromLibraryName() {
    loadLibrary(() -> System.loadLibrary(Core.NATIVE_LIBRARY_NAME));
  }

  private static void loadLibrary(Runnable loader) {
    if (libraryLoaded) {
      return;
    }
    synchronized (LIBRARY_LOCK) {
      if (libraryLoaded) {
        return;
      }
      try {
        loader.run();
        libraryLoaded = true;
      } catch (Throwable e) {
        System.err.println("Cannot load OpenCV native library: " + e.getMessage());
      }
    }
  }

  private static String buildNativeLibSpecification() {
    var osName = normalizeOsName(System.getProperty(PROP_OS_NAME, ""));
    var osArch = normalizeArchitecture(System.getProperty(PROP_OS_ARCH, ""));
    return osName + "-" + osArch;
  }

  private static String normalizeOsName(String rawOsName) {
    if (rawOsName.isBlank()) {
      throw new IllegalStateException("OS name system property is null or empty");
    }
    var osName = rawOsName.toLowerCase();
    if (osName.startsWith("win")) {
      return OS_WINDOWS;
    }
    if (osName.startsWith("mac")) {
      return OS_MACOSX;
    }
    if (osName.startsWith(OS_LINUX)) {
      return OS_LINUX;
    }
    throw new UnsupportedOperationException(
        "Unsupported operating system: "
            + rawOsName
            + ". The native library is available for Linux, Windows and macOS only.");
  }

  private static String normalizeArchitecture(String rawOsArch) {
    if (rawOsArch.isBlank()) {
      throw new IllegalStateException("OS architecture system property is null or empty");
    }
    var osArch = ARCH_MAPPINGS.get(rawOsArch.toLowerCase());
    if (osArch == null) {
      throw new UnsupportedOperationException(
          "Unsupported architecture: "
              + rawOsArch
              + ". The native library is available for 64-bit architectures only.");
    }
    return osArch;
  }

  /** Clears the cached specification, for tests only. */
  static void clearCache() {
    synchronized (NativeLibrary.class) {
      cachedSpecification = null;
    }
  }

  /** Prints the native library specification of the running platform. */
  public static void main(String[] args) {
    try {
      System.out.println(getNativeLibSpecification());
    } catch (Exception e) {
      System.err.println("Error determining native library specification: " + e.getMessage());
      System.exit(1);
    }
  }
}
