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
    } else if (osName.equals("symbianos")) {
      osName = "epoc32";
    } else if (osName.equals("hp-ux")) {
      osName = "hpux";
    } else if (osName.equals("os/2")) {
      osName = "os2";
    } else if (osName.equals("procnto")) {
      osName = "qnx";
    } else {
      osName = osName.toLowerCase();
    }

    if (osArch.equals("x86-64")
        || osArch.equals("amd64")
        || osArch.equals("em64t")
        || osArch.equals("x86_64")) {
      osArch = "x86-64";
    } else if (osArch.equals("aarch64") || osArch.equals("arm64")) {
      osArch = "aarch64";
    } else if (osArch.equals("arm")) {
      osArch = "armv7a";
    } else if (osArch.equals("pentium")
        || osArch.equals("i386")
        || osArch.equals("i486")
        || osArch.equals("i586")
        || osArch.equals("i686")) {
      osArch = "x86";
    } else if (osArch.equals("power ppc")) {
      osArch = "powerpc";
    } else if (osArch.equals("psc1k")) {
      osArch = "ignite";
    }
    return osName + "-" + osArch;
  }

  public static void main(String[] args) {
    System.out.println(getNativeLibSpecification());
  }
}
