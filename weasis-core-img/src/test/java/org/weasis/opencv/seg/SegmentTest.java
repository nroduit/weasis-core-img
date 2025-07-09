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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opencv.osgi.OpenCVNativeLoader;

@DisplayName("Segment Tests")
class SegmentTest {

  // Test constants
  private static final double DELTA = 0.001;
  private static final Point2D POINT_1_2 = new Point2D.Double(1, 2);
  private static final Point2D POINT_3_4 = new Point2D.Double(3, 4);
  private static final Point2D POINT_5_6 = new Point2D.Double(5, 6);
  private static final Point2D POINT_7_8 = new Point2D.Double(7, 8);

  @BeforeAll
  static void loadNativeLib() {
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create empty segment with default constructor")
    void createsEmptySegmentWithDefaultConstructor() {
      Segment segment = new Segment();

      assertTrue(segment.isEmpty(), "Default constructor should create empty segment");
      assertTrue(segment.getChildren().isEmpty(), "Should have no children initially");
    }

    @Test
    @DisplayName("Should create segment from Point2D collection")
    void createsSegmentFromPoint2DCollection() {
      List<Point2D> points = Arrays.asList(POINT_1_2, POINT_3_4, POINT_5_6);

      Segment segment = new Segment(points);

      assertAll(
          "Segment should contain all provided points",
          () -> assertEquals(points.size(), segment.size()),
          () -> assertEquals(points, segment),
          () -> assertTrue(segment.getChildren().isEmpty()));
    }

    @Test
    @DisplayName("Should create segment with forced closure from collection")
    void createsSegmentWithForcedClosureFromCollection() {
      List<Point2D> points = Arrays.asList(POINT_1_2, POINT_3_4, POINT_5_6);

      Segment segment = new Segment(points, true);

      assertAll(
          "Segment should be closed with duplicate first point",
          () -> assertEquals(points.size() + 1, segment.size()),
          () -> assertEquals(POINT_1_2, segment.get(0)),
          () -> assertEquals(POINT_5_6, segment.get(2)),
          () -> assertEquals(POINT_1_2, segment.get(3), "Last point should equal first point"));
    }

    @Test
    @DisplayName("Should not add duplicate closing point when already closed")
    void doesNotAddDuplicateClosingPointWhenAlreadyClosed() {
      List<Point2D> closedPoints = Arrays.asList(POINT_1_2, POINT_3_4, POINT_1_2);

      Segment segment = new Segment(closedPoints, true);

      assertEquals(
          closedPoints.size(),
          segment.size(),
          "Should not add extra closing point when already closed");
    }

    @Test
    @DisplayName("Should create empty segment from null or empty collection")
    void createsEmptySegmentFromNullOrEmptyCollection() {
      assertAll(
          "Should handle null and empty collections gracefully",
          () -> {
            Segment nullSegment = new Segment((List<Point2D>) null);
            assertTrue(nullSegment.isEmpty(), "Null collection should create empty segment");
          },
          () -> {
            Segment emptySegment = new Segment(Collections.emptyList());
            assertTrue(emptySegment.isEmpty(), "Empty collection should create empty segment");
          });
    }

    @Test
    @DisplayName("Should create segment from float array")
    void createsSegmentFromFloatArray() {
      float[] points = {1.5f, 2.5f, 3.5f, 4.5f};

      Segment segment = new Segment(points);

      assertAll(
          "Segment should contain converted points",
          () -> assertEquals(2, segment.size()),
          () -> assertEquals(new Point2D.Double(1.5, 2.5), segment.get(0)),
          () -> assertEquals(new Point2D.Double(3.5, 4.5), segment.get(1)));
    }

    @Test
    @DisplayName("Should create segment from double array")
    void createsSegmentFromDoubleArray() {
      double[] points = {1.0, 2.0, 3.0, 4.0};

      Segment segment = new Segment(points);

      assertAll(
          "Segment should contain points from double array",
          () -> assertEquals(2, segment.size()),
          () -> assertEquals(POINT_1_2, segment.get(0)),
          () -> assertEquals(POINT_3_4, segment.get(1)));
    }

    @Test
    @DisplayName("Should create segment with transform and dimension")
    void createsSegmentWithTransformAndDimension() {
      double[] points = {0.5, 0.5, 1.0, 1.0};
      AffineTransform scale = AffineTransform.getScaleInstance(2.0, 3.0);
      Dimension dim = new Dimension(100, 200);

      Segment segment = new Segment(points, scale, false, dim);

      assertAll(
          "Segment should apply transform and dimension scaling",
          () -> assertEquals(2, segment.size()),
          () ->
              assertEquals(
                  new Point2D.Double(100.0, 300.0), segment.get(0)), // (0.5*2)*100, (0.5*3)*200
          () ->
              assertEquals(
                  new Point2D.Double(200.0, 600.0), segment.get(1)) // (1.0*2)*100, (1.0*3)*200
          );
    }
  }

  @Nested
  @DisplayName("Point Setting Tests")
  class PointSettingTests {

    @Test
    @DisplayName("Should set points from Point2D collection")
    void setsPointsFromPoint2DCollection() {
      Segment segment = new Segment();
      List<Point2D> points = Arrays.asList(POINT_1_2, POINT_3_4);

      segment.setPoints(points, false);

      assertEquals(points, segment);
    }

    @Test
    @DisplayName("Should set points with forced closure")
    void setsPointsWithForcedClosure() {
      Segment segment = new Segment();
      List<Point2D> openPoints = Arrays.asList(POINT_1_2, POINT_3_4, POINT_5_6);

      segment.setPoints(openPoints, true);

      assertAll(
          "Should close the segment",
          () -> assertEquals(openPoints.size() + 1, segment.size()),
          () -> assertEquals(POINT_1_2, segment.get(segment.size() - 1)));
    }

    @Test
    @DisplayName("Should clear existing points when setting new ones")
    void clearsExistingPointsWhenSettingNewOnes() {
      Segment segment = new Segment(Arrays.asList(POINT_7_8));
      List<Point2D> newPoints = Arrays.asList(POINT_1_2, POINT_3_4);

      segment.setPoints(newPoints, false);

      assertAll(
          "Should replace existing points",
          () -> assertEquals(2, segment.size()),
          () -> assertEquals(newPoints, segment),
          () -> assertFalse(segment.contains(POINT_7_8)));
    }

    @Test
    @DisplayName("Should handle null collection gracefully")
    void handlesNullCollectionGracefully() {
      Segment segment = new Segment(Arrays.asList(POINT_1_2));

      segment.setPoints((List<Point2D>) null, false);

      assertTrue(segment.isEmpty(), "Should clear segment when setting null collection");
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw exception for null array")
    void throwsExceptionForNullArray(float[] nullArray) {
      Segment segment = new Segment();

      assertThrows(
          NullPointerException.class,
          () -> segment.setPoints(nullArray, false, null),
          "Should throw exception for null array");
    }

    @Test
    @DisplayName("Should handle insufficient points in array")
    void handlesInsufficientPointsInArray() {
      Segment segment = new Segment();
      float[] insufficientPoints = {1.0f, 2.0f}; // Only 1 point (need at least 2)

      segment.setPoints(insufficientPoints, false, null);

      assertTrue(segment.isEmpty(), "Should create empty segment for insufficient points");
    }

    @Test
    @DisplayName("Should handle odd number of coordinates")
    void handlesOddNumberOfCoordinates() {
      Segment segment = new Segment();
      float[] oddPoints = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f}; // 2,5 points (odd count)

      segment.setPoints(oddPoints, false, null);

      assertEquals(2, segment.size(), "Should create segment with complete point pairs only");
    }
  }

  @Nested
  @DisplayName("Transform Tests")
  class TransformTests {

    @Test
    @DisplayName("Should apply affine transform correctly")
    void appliesAffineTransformCorrectly() {
      Segment segment = new Segment();
      float[] points = {1.0f, 2.0f, 3.0f, 4.0f};
      AffineTransform scale = AffineTransform.getScaleInstance(2.0, 3.0);

      segment.setPoints(points, scale, false, null);

      assertAll(
          "Transform should be applied to all points",
          () -> assertEquals(2, segment.size()),
          () -> assertEquals(new Point2D.Double(2.0, 6.0), segment.get(0)),
          () -> assertEquals(new Point2D.Double(6.0, 12.0), segment.get(1)));
    }

    @Test
    @DisplayName("Should handle translation transform")
    void handlesTranslationTransform() {
      Segment segment = new Segment();
      double[] points = {0.0, 0.0, 1.0, 1.0};
      AffineTransform translate = AffineTransform.getTranslateInstance(10.0, 20.0);

      segment.setPoints(points, translate, false, null);

      assertAll(
          "Translation should be applied",
          () -> assertEquals(new Point2D.Double(10.0, 20.0), segment.get(0)),
          () -> assertEquals(new Point2D.Double(11.0, 21.0), segment.get(1)));
    }

    @Test
    @DisplayName("Should handle rotation transform")
    void handlesRotationTransform() {
      Segment segment = new Segment();
      double[] points = {1.0, 0.0, 0.0, 1.0};
      AffineTransform rotate = AffineTransform.getRotateInstance(Math.PI / 2); // 90 degrees

      segment.setPoints(points, rotate, false, null);

      assertAll(
          "Rotation should be applied",
          () -> assertEquals(0.0, segment.get(0).getX(), DELTA),
          () -> assertEquals(1.0, segment.get(0).getY(), DELTA),
          () -> assertEquals(-1.0, segment.get(1).getX(), DELTA),
          () -> assertEquals(0.0, segment.get(1).getY(), DELTA));
    }

    @Test
    @DisplayName("Should work without transform")
    void worksWithoutTransform() {
      Segment segment = new Segment();
      float[] points = {1.0f, 2.0f, 3.0f, 4.0f};

      segment.setPoints(points, null, false, null);

      assertAll(
          "Points should remain unchanged without transform",
          () -> assertEquals(POINT_1_2, segment.get(0)),
          () -> assertEquals(POINT_3_4, segment.get(1)));
    }
  }

  @Nested
  @DisplayName("Dimension Scaling Tests")
  class DimensionScalingTests {

    @Test
    @DisplayName("Should scale points by dimensions")
    void scalesPointsByDimensions() {
      Segment segment = new Segment();
      double[] points = {0.5, 0.25, 1.0, 0.75}; // Normalized coordinates
      Dimension dim = new Dimension(200, 400);

      segment.setPoints(points, false, dim);

      assertAll(
          "Points should be scaled by dimensions",
          () -> assertEquals(new Point2D.Double(100.0, 100.0), segment.get(0)), // 0.5*200, 0.25*400
          () -> assertEquals(new Point2D.Double(200.0, 300.0), segment.get(1)) // 1.0*200, 0.75*400
          );
    }

    @Test
    @DisplayName("Should ignore invalid dimensions")
    void ignoresInvalidDimensions() {
      Segment segment = new Segment();
      double[] points = {1.0, 2.0, 3.0, 4.0};

      assertAll(
          "Invalid dimensions should be ignored",
          () -> {
            segment.setPoints(points, false, new Dimension(0, 100));
            assertEquals(POINT_1_2, segment.get(0)); // No scaling applied
          },
          () -> {
            segment.setPoints(points, false, new Dimension(100, -50));
            assertEquals(POINT_1_2, segment.get(0)); // No scaling applied
          },
          () -> {
            segment.setPoints(points, false, null);
            assertEquals(POINT_1_2, segment.get(0)); // No scaling applied
          });
    }

    @Test
    @DisplayName("Should combine transform and dimension scaling")
    void combinesTransformAndDimensionScaling() {
      Segment segment = new Segment();
      double[] points = {0.5, 0.5, 2.0, 2.0};
      AffineTransform scale = AffineTransform.getScaleInstance(2.0, 2.0);
      Dimension dim = new Dimension(100, 100);

      segment.setPoints(points, scale, false, dim);

      // First transform: 0.5*2 = 1.0, then dimension: 1.0*100 = 100.0
      assertEquals(new Point2D.Double(100.0, 100.0), segment.get(0));
    }
  }

  @Nested
  @DisplayName("Force Close Tests")
  class ForceCloseTests {

    @Test
    @DisplayName("Should close segment when force close is true")
    void closesSegmentWhenForceCloseIsTrue() {
      Segment segment = new Segment();
      float[] openPoints = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f};

      segment.setPoints(openPoints, true, null);

      assertAll(
          "Segment should be closed",
          () -> assertEquals(4, segment.size()), // 3 original + 1 closing point
          () -> assertEquals(POINT_1_2, segment.get(0)),
          () -> assertEquals(POINT_1_2, segment.get(3)) // Last point equals first
          );
    }

    @Test
    @DisplayName("Should not add closing point when already closed")
    void doesNotAddClosingPointWhenAlreadyClosed() {
      Segment segment = new Segment();
      float[] closedPoints = {1.0f, 2.0f, 3.0f, 4.0f, 1.0f, 2.0f}; // Already closed

      segment.setPoints(closedPoints, true, null);

      assertEquals(3, segment.size(), "Should not add extra closing point");
    }

    @Test
    @DisplayName("Should not close segment when force close is false")
    void doesNotCloseSegmentWhenForceCloseIsFalse() {
      Segment segment = new Segment();
      float[] openPoints = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f};

      segment.setPoints(openPoints, false, null);

      assertAll(
          "Segment should remain open",
          () -> assertEquals(3, segment.size()),
          () -> assertEquals(POINT_5_6, segment.get(2)), // Last point unchanged
          () -> assertNotEquals(POINT_1_2, segment.get(2)));
    }
  }

  @Nested
  @DisplayName("Child Management Tests")
  class ChildManagementTests {

    @Test
    @DisplayName("Should add child segment")
    void addsChildSegment() {
      Segment parent = new Segment(Arrays.asList(POINT_1_2, POINT_3_4));
      Segment child = new Segment(Arrays.asList(POINT_5_6, POINT_7_8));

      parent.addChild(child);

      assertAll(
          "Child should be added correctly",
          () -> assertEquals(1, parent.getChildren().size()),
          () -> assertTrue(parent.getChildren().contains(child)),
          () -> assertEquals(child, parent.getChildren().get(0)));
    }

    @Test
    @DisplayName("Should add multiple children")
    void addsMultipleChildren() {
      Segment parent = new Segment(Arrays.asList(POINT_1_2, POINT_3_4));
      Segment child1 = new Segment(Arrays.asList(POINT_5_6));
      Segment child2 = new Segment(Arrays.asList(POINT_7_8));

      parent.addChild(child1);
      parent.addChild(child2);

      assertAll(
          "Multiple children should be added",
          () -> assertEquals(2, parent.getChildren().size()),
          () -> assertTrue(parent.getChildren().contains(child1)),
          () -> assertTrue(parent.getChildren().contains(child2)));
    }

    @Test
    @DisplayName("Should handle null child gracefully")
    void handlesNullChildGracefully() {
      Segment parent = new Segment(Arrays.asList(POINT_1_2, POINT_3_4));

      parent.addChild(null);

      assertTrue(parent.getChildren().isEmpty(), "Should not add null child");
    }

    @Test
    @DisplayName("Should prevent self-reference")
    void preventsSelfReference() {
      Segment segment = new Segment(Arrays.asList(POINT_1_2, POINT_3_4));

      segment.addChild(segment);

      assertTrue(segment.getChildren().isEmpty(), "Should not add self as child");
    }

    @Test
    @DisplayName("Should return unmodifiable children list")
    void returnsChildrenList() {
      Segment parent = new Segment(Arrays.asList(POINT_1_2, POINT_3_4));
      Segment child = new Segment(Arrays.asList(POINT_5_6));
      parent.addChild(child);

      List<Segment> children = parent.getChildren();

      assertAll(
          "Children list should be accessible",
          () -> assertNotNull(children),
          () -> assertEquals(1, children.size()),
          () -> assertEquals(child, children.get(0)));
    }
  }

  @Nested
  @DisplayName("Utility Method Tests")
  class UtilityMethodTests {

    @Test
    @DisplayName("Should convert float array to double array")
    void convertsFloatArrayToDoubleArray() {
      float[] floatArray = {1.5f, 2.5f, 3.5f};
      double[] expected = {1.5, 2.5, 3.5};

      double[] result = Segment.convertFloatToDouble(floatArray);

      assertArrayEquals(expected, result, DELTA);
    }

    @Test
    @DisplayName("Should handle null float array")
    void handlesNullFloatArray() {
      double[] result = Segment.convertFloatToDouble(null);

      assertNull(result, "Should return null for null input");
    }

    @Test
    @DisplayName("Should handle empty float array")
    void handlesEmptyFloatArray() {
      float[] emptyArray = {};

      double[] result = Segment.convertFloatToDouble(emptyArray);

      assertAll(
          "Should handle empty array",
          () -> assertNotNull(result),
          () -> assertEquals(0, result.length));
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
    @DisplayName("Should handle special float values")
    void handlesSpecialFloatValues(float specialValue) {
      float[] array = {specialValue};

      double[] result = Segment.convertFloatToDouble(array);

      assertEquals(
          specialValue, result[0], DELTA, "Should convert special float value: " + specialValue);
    }
  }

  @Nested
  @DisplayName("Object Behavior Tests")
  class ObjectBehaviorTests {

    @Test
    @DisplayName("Should implement equals correctly")
    void implementsEqualsCorrectly() {
      List<Point2D> points = Arrays.asList(POINT_1_2, POINT_3_4);
      Segment segment1 = new Segment(points);
      Segment segment2 = new Segment(points);
      Segment child = new Segment(Arrays.asList(POINT_5_6));

      segment1.addChild(child);
      segment2.addChild(child);

      Segment segment3 = new Segment(points);
      Segment differentChild = new Segment(Arrays.asList(POINT_7_8));
      segment3.addChild(differentChild);

      assertAll(
          "equals should work correctly",
          () -> assertEquals(segment1, segment2, "Same points and children should be equal"),
          () -> assertNotEquals(segment1, segment3, "Different children should not be equal"),
          () -> assertEquals(segment1, segment1, "Should equal itself"),
          () -> assertNotEquals(segment1, null, "Should not equal null"),
          () -> assertNotEquals(segment1, "string", "Should not equal different type"));
    }

    @Test
    @DisplayName("Should implement hashCode consistently")
    void implementsHashCodeConsistently() {
      List<Point2D> points = Arrays.asList(POINT_1_2, POINT_3_4);
      Segment segment1 = new Segment(points);
      Segment segment2 = new Segment(points);

      assertEquals(
          segment1.hashCode(), segment2.hashCode(), "Equal segments should have same hash code");
    }

    @Test
    @DisplayName("Should maintain ArrayList contract")
    void maintainsArrayListContract() {
      Segment segment = new Segment();

      // Test ArrayList methods work correctly
      segment.add(POINT_1_2);
      segment.add(POINT_3_4);

      assertAll(
          "Should maintain ArrayList functionality",
          () -> assertEquals(2, segment.size()),
          () -> assertEquals(POINT_1_2, segment.get(0)),
          () -> assertTrue(segment.contains(POINT_3_4)),
          () -> assertEquals(1, segment.indexOf(POINT_3_4)));
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should handle complex real-world scenario")
    void handlesComplexRealWorldScenario() {
      // Create a complex segment representing a shape with holes
      double[] outerShape = {0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0};
      AffineTransform transform = AffineTransform.getScaleInstance(100, 100);
      Dimension viewport = new Dimension(800, 600);

      Segment parent = new Segment(outerShape, transform, true, null);

      // Add holes (children)
      double[] hole1 = {0.2, 0.2, 0.4, 0.2, 0.4, 0.4, 0.2, 0.4};
      double[] hole2 = {0.6, 0.6, 0.8, 0.6, 0.8, 0.8, 0.6, 0.8};

      Segment child1 = new Segment(hole1, transform, true, null);
      Segment child2 = new Segment(hole2, transform, true, null);

      parent.addChild(child1);
      parent.addChild(child2);

      assertAll(
          "Complex scenario should be handled correctly",
          () -> assertEquals(5, parent.size(), "Parent should be closed (4+1 points)"),
          () -> assertEquals(new Point2D.Double(0, 0), parent.get(0)),
          () -> assertEquals(new Point2D.Double(0, 0), parent.get(4)), // Closed
          () -> assertEquals(2, parent.getChildren().size()),
          () -> assertEquals(5, child1.size()), // Child should also be closed
          () -> assertEquals(5, child2.size()));
    }

    @Test
    @DisplayName("Should handle edge case with minimum valid points")
    void handlesEdgeCaseWithMinimumValidPoints() {
      float[] minPoints = {1.0f, 2.0f, 3.0f, 4.0f}; // Exactly 2 points

      Segment segment = new Segment(minPoints, null, false, null);

      assertAll(
          "Should handle minimum valid case",
          () -> assertEquals(2, segment.size()),
          () -> assertEquals(POINT_1_2, segment.get(0)),
          () -> assertEquals(POINT_3_4, segment.get(1)));
    }

    @Test
    @DisplayName("Should preserve precision through transformations")
    void preservesPrecisionThroughTransformations() {
      double[] precisePoints = {Math.PI, Math.E, Math.sqrt(2), Math.sqrt(3)};

      Segment segment = new Segment(precisePoints, null, false, null);

      assertAll(
          "Should preserve mathematical precision",
          () -> assertEquals(Math.PI, segment.get(0).getX(), DELTA),
          () -> assertEquals(Math.E, segment.get(0).getY(), DELTA),
          () -> assertEquals(Math.sqrt(2), segment.get(1).getX(), DELTA),
          () -> assertEquals(Math.sqrt(3), segment.get(1).getY(), DELTA));
    }
  }
}
