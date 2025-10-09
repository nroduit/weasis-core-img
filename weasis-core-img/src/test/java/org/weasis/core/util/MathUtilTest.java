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
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("MathUtil Tests")
@DisplayNameGeneration(ReplaceUnderscores.class)
class MathUtilTest {

  // Test data record for floating point comparisons
  record FloatingPointTestData(
      double value,
      double epsilon,
      boolean expectedEqualToZero,
      boolean expectedDifferentFromZero) {}

  // Test data record for geometric calculations
  record GeometricTestData(double x1, double y1, double x2, double y2, double expectedResult) {}

  // Test data record for angle normalization
  record AngleTestData(double input, double expected) {}

  // ================= Constants Tests =================

  @Test
  void mathematical_constants_should_match_expected_values() {
    assertAll(
        "Mathematical constants verification",
        () -> assertEquals(1e-9, MathUtil.DOUBLE_EPSILON),
        () -> assertEquals(1e-5f, MathUtil.FLOAT_EPSILON),
        () -> assertEquals(Math.PI / 180.0, MathUtil.DEGREES_TO_RADIANS, 1e-15),
        () -> assertEquals(180.0 / Math.PI, MathUtil.RADIANS_TO_DEGREES, 1e-13));
  }

  // ================= Zero Comparison Tests =================

  @Nested
  class Zero_Comparison_Tests {

    static Stream<FloatingPointTestData> double_zero_comparison_test_data() {
      return Stream.of(
          new FloatingPointTestData(0.0, MathUtil.DOUBLE_EPSILON, true, false),
          new FloatingPointTestData(1e-10, MathUtil.DOUBLE_EPSILON, true, false),
          new FloatingPointTestData(-1e-10, MathUtil.DOUBLE_EPSILON, true, false),
          new FloatingPointTestData(1e-8, MathUtil.DOUBLE_EPSILON, false, true),
          new FloatingPointTestData(-1e-8, MathUtil.DOUBLE_EPSILON, false, true),
          new FloatingPointTestData(1.0, MathUtil.DOUBLE_EPSILON, false, true));
    }

    static Stream<FloatingPointTestData> float_zero_comparison_test_data() {
      return Stream.of(
          new FloatingPointTestData(0.0f, MathUtil.FLOAT_EPSILON, true, false),
          new FloatingPointTestData(1e-6f, MathUtil.FLOAT_EPSILON, true, false),
          new FloatingPointTestData(-1e-6f, MathUtil.FLOAT_EPSILON, true, false),
          new FloatingPointTestData(1e-4f, MathUtil.FLOAT_EPSILON, false, true),
          new FloatingPointTestData(-1e-4f, MathUtil.FLOAT_EPSILON, false, true),
          new FloatingPointTestData(1.0f, MathUtil.FLOAT_EPSILON, false, true));
    }

    @ParameterizedTest
    @MethodSource("double_zero_comparison_test_data")
    void double_zero_comparisons_with_default_epsilon(FloatingPointTestData testData) {
      assertEquals(
          testData.expectedEqualToZero(),
          MathUtil.isEqualToZero(testData.value()),
          () -> "isEqualToZero failed for value: " + testData.value());
      assertEquals(
          testData.expectedDifferentFromZero(),
          MathUtil.isDifferentFromZero(testData.value()),
          () -> "isDifferentFromZero failed for value: " + testData.value());
    }

    @ParameterizedTest
    @MethodSource("float_zero_comparison_test_data")
    void float_zero_comparisons_with_default_epsilon(FloatingPointTestData testData) {
      assertEquals(
          testData.expectedEqualToZero(),
          MathUtil.isEqualToZero((float) testData.value()),
          () -> "isEqualToZero failed for float value: " + testData.value());
      assertEquals(
          testData.expectedDifferentFromZero(),
          MathUtil.isDifferentFromZero((float) testData.value()),
          () -> "isDifferentFromZero failed for float value: " + testData.value());
    }

    @ParameterizedTest
    @CsvSource({"0.001, 0.01, true", "-0.005, 0.01, true", "0.02, 0.01, false"})
    void double_zero_comparisons_with_custom_epsilon(
        double value, double epsilon, boolean expectedEqualToZero) {
      assertEquals(expectedEqualToZero, MathUtil.isEqualToZero(value, epsilon));
      assertEquals(!expectedEqualToZero, MathUtil.isDifferentFromZero(value, epsilon));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void special_double_values_should_not_be_equal_to_zero(double specialValue) {
      assertFalse(MathUtil.isEqualToZero(specialValue));
      // NaN should return false for isDifferentFromZero, but infinities should return true
      assertEquals(!Double.isNaN(specialValue), MathUtil.isDifferentFromZero(specialValue));
    }

    @ParameterizedTest
    @ValueSource(floats = {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
    void special_float_values_should_not_be_equal_to_zero(float specialValue) {
      assertFalse(MathUtil.isEqualToZero(specialValue));
      // NaN should return false for isDifferentFromZero, but infinities should return true
      assertEquals(!Float.isNaN(specialValue), MathUtil.isDifferentFromZero(specialValue));
    }
  }

  // ================= Equality Comparison Tests =================

  @Nested
  class Equality_Comparison_Tests {

    static Stream<Arguments> double_equality_test_data() {
      return Stream.of(
          Arguments.of(10.0, 10.0, MathUtil.DOUBLE_EPSILON, true, "exact equality"),
          Arguments.of(0.0, 0.0, MathUtil.DOUBLE_EPSILON, true, "zero equality"),
          Arguments.of(10.0, 10.0 + 5e-9, MathUtil.DOUBLE_EPSILON, true, "within tolerance"),
          Arguments.of(10.0, 10.0 + 2e-8, MathUtil.DOUBLE_EPSILON, false, "outside tolerance"),
          Arguments.of(10.0, 11.0, MathUtil.DOUBLE_EPSILON, false, "clearly different"));
    }

    static Stream<Arguments> float_equality_test_data() {
      return Stream.of(
          Arguments.of(10.0f, 10.0f, MathUtil.FLOAT_EPSILON, true, "exact equality"),
          Arguments.of(0.0f, 0.0f, MathUtil.FLOAT_EPSILON, true, "zero equality"),
          Arguments.of(10.0f, 10.0f + 5e-5f, MathUtil.FLOAT_EPSILON, true, "within tolerance"),
          Arguments.of(10.0f, 10.0f + 2e-4f, MathUtil.FLOAT_EPSILON, false, "outside tolerance"),
          Arguments.of(10.0f, 11.0f, MathUtil.FLOAT_EPSILON, false, "clearly different"));
    }

    @ParameterizedTest
    @MethodSource("double_equality_test_data")
    void double_equality_comparisons(
        double a, double b, double epsilon, boolean expected, String description) {
      assertEquals(expected, MathUtil.isEqual(a, b, epsilon), description);
      assertEquals(
          !expected,
          MathUtil.isDifferent(a, b, epsilon),
          "isDifferent should be opposite: " + description);
    }

    @ParameterizedTest
    @MethodSource("float_equality_test_data")
    void float_equality_comparisons(
        float a, float b, float epsilon, boolean expected, String description) {
      assertEquals(expected, MathUtil.isEqual(a, b, epsilon), description);
      assertEquals(
          !expected,
          MathUtil.isDifferent(a, b, epsilon),
          "isDifferent should be opposite: " + description);
    }

    @Test
    void special_values_equality_handling() {
      assertAll(
          "Special values equality",
          // Infinity comparisons
          () -> assertTrue(MathUtil.isEqual(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)),
          () -> assertTrue(MathUtil.isEqual(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)),
          () -> assertFalse(MathUtil.isEqual(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)),
          // NaN comparisons
          () -> assertTrue(MathUtil.isEqual(Double.NaN, Double.NaN)),
          () -> assertFalse(MathUtil.isEqual(Double.NaN, 1.0)),
          () -> assertFalse(MathUtil.isEqual(1.0, Double.NaN)));
    }

    @Test
    void small_values_use_absolute_comparison() {
      // Values smaller than MIN_EPSILON should use absolute comparison
      assertTrue(MathUtil.isEqual(1e-15, 2e-15, 1e-9));
      assertTrue(MathUtil.isEqual(0.0, 1e-10));
      assertTrue(MathUtil.isEqual(0.0f, 1e-6f));

      assertFalse(MathUtil.isEqual(0.0, 1e-8));
      assertFalse(MathUtil.isEqual(0.0f, 1e-4f));
    }
  }

  // ================= Geometric Calculations Tests =================

  @Nested
  class Geometric_Calculations_Tests {

    static Stream<GeometricTestData> orientation_test_data() {
      return Stream.of(
          new GeometricTestData(0.0, 0.0, 1.0, 0.0, 0.0), // Horizontal right
          new GeometricTestData(0.0, 0.0, 1.0, 1.0, 135.0), // Down-right diagonal
          new GeometricTestData(0.0, 0.0, -1.0, 1.0, 45.0), // Down-left diagonal
          new GeometricTestData(0.0, 0.0, 0.0, 1.0, 90.0), // Vertical down
          new GeometricTestData(0.0, 0.0, -1.0, 0.0, 180.0), // Horizontal left
          new GeometricTestData(0.0, 0.0, 1.0, -1.0, 45.0), // Up-right in image
          new GeometricTestData(0.0, 0.0, -1.0, -1.0, 135.0) // Up-left in image
          );
    }

    static Stream<GeometricTestData> azimuth_test_data() {
      return Stream.of(
          new GeometricTestData(0.0, 0.0, 0.0, -1.0, 0.0), // North
          new GeometricTestData(0.0, 0.0, 1.0, 0.0, 90.0), // East
          new GeometricTestData(0.0, 0.0, 0.0, 1.0, 180.0), // South
          new GeometricTestData(0.0, 0.0, -1.0, 0.0, 270.0), // West
          new GeometricTestData(0.0, 0.0, 1.0, -1.0, 45.0), // Northeast
          new GeometricTestData(0.0, 0.0, -1.0, -1.0, 315.0), // Northwest
          new GeometricTestData(0.0, 0.0, -1.0, 1.0, 225.0), // Southwest
          new GeometricTestData(0.0, 0.0, 1.0, 1.0, 135.0) // Southeast
          );
    }

    static Stream<GeometricTestData> distance_test_data() {
      return Stream.of(
          new GeometricTestData(0.0, 0.0, 3.0, 4.0, 5.0), // Pythagorean triple
          new GeometricTestData(1.0, 1.0, 1.0, 1.0, 0.0), // Same point
          new GeometricTestData(0.0, 0.0, 1.0, 1.0, Math.sqrt(2)), // Diagonal unit
          new GeometricTestData(-3.0, -4.0, 0.0, 0.0, 5.0), // Negative coordinates
          new GeometricTestData(1000.0, 1000.0, 1003.0, 1004.0, 5.0) // Large coordinates
          );
    }

    @ParameterizedTest
    @MethodSource("orientation_test_data")
    void orientation_calculations_with_coordinates(GeometricTestData testData) {
      double result =
          MathUtil.getOrientation(testData.x1(), testData.y1(), testData.x2(), testData.y2());
      assertEquals(
          testData.expectedResult(),
          result,
          1e-10,
          () ->
              String.format(
                  "Orientation from (%.1f,%.1f) to (%.1f,%.1f)",
                  testData.x1(), testData.y1(), testData.x2(), testData.y2()));
    }

    @ParameterizedTest
    @MethodSource("azimuth_test_data")
    void azimuth_calculations_with_coordinates(GeometricTestData testData) {
      double result =
          MathUtil.getAzimuth(testData.x1(), testData.y1(), testData.x2(), testData.y2());
      assertEquals(
          testData.expectedResult(),
          result,
          1e-10,
          () ->
              String.format(
                  "Azimuth from (%.1f,%.1f) to (%.1f,%.1f)",
                  testData.x1(), testData.y1(), testData.x2(), testData.y2()));
    }

    @ParameterizedTest
    @MethodSource("distance_test_data")
    void distance_calculations_with_coordinates(GeometricTestData testData) {
      double result =
          MathUtil.getDistance(testData.x1(), testData.y1(), testData.x2(), testData.y2());
      assertEquals(
          testData.expectedResult(),
          result,
          1e-10,
          () ->
              String.format(
                  "Distance from (%.1f,%.1f) to (%.1f,%.1f)",
                  testData.x1(), testData.y1(), testData.x2(), testData.y2()));
    }

    @Test
    void point2d_geometric_operations() {
      var origin = new Point2D.Double(0.0, 0.0);
      var right = new Point2D.Double(1.0, 0.0);
      var downRight = new Point2D.Double(1.0, 1.0);
      var point345 = new Point2D.Double(3.0, 4.0);

      assertAll(
          "Point2D geometric operations",
          () -> assertEquals(0.0, MathUtil.getOrientation(origin, right), 1e-10),
          () -> assertEquals(135.0, MathUtil.getOrientation(origin, downRight), 1e-10),
          () -> assertEquals(90.0, MathUtil.getAzimuth(origin, right), 1e-10),
          () -> assertEquals(5.0, MathUtil.getDistance(origin, point345), 1e-10));
    }

    @Test
    void null_point_inputs_should_return_null() {
      var validPoint = new Point2D.Double(1.0, 1.0);

      assertAll(
          "Null point handling",
          () -> assertNull(MathUtil.getOrientation(null, validPoint)),
          () -> assertNull(MathUtil.getOrientation(validPoint, null)),
          () -> assertNull(MathUtil.getAzimuth(null, validPoint)),
          () -> assertNull(MathUtil.getAzimuth(validPoint, null)),
          () -> assertNull(MathUtil.getDistance(null, validPoint)),
          () -> assertNull(MathUtil.getDistance(validPoint, null)));
    }

    @Test
    void invalid_coordinate_inputs_should_throw_exception() {
      assertAll(
          "Invalid coordinate validation",
          () ->
              assertThrows(
                  IllegalArgumentException.class,
                  () -> MathUtil.getOrientation(Double.NaN, 0, 1, 1)),
          () ->
              assertThrows(
                  IllegalArgumentException.class,
                  () -> MathUtil.getAzimuth(Double.POSITIVE_INFINITY, 0, 1, 1)),
          () ->
              assertThrows(
                  IllegalArgumentException.class,
                  () -> MathUtil.getDistance(0, Double.NEGATIVE_INFINITY, 1, 1)));
    }

    static Stream<AngleTestData> normalize_angle_test_data() {
      return Stream.of(
          new AngleTestData(0.0, 0.0),
          new AngleTestData(360.0, 0.0),
          new AngleTestData(-360.0, 0.0),
          new AngleTestData(90.0, 90.0),
          new AngleTestData(-90.0, 270.0),
          new AngleTestData(405.0, 45.0),
          new AngleTestData(-45.0, 315.0),
          new AngleTestData(180.0, 180.0),
          new AngleTestData(-180.0, 180.0),
          new AngleTestData(720.0 + 90.0, 90.0),
          new AngleTestData(-720.0 - 90.0, 270.0));
    }

    @ParameterizedTest
    @MethodSource("normalize_angle_test_data")
    void angle_normalization(AngleTestData testData) {
      assertEquals(
          testData.expected(),
          MathUtil.normalizeAngle(testData.input()),
          1e-10,
          () -> "Normalizing angle: " + testData.input());
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.POSITIVE_INFINITY, Double.NaN, Double.NEGATIVE_INFINITY})
    void invalid_angles_should_throw_exception(double invalidAngle) {
      assertThrows(IllegalArgumentException.class, () -> MathUtil.normalizeAngle(invalidAngle));
    }
  }

  // ================= Rounding Tests =================

  @Nested
  class Rounding_Tests {

    record RoundingTestData(
        double value, int places, RoundingMode mode, double expected, String description) {}

    static Stream<RoundingTestData> rounding_test_data() {
      return Stream.of(
          new RoundingTestData(3.14159, 2, RoundingMode.HALF_UP, 3.14, "basic rounding"),
          new RoundingTestData(3.14559, 2, RoundingMode.HALF_UP, 3.15, "round up"),
          new RoundingTestData(2.5, 0, RoundingMode.HALF_UP, 3.0, "half up positive"),
          new RoundingTestData(-2.5, 0, RoundingMode.HALF_UP, -3.0, "half up negative"),
          new RoundingTestData(3.14559, 2, RoundingMode.HALF_DOWN, 3.15, "half down"),
          new RoundingTestData(3.14999, 2, RoundingMode.DOWN, 3.14, "down"),
          new RoundingTestData(3.14001, 2, RoundingMode.UP, 3.15, "up"),
          new RoundingTestData(2.5, 0, RoundingMode.HALF_EVEN, 2.0, "half even to even"),
          new RoundingTestData(3.5, 0, RoundingMode.HALF_EVEN, 4.0, "half even to even"));
    }

    @ParameterizedTest
    @MethodSource("rounding_test_data")
    void rounding_with_various_modes(RoundingTestData testData) {
      double result = MathUtil.round(testData.value(), testData.places(), testData.mode());
      assertEquals(testData.expected(), result, 1e-10, testData.description());
    }

    @Test
    void default_rounding_uses_half_up() {
      assertEquals(3.14, MathUtil.round(3.14159, 2), 1e-10);
      assertEquals(3.0, MathUtil.round(2.5, 0), 1e-10);
    }

    @ParameterizedTest
    @CsvSource({
      "3.14999, 2, 3.14",
      "-3.14999, 2, -3.14",
      "123.456, 0, 123.0",
      "-123.456, 0, -123.0",
      "3.141592653589793, 5, 3.14159"
    })
    void truncate_operations(double value, int places, double expected) {
      assertEquals(expected, MathUtil.truncate(value, places), 1e-10);
    }

    @Test
    void special_values_rounding() {
      assertAll(
          "Special values rounding",
          () -> assertTrue(Double.isNaN(MathUtil.round(Double.NaN, 2))),
          () -> assertEquals(Double.POSITIVE_INFINITY, MathUtil.round(Double.POSITIVE_INFINITY, 2)),
          () ->
              assertEquals(Double.NEGATIVE_INFINITY, MathUtil.round(Double.NEGATIVE_INFINITY, 2)));
    }

    @Test
    void invalid_places_should_throw_exception() {
      assertThrows(IllegalArgumentException.class, () -> MathUtil.round(3.14159, -1));
      assertThrows(IllegalArgumentException.class, () -> MathUtil.truncate(3.14159, -1));
    }

    @ParameterizedTest
    @CsvSource({"3.14, 3", "3.5, 4", "3.6, 4", "-3.14, -3", "-3.6, -4", "0.0, 0", "0.5, 1"})
    void round_to_long_conversions(double value, long expected) {
      assertEquals(expected, MathUtil.roundToLong(value));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void round_to_long_with_invalid_values_should_throw_exception(double invalidValue) {
      assertThrows(IllegalArgumentException.class, () -> MathUtil.roundToLong(invalidValue));
    }
  }

  // ================= Range and Interpolation Tests =================

  @Nested
  class Range_And_Interpolation_Tests {

    @ParameterizedTest
    @CsvSource({
      "10.0, 0.0, 5.0, 5.0", // Clamp above max
      "-5.0, 0.0, 10.0, 0.0", // Clamp below min
      "7.5, 0.0, 10.0, 7.5", // Within range
      "0.0, 0.0, 10.0, 0.0", // At minimum
      "10.0, 0.0, 10.0, 10.0" // At maximum
    })
    void double_clamp_operations(double value, double min, double max, double expected) {
      assertEquals(expected, MathUtil.clamp(value, min, max), 1e-10);
    }

    @ParameterizedTest
    @CsvSource({"10, 0, 5, 5", "-5, 0, 10, 0", "7, 0, 10, 7"})
    void integer_clamp_operations(int value, int min, int max, int expected) {
      assertEquals(expected, MathUtil.clamp(value, min, max));
    }

    @Test
    void clamp_with_invalid_range_should_throw_exception() {
      assertThrows(IllegalArgumentException.class, () -> MathUtil.clamp(5.0, 10.0, 0.0));
      assertThrows(IllegalArgumentException.class, () -> MathUtil.clamp(5, 10, 0));
    }

    @Test
    void clamp_with_nan_preserves_nan() {
      assertTrue(Double.isNaN(MathUtil.clamp(Double.NaN, 0.0, 10.0)));
    }

    @ParameterizedTest
    @CsvSource({
      "0.0, 10.0, 0.0, 0.0", // Start
      "0.0, 10.0, 0.5, 5.0", // Middle
      "0.0, 10.0, 1.0, 10.0", // End
      "5.0, 10.0, 0.5, 7.5", // Different range
      "-10.0, 0.0, 0.5, -5.0", // Negative values
      "0.0, 10.0, -0.5, -5.0", // Extrapolation below
      "0.0, 10.0, 1.5, 15.0" // Extrapolation above
    })
    void linear_interpolation(double start, double end, double t, double expected) {
      assertEquals(expected, MathUtil.lerp(start, end, t), 1e-10);
    }

    @ParameterizedTest
    @CsvSource({
      "0.0, 10.0, 0.0, 0.0",
      "0.0, 10.0, 5.0, 0.5",
      "0.0, 10.0, 10.0, 1.0",
      "5.0, 10.0, 7.5, 0.5"
    })
    void inverse_linear_interpolation(double start, double end, double value, double expected) {
      assertEquals(expected, MathUtil.invLerp(start, end, value), 1e-10);
    }

    @Test
    void inverse_lerp_with_equal_start_and_end_should_throw_exception() {
      assertThrows(IllegalArgumentException.class, () -> MathUtil.invLerp(5.0, 5.0, 7.0));
    }

    @ParameterizedTest
    @CsvSource({
      "5.0, 0.0, 10.0, 0.0, 100.0, 50.0",
      "0.0, 0.0, 10.0, 0.0, 100.0, 0.0",
      "10.0, 0.0, 10.0, 0.0, 100.0, 100.0"
    })
    void map_function(
        double value, double fromMin, double fromMax, double toMin, double toMax, double expected) {
      assertEquals(expected, MathUtil.map(value, fromMin, fromMax, toMin, toMax), 1e-10);
    }

    @Test
    void map_with_invalid_source_range_should_throw_exception() {
      assertThrows(IllegalArgumentException.class, () -> MathUtil.map(5.0, 10.0, 10.0, 0.0, 100.0));
    }

    @ParameterizedTest
    @CsvSource({
      "5.0, 0.0, 10.0, true",
      "-1.0, 0.0, 10.0, false",
      "11.0, 0.0, 10.0, false",
      "0.0, 0.0, 10.0, true",
      "10.0, 0.0, 10.0, true"
    })
    void in_range_checks(double value, double min, double max, boolean expected) {
      assertEquals(expected, MathUtil.inRange(value, min, max));
    }

    @Test
    void in_range_with_special_values() {
      assertFalse(MathUtil.inRange(Double.NaN, 0.0, 10.0));
      assertFalse(MathUtil.inRange(Double.POSITIVE_INFINITY, 0.0, 10.0));
      assertFalse(MathUtil.inRange(Double.NEGATIVE_INFINITY, 0.0, 10.0));
    }

    @ParameterizedTest
    @CsvSource({
      "5.0, 0.0, 10.0, 0.5",
      "0.0, 0.0, 10.0, 0.0",
      "10.0, 0.0, 10.0, 1.0",
      "-5.0, 0.0, 10.0, 0.0", // Clamped to 0.0
      "15.0, 0.0, 10.0, 1.0" // Clamped to 1.0
    })
    void percentage_calculations(double value, double min, double max, double expected) {
      assertEquals(expected, MathUtil.percentage(value, min, max), 1e-10);
    }

    @Test
    void percentage_with_zero_width_range_should_throw_exception() {
      assertThrows(IllegalArgumentException.class, () -> MathUtil.percentage(5.0, 10.0, 10.0));
    }
  }

  // ================= Edge Cases and Validation Tests =================

  @Nested
  class Edge_Cases_And_Validation_Tests {

    @Test
    void very_small_values_handling() {
      assertAll(
          "Very small values",
          () -> assertTrue(MathUtil.isEqualToZero(1e-15)),
          () -> assertTrue(MathUtil.isEqual(1e-15, 2e-15, 1e-9)),
          () -> assertFalse(MathUtil.isDifferentFromZero(1e-15)),
          () -> assertTrue(MathUtil.isEqualToZero(Double.MIN_VALUE)),
          () -> assertTrue(MathUtil.isEqualToZero(Float.MIN_VALUE)));
    }

    @Test
    void extreme_value_calculations() {
      assertAll(
          "Extreme values",
          () -> assertTrue(MathUtil.isEqual(Double.MAX_VALUE, Double.MAX_VALUE)),
          () -> assertFalse(MathUtil.isDifferent(Double.MAX_VALUE, Double.MAX_VALUE)),
          () -> assertEquals(100.0, MathUtil.clamp(Double.MAX_VALUE, 0.0, 100.0)),
          () -> assertEquals(0.0, MathUtil.clamp(-Double.MAX_VALUE, 0.0, 100.0)));
    }

    @Test
    void precision_edge_cases() {
      double epsilon = MathUtil.DOUBLE_EPSILON;
      float floatEpsilon = MathUtil.FLOAT_EPSILON;

      assertAll(
          "Precision edge cases",
          () -> assertTrue(MathUtil.isEqual(1.0, 1.0 + epsilon / 2)),
          () -> assertFalse(MathUtil.isEqual(1.0, 1.0 + epsilon * 2)),
          () -> assertTrue(MathUtil.isEqual(1.0f, 1.0f + floatEpsilon / 2)),
          () -> assertFalse(MathUtil.isEqual(1.0f, 1.0f + floatEpsilon * 2)));
    }

    @Test
    void point2d_input_validation() {
      var invalidPoint = new Point2D.Double(Double.NaN, 0);
      var validPoint = new Point2D.Double(1, 1);

      assertAll(
          "Point2D input validation",
          () ->
              assertThrows(
                  IllegalArgumentException.class,
                  () -> MathUtil.getOrientation(invalidPoint, validPoint)),
          () ->
              assertThrows(
                  IllegalArgumentException.class,
                  () -> MathUtil.getAzimuth(invalidPoint, validPoint)),
          () ->
              assertThrows(
                  IllegalArgumentException.class,
                  () -> MathUtil.getDistance(invalidPoint, validPoint)));
    }

    @Test
    void finite_value_validation() {
      assertAll(
          "Finite value validation",
          () ->
              assertThrows(
                  IllegalArgumentException.class, () -> MathUtil.lerp(Double.NaN, 1.0, 0.5)),
          () ->
              assertThrows(
                  IllegalArgumentException.class,
                  () -> MathUtil.invLerp(Double.POSITIVE_INFINITY, 1.0, 0.5)),
          () ->
              assertThrows(
                  IllegalArgumentException.class,
                  () -> MathUtil.map(1.0, Double.NEGATIVE_INFINITY, 10.0, 0.0, 100.0)));
    }
  }
}
