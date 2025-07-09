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

/**
 * Interface representing window/level presentation parameters.
 *
 * <p>This interface defines the core presentation state for image display, including pixel padding
 * behavior and presentation state LUT information.
 *
 * @author Weasis Team
 */
public interface WlPresentation {

  /**
   * Determines whether pixel padding should be applied during image processing.
   *
   * @return {@code true} if pixel padding is enabled, {@code false} otherwise
   */
  boolean isPixelPadding();

  /**
   * Retrieves the presentation state lookup table configuration.
   *
   * @return the presentation state LUT, or {@code null} if not available
   */
  PresentationStateLut getPresentationState();
}
