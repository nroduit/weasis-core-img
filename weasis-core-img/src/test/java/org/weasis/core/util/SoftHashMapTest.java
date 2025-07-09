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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SoftHashMap Tests")
class SoftHashMapTest {

  private static final String TEST_KEY = "TestKey";
  private static final String TEST_VALUE = "TestValue";
  private static final String ANOTHER_KEY = "AnotherKey";
  private static final String ANOTHER_VALUE = "AnotherValue";

  private SoftHashMap<String, String> softHashMap;

  @BeforeEach
  void setUp() {
    softHashMap = new SoftHashMap<>();
  }

  @Nested
  @DisplayName("Basic Operations")
  class BasicOperationsTests {

    @Test
    @DisplayName("Should return null when getting non-existent key")
    void shouldReturnNullWhenGettingNonExistentKey() {
      assertNull(softHashMap.get(TEST_KEY));
    }

    @Test
    @DisplayName("Should store and retrieve values correctly")
    void shouldStoreAndRetrieveValuesCorrectly() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      assertEquals(TEST_VALUE, softHashMap.get(TEST_KEY));
    }

    @Test
    @DisplayName("Should remove entry when putting null value")
    void shouldRemoveEntryWhenPuttingNullValue() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(TEST_KEY, null);

      assertNull(softHashMap.get(TEST_KEY));
      assertEquals(0, softHashMap.size());
    }

    @Test
    @DisplayName("Should return null when removing non-existent key")
    void shouldReturnNullWhenRemovingNonExistentKey() {
      assertNull(softHashMap.remove(TEST_KEY));
    }

    @Test
    @DisplayName("Should remove and return existing value")
    void shouldRemoveAndReturnExistingValue() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      assertEquals(TEST_VALUE, softHashMap.remove(TEST_KEY));
      assertNull(softHashMap.get(TEST_KEY));
    }

    @Test
    @DisplayName("Should handle null keys")
    void shouldHandleNullKeys() {
      softHashMap.put(null, TEST_VALUE);
      assertEquals(TEST_VALUE, softHashMap.get(null));
      assertTrue(softHashMap.containsKey(null));

      assertEquals(TEST_VALUE, softHashMap.remove(null));
      assertNull(softHashMap.get(null));
      assertFalse(softHashMap.containsKey(null));
    }

    @Test
    @DisplayName("Should overwrite existing values")
    void shouldOverwriteExistingValues() {
      String oldValue = softHashMap.put(TEST_KEY, TEST_VALUE);
      assertNull(oldValue);

      String overwrittenValue = softHashMap.put(TEST_KEY, ANOTHER_VALUE);
      assertEquals(TEST_VALUE, overwrittenValue);
      assertEquals(ANOTHER_VALUE, softHashMap.get(TEST_KEY));
      assertEquals(1, softHashMap.size());
    }
  }

  @Nested
  @DisplayName("Clear and remove Operations")
  class ClearOperationsTests {

    @Test
    @DisplayName("Should clear all entries from map")
    void shouldClearAllEntriesFromMap() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);
      assertEquals(2, softHashMap.size());

      softHashMap.clear();

      assertEquals(0, softHashMap.size());
      assertTrue(softHashMap.isEmpty());
    }

    @Test
    @DisplayName("Should handle clear on empty map")
    void shouldHandleClearOnEmptyMap() {
      softHashMap.clear();
      assertEquals(0, softHashMap.size());
      assertTrue(softHashMap.isEmpty());
    }

    private static void runGarbageCollectorAndWait(long ms) {
      System.gc();
      System.gc();
      try {
        TimeUnit.MILLISECONDS.sleep(ms);
      } catch (InterruptedException et) {
        Thread.currentThread().interrupt();
      }
    }

    @Test
    @DisplayName("Should maintain internal consistency when removeElement processes stale entries")
    void shouldMaintainInternalConsistencyAfterRemoveElement() {
      // Add entries
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);
      softHashMap.put("key3", "value3");
      softHashMap.put("key4", "value4");

      int initialSize = softHashMap.size();

      // Repeatedly trigger expungeStaleEntries through various operations
      // This exercises the removeElement method path
      for (int i = 0; i < 10; i++) {
        runGarbageCollectorAndWait(5);
        softHashMap.size(); // calls expungeStaleEntries
        softHashMap.isEmpty(); // calls expungeStaleEntries
        softHashMap.containsValue(TEST_VALUE); // calls expungeStaleEntries
        softHashMap.entrySet(); // calls expungeStaleEntries
        softHashMap.get(TEST_KEY); // calls expungeStaleEntries and potentially removeElement
      }

      // Verify consistency between different views of the map
      assertEquals(softHashMap.size(), softHashMap.entrySet().size());
      assertEquals(softHashMap.size(), softHashMap.keySet().size());
      assertEquals(softHashMap.isEmpty(), softHashMap.size() == 0);

      // Verify that any remaining keys still have their values
      for (String key : softHashMap.keySet()) {
        String value = softHashMap.get(key);
        assertNotNull(value, "Value should not be null for key: " + key);
        assertTrue(softHashMap.containsKey(key));
        assertTrue(softHashMap.containsValue(value));
      }

      // Size should never exceed initial size after cleanup
      assertTrue(softHashMap.size() <= initialSize);
    }

    @Test
    @DisplayName("Should expunge stale entries from reference queue")
    void shouldExpungeStaleEntriesFromQueue() throws Exception {
      // This is the most reliable approach using reflection
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);

      // Access private fields
      Field hashField = SoftHashMap.class.getDeclaredField("hash");
      hashField.setAccessible(true);

      @SuppressWarnings("unchecked")
      Map<String, SoftReference<String>> hash =
          (Map<String, SoftReference<String>>) hashField.get(softHashMap);

      // Get reference to manipulate
      SoftReference<String> ref = hash.get(TEST_KEY);

      // Simulate GC clearing the reference
      ref.clear();
      ref.enqueue();

      softHashMap.isEmpty(); // This should trigger expungeStaleEntries

      // After expunge - stale entry should be removed
      assertEquals(1, softHashMap.size());
      assertNull(softHashMap.get(TEST_KEY));
      assertEquals(ANOTHER_VALUE, softHashMap.get(ANOTHER_KEY));
    }
  }

  @Nested
  @DisplayName("Size and Empty Operations")
  class SizeAndEmptyOperationsTests {

    @Test
    @DisplayName("Should be empty when newly created")
    void shouldBeEmptyWhenNewlyCreated() {
      assertTrue(softHashMap.isEmpty());
      assertEquals(0, softHashMap.size());
    }

    @Test
    @DisplayName("Should track size correctly when adding entries")
    void shouldTrackSizeCorrectlyWhenAddingEntries() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      assertFalse(softHashMap.isEmpty());
      assertEquals(1, softHashMap.size());

      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);
      assertEquals(2, softHashMap.size());
    }

    @Test
    @DisplayName("Should update size when removing entries")
    void shouldUpdateSizeWhenRemovingEntries() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);
      assertEquals(2, softHashMap.size());

      softHashMap.remove(TEST_KEY);
      assertEquals(1, softHashMap.size());
    }
  }

  @Nested
  @DisplayName("Contains Operations")
  class ContainsOperationsTests {

    @Test
    @DisplayName("Should not contain key when empty")
    void shouldNotContainKeyWhenEmpty() {
      assertFalse(softHashMap.containsKey(TEST_KEY));
    }

    @Test
    @DisplayName("Should contain key after putting value")
    void shouldContainKeyAfterPuttingValue() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      assertTrue(softHashMap.containsKey(TEST_KEY));
    }

    @Test
    @DisplayName("Should not contain key after putting null value")
    void shouldNotContainKeyAfterPuttingNullValue() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(TEST_KEY, null);
      assertFalse(softHashMap.containsKey(TEST_KEY));
    }

    @Test
    @DisplayName("Should not contain value when empty")
    void shouldNotContainValueWhenEmpty() {
      assertFalse(softHashMap.containsValue(TEST_VALUE));
      assertFalse(softHashMap.containsValue(null));
    }

    @Test
    @DisplayName("Should contain value after putting it")
    void shouldContainValueAfterPuttingIt() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      assertTrue(softHashMap.containsValue(TEST_VALUE));
      assertFalse(softHashMap.containsValue(ANOTHER_VALUE));
    }

    @Test
    @DisplayName("Should not contain value after removing it")
    void shouldNotContainValueAfterRemovingIt() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.remove(TEST_KEY);
      assertFalse(softHashMap.containsValue(TEST_VALUE));
    }

    @Test
    @DisplayName("Should handle containsKey with null key")
    void shouldHandleContainsKeyWithNullKey() {
      assertFalse(softHashMap.containsKey(null));

      softHashMap.put(null, TEST_VALUE);
      assertTrue(softHashMap.containsKey(null));
    }

    @Test
    @DisplayName("Should handle containsValue with duplicate values")
    void shouldHandleContainsValueWithDuplicateValues() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(ANOTHER_KEY, TEST_VALUE);

      assertTrue(softHashMap.containsValue(TEST_VALUE));

      softHashMap.remove(TEST_KEY);
      assertTrue(softHashMap.containsValue(TEST_VALUE)); // Still contained in ANOTHER_KEY

      softHashMap.remove(ANOTHER_KEY);
      assertFalse(softHashMap.containsValue(TEST_VALUE));
    }
  }

  @Nested
  @DisplayName("EntrySet Operations")
  class EntrySetOperationsTests {

    @Test
    @DisplayName("Should return empty entry set when map is empty")
    void shouldReturnEmptyEntrySetWhenMapIsEmpty() {
      assertTrue(softHashMap.entrySet().isEmpty());
    }

    @Test
    @DisplayName("Should return correct entry set with one entry")
    void shouldReturnCorrectEntrySetWithOneEntry() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      Set<Entry<String, String>> entrySet = softHashMap.entrySet();

      assertEquals(1, entrySet.size());
      Entry<String, String> entry = entrySet.stream().findFirst().orElseThrow();
      assertEquals(TEST_KEY, entry.getKey());
      assertEquals(TEST_VALUE, entry.getValue());
    }

    @Test
    @DisplayName("Should allow value modification through entry")
    void shouldAllowValueModificationThroughEntry() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      Set<Entry<String, String>> entrySet = softHashMap.entrySet();
      Entry<String, String> entry = entrySet.stream().findFirst().orElseThrow();

      assertEquals(TEST_VALUE, entry.setValue(null));
      assertNull(softHashMap.get(TEST_KEY));
    }

    @Test
    @DisplayName("Should return entry set with multiple entries")
    void shouldReturnEntrySetWithMultipleEntries() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);
      Set<Entry<String, String>> entrySet = softHashMap.entrySet();

      assertEquals(2, entrySet.size());
    }

    @Test
    @DisplayName("Should handle entry set modification through setValue")
    void shouldHandleEntrySetModificationThroughSetValue() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);

      Set<Entry<String, String>> entrySet = softHashMap.entrySet();
      for (Entry<String, String> entry : entrySet) {
        if (entry.getKey().equals(TEST_KEY)) {
          String oldValue = entry.setValue("NewValue");
          assertEquals(TEST_VALUE, oldValue);
          break;
        }
      }

      assertEquals("NewValue", softHashMap.get(TEST_KEY));
      assertEquals(ANOTHER_VALUE, softHashMap.get(ANOTHER_KEY));
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Operations")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("Should be equal to itself")
    void shouldBeEqualToItself() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      assertEquals(softHashMap, softHashMap);
    }

    @Test
    @DisplayName("Should be equal to another SoftHashMap with same content")
    void shouldBeEqualToAnotherSoftHashMapWithSameContent() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);

      SoftHashMap<String, String> otherMap = new SoftHashMap<>();
      otherMap.put(TEST_KEY, TEST_VALUE);
      otherMap.put(ANOTHER_KEY, ANOTHER_VALUE);

      assertEquals(softHashMap, otherMap);
      assertEquals(softHashMap.hashCode(), otherMap.hashCode());
    }

    @Test
    @DisplayName("Should not be equal to SoftHashMap with different content")
    void shouldNotBeEqualToSoftHashMapWithDifferentContent() {
      softHashMap.put(TEST_KEY, TEST_VALUE);

      SoftHashMap<String, String> otherMap = new SoftHashMap<>();
      otherMap.put(TEST_KEY, ANOTHER_VALUE);

      assertNotEquals(softHashMap, otherMap);
    }

    @Test
    @DisplayName("Should not be equal to SoftHashMap with different size")
    void shouldNotBeEqualToSoftHashMapWithDifferentSize() {
      softHashMap.put(TEST_KEY, TEST_VALUE);

      SoftHashMap<String, String> otherMap = new SoftHashMap<>();
      otherMap.put(TEST_KEY, TEST_VALUE);
      otherMap.put(ANOTHER_KEY, ANOTHER_VALUE);

      assertNotEquals(softHashMap, otherMap);
    }

    @Test
    @DisplayName("Should not be equal to null")
    void shouldNotBeEqualToNull() {
      assertNotEquals(softHashMap, null);
    }

    @Test
    @DisplayName("Should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      Map<String, String> regularMap = new HashMap<>();
      regularMap.put(TEST_KEY, TEST_VALUE);

      softHashMap.put(TEST_KEY, TEST_VALUE);

      assertNotEquals(softHashMap, regularMap);
    }

    @Test
    @DisplayName("Should have consistent hashCode for empty maps")
    void shouldHaveConsistentHashCodeForEmptyMaps() {
      assertEquals(softHashMap.hashCode(), new SoftHashMap<>().hashCode());
    }

    @Test
    @DisplayName("Should handle null keys in equals and hashCode")
    void shouldHandleNullKeysInEqualsAndHashCode() {
      softHashMap.put(null, TEST_VALUE);

      SoftHashMap<String, String> otherMap = new SoftHashMap<>();
      otherMap.put(null, TEST_VALUE);

      assertEquals(softHashMap, otherMap);
      assertEquals(softHashMap.hashCode(), otherMap.hashCode());
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle multiple operations on same key")
    void shouldHandleMultipleOperationsOnSameKey() {
      // Put, get, update, remove cycle
      assertNull(softHashMap.put(TEST_KEY, TEST_VALUE));
      assertEquals(TEST_VALUE, softHashMap.get(TEST_KEY));
      assertEquals(TEST_VALUE, softHashMap.put(TEST_KEY, ANOTHER_VALUE));
      assertEquals(ANOTHER_VALUE, softHashMap.get(TEST_KEY));
      assertEquals(ANOTHER_VALUE, softHashMap.remove(TEST_KEY));
      assertNull(softHashMap.get(TEST_KEY));
    }

    @Test
    @DisplayName("Should handle operations after clear")
    void shouldHandleOperationsAfterClear() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);

      softHashMap.clear();

      // Verify all operations work after clear
      assertNull(softHashMap.get(TEST_KEY));
      assertFalse(softHashMap.containsKey(TEST_KEY));
      assertFalse(softHashMap.containsValue(TEST_VALUE));
      assertTrue(softHashMap.isEmpty());
      assertEquals(0, softHashMap.size());
      assertTrue(softHashMap.entrySet().isEmpty());

      // Should be able to add new entries
      softHashMap.put(TEST_KEY, TEST_VALUE);
      assertEquals(1, softHashMap.size());
      assertEquals(TEST_VALUE, softHashMap.get(TEST_KEY));
    }

    @Test
    @DisplayName("Should handle concurrent modifications of entry set")
    void shouldHandleEntrySetOperations() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);

      Set<Entry<String, String>> entrySet1 = softHashMap.entrySet();
      Set<Entry<String, String>> entrySet2 = softHashMap.entrySet();

      // Entry sets should be independent snapshots
      assertEquals(entrySet1.size(), entrySet2.size());

      // Modifying the map after getting entry set shouldn't affect the entry set
      int originalSize = entrySet1.size();
      softHashMap.put("NewKey", "NewValue");
      assertEquals(originalSize, entrySet1.size());
    }
  }

  @Nested
  @DisplayName("RemoveElement Operations")
  class RemoveElementOperationsTests {

    @Test
    @DisplayName("Should handle garbage collection and soft reference cleanup")
    void shouldHandleSoftReferenceCleanup() {
      // This test verifies the internal removeElement method behavior
      // by triggering conditions where soft references might be cleared

      // Add multiple entries to the map
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);
      softHashMap.put("key3", "value3");
      softHashMap.put("key4", "value4");

      assertEquals(4, softHashMap.size());
      assertTrue(softHashMap.containsKey(TEST_KEY));
      assertTrue(softHashMap.containsKey(ANOTHER_KEY));

      // Force garbage collection to potentially clear soft references
      // Note: This doesn't guarantee soft references will be cleared,
      // but it exercises the expungeStaleEntries path
      System.gc();
      System.runFinalization();

      // Access the map to trigger expungeStaleEntries
      softHashMap.get(TEST_KEY);

      // Verify the map is still functional
      assertNotNull(softHashMap.get(TEST_KEY));
      assertNotNull(softHashMap.get(ANOTHER_KEY));

      // The size might be reduced if any soft references were cleared
      assertTrue(softHashMap.size() <= 4);
      assertTrue(softHashMap.size() >= 0);
    }

    @Test
    @DisplayName("Should maintain consistency after soft reference cleanup")
    void shouldMaintainConsistencyAfterCleanup() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);

      // Store initial state
      int initialSize = softHashMap.size();

      // Trigger potential cleanup through various operations
      softHashMap.entrySet(); // This calls expungeStaleEntries
      softHashMap.containsValue(TEST_VALUE); // This also calls expungeStaleEntries

      // Verify map consistency
      if (softHashMap.containsKey(TEST_KEY)) {
        assertEquals(TEST_VALUE, softHashMap.get(TEST_KEY));
      }
      if (softHashMap.containsKey(ANOTHER_KEY)) {
        assertEquals(ANOTHER_VALUE, softHashMap.get(ANOTHER_KEY));
      }

      // Size should be consistent with actual content
      assertTrue(softHashMap.size() <= initialSize);
      assertEquals(softHashMap.size(), softHashMap.entrySet().size());
    }

    @Test
    @DisplayName("Should handle removeElement through normal operations")
    void shouldHandleRemoveElementThroughOperations() {
      // The removeElement method is private, but we can test its effects
      // through operations that call expungeStaleEntries

      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);

      assertEquals(2, softHashMap.size());

      // Operations that trigger expungeStaleEntries and potentially removeElement
      softHashMap.size(); // calls expungeStaleEntries
      softHashMap.isEmpty(); // calls expungeStaleEntries
      softHashMap.get(TEST_KEY); // calls expungeStaleEntries
      softHashMap.put("newKey", "newValue"); // calls expungeStaleEntries

      // Verify the map still works correctly
      assertTrue(softHashMap.size() >= 2); // Could be higher due to new entry
      assertNotNull(softHashMap.get(TEST_KEY));
      assertNotNull(softHashMap.get(ANOTHER_KEY));
      assertEquals("newValue", softHashMap.get("newKey"));
    }
  }

  @Nested
  @DisplayName("Enhanced Equals Operations")
  class EnhancedEqualsOperationsTests {

    @Test
    @DisplayName("Should handle equals with maps containing null values")
    void shouldHandleEqualsWithNullValues() {
      // Test equals behavior when both maps have null values
      softHashMap.put(TEST_KEY, null);
      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);

      SoftHashMap<String, String> otherMap = new SoftHashMap<>();
      otherMap.put(TEST_KEY, null);
      otherMap.put(ANOTHER_KEY, ANOTHER_VALUE);

      assertEquals(softHashMap, otherMap);
      assertEquals(softHashMap.hashCode(), otherMap.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when one map has null value and other doesn't")
    void shouldNotBeEqualWithMismatchedNullValues() {
      softHashMap.put(TEST_KEY, TEST_VALUE);

      SoftHashMap<String, String> otherMap = new SoftHashMap<>();
      otherMap.put(TEST_KEY, null);

      assertNotEquals(softHashMap, otherMap);
    }

    @Test
    @DisplayName("Should handle equals with empty maps")
    void shouldHandleEqualsWithEmptyMaps() {
      SoftHashMap<String, String> emptyMap1 = new SoftHashMap<>();
      SoftHashMap<String, String> emptyMap2 = new SoftHashMap<>();

      assertEquals(emptyMap1, emptyMap2);
      assertEquals(emptyMap1.hashCode(), emptyMap2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal to map with subset of entries")
    void shouldNotBeEqualToSubset() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);
      softHashMap.put("key3", "value3");

      SoftHashMap<String, String> subsetMap = new SoftHashMap<>();
      subsetMap.put(TEST_KEY, TEST_VALUE);
      subsetMap.put(ANOTHER_KEY, ANOTHER_VALUE);

      assertNotEquals(softHashMap, subsetMap);
      assertNotEquals(subsetMap, softHashMap);
    }

    @Test
    @DisplayName("Should not be equal to map with superset of entries")
    void shouldNotBeEqualToSuperset() {
      softHashMap.put(TEST_KEY, TEST_VALUE);

      SoftHashMap<String, String> supersetMap = new SoftHashMap<>();
      supersetMap.put(TEST_KEY, TEST_VALUE);
      supersetMap.put(ANOTHER_KEY, ANOTHER_VALUE);
      supersetMap.put("key3", "value3");

      assertNotEquals(softHashMap, supersetMap);
      assertNotEquals(supersetMap, softHashMap);
    }

    @Test
    @DisplayName("Should handle equals with different generic types")
    void shouldHandleEqualsWithDifferentTypes() {
      SoftHashMap<String, Integer> intMap = new SoftHashMap<>();
      intMap.put(TEST_KEY, 42);

      softHashMap.put(TEST_KEY, "42");

      // Even though the string representation might be the same,
      // different value types should not be equal
      assertNotEquals(softHashMap, intMap);
    }

    @Test
    @DisplayName("Should maintain equals contract with hashCode")
    void shouldMaintainEqualsHashCodeContract() {
      softHashMap.put(TEST_KEY, TEST_VALUE);
      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);
      softHashMap.put(null, "nullKeyValue");

      SoftHashMap<String, String> otherMap = new SoftHashMap<>();
      otherMap.put(ANOTHER_KEY, ANOTHER_VALUE);
      otherMap.put(TEST_KEY, TEST_VALUE);
      otherMap.put(null, "nullKeyValue");

      // Order shouldn't matter for equality
      assertEquals(softHashMap, otherMap);
      assertEquals(softHashMap.hashCode(), otherMap.hashCode());

      // Reflexive property
      assertEquals(softHashMap, softHashMap);

      // Symmetric property
      assertEquals(otherMap, softHashMap);

      // Test with third map for transitivity
      SoftHashMap<String, String> thirdMap = new SoftHashMap<>();
      thirdMap.putAll(otherMap);

      assertEquals(softHashMap, thirdMap);
      assertEquals(otherMap, thirdMap);
    }

    @Test
    @DisplayName("Should handle equals after modifications")
    void shouldHandleEqualsAfterModifications() {
      softHashMap.put(TEST_KEY, TEST_VALUE);

      SoftHashMap<String, String> otherMap = new SoftHashMap<>();
      otherMap.put(TEST_KEY, TEST_VALUE);

      assertEquals(softHashMap, otherMap);

      // Modify one map
      softHashMap.put(ANOTHER_KEY, ANOTHER_VALUE);
      assertNotEquals(softHashMap, otherMap);

      // Make them equal again
      otherMap.put(ANOTHER_KEY, ANOTHER_VALUE);
      assertEquals(softHashMap, otherMap);

      // Remove from both
      softHashMap.remove(TEST_KEY);
      otherMap.remove(TEST_KEY);
      assertEquals(softHashMap, otherMap);
    }
  }
}
