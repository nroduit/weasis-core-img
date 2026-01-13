/*
 * Copyright (c) 2024 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.seg;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.weasis.opencv.natives.NativeLibrary;

@DisplayNameGeneration(ReplaceUnderscores.class)
class SegmentTest {

  private static final double DELTA = 0.001;

  // Test data structures - using records for immutable test data
  record TestPoints(Point2D point1, Point2D point2, Point2D point3, Point2D point4) {}

  record GeometricShape(String name, double[] coordinates, int expectedPointCount) {}

  record TransformTest(String name, AffineTransform transform, Point2D input, Point2D expected) {}

  private static final TestPoints BASIC_POINTS =
      new TestPoints(
          new Point2D.Double(1, 2),
          new Point2D.Double(3, 4),
          new Point2D.Double(5, 6),
          new Point2D.Double(7, 8));

  @BeforeAll
  static void load_native_library() {
    NativeLibrary.loadLibraryFromLibraryName();
  }

  @Nested
  class Constructor_tests {

    @Test
    void creates_empty_segment_with_default_constructor() {
      var segment = new Segment();

      assertAll(
          () -> assertTrue(segment.isEmpty()), () -> assertTrue(segment.getChildren().isEmpty()));
    }

    @Test
    void creates_segment_from_point2d_collection() {
      var points = List.of(BASIC_POINTS.point1(), BASIC_POINTS.point2(), BASIC_POINTS.point3());

      var segment = new Segment(points);

      assertAll(
          () -> assertEquals(points.size(), segment.size()),
          () -> assertEquals(points, segment),
          () -> assertTrue(segment.getChildren().isEmpty()));
    }

    @ParameterizedTest
    @MethodSource("closedSegmentTestData")
    void creates_segment_with_forced_closure(List<Point2D> points, boolean shouldAddClosingPoint) {
      var segment = new Segment(points, true);

      int expectedSize = shouldAddClosingPoint ? points.size() + 1 : points.size();
      assertEquals(expectedSize, segment.size());

      if (shouldAddClosingPoint) {
        assertEquals(points.get(0), segment.get(segment.size() - 1));
      }
    }

    @ParameterizedTest
    @NullSource
    void creates_empty_segment_from_null_collection(List<Point2D> nullPoints) {
      var segment = new Segment(nullPoints);
      assertTrue(segment.isEmpty());
    }

    @Test
    void creates_empty_segment_from_empty_collection() {
      var segment = new Segment(Collections.<Point2D>emptyList());
      assertTrue(segment.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("geometricShapeData")
    void creates_segment_from_coordinate_arrays(GeometricShape shape) {
      var segment = new Segment(shape.coordinates());

      assertEquals(
          shape.expectedPointCount(), segment.size(), () -> "Failed for shape: " + shape.name());
    }

    @ParameterizedTest
    @MethodSource("transformAndDimensionData")
    void creates_segment_with_transform_and_dimension(
        double[] coordinates,
        AffineTransform transform,
        Dimension dimension,
        Point2D[] expectedPoints) {
      var segment = new Segment(coordinates, transform, false, dimension);

      assertAll(
          () -> assertEquals(expectedPoints.length, segment.size()),
          () -> {
            for (int i = 0; i < expectedPoints.length; i++) {
              int finalI = i;
              assertEquals(
                  expectedPoints[i], segment.get(i), () -> "Point " + finalI + " mismatch");
            }
          });
    }

    // Test data providers
    static Stream<Arguments> closedSegmentTestData() {
      return Stream.of(
          Arguments.of(
              List.of(BASIC_POINTS.point1(), BASIC_POINTS.point2(), BASIC_POINTS.point3()), true),
          Arguments.of(
              List.of(BASIC_POINTS.point1(), BASIC_POINTS.point2(), BASIC_POINTS.point1()), false));
    }

    static Stream<GeometricShape> geometricShapeData() {
      return Stream.of(
          new GeometricShape("rectangle", new double[] {0, 0, 1, 0, 1, 1, 0, 1}, 4),
          new GeometricShape("triangle", new double[] {0, 0, 1, 0, 0.5, 1}, 3),
          new GeometricShape("line", new double[] {0, 0, 1, 1}, 2));
    }

    static Stream<Arguments> transformAndDimensionData() {
      return Stream.of(
          Arguments.of(
              new double[] {0.5, 0.5, 1.0, 1.0},
              AffineTransform.getScaleInstance(2.0, 3.0),
              new Dimension(100, 200),
              new Point2D[] {
                new Point2D.Double(100.0, 300.0), // (0.5*2)*100, (0.5*3)*200
                new Point2D.Double(200.0, 600.0) // (1.0*2)*100, (1.0*3)*200
              }));
    }
  }

  @Nested
  class Point_setting_tests {

    @Test
    void sets_points_from_point2d_collection() {
      var segment = new Segment();
      var points = List.of(BASIC_POINTS.point1(), BASIC_POINTS.point2());

      segment.setPoints(points, false);

      assertEquals(points, segment);
    }

    @Test
    void clears_existing_points_when_setting_new_ones() {
      var segment = new Segment(List.of(BASIC_POINTS.point4()));
      var newPoints = List.of(BASIC_POINTS.point1(), BASIC_POINTS.point2());

      segment.setPoints(newPoints, false);

      assertAll(
          () -> assertEquals(2, segment.size()),
          () -> assertEquals(newPoints, segment),
          () -> assertFalse(segment.contains(BASIC_POINTS.point4())));
    }

    @ParameterizedTest
    @NullSource
    void throws_exception_for_null_array(float[] nullArray) {
      var segment = new Segment();

      assertThrows(NullPointerException.class, () -> segment.setPoints(nullArray, false, null));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3}) // Less than 4 coordinates (2 points minimum)
    void handles_insufficient_coordinates(int coordinateCount) {
      var segment = new Segment();
      var insufficientPoints = new float[coordinateCount];

      segment.setPoints(insufficientPoints, false, null);

      assertTrue(segment.isEmpty());
    }

    @Test
    void handles_odd_number_of_coordinates() {
      var segment = new Segment();
      var oddPoints = new float[] {1.0f, 2.0f, 3.0f, 4.0f, 5.0f}; // 2.5 points

      segment.setPoints(oddPoints, false, null);

      assertEquals(2, segment.size()); // Only complete point pairs
    }
  }

  @Nested
  class Transform_tests {

    @ParameterizedTest
    @MethodSource("transformTestData")
    void applies_transforms_correctly(TransformTest testCase) {
      var segment = new Segment();
      var coords = new double[] {testCase.input().getX(), testCase.input().getY(), 0, 0};

      segment.setPoints(coords, testCase.transform(), false, null);

      assertEquals(testCase.expected(), segment.get(0), testCase.name());
    }

    @Test
    void works_without_transform() {
      var segment = new Segment();
      var points = new float[] {1.0f, 2.0f, 3.0f, 4.0f};

      segment.setPoints(points, null, false, null);

      assertAll(
          () -> assertEquals(BASIC_POINTS.point1(), segment.get(0)),
          () -> assertEquals(BASIC_POINTS.point2(), segment.get(1)));
    }

    static Stream<TransformTest> transformTestData() {
      return Stream.of(
          new TransformTest(
              "scale",
              AffineTransform.getScaleInstance(2.0, 3.0),
              new Point2D.Double(1.0, 2.0),
              new Point2D.Double(2.0, 6.0)),
          new TransformTest(
              "translate",
              AffineTransform.getTranslateInstance(10.0, 20.0),
              new Point2D.Double(0.0, 0.0),
              new Point2D.Double(10.0, 20.0)),
          new TransformTest(
              "rotate_90_degrees",
              AffineTransform.getRotateInstance(Math.PI / 2),
              new Point2D.Double(1.0, 0.0),
              new Point2D.Double(0.0, 1.0)));
    }
  }

  @Nested
  class Dimension_scaling_tests {

    @Test
    void scales_points_by_dimensions() {
      var segment = new Segment();
      var normalizedCoords = new double[] {0.5, 0.25, 1.0, 0.75};
      var viewport = new Dimension(200, 400);

      segment.setPoints(normalizedCoords, false, viewport);

      assertAll(
          () -> assertEquals(new Point2D.Double(100.0, 100.0), segment.get(0)),
          () -> assertEquals(new Point2D.Double(200.0, 300.0), segment.get(1)));
    }

    @ParameterizedTest
    @MethodSource("invalidDimensionData")
    void ignores_invalid_dimensions(Dimension invalidDim) {
      var segment = new Segment();
      var points = new double[] {1.0, 2.0, 3.0, 4.0};

      segment.setPoints(points, false, invalidDim);

      assertEquals(BASIC_POINTS.point1(), segment.get(0)); // No scaling applied
    }

    static Stream<Dimension> invalidDimensionData() {
      return Stream.of(
          new Dimension(0, 100), new Dimension(100, -50), new Dimension(-10, -20), null);
    }
  }

  @Nested
  class Force_close_tests {

    @Test
    void closes_segment_when_force_close_is_true() {
      var segment = new Segment();
      var openPoints = new float[] {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f};

      segment.setPoints(openPoints, true, null);

      assertAll(
          () -> assertEquals(4, segment.size()), // 3 original + 1 closing
          () -> assertEquals(BASIC_POINTS.point1(), segment.get(0)),
          () -> assertEquals(BASIC_POINTS.point1(), segment.get(3)) // Closed
          );
    }

    @Test
    void does_not_add_closing_point_when_already_closed() {
      var segment = new Segment();
      var closedPoints = new float[] {1.0f, 2.0f, 3.0f, 4.0f, 1.0f, 2.0f};

      segment.setPoints(closedPoints, true, null);

      assertEquals(3, segment.size()); // No extra closing point
    }
  }

  @Nested
  class Child_management_tests {

    @Test
    void adds_and_manages_child_segments() {
      var parent = new Segment(List.of(BASIC_POINTS.point1(), BASIC_POINTS.point2()));
      var child1 = new Segment(List.of(BASIC_POINTS.point3()));
      var child2 = new Segment(List.of(BASIC_POINTS.point4()));

      parent.addChild(child1);
      parent.addChild(child2);

      var children = parent.getChildren();
      assertAll(
          () -> assertEquals(2, children.size()),
          () -> assertTrue(children.contains(child1)),
          () -> assertTrue(children.contains(child2)),
          () ->
              assertThrows(
                  UnsupportedOperationException.class,
                  () -> children.add(new Segment())) // Should be unmodifiable
          );
    }

    @Test
    void prevents_invalid_child_additions() {
      var segment = new Segment(List.of(BASIC_POINTS.point1()));

      segment.addChild(null);
      segment.addChild(segment); // Self-reference

      assertTrue(segment.getChildren().isEmpty());
    }
  }

  @Nested
  class Utility_method_tests {

    @ParameterizedTest
    @MethodSource("floatToDoubleConversionData")
    void converts_float_array_to_double_array(float[] input, double[] expected) {
      var result = Segment.convertFloatToDouble(input);

      if (expected == null) {
        assertNull(result);
      } else {
        assertArrayEquals(expected, result, DELTA);
      }
    }

    @ParameterizedTest
    @ValueSource(
        floats = {
          Float.MIN_VALUE,
          Float.MAX_VALUE,
          Float.POSITIVE_INFINITY,
          Float.NEGATIVE_INFINITY,
          Float.NaN
        })
    void handles_special_float_values(float specialValue) {
      var input = new float[] {specialValue};
      var result = Segment.convertFloatToDouble(input);

      assertEquals(specialValue, result[0], DELTA, "Failed for special value: " + specialValue);
    }

    static Stream<Arguments> floatToDoubleConversionData() {
      return Stream.of(
          Arguments.of(null, null),
          Arguments.of(new float[] {}, new double[] {}),
          Arguments.of(new float[] {1.5f, 2.5f, 3.5f}, new double[] {1.5, 2.5, 3.5}),
          Arguments.of(new float[] {0.1f, 0.2f}, new double[] {0.1, 0.2}));
    }
  }

  @Nested
  class Object_behavior_tests {

    @Test
    void implements_equals_and_hashcode_correctly() {
      var points = List.of(BASIC_POINTS.point1(), BASIC_POINTS.point2());
      var segment1 = new Segment(points);
      var segment2 = new Segment(points);
      var child = new Segment(List.of(BASIC_POINTS.point3()));

      segment1.addChild(child);
      segment2.addChild(child);

      assertAll(
          () -> assertEquals(segment1, segment2),
          () -> assertEquals(segment1.hashCode(), segment2.hashCode()),
          () -> assertEquals(segment1, segment1), // Reflexive
          () -> assertNotEquals(segment1, null),
          () -> assertNotEquals(segment1, "not a segment"));
    }

    @Test
    void maintains_arraylist_contract() {
      var segment = new Segment();

      segment.add(BASIC_POINTS.point1());
      segment.add(BASIC_POINTS.point2());

      assertAll(
          () -> assertEquals(2, segment.size()),
          () -> assertEquals(BASIC_POINTS.point1(), segment.get(0)),
          () -> assertTrue(segment.contains(BASIC_POINTS.point2())),
          () -> assertEquals(1, segment.indexOf(BASIC_POINTS.point2())),
          () -> assertTrue(segment.remove(BASIC_POINTS.point1())),
          () -> assertEquals(1, segment.size()));
    }
  }

  @Nested
  class Integration_tests {

    @Test
    void handles_complex_hierarchical_shape() {
      // Create main shape (rectangle)
      var outerBounds = new double[] {0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0};
      var transform = AffineTransform.getScaleInstance(100, 100);
      var parent = new Segment(outerBounds, transform, true, null);

      // Add holes (inner rectangles)
      var hole1Bounds = new double[] {0.2, 0.2, 0.4, 0.2, 0.4, 0.4, 0.2, 0.4};
      var hole2Bounds = new double[] {0.6, 0.6, 0.8, 0.6, 0.8, 0.8, 0.6, 0.8};

      var hole1 = new Segment(hole1Bounds, transform, true, null);
      var hole2 = new Segment(hole2Bounds, transform, true, null);

      parent.addChild(hole1);
      parent.addChild(hole2);

      assertAll(
          () -> assertEquals(5, parent.size()), // Closed rectangle
          () -> assertEquals(new Point2D.Double(0, 0), parent.get(0)),
          () -> assertEquals(new Point2D.Double(0, 0), parent.get(parent.size() - 1)), // Closed
          () -> assertEquals(2, parent.getChildren().size()),
          () -> assertEquals(5, hole1.size()),
          () -> assertEquals(5, hole2.size()),
          () -> assertTrue(parent.getChildren().containsAll(List.of(hole1, hole2))));
    }

    @Test
    void preserves_mathematical_precision() {
      var preciseCoords = new double[] {Math.PI, Math.E, Math.sqrt(2), Math.sqrt(3)};
      var segment = new Segment(preciseCoords);

      assertAll(
          () -> assertEquals(Math.PI, segment.get(0).getX(), DELTA),
          () -> assertEquals(Math.E, segment.get(0).getY(), DELTA),
          () -> assertEquals(Math.sqrt(2), segment.get(1).getX(), DELTA),
          () -> assertEquals(Math.sqrt(3), segment.get(1).getY(), DELTA));
    }

    @Test
    void handles_minimum_valid_input() {
      var minimalCoords = new float[] {1.0f, 2.0f, 3.0f, 4.0f}; // Exactly 2 points
      var segment = new Segment(minimalCoords);

      assertAll(
          () -> assertEquals(2, segment.size()),
          () -> assertEquals(BASIC_POINTS.point1(), segment.get(0)),
          () -> assertEquals(BASIC_POINTS.point2(), segment.get(1)));
    }
  }
}
