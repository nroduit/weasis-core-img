/*
 * Copyright (c) 2025 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.data;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;

/** Parses image metadata extracted by OpenCV's imreadWithMetadata. */
public final class MetadataParser {

  private MetadataParser() {}

  // Future work: extract XMP (Imgcodecs.IMAGE_METADATA_XMP, UTF-8 packet) and ICC profile
  // (Imgcodecs.IMAGE_METADATA_ICCP, raw profile bytes). Both are 1xN CV_8U Mats located by
  // matching their type in metadataTypes, since entries are only present when non-empty.

  /**
   * Parses EXIF metadata from OpenCV imreadWithMetadata results.
   *
   * @param metadataList the metadata matrices returned by imreadWithMetadata
   * @param metadataTypes the metadata type indicators
   * @return list of parsed EXIF tag values as strings
   */
  public static List<String> parseExifParseMetadata(
      List<Mat> metadataList, MatOfInt metadataTypes) {
    if (metadataList == null || metadataTypes == null || metadataTypes.empty()) {
      return List.of();
    }

    int[] typesArray = metadataTypes.toArray();
    if (metadataList.size() != typesArray.length || typesArray[typesArray.length - 1] != 1000) {
      return List.of();
    }

    Mat metadata = metadataList.get(typesArray.length - 1);
    if (metadata.empty()) {
      return List.of();
    }

    int numTags = metadata.rows();
    var result = new ArrayList<String>(numTags);
    for (int i = 0; i < numTags; i++) {
      var row = metadata.row(i);
      try {
        result.add(parseTagRow(row));
      } finally {
        row.release();
      }
    }
    return result;
  }

  private static String parseTagRow(Mat row) {
    if (row.empty()) {
      return "";
    }
    var tagBytes = new byte[(int) (row.total() * row.elemSize())];
    row.get(0, 0, tagBytes);
    return new String(tagBytes, StandardCharsets.UTF_8).trim();
  }
}
