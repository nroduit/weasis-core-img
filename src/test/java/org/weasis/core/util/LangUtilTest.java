/*
 * Copyright (c) 2010-2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Point;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import javax.xml.stream.util.EventReaderDelegate;
import org.junit.jupiter.api.Test;

class LangUtilTest {

  /** Method under test: {@link LangUtil#emptyIfNull(Iterable)} */
  @Test
  void testEmptyIfNull() {
    Iterable<Object> actualEmptyIfNullResult = LangUtil.emptyIfNull(null);
    actualEmptyIfNullResult.iterator();
    assertTrue(((Collection<Object>) actualEmptyIfNullResult).isEmpty());
  }

  /** Method under test: {@link LangUtil#emptyIfNull(Iterable)} */
  @Test
  void testEmptyIfNull2() {
    Iterable<Object> iterable = mock(Iterable.class);
    EventReaderDelegate eventReaderDelegate = new EventReaderDelegate();
    when(iterable.iterator()).thenReturn(eventReaderDelegate);
    Iterable<Object> actualEmptyIfNullResult = LangUtil.emptyIfNull(iterable);
    Iterator<Object> actualIteratorResult = actualEmptyIfNullResult.iterator();
    verify(iterable).iterator();
    assertSame(eventReaderDelegate, actualIteratorResult);
  }

  /** Method under test: {@link LangUtil#memoize(Supplier)} */
  @Test
  void testMemoize() {
    Point p = new Point(3, 3);
    Supplier<Point> original = mock(Supplier.class);
    when(original.get()).thenReturn(p);
    Supplier<Point> actualMemoizeResult = LangUtil.memoize(original);
    Point actualGetResult = actualMemoizeResult.get();
    assertSame(p, actualGetResult);
  }

  /** Method under test: {@link LangUtil#getNULLtoFalse(Boolean)} */
  @Test
  void testGetNULLtoFalse() {
    assertTrue(LangUtil.getNULLtoFalse(true));
    assertFalse(LangUtil.getNULLtoFalse(null));
    assertFalse(LangUtil.getNULLtoTrue(false));
  }

  /** Method under test: {@link LangUtil#getNULLtoTrue(Boolean)} */
  @Test
  void testGetNULLtoTrue() {
    assertTrue(LangUtil.getNULLtoTrue(true));
    assertTrue(LangUtil.getNULLtoTrue(null));
    assertFalse(LangUtil.getNULLtoTrue(false));
  }

  /** Method under test: {@link LangUtil#getEmptytoFalse(String)} */
  @Test
  void testGetEmptytoFalse() {
    assertFalse(LangUtil.getEmptytoFalse("false"));
    assertTrue(LangUtil.getEmptytoFalse("true"));
    assertFalse(LangUtil.getEmptytoFalse(null));
    assertFalse(LangUtil.getEmptytoFalse(StringUtil.EMPTY_STRING));
    assertTrue(LangUtil.geEmptytoTrue(" "));
  }

  /** Method under test: {@link LangUtil#geEmptytoTrue(String)} */
  @Test
  void testGeEmptytoTrue() {
    assertFalse(LangUtil.geEmptytoTrue("false"));
    assertTrue(LangUtil.geEmptytoTrue("true"));
    assertTrue(LangUtil.geEmptytoTrue(null));
    assertTrue(LangUtil.geEmptytoTrue(StringUtil.EMPTY_STRING));
    assertTrue(LangUtil.geEmptytoTrue(" "));
  }

  /** Method under test: {@link LangUtil#getOptionalDouble(Double)} */
  @Test
  void testGetOptionalDouble() {
    OptionalDouble opDouble = LangUtil.getOptionalDouble(null);
    assertFalse(opDouble.isPresent());
    opDouble = LangUtil.getOptionalDouble(10.0d);
    assertEquals(10.0d, opDouble.getAsDouble());
  }

  /** Method under test: {@link LangUtil#getOptionalInteger(Integer)} */
  @Test
  void testGetOptionalInteger() {
    OptionalInt optionalInteger = LangUtil.getOptionalInteger(null);
    assertFalse(optionalInteger.isPresent());
    optionalInteger = LangUtil.getOptionalInteger(42);
    assertEquals(42, optionalInteger.getAsInt());
  }

  /** Method under test: {@link LangUtil#safeBufferType(ByteBuffer)} */
  @Test
  void testSafeBufferType() {
    assertNull(LangUtil.safeBufferType(null));
  }
}
