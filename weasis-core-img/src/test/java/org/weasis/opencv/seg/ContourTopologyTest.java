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

import java.awt.geom.Point2D;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.weasis.opencv.natives.NativeLibrary;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ContourTopologyTest {

  // Test data factory methods
  private static final Point ORIGIN = new Point(0, 0);
  private static final Point UNIT_X = new Point(1, 0);
  private static final Point UNIT_Y = new Point(0, 1);
  private static final Point POINT_1_2 = new Point(1, 2);
  private static final Point POINT_3_4 = new Point(3, 4);
  private static final Point POINT_5_6 = new Point(5, 6);
  private static final Point POINT_7_8 = new Point(7, 8);

  private static final double TOLERANCE = 1e-7;
  private static final int NO_PARENT = -1;

  @BeforeAll
  static void setup_opencv() {
    NativeLibrary.loadLibraryFromLibraryName();
  }

  private static Point[] createRectanglePoints(double width, double height) {
    return new Point[] {
      new Point(0, 0), new Point(width, 0), new Point(width, height), new Point(0, height)
    };
  }

  private static Point[] createCircleApproximation(double radius, int segments) {
    return IntStream.range(0, segments)
        .mapToObj(
            i -> {
              double angle = 2.0 * Math.PI * i / segments;
              return new Point(radius * Math.cos(angle), radius * Math.sin(angle));
            })
        .toArray(Point[]::new);
  }

  @Nested
  class Constructor_tests {

    @Nested
    class MatOfPoint_constructor {

      @Test
      void creates_topology_from_single_point() {
        var contour = new MatOfPoint(POINT_1_2);
        int parentId = 5;

        var topology = new ContourTopology(contour, parentId);

        assertAll(
            () -> assertEquals(parentId, topology.getParent()),
            () -> assertEquals(1, topology.getSegment().size()),
            () -> assertEquals(new Point2D.Double(1, 2), topology.getSegment().get(0)));
      }

      @Test
      void creates_topology_from_multiple_points() {
        var points = new Point[] {POINT_1_2, POINT_3_4, POINT_5_6};
        var contour = new MatOfPoint(points);
        int parentId = 10;

        var topology = new ContourTopology(contour, parentId);
        var segment = topology.getSegment();

        var expectedPoints =
            List.of(new Point2D.Double(1, 2), new Point2D.Double(3, 4), new Point2D.Double(5, 6));

        assertAll(
            () -> assertEquals(parentId, topology.getParent()),
            () -> assertEquals(3, segment.size()),
            () -> assertEquals(expectedPoints, segment));
      }

      @Test
      void creates_empty_topology_from_empty_contour() {
        var emptyContour = new MatOfPoint();
        int parentId = 0;

        var topology = new ContourTopology(emptyContour, parentId);

        assertAll(
            () -> assertEquals(parentId, topology.getParent()),
            () -> assertTrue(topology.getSegment().isEmpty()));
      }
    }

    @Nested
    class MatOfPoint2f_constructor {

      @Test
      void creates_topology_preserving_fractional_coordinates() {
        var fractionalPoint = new Point(1.5, 2.7);
        var contour = new MatOfPoint2f(fractionalPoint);

        var topology = new ContourTopology(contour, 1);

        assertAll(
            () -> assertEquals(1, topology.getSegment().size()),
            () -> assertEquals(1.5, topology.getSegment().get(0).getX(), TOLERANCE),
            () -> assertEquals(2.7, topology.getSegment().get(0).getY(), TOLERANCE));
      }

      @Test
      void creates_topology_with_high_precision_coordinates() {
        var precisePoint = new Point(Math.PI, Math.E);
        var contour = new MatOfPoint2f(precisePoint);

        var topology = new ContourTopology(contour, 1);

        assertAll(
            () -> assertEquals(1, topology.getSegment().size()),
            () -> assertEquals(Math.PI, topology.getSegment().get(0).getX(), TOLERANCE),
            () -> assertEquals(Math.E, topology.getSegment().get(0).getY(), TOLERANCE));
      }

      @Test
      void creates_empty_topology_from_empty_contour() {
        var emptyContour = new MatOfPoint2f();

        var topology = new ContourTopology(emptyContour, NO_PARENT);

        assertAll(
            () -> assertEquals(NO_PARENT, topology.getParent()),
            () -> assertTrue(topology.getSegment().isEmpty()));
      }
    }

    @Nested
    class Point_array_constructor {

      @Test
      void creates_topology_from_point_array() {
        var points = new Point[] {POINT_1_2, POINT_3_4, POINT_5_6, POINT_7_8};
        int parentId = 15;

        var topology = new ContourTopology(points, parentId);

        assertAll(
            () -> assertEquals(parentId, topology.getParent()),
            () -> assertEquals(4, topology.getSegment().size()),
            () -> assertEquals(new Point2D.Double(1, 2), topology.getSegment().get(0)),
            () -> assertEquals(new Point2D.Double(3, 4), topology.getSegment().get(1)),
            () -> assertEquals(new Point2D.Double(5, 6), topology.getSegment().get(2)),
            () -> assertEquals(new Point2D.Double(7, 8), topology.getSegment().get(3)));
      }

      @Test
      void creates_empty_topology_from_empty_array() {
        var emptyPoints = new Point[0];

        var topology = new ContourTopology(emptyPoints, 20);

        assertAll(
            () -> assertEquals(20, topology.getParent()),
            () -> assertTrue(topology.getSegment().isEmpty()));
      }

      @Test
      void creates_topology_from_single_point() {
        var singlePoint = new Point[] {POINT_5_6};

        var topology = new ContourTopology(singlePoint, 3);

        assertAll(
            () -> assertEquals(3, topology.getParent()),
            () -> assertEquals(1, topology.getSegment().size()),
            () -> assertEquals(new Point2D.Double(5, 6), topology.getSegment().get(0)));
      }
    }
  }

  @Nested
  class Parent_relationships {

    static Stream<Arguments> parent_id_values() {
      return Stream.of(
          Arguments.of(NO_PARENT, "no parent"),
          Arguments.of(0, "root parent"),
          Arguments.of(1, "first child"),
          Arguments.of(100, "deep hierarchy"),
          Arguments.of(Integer.MAX_VALUE, "maximum value"),
          Arguments.of(Integer.MIN_VALUE, "minimum value"));
    }

    @ParameterizedTest(name = "handles parent ID {0} ({1})")
    @MethodSource("parent_id_values")
    void stores_parent_id_correctly(int parentId, String description) {
      var contour = new MatOfPoint(POINT_1_2);

      var topology = new ContourTopology(contour, parentId);

      assertEquals(parentId, topology.getParent());
    }

    @Test
    void parent_id_is_immutable() {
      var originalParentId = 42;
      var topology = new ContourTopology(new MatOfPoint(POINT_1_2), originalParentId);

      assertEquals(originalParentId, topology.getParent());
    }
  }

  @Nested
  class Segment_behavior {

    @Test
    void segment_behaves_like_list() {
      var points = new Point[] {POINT_1_2, POINT_3_4, POINT_5_6};
      var topology = new ContourTopology(points, 1);
      var segment = topology.getSegment();

      assertAll(
          () -> assertEquals(3, segment.size()),
          () -> assertEquals(new Point2D.Double(3, 4), segment.get(1)),
          () -> assertTrue(segment.contains(new Point2D.Double(5, 6))),
          () -> assertEquals(0, segment.indexOf(new Point2D.Double(1, 2))));
    }

    @Test
    void newly_created_segment_has_no_children() {
      var contour = new MatOfPoint(POINT_1_2, POINT_3_4);
      var topology = new ContourTopology(contour, 1);

      assertTrue(topology.getSegment().getChildren().isEmpty());
    }

    @Test
    void segment_is_mutable_after_creation() {
      var topology = new ContourTopology(new Point[] {POINT_1_2, POINT_3_4}, 1);
      var segment = topology.getSegment();

      var newPoint = new Point2D.Double(9, 10);
      segment.add(newPoint);

      assertAll(
          () -> assertEquals(3, segment.size()),
          () -> assertTrue(segment.contains(newPoint)),
          () -> assertEquals(newPoint, segment.get(2)));
    }

    @Test
    void returns_same_segment_instance() {
      var topology = new ContourTopology(new Point[] {POINT_1_2, POINT_3_4}, 1);

      var segment1 = topology.getSegment();
      var segment2 = topology.getSegment();

      assertSame(segment1, segment2);
    }
  }

  @Nested
  class Coordinate_handling {

    @Test
    void preserves_zero_coordinates() {
      var points = new Point[] {ORIGIN, POINT_1_2};
      var topology = new ContourTopology(points, 1);

      assertAll(
          () -> assertEquals(0.0, topology.getSegment().get(0).getX(), TOLERANCE),
          () -> assertEquals(0.0, topology.getSegment().get(0).getY(), TOLERANCE));
    }

    @Test
    void handles_large_coordinate_values() {
      var largePoint = new Point(1_000_000, -1_000_000);
      var contour = new MatOfPoint2f(largePoint);

      var topology = new ContourTopology(contour, 1);
      var convertedPoint = topology.getSegment().get(0);

      assertAll(
          () -> assertEquals(1_000_000.0, convertedPoint.getX(), TOLERANCE),
          () -> assertEquals(-1_000_000.0, convertedPoint.getY(), TOLERANCE));
    }

    @Test
    void preserves_small_coordinate_differences() {
      var point1 = new Point(1.0, 1.0);
      var point2 = new Point(1.0000001, 1.0000001);
      var topology = new ContourTopology(new Point[] {point1, point2}, 1);

      assertAll(
          () -> assertEquals(2, topology.getSegment().size()),
          () -> assertNotEquals(topology.getSegment().get(0), topology.getSegment().get(1)));
    }
  }

  @Nested
  class Edge_cases {

    @Test
    void handles_duplicate_points() {
      var duplicatePoints = new Point[] {POINT_1_2, POINT_1_2, POINT_3_4, POINT_1_2};
      var topology = new ContourTopology(duplicatePoints, 1);

      var expectedPoint = new Point2D.Double(1, 2);
      assertAll(
          () -> assertEquals(4, topology.getSegment().size()),
          () -> assertEquals(expectedPoint, topology.getSegment().get(0)),
          () -> assertEquals(expectedPoint, topology.getSegment().get(1)),
          () -> assertEquals(expectedPoint, topology.getSegment().get(3)));
    }

    @Test
    void handles_collinear_points() {
      var collinearPoints = new Point[] {ORIGIN, UNIT_X, new Point(2, 0), new Point(3, 0)};
      var topology = new ContourTopology(collinearPoints, 1);

      assertAll(
          () -> assertEquals(4, topology.getSegment().size()),
          () -> assertEquals(new Point2D.Double(0, 0), topology.getSegment().get(0)),
          () -> assertEquals(new Point2D.Double(3, 0), topology.getSegment().get(3)));
    }
  }

  @Nested
  class Real_world_scenarios {

    @Test
    void handles_contour_hierarchy() {
      var outerContour = createRectanglePoints(100, 100);
      var innerContour = createRectanglePoints(50, 50);

      var outer = new ContourTopology(outerContour, NO_PARENT);
      var inner = new ContourTopology(innerContour, 0);

      assertAll(
          () -> assertEquals(NO_PARENT, outer.getParent()),
          () -> assertEquals(0, inner.getParent()),
          () -> assertEquals(4, outer.getSegment().size()),
          () -> assertEquals(4, inner.getSegment().size()));
    }

    @Test
    void handles_realistic_image_coordinates() {
      var imageCoords =
          new Point[] {
            new Point(150.5, 200.7),
            new Point(800.2, 300.1),
            new Point(1200.9, 600.5),
            new Point(500.3, 800.8)
          };

      var topology = new ContourTopology(new MatOfPoint2f(imageCoords), 2);

      assertAll(
          () -> assertEquals(2, topology.getParent()),
          () -> assertEquals(4, topology.getSegment().size()),
          () -> assertEquals(150.5, topology.getSegment().get(0).getX(), TOLERANCE));
    }

    @Test
    void maintains_data_integrity_across_constructors() {
      var originalPoints = new Point[] {POINT_1_2, POINT_3_4, POINT_5_6};

      var fromArray = new ContourTopology(originalPoints, 1);
      var fromMatOfPoint = new ContourTopology(new MatOfPoint(originalPoints), 1);
      var fromMatOfPoint2f = new ContourTopology(new MatOfPoint2f(originalPoints), 1);

      assertAll(
          () -> assertEquals(fromArray.getParent(), fromMatOfPoint.getParent()),
          () -> assertEquals(fromMatOfPoint.getParent(), fromMatOfPoint2f.getParent()),
          () -> assertEquals(fromArray.getSegment(), fromMatOfPoint.getSegment()),
          () -> assertEquals(fromMatOfPoint.getSegment(), fromMatOfPoint2f.getSegment()));
    }
  }

  @Nested
  class Performance_characteristics {

    @Test
    void handles_large_contours_efficiently() {
      var largeContour =
          IntStream.range(0, 1000).mapToObj(i -> new Point(i, i * 0.5)).toArray(Point[]::new);

      var topology = new ContourTopology(largeContour, 1);

      assertAll(
          () -> assertEquals(1, topology.getParent()),
          () -> assertEquals(1000, topology.getSegment().size()),
          () -> assertEquals(new Point2D.Double(0, 0), topology.getSegment().get(0)),
          () -> assertEquals(new Point2D.Double(999, 499.5), topology.getSegment().get(999)));
    }

    @Test
    void reuses_segment_instance() {
      var topology = new ContourTopology(new Point[] {POINT_1_2, POINT_3_4}, 1);

      var segment1 = topology.getSegment();
      var segment2 = topology.getSegment();

      assertSame(segment1, segment2);
    }
  }

  @Nested
  class Geometric_patterns {

    @Test
    void creates_circular_approximation() {
      var circlePoints = createCircleApproximation(10.0, 8);
      var topology = new ContourTopology(circlePoints, NO_PARENT);

      assertAll(
          () -> assertEquals(8, topology.getSegment().size()),
          () -> assertEquals(NO_PARENT, topology.getParent()),
          // First point should be at (radius, 0)
          () -> assertEquals(10.0, topology.getSegment().get(0).getX(), TOLERANCE),
          () -> assertEquals(0.0, topology.getSegment().get(0).getY(), TOLERANCE));
    }

    @Test
    void creates_rectangular_contour() {
      var rectPoints = createRectanglePoints(50, 30);
      var topology = new ContourTopology(rectPoints, 1);

      var segment = topology.getSegment();
      assertAll(
          () -> assertEquals(4, segment.size()),
          () -> assertEquals(new Point2D.Double(0, 0), segment.get(0)),
          () -> assertEquals(new Point2D.Double(50, 0), segment.get(1)),
          () -> assertEquals(new Point2D.Double(50, 30), segment.get(2)),
          () -> assertEquals(new Point2D.Double(0, 30), segment.get(3)));
    }
  }
}
