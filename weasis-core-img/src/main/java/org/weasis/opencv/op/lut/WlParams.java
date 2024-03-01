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

public interface WlParams extends WlPresentation {

  double getWindow();

  double getLevel();

  double getLevelMin();

  double getLevelMax();

  boolean isInverseLut();

  boolean isFillOutsideLutRange();

  boolean isAllowWinLevelOnColorImage();

  LutShape getLutShape();
}
