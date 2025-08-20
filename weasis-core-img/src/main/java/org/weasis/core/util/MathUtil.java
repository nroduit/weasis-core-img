/*
 * Copyright (c) 2010-2025 Weasis Team and other contributors.
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
import java.util.Objects;

/**
 * Utility class providing helper methods for common mathematical operations. This includes
 * floating-point comparisons, geometric calculations, rounding, and other mathematical utilities.
 *
 * <p>The class provides both absolute and relative epsilon-based comparisons for floating-point
 * numbers, handles special values like NaN and infinity correctly, and includes optimized geometric
 * calculations.
 *
 * @author Nicolas Roduit
 */
public final class MathUtil {
  // ================= Constants =================

  /** Default epsilon for double precision comparisons */
  public static final double DOUBLE_EPSILON = 1e-9;

  /** Default epsilon for float precision comparisons */
  public static final float FLOAT_EPSILON = 1e-5f;

  /** Cached values for performance optimization */
  public static final double DEGREES_TO_RADIANS = Math.PI / 180.0;

  public static final double RADIANS_TO_DEGREES = 180.0 / Math.PI;

  /** Minimum epsilon value to prevent division by zero in relative comparisons */
  private static final double MIN_DOUBLE_EPSILON = 1e-14;

  private static final float MIN_FLOAT_EPSILON = 1e-6f;

  private MathUtil() {}

  // ================= Floating-Point Comparisons =================

  /**
   * Checks if a float number is approximately equal to zero using the default epsilon.
   *
   * @param val the number to test
   * @return true if the value is considered equal to zero, false otherwise
   */
  public static boolean isEqualToZero(float val) {
    return isEqualToZero(val, FLOAT_EPSILON);
  }

  /**
   * Checks if a float number is approximately equal to zero using a custom epsilon.
   *
   * @param val the number to test
   * @param epsilon the tolerance for comparison
   * @return true if the value is considered equal to zero, false otherwise
   */
  public static boolean isEqualToZero(float val, float epsilon) {
    return Math.abs(val) < Math.abs(epsilon);
  }

  /**
   * Checks if a double number is approximately equal to zero using the default epsilon.
   *
   * @param val the number to test
   * @return true if the value is considered equal to zero, false otherwise
   */
  public static boolean isEqualToZero(double val) {
    return isEqualToZero(val, DOUBLE_EPSILON);
  }

  /**
   * Checks if a double number is approximately equal to zero using a custom epsilon.
   *
   * @param val the number to test
   * @param epsilon the tolerance for comparison
   * @return true if the value is considered equal to zero, false otherwise
   */
  public static boolean isEqualToZero(double val, double epsilon) {
    return Math.abs(val) < Math.abs(epsilon);
  }

  /**
   * Checks if a float number is approximately different from zero using the default epsilon.
   *
   * @param val the number to test
   * @return true if the value is considered different from zero, false otherwise
   */
  public static boolean isDifferentFromZero(float val) {
    return isDifferentFromZero(val, FLOAT_EPSILON);
  }

  /**
   * Checks if a float number is approximately different from zero using a custom epsilon.
   *
   * @param val the number to test
   * @param epsilon the tolerance for comparison
   * @return true if the value is considered different from zero, false otherwise
   */
  public static boolean isDifferentFromZero(float val, float epsilon) {
    if (Float.isNaN(val)) {
      return false;
    }
    return !isEqualToZero(val, epsilon);
  }

  /**
   * Checks if a double number is approximately different from zero using the default epsilon.
   *
   * @param val the number to test
   * @return true if the value is considered different from zero, false otherwise
   */
  public static boolean isDifferentFromZero(double val) {
    return isDifferentFromZero(val, DOUBLE_EPSILON);
  }

  /**
   * Checks if a double number is approximately different from zero using a custom epsilon.
   *
   * @param val the number to test
   * @param epsilon the tolerance for comparison
   * @return true if the value is considered different from zero, false otherwise
   */
  public static boolean isDifferentFromZero(double val, double epsilon) {
    if (Double.isNaN(val)) {
      return false;
    }
    return !isEqualToZero(val, epsilon);
  }

  /**
   * Checks if two float values are approximately equal using the default epsilon.
   *
   * @param a the first number
   * @param b the second number
   * @return true if the numbers are approximately equal, false otherwise
   */
  public static boolean isEqual(float a, float b) {
    return isEqual(a, b, FLOAT_EPSILON);
  }

  /**
   * Checks if two float values are approximately equal using a custom epsilon. This method handles
   * special cases like NaN and infinity correctly.
   *
   * @param a the first number
   * @param b the second number
   * @param epsilon the tolerance for comparison
   * @return true if the numbers are approximately equal, false otherwise
   */
  public static boolean isEqual(float a, float b, float epsilon) {
    if (a == b) return true; // Handle exact equality and infinities
    if (Float.isNaN(a) && Float.isNaN(b)) return true;
    if (Float.isNaN(a) || Float.isNaN(b)) return false;
    if (!Float.isFinite(a) || !Float.isFinite(b)) return false;

    return performEpsilonComparison(a, b, epsilon, MIN_FLOAT_EPSILON);
  }

  /**
   * Checks if two double values are approximately equal using the default epsilon.
   *
   * @param a the first number
   * @param b the second number
   * @return true if the numbers are approximately equal, false otherwise
   */
  public static boolean isEqual(double a, double b) {
    return isEqual(a, b, DOUBLE_EPSILON);
  }

  /**
   * Checks if two double values are approximately equal using a custom epsilon. This method handles
   * special cases like NaN and infinity correctly.
   *
   * @param a the first number
   * @param b the second number
   * @param epsilon the tolerance for comparison
   * @return true if the numbers are approximately equal, false otherwise
   */
  public static boolean isEqual(double a, double b, double epsilon) {
    if (a == b) return true; // Handle exact equality and infinities
    if (Double.isNaN(a) && Double.isNaN(b)) return true;
    if (Double.isNaN(a) || Double.isNaN(b)) return false;
    if (!Double.isFinite(a) || !Double.isFinite(b)) return false;

    return performEpsilonComparison(a, b, epsilon, MIN_DOUBLE_EPSILON);
  }

  /** Performs epsilon comparison using absolute for small values, relative for larger values */
  private static boolean performEpsilonComparison(
      double a, double b, double epsilon, double minEpsilon) {
    double diff = Math.abs(a - b);
    double absA = Math.abs(a);
    double absB = Math.abs(b);

    // Use absolute comparison for values close to zero
    if (absA < minEpsilon || absB < minEpsilon) {
      return diff < Math.abs(epsilon);
    }

    // Use relative comparison for larger values
    return diff < Math.abs(epsilon) * Math.max(absA, absB);
  }

  /**
   * Checks if two float values are approximately different using the default epsilon.
   *
   * @param a the first number
   * @param b the second number
   * @return true if the numbers are approximately different, false otherwise
   */
  public static boolean isDifferent(float a, float b) {
    return !isEqual(a, b);
  }

  /**
   * Checks if two float values are approximately different using a custom epsilon.
   *
   * @param a the first number
   * @param b the second number
   * @param epsilon the tolerance for comparison
   * @return true if the numbers are approximately different, false otherwise
   */
  public static boolean isDifferent(float a, float b, float epsilon) {
    return !isEqual(a, b, epsilon);
  }

  /**
   * Checks if two double values are approximately different using the default epsilon.
   *
   * @param a the first number
   * @param b the second number
   * @return true if the numbers are approximately different, false otherwise
   */
  public static boolean isDifferent(double a, double b) {
    return !isEqual(a, b);
  }

  /**
   * Checks if two double values are approximately different using a custom epsilon.
   *
   * @param a the first number
   * @param b the second number
   * @param epsilon the tolerance for comparison
   * @return true if the numbers are approximately different, false otherwise
   */
  public static boolean isDifferent(double a, double b, double epsilon) {
    return !isEqual(a, b, epsilon);
  }

  // ================= Geometric Calculations =================

  /**
   * Calculates the orientation (angle) between two points in image coordinate system. Uses image
   * coordinate system where Y increases downward.
   *
   * @param p1 the first point
   * @param p2 the second point
   * @return the angle (in degrees) between the points, or null if inputs are invalid
   */
  public static Double getOrientation(Point2D p1, Point2D p2) {
    if (p1 == null || p2 == null) return null;
    return getOrientation(p1.getX(), p1.getY(), p2.getX(), p2.getY());
  }

  /**
   * Calculates the orientation (angle) between two coordinates in image coordinate system. The
   * orientation is measured from the first point to the second point. In image coordinates, Y
   * increases downward.
   *
   * @param x1 x-coordinate of the first point
   * @param y1 y-coordinate of the first point
   * @param x2 x-coordinate of the second point
   * @param y2 y-coordinate of the second point
   * @return the angle (in degrees) between the points, normalized to [0, 180]
   */
  public static double getOrientation(double x1, double y1, double x2, double y2) {
    validateFiniteValues(x1, y1, x2, y2);

    // Image coordinates: Y increases downward. Flip Y to account for inverted Y-axis
    double angle = Math.toDegrees(Math.atan2(y1 - y2, x1 - x2));
    return angle < 0 ? -angle : 180.0 - angle;
  }

  /**
   * Calculates the azimuth angle between two points in image coordinate system (angle clockwise
   * from North). In image coordinates, Y increases downward.
   *
   * @param p1 the first point
   * @param p2 the second point
   * @return the azimuth angle (in degrees), or null if inputs are invalid
   */
  public static Double getAzimuth(Point2D p1, Point2D p2) {
    if (p1 == null || p2 == null) return null;
    return getAzimuth(p1.getX(), p1.getY(), p2.getX(), p2.getY());
  }

  /**
   * Calculates the azimuth angle between two coordinates in image coordinate system (angle
   * clockwise from North). In image coordinates, Y increases downward.
   *
   * @param x1 x-coordinate of the first point
   * @param y1 y-coordinate of the first point
   * @param x2 x-coordinate of the second point
   * @param y2 y-coordinate of the second point
   * @return the azimuth angle (in degrees), normalized to [0, 360)
   */
  public static double getAzimuth(double x1, double y1, double x2, double y2) {
    validateFiniteValues(x1, y1, x2, y2);

    // Image coordinates: Y increases downward. North = -Y, East = +X
    double angle = Math.atan2(x2 - x1, -(y2 - y1)) * RADIANS_TO_DEGREES;
    return normalizeAngle(angle);
  }

  /**
   * Calculates the distance between two points.
   *
   * @param p1 the first point
   * @param p2 the second point
   * @return the distance between the points, or null if inputs are invalid
   */
  public static Double getDistance(Point2D p1, Point2D p2) {
    if (p1 == null || p2 == null) return null;
    return getDistance(p1.getX(), p1.getY(), p2.getX(), p2.getY());
  }

  /**
   * Calculates the distance between two coordinates.
   *
   * @param x1 x-coordinate of the first point
   * @param y1 y-coordinate of the first point
   * @param x2 x-coordinate of the second point
   * @param y2 y-coordinate of the second point
   * @return the distance between the points
   */
  public static double getDistance(double x1, double y1, double x2, double y2) {
    validateFiniteValues(x1, y1, x2, y2);

    double dx = x2 - x1;
    double dy = y2 - y1;
    return Math.sqrt(dx * dx + dy * dy);
  }

  /**
   * Normalizes an angle to the range [0, 360).
   *
   * @param angle the angle in degrees
   * @return the normalized angle
   */
  public static double normalizeAngle(double angle) {
    if (!Double.isFinite(angle)) {
      throw new IllegalArgumentException("Angle must be finite: " + angle);
    }

    angle = angle % 360.0;
    return angle < 0 ? angle + 360.0 : angle;
  }

  // ================= Rounding and Precision =================

  /**
   * Rounds a double value to the specified number of decimal places using HALF_UP rounding.
   *
   * @param value the value to round
   * @param places the number of decimal places (must be non-negative)
   * @return the rounded value
   */
  public static double round(double value, int places) {
    return round(value, places, RoundingMode.HALF_UP);
  }

  /**
   * Rounds a double value to the specified number of decimal places using the specified rounding
   * mode.
   *
   * @param value the value to round
   * @param places the number of decimal places (must be non-negative)
   * @param roundingMode the rounding mode to use
   * @return the rounded value
   */
  public static double round(double value, int places, RoundingMode roundingMode) {
    if (places < 0) {
      throw new IllegalArgumentException("Decimal places must be non-negative: " + places);
    }

    Objects.requireNonNull(roundingMode, "RoundingMode cannot be null");

    if (!Double.isFinite(value)) return value; // Return NaN or infinity as-is

    try {
      return BigDecimal.valueOf(value).setScale(places, roundingMode).doubleValue();
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid value for rounding: " + value, e);
    }
  }

  /**
   * Truncates a double value to the specified number of decimal places.
   *
   * @param value the value to truncate
   * @param places the number of decimal places (must be non-negative)
   * @return the truncated value
   * @throws IllegalArgumentException if places is negative
   */
  public static double truncate(double value, int places) {
    return round(value, places, RoundingMode.DOWN);
  }

  /**
   * Rounds a double value to the nearest integer.
   *
   * @param value the value to round
   * @return the rounded integer value
   */
  public static long roundToLong(double value) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Value must be finite: " + value);
    }
    return Math.round(value);
  }

  // ================= Range and Interpolation Utilities =================
  // clamp available in Java 21 in Math.clamp(double, double, double)

  /**
   * Clamps a value to a specified range.
   *
   * @param value the value to clamp
   * @param min the minimum value
   * @param max the maximum value
   * @return the clamped value
   */
  public static double clamp(double value, double min, double max) {
    validateMinMax(min, max);
    return Double.isNaN(value) ? value : Math.max(min, Math.min(value, max));
  }

  /**
   * Clamps a value to a specified range with float bounds.
   *
   * @param value the value to clamp
   * @param min the minimum value
   * @param max the maximum value
   * @return the clamped value
   */
  public static float clamp(float value, float min, float max) {
    validateMinMax(min, max);
    return Float.isNaN(value) ? value : Math.max(min, Math.min(value, max));
  }

  /**
   * Clamps a value to a specified range with integer bounds.
   *
   * @param value the value to clamp
   * @param min the minimum value
   * @param max the maximum value
   * @return the clamped value
   */
  public static int clamp(int value, int min, int max) {
    validateMinMax(min, max);
    return Math.max(min, Math.min(value, max));
  }

  /**
   * Linearly interpolates between two values.
   *
   * @param start the starting value
   * @param end the ending value
   * @param t the interpolation factor (typically 0.0 - 1.0, but can be outside this range)
   * @return the interpolated value
   */
  public static double lerp(double start, double end, double t) {
    validateFiniteValues(start, end, t);

    // Use more numerically stable formula for extreme values
    if (t == 0.0) return start;
    if (t == 1.0) return end;
    return start + t * (end - start);
  }

  /**
   * Performs inverse linear interpolation to find the interpolation factor.
   *
   * @param start the starting value
   * @param end the ending value
   * @param value the value to find the factor for
   * @return the interpolation factor
   */
  public static double invLerp(double start, double end, double value) {
    validateFiniteValues(start, end, value);

    if (start == end) {
      throw new IllegalArgumentException(
          "Start and end values cannot be equal for inverse interpolation");
    }

    return (value - start) / (end - start);
  }

  /**
   * Maps a value from one range to another.
   *
   * @param value the value to map
   * @param fromMin the minimum of the source range
   * @param fromMax the maximum of the source range
   * @param toMin the minimum of the target range
   * @param toMax the maximum of the target range
   * @return the mapped value
   */
  public static double map(
      double value, double fromMin, double fromMax, double toMin, double toMax) {
    double t = invLerp(fromMin, fromMax, value);
    return lerp(toMin, toMax, t);
  }

  // ================= Additional Utility Methods =================

  /**
   * Checks if a value is within a specified range (inclusive).
   *
   * @param value the value to check
   * @param min the minimum value
   * @param max the maximum value
   * @return true if the value is within the range, false otherwise
   */
  public static boolean inRange(double value, double min, double max) {
    return value >= min && value <= max;
  }

  /**
   * Calculates the percentage of a value within a range.
   *
   * @param value the value
   * @param min the minimum of the range
   * @param max the maximum of the range
   * @return the percentage (0.0 to 1.0)
   */
  public static double percentage(double value, double min, double max) {
    if (isEqual(min, max)) {
      throw new IllegalArgumentException("Range cannot have zero width");
    }
    return clamp(invLerp(min, max, value), 0.0, 1.0);
  }

  // ================= Private Helper Methods =================

  /** Validates that values are finite (not NaN or infinite) */
  private static void validateFiniteValues(double... values) {
    for (int i = 0; i < values.length; i++) {
      if (!Double.isFinite(values[i])) {
        throw new IllegalArgumentException("Value " + i + " must be finite: " + values[i]);
      }
    }
  }

  private static void validateMinMax(double min, double max) {
    if (min > max) {
      throw new IllegalArgumentException(
          "Minimum value cannot be greater than maximum: " + min + " > " + max);
    }
  }

  private static void validateMinMax(float min, float max) {
    if (min > max) {
      throw new IllegalArgumentException(
          "Minimum value cannot be greater than maximum: " + min + " > " + max);
    }
  }

  private static void validateMinMax(int min, int max) {
    if (min > max) {
      throw new IllegalArgumentException(
          "Minimum value cannot be greater than maximum: " + min + " > " + max);
    }
  }
}
