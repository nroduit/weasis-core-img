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

import java.io.Serial;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A map implementation that uses {@link SoftReference} to allow values to be garbage collected when
 * memory is low. The map entries are cleared when the associated {@link SoftReference} is cleared
 * by the garbage collector.
 *
 * <p>This implementation is not thread-safe. If multiple threads access this map concurrently, it
 * must be synchronized externally.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of values held by this map (must not be null)
 * @author Nicolas Roduit
 */
public final class SoftHashMap<K, V> extends AbstractMap<K, V> implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private final transient Map<K, SoftReference<V>> primaryMap = new HashMap<>();
  private final transient Map<SoftReference<V>, K> reverseLookup = new HashMap<>();
  private final transient ReferenceQueue<V> referenceQueue = new ReferenceQueue<>();

  @Override
  public V get(Object key) {
    if (key == null) {
      return null;
    }
    expungeStaleEntries();
    var softRef = primaryMap.get(key);
    if (softRef == null) {
      return null;
    }

    var result = softRef.get();
    if (result == null) {
      removeStaleReference(softRef);
    }
    return result;
  }

  @Override
  public V put(K key, V value) {
    Objects.requireNonNull(key);
    if (value == null) {
      return remove(key);
    }
    expungeStaleEntries();
    var oldValue = removeExistingMapping(key);
    addNewMapping(key, value);
    return oldValue;
  }

  @Override
  public V remove(Object key) {
    if (key == null) {
      return null;
    }
    expungeStaleEntries();
    var removedRef = primaryMap.remove(key);
    if (removedRef == null) {
      return null;
    }

    reverseLookup.remove(removedRef);
    return removedRef.get();
  }

  @Override
  public void clear() {
    primaryMap.clear();
    reverseLookup.clear();
    drainReferenceQueue();
  }

  @Override
  public int size() {
    expungeStaleEntries();
    return primaryMap.size();
  }

  @Override
  public boolean isEmpty() {
    expungeStaleEntries();
    return primaryMap.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    if (key == null) {
      return false;
    }
    expungeStaleEntries();
    return primaryMap.containsKey(key);
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    expungeStaleEntries();
    return primaryMap.entrySet().stream()
        .map(
            entry -> {
              var key = entry.getKey();
              var value = entry.getValue().get();
              return key != null && value != null ? Map.entry(key, value) : null;
            })
        .filter(Objects::nonNull)
        .map(entry -> new SoftEntry<>(entry.getKey(), entry.getValue(), this))
        .collect(LinkedHashSet::new, Set::add, Set::addAll);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof SoftHashMap<?, ?> other)) return false;
    expungeStaleEntries();
    other.expungeStaleEntries();

    if (primaryMap.size() != other.primaryMap.size()) {
      return false;
    }

    return primaryMap.entrySet().stream()
        .allMatch(
            entry -> {
              var value = entry.getValue().get();
              return value != null && Objects.equals(value, other.get(entry.getKey()));
            });
  }

  @Override
  public int hashCode() {
    expungeStaleEntries();
    return primaryMap.entrySet().stream()
        .mapToInt(
            entry -> {
              var value = entry.getValue().get();
              return value != null ? Objects.hashCode(entry.getKey()) ^ Objects.hashCode(value) : 0;
            })
        .sum();
  }

  private V removeExistingMapping(K key) {
    var oldRef = primaryMap.get(key);
    if (oldRef == null) {
      return null;
    }
    var oldValue = oldRef.get();
    reverseLookup.remove(oldRef);
    return oldValue;
  }

  private void addNewMapping(K key, V value) {
    var softRef = new SoftReference<>(value, referenceQueue);
    primaryMap.put(key, softRef);
    reverseLookup.put(softRef, key);
  }

  private void removeStaleReference(Reference<? extends V> staleRef) {
    // Safe cast: we only put SoftReference instances in the reverseLookup map
    @SuppressWarnings("unchecked")
    var softRef = (SoftReference<V>) staleRef;
    var key = reverseLookup.remove(softRef);
    if (key != null) {
      primaryMap.remove(key);
    }
  }

  private void expungeStaleEntries() {
    Reference<? extends V> staleRef;
    while ((staleRef = referenceQueue.poll()) != null) {
      removeStaleReference(staleRef);
    }
  }

  private void drainReferenceQueue() {
    while (referenceQueue.poll() != null) {
      // Intentionally empty - just drain the queue
    }
  }

  /** Entry implementation that delegates setValue operations to the parent map. */
  private record SoftEntry<K, V>(K key, V value, SoftHashMap<K, V> parentMap)
      implements Entry<K, V> {

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public V setValue(V newValue) {
      return parentMap.put(key, newValue);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Entry<?, ?> entry
          && Objects.equals(key, entry.getKey())
          && Objects.equals(value, entry.getValue());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(key) ^ Objects.hashCode(value);
    }
  }
}
