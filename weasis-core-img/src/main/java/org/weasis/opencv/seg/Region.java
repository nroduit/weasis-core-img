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
import java.util.Collections;
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

public class Region {
  private static final long UNINITIALIZED_PIXEL_COUNT = -1L;
  private static final int HIERARCHY_PARENT_INDEX = 3;

  private final String id;
  protected long numberOfPixels;
  protected List<Segment> segmentList;

  protected RegionAttributes attributes;

  /**
   * Constructs a Region with a unique identifier. If the ID is null or empty, it generates a new
   * UUID as the ID.
   *
   * @param id the unique identifier for this region
   */
  public Region(String id) {
    this(id, null);
  }

  /**
   * Constructs a Region with a unique identifier and an empty segment list. If the ID is null or
   * empty, it generates a new UUID as the ID.
   *
   * @param id the unique identifier for this region
   */
  public Region(String id, List<Segment> segmentList) {
    this(id, segmentList, UNINITIALIZED_PIXEL_COUNT);
  }

  /**
   * Constructs a Region with a unique identifier, a list of segments, and the number of pixels. If
   * the ID is null or empty, it generates a new UUID as the ID.
   *
   * @param id the unique identifier for this region
   * @param segmentList the list of segments that make up this region
   * @param numberOfPixels the number of pixels in this region
   */
  public Region(String id, List<Segment> segmentList, long numberOfPixels) {
    this.id = generateOrValidateId(id);
    setSegmentList(segmentList, numberOfPixels);
  }

  private static String generateOrValidateId(String id) {
    return StringUtil.hasText(id) ? id.trim() : UUID.randomUUID().toString();
  }

  /**
   * Returns the unique identifier for this region. If the ID is not set, it generates a new UUID.
   *
   * @return the unique identifier for this region
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the list of segments for this region. If the segment list is not initialized, it
   * returns an empty list.
   *
   * @return the list of segments for this region
   */
  public List<Segment> getSegmentList() {
    return segmentList != null ? segmentList : Collections.emptyList();
  }

  /**
   * Sets the segment list for this region and initializes the number of pixels to
   * UNINITIALIZED_PIXEL_COUNT.
   *
   * @param segmentList the list of segments to set for this region
   */
  public void setSegmentList(List<Segment> segmentList) {
    setSegmentList(segmentList, UNINITIALIZED_PIXEL_COUNT);
  }

  /**
   * Sets the segment list for this region and initializes the number of pixels. If the segment list
   * is null, it initializes an empty list. If the number of pixels is less than or equal to zero,
   * it sets it to UNINITIALIZED_PIXEL_COUNT.
   *
   * @param segmentList the list of segments to set for this region
   * @param numberOfPixels the number of pixels in the region
   */
  public void setSegmentList(List<Segment> segmentList, long numberOfPixels) {
    this.segmentList = segmentList != null ? new ArrayList<>(segmentList) : new ArrayList<>();
    this.numberOfPixels = numberOfPixels > 0 ? numberOfPixels : UNINITIALIZED_PIXEL_COUNT;
  }

  /**
   * Returns the attributes of this region. If the attributes are not set, it returns null.
   *
   * @return the attributes of this region
   */
  public RegionAttributes getAttributes() {
    return attributes;
  }

  /**
   * Sets the attributes for this region. If the attributes are null, it will not change the current
   * attributes.
   *
   * @param attributes the attributes to set for this region
   */
  public void setAttributes(RegionAttributes attributes) {
    this.attributes = attributes;
  }

  /**
   * Returns the number of pixels in the region. If the pixel count is initialized, it returns that
   * value. Otherwise, it returns -1 to indicate that the pixel count is not set.
   *
   * @return the number of pixels in the region
   */
  public long getNumberOfPixels() {
    return numberOfPixels;
  }

  /**
   * Checks if this region has a valid pixel count. A valid pixel count is any value greater than
   * UNINITIALIZED_PIXEL_COUNT (-1).
   *
   * @return true if the region has a valid pixel count, false otherwise
   */
  public boolean hasValidPixelCount() {
    return numberOfPixels > UNINITIALIZED_PIXEL_COUNT;
  }

  // Static factory methods for building segments from binary images
  /**
   * Builds a segment list from a binary image. If the image is null, it returns an empty list.
   *
   * @param binary the binary image from which to extract segments
   * @return a list of segments extracted from the binary image
   */
  public static List<Segment> buildSegmentList(PlanarImage binary) {
    return buildSegmentList(binary, null);
  }

  /**
   * Builds a segment list from a binary image. If the image is null, it returns an empty list.
   * Optionally, an offset can be provided to adjust the contour coordinates.
   *
   * @param binary the binary image from which to extract segments
   * @param offset an optional offset to apply to the contour coordinates
   * @return a list of segments extracted from the binary image
   */
  public static List<Segment> buildSegmentList(PlanarImage binary, Point offset) {
    if (binary == null) {
      return Collections.emptyList();
    }
    List<MatOfPoint> contours = new ArrayList<>();
    Mat hierarchy = new Mat();
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

  /**
   * Builds a segment list from contours with hierarchical relationships. This method handles both
   * MatOfPoint and MatOfPoint2f contours.
   */
  public static List<Segment> buildSegmentListFromFloat(
      List<MatOfPoint2f> contours, Mat hierarchy) {
    return buildSegmentListFromContours(contours, hierarchy);
  }

  /**
   * Builds a segment list from contours with hierarchical relationships. This method handles both
   * MatOfPoint and MatOfPoint2f contours.
   */
  public static List<Segment> buildSegmentList(List<MatOfPoint> contours, Mat hierarchy) {
    return buildSegmentListFromContours(contours, hierarchy);
  }

  /**
   * Builds a segment list from contours with hierarchical relationships. This method handles both
   * MatOfPoint and MatOfPoint2f contours.
   */
  private static List<Segment> buildSegmentListFromContours(
      List<? extends Mat> contours, Mat hierarchy) {
    if (contours == null || hierarchy == null || contours.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Integer, ContourTopology> contourMap = createContourTopologyMap(contours, hierarchy);
    return extractRootSegments(contourMap, contours.size());
  }

  private static Map<Integer, ContourTopology> createContourTopologyMap(
      List<? extends Mat> contours, Mat hierarchy) {
    Map<Integer, ContourTopology> contourMap = new HashMap<>();
    int[] hierarchyData = new int[4];
    for (int i = 0; i < contours.size(); i++) {
      hierarchy.get(0, i, hierarchyData);
      ContourTopology topology =
          createContourTopology(contours.get(i), hierarchyData[HIERARCHY_PARENT_INDEX]);
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
    List<Segment> segmentList = new ArrayList<>();
    // Build parent-child relationships and collect root segments
    for (int i = 0; i < contourCount; i++) {
      Segment segment = buildSegmentWithChildren(contourMap, i);
      if (segment != null) {
        segmentList.add(segment);
      }
    }
    return segmentList;
  }

  private static Segment buildSegmentWithChildren(
      Map<Integer, ContourTopology> contourMap, int index) {
    ContourTopology contourTopology = contourMap.get(index);
    if (contourTopology == null) {
      return null;
    }
    int parentIndex = contourTopology.getParent();

    // If this contour has a parent, add it as a child and return null (not a root)
    if (parentIndex >= 0) {
      ContourTopology parent = contourMap.get(parentIndex);
      if (parent != null) {
        parent.getSegment().addChild(contourTopology.getSegment());
      }
      return null;
    }
    // This is a root segment
    return contourTopology.getSegment();
  }

  /**
   * Returns the area of the region. If the pixel count is initialized, it returns that value.
   * Otherwise, it calculates the area based on the segments.
   *
   * @return the area of the region
   */
  public double getArea() {
    if (hasValidPixelCount()) {
      return numberOfPixels;
    }
    return Math.round(calculateSegmentListArea(getSegmentList()));
  }

  private static double calculateSegmentListArea(List<Segment> segments) {
    return calculateArea(segments, 0);
  }

  private static double calculateArea(List<Segment> segments, int level) {
    if (segments == null || segments.isEmpty()) {
      return 0.0;
    }

    double totalArea = 0.0;
    for (Segment segment : segments) {
      // Alternating signs for holes (even levels are positive, odd levels are negative)
      double segmentArea = polygonArea(segment);
      totalArea += (level % 2 == 0) ? segmentArea : -segmentArea;

      // Recursively calculate area of children
      totalArea += calculateArea(segment.getChildren(), level + 1);
    }
    return totalArea;
  }

  /**
   * Calculate the area of a polygon using the shoelace formula.
   *
   * @param segment the polygon segment
   * @return the area which approximates the number of pixels inside the polygon
   */
  private static double polygonArea(Segment segment) {
    if (segment == null || segment.size() < 3) {
      return 0.0;
    }
    double area = 0.0;
    int vertexCount = segment.size();

    for (int i = 0; i < vertexCount; i++) {
      Point2D currentPoint = segment.get(i);
      Point2D nextPoint = segment.get((i + 1) % vertexCount);

      // Shoelace formula: sum of (x_i * y_{i+1} - x_{i+1} * y_i)
      area += currentPoint.getX() * nextPoint.getY() - nextPoint.getX() * currentPoint.getY();
    }
    return Math.abs(area) / 2.0;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Region other)) return false;

    return Objects.equals(id, other.id)
        && numberOfPixels == other.numberOfPixels
        && Objects.equals(segmentList, other.segmentList)
        && Objects.equals(attributes, other.attributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, numberOfPixels, segmentList, attributes);
  }

  @Override
  public String toString() {
    return String.format(
        "Region{id='%s', segments=%d, pixels=%d, hasAttributes=%s}",
        id, getSegmentList().size(), numberOfPixels, attributes != null);
  }
}
