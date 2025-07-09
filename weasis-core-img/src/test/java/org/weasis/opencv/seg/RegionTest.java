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
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.osgi.OpenCVNativeLoader;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;

@DisplayName("Region Tests")
class RegionTest {

  // Test constants for better maintainability
  private static final String TEST_ID = "testId";
  private static final String EXPECTED_AREA_MESSAGE =
      "Area calculation should match expected value";
  private static final double DELTA = 0.001; // For floating point comparisons

  @BeforeAll
  static void loadNativeLib() {
    OpenCVNativeLoader loader = new OpenCVNativeLoader();
    loader.init();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should generate random ID when ID is null")
    void createsRegionWithRandomIdWhenIdIsNull() {
      Region region = new Region(null);

      assertNotNull(region.getId(), "ID should not be null");
      assertFalse(region.getId().trim().isEmpty(), "ID should not be empty");
    }

    @Test
    @DisplayName("Should generate random ID when ID is empty")
    void createsRegionWithRandomIdWhenIdIsEmpty() {
      Region region = new Region("  ");

      assertNotNull(region.getId());
      assertFalse(region.getId().trim().isEmpty());
    }

    @Test
    @DisplayName("Should use provided ID when valid")
    void createsRegionWithProvidedId() {
      Region region = new Region(TEST_ID);

      assertEquals(TEST_ID, region.getId());
    }

    @Test
    @DisplayName("Should create region with empty segment list when null provided")
    void createsRegionWithEmptySegmentListWhenNull() {
      Region region = new Region(TEST_ID, null);

      assertEquals(TEST_ID, region.getId());
      assertTrue(region.getSegmentList().isEmpty());
      assertEquals(0.0, region.getArea(), DELTA);
      assertNull(region.getAttributes());
      assertFalse(region.hasValidPixelCount());
    }

    @Test
    @DisplayName("Should create region with specified pixel count")
    void createsRegionWithPixelCount() {
      long expectedPixels = 100L;
      Region region = new Region(TEST_ID, null, expectedPixels);

      assertEquals(expectedPixels, region.getNumberOfPixels());
      assertEquals(expectedPixels, region.getArea(), DELTA);
      assertTrue(region.hasValidPixelCount());
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, -100})
    @DisplayName("Should not set invalid pixel counts")
    void doesNotSetInvalidPixelCounts(long invalidPixelCount) {
      Region region = new Region(TEST_ID, null, invalidPixelCount);

      assertFalse(region.hasValidPixelCount());
      assertEquals(-1L, region.getNumberOfPixels());
    }
  }

  @Nested
  @DisplayName("Segment Management Tests")
  class SegmentManagementTests {

    @Test
    @DisplayName("Should set and get segment list correctly")
    void setsAndGetsSegmentList() {
      List<Segment> segments = createTestSegments();
      Region region = new Region(TEST_ID);

      region.setSegmentList(segments);

      assertEquals(segments, region.getSegmentList());
      assertNotSame(segments, region.getSegmentList(), "Should create defensive copy");
    }

    @Test
    @DisplayName("Should handle null segment list gracefully")
    void handlesNullSegmentList() {
      Region region = new Region(TEST_ID);

      region.setSegmentList(null);

      assertTrue(region.getSegmentList().isEmpty());
      assertEquals(0.0, region.getArea(), DELTA);
    }

    @Test
    @DisplayName("Should set segment list with pixel count")
    void setsSegmentListWithPixelCount() {
      List<Segment> segments = createTestSegments();
      long pixelCount = 140L;
      Region region = new Region(TEST_ID);

      region.setSegmentList(segments, pixelCount);

      assertEquals(segments, region.getSegmentList());
      assertEquals(pixelCount, region.getNumberOfPixels());
      assertEquals(pixelCount, region.getArea(), DELTA);
    }

    private List<Segment> createTestSegments() {
      return Arrays.asList(
          new Segment(
              Arrays.asList(
                  new Point2D.Double(1, 2),
                  new Point2D.Double(21, 2),
                  new Point2D.Double(21, 12),
                  new Point2D.Double(1, 12))));
    }
  }

  @Nested
  @DisplayName("Area Calculation Tests")
  class AreaCalculationTests {

    @Test
    @DisplayName("Should calculate area with hierarchical segments")
    void calculatesAreaWithHierarchicalSegments() {
      // Create parent segment (outer rectangle): area = 200
      Segment parentSegment =
          new Segment(
              Arrays.asList(
                  new Point2D.Double(1, 2), // (1,2)
                  new Point2D.Double(21, 2), // (21,2)
                  new Point2D.Double(21, 12), // (21,12)
                  new Point2D.Double(1, 12) // (1,12)
                  )); // Area: 20 * 10 = 200

      // Create first child segment (hole): area = 60
      Segment child1 =
          new Segment(
              Arrays.asList(
                  new Point2D.Double(5, 4), // (5,4)
                  new Point2D.Double(15, 4), // (15,4)
                  new Point2D.Double(15, 10), // (15,10)
                  new Point2D.Double(5, 10) // (5,10)
                  )); // Area: 10 * 6 = 60

      // Create second child segment (island within hole): area = 10
      Segment child2 =
          new Segment(
              Arrays.asList(
                  new Point2D.Double(7, 6), // (7,6)
                  new Point2D.Double(12, 6), // (12,6)
                  new Point2D.Double(12, 8), // (12,8)
                  new Point2D.Double(7, 8) // (7,8)
                  )); // Area: 5 * 2 = 10

      // Build hierarchy: parent -> child1 -> child2
      parentSegment.addChild(child1);
      child1.addChild(child2);

      List<Segment> segments = Collections.singletonList(parentSegment);
      Region region = new Region(TEST_ID, segments);

      // Expected area: parent (200) - child1 (60) + child2 (10) = 150
      assertEquals(150.0, region.getArea(), DELTA, EXPECTED_AREA_MESSAGE);
    }

    @Test
    @DisplayName("Should return pixel count when available")
    void returnsPixelCountWhenAvailable() {
      List<Segment> segments = createSimpleSegment();
      long pixelCount = 140L;
      Region region = new Region(TEST_ID, segments, pixelCount);

      assertEquals(pixelCount, region.getArea(), DELTA);
    }

    @Test
    @DisplayName("Should calculate area for empty segments")
    void calculatesAreaForEmptySegments() {
      Region region = new Region(TEST_ID, Collections.emptyList());

      assertEquals(0.0, region.getArea(), DELTA);
    }

    private List<Segment> createSimpleSegment() {
      return Arrays.asList(
          new Segment(
              Arrays.asList(
                  new Point2D.Double(0, 0),
                  new Point2D.Double(10, 0),
                  new Point2D.Double(10, 10),
                  new Point2D.Double(0, 10))));
    }
  }

  @Nested
  @DisplayName("Attributes Management Tests")
  class AttributesManagementTests {

    @Test
    @DisplayName("Should set and get attributes")
    void setsAndGetsAttributes() {
      Region region = new Region(TEST_ID);
      RegionAttributes attributes = new RegionAttributes(1, "TestLabel");

      region.setAttributes(attributes);

      assertEquals(attributes, region.getAttributes());
    }

    @Test
    @DisplayName("Should handle null attributes")
    void handlesNullAttributes() {
      Region region = new Region(TEST_ID);

      region.setAttributes(null);

      assertNull(region.getAttributes());
    }
  }

  @Nested
  @DisplayName("Static Factory Methods Tests")
  class StaticFactoryMethodsTests {

    @Test
    @DisplayName("Should return empty list for null PlanarImage")
    void returnsEmptyListForNullPlanarImage() {
      List<Segment> segments = Region.buildSegmentList((PlanarImage) null, null);

      assertTrue(segments.isEmpty());
    }

    @Test
    @DisplayName("Should build segment list from binary image")
    void buildsSegmentListFromBinaryImage() {
      Mat source = createTestBinaryImage();

      List<Segment> segments = Region.buildSegmentList(ImageCV.toImageCV(source));
      Region region = new Region(TEST_ID, segments);

      assertFalse(segments.isEmpty(), "Should create segments from binary image");
      assertTrue(region.getArea() > 0, "Region should have positive area");
    }

    @Test
    @DisplayName("Should build segment list with offset")
    void buildsSegmentListWithOffset() {
      Mat source = createTestBinaryImage();
      Point offset = new Point(4, 3);

      List<Segment> segments = Region.buildSegmentList(ImageCV.toImageCV(source), offset);
      Region region = new Region(TEST_ID, segments);

      assertFalse(segments.isEmpty());
      // Verify offset was applied by checking point coordinates
      Point2D firstPoint = region.getSegmentList().get(0).get(0);
      assertTrue(firstPoint.getX() >= offset.x, "X coordinate should be offset");
      assertTrue(firstPoint.getY() >= offset.y, "Y coordinate should be offset");
    }

    @Test
    @DisplayName("Should build segment list from MatOfPoint2f")
    void buildsSegmentListFromMatOfPoint2f() {
      Point[] points = createTestPoints();
      MatOfPoint2f pt2f = new MatOfPoint2f(points);
      Mat hierarchy = createTestHierarchy();

      List<Segment> segments = Region.buildSegmentListFromFloat(List.of(pt2f), hierarchy);

      assertFalse(segments.isEmpty(), "Should create segments from MatOfPoint2f");
      assertEquals(points[0].x, segments.get(0).get(0).getX(), DELTA);
      assertEquals(points[0].y, segments.get(0).get(0).getY(), DELTA);
    }

    @Test
    @DisplayName("Should handle empty contour list")
    void handlesEmptyContourList() {
      Mat hierarchy = createTestHierarchy();

      List<Segment> segments = Region.buildSegmentList(Collections.emptyList(), hierarchy);

      assertTrue(segments.isEmpty());
    }

    @Test
    @DisplayName("Should handle null hierarchy")
    void handlesNullHierarchy() {
      MatOfPoint contour = new MatOfPoint(createTestPoints());

      List<Segment> segments = Region.buildSegmentList(List.of(contour), null);

      assertTrue(segments.isEmpty());
    }

    private Mat createTestBinaryImage() {
      Mat source = Mat.zeros(10, 20, CvType.CV_8UC1);

      // Create outer rectangle
      Point[] outerPoints = {
        new Point(4, 3), new Point(13, 3), new Point(13, 8), new Point(4, 8), new Point(4, 3)
      };
      Imgproc.fillPoly(
          source, Collections.singletonList(new MatOfPoint(outerPoints)), new Scalar(255));

      // Create inner rectangle (hole)
      Point[] innerPoints = {
        new Point(6, 5), new Point(10, 5), new Point(10, 6), new Point(6, 6), new Point(6, 5)
      };
      Imgproc.fillPoly(
          source, Collections.singletonList(new MatOfPoint(innerPoints)), new Scalar(0));

      return source;
    }

    private Point[] createTestPoints() {
      return new Point[] {
        new Point(6, 5), new Point(10, 5), new Point(10, 6), new Point(6, 6), new Point(6, 5)
      };
    }

    private Point[] createDoubleTestPoints() {
      return new Point[] {
        new Point(6.3, 5.2), new Point(10.7, 5.9), new Point(10.99, 6.1), new Point(6.5, 6.8)
      };
    }

    private Mat createTestHierarchy() {
      Mat hierarchy = new Mat(1, 4, CvType.CV_32SC1);
      int[] hierarchyData = {-1, -1, -1, -1}; // No parent, no siblings
      hierarchy.put(0, 0, hierarchyData);
      return hierarchy;
    }
  }

  @Nested
  @DisplayName("Object Behavior Tests")
  class ObjectBehaviorTests {

    @Test
    @DisplayName("Should implement equals correctly")
    void implementsEqualsCorrectly() {
      List<Segment> segments = createSimpleSegment();
      Region region1 = new Region(TEST_ID, segments, 100);
      Region region2 = new Region(TEST_ID, segments, 100);
      Region region3 = new Region("different", segments, 100);

      assertEquals(region1, region2, "Regions with same properties should be equal");
      assertNotEquals(region1, region3, "Regions with different IDs should not be equal");
      assertNotEquals(region1, null, "Region should not equal null");
      assertEquals(region1, region1, "Region should equal itself");
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void implementsHashCodeCorrectly() {
      List<Segment> segments = createSimpleSegment();
      Region region1 = new Region(TEST_ID, segments, 100);
      Region region2 = new Region(TEST_ID, segments, 100);

      assertEquals(
          region1.hashCode(), region2.hashCode(), "Equal regions should have same hash code");
    }

    @Test
    @DisplayName("Should implement toString meaningfully")
    void implementsToStringMeaningfully() {
      Region region = new Region(TEST_ID, createSimpleSegment(), 100);
      region.setAttributes(new RegionAttributes(1, "TestLabel"));

      String toString = region.toString();

      assertAll(
          "toString should contain key information",
          () -> assertTrue(toString.contains(TEST_ID), "Should contain ID"),
          () -> assertTrue(toString.contains("100"), "Should contain pixel count"),
          () -> assertTrue(toString.contains("true"), "Should indicate attributes presence"));
    }

    private List<Segment> createSimpleSegment() {
      return Arrays.asList(
          new Segment(
              Arrays.asList(
                  new Point2D.Double(0, 0),
                  new Point2D.Double(1, 0),
                  new Point2D.Double(1, 1),
                  new Point2D.Double(0, 1))));
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should handle complex real-world scenario")
    void handlesComplexRealWorldScenario() {
      // Create a complex binary image with multiple regions and holes
      Mat source = Mat.zeros(50, 50, CvType.CV_8UC1);

      // Large outer region
      Point[] outer = {
        new Point(5, 5), new Point(45, 5), new Point(45, 45), new Point(5, 45), new Point(5, 5)
      };
      Imgproc.fillPoly(source, Collections.singletonList(new MatOfPoint(outer)), new Scalar(255));

      // Inner hole
      Point[] hole = {
        new Point(15, 15),
        new Point(35, 15),
        new Point(35, 35),
        new Point(15, 35),
        new Point(15, 15)
      };
      Imgproc.fillPoly(source, Collections.singletonList(new MatOfPoint(hole)), new Scalar(0));

      // Small island in the hole
      Point[] island = {
        new Point(22, 22),
        new Point(28, 22),
        new Point(28, 28),
        new Point(22, 28),
        new Point(22, 22)
      };
      Imgproc.fillPoly(source, Collections.singletonList(new MatOfPoint(island)), new Scalar(255));

      List<Segment> segments = Region.buildSegmentList(ImageCV.toImageCV(source));
      Region region = new Region("complexRegion", segments);
      RegionAttributes attributes = new RegionAttributes(1, "ComplexRegion");
      region.setAttributes(attributes);

      assertAll(
          "Complex region should be handled correctly",
          () -> assertFalse(segments.isEmpty(), "Should detect segments"),
          () -> assertTrue(region.getArea() > 0, "Should have positive area"),
          () -> assertNotNull(region.getAttributes(), "Should have attributes"),
          () -> assertEquals("ComplexRegion", region.getAttributes().getLabel()));

      // Verify pixel count vs calculated area difference
      int actualPixels = Core.countNonZero(source);
      double calculatedArea = region.getArea();

      // The difference should be reasonable (polygonal approximation vs pixel count)
      double difference = Math.abs(actualPixels - calculatedArea);
      assertTrue(
          difference < actualPixels * 0.2, // Allow 20% difference
          "Calculated area should be reasonably close to actual pixel count");
    }
  }
}
