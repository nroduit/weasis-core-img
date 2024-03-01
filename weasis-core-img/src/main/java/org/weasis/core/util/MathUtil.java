/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author Nicolas Roduit
 */
public class MathUtil {
  public static final double DOUBLE_EPSILON = 1e-6;
  public static final double FLOAT_EPSILON = 1e-5;

  private MathUtil() {}

  public static boolean isEqualToZero(float val) {
    return Math.copySign(val, 1.0) < FLOAT_EPSILON;
  }

  public static boolean isDifferentFromZero(float val) {
    return Math.copySign(val, 1.0) > FLOAT_EPSILON;
  }

  public static boolean isEqual(float a, float b) {
    // Math.copySign is similar to Math.abs(x), but with different NaN semantics
    return Math.copySign(a - b, 1.0) <= FLOAT_EPSILON
        || (a == b) // infinities equal themselves
        || (Float.isNaN(a) && Float.isNaN(b));
  }

  public static boolean isDifferent(float a, float b) {
    // Math.copySign is similar to Math.abs(x), but with different NaN semantics
    return Math.copySign(a - b, 1.0) >= FLOAT_EPSILON;
  }

  public static boolean isEqualToZero(double val) {
    return Math.copySign(val, 1.0) < DOUBLE_EPSILON;
  }

  public static boolean isDifferentFromZero(double val) {
    return Math.copySign(val, 1.0) > DOUBLE_EPSILON;
  }

  public static boolean isEqual(double a, double b) {
    // Math.copySign is similar to Math.abs(x), but with different NaN semantics
    return Math.copySign(a - b, 1.0) <= DOUBLE_EPSILON
        || (a == b) // infinities equal themselves
        || (Double.isNaN(a) && Double.isNaN(b));
  }

  public static boolean isDifferent(double a, double b) {
    // Math.copySign is similar to Math.abs(x), but with different NaN semantics
    return Math.copySign(a - b, 1.0) >= DOUBLE_EPSILON;
  }

  public static Double getOrientation(Point2D p1, Point2D p2) {
    return (p1 != null && p2 != null)
        ? getOrientation(p1.getX(), p1.getY(), p2.getX(), p2.getY())
        : null;
  }

  public static double getOrientation(double x1, double y1, double x2, double y2) {
    // Use arctan2 to handle to handle possible negative values
    double teta = Math.atan2(y1 - y2, x1 - x2);
    double angle = Math.toDegrees(teta); // convert from radians to degrees
    // Return the orientation from 0 to 180 degrees
    if (angle < 0) {
      angle = -angle;
    } else {
      angle = 180 - angle;
    }
    return angle;
  }

  public static Double getAzimuth(Point2D p1, Point2D p2) {
    return (p1 != null && p2 != null)
        ? getAzimuth(p1.getX(), p1.getY(), p2.getX(), p2.getY())
        : null;
  }

  public static double getAzimuth(double x1, double y1, double x2, double y2) {
    double angle = Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
    angle = (angle + 450.0) % 360.0;
    return angle;
  }

  public static double round(double value, int places) {
    if (places < 0) throw new IllegalArgumentException();

    BigDecimal bd = BigDecimal.valueOf(value);
    bd = bd.setScale(places, RoundingMode.HALF_UP);
    return bd.doubleValue();
  }
}
