module org.weasis.core.img {
  requires java.desktop;
  requires java.xml;
  requires org.slf4j;

  opens org.opencv.osgi; // Allow reflective access for native code loading

  exports org.weasis.core.util;
  exports org.weasis.core.util.annotations;
  exports org.weasis.opencv.op;
  exports org.weasis.opencv.op.lut;
  exports org.weasis.opencv.data;
  exports org.weasis.opencv.seg;
  exports org.opencv.core;
  exports org.opencv.img_hash;
  exports org.opencv.imgcodecs;
  exports org.opencv.imgproc;
  exports org.opencv.osgi;
  exports org.opencv.utils;
}