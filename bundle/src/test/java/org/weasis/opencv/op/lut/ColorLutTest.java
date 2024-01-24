/*
 * Copyright (c) 2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op.lut;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ColorLutTest {

  @Test
  void testGetByteLut() {
    assertNull(ColorLut.IMAGE.getByteLut().lutTable());

    assertNotNull(ColorLut.FLAG.getByteLut().lutTable());
    assertNotNull(ColorLut.MULTICOLOR.getByteLut().lutTable());
    assertNotNull(ColorLut.HUE.getByteLut().lutTable());
    assertNotNull(ColorLut.RED.getByteLut().lutTable());
    assertNotNull(ColorLut.GREEN.getByteLut().lutTable());
    assertNotNull(ColorLut.BLUE.getByteLut().lutTable());
    assertNotNull(ColorLut.GRAY.getByteLut().lutTable());
  }

  @Test
  void testToString() {
    assertEquals("Default (image)", ColorLut.IMAGE.getName());
    assertEquals("Default (image)", ColorLut.IMAGE.toString());
    assertEquals("Flag", ColorLut.FLAG.toString());
    assertEquals("Multi-Color", ColorLut.MULTICOLOR.toString());
    assertEquals("Hue", ColorLut.HUE.toString());
    assertEquals("Red", ColorLut.RED.toString());
    assertEquals("Green", ColorLut.GREEN.toString());
    assertEquals("Blue", ColorLut.BLUE.toString());
    assertEquals("Gray", ColorLut.GRAY.toString());
  }
}
