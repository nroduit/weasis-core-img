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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import org.junit.jupiter.api.Test;

class MathUtilTest {

  /** Method under test: {@link MathUtil#isEqualToZero(double)} */
  @Test
  void testIsEqualToZero() {
    assertFalse(MathUtil.isEqualToZero(MathUtil.DOUBLE_EPSILON));
    assertTrue(MathUtil.isEqualToZero(0.0));
    assertTrue(MathUtil.isEqualToZero(0.0000001));

    assertFalse(MathUtil.isEqualToZero(MathUtil.FLOAT_EPSILON));
    assertTrue(MathUtil.isEqualToZero(0.0f));
    assertTrue(MathUtil.isEqualToZero(0.000001f));
  }

  /** Method under test: {@link MathUtil#isDifferentFromZero(double)} */
  @Test
  void testIsDifferentFromZero() {
    assertTrue(MathUtil.isDifferentFromZero(10.0d));
    assertTrue(MathUtil.isDifferentFromZero(0.00001));
    assertFalse(MathUtil.isDifferentFromZero(0.0));
    assertFalse(MathUtil.isDifferentFromZero(0.000001));
    assertFalse(MathUtil.isDifferentFromZero(Double.NaN));

    assertTrue(MathUtil.isDifferentFromZero(10.0f));
    assertTrue(MathUtil.isDifferentFromZero(0.0001f));
    assertFalse(MathUtil.isDifferentFromZero(0.0f));
    assertFalse(MathUtil.isDifferentFromZero(0.00001f));
    assertFalse(MathUtil.isDifferentFromZero(Double.NaN));
  }

  /** Method under test: {@link MathUtil#isEqual(double, double)} */
  @Test
  void testIsEqual() {
    assertTrue(MathUtil.isEqual(10.0d, 10.0d));
    assertTrue(MathUtil.isEqual(10.0d, 10.000001d));
    assertFalse(MathUtil.isEqual(1.0d, 10.0d));
    assertFalse(MathUtil.isEqual(Double.NaN, 10.0d));
    assertTrue(MathUtil.isEqual(Double.NaN, Double.NaN));
    assertTrue(MathUtil.isEqual(10.0f, 10.0f));
    assertTrue(MathUtil.isEqual(10.0f, 10.00001f));
    assertFalse(MathUtil.isEqual(0.5f, 10.0f));
    assertFalse(MathUtil.isEqual(Float.NaN, 10.0f));
    assertTrue(MathUtil.isEqual(Float.NaN, Float.NaN));
  }

  /** Method under test: {@link MathUtil#isDifferent(double, double)} */
  @Test
  void testIsDifferent() {
    assertFalse(MathUtil.isDifferent(10.0d, 10.0d));
    assertTrue(MathUtil.isDifferent(1.0d, 10.00001d));
    assertTrue(MathUtil.isDifferent(1.0d, 10.0d));
    assertFalse(MathUtil.isDifferent(10.0f, 10.0f));
    assertTrue(MathUtil.isDifferent(10.0f, 10.0001f));
    assertTrue(MathUtil.isDifferent(0.5f, 10.0f));
  }

  /** Method under test: {@link MathUtil#getOrientation(double, double, double, double)} */
  @Test
  void testGetOrientation() {
    assertEquals(142.12501634890182d, MathUtil.getOrientation(1.0d, 3.0d, 10.0d, 10.0d));
    assertEquals(180.0d, MathUtil.getOrientation(10.0d, 10.0d, 5.0d, 10.0d));
    assertEquals(
        177.36540540774246d, MathUtil.getOrientation(-142.12501634890182d, 3.0d, 10.0d, 10.0d));
    assertEquals(90.0d, MathUtil.getOrientation(10.0d, 3.0d, 10.0d, 10.0d));

    Point p1 = new Point(10, 1);
    assertEquals(180.0d, MathUtil.getOrientation(p1, new Point(5, 1)).doubleValue());
    p1 = new Point(1, 0);
    assertEquals(90.0d, MathUtil.getOrientation(p1, new Point(1, 1)).doubleValue());

    assertNull(MathUtil.getOrientation(null, null));
    assertNull(MathUtil.getOrientation(new Point(1, 1), null));
  }

  /** Method under test: {@link MathUtil#getAzimuth(double, double, double, double)} */
  @Test
  void testGetAzimuth() {
    assertEquals(127.8749836510982d, MathUtil.getAzimuth(1.0d, 3.0d, 10.0d, 10.0d));
    assertEquals(269.0885531253135d, MathUtil.getAzimuth(450.0d, 3.0d, 10.0d, 10.0d));
    assertNull(MathUtil.getAzimuth(null, null));
    assertNull(MathUtil.getAzimuth(new Point(1, 1), null));

    Point p1 = new Point(0, 1);
    assertEquals(270.0d, MathUtil.getAzimuth(p1, new Point(-5, 1)).doubleValue());
  }

  /** Method under test: {@link MathUtil#round(double, int)} */
  @Test
  void testRound() {
    assertEquals(10.0d, MathUtil.round(10.0d, 1));
    assertEquals(10.0d, MathUtil.round(10.0d, 0));
    assertEquals(0.0d, MathUtil.round(MathUtil.DOUBLE_EPSILON, 1));
    assertEquals(0.5d, MathUtil.round(0.5d, 1));
    assertThrows(IllegalArgumentException.class, () -> MathUtil.round(10.0d, -1));
  }
}
