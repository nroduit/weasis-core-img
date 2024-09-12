/*
 * Copyright (c) 2024 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TripleTest {

  @Test
  void tripleStoresFirstSecondAndThirdValues() {
    Triple<String, Integer, Double> triple = new Triple<>("first", 1, 2.0);
    assertEquals("first", triple.first());
    assertEquals(1, triple.second());
    assertEquals(2.0, triple.third());
  }

  @Test
  void tripleHandlesMixedNullAndNonNullValues() {
    Triple<String, Integer, Double> triple = new Triple<>("first", null, 2.0);
    assertEquals("first", triple.first());
    assertNull(triple.second());
    assertEquals(2.0, triple.third());

    triple = new Triple<>(null, 1, null);
    assertNull(triple.first());
    assertEquals(1, triple.second());
    assertNull(triple.third());
  }

  @Test
  void tripleWithDifferentTypes() {
    Triple<Integer, String, Boolean> triple = new Triple<>(1, "second", true);
    assertEquals(1, triple.first());
    assertEquals("second", triple.second());
    assertEquals(true, triple.third());
  }

  @Test
  void tripleEquality() {
    Triple<String, Integer, Double> triple1 = new Triple<>("first", 1, 2.0);
    Triple<String, Integer, Double> triple2 = new Triple<>("first", 1, 2.0);
    assertEquals(triple1, triple2);
  }

  @Test
  void tripleInequality() {
    Triple<String, Integer, Double> triple1 = new Triple<>("first", 1, 2.0);
    Triple<String, Integer, Double> triple2 = new Triple<>("second", 1, 2.0);
    assertNotEquals(triple1, triple2);

    triple2 = new Triple<>("first", 2, 2.0);
    assertNotEquals(triple1, triple2);

    triple2 = new Triple<>("first", 1, 3.0);
    assertNotEquals(triple1, triple2);
  }
}
