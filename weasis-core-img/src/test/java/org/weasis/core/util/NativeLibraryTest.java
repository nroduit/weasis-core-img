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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.opencv.osgi.OpenCVNativeLoader;

class NativeLibraryTest {

  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

  @BeforeAll
   static void loadNativeLib() {
    // Load the native OpenCV library
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @BeforeEach
   void setUp() {
    System.setOut(new PrintStream(outputStreamCaptor));
  }

  @AfterEach
   void tearDown() {
    System.setOut(System.out);
  }

  @Test
  @SetSystemProperty(key = "os.name", value = "Linux")
  @SetSystemProperty(key = "os.arch", value = "amd64")
  void getNativeLibSpecificationLinux() {
    assertEquals("linux-x86-64", NativeLibrary.getNativeLibSpecification());
  }

  @Test
  @SetSystemProperty(key = "os.name", value = "WindowsServer2019")
  @SetSystemProperty(key = "os.arch", value = "x86_64")
  void getNativeLibSpecificationWindows() {
    assertEquals("windows-x86-64", NativeLibrary.getNativeLibSpecification());
  }

  @Test
  @SetSystemProperty(key = "os.name", value = "Mac OS X")
  @SetSystemProperty(key = "os.arch", value = "AArch64")
  void getNativeLibSpecificationMac() {
    assertEquals("macosx-aarch64", NativeLibrary.getNativeLibSpecification());
  }

  @Test
  @SetSystemProperty(key = "os.name", value = "SymbianOS")
  @SetSystemProperty(key = "os.arch", value = "i686")
  void getNativeLibSpecificationSymbianOS() {
    assertEquals("epoc32-x86", NativeLibrary.getNativeLibSpecification());
  }

  @Test
  @SetSystemProperty(key = "os.name", value = "OS/2")
  @SetSystemProperty(key = "os.arch", value = "power ppc")
  void getNativeLibSpecificationPowerppc() {
    assertEquals("os2-powerpc", NativeLibrary.getNativeLibSpecification());
  }

  @Test
  @SetSystemProperty(key = "os.name", value = "Windows12")
  @SetSystemProperty(key = "os.arch", value = "ARM64")
  void mainMethod() {
    NativeLibrary.main(null);
    String capturedOutput = outputStreamCaptor.toString().trim();
    assertEquals("windows-aarch64", capturedOutput);
  }
}
