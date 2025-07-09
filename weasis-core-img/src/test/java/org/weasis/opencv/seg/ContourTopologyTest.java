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
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.osgi.OpenCVNativeLoader;

@DisplayName("ContourTopology Tests")
class ContourTopologyTest {

  // Test constants
  private static final Point POINT_1_2 = new Point(1, 2);
  private static final Point POINT_3_4 = new Point(3, 4);
  private static final Point POINT_5_6 = new Point(5, 6);
  private static final Point POINT_7_8 = new Point(7, 8);
  private static final double DELTA = 0.001;

  @BeforeAll
  static void loadNativeLib() {
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Nested
    @DisplayName("MatOfPoint Constructor Tests")
    class MatOfPointConstructorTests {

      @Test
      @DisplayName("Should create ContourTopology from MatOfPoint with single point")
      void createsContourTopologyFromMatOfPointWithSinglePoint() {
        MatOfPoint contour = new MatOfPoint(POINT_1_2);
        int parentId = 5;

        ContourTopology topology = new ContourTopology(contour, parentId);

        assertAll(
            "Single point contour should be handled correctly",
            () -> assertEquals(parentId, topology.getParent()),
            () -> assertEquals(1, topology.getSegment().size()),
            () -> assertEquals(new Point2D.Double(1, 2), topology.getSegment().get(0)));
      }

      @Test
      @DisplayName("Should create ContourTopology from MatOfPoint with multiple points")
      void createsContourTopologyFromMatOfPointWithMultiplePoints() {
        MatOfPoint contour = new MatOfPoint(POINT_1_2, POINT_3_4, POINT_5_6);
        int parentId = 10;

        ContourTopology topology = new ContourTopology(contour, parentId);
        Segment segment = topology.getSegment();

        List<Point2D> expectedPoints =
            Arrays.asList(
                new Point2D.Double(1, 2), new Point2D.Double(3, 4), new Point2D.Double(5, 6));

        assertAll(
            "Multiple points contour should be handled correctly",
            () -> assertEquals(parentId, topology.getParent()),
            () -> assertEquals(3, segment.size()),
            () -> assertEquals(expectedPoints, segment));
      }

      @Test
      @DisplayName("Should create empty ContourTopology from empty MatOfPoint")
      void createsEmptyContourTopologyFromEmptyMatOfPoint() {
        MatOfPoint emptyContour = new MatOfPoint();
        int parentId = 0;

        ContourTopology topology = new ContourTopology(emptyContour, parentId);

        assertAll(
            "Empty contour should create empty segment",
            () -> assertEquals(parentId, topology.getParent()),
            () -> assertEquals(0, topology.getSegment().size()),
            () -> assertTrue(topology.getSegment().isEmpty()));
      }

      @Test
      @DisplayName("Should handle MatOfPoint with fractional coordinates")
      void handlesMatOfPointWithFractionalCoordinates() {
        Point fractionalPoint = new Point(1.5, 2.7);
        MatOfPoint2f contour = new MatOfPoint2f(fractionalPoint);

        ContourTopology topology = new ContourTopology(contour, 1);

        assertAll(
            "Fractional coordinates should be preserved",
            () -> assertEquals(1, topology.getSegment().size()),
            () -> assertEquals(1.5, topology.getSegment().get(0).getX(), DELTA),
            () -> assertEquals(2.7, topology.getSegment().get(0).getY(), DELTA));
      }
    }

    @Nested
    @DisplayName("MatOfPoint2f Constructor Tests")
    class MatOfPoint2fConstructorTests {

      @Test
      @DisplayName("Should create ContourTopology from MatOfPoint2f")
      void createsContourTopologyFromMatOfPoint2f() {
        Point[] points = {POINT_1_2, POINT_3_4};
        MatOfPoint2f contour2f = new MatOfPoint2f(points);
        int parentId = 7;

        ContourTopology topology = new ContourTopology(contour2f, parentId);

        assertAll(
            "MatOfPoint2f should be handled correctly",
            () -> assertEquals(parentId, topology.getParent()),
            () -> assertEquals(2, topology.getSegment().size()),
            () -> assertEquals(new Point2D.Double(1, 2), topology.getSegment().get(0)),
            () -> assertEquals(new Point2D.Double(3, 4), topology.getSegment().get(1)));
      }

      @Test
      @DisplayName("Should create empty ContourTopology from empty MatOfPoint2f")
      void createsEmptyContourTopologyFromEmptyMatOfPoint2f() {
        MatOfPoint2f emptyContour2f = new MatOfPoint2f();
        int parentId = -1;

        ContourTopology topology = new ContourTopology(emptyContour2f, parentId);

        assertAll(
            "Empty MatOfPoint2f should create empty segment",
            () -> assertEquals(parentId, topology.getParent()),
            () -> assertTrue(topology.getSegment().isEmpty()));
      }

      @Test
      @DisplayName("Should handle MatOfPoint2f with high precision coordinates")
      void handlesMatOfPoint2fWithHighPrecisionCoordinates() {
        Point precisePoint = new Point(Math.PI, Math.E);
        MatOfPoint2f contour2f = new MatOfPoint2f(precisePoint);

        ContourTopology topology = new ContourTopology(contour2f, 1);

        assertAll(
            "High precision coordinates should be preserved",
            () -> assertEquals(1, topology.getSegment().size()),
            () -> assertEquals(Math.PI, topology.getSegment().get(0).getX(), DELTA),
            () -> assertEquals(Math.E, topology.getSegment().get(0).getY(), DELTA));
      }
    }

    @Nested
    @DisplayName("Point Array Constructor Tests")
    class PointArrayConstructorTests {

      @Test
      @DisplayName("Should create ContourTopology from Point array")
      void createsContourTopologyFromPointArray() {
        Point[] points = {POINT_1_2, POINT_3_4, POINT_5_6, POINT_7_8};
        int parentId = 15;

        ContourTopology topology = new ContourTopology(points, parentId);

        assertAll(
            "Point array should be handled correctly",
            () -> assertEquals(parentId, topology.getParent()),
            () -> assertEquals(4, topology.getSegment().size()),
            () -> assertEquals(new Point2D.Double(1, 2), topology.getSegment().get(0)),
            () -> assertEquals(new Point2D.Double(3, 4), topology.getSegment().get(1)),
            () -> assertEquals(new Point2D.Double(5, 6), topology.getSegment().get(2)),
            () -> assertEquals(new Point2D.Double(7, 8), topology.getSegment().get(3)));
      }

      @Test
      @DisplayName("Should create empty ContourTopology from empty Point array")
      void createsEmptyContourTopologyFromEmptyPointArray() {
        Point[] emptyPoints = {};
        int parentId = 20;

        ContourTopology topology = new ContourTopology(emptyPoints, parentId);

        assertAll(
            "Empty point array should create empty segment",
            () -> assertEquals(parentId, topology.getParent()),
            () -> assertTrue(topology.getSegment().isEmpty()));
      }

      @Test
      @DisplayName("Should handle single point array")
      void handlesSinglePointArray() {
        Point[] singlePoint = {POINT_5_6};
        int parentId = 3;

        ContourTopology topology = new ContourTopology(singlePoint, parentId);

        assertAll(
            "Single point array should be handled correctly",
            () -> assertEquals(parentId, topology.getParent()),
            () -> assertEquals(1, topology.getSegment().size()),
            () -> assertEquals(new Point2D.Double(5, 6), topology.getSegment().get(0)));
      }
    }
  }

  @Nested
  @DisplayName("Parent ID Tests")
  class ParentIdTests {

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 10, 100, 1000, Integer.MAX_VALUE, Integer.MIN_VALUE})
    @DisplayName("Should handle various parent ID values")
    void handlesVariousParentIdValues(int parentId) {
      MatOfPoint contour = new MatOfPoint(POINT_1_2);

      ContourTopology topology = new ContourTopology(contour, parentId);

      assertEquals(parentId, topology.getParent(), "Should correctly store parent ID: " + parentId);
    }

    @Test
    @DisplayName("Should handle negative parent ID correctly")
    void handlesNegativeParentIdCorrectly() {
      Point[] points = {POINT_1_2, POINT_3_4};
      int negativeParentId = -5;

      ContourTopology topology = new ContourTopology(points, negativeParentId);

      assertAll(
          "Negative parent ID should be handled correctly",
          () -> assertEquals(negativeParentId, topology.getParent()),
          () -> assertEquals(2, topology.getSegment().size()));
    }
  }

  @Nested
  @DisplayName("Segment Integration Tests")
  class SegmentIntegrationTests {

    @Test
    @DisplayName("Should create segment that behaves like ArrayList")
    void createsSegmentThatBehavesLikeArrayList() {
      Point[] points = {POINT_1_2, POINT_3_4, POINT_5_6};
      ContourTopology topology = new ContourTopology(points, 1);
      Segment segment = topology.getSegment();

      assertAll(
          "Segment should behave like ArrayList",
          () -> assertEquals(3, segment.size()),
          () -> assertEquals(new Point2D.Double(3, 4), segment.get(1)),
          () -> assertTrue(segment.contains(new Point2D.Double(5, 6))),
          () -> assertEquals(0, segment.indexOf(new Point2D.Double(1, 2))));
    }

    @Test
    @DisplayName("Should create segment with no children initially")
    void createsSegmentWithNoChildrenInitially() {
      MatOfPoint contour = new MatOfPoint(POINT_1_2, POINT_3_4);
      ContourTopology topology = new ContourTopology(contour, 1);

      assertTrue(
          topology.getSegment().getChildren().isEmpty(),
          "Newly created segment should have no children");
    }

    @Test
    @DisplayName("Should allow segment modification after creation")
    void allowsSegmentModificationAfterCreation() {
      Point[] points = {POINT_1_2, POINT_3_4};
      ContourTopology topology = new ContourTopology(points, 1);
      Segment segment = topology.getSegment();

      // Add a point to the segment
      Point2D newPoint = new Point2D.Double(9, 10);
      segment.add(newPoint);

      assertAll(
          "Segment should be modifiable",
          () -> assertEquals(3, segment.size()),
          () -> assertTrue(segment.contains(newPoint)),
          () -> assertEquals(newPoint, segment.get(2)));
    }
  }

  @Nested
  @DisplayName("Coordinate Conversion Tests")
  class CoordinateConversionTests {

    @Test
    @DisplayName("Should preserve coordinate precision during conversion")
    void preservesCoordinatePrecisionDuringConversion() {
      double preciseX = 1.23456789;
      double preciseY = 9.87654321;
      Point precisePoint = new Point(preciseX, preciseY);
      MatOfPoint2f contour = new MatOfPoint2f(precisePoint);

      ContourTopology topology = new ContourTopology(contour, 1);
      Point2D convertedPoint = topology.getSegment().get(0);

      assertAll(
          "Coordinate precision should be preserved",
          () -> assertEquals(preciseX, convertedPoint.getX(), DELTA),
          () -> assertEquals(preciseY, convertedPoint.getY(), DELTA));
    }

    @Test
    @DisplayName("Should handle zero coordinates correctly")
    void handlesZeroCoordinatesCorrectly() {
      Point zeroPoint = new Point(0, 0);
      Point[] points = {zeroPoint, POINT_1_2};
      ContourTopology topology = new ContourTopology(points, 1);

      assertAll(
          "Zero coordinates should be handled correctly",
          () -> assertEquals(0.0, topology.getSegment().get(0).getX(), DELTA),
          () -> assertEquals(0.0, topology.getSegment().get(0).getY(), DELTA));
    }

    @Test
    @DisplayName("Should handle large coordinate values")
    void handlesLargeCoordinateValues() {
      Point largePoint = new Point(1000000, -1000000);
      MatOfPoint2f contour = new MatOfPoint2f(largePoint);

      ContourTopology topology = new ContourTopology(contour, 1);
      Point2D convertedPoint = topology.getSegment().get(0);

      assertAll(
          "Large coordinate values should be handled correctly",
          () -> assertEquals(1000000.0, convertedPoint.getX(), DELTA),
          () -> assertEquals(-1000000.0, convertedPoint.getY(), DELTA));
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("Should have immutable parent ID")
    void hasImmutableParentId() {
      MatOfPoint contour = new MatOfPoint(POINT_1_2);
      int originalParentId = 42;
      ContourTopology topology = new ContourTopology(contour, originalParentId);

      // Parent ID should be immutable (final field)
      assertEquals(originalParentId, topology.getParent(), "Parent ID should remain unchanged");
    }

    @Test
    @DisplayName("Should return same segment instance consistently")
    void returnsSameSegmentInstanceConsistently() {
      Point[] points = {POINT_1_2, POINT_3_4};
      ContourTopology topology = new ContourTopology(points, 1);

      Segment segment1 = topology.getSegment();
      Segment segment2 = topology.getSegment();

      assertSame(segment1, segment2, "Should return same segment instance");
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling Tests")
  class EdgeCasesAndErrorHandlingTests {

    @Test
    @DisplayName("Should handle duplicate points in contour")
    void handlesDuplicatePointsInContour() {
      Point[] duplicatePoints = {POINT_1_2, POINT_1_2, POINT_3_4, POINT_1_2};
      ContourTopology topology = new ContourTopology(duplicatePoints, 1);

      assertAll(
          "Duplicate points should be preserved",
          () -> assertEquals(4, topology.getSegment().size()),
          () -> assertEquals(new Point2D.Double(1, 2), topology.getSegment().get(0)),
          () -> assertEquals(new Point2D.Double(1, 2), topology.getSegment().get(1)),
          () -> assertEquals(new Point2D.Double(1, 2), topology.getSegment().get(3)));
    }

    @Test
    @DisplayName("Should handle contour with collinear points")
    void handlesContourWithCollinearPoints() {
      Point[] collinearPoints = {
        new Point(0, 0), new Point(1, 1), new Point(2, 2), new Point(3, 3)
      };
      ContourTopology topology = new ContourTopology(collinearPoints, 1);

      assertAll(
          "Collinear points should be preserved",
          () -> assertEquals(4, topology.getSegment().size()),
          () -> assertEquals(new Point2D.Double(0, 0), topology.getSegment().get(0)),
          () -> assertEquals(new Point2D.Double(3, 3), topology.getSegment().get(3)));
    }

    @Test
    @DisplayName("Should handle very small coordinate differences")
    void handlesVerySmallCoordinateDifferences() {
      Point point1 = new Point(1.0, 1.0);
      Point point2 = new Point(1.0000001, 1.0000001);
      Point[] points = {point1, point2};

      ContourTopology topology = new ContourTopology(points, 1);

      assertAll(
          "Small coordinate differences should be preserved",
          () -> assertEquals(2, topology.getSegment().size()),
          () ->
              assertNotEquals(
                  topology.getSegment().get(0),
                  topology.getSegment().get(1),
                  "Points with small differences should not be equal"));
    }
  }

  @Nested
  @DisplayName("Integration and Real-World Scenario Tests")
  class IntegrationAndRealWorldScenarioTests {

    @Test
    @DisplayName("Should handle typical contour hierarchy scenario")
    void handlesTypicalContourHierarchyScenario() {
      // Simulate a typical OpenCV contour hierarchy scenario
      Point[] outerContour = {
        new Point(0, 0), new Point(100, 0),
        new Point(100, 100), new Point(0, 100)
      };
      Point[] innerContour = {
        new Point(25, 25), new Point(75, 25),
        new Point(75, 75), new Point(25, 75)
      };

      ContourTopology outer = new ContourTopology(outerContour, -1); // No parent
      ContourTopology inner = new ContourTopology(innerContour, 0); // Parent is contour 0

      assertAll(
          "Hierarchy scenario should work correctly",
          () -> assertEquals(-1, outer.getParent(), "Outer contour has no parent"),
          () -> assertEquals(0, inner.getParent(), "Inner contour's parent is outer"),
          () -> assertEquals(4, outer.getSegment().size()),
          () -> assertEquals(4, inner.getSegment().size()),
          () -> assertEquals(new Point2D.Double(0, 0), outer.getSegment().get(0)),
          () -> assertEquals(new Point2D.Double(25, 25), inner.getSegment().get(0)));
    }

    @Test
    @DisplayName("Should work with realistic image processing coordinates")
    void worksWithRealisticImageProcessingCoordinates() {
      // Simulate coordinates from a 1920x1080 image
      Point[] imageCoords = {
        new Point(150.5, 200.7),
        new Point(800.2, 300.1),
        new Point(1200.9, 600.5),
        new Point(500.3, 800.8)
      };

      MatOfPoint2f contour = new MatOfPoint2f(imageCoords);
      ContourTopology topology = new ContourTopology(contour, 2);

      assertAll(
          "Image processing coordinates should be handled correctly",
          () -> assertEquals(2, topology.getParent()),
          () -> assertEquals(4, topology.getSegment().size()),
          () -> assertEquals(150.5, topology.getSegment().get(0).getX(), DELTA),
          () -> assertEquals(1200.9, topology.getSegment().get(2).getX(), DELTA));
    }

    @Test
    @DisplayName("Should maintain data integrity through different constructor paths")
    void maintainsDataIntegrityThroughDifferentConstructorPaths() {
      Point[] originalPoints = {POINT_1_2, POINT_3_4, POINT_5_6};

      // Test all three constructor paths with same data
      ContourTopology fromArray = new ContourTopology(originalPoints, 1);
      ContourTopology fromMatOfPoint = new ContourTopology(new MatOfPoint(originalPoints), 1);
      ContourTopology fromMatOfPoint2f = new ContourTopology(new MatOfPoint2f(originalPoints), 1);

      assertAll(
          "All constructor paths should produce equivalent results",
          () -> assertEquals(fromArray.getParent(), fromMatOfPoint.getParent()),
          () -> assertEquals(fromMatOfPoint.getParent(), fromMatOfPoint2f.getParent()),
          () -> assertEquals(fromArray.getSegment().size(), fromMatOfPoint.getSegment().size()),
          () ->
              assertEquals(
                  fromMatOfPoint.getSegment().size(), fromMatOfPoint2f.getSegment().size()),
          () -> assertEquals(fromArray.getSegment(), fromMatOfPoint.getSegment()),
          () -> assertEquals(fromMatOfPoint.getSegment(), fromMatOfPoint2f.getSegment()));
    }
  }

  @Nested
  @DisplayName("Performance and Memory Tests")
  class PerformanceAndMemoryTests {

    @Test
    @DisplayName("Should handle large contours efficiently")
    void handlesLargeContoursEfficiently() {
      // Create a large contour (1000 points)
      Point[] largeContour = new Point[1000];
      for (int i = 0; i < 1000; i++) {
        largeContour[i] = new Point(i, i * 0.5);
      }

      ContourTopology topology = new ContourTopology(largeContour, 1);

      assertAll(
          "Large contour should be handled efficiently",
          () -> assertEquals(1, topology.getParent()),
          () -> assertEquals(1000, topology.getSegment().size()),
          () -> assertEquals(new Point2D.Double(0, 0), topology.getSegment().get(0)),
          () -> assertEquals(new Point2D.Double(999, 499.5), topology.getSegment().get(999)));
    }

    @Test
    @DisplayName("Should not create unnecessary object copies")
    void doesNotCreateUnnecessaryObjectCopies() {
      Point[] points = {POINT_1_2, POINT_3_4};
      ContourTopology topology = new ContourTopology(points, 1);

      // Segment should be created once and reused
      Segment segment1 = topology.getSegment();
      Segment segment2 = topology.getSegment();

      assertSame(segment1, segment2, "Should reuse segment instance");
    }
  }
}
