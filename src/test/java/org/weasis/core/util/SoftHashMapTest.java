/*
 * Copyright (c) 2023 Weasis Team and other contributors.
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map.Entry;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SoftHashMapTest {

  /**
   * Method under test: {@link SoftHashMap#get(Object)} and {@link SoftHashMap#put(Object, Object)}
   */
  @Test
  void testGetPut() {
    SoftHashMap<Object, Object> objectObjectMap = new SoftHashMap<>();
    assertNull(objectObjectMap.get("Key"));

    objectObjectMap.put("Key", "Value");
    assertEquals("Value", objectObjectMap.get("Key"));

    // Simulate garbage collection
    objectObjectMap.put("Key", null);
    assertNull(objectObjectMap.get("Key"));
    // Check that the entry is removed during get()
    assertEquals(0, objectObjectMap.size());
  }

  /** Method under test: {@link SoftHashMap#remove(Object)} */
  @Test
  void testRemove() {
    SoftHashMap<Object, Object> objectObjectMap = new SoftHashMap<>();
    assertNull(objectObjectMap.remove("Key"));

    objectObjectMap.put("Key", "Value");
    assertEquals("Value", objectObjectMap.remove("Key"));
  }

  /** Method under test: {@link SoftHashMap#clear()} */
  @Test
  void testClear() {
    SoftHashMap<Object, Object> objectObjectMap = new SoftHashMap<>();
    objectObjectMap.put("Key", "Value");
    objectObjectMap.clear();
    assertEquals(0, objectObjectMap.size());
  }

  /** Method under test: {@link SoftHashMap#entrySet()} */
  @Test
  void testEntrySet() {
    SoftHashMap<Object, Object> objectObjectMap = new SoftHashMap<>();
    assertTrue(objectObjectMap.entrySet().isEmpty());

    objectObjectMap.put("Key", "Value");
    Set<Entry<Object, Object>> set = objectObjectMap.entrySet();
    assertEquals(1, set.size());
    Entry<Object, Object> entry = set.stream().findFirst().get();
    assertEquals("Key", entry.getKey());
    assertEquals("Value", entry.getValue());
    assertEquals("Value", entry.setValue(null));
  }

  /** Method under test: {@link SoftHashMap#containsKey(Object)} */
  @Test
  void testContainsKey() {
    SoftHashMap<Object, Object> objectObjectMap = new SoftHashMap<>();
    assertFalse(objectObjectMap.containsKey("Key"));

    objectObjectMap.put("Key", "Value");
    assertTrue(objectObjectMap.containsKey("Key"));

    objectObjectMap.put("Key", null);
    assertFalse(objectObjectMap.containsKey("Key"));
  }
}
