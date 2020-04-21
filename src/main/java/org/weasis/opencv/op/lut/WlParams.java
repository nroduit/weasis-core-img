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