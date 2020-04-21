package org.weasis.opencv.op.lut;

import java.util.Optional;

import org.weasis.opencv.data.LookupTableCV;

public interface PresentationStateLut {

    Optional<LookupTableCV> getPrLut();

    Optional<String> getPrLutExplanation();

    Optional<String> getPrLutShapeMode();

}