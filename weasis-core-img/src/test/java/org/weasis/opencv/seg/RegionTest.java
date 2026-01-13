/*
 * Copyright (c) 2025 Weasis Team and other contributors.
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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.natives.NativeLibrary;

@DisplayNameGeneration(ReplaceUnderscores.class)
class RegionTest {

  private static final String TEST_ID = "test-region-123";
  private static final double AREA_TOLERANCE = 0.001;

  @BeforeAll
  static void setUp() {
    NativeLibrary.loadLibraryFromLibraryName();
  }

  @Nested
  class Constructor_Tests {

    @Test
    void should_generate_uuid_when_id_is_null() {
      var region = new Region(null);

      assertNotNull(region.getId());
      assertTrue(region.getId().length() > 10); // UUID format check
    }

    @Test
    void should_generate_uuid_when_id_is_blank() {
      var region = new Region("   ");

      assertNotNull(region.getId());
      assertTrue(region.getId().length() > 10);
    }

    @Test
    void should_use_provided_id_when_valid() {
      var region = new Region(TEST_ID);

      assertEquals(TEST_ID, region.getId());
    }

    @Test
    void should_trim_whitespace_from_id() {
      var region = new Region("  " + TEST_ID + "  ");

      assertEquals(TEST_ID, region.getId());
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, -100})
    void should_mark_invalid_pixel_counts_as_uninitialized(long invalidPixelCount) {
      var region = new Region(TEST_ID, null, invalidPixelCount);

      assertFalse(region.hasValidPixelCount());
      assertEquals(-1L, region.getNumberOfPixels());
    }

    @Test
    void should_initialize_with_valid_pixel_count() {
      var region = new Region(TEST_ID, List.of(), 150L);

      assertTrue(region.hasValidPixelCount());
      assertEquals(150L, region.getNumberOfPixels());
      assertEquals(150.0, region.getArea());
    }
  }

  @Nested
  class Segment_Management_Tests {

    @Test
    void should_return_immutable_copy_of_segments() {
      var segments = createRectangleSegment(0, 0, 10, 10);
      var region = new Region(TEST_ID, segments);

      var retrievedSegments = region.getSegmentList();

      assertEquals(segments.size(), retrievedSegments.size());
      assertThrows(UnsupportedOperationException.class, () -> retrievedSegments.add(new Segment()));
    }

    @Test
    void should_return_empty_list_when_segments_null() {
      var region = new Region(TEST_ID, null);

      assertTrue(region.getSegmentList().isEmpty());
    }

    @Test
    void should_reset_pixel_count_when_setting_segments_without_count() {
      var region = new Region(TEST_ID, List.of(), 100L);
      var newSegments = createRectangleSegment(0, 0, 5, 5);

      region.setSegmentList(newSegments);

      assertFalse(region.hasValidPixelCount());
    }
  }

  @Nested
  class Area_Calculation_Tests {

    @Test
    void should_return_pixel_count_when_available() {
      var segments = createRectangleSegment(0, 0, 10, 10);
      var region = new Region(TEST_ID, segments, 250L);

      assertEquals(250.0, region.getArea());
    }

    @Test
    void should_calculate_area_from_segments_when_no_pixel_count() {
      var segments = createRectangleSegment(0, 0, 10, 10);
      var region = new Region(TEST_ID, segments);

      assertEquals(100.0, region.getArea(), AREA_TOLERANCE);
    }

    @Test
    void should_calculate_zero_area_for_empty_segments() {
      var region = new Region(TEST_ID, List.of());

      assertEquals(0.0, region.getArea());
    }

    @ParameterizedTest
    @MethodSource("providePolygonTestCases")
    void should_calculate_correct_area_for_various_polygons(
        List<Point2D> points, double expectedArea) {
      var segment = new Segment(points);
      var region = new Region(TEST_ID, List.of(segment));

      assertEquals(expectedArea, region.getArea(), AREA_TOLERANCE);
    }

    static Stream<Arguments> providePolygonTestCases() {
      return Stream.of(
          // Unit square
          Arguments.of(
              List.of(
                  new Point2D.Double(0, 0),
                  new Point2D.Double(1, 0),
                  new Point2D.Double(1, 1),
                  new Point2D.Double(0, 1)),
              1.0),

          // Rectangle 5x3
          Arguments.of(
              List.of(
                  new Point2D.Double(0, 0),
                  new Point2D.Double(5, 0),
                  new Point2D.Double(5, 3),
                  new Point2D.Double(0, 3)),
              15.0),

          // Triangle
          Arguments.of(
              List.of(new Point2D.Double(0, 0), new Point2D.Double(4, 0), new Point2D.Double(2, 3)),
              6.0));
    }

    @Test
    void should_handle_hierarchical_segments_with_holes() {
      // Outer rectangle: 10x10 = 100
      var outerSegment =
          new Segment(
              List.of(
                  new Point2D.Double(0, 0),
                  new Point2D.Double(10, 0),
                  new Point2D.Double(10, 10),
                  new Point2D.Double(0, 10)));

      // Inner hole: 4x4 = 16 (subtracted)
      var holeSegment =
          new Segment(
              List.of(
                  new Point2D.Double(3, 3),
                  new Point2D.Double(7, 3),
                  new Point2D.Double(7, 7),
                  new Point2D.Double(3, 7)));

      // Island in hole: 2x2 = 4 (added back)
      var islandSegment =
          new Segment(
              List.of(
                  new Point2D.Double(4, 4),
                  new Point2D.Double(6, 4),
                  new Point2D.Double(6, 6),
                  new Point2D.Double(4, 6)));

      outerSegment.addChild(holeSegment);
      holeSegment.addChild(islandSegment);

      var region = new Region(TEST_ID, List.of(outerSegment));

      // Expected: 100 - 16 + 4 = 88
      assertEquals(88.0, region.getArea(), AREA_TOLERANCE);
    }
  }

  @Nested
  class Static_Factory_Method_Tests {

    @Test
    void should_return_empty_list_for_null_planar_image() {
      var segments = Region.buildSegmentList((PlanarImage) null);

      assertTrue(segments.isEmpty());
    }

    @Test
    void should_build_segments_from_binary_image() {
      var binaryImage = createSimpleBinaryImage();

      var segments = Region.buildSegmentList(binaryImage);

      assertFalse(segments.isEmpty());
      assertTrue(segments.get(0).size() >= 3);
    }

    @Test
    void should_apply_offset_to_contour_coordinates() {
      var binaryImage = createSimpleBinaryImage();
      var offset = new Point(10, 5);

      var segments = Region.buildSegmentList(binaryImage, offset);
      var firstPoint = segments.get(0).get(0);

      assertTrue(firstPoint.getX() >= offset.x);
      assertTrue(firstPoint.getY() >= offset.y);
    }

    @Test
    void should_build_segments_from_mat_of_point2f() {
      var contours = List.of(createMatOfPoint2f());
      var hierarchy = createBasicHierarchy();

      var segments = Region.buildSegmentListFromFloat(contours, hierarchy);

      assertFalse(segments.isEmpty());
      assertEquals(4, segments.get(0).size());
    }

    @Test
    void should_handle_empty_contour_lists_gracefully() {
      var hierarchy = createBasicHierarchy();

      var segments = Region.buildSegmentList(List.<MatOfPoint>of(), hierarchy);

      assertTrue(segments.isEmpty());
    }

    @Test
    void should_handle_null_hierarchy_gracefully() {
      var contours = List.of(createMatOfPoint());

      var segments = Region.buildSegmentList(contours, null);

      assertTrue(segments.isEmpty());
    }
  }

  @Nested
  class Attributes_Management_Tests {

    @Test
    void should_set_and_retrieve_attributes() {
      var region = new Region(TEST_ID);
      var attributes = new RegionAttributes(1, "Test Label");

      region.setAttributes(attributes);

      assertEquals(attributes, region.getAttributes());
    }

    @Test
    void should_accept_null_attributes() {
      var region = new Region(TEST_ID);

      region.setAttributes(null);

      assertNull(region.getAttributes());
    }
  }

  @Nested
  class Object_Behavior_Tests {

    @Test
    void should_implement_equals_correctly() {
      var segments = createRectangleSegment(0, 0, 5, 5);
      var attributes = new RegionAttributes(1, "Test");

      var region1 = new Region(TEST_ID, segments, 100L);
      region1.setAttributes(attributes);

      var region2 = new Region(TEST_ID, segments, 100L);
      region2.setAttributes(attributes);

      var region3 = new Region("different-id", segments, 100L);

      assertEquals(region1, region2);
      assertNotEquals(region1, region3);
      assertNotEquals(region1, null);
      assertEquals(region1, region1);
    }

    @Test
    void should_implement_hash_code_consistently() {
      var segments = createRectangleSegment(0, 0, 5, 5);
      var region1 = new Region(TEST_ID, segments, 100L);
      var region2 = new Region(TEST_ID, segments, 100L);

      assertEquals(region1.hashCode(), region2.hashCode());
    }

    @Test
    void should_provide_meaningful_string_representation() {
      var segments = createRectangleSegment(0, 0, 5, 5);
      var region = new Region(TEST_ID, segments, 100L);
      region.setAttributes(new RegionAttributes(1, "Test Label"));

      var toString = region.toString();

      assertAll(
          () -> assertTrue(toString.contains(TEST_ID)),
          () -> assertTrue(toString.contains("100")),
          () -> assertTrue(toString.contains("segments=1")),
          () -> assertTrue(toString.contains("hasAttributes=true")));
    }
  }

  @Nested
  class Integration_Tests {

    @Test
    void should_handle_complete_workflow() {
      // Create complex binary image
      var binaryImage = createComplexBinaryImage();

      // Extract segments
      var segments = Region.buildSegmentList(binaryImage);

      // Create region with attributes
      var region = new Region("workflow-test", segments);
      var attributes = new RegionAttributes(1, "Complex Region");
      region.setAttributes(attributes);

      assertAll(
          () -> assertFalse(segments.isEmpty()),
          () -> assertTrue(region.getArea() > 0),
          () -> assertEquals("Complex Region", region.getAttributes().getLabel()),
          () -> assertTrue(region.getId().startsWith("workflow")));
    }

    @Test
    void should_calculate_area_close_to_actual_pixel_count() {
      var binaryImage = createKnownAreaBinaryImage();
      var actualPixelCount = Core.countNonZero(binaryImage.toMat());

      var segments = Region.buildSegmentList(binaryImage);
      var region = new Region("pixel-test", segments);

      var calculatedArea = region.getArea();
      var difference = Math.abs(actualPixelCount - calculatedArea);

      // Allow reasonable approximation error (polygonal vs pixel)
      assertTrue(
          difference < actualPixelCount * 0.15,
          "Calculated area should approximate pixel count within 15%");
    }
  }

  // Helper methods for creating test data

  private static List<Segment> createRectangleSegment(
      double x, double y, double width, double height) {
    var segment =
        new Segment(
            List.of(
                new Point2D.Double(x, y),
                new Point2D.Double(x + width, y),
                new Point2D.Double(x + width, y + height),
                new Point2D.Double(x, y + height)));
    return List.of(segment);
  }

  private static PlanarImage createSimpleBinaryImage() {
    var mat = Mat.zeros(20, 20, CvType.CV_8UC1);
    var rectangle = new org.opencv.core.Rect(5, 5, 10, 8);
    Imgproc.rectangle(mat, rectangle, new Scalar(255), -1);
    return ImageCV.fromMat(mat);
  }

  private static PlanarImage createComplexBinaryImage() {
    var mat = Mat.zeros(50, 50, CvType.CV_8UC1);

    // Outer shape
    var outerPoints =
        new Point[] {
          new Point(10, 10), new Point(40, 10),
          new Point(40, 40), new Point(10, 40)
        };
    Imgproc.fillPoly(mat, List.of(new MatOfPoint(outerPoints)), new Scalar(255));

    // Inner hole
    var holePoints =
        new Point[] {
          new Point(20, 20), new Point(30, 20),
          new Point(30, 30), new Point(20, 30)
        };
    Imgproc.fillPoly(mat, List.of(new MatOfPoint(holePoints)), new Scalar(0));

    return ImageCV.fromMat(mat);
  }

  private static PlanarImage createKnownAreaBinaryImage() {
    var mat = Mat.zeros(30, 30, CvType.CV_8UC1);
    // Create a filled circle with known approximate area
    Imgproc.circle(mat, new Point(15, 15), 10, new Scalar(255), -1);
    return ImageCV.fromMat(mat);
  }

  private static MatOfPoint createMatOfPoint() {
    return new MatOfPoint(
        new Point(0, 0), new Point(10, 0),
        new Point(10, 10), new Point(0, 10));
  }

  private static MatOfPoint2f createMatOfPoint2f() {
    return new MatOfPoint2f(
        new Point(0.5, 0.5), new Point(10.5, 0.5),
        new Point(10.5, 10.5), new Point(0.5, 10.5));
  }

  private static Mat createBasicHierarchy() {
    var hierarchy = new Mat(1, 4, CvType.CV_32SC1);
    hierarchy.put(0, 0, new int[] {-1, -1, -1, -1});
    return hierarchy;
  }
}
