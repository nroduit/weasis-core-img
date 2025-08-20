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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.weasis.core.util.StringUtil;
import org.weasis.opencv.data.PlanarImage;

/**
 * Represents a segmentation region containing polygonal segments with hierarchical relationships.
 * Each region has a unique identifier, a list of segments, and optional attributes for
 * visualization.
 *
 * <p>The region can calculate its area using either pre-computed pixel counts or by calculating the
 * area of its constituent segments using the shoelace formula.
 */
public class Region {
  private static final long UNINITIALIZED_PIXEL_COUNT = -1L;
  private static final int HIERARCHY_PARENT_INDEX = 3;

  private final String id;
  protected long numberOfPixels;
  protected List<Segment> segmentList;
  protected RegionAttributes attributes;

  /**
   * Creates a new region with the specified identifier.
   *
   * @param id the unique identifier, or null to generate a UUID
   */
  public Region(String id) {
    this(id, null);
  }

  /**
   * Creates a new region with the specified identifier and segments.
   *
   * @param id the unique identifier, or null to generate a UUID
   * @param segmentList the segments that make up this region
   */
  public Region(String id, List<Segment> segmentList) {
    this(id, segmentList, UNINITIALIZED_PIXEL_COUNT);
  }

  /**
   * Creates a new region with the specified identifier, segments, and pixel count.
   *
   * @param id the unique identifier, or null to generate a UUID
   * @param segmentList the segments that make up this region
   * @param numberOfPixels the pre-computed number of pixels in this region
   */
  public Region(String id, List<Segment> segmentList, long numberOfPixels) {
    this.id = generateOrValidateId(id);
    setSegmentList(segmentList, numberOfPixels);
  }

  private static String generateOrValidateId(String id) {
    return StringUtil.hasText(id) ? id.trim() : UUID.randomUUID().toString();
  }

  public String getId() {
    return id;
  }

  /**
   * Returns an immutable view of the segments in this region.
   *
   * @return the segments, never null
   */
  public List<Segment> getSegmentList() {
    return segmentList != null ? List.copyOf(segmentList) : List.of();
  }

  public void setSegmentList(List<Segment> segmentList) {
    setSegmentList(segmentList, UNINITIALIZED_PIXEL_COUNT);
  }

  /**
   * Sets the segments and pixel count for this region.
   *
   * @param segmentList the segments to set
   * @param numberOfPixels the pixel count, or negative to mark as uninitialized
   */
  public void setSegmentList(List<Segment> segmentList, long numberOfPixels) {
    this.segmentList = segmentList != null ? new ArrayList<>(segmentList) : new ArrayList<>();
    this.numberOfPixels = numberOfPixels > 0 ? numberOfPixels : UNINITIALIZED_PIXEL_COUNT;
  }

  public RegionAttributes getAttributes() {
    return attributes;
  }

  public void setAttributes(RegionAttributes attributes) {
    this.attributes = attributes;
  }

  public long getNumberOfPixels() {
    return numberOfPixels;
  }

  public boolean hasValidPixelCount() {
    return numberOfPixels > UNINITIALIZED_PIXEL_COUNT;
  }

  // Static factory methods for building segments from binary images
  /**
   * Extracts segments from a binary image using OpenCV contour detection.
   *
   * @param binary the binary image to process
   * @return list of extracted segments, empty if image is null
   */
  public static List<Segment> buildSegmentList(PlanarImage binary) {
    return buildSegmentList(binary, null);
  }

  /**
   * Extracts segments from a binary image with coordinate offset adjustment.
   *
   * @param binary the binary image to process
   * @param offset coordinate offset to apply to contours
   * @return list of extracted segments, empty if image is null
   */
  public static List<Segment> buildSegmentList(PlanarImage binary, Point offset) {
    if (binary == null) {
      return List.of();
    }
    var contours = new ArrayList<MatOfPoint>();
    var hierarchy = new Mat();
    findContours(binary, contours, hierarchy, offset);
    return buildSegmentList(contours, hierarchy);
  }

  private static void findContours(
      PlanarImage binary, List<MatOfPoint> contours, Mat hierarchy, Point offset) {
    if (offset == null) {
      Imgproc.findContours(
          binary.toMat(), contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
    } else {
      Imgproc.findContours(
          binary.toMat(),
          contours,
          hierarchy,
          Imgproc.RETR_TREE,
          Imgproc.CHAIN_APPROX_SIMPLE,
          offset);
    }
  }

  /** Builds segments from MatOfPoint2f contours with hierarchical relationships. */
  public static List<Segment> buildSegmentListFromFloat(
      List<MatOfPoint2f> contours, Mat hierarchy) {
    return buildSegmentListFromContours(contours, hierarchy);
  }

  /** Builds segments from MatOfPoint contours with hierarchical relationships. */
  public static List<Segment> buildSegmentList(List<MatOfPoint> contours, Mat hierarchy) {
    return buildSegmentListFromContours(contours, hierarchy);
  }

  private static List<Segment> buildSegmentListFromContours(
      List<? extends Mat> contours, Mat hierarchy) {
    if (contours == null || hierarchy == null || contours.isEmpty()) {
      return List.of();
    }
    var contourMap = createContourTopologyMap(contours, hierarchy);
    return extractRootSegments(contourMap, contours.size());
  }

  private static Map<Integer, ContourTopology> createContourTopologyMap(
      List<? extends Mat> contours, Mat hierarchy) {
    var contourMap = new HashMap<Integer, ContourTopology>();
    var hierarchyData = new int[4];
    for (int i = 0; i < contours.size(); i++) {
      hierarchy.get(0, i, hierarchyData);
      var topology = createContourTopology(contours.get(i), hierarchyData[HIERARCHY_PARENT_INDEX]);
      if (topology != null) {
        contourMap.put(i, topology);
      }
    }
    return contourMap;
  }

  private static ContourTopology createContourTopology(Mat contour, int parentIndex) {
    if (contour instanceof MatOfPoint matOfPoint) {
      return new ContourTopology(matOfPoint, parentIndex);
    } else if (contour instanceof MatOfPoint2f matOfPoint2f) {
      return new ContourTopology(matOfPoint2f, parentIndex);
    }
    return null;
  }

  private static List<Segment> extractRootSegments(
      Map<Integer, ContourTopology> contourMap, int contourCount) {
    var segmentList = new ArrayList<Segment>();
    for (int i = 0; i < contourCount; i++) {
      var segment = buildSegmentWithChildren(contourMap, i);
      if (segment != null) {
        segmentList.add(segment);
      }
    }
    return segmentList;
  }

  private static Segment buildSegmentWithChildren(
      Map<Integer, ContourTopology> contourMap, int index) {
    var contourTopology = contourMap.get(index);
    if (contourTopology == null) {
      return null;
    }
    int parentIndex = contourTopology.getParent();

    if (parentIndex >= 0) {
      var parent = contourMap.get(parentIndex);
      if (parent != null) {
        parent.getSegment().addChild(contourTopology.getSegment());
      }
      return null; // Not a root segment
    }
    return contourTopology.getSegment(); // Root segment
  }

  /**
   * Calculates the area of this region. Uses pre-computed pixel count if available, otherwise
   * computes area from segments using the shoelace formula.
   *
   * @return the area in pixels
   */
  public double getArea() {
    return hasValidPixelCount() ? numberOfPixels : Math.round(calculateArea(segmentList, 0));
  }

  private static double calculateArea(List<Segment> segments, int level) {
    if (segments.isEmpty()) {
      return 0.0;
    }

    double totalArea = 0.0;
    for (var segment : segments) {
      double segmentArea = polygonArea(segment);
      // Alternate signs for holes: positive for even levels, negative for odd levels
      totalArea += (level % 2 == 0) ? segmentArea : -segmentArea;
      totalArea += calculateArea(segment.children, level + 1);
    }
    return totalArea;
  }

  private static double polygonArea(Segment segment) {
    if (segment == null || segment.size() < 3) {
      return 0.0;
    }
    double area = 0.0;
    int vertexCount = segment.size();

    for (int i = 0; i < vertexCount; i++) {
      Point2D current = segment.get(i);
      Point2D next = segment.get((i + 1) % vertexCount);

      // Shoelace formula: sum of (x_i * y_{i+1} - x_{i+1} * y_i)
      area += current.getX() * next.getY() - next.getX() * current.getY();
    }
    return Math.abs(area) / 2.0;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || (obj instanceof Region other
            && Objects.equals(id, other.id)
            && numberOfPixels == other.numberOfPixels
            && Objects.equals(segmentList, other.segmentList)
            && Objects.equals(attributes, other.attributes));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, numberOfPixels, segmentList, attributes);
  }

  @Override
  public String toString() {
    return "Region{id='%s', segments=%d, pixels=%d, hasAttributes=%s}"
        .formatted(id, segmentList.size(), numberOfPixels, attributes != null);
  }
}
