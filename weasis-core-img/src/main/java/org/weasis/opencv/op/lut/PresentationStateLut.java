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

import java.util.Optional;
import org.weasis.opencv.data.LookupTableCV;

/**
 * Interface representing DICOM Presentation State lookup table configuration.
 *
 * <p>This interface encapsulates the presentation state parameters defined in DICOM Presentation
 * State (PR) objects, which control how medical images are displayed through lookup table
 * transformations.
 *
 * <p>The presentation state LUT defines:
 *
 * <ul>
 *   <li>Custom lookup table data for pixel value transformation
 *   <li>Human-readable explanation of the transformation purpose
 *   <li>Shape mode indicating the mathematical function used
 * </ul>
 *
 * @author Weasis Team
 * @see LookupTableCV
 * @see LutShape
 */
public interface PresentationStateLut {

  /**
   * Retrieves the presentation state lookup table for pixel value transformation.
   *
   * <p>This lookup table, when present, defines a custom mapping from input pixel values to output
   * display values. It is typically used for specialized medical image visualization requirements
   * defined in DICOM Presentation State objects.
   *
   * @return an {@link Optional} containing the lookup table if available, or {@link
   *     Optional#empty()} if no custom LUT is defined
   */
  Optional<LookupTableCV> getPrLut();

  /**
   * Retrieves the human-readable explanation of the presentation state LUT purpose.
   *
   * <p>This explanation provides context about why this particular lookup table transformation is
   * being applied, such as "Chest X-Ray Enhancement" or "Bone Window Display".
   *
   * @return an {@link Optional} containing the LUT explanation if available, or {@link
   *     Optional#empty()} if no explanation is provided
   */
  Optional<String> getPrLutExplanation();

  /**
   * Retrieves the shape mode identifier for the presentation state LUT.
   *
   * <p>The shape mode indicates the mathematical function or transformation type applied by this
   * LUT (e.g., "LINEAR", "SIGMOID"). This corresponds to DICOM tag (2050,0020) LUT Function.
   *
   * @return an {@link Optional} containing the LUT shape mode if available, or {@link
   *     Optional#empty()} if no shape mode is specified
   */
  Optional<String> getPrLutShapeMode();
}
