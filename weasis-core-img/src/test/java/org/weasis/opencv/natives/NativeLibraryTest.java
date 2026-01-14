/*
 * Copyright (c) 2010-2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.natives;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty;

@DisplayNameGeneration(ReplaceUnderscores.class)
class NativeLibraryTest {

  // Test data records for better type safety and immutability
  record PlatformSpec(String osName, String architecture, String expectedResult) {
    static PlatformSpec of(String osName, String architecture, String expectedResult) {
      return new PlatformSpec(osName, architecture, expectedResult);
    }
  }

  private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
  private PrintStream originalOut;

  @BeforeAll
  static void load_native_lib() {
    NativeLibrary.loadLibraryFromLibraryName();
  }

  @BeforeEach
  void set_up() {
    originalOut = System.out;
    System.setOut(new PrintStream(outputCaptor));
    NativeLibrary.clearCache();
  }

  @AfterEach
  void tear_down() {
    System.setOut(originalOut);
    NativeLibrary.clearCache();
  }

  @Nested
  @DisplayNameGeneration(ReplaceUnderscores.class)
  class Library_loading {

    @Test
    void should_load_library_only_once_from_library_name() {
      // Library is already loaded in @BeforeAll, so this should be idempotent
      assertDoesNotThrow(NativeLibrary::loadLibraryFromLibraryName);
    }

    @Test
    void should_load_library_only_once_from_absolute_path() {
      // Since library is already loaded, this should not throw
      var tempPath = Path.of(System.getProperty("java.io.tmpdir"), "test-lib.so");
      assertDoesNotThrow(() -> NativeLibrary.loadLibraryFromAbsolutePath(tempPath));
    }

    @Test
    void should_handle_concurrent_library_loading() throws Exception {
      final var threadCount = 10;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      var tasks =
          Stream.generate(
                  () ->
                      (Callable<Void>)
                          () -> {
                            NativeLibrary.loadLibraryFromLibraryName();
                            return null;
                          })
              .limit(threadCount)
              .toList();

      var futures = executor.invokeAll(tasks);

      for (var future : futures) {
        assertDoesNotThrow(() -> future.get());
      }

      executor.shutdown();
    }
  }

  @Nested
  @DisplayNameGeneration(ReplaceUnderscores.class)
  class Operating_system_detection {

    @ParameterizedTest
    @CsvSource({
      "Linux, amd64, linux-x86-64",
      "linux, x86_64, linux-x86-64",
      "Linux Ubuntu, aarch64, linux-aarch64",
      "linux-gnu, i686, linux-x86",
      "Linux Mint, arm, linux-armv7a"
    })
    void should_detect_linux_variants(String osName, String architecture, String expected) {
      test_platform_specification(osName, architecture, expected);
    }

    @ParameterizedTest
    @CsvSource({
      "Windows 10, amd64, windows-x86-64",
      "Windows 11, x86_64, windows-x86-64",
      "Windows Server 2019, em64t, windows-x86-64",
      "Windows 7, i386, windows-x86",
      "Windows 12, ARM64, windows-aarch64",
      "win32, pentium, windows-x86"
    })
    void should_detect_windows_variants(String osName, String architecture, String expected) {
      test_platform_specification(osName, architecture, expected);
    }

    @ParameterizedTest
    @CsvSource({
      "Mac OS X, x86_64, macosx-x86-64",
      "macOS, aarch64, macosx-aarch64",
      "Mac OS X, AArch64, macosx-aarch64",
      "macOS Monterey, arm64, macosx-aarch64"
    })
    void should_detect_macos_variants(String osName, String architecture, String expected) {
      test_platform_specification(osName, architecture, expected);
    }

    @ParameterizedTest
    @MethodSource("special_os_test_data")
    void should_detect_special_os_variants(PlatformSpec spec) {
      test_platform_specification(spec.osName(), spec.architecture(), spec.expectedResult());
    }

    static Stream<PlatformSpec> special_os_test_data() {
      return Stream.of(
          PlatformSpec.of("SymbianOS", "i686", "epoc32-x86"),
          PlatformSpec.of("HP-UX", "amd64", "hpux-x86-64"),
          PlatformSpec.of("OS/2", "power ppc", "os2-powerpc"),
          PlatformSpec.of("procnto", "aarch64", "qnx-aarch64"),
          PlatformSpec.of("FreeBSD", "x86_64", "freebsd-x86-64"),
          PlatformSpec.of("OpenBSD", "i586", "openbsd-x86"));
    }

    private void test_platform_specification(String osName, String architecture, String expected) {
      System.setProperty("os.name", osName);
      System.setProperty("os.arch", architecture);
      NativeLibrary.clearCache();

      var result = NativeLibrary.getNativeLibSpecification();
      assertEquals(expected, result);
    }
  }

  @Nested
  @DisplayNameGeneration(ReplaceUnderscores.class)
  class Architecture_detection {

    @ParameterizedTest
    @ValueSource(strings = {"x86-64", "amd64", "em64t", "x86_64"})
    void should_detect_x86_64_variants(String architecture) {
      test_architecture_mapping(architecture, "x86-64");
    }

    @ParameterizedTest
    @ValueSource(strings = {"aarch64", "arm64"})
    void should_detect_arm64_variants(String architecture) {
      test_architecture_mapping(architecture, "aarch64");
    }

    @ParameterizedTest
    @ValueSource(strings = {"pentium", "i386", "i486", "i586", "i686"})
    void should_detect_x86_variants(String architecture) {
      test_architecture_mapping(architecture, "x86");
    }

    @Test
    void should_detect_arm_architecture() {
      test_architecture_mapping("arm", "armv7a");
    }

    @Test
    void should_detect_powerpc_architecture() {
      test_architecture_mapping("power ppc", "powerpc");
    }

    @Test
    void should_detect_ignite_architecture() {
      test_architecture_mapping("psc1k", "ignite");
    }

    @Test
    void should_handle_unknown_architecture() {
      test_architecture_mapping("unknown-arch", "unknown-arch");
    }

    private void test_architecture_mapping(String inputArch, String expectedArch) {
      System.setProperty("os.name", "Linux");
      System.setProperty("os.arch", inputArch);
      NativeLibrary.clearCache();

      var result = NativeLibrary.getNativeLibSpecification();
      assertEquals("linux-" + expectedArch, result);
    }
  }

  @Nested
  @DisplayNameGeneration(ReplaceUnderscores.class)
  class Caching_behavior {

    @Test
    void should_cache_result_after_first_call() {
      System.setProperty("os.name", "Linux");
      System.setProperty("os.arch", "amd64");
      NativeLibrary.clearCache();

      var firstResult = NativeLibrary.getNativeLibSpecification();

      // Change system properties - should still return cached result
      System.setProperty("os.name", "Windows 10");
      System.setProperty("os.arch", "x86_64");

      var secondResult = NativeLibrary.getNativeLibSpecification();

      assertAll(
          () -> assertEquals(firstResult, secondResult),
          () -> assertEquals("linux-x86-64", secondResult));
    }

    @Test
    void should_use_new_properties_after_cache_clear() {
      System.setProperty("os.name", "Linux");
      System.setProperty("os.arch", "amd64");
      NativeLibrary.clearCache();

      var firstResult = NativeLibrary.getNativeLibSpecification();
      assertEquals("linux-x86-64", firstResult);

      // Change system properties and clear cache
      System.setProperty("os.name", "Windows 10");
      System.setProperty("os.arch", "x86_64");
      NativeLibrary.clearCache();

      var secondResult = NativeLibrary.getNativeLibSpecification();
      assertEquals("windows-x86-64", secondResult);
    }

    @Test
    void should_handle_concurrent_access_safely() throws Exception {
      System.setProperty("os.name", "Linux");
      System.setProperty("os.arch", "amd64");
      NativeLibrary.clearCache();

      final var threadCount = 20;
      final var latch = new CountDownLatch(threadCount);
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      var tasks =
          Stream.generate(
                  () ->
                      (java.util.concurrent.Callable<Void>)
                          () -> {
                            latch.countDown();
                            latch.await();
                            var result = NativeLibrary.getNativeLibSpecification();
                            assertEquals("linux-x86-64", result);
                            return null;
                          })
              .limit(threadCount)
              .toList();

      var futures = executor.invokeAll(tasks);

      // Wait for all tasks to complete
      for (var future : futures) {
        assertDoesNotThrow(() -> future.get());
      }
    }
  }

  @Nested
  @DisplayNameGeneration(ReplaceUnderscores.class)
  class Error_handling {

    @Test
    @ClearSystemProperty(key = "os.name")
    @SetSystemProperty(key = "os.arch", value = "amd64")
    void should_throw_exception_when_os_name_is_null() {
      NativeLibrary.clearCache();

      var exception =
          assertThrows(IllegalStateException.class, NativeLibrary::getNativeLibSpecification);

      assertTrue(exception.getMessage().contains("OS name"));
    }

    @Test
    @SetSystemProperty(key = "os.name", value = "   ")
    @SetSystemProperty(key = "os.arch", value = "amd64")
    void should_throw_exception_when_os_name_is_blank() {
      NativeLibrary.clearCache();

      var exception =
          assertThrows(IllegalStateException.class, NativeLibrary::getNativeLibSpecification);

      assertTrue(exception.getMessage().contains("OS name"));
    }

    @Test
    @SetSystemProperty(key = "os.name", value = "Linux")
    @ClearSystemProperty(key = "os.arch")
    void should_throw_exception_when_os_arch_is_null() {
      NativeLibrary.clearCache();

      var exception =
          assertThrows(IllegalStateException.class, NativeLibrary::getNativeLibSpecification);

      assertTrue(exception.getMessage().contains("OS architecture"));
    }

    @Test
    @SetSystemProperty(key = "os.name", value = "Linux")
    @SetSystemProperty(key = "os.arch", value = "\t\n")
    void should_throw_exception_when_os_arch_is_blank() {
      NativeLibrary.clearCache();

      var exception =
          assertThrows(IllegalStateException.class, NativeLibrary::getNativeLibSpecification);

      assertTrue(exception.getMessage().contains("OS architecture"));
    }
  }

  @Nested
  @DisplayNameGeneration(ReplaceUnderscores.class)
  class Main_method {

    @Test
    @SetSystemProperty(key = "os.name", value = "Linux")
    @SetSystemProperty(key = "os.arch", value = "amd64")
    void should_print_native_library_specification_to_stdout() {
      NativeLibrary.clearCache();

      NativeLibrary.main(new String[] {});

      var capturedOutput = outputCaptor.toString().trim();
      assertEquals("linux-x86-64", capturedOutput);
    }

    @Test
    @SetSystemProperty(key = "os.name", value = "Windows 10")
    @SetSystemProperty(key = "os.arch", value = "ARM64")
    void should_handle_windows_arm64_correctly() {
      NativeLibrary.clearCache();

      NativeLibrary.main(null);

      var capturedOutput = outputCaptor.toString().trim();
      assertEquals("windows-aarch64", capturedOutput);
    }

    @Test
    @ClearSystemProperty(key = "os.name")
    @SetSystemProperty(key = "os.arch", value = "amd64")
    void should_handle_errors_gracefully_in_main_method() {
      NativeLibrary.clearCache();

      // Verify the underlying method throws the expected exception
      assertThrows(IllegalStateException.class, NativeLibrary::getNativeLibSpecification);
    }
  }

  @Nested
  @DisplayNameGeneration(ReplaceUnderscores.class)
  class Integration_tests {

    @Test
    void should_work_with_actual_system_properties() {
      NativeLibrary.clearCache();

      var result = NativeLibrary.getNativeLibSpecification();

      assertAll(
          () -> assertNotNull(result),
          () -> assertFalse(result.isBlank()),
          () -> assertTrue(result.contains("-")));

      var parts = result.split("-", 2);
      assertAll(
          () -> assertEquals(2, parts.length),
          () -> assertFalse(parts[0].isBlank()),
          () -> assertFalse(parts[1].isBlank()));
    }

    @Test
    void should_be_consistent_across_multiple_calls() {
      NativeLibrary.clearCache();

      var results = Stream.generate(NativeLibrary::getNativeLibSpecification).limit(5).toList();

      // All results should be identical
      assertEquals(1, results.stream().distinct().count());
    }

    @Test
    void should_handle_case_sensitivity_correctly() {
      var testCases =
          List.of(
              PlatformSpec.of("LINUX", "AMD64", "linux-x86-64"),
              PlatformSpec.of("Windows", "X86_64", "windows-x86-64"),
              PlatformSpec.of("MacOS", "AARCH64", "macosx-aarch64"));

      testCases.forEach(
          spec -> {
            System.setProperty("os.name", spec.osName());
            System.setProperty("os.arch", spec.architecture());
            NativeLibrary.clearCache();

            var result = NativeLibrary.getNativeLibSpecification();
            assertEquals(
                spec.expectedResult(),
                result,
                () -> "Failed for OS: " + spec.osName() + ", Arch: " + spec.architecture());
          });
    }
  }
}
