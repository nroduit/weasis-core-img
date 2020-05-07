/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.weasis.opencv.op.lut;

/**
 * @author Nicolas Roduit
 *
 */
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
