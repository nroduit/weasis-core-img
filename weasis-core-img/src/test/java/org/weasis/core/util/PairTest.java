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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PairTest {
  @Test
  void pairStoresFirstAndSecondValues() {
    Pair<String, Integer> pair = new Pair<>("first", 1);
    assertEquals("first", pair.first());
    assertEquals(1, pair.second());
  }

  @Test
  void pairHandlesMixedNullAndNonNullValues() {
    Pair<String, Integer> pair = new Pair<>("first", null);
    assertEquals("first", pair.first());
    assertNull(pair.second());

    pair = new Pair<>(null, 1);
    assertNull(pair.first());
    assertEquals(1, pair.second());
  }

  @Test
  void pairWithDifferentTypes() {
    Pair<Integer, String> pair = new Pair<>(1, "second");
    assertEquals(1, pair.first());
    assertEquals("second", pair.second());
  }

  @Test
  void pairEquality() {
    Pair<String, Integer> pair1 = new Pair<>("first", 1);
    Pair<String, Integer> pair2 = new Pair<>("first", 1);
    assertEquals(pair1, pair2);
  }

  @Test
  void pairInequality() {
    Pair<String, Integer> pair1 = new Pair<>("first", 1);
    Pair<String, Integer> pair2 = new Pair<>("second", 1);
    assertNotEquals(pair1, pair2);

    pair2 = new Pair<>("first", 2);
    assertNotEquals(pair1, pair2);
  }
}
