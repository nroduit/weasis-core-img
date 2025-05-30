/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op.lut;

import org.weasis.core.util.annotations.Generated;

/**
 * Represents a default implementation of the {@code WlPresentation} interface. This class provides
 * functionality for managing window level presentation states with specific properties including
 * pixel padding and a presentation state LUT.
 */
@Generated
public class DefaultWlPresentation implements WlPresentation {

  private final boolean pixelPadding;
  private final PresentationStateLut dcmPR;

  public DefaultWlPresentation(PresentationStateLut dcmPR, boolean pixelPadding) {
    this.dcmPR = dcmPR;
    this.pixelPadding = pixelPadding;
  }

  @Override
  public boolean isPixelPadding() {
    return pixelPadding;
  }

  @Override
  public PresentationStateLut getPresentationState() {
    return dcmPR;
  }
}
