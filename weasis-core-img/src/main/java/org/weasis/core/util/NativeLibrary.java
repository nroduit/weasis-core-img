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

public class NativeLibrary {

  private NativeLibrary() {}

  public static String getNativeLibSpecification() {
    // See naming conventions at https://docs.osgi.org/reference/osnames.html
    String osName = System.getProperty("os.name").toLowerCase();
    String osArch = System.getProperty("os.arch").toLowerCase();

    if (osName.startsWith("win")) {
      // All Windows versions with a specific processor architecture (x86 or x86-64) are grouped
      // under windows. If you need to make different native libraries for the Windows versions,
      // define it in the Bundle-NativeCode tag of the bundle fragment.
      osName = "windows";
    } else if (osName.startsWith("mac")) {
      osName = "macosx";
    } else if (osName.startsWith("linux")) {
      osName = "linux";
    } else {
      switch (osName) {
        case "symbianos" -> osName = "epoc32";
        case "hp-ux" -> osName = "hpux";
        case "os/2" -> osName = "os2";
        case "procnto" -> osName = "qnx";
        default -> osName = osName.toLowerCase();
      }
    }

    osArch = switch (osArch) {
      case "x86-64", "amd64", "em64t", "x86_64" -> "x86-64";
      case "aarch64", "arm64" -> "aarch64";
      case "arm" -> "armv7a";
      case "pentium", "i386", "i486", "i586", "i686" -> "x86";
      case "power ppc" -> "powerpc";
      case "psc1k" -> "ignite";
      default -> osArch;
    };
    return osName + "-" + osArch;
  }

  public static void main(String[] args) {
    System.out.println(getNativeLibSpecification());
  }
}
