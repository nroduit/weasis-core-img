/*
 * Copyright (c) 2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op.lut;

import java.util.Objects;
import java.util.Set;
import org.weasis.core.util.StringUtil;
import org.weasis.opencv.data.LookupTableCV;

/**
 * Represents a lookup table transformation shape for medical image processing.
 *
 * <p>A {@code LutShape} defines how pixel values are transformed during window/level operations. It
 * can be either:
 *
 * <ul>
 *   <li>A predefined mathematical function (LINEAR, SIGMOID, LOG, etc.)
 *   <li>A custom lookup table with arbitrary transformation values
 * </ul>
 *
 * <p>The LINEAR and SIGMOID functions comply with DICOM Part 3 standard specifications for
 * presentation LUT shapes, while other functions provide enhanced visualization capabilities for
 * specific medical imaging needs.
 *
 * <p>This class is immutable and thread-safe. Instances can be compared by their underlying
 * function or lookup table content, ignoring explanation differences.
 *
 * @author Benoit Jacquemoud
 * @author Nicolas Roduit
 */
public final class LutShape {

  /** Linear transformation - DICOM standard LUT function */
  public static final LutShape LINEAR = new LutShape(Function.LINEAR);

  /** Sigmoid transformation - DICOM standard LUT function */
  public static final LutShape SIGMOID = new LutShape(Function.SIGMOID);

  /** Normalized sigmoid transformation - custom implementation */
  public static final LutShape SIGMOID_NORM = new LutShape(Function.SIGMOID_NORM);

  /** Logarithmic transformation - custom implementation */
  public static final LutShape LOG = new LutShape(Function.LOG);

  /** Inverse logarithmic transformation - custom implementation */
  public static final LutShape LOG_INV = new LutShape(Function.LOG_INV);

  /**
   * Enumeration of predefined lookup table transformation functions.
   *
   * <p>LINEAR and SIGMOID are defined according to DICOM Part 3 standard. Other functions provide
   * custom implementations for specialized imaging needs.
   */
  public enum Function {
    /** Linear transformation: f(x) = x */
    LINEAR("Linear"),
    /** Sigmoid transformation: f(x) = 1/(1+e^(-x)) */
    SIGMOID("Sigmoid"),
    /** Normalized sigmoid transformation with enhanced contrast */
    SIGMOID_NORM("Sigmoid Normalize"),
    /** Logarithmic transformation: f(x) = log(x) */
    LOG("Logarithmic"),
    /** Inverse logarithmic transformation: f(x) = e^x */
    LOG_INV("Logarithmic Inverse");

    private final String description;

    Function(String description) {
      this.description = description;
    }

    /**
     * Gets the human-readable description of this function.
     *
     * @return the function description
     */
    public String getDescription() {
      return description;
    }

    @Override
    public String toString() {
      return description;
    }
  }

  private final Function function;

  private final String explanation;
  private final LookupTableCV lookup;

  /**
   * Creates a LutShape with a custom lookup table.
   *
   * @param lookup the custom lookup table for pixel transformation
   * @param explanation human-readable description of this transformation
   * @throws IllegalArgumentException if lookup is null
   * @throws IllegalArgumentException if explanation is null or empty
   */
  public LutShape(LookupTableCV lookup, String explanation) {
    if (lookup == null) {
      throw new IllegalArgumentException("Lookup table cannot be null");
    }
    this.function = null;
    this.explanation = StringUtil.getEmptyStringIfNull(explanation);
    this.lookup = lookup;
  }

  /**
   * Creates a LutShape with a predefined function using its default description.
   *
   * @param function the predefined transformation function
   * @throws IllegalArgumentException if function is null
   */
  public LutShape(Function function) {
    this(function, function.getDescription());
  }

  /**
   * Creates a LutShape with a predefined function and custom explanation.
   *
   * @param function the predefined transformation function
   * @param explanation human-readable description of this transformation
   * @throws IllegalArgumentException if function is null
   * @throws IllegalArgumentException if explanation is null or empty
   */
  public LutShape(Function function, String explanation) {
    if (function == null) {
      throw new IllegalArgumentException("Function cannot be null");
    }
    this.function = function;
    this.explanation = StringUtil.getEmptyStringIfNull(explanation);
    this.lookup = null;
  }

  /**
   * Gets the predefined function type, if this LutShape uses a mathematical function.
   *
   * @return the function type, or {@code null} if this uses a custom lookup table
   */
  public Function getFunctionType() {
    return function;
  }

  /**
   * Gets the custom lookup table, if this LutShape uses tabulated values.
   *
   * @return the lookup table, or {@code null} if this uses a predefined function
   */
  public LookupTableCV getLookup() {
    return lookup;
  }

  /**
   * Gets the explanation describing this transformation.
   *
   * @return the human-readable explanation
   */
  public String getExplanation() {
    return explanation;
  }

  @Override
  public String toString() {
    return explanation;
  }

  /**
   * Checks if this LutShape uses a predefined mathematical function.
   *
   * @return {@code true} if this uses a function, {@code false} if using a lookup table
   */
  public boolean isFunction() {
    return function != null;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LutShape lutShape = (LutShape) o;
    return isFunction() == lutShape.isFunction()
        && Objects.equals(getExplanation(), lutShape.getExplanation())
        && Objects.equals(getLookup(), lutShape.getLookup());
  }

  @Override
  public int hashCode() {
    return Objects.hash(isFunction(), getExplanation(), getLookup());
  }

  /**
   * Creates a LutShape from a string representation of a function name.
   *
   * <p>Supported function names (case-insensitive):
   *
   * <ul>
   *   <li>"LINEAR" - Linear transformation
   *   <li>"SIGMOID" - Sigmoid transformation
   *   <li>"SIGMOID_NORM" - Normalized sigmoid transformation
   *   <li>"LOG" - Logarithmic transformation
   *   <li>"LOG_INV" - Inverse logarithmic transformation
   * </ul>
   *
   * @param shape the string representation of the function name
   * @return the corresponding LutShape, or {@code null} if not found
   */
  public static LutShape getLutShape(String shape) {
    if (shape == null || shape.trim().isEmpty()) {
      return null;
    }
    String normalizedShape = shape.trim().toUpperCase();
    return switch (normalizedShape) {
      case "LINEAR" -> LINEAR;
      case "SIGMOID" -> SIGMOID;
      case "SIGMOID_NORM" -> SIGMOID_NORM;
      case "LOG" -> LOG;
      case "LOG_INV" -> LOG_INV;
      default -> null;
    };
  }

  /**
   * Gets all available predefined LutShape constants.
   *
   * @return an immutable set of all predefined LutShape instances
   */
  public static Set<LutShape> getAllPredefined() {
    return Set.of(LINEAR, SIGMOID, SIGMOID_NORM, LOG, LOG_INV);
  }
}
