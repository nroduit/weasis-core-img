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

import static org.assertj.core.api.Assertions.assertThat;
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
  public static void loadNativeLib() {
    // Load the native OpenCV library
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @BeforeEach
  public void setUp() {
    System.setOut(new PrintStream(outputStreamCaptor));
  }

  @AfterEach
  public void tearDown() {
    System.setOut(System.out);
  }

  @Test
  @SetSystemProperty(key = "os.name", value = "Linux")
  @SetSystemProperty(key = "os.arch", value = "amd64")
  void getNativeLibSpecificationLinux() {
    assertThat(NativeLibrary.getNativeLibSpecification()).hasToString("linux-x86-64");
  }

  @Test
  @SetSystemProperty(key = "os.name", value = "WindowsServer2019")
  @SetSystemProperty(key = "os.arch", value = "x86_64")
  void getNativeLibSpecificationWindows() {
    assertThat(NativeLibrary.getNativeLibSpecification()).hasToString("windows-x86-64");
  }

  @Test
  @SetSystemProperty(key = "os.name", value = "Mac OS X")
  @SetSystemProperty(key = "os.arch", value = "AArch64")
  void getNativeLibSpecificationMac() {
    assertThat(NativeLibrary.getNativeLibSpecification()).hasToString("macosx-aarch64");
  }

  @Test
  @SetSystemProperty(key = "os.name", value = "Windows12")
  @SetSystemProperty(key = "os.arch", value = "ARM64")
  void mainMethod() {
    NativeLibrary.main(null);
    String capturedOutput = outputStreamCaptor.toString().trim();
    assertThat("windows-aarch64").hasToString(capturedOutput);
  }
}
