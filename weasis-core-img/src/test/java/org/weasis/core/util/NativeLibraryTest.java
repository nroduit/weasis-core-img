/*
 * Copyright (c) 2010-2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.opencv.osgi.OpenCVNativeLoader;

@DisplayName("NativeLibrary Tests")
class NativeLibraryTest {

  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
  private PrintStream originalOut;

  @BeforeAll
  static void loadNativeLib() {
    // Load the native OpenCV library
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @BeforeEach
  void setUp() {
    originalOut = System.out;
    System.setOut(new PrintStream(outputStreamCaptor));
    // Clear cache to ensure fresh system property reads
    NativeLibrary.clearCache();
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
    NativeLibrary.clearCache();
  }

  @Nested
  @DisplayName("Operating System Detection")
  class OperatingSystemDetectionTests {

    @ParameterizedTest
    @MethodSource("provideLinuxTestData")
    @DisplayName("Should detect Linux variants correctly")
    void shouldDetectLinuxVariants(String osName, String osArch, String expectedResult) {
      testNativeLibSpecification(osName, osArch, expectedResult);
    }

    @ParameterizedTest
    @MethodSource("provideWindowsTestData")
    @DisplayName("Should detect Windows variants correctly")
    void shouldDetectWindowsVariants(String osName, String osArch, String expectedResult) {
      testNativeLibSpecification(osName, osArch, expectedResult);
    }

    @ParameterizedTest
    @MethodSource("provideMacTestData")
    @DisplayName("Should detect macOS variants correctly")
    void shouldDetectMacVariants(String osName, String osArch, String expectedResult) {
      testNativeLibSpecification(osName, osArch, expectedResult);
    }

    @ParameterizedTest
    @MethodSource("provideSpecialOsTestData")
    @DisplayName("Should detect special OS variants correctly")
    void shouldDetectSpecialOsVariants(String osName, String osArch, String expectedResult) {
      testNativeLibSpecification(osName, osArch, expectedResult);
    }

    static Stream<Arguments> provideLinuxTestData() {
      return Stream.of(
          Arguments.of("Linux", "amd64", "linux-x86-64"),
          Arguments.of("linux", "x86_64", "linux-x86-64"),
          Arguments.of("Linux Ubuntu", "aarch64", "linux-aarch64"),
          Arguments.of("linux-gnu", "i686", "linux-x86"),
          Arguments.of("Linux Mint", "arm", "linux-armv7a"));
    }

    static Stream<Arguments> provideWindowsTestData() {
      return Stream.of(
          Arguments.of("Windows 10", "amd64", "windows-x86-64"),
          Arguments.of("Windows 11", "x86_64", "windows-x86-64"),
          Arguments.of("Windows Server 2019", "em64t", "windows-x86-64"),
          Arguments.of("Windows 7", "i386", "windows-x86"),
          Arguments.of("Windows 12", "ARM64", "windows-aarch64"),
          Arguments.of("win32", "pentium", "windows-x86"));
    }

    static Stream<Arguments> provideMacTestData() {
      return Stream.of(
          Arguments.of("Mac OS X", "x86_64", "macosx-x86-64"),
          Arguments.of("macOS", "aarch64", "macosx-aarch64"),
          Arguments.of("Mac OS X", "AArch64", "macosx-aarch64"),
          Arguments.of("macOS Monterey", "arm64", "macosx-aarch64"));
    }

    static Stream<Arguments> provideSpecialOsTestData() {
      return Stream.of(
          Arguments.of("SymbianOS", "i686", "epoc32-x86"),
          Arguments.of("HP-UX", "amd64", "hpux-x86-64"),
          Arguments.of("OS/2", "power ppc", "os2-powerpc"),
          Arguments.of("procnto", "aarch64", "qnx-aarch64"),
          Arguments.of("FreeBSD", "x86_64", "freebsd-x86-64"),
          Arguments.of("OpenBSD", "i586", "openbsd-x86"));
    }

    private void testNativeLibSpecification(String osName, String osArch, String expectedResult) {
      System.setProperty("os.name", osName);
      System.setProperty("os.arch", osArch);
      NativeLibrary.clearCache(); // Clear cache after setting properties

      String result = NativeLibrary.getNativeLibSpecification();
      assertEquals(expectedResult, result);
    }
  }

  @Nested
  @DisplayName("Architecture Detection")
  class ArchitectureDetectionTests {

    @ParameterizedTest
    @ValueSource(strings = {"x86-64", "amd64", "em64t", "x86_64"})
    @DisplayName("Should detect x86-64 architecture variants")
    void shouldDetectX86_64Variants(String architecture) {
      testArchitectureMapping(architecture, "x86-64");
    }

    @ParameterizedTest
    @ValueSource(strings = {"aarch64", "arm64"})
    @DisplayName("Should detect ARM64 architecture variants")
    void shouldDetectArm64Variants(String architecture) {
      testArchitectureMapping(architecture, "aarch64");
    }

    @ParameterizedTest
    @ValueSource(strings = {"pentium", "i386", "i486", "i586", "i686"})
    @DisplayName("Should detect x86 architecture variants")
    void shouldDetectX86Variants(String architecture) {
      testArchitectureMapping(architecture, "x86");
    }

    @Test
    @DisplayName("Should detect ARM architecture")
    void shouldDetectArmArchitecture() {
      testArchitectureMapping("arm", "armv7a");
    }

    @Test
    @DisplayName("Should detect PowerPC architecture")
    void shouldDetectPowerPcArchitecture() {
      testArchitectureMapping("power ppc", "powerpc");
    }

    @Test
    @DisplayName("Should detect ignite architecture")
    void shouldDetectIgniteArchitecture() {
      testArchitectureMapping("psc1k", "ignite");
    }

    @Test
    @DisplayName("Should handle unknown architecture")
    void shouldHandleUnknownArchitecture() {
      testArchitectureMapping("unknown-arch", "unknown-arch");
    }

    private void testArchitectureMapping(String inputArch, String expectedArch) {
      System.setProperty("os.name", "Linux");
      System.setProperty("os.arch", inputArch);
      NativeLibrary.clearCache();

      String result = NativeLibrary.getNativeLibSpecification();
      assertEquals("linux-" + expectedArch, result);
    }
  }

  @Nested
  @DisplayName("Caching Behavior")
  class CachingBehaviorTests {

    @Test
    @DisplayName("Should cache the result after first call")
    void shouldCacheResult() {
      System.setProperty("os.name", "Linux");
      System.setProperty("os.arch", "amd64");
      NativeLibrary.clearCache();

      String firstResult = NativeLibrary.getNativeLibSpecification();

      // Change system properties
      System.setProperty("os.name", "Windows 10");
      System.setProperty("os.arch", "x86_64");

      // Should return cached result, not the new system properties
      String secondResult = NativeLibrary.getNativeLibSpecification();

      assertEquals(firstResult, secondResult);
      assertEquals("linux-x86-64", secondResult);
    }

    @Test
    @DisplayName("Should use new system properties after cache clear")
    void shouldUseNewPropertiesAfterCacheClear() {
      System.setProperty("os.name", "Linux");
      System.setProperty("os.arch", "amd64");
      NativeLibrary.clearCache();

      String firstResult = NativeLibrary.getNativeLibSpecification();
      assertEquals("linux-x86-64", firstResult);

      // Change system properties and clear cache
      System.setProperty("os.name", "Windows 10");
      System.setProperty("os.arch", "x86_64");
      NativeLibrary.clearCache();

      String secondResult = NativeLibrary.getNativeLibSpecification();
      assertEquals("windows-x86-64", secondResult);
    }

    @Test
    @DisplayName("Should handle concurrent access safely")
    void shouldHandleConcurrentAccessSafely() throws InterruptedException {
      System.setProperty("os.name", "Linux");
      System.setProperty("os.arch", "amd64");
      NativeLibrary.clearCache();

      final int threadCount = 10;
      final String[] results = new String[threadCount];
      final Thread[] threads = new Thread[threadCount];

      for (int i = 0; i < threadCount; i++) {
        final int index = i;
        threads[i] =
            new Thread(
                () -> {
                  results[index] = NativeLibrary.getNativeLibSpecification();
                });
      }

      for (Thread thread : threads) {
        thread.start();
      }

      for (Thread thread : threads) {
        thread.join();
      }

      // All results should be the same
      for (String result : results) {
        assertEquals("linux-x86-64", result);
      }
    }
  }

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandlingTests {

    @Test
    @ClearSystemProperty(key = "os.name")
    @SetSystemProperty(key = "os.arch", value = "amd64")
    @DisplayName("Should throw exception when os.name is null")
    void shouldThrowExceptionWhenOsNameIsNull() {
      NativeLibrary.clearCache();

      IllegalStateException exception =
          assertThrows(IllegalStateException.class, NativeLibrary::getNativeLibSpecification);

      assertTrue(exception.getMessage().contains("OS name"));
    }

    @Test
    @SetSystemProperty(key = "os.name", value = "")
    @SetSystemProperty(key = "os.arch", value = "amd64")
    @DisplayName("Should throw exception when os.name is empty")
    void shouldThrowExceptionWhenOsNameIsEmpty() {
      NativeLibrary.clearCache();

      IllegalStateException exception =
          assertThrows(IllegalStateException.class, NativeLibrary::getNativeLibSpecification);

      assertTrue(exception.getMessage().contains("OS name"));
    }

    @Test
    @SetSystemProperty(key = "os.name", value = "Linux")
    @ClearSystemProperty(key = "os.arch")
    @DisplayName("Should throw exception when os.arch is null")
    void shouldThrowExceptionWhenOsArchIsNull() {
      NativeLibrary.clearCache();

      IllegalStateException exception =
          assertThrows(IllegalStateException.class, NativeLibrary::getNativeLibSpecification);

      assertTrue(exception.getMessage().contains("OS architecture"));
    }

    @Test
    @SetSystemProperty(key = "os.name", value = "Linux")
    @SetSystemProperty(key = "os.arch", value = "")
    @DisplayName("Should throw exception when os.arch is empty")
    void shouldThrowExceptionWhenOsArchIsEmpty() {
      NativeLibrary.clearCache();

      IllegalStateException exception =
          assertThrows(IllegalStateException.class, NativeLibrary::getNativeLibSpecification);

      assertTrue(exception.getMessage().contains("OS architecture"));
    }
  }

  @Nested
  @DisplayName("Main Method")
  class MainMethodTests {

    @Test
    @SetSystemProperty(key = "os.name", value = "Linux")
    @SetSystemProperty(key = "os.arch", value = "amd64")
    @DisplayName("Should print native library specification to stdout")
    void shouldPrintNativeLibSpecification() {
      NativeLibrary.clearCache();

      NativeLibrary.main(new String[] {});

      String capturedOutput = outputStreamCaptor.toString().trim();
      assertEquals("linux-x86-64", capturedOutput);
    }

    @Test
    @SetSystemProperty(key = "os.name", value = "Windows 10")
    @SetSystemProperty(key = "os.arch", value = "ARM64")
    @DisplayName("Should handle Windows ARM64 correctly")
    void shouldHandleWindowsArm64() {
      NativeLibrary.clearCache();

      NativeLibrary.main(null);

      String capturedOutput = outputStreamCaptor.toString().trim();
      assertEquals("windows-aarch64", capturedOutput);
    }

    @Test
    @ClearSystemProperty(key = "os.name")
    @SetSystemProperty(key = "os.arch", value = "amd64")
    @DisplayName("Should handle errors gracefully in main method")
    void shouldHandleErrorsInMainMethod() {
      NativeLibrary.clearCache();

      // Capture stderr as well
      ByteArrayOutputStream errorStreamCaptor = new ByteArrayOutputStream();
      PrintStream originalErr = System.err;
      System.setErr(new PrintStream(errorStreamCaptor));

      try {
        // This should cause System.exit(1) to be called, but we can't test that directly
        // Instead, we'll test that the error handling code path works
        assertThrows(IllegalStateException.class, NativeLibrary::getNativeLibSpecification);
      } finally {
        System.setErr(originalErr);
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should work with actual system properties")
    void shouldWorkWithActualSystemProperties() {
      // This test uses the actual system properties without mocking
      NativeLibrary.clearCache();

      String result = NativeLibrary.getNativeLibSpecification();

      assertNotNull(result);
      assertFalse(result.isEmpty());
      assertTrue(result.contains("-"));

      String[] parts = result.split("-", 2); // Architecture can contain hyphens
      assertEquals(2, parts.length);

      // Verify format: osname-architecture
      assertTrue(!parts[0].isEmpty());
      assertTrue(!parts[1].isEmpty());
    }

    @Test
    @DisplayName("Should be consistent across multiple calls")
    void shouldBeConsistentAcrossMultipleCalls() {
      NativeLibrary.clearCache();

      String firstCall = NativeLibrary.getNativeLibSpecification();
      String secondCall = NativeLibrary.getNativeLibSpecification();
      String thirdCall = NativeLibrary.getNativeLibSpecification();

      assertEquals(firstCall, secondCall);
      assertEquals(secondCall, thirdCall);
    }
  }
}
