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

import java.awt.geom.Point2D;
import java.math.RoundingMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MathUtil Tests")
class MathUtilTest {

  // ================= Constants Tests =================

  @Test
  @DisplayName("Test mathematical constants")
  void testConstants() {
    assertEquals(1e-9, MathUtil.DOUBLE_EPSILON);
    assertEquals(1e-5f, MathUtil.FLOAT_EPSILON);
    assertEquals(Math.PI / 180.0, MathUtil.DEGREES_TO_RADIANS, 1e-15);
    assertEquals(180.0 / Math.PI, MathUtil.RADIANS_TO_DEGREES, 1e-13);
  }

  // ================= Zero Comparison Tests =================

  @Nested
  @DisplayName("Zero Comparison Tests")
  class ZeroComparisonTests {

    @Test
    @DisplayName("Test isEqualToZero with double values")
    void testIsEqualToZero_Double() {
      assertTrue(MathUtil.isEqualToZero(0.0));
      assertTrue(MathUtil.isEqualToZero(1e-10)); // Within DOUBLE_EPSILON (1e-9)
      assertTrue(MathUtil.isEqualToZero(-1e-10));

      assertFalse(MathUtil.isEqualToZero(Double.NaN));
      assertFalse(MathUtil.isEqualToZero(1e-8)); // Outside DOUBLE_EPSILON
      assertFalse(MathUtil.isEqualToZero(-1e-8));
      assertFalse(MathUtil.isEqualToZero(1.0));
      assertFalse(MathUtil.isEqualToZero(Double.POSITIVE_INFINITY));
      assertFalse(MathUtil.isEqualToZero(Double.NEGATIVE_INFINITY));
    }

    @Test
    @DisplayName("Test isEqualToZero with custom epsilon")
    void testIsEqualToZero_Double_CustomEpsilon() {
      assertTrue(MathUtil.isEqualToZero(0.001, 0.01));
      assertTrue(MathUtil.isEqualToZero(-0.005, 0.01));
      assertFalse(MathUtil.isEqualToZero(0.02, 0.01));
      assertFalse(MathUtil.isEqualToZero(Double.NaN, 0.01));
    }

    @Test
    @DisplayName("Test isEqualToZero with float values")
    void testIsEqualToZero_Float() {
      assertTrue(MathUtil.isEqualToZero(0.0f));
      assertTrue(MathUtil.isEqualToZero(1e-6f)); // Within FLOAT_EPSILON (1e-5)
      assertTrue(MathUtil.isEqualToZero(-1e-6f));

      assertFalse(MathUtil.isEqualToZero(Float.NaN));
      assertFalse(MathUtil.isEqualToZero(1e-4f)); // Outside FLOAT_EPSILON
      assertFalse(MathUtil.isEqualToZero(-1e-4f));
      assertFalse(MathUtil.isEqualToZero(1.0f));
      assertFalse(MathUtil.isEqualToZero(Float.POSITIVE_INFINITY));
      assertFalse(MathUtil.isEqualToZero(Float.NEGATIVE_INFINITY));
    }

    @Test
    @DisplayName("Test isEqualToZero with float custom epsilon")
    void testIsEqualToZero_Float_CustomEpsilon() {
      assertTrue(MathUtil.isEqualToZero(0.001f, 0.01f));
      assertTrue(MathUtil.isEqualToZero(-0.005f, 0.01f));
      assertFalse(MathUtil.isEqualToZero(0.02f, 0.01f));
      assertFalse(MathUtil.isEqualToZero(Float.NaN, 0.01f));
    }

    @Test
    @DisplayName("Test isDifferentFromZero with double values")
    void testIsDifferentFromZero_Double() {
      assertFalse(MathUtil.isDifferentFromZero(0.0));
      assertFalse(MathUtil.isDifferentFromZero(1e-10)); // Within DOUBLE_EPSILON
      assertFalse(MathUtil.isDifferentFromZero(-1e-10));
      assertFalse(MathUtil.isDifferentFromZero(Double.NaN)); // NaN should be false

      assertTrue(MathUtil.isDifferentFromZero(1e-8)); // Outside DOUBLE_EPSILON
      assertTrue(MathUtil.isDifferentFromZero(-1e-8));
      assertTrue(MathUtil.isDifferentFromZero(1.0));
      assertTrue(MathUtil.isDifferentFromZero(Double.POSITIVE_INFINITY));
      assertTrue(MathUtil.isDifferentFromZero(Double.NEGATIVE_INFINITY));
    }

    @Test
    @DisplayName("Test isDifferentFromZero with custom epsilon")
    void testIsDifferentFromZero_Double_CustomEpsilon() {
      assertFalse(MathUtil.isDifferentFromZero(0.001, 0.01));
      assertTrue(MathUtil.isDifferentFromZero(0.02, 0.01));
      assertFalse(MathUtil.isDifferentFromZero(Double.NaN, 0.01)); // NaN handling
    }

    @Test
    @DisplayName("Test isDifferentFromZero with float values")
    void testIsDifferentFromZero_Float() {
      assertFalse(MathUtil.isDifferentFromZero(0.0f));
      assertFalse(MathUtil.isDifferentFromZero(1e-6f)); // Within FLOAT_EPSILON
      assertFalse(MathUtil.isDifferentFromZero(-1e-6f));
      assertFalse(MathUtil.isDifferentFromZero(Float.NaN)); // NaN should be false

      assertTrue(MathUtil.isDifferentFromZero(1e-4f)); // Outside FLOAT_EPSILON
      assertTrue(MathUtil.isDifferentFromZero(-1e-4f));
      assertTrue(MathUtil.isDifferentFromZero(1.0f));
      assertTrue(MathUtil.isDifferentFromZero(Float.POSITIVE_INFINITY));
      assertTrue(MathUtil.isDifferentFromZero(Float.NEGATIVE_INFINITY));
    }

    @Test
    @DisplayName("Test isDifferentFromZero with float custom epsilon")
    void testIsDifferentFromZero_Float_CustomEpsilon() {
      assertFalse(MathUtil.isDifferentFromZero(0.001f, 0.01f));
      assertTrue(MathUtil.isDifferentFromZero(0.02f, 0.01f));
      assertFalse(MathUtil.isDifferentFromZero(Float.NaN, 0.01f)); // NaN handling
    }
  }

  // ================= Equality Comparison Tests =================

  @Nested
  @DisplayName("Equality Comparison Tests")
  class EqualityComparisonTests {

    @Test
    @DisplayName("Test isEqual with double values using default epsilon")
    void testIsEqual_Double_DefaultEpsilon() {
      // Exact equality
      assertTrue(MathUtil.isEqual(10.0, 10.0));
      assertTrue(MathUtil.isEqual(0.0, 0.0));
      assertTrue(MathUtil.isEqual(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
      assertTrue(MathUtil.isEqual(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
      assertTrue(MathUtil.isEqual(Double.NaN, Double.NaN));

      // Within relative tolerance (DOUBLE_EPSILON = 1e-9)
      // For numbers around 10.0, relative epsilon = 1e-9 * 10 = 1e-8
      assertTrue(MathUtil.isEqual(10.0, 10.0 + 5e-9)); // Well within tolerance

      // Outside relative tolerance
      assertFalse(MathUtil.isEqual(10.0, 10.0 + 2e-8)); // Outside tolerance
      assertFalse(MathUtil.isEqual(10.0, 11.0));
      assertFalse(MathUtil.isEqual(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
      assertFalse(MathUtil.isEqual(Double.NaN, 1.0));
      assertFalse(MathUtil.isEqual(1.0, Double.NaN));
    }

    @Test
    @DisplayName("Test isEqual with double custom epsilon")
    void testIsEqual_Double_CustomEpsilon() {
      assertTrue(MathUtil.isEqual(10.0, 10.01, 0.02));
      assertTrue(MathUtil.isEqual(10.0, 10.05, 0.02));
      assertTrue(MathUtil.isEqual(Double.NaN, Double.NaN, 0.01));
      assertFalse(MathUtil.isEqual(Double.NaN, 1.0, 0.01));
    }

    @Test
    @DisplayName("Test isEqual with float values using default epsilon")
    void testIsEqual_Float_DefaultEpsilon() {
      // Exact equality
      assertTrue(MathUtil.isEqual(10.0f, 10.0f));
      assertTrue(MathUtil.isEqual(0.0f, 0.0f));
      assertTrue(MathUtil.isEqual(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
      assertTrue(MathUtil.isEqual(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY));
      assertTrue(MathUtil.isEqual(Float.NaN, Float.NaN));

      // Within relative tolerance (FLOAT_EPSILON = 1e-5)
      // For numbers around 10.0f, relative epsilon = 1e-5 * 10 = 1e-4
      assertTrue(MathUtil.isEqual(10.0f, 10.0f + 5e-5f)); // Within tolerance

      // Outside relative tolerance
      assertFalse(MathUtil.isEqual(10.0f, 10.0f + 2e-4f)); // Outside tolerance
      assertFalse(MathUtil.isEqual(10.0f, 11.0f));
      assertFalse(MathUtil.isEqual(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY));
      assertFalse(MathUtil.isEqual(Float.NaN, 1.0f));
      assertFalse(MathUtil.isEqual(1.0f, Float.NaN));
    }

    @Test
    @DisplayName("Test isEqual with float custom epsilon")
    void testIsEqual_Float_CustomEpsilon() {
      assertTrue(MathUtil.isEqual(10.0f, 10.01f, 0.02f));
      assertTrue(MathUtil.isEqual(10.0f, 10.05f, 0.02f));
      assertTrue(MathUtil.isEqual(Float.NaN, Float.NaN, 0.01f));
      assertFalse(MathUtil.isEqual(Float.NaN, 1.0f, 0.01f));
    }

    @Test
    @DisplayName("Test isEqual with small values")
    void testIsEqual_SmallValues() {
      // Test near-zero values with absolute comparison
      // Values smaller than MIN_EPSILON use absolute comparison
      assertTrue(MathUtil.isEqual(1e-15, 2e-15, 1e-9)); // Both very small, use absolute
      assertTrue(MathUtil.isEqual(0.0, 1e-10)); // Within DOUBLE_EPSILON
      assertTrue(MathUtil.isEqual(0.0f, 1e-6f)); // Within FLOAT_EPSILON

      assertFalse(MathUtil.isEqual(0.0, 1e-8)); // Outside DOUBLE_EPSILON
      assertFalse(MathUtil.isEqual(0.0f, 1e-4f)); // Outside FLOAT_EPSILON
    }

    @Test
    @DisplayName("Test isDifferent with double values")
    void testIsDifferent_Double() {
      // Should be opposite of isEqual
      assertFalse(MathUtil.isDifferent(10.0, 10.0 + 5e-9)); // Within tolerance
      assertTrue(MathUtil.isDifferent(10.0, 10.0 + 2e-8)); // Outside tolerance
      assertTrue(MathUtil.isDifferent(10.0, 11.0));
      assertFalse(MathUtil.isDifferent(Double.NaN, Double.NaN));
      assertTrue(MathUtil.isDifferent(Double.NaN, 1.0));
    }

    @Test
    @DisplayName("Test isDifferent with double custom epsilon")
    void testIsDifferent_Double_CustomEpsilon() {
      assertFalse(MathUtil.isDifferent(10.0, 10.01, 0.02));
      assertFalse(MathUtil.isDifferent(10.0, 10.05, 0.02));
      assertFalse(MathUtil.isDifferent(Double.NaN, Double.NaN, 0.01));
      assertTrue(MathUtil.isDifferent(Double.NaN, 1.0, 0.01));
    }

    @Test
    @DisplayName("Test isDifferent with float values")
    void testIsDifferent_Float() {
      // Should be opposite of isEqual
      assertFalse(MathUtil.isDifferent(10.0f, 10.0f + 5e-5f)); // Within tolerance
      assertTrue(MathUtil.isDifferent(10.0f, 10.0f + 2e-4f)); // Outside tolerance
      assertTrue(MathUtil.isDifferent(10.0f, 11.0f));
      assertFalse(MathUtil.isDifferent(Float.NaN, Float.NaN));
      assertTrue(MathUtil.isDifferent(Float.NaN, 1.0f));
    }

    @Test
    @DisplayName("Test isDifferent with float custom epsilon")
    void testIsDifferent_Float_CustomEpsilon() {
      assertFalse(MathUtil.isDifferent(10.0f, 10.01f, 0.02f));
      assertFalse(MathUtil.isDifferent(10.0f, 10.05f, 0.02f));
      assertFalse(MathUtil.isDifferent(Float.NaN, Float.NaN, 0.01f));
    }
  }

  // ================= Geometric Calculations Tests =================

  @Nested
  @DisplayName("Geometric Calculations Tests")
  class GeometricCalculationsTests {

    @Test
    @DisplayName("Test getOrientation with coordinates")
    void testGetOrientation_Coordinates() {
      // Image coordinates (Y increases downward)
      // Test basic orientations
      assertEquals(0.0, MathUtil.getOrientation(0.0, 0.0, 1.0, 0.0), 1e-10); // Horizontal right
      assertEquals(135.0, MathUtil.getOrientation(0.0, 0.0, 1.0, 1.0), 1e-10); // 135° down-right
      assertEquals(45.0, MathUtil.getOrientation(0.0, 0.0, -1.0, 1.0), 1e-10); // 45° down-left
      assertEquals(90.0, MathUtil.getOrientation(0.0, 0.0, 0.0, 1.0), 1e-10); // Vertical down
      assertEquals(180.0, MathUtil.getOrientation(0.0, 0.0, -1.0, 0.0), 1e-10); // Horizontal left

      // Test diagonal cases in image coordinates
      assertEquals(45.0, MathUtil.getOrientation(0.0, 0.0, 1.0, -1.0), 1e-10); // Up-right in image
      assertEquals(135.0, MathUtil.getOrientation(0.0, 0.0, -1.0, -1.0), 1e-10); // Up-left in image

      // Test specific image coordinate scenarios
      assertEquals(90.0, MathUtil.getOrientation(10.0, 10.0, 10.0, 20.0), 1e-10); // Vertical down
      assertEquals(0.0, MathUtil.getOrientation(10.0, 10.0, 20.0, 10.0), 1e-10); // Horizontal right
    }

    @Test
    @DisplayName("Test getOrientation with Point2D objects")
    void testGetOrientation_Points() {
      // Image coordinates using Point2D
      Point2D origin = new Point2D.Double(0.0, 0.0);
      Point2D right = new Point2D.Double(1.0, 0.0);
      Point2D downRight = new Point2D.Double(1.0, 1.0);

      assertEquals(0.0, MathUtil.getOrientation(origin, right), 1e-10);
      assertEquals(135.0, MathUtil.getOrientation(origin, downRight), 1e-10);

      // Test with null inputs
      assertNull(MathUtil.getOrientation(null, right));
      assertNull(MathUtil.getOrientation(origin, null));
      assertNull(MathUtil.getOrientation(null, null));
    }

    @Test
    @DisplayName("Test getOrientation with null inputs")
    void testGetOrientation_NullInputs() {
      assertNull(MathUtil.getOrientation(null, null));
      assertNull(MathUtil.getOrientation(new Point2D.Double(1, 1), null));
      assertNull(MathUtil.getOrientation(null, new Point2D.Double(1, 1)));
    }

    @Test
    @DisplayName("Test getOrientation with invalid inputs")
    void testGetOrientation_InvalidInputs() {
      // Test with infinite coordinates
      assertThrows(
          IllegalArgumentException.class,
          () -> MathUtil.getOrientation(Double.POSITIVE_INFINITY, 0, 1, 1));
      assertThrows(
          IllegalArgumentException.class, () -> MathUtil.getOrientation(0, Double.NaN, 1, 1));
    }

    @Test
    @DisplayName("Test getAzimuth with coordinates")
    void testGetAzimuth_Coordinates() {
      // Image coordinates (Y increases downward) - North = negative Y
      assertEquals(0.0, MathUtil.getAzimuth(0.0, 0.0, 0.0, -1.0), 1e-10); // North (up in image)
      assertEquals(90.0, MathUtil.getAzimuth(0.0, 0.0, 1.0, 0.0), 1e-10); // East
      assertEquals(180.0, MathUtil.getAzimuth(0.0, 0.0, 0.0, 1.0), 1e-10); // South (down in image)
      assertEquals(270.0, MathUtil.getAzimuth(0.0, 0.0, -1.0, 0.0), 1e-10); // West

      // Test diagonal directions in image coordinates
      assertEquals(45.0, MathUtil.getAzimuth(0.0, 0.0, 1.0, -1.0), 1e-10); // NE
      assertEquals(315.0, MathUtil.getAzimuth(0.0, 0.0, -1.0, -1.0), 1e-10); // NW
      assertEquals(225.0, MathUtil.getAzimuth(0.0, 0.0, -1.0, 1.0), 1e-10); // SW
      assertEquals(135.0, MathUtil.getAzimuth(0.0, 0.0, 1.0, 1.0), 1e-10); // SE

      // Test with different starting points in image coordinates
      assertEquals(
          0.0, MathUtil.getAzimuth(100.0, 100.0, 100.0, 50.0), 1e-10); // North (up in image)
      assertEquals(90.0, MathUtil.getAzimuth(100.0, 100.0, 150.0, 100.0), 1e-10); // East
    }

    @Test
    @DisplayName("Test getAzimuth with Point2D objects")
    void testGetAzimuth_Points() {
      // Image coordinates using Point2D
      Point2D origin = new Point2D.Double(0.0, 0.0);
      Point2D south = new Point2D.Double(0.0, 1.0); // South in image coordinates
      Point2D east = new Point2D.Double(1.0, 0.0);

      assertEquals(180.0, MathUtil.getAzimuth(origin, south), 1e-10); // South (down in image)
      assertEquals(90.0, MathUtil.getAzimuth(origin, east), 1e-10); // East

      // Test with null inputs
      assertNull(MathUtil.getAzimuth(null, south));
      assertNull(MathUtil.getAzimuth(origin, null));
      assertNull(MathUtil.getAzimuth(null, null));
    }

    @Test
    @DisplayName("Test getAzimuth with null inputs")
    void testGetAzimuth_NullInputs() {
      assertNull(MathUtil.getAzimuth(null, null));
      assertNull(MathUtil.getAzimuth(new Point2D.Double(1, 1), null));
      assertNull(MathUtil.getAzimuth(null, new Point2D.Double(1, 1)));
    }

    @Test
    @DisplayName("Test getAzimuth with invalid inputs")
    void testGetAzimuth_InvalidInputs() {
      // Test with infinite coordinates
      assertThrows(
          IllegalArgumentException.class,
          () -> MathUtil.getAzimuth(Double.POSITIVE_INFINITY, 0, 1, 1));
      assertThrows(IllegalArgumentException.class, () -> MathUtil.getAzimuth(0, Double.NaN, 1, 1));
    }

    @Test
    @DisplayName("Test getDistance with coordinates")
    void testGetDistance_Coordinates() {
      // Test Pythagorean theorem
      assertEquals(5.0, MathUtil.getDistance(0.0, 0.0, 3.0, 4.0), 1e-10);
      assertEquals(0.0, MathUtil.getDistance(1.0, 1.0, 1.0, 1.0), 1e-10);
      assertEquals(Math.sqrt(2), MathUtil.getDistance(0.0, 0.0, 1.0, 1.0), 1e-10);

      // Test with negative coordinates
      assertEquals(5.0, MathUtil.getDistance(-3.0, -4.0, 0.0, 0.0), 1e-10);
      // Test with large values
      assertEquals(5.0, MathUtil.getDistance(1000.0, 1000.0, 1003.0, 1004.0), 1e-10);
    }

    @Test
    @DisplayName("Test getDistance with Point2D objects")
    void testGetDistance_Points() {
      Point2D p1 = new Point2D.Double(0, 0);
      Point2D p2 = new Point2D.Double(3, 4);
      assertEquals(5.0, MathUtil.getDistance(p1, p2), 1e-10);
      // Test same point
      assertEquals(0.0, MathUtil.getDistance(p1, p1), 1e-10);
    }

    @Test
    @DisplayName("Test getDistance with null inputs")
    void testGetDistance_NullInputs() {
      assertNull(MathUtil.getDistance(null, null));
      assertNull(MathUtil.getDistance(new Point2D.Double(1, 1), null));
      assertNull(MathUtil.getDistance(null, new Point2D.Double(1, 1)));
    }

    @Test
    @DisplayName("Test getDistance with invalid inputs")
    void testGetDistance_InvalidInputs() {
      // Test with infinite coordinates
      assertThrows(
          IllegalArgumentException.class,
          () -> MathUtil.getDistance(Double.POSITIVE_INFINITY, 0, 1, 1));
      assertThrows(IllegalArgumentException.class, () -> MathUtil.getDistance(0, Double.NaN, 1, 1));
    }

    @Test
    @DisplayName("Test normalizeAngle")
    void testNormalizeAngle() {
      assertEquals(0.0, MathUtil.normalizeAngle(0.0), 1e-10);
      assertEquals(0.0, MathUtil.normalizeAngle(360.0), 1e-10);
      assertEquals(0.0, MathUtil.normalizeAngle(-360.0), 1e-10);
      assertEquals(90.0, MathUtil.normalizeAngle(90.0), 1e-10);
      assertEquals(270.0, MathUtil.normalizeAngle(-90.0), 1e-10);
      assertEquals(45.0, MathUtil.normalizeAngle(405.0), 1e-10);
      assertEquals(315.0, MathUtil.normalizeAngle(-45.0), 1e-10);
      assertEquals(180.0, MathUtil.normalizeAngle(180.0), 1e-10);
      assertEquals(180.0, MathUtil.normalizeAngle(-180.0), 1e-10);

      // Test multiple rotations
      assertEquals(90.0, MathUtil.normalizeAngle(720.0 + 90.0), 1e-10);
      assertEquals(270.0, MathUtil.normalizeAngle(-720.0 - 90.0), 1e-10);
    }

    @Test
    @DisplayName("Test normalizeAngle with invalid inputs")
    void testNormalizeAngle_InvalidInputs() {
      assertThrows(
          IllegalArgumentException.class, () -> MathUtil.normalizeAngle(Double.POSITIVE_INFINITY));
      assertThrows(IllegalArgumentException.class, () -> MathUtil.normalizeAngle(Double.NaN));
      assertThrows(
          IllegalArgumentException.class, () -> MathUtil.normalizeAngle(Double.NEGATIVE_INFINITY));
    }
  }

  // ================= Rounding Tests =================

  @Nested
  @DisplayName("Rounding and Precision Tests")
  class RoundingTests {

    @Test
    @DisplayName("Test round with default HALF_UP mode")
    void testRound_DefaultMode() {
      assertEquals(3.14, MathUtil.round(3.14159, 2), 1e-10);
      assertEquals(3.15, MathUtil.round(3.14559, 2), 1e-10);
      assertEquals(0.0, MathUtil.round(0.0, 2), 1e-10);
      assertEquals(-3.14, MathUtil.round(-3.14159, 2), 1e-10);

      // Test edge cases
      assertEquals(3.0, MathUtil.round(2.5, 0), 1e-10); // HALF_UP rounds up
      assertEquals(-3.0, MathUtil.round(-2.5, 0), 1e-10); // HALF_UP rounds away from zero

      // Test zero places
      assertEquals(123.0, MathUtil.round(123.456, 0), 1e-10);
      assertEquals(124.0, MathUtil.round(123.56, 0), 1e-10);

      // Test with larger decimals
      assertEquals(3.14159, MathUtil.round(3.141592653589793, 5), 1e-10);
    }

    @Test
    @DisplayName("Test round with custom rounding mode")
    void testRound_CustomMode() {
      assertEquals(3.15, MathUtil.round(3.14559, 2, RoundingMode.HALF_DOWN), 1e-10);
      assertEquals(3.15, MathUtil.round(3.14559, 2, RoundingMode.HALF_UP), 1e-10);
      assertEquals(3.14, MathUtil.round(3.14999, 2, RoundingMode.DOWN), 1e-10);
      assertEquals(3.15, MathUtil.round(3.14001, 2, RoundingMode.UP), 1e-10);
      // Test CEILING and FLOOR
      assertEquals(3.15, MathUtil.round(3.14001, 2, RoundingMode.CEILING), 1e-10);
      assertEquals(3.14, MathUtil.round(3.14999, 2, RoundingMode.FLOOR), 1e-10);
      assertEquals(-3.14, MathUtil.round(-3.14999, 2, RoundingMode.CEILING), 1e-10);
      assertEquals(-3.15, MathUtil.round(-3.14001, 2, RoundingMode.FLOOR), 1e-10);

      // Test HALF_EVEN
      assertEquals(2.0, MathUtil.round(2.5, 0, RoundingMode.HALF_EVEN), 1e-10);
      assertEquals(4.0, MathUtil.round(3.5, 0, RoundingMode.HALF_EVEN), 1e-10);
    }

    @Test
    @DisplayName("Test round with invalid places")
    void testRound_InvalidPlaces() {
      assertThrows(IllegalArgumentException.class, () -> MathUtil.round(3.14159, -1));
      assertThrows(
          IllegalArgumentException.class, () -> MathUtil.round(3.14159, -5, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Test round with special values")
    void testRound_SpecialValues() {
      // Test with NaN
      assertTrue(Double.isNaN(MathUtil.round(Double.NaN, 2)));
      assertTrue(Double.isNaN(MathUtil.round(Double.NaN, 2, RoundingMode.HALF_UP)));

      // Test with infinity
      assertEquals(Double.POSITIVE_INFINITY, MathUtil.round(Double.POSITIVE_INFINITY, 2), 1e-10);
      assertEquals(Double.NEGATIVE_INFINITY, MathUtil.round(Double.NEGATIVE_INFINITY, 2), 1e-10);
    }

    @Test
    @DisplayName("Test truncate")
    void testTruncate() {
      assertEquals(3.14, MathUtil.truncate(3.14999, 2), 1e-10);
      assertEquals(-3.14, MathUtil.truncate(-3.14999, 2), 1e-10);
      assertEquals(0.0, MathUtil.truncate(0.999, 0), 1e-10);
      assertEquals(123.0, MathUtil.truncate(123.456, 0), 1e-10);
      assertEquals(-123.0, MathUtil.truncate(-123.456, 0), 1e-10);

      // Test with zero
      assertEquals(0.0, MathUtil.truncate(0.0, 2), 1e-10);

      // Test with larger precision
      assertEquals(3.14159, MathUtil.truncate(3.141592653589793, 5), 1e-10);
    }

    @Test
    @DisplayName("Test truncate with invalid places")
    void testTruncate_InvalidPlaces() {
      assertThrows(IllegalArgumentException.class, () -> MathUtil.truncate(3.14159, -1));
    }

    @Test
    @DisplayName("Test truncate with special values")
    void testTruncate_SpecialValues() {
      // Test with NaN
      assertTrue(Double.isNaN(MathUtil.truncate(Double.NaN, 2)));

      // Test with infinity
      assertEquals(Double.POSITIVE_INFINITY, MathUtil.truncate(Double.POSITIVE_INFINITY, 2), 1e-10);
      assertEquals(Double.NEGATIVE_INFINITY, MathUtil.truncate(Double.NEGATIVE_INFINITY, 2), 1e-10);
    }

    @Test
    @DisplayName("Test roundToLong")
    void testRoundToLong() {
      assertEquals(3L, MathUtil.roundToLong(3.14));
      assertEquals(4L, MathUtil.roundToLong(3.5));
      assertEquals(4L, MathUtil.roundToLong(3.6));
      assertEquals(-3L, MathUtil.roundToLong(-3.14));
      assertEquals(-3L, MathUtil.roundToLong(-3.5));
      assertEquals(-4L, MathUtil.roundToLong(-3.6));
      assertEquals(0L, MathUtil.roundToLong(0.0));
      assertEquals(0L, MathUtil.roundToLong(0.4));
      assertEquals(1L, MathUtil.roundToLong(0.5));

      // Test with large values
      assertEquals(Long.MAX_VALUE, MathUtil.roundToLong(Double.MAX_VALUE));
      assertEquals(Long.MIN_VALUE, MathUtil.roundToLong(-Double.MAX_VALUE));
    }

    @Test
    @DisplayName("Test roundToLong with special values")
    void testRoundToLong_SpecialValues() {
      // Test with NaN
      assertThrows(IllegalArgumentException.class, () -> MathUtil.roundToLong(Double.NaN));

      // Test with infinity
      assertThrows(
          IllegalArgumentException.class, () -> MathUtil.roundToLong(Double.POSITIVE_INFINITY));
      assertThrows(
          IllegalArgumentException.class, () -> MathUtil.roundToLong(Double.NEGATIVE_INFINITY));
    }
  }

  // ================= Range and Interpolation Tests =================

  @Nested
  @DisplayName("Range and Interpolation Tests")
  class RangeAndInterpolationTests {

    @Test
    @DisplayName("Test clamp with double values")
    void testClamp_Double() {
      assertEquals(5.0, MathUtil.clamp(10.0, 0.0, 5.0), 1e-10);
      assertEquals(0.0, MathUtil.clamp(-5.0, 0.0, 10.0), 1e-10);
      assertEquals(7.5, MathUtil.clamp(7.5, 0.0, 10.0), 1e-10);
      assertEquals(0.0, MathUtil.clamp(0.0, 0.0, 10.0), 1e-10);
      assertEquals(10.0, MathUtil.clamp(10.0, 0.0, 10.0), 1e-10);
      // Test with negative ranges
      assertEquals(-2.0, MathUtil.clamp(-5.0, -2.0, 5.0), 1e-10);
      assertEquals(5.0, MathUtil.clamp(10.0, -2.0, 5.0), 1e-10);
      assertEquals(0.0, MathUtil.clamp(0.0, -2.0, 5.0), 1e-10);
    }

    @Test
    @DisplayName("Test clamp with integer values")
    void testClamp_Integer() {
      assertEquals(5, MathUtil.clamp(10, 0, 5));
      assertEquals(0, MathUtil.clamp(-5, 0, 10));
      assertEquals(7, MathUtil.clamp(7, 0, 10));
      assertEquals(0, MathUtil.clamp(0, 0, 10));
      assertEquals(10, MathUtil.clamp(10, 0, 10));
      // Test with negative ranges
      assertEquals(-2, MathUtil.clamp(-5, -2, 5));
      assertEquals(5, MathUtil.clamp(10, -2, 5));
      assertEquals(0, MathUtil.clamp(0, -2, 5));
    }

    @Test
    @DisplayName("Test clamp with invalid range")
    void testClamp_InvalidRange() {
      assertThrows(IllegalArgumentException.class, () -> MathUtil.clamp(5.0, 10.0, 0.0));
      assertThrows(IllegalArgumentException.class, () -> MathUtil.clamp(5, 10, 0));
    }

    @Test
    @DisplayName("Test clamp with special values")
    void testClamp_SpecialValues() {
      // Test with NaN
      assertTrue(Double.isNaN(MathUtil.clamp(Double.NaN, 0.0, 10.0)));

      // Test with infinity
      assertEquals(10.0, MathUtil.clamp(Double.POSITIVE_INFINITY, 0.0, 10.0), 1e-10);
      assertEquals(0.0, MathUtil.clamp(Double.NEGATIVE_INFINITY, 0.0, 10.0), 1e-10);
    }

    @Test
    @DisplayName("Test linear interpolation")
    void testLerp() {
      assertEquals(0.0, MathUtil.lerp(0.0, 10.0, 0.0), 1e-10);
      assertEquals(5.0, MathUtil.lerp(0.0, 10.0, 0.5), 1e-10);
      assertEquals(10.0, MathUtil.lerp(0.0, 10.0, 1.0), 1e-10);
      assertEquals(7.5, MathUtil.lerp(5.0, 10.0, 0.5), 1e-10);

      // Test with negative values
      assertEquals(-5.0, MathUtil.lerp(-10.0, 0.0, 0.5), 1e-10);
      assertEquals(0.0, MathUtil.lerp(-5.0, 5.0, 0.5), 1e-10);
      // Test extrapolation
      assertEquals(-5.0, MathUtil.lerp(0.0, 10.0, -0.5), 1e-10);
      assertEquals(15.0, MathUtil.lerp(0.0, 10.0, 1.5), 1e-10);
      // Test same start and end
      assertEquals(5.0, MathUtil.lerp(5.0, 5.0, 0.5), 1e-10);
    }

    @Test
    @DisplayName("Test inverse linear interpolation")
    void testInvLerp() {
      assertEquals(0.0, MathUtil.invLerp(0.0, 10.0, 0.0), 1e-10);
      assertEquals(0.5, MathUtil.invLerp(0.0, 10.0, 5.0), 1e-10);
      assertEquals(1.0, MathUtil.invLerp(0.0, 10.0, 10.0), 1e-10);
      assertEquals(0.5, MathUtil.invLerp(5.0, 10.0, 7.5), 1e-10);
      // Test with negative values
      assertEquals(0.5, MathUtil.invLerp(-10.0, 0.0, -5.0), 1e-10);
      assertEquals(0.5, MathUtil.invLerp(-5.0, 5.0, 0.0), 1e-10);

      // Test extrapolation
      assertEquals(-0.5, MathUtil.invLerp(0.0, 10.0, -5.0), 1e-10);
      assertEquals(1.5, MathUtil.invLerp(0.0, 10.0, 15.0), 1e-10);
    }

    @Test
    @DisplayName("Test invLerp with invalid range")
    void testInvLerp_InvalidRange() {
      assertThrows(IllegalArgumentException.class, () -> MathUtil.invLerp(5.0, 5.0, 7.0));
    }

    @Test
    @DisplayName("Test map function")
    void testMap() {
      assertEquals(50.0, MathUtil.map(5.0, 0.0, 10.0, 0.0, 100.0), 1e-10);
      assertEquals(0.0, MathUtil.map(0.0, 0.0, 10.0, 0.0, 100.0), 1e-10);
      assertEquals(100.0, MathUtil.map(10.0, 0.0, 10.0, 0.0, 100.0), 1e-10);

      // Test different ranges
      assertEquals(15.0, MathUtil.map(0.5, 0.0, 1.0, 10.0, 20.0), 1e-10);
      assertEquals(0.0, MathUtil.map(5.0, 0.0, 10.0, -10.0, 10.0), 1e-10);
      // Test with negative source range
      assertEquals(50.0, MathUtil.map(0.0, -5.0, 5.0, 0.0, 100.0), 1e-10);

      // Test extrapolation
      assertEquals(-50.0, MathUtil.map(-5.0, 0.0, 10.0, 0.0, 100.0), 1e-10);
      assertEquals(150.0, MathUtil.map(15.0, 0.0, 10.0, 0.0, 100.0), 1e-10);
    }

    @Test
    @DisplayName("Test map with invalid source range")
    void testMap_InvalidRange() {
      assertThrows(IllegalArgumentException.class, () -> MathUtil.map(5.0, 10.0, 10.0, 0.0, 100.0));
    }

    @Test
    @DisplayName("Test inRange")
    void testInRange() {
      assertTrue(MathUtil.inRange(5.0, 0.0, 10.0));
      assertTrue(MathUtil.inRange(0.0, 0.0, 10.0));
      assertTrue(MathUtil.inRange(10.0, 0.0, 10.0));
      assertFalse(MathUtil.inRange(-1.0, 0.0, 10.0));
      assertFalse(MathUtil.inRange(11.0, 0.0, 10.0));

      // Test with negative ranges
      assertTrue(MathUtil.inRange(0.0, -5.0, 5.0));
      assertFalse(MathUtil.inRange(-6.0, -5.0, 5.0));
      assertFalse(MathUtil.inRange(6.0, -5.0, 5.0));

      // Test edge case
      assertTrue(MathUtil.inRange(5.0, 5.0, 5.0)); // Single point range
    }

    @Test
    @DisplayName("Test inRange with special values")
    void testInRange_SpecialValues() {
      // Test with NaN
      assertFalse(MathUtil.inRange(Double.NaN, 0.0, 10.0));

      // Test with infinity
      assertFalse(MathUtil.inRange(Double.POSITIVE_INFINITY, 0.0, 10.0));
      assertFalse(MathUtil.inRange(Double.NEGATIVE_INFINITY, 0.0, 10.0));
    }

    @Test
    @DisplayName("Test percentage calculation")
    void testPercentage() {
      assertEquals(0.5, MathUtil.percentage(5.0, 0.0, 10.0), 1e-10);
      assertEquals(0.0, MathUtil.percentage(0.0, 0.0, 10.0), 1e-10);
      assertEquals(1.0, MathUtil.percentage(10.0, 0.0, 10.0), 1e-10);
      assertEquals(0.25, MathUtil.percentage(2.5, 0.0, 10.0), 1e-10);

      // Test with negative range
      assertEquals(0.5, MathUtil.percentage(0.0, -10.0, 10.0), 1e-10);
      assertEquals(0.0, MathUtil.percentage(-10.0, -10.0, 10.0), 1e-10);
      assertEquals(1.0, MathUtil.percentage(10.0, -10.0, 10.0), 1e-10);

      // Test extrapolation
      assertEquals(0.0, MathUtil.percentage(-5.0, 0.0, 10.0), 1e-10);
      assertEquals(1.0, MathUtil.percentage(15.0, 0.0, 10.0), 1e-10);
    }

    @Test
    @DisplayName("Test percentage with invalid range")
    void testPercentage_InvalidRange() {
      assertThrows(IllegalArgumentException.class, () -> MathUtil.percentage(5.0, 10.0, 10.0));
    }
  }

  // ================= Edge Cases and Special Values Tests =================

  @Nested
  @DisplayName("Edge Cases and Special Values Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Test methods with special double values")
    void testSpecialDoubleValues() {
      // Test with infinity
      assertFalse(MathUtil.isEqualToZero(Double.POSITIVE_INFINITY));
      assertFalse(MathUtil.isEqualToZero(Double.NEGATIVE_INFINITY));
      assertTrue(MathUtil.isDifferentFromZero(Double.POSITIVE_INFINITY));
      assertTrue(MathUtil.isDifferentFromZero(Double.NEGATIVE_INFINITY));

      // Test with NaN
      assertFalse(MathUtil.isEqualToZero(Double.NaN));
      assertFalse(MathUtil.isDifferentFromZero(Double.NaN));
      assertTrue(MathUtil.isEqual(Double.NaN, Double.NaN));
      assertFalse(MathUtil.isDifferent(Double.NaN, Double.NaN));
    }

    @Test
    @DisplayName("Test methods with special float values")
    void testSpecialFloatValues() {
      // Test with infinity
      assertFalse(MathUtil.isEqualToZero(Float.POSITIVE_INFINITY));
      assertFalse(MathUtil.isEqualToZero(Float.NEGATIVE_INFINITY));
      assertTrue(MathUtil.isDifferentFromZero(Float.POSITIVE_INFINITY));
      assertTrue(MathUtil.isDifferentFromZero(Float.NEGATIVE_INFINITY));

      // Test with NaN
      assertFalse(MathUtil.isEqualToZero(Float.NaN));
      assertFalse(MathUtil.isDifferentFromZero(Float.NaN));
      assertTrue(MathUtil.isEqual(Float.NaN, Float.NaN));
      assertFalse(MathUtil.isDifferent(Float.NaN, Float.NaN));
    }

    @Test
    @DisplayName("Test methods with very small values")
    void testVerySmallValues() {
      // Test with values smaller than epsilon
      assertTrue(MathUtil.isEqualToZero(1e-15));
      assertTrue(MathUtil.isEqual(1e-15, 2e-15, 1e-9));
      assertFalse(MathUtil.isDifferentFromZero(1e-15));
      // Test subnormal values
      assertTrue(MathUtil.isEqualToZero(Double.MIN_VALUE));
      assertTrue(MathUtil.isEqualToZero(Float.MIN_VALUE));
    }

    @Test
    @DisplayName("Test custom epsilon methods")
    void testCustomEpsilonMethods() {
      // Test with custom epsilon values
      assertTrue(MathUtil.isEqualToZero(0.001, 0.01));
      assertFalse(MathUtil.isEqualToZero(0.1, 0.01));

      assertTrue(MathUtil.isEqual(1.0, 1.01, 0.02));
      assertFalse(MathUtil.isEqual(1.0, 1.05, 0.02));

      assertTrue(MathUtil.isDifferent(1.0, 1.05, 0.02));
      assertFalse(MathUtil.isDifferent(1.0, 1.01, 0.02));
      // Test with zero epsilon
      assertFalse(MathUtil.isEqual(1.0, 1.0001, 0.0));
      assertTrue(MathUtil.isEqual(1.0, 1.0, 0.0));

      // Test with very large epsilon
      assertTrue(MathUtil.isEqual(1.0, 2.0, 10.0));
      assertFalse(MathUtil.isDifferent(1.0, 2.0, 10.0));
    }

    @Test
    @DisplayName("Test extreme value calculations")
    void testExtremeValues() {
      // Test with maximum values
      assertTrue(MathUtil.isEqual(Double.MAX_VALUE, Double.MAX_VALUE));
      assertFalse(MathUtil.isDifferent(Double.MAX_VALUE, Double.MAX_VALUE));

      // Test with minimum positive values
      assertTrue(MathUtil.isEqualToZero(Double.MIN_VALUE));
      assertTrue(MathUtil.isEqualToZero(Float.MIN_VALUE));

      // Test clamping with extreme values
      assertEquals(100.0, MathUtil.clamp(Double.MAX_VALUE, 0.0, 100.0), 1e-10);
      assertEquals(0.0, MathUtil.clamp(-Double.MAX_VALUE, 0.0, 100.0), 1e-10);
    }
  }

  // ================= Additional Validation Tests =================

  @Nested
  @DisplayName("Additional Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("Test validateFiniteValues behavior")
    void testValidateFiniteValues() {
      // These should not throw exceptions as they test internal validation
      // We test this indirectly through methods that use validation

      // Test geometric methods with invalid values
      assertThrows(
          IllegalArgumentException.class, () -> MathUtil.getOrientation(Double.NaN, 0, 1, 1));
      assertThrows(
          IllegalArgumentException.class,
          () -> MathUtil.getAzimuth(Double.POSITIVE_INFINITY, 0, 1, 1));
      assertThrows(
          IllegalArgumentException.class,
          () -> MathUtil.getDistance(0, Double.NEGATIVE_INFINITY, 1, 1));
    }

    @Test
    @DisplayName("Test Point2D input validation")
    void testPoint2DValidation() {
      Point2D invalidPoint = new Point2D.Double(Double.NaN, 0);
      Point2D validPoint = new Point2D.Double(1, 1);

      assertThrows(
          IllegalArgumentException.class, () -> MathUtil.getOrientation(invalidPoint, validPoint));
      assertThrows(
          IllegalArgumentException.class, () -> MathUtil.getAzimuth(invalidPoint, validPoint));
      assertThrows(
          IllegalArgumentException.class, () -> MathUtil.getDistance(invalidPoint, validPoint));
    }

    @Test
    @DisplayName("Test precision edge cases")
    void testPrecisionEdgeCases() {
      // Test values at the epsilon boundary
      double epsilon = MathUtil.DOUBLE_EPSILON;
      assertTrue(MathUtil.isEqual(1.0, 1.0 + epsilon / 2));
      assertFalse(MathUtil.isEqual(1.0, 1.0 + epsilon * 2));

      float floatEpsilon = MathUtil.FLOAT_EPSILON;
      assertTrue(MathUtil.isEqual(1.0f, 1.0f + floatEpsilon / 2));
      assertFalse(MathUtil.isEqual(1.0f, 1.0f + floatEpsilon * 2));
    }
  }
}
