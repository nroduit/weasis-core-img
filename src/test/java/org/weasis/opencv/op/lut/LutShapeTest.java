/*
 * Copyright (c) 2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.opencv.op.lut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.weasis.opencv.data.LookupTableCV;
import org.weasis.opencv.op.lut.LutShape.eFunction;

class LutShapeTest {

  /** Method under test: {@link LutShape#LutShape(LutShape.eFunction, String)} */
  @Test
  void testConstructor() {
    LookupTableCV lookup = new LookupTableCV(new byte[] {1, -1, 1, -1, 0, 127, -128, -9, 9});
    LutShape actualLutShape = new LutShape(lookup, "Explanation");
    assertNull(actualLutShape.getFunctionType());
    assertEquals("Explanation", actualLutShape.toString());
    assertSame(lookup, actualLutShape.getLookup());

    actualLutShape = new LutShape(LutShape.eFunction.LOG_INV, "Explanation");
    assertEquals(LutShape.eFunction.LOG_INV, actualLutShape.getFunctionType());
    assertEquals("Explanation", actualLutShape.toString());
    assertNull(actualLutShape.getLookup());

    assertThrows(IllegalArgumentException.class, () -> new LutShape((LookupTableCV) null, "foo"));
    assertThrows(IllegalArgumentException.class, () -> new LutShape((eFunction) null, "foo"));
  }

  /**
   * Methods under test:
   *
   * <ul>
   *   <li>{@link LutShape#equals(Object)}
   *   <li>{@link LutShape#hashCode()}
   * </ul>
   */
  @Test
  void testEquals() {
    LutShape lutShape = new LutShape(LutShape.eFunction.LINEAR);
    LutShape lutShape2 = new LutShape(LutShape.eFunction.LINEAR, "Explanation");
    assertEquals(lutShape, lutShape2);
    assertEquals(lutShape.hashCode(), lutShape2.hashCode());
  }

  /**
   * Methods under test:
   *
   * <ul>
   *   <li>{@link LutShape#getFunctionType()}
   *   <li>{@link LutShape#getLookup()}
   *   <li>{@link LutShape#toString()}
   * </ul>
   */
  @Test
  void testGetFunctionType() {
    LutShape lutShape = new LutShape(LutShape.eFunction.LINEAR);
    LutShape.eFunction actualFunctionType = lutShape.getFunctionType();
    LookupTableCV actualLookup = lutShape.getLookup();
    assertEquals(LutShape.eFunction.LINEAR, actualFunctionType);
    assertNull(actualLookup);
  }

  /** Method under test: {@link LutShape#getLutShape(String)} */
  @Test
  void testGetLutShape() {
    assertNull(LutShape.getLutShape("Shape"));
    assertNull(LutShape.getLutShape(null));
    assertSame(LutShape.LINEAR, LutShape.getLutShape("LINEAR"));
    assertSame(LutShape.SIGMOID, LutShape.getLutShape("SIGMOID"));
    assertSame(LutShape.SIGMOID_NORM, LutShape.getLutShape("SIGMOID_NORM"));
    assertSame(LutShape.LOG, LutShape.getLutShape("LOG"));
    assertSame(LutShape.LOG_INV, LutShape.getLutShape("LOG_INV"));
  }
}
