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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.weasis.core.util.StringUtil;
import org.weasis.opencv.data.PlanarImage;

public class Region {
  private final String id;
  protected int numberOfPixels;
  protected List<Segment> segmentList;

  protected RegionAttributes attributes;

  public Region(String id) {
    this(id, null);
  }

  public Region(String id, List<Segment> segmentList) {
    this(id, segmentList, -1);
  }

  public Region(String id, List<Segment> segmentList, int numberOfPixels) {
    this.id = StringUtil.hasText(id) ? id : UUID.randomUUID().toString();
    setSegmentList(segmentList, numberOfPixels);
  }

  public String getId() {
    return id;
  }

  public List<Segment> getSegmentList() {
    return segmentList;
  }

  public void setSegmentList(List<Segment> segmentList) {
    setSegmentList(segmentList, -1);
  }

  public void setSegmentList(List<Segment> segmentList, int numberOfPixels) {
    this.segmentList = segmentList == null ? new ArrayList<>() : segmentList;
    this.numberOfPixels = numberOfPixels;
  }

  public RegionAttributes getAttributes() {
    return attributes;
  }

  public void setAttributes(RegionAttributes attributes) {
    this.attributes = attributes;
  }

  public int getNumberOfPixels() {
    return numberOfPixels;
  }

  public static List<Segment> buildSegmentList(PlanarImage binary) {
    if (binary == null) {
      return Collections.emptyList();
    }
    List<MatOfPoint> contours = new ArrayList<>();
    Mat hierarchy = new Mat();
    Imgproc.findContours(
        binary.toMat(), contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
    return buildSegmentList(contours, hierarchy);
  }

  public static List<Segment> buildSegmentList(List<MatOfPoint> contours, Mat hierarchy) {
    if (contours == null || hierarchy == null) {
      return Collections.emptyList();
    }
    Map<Integer, ContourTopology> contourMap = new HashMap<>();
    int[] hierarchyData = new int[4];
    for (int i = 0; i < contours.size(); i++) {
      hierarchy.get(0, i, hierarchyData);
      ContourTopology contourTopology = new ContourTopology(contours.get(i), hierarchyData[3]);
      contourMap.put(i, contourTopology);
    }

    List<Segment> segmentList = new ArrayList<>();
    for (int i = 0; i < contours.size(); i++) {
      Segment segment = buildSegment(contourMap, i);
      if (segment != null) {
        segmentList.add(segment);
      }
    }
    return segmentList;
  }

  protected static Segment buildSegment(Map<Integer, ContourTopology> contourMap, int index) {
    if (contourMap == null) {
      return null;
    }
    ContourTopology contourTopology = contourMap.get(index);
    if (contourTopology != null) {
      int parent = contourTopology.getParent();
      if (parent >= 0) {
        ContourTopology p = contourMap.get(parent);
        if (p != null) {
          p.getSegment().addChild(contourTopology.getSegment());
        }
        return null;
      }
      return contourTopology.getSegment();
    }
    return null;
  }
}
