module weasis.core.img {
  requires java.desktop;
  requires org.slf4j;
  requires java.logging;

  exports org.weasis.core.util;
  exports org.weasis.opencv.data;
  exports org.weasis.opencv.op;
  exports org.weasis.opencv.op.lut;
  exports org.weasis.opencv.op.tile;
  exports org.opencv.core;
  exports org.opencv.imgcodecs;
  exports org.opencv.img_hash;
  exports org.opencv.imgproc;
  exports org.opencv.osgi;
  exports org.opencv.utils;
  exports org.weasis.core.util.annotations;
}
