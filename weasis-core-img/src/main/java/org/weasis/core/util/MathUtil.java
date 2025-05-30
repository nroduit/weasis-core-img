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
 * Utility class providing helper methods for common mathematical operations. This includes
 * floating-point comparisons, geometric calculations, and rounding.
 *
 * @author Nicolas Roduit
 */
public final class MathUtil {
  public static final double DOUBLE_EPSILON = 1e-6;
  public static final double FLOAT_EPSILON = 1e-5;

  private MathUtil() {}

  /**
   * Checks if a float number is approximately equal to zero.
   *
   * @param val the number to test
   * @return true if the value is considered equal to zero, false otherwise
   */
  public static boolean isEqualToZero(float val) {
    return Math.copySign(val, 1.0) < FLOAT_EPSILON;
  }

  /**
   * Checks if a float number is approximately different from zero.
   *
   * @param val the number to test
   * @return true if the value is considered different from zero, false otherwise
   */
  public static boolean isDifferentFromZero(float val) {
    return Math.copySign(val, 1.0) > FLOAT_EPSILON;
  }

  /**
   * Checks if two float values are approximately equal.
   *
   * @param a the first number
   * @param b the second number
   * @return true if the numbers are approximately equal, false otherwise
   */
  public static boolean isEqual(float a, float b) {
    // Math.copySign is similar to Math.abs(x), but with different NaN semantics
    return Math.copySign(a - b, 1.0) <= FLOAT_EPSILON
        || (a == b) // infinities equal themselves
        || (Float.isNaN(a) && Float.isNaN(b));
  }

  /**
   * Checks if two float values are approximately different.
   *
   * @param a the first number
   * @param b the second number
   * @return true if the numbers are approximately different, false otherwise
   */
  public static boolean isDifferent(float a, float b) {
    // Math.copySign is similar to Math.abs(x), but with different NaN semantics
    return Math.copySign(a - b, 1.0) >= FLOAT_EPSILON;
  }

  /**
   * Checks if a floating-point number is approximately equal to zero.
   *
   * @param val the number to test
   * @return true if the value is considered equal to zero, false otherwise
   */
  public static boolean isEqualToZero(double val) {
    return Math.copySign(val, 1.0) < DOUBLE_EPSILON;
  }

  /**
   * Checks if a floating-point number is approximately different from zero.
   *
   * @param val the number to test
   * @return true if the value is considered different from zero, false otherwise
   */
  public static boolean isDifferentFromZero(double val) {
    return Math.copySign(val, 1.0) > DOUBLE_EPSILON;
  }

  /**
   * Checks if two floating-point numbers are approximately equal.
   *
   * @param a the first number
   * @param b the second number
   * @return true if the numbers are approximately equal, false otherwise
   */
  public static boolean isEqual(double a, double b) {
    // Math.copySign is similar to Math.abs(x), but with different NaN semantics
    return Math.copySign(a - b, 1.0) <= DOUBLE_EPSILON
        || (a == b) // infinities equal themselves
        || (Double.isNaN(a) && Double.isNaN(b));
  }

  /**
   * Checks if two floating-point numbers are approximately different.
   *
   * @param a the first number
   * @param b the second number
   * @return true if the numbers are approximately different, false otherwise
   */
  public static boolean isDifferent(double a, double b) {
    // Math.copySign is similar to Math.abs(x), but with different NaN semantics
    return Math.copySign(a - b, 1.0) >= DOUBLE_EPSILON;
  }

  // ------------------- Geometric Calculations -------------------

  /**
   * Calculates the orientation (angle) between two points.
   *
   * @param p1 the first point
   * @param p2 the second point
   * @return the angle (in degrees) between the points, or null if inputs are invalid
   */
  public static Double getOrientation(Point2D p1, Point2D p2) {
    return (p1 != null && p2 != null)
        ? getOrientation(p1.getX(), p1.getY(), p2.getX(), p2.getY())
        : null;
  }

  /**
   * Calculates the orientation (angle) between two coordinates.
   *
   * @param x1 x-coordinate of the first point
   * @param y1 y-coordinate of the first point
   * @param x2 x-coordinate of the second point
   * @param y2 y-coordinate of the second point
   * @return the angle (in degrees) between the points
   */
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

  /**
   * Calculates the azimuth angle between two points (angle clockwise from North).
   *
   * @param p1 the first point
   * @param p2 the second point
   * @return the azimuth angle (in degrees), or null if inputs are invalid
   */
  public static Double getAzimuth(Point2D p1, Point2D p2) {
    return (p1 != null && p2 != null)
        ? getAzimuth(p1.getX(), p1.getY(), p2.getX(), p2.getY())
        : null;
  }

  /**
   * Calculates the azimuth angle between two coordinates (angle clockwise from North).
   *
   * @param x1 x-coordinate of the first point
   * @param y1 y-coordinate of the first point
   * @param x2 x-coordinate of the second point
   * @param y2 y-coordinate of the second point
   * @return the azimuth angle (in degrees)
   */
  public static double getAzimuth(double x1, double y1, double x2, double y2) {
    double angle = Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
    angle = (angle + 450) % 360; // Adjust for clockwise direction and "N=0°"
    return angle;
  }

  // ------------------- Rounding -------------------

  /**
   * Rounds a double value to the specified number of decimal places.
   *
   * @param value the value to round
   * @param places the number of decimal places
   * @return the rounded value
   * @throws IllegalArgumentException if places is negative
   */
  public static double round(double value, int places) {
    if (places < 0) {
      throw new IllegalArgumentException("Decimal places must be non-negative");
    }

    BigDecimal bd = BigDecimal.valueOf(value);
    return bd.setScale(places, RoundingMode.HALF_UP).doubleValue();
  }

  // ------------------- Other Utilities -------------------

  /**
   * Clamps a value to a specified range.
   *
   * @param value the value to clamp
   * @param min the minimum value
   * @param max the maximum value
   * @return the clamped value
   */
  public static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(value, max));
  }

  /**
   * Linearly interpolates between two values.
   *
   * @param start the starting value
   * @param end the ending value
   * @param t the interpolation factor (0.0 - 1.0)
   * @return the interpolated value
   */
  public static double lerp(double start, double end, double t) {
    return start + t * (end - start);
  }
}
