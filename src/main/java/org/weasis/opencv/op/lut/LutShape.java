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

import org.weasis.opencv.data.LookupTableCV;

/**
 * @author Benoit Jacquemoud, Nicolas Roduit
 */
public final class LutShape {

  public static final LutShape LINEAR = new LutShape(eFunction.LINEAR);
  public static final LutShape SIGMOID = new LutShape(eFunction.SIGMOID);
  public static final LutShape SIGMOID_NORM = new LutShape(eFunction.SIGMOID_NORM);
  public static final LutShape LOG = new LutShape(eFunction.LOG);
  public static final LutShape LOG_INV = new LutShape(eFunction.LOG_INV);

  /**
   * LINEAR and SIGMOID descriptors are defined as DICOM standard LUT function <br>
   * Other LUT functions have their own custom implementation
   */
  public enum eFunction {
    LINEAR("Linear"),
    SIGMOID("Sigmoid"),
    SIGMOID_NORM("Sigmoid Normalize"),
    LOG("Logarithmic"),
    LOG_INV("Logarithmic Inverse");

    final String explanation;

    eFunction(String explanation) {
      this.explanation = explanation;
    }

    @Override
    public String toString() {
      return explanation;
    }
  }

  /**
   * A LutShape can be either a predefined function or a custom shape with a provided lookup table.
   * <br>
   * That is a LutShape can be defined as a function or by a lookup but not both
   */
  private final eFunction function;

  private final String explanation;
  private final LookupTableCV lookup;

  public LutShape(LookupTableCV lookup, String explanation) {
    if (lookup == null) {
      throw new IllegalArgumentException();
    }
    this.function = null;
    this.explanation = explanation;
    this.lookup = lookup;
  }

  public LutShape(eFunction function) {
    this(function, function.toString());
  }

  public LutShape(eFunction function, String explanation) {
    if (function == null) {
      throw new IllegalArgumentException();
    }
    this.function = function;
    this.explanation = explanation;
    this.lookup = null;
  }

  public eFunction getFunctionType() {
    return function;
  }

  public LookupTableCV getLookup() {
    return lookup;
  }

  @Override
  public String toString() {
    return explanation;
  }

  /**
   * LutShape objects are defined either by a factory function or by a custom LUT. They can be equal
   * even if they have different explanation property
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof LutShape shape) {
      return (function != null) ? function.equals(shape.function) : lookup.equals(shape.lookup);
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return (function != null) ? function.hashCode() : lookup.hashCode();
  }

  public static LutShape getLutShape(String shape) {
    if (shape != null) {
      String val = shape.toUpperCase();
      switch (val) {
        case "LINEAR":
          return LutShape.LINEAR;
        case "SIGMOID":
          return LutShape.SIGMOID;
        case "SIGMOID_NORM":
          return LutShape.SIGMOID_NORM;
        case "LOG":
          return LutShape.LOG;
        case "LOG_INV":
          return LutShape.LOG_INV;
      }
    }
    return null;
  }
}
