/*
 * Copyright (c) 2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.seg;

public final class SegmentCategory {

  private final int id;
  private String label;
  private String description;
  private String type;

  public SegmentCategory(int id, String label, String description, String type) {
    this.id = id;
    this.label = label;
    this.description = description;
    this.type = type;
  }

  public String label() {
    return label;
  }

  public String description() {
    return description;
  }

  public String type() {
    return type;
  }

  public int getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
