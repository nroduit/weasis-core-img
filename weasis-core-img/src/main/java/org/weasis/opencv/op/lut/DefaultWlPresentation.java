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
 * Default immutable record implementation of the {@link WlPresentation} interface.
 *
 * <p>This record provides a concrete implementation for managing window/level presentation
 * parameters in medical image processing. It encapsulates:
 *
 * <ul>
 *   <li>Pixel padding behavior configuration
 *   <li>DICOM Presentation State lookup table information
 * </ul>
 *
 * <p>As a record, this class is inherently immutable and thread-safe, making it suitable for
 * concurrent image processing operations. All instances are created through constructor parameters
 * and cannot be modified after instantiation.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * PresentationStateLut prLut = // ... obtain presentation state LUT
 * DefaultWlPresentation presentation = new DefaultWlPresentation(prLut, true);
 *
 * if (presentation.isPixelPadding()) {
 *     // Apply pixel padding during transformation
 * }
 * }</pre>
 *
 * @param presentationState the DICOM presentation state LUT configuration, may be {@code null} if
 *     no presentation state is available
 * @param pixelPadding {@code true} to enable pixel padding during image processing, {@code false}
 *     to disable it
 * @author Weasis Team
 * @see WlPresentation
 * @see PresentationStateLut
 */
@Generated
public record DefaultWlPresentation(PresentationStateLut presentationState, boolean pixelPadding)
    implements WlPresentation {

  @Override
  public boolean isPixelPadding() {
    return pixelPadding;
  }

  @Override
  public PresentationStateLut getPresentationState() {
    return presentationState;
  }
}
