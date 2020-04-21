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
