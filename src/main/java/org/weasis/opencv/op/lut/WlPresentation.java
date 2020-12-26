/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.weasis.opencv.op.lut;

public interface WlPresentation {

  boolean isPixelPadding();

  PresentationStateLut getPresentationState();
}
