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
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of values held by this map
 * @author Nicolas Roduit
 */
public class SoftHashMap<K, V> extends AbstractMap<K, V> implements Serializable {

  /** The internal HashMap that will hold the SoftReference. */
  private final transient Map<K, SoftReference<V>> hash = new HashMap<>();

  private final transient Map<SoftReference<V>, K> reverseLookup = new HashMap<>();

  /** Reference queue for cleared SoftReference objects. */
  private final transient ReferenceQueue<V> queue = new ReferenceQueue<>();

  @Override
  public V get(Object key) {
    expungeStaleEntries();
    SoftReference<V> softRef = hash.get(key);
    if (softRef != null) {
      V result = softRef.get();
      if (result == null) {
        // If the value has been garbage collected, remove the entry from the HashMap.
        removeElement(softRef);
      }
      return result;
    }

    return null;
  }

  @Override
  public V put(K key, V value) {
    if (value == null) {
      return remove(key);
    }
    expungeStaleEntries();
    // Remove existing mapping if present
    SoftReference<V> oldRef = hash.get(key);
    V oldValue = null;
    if (oldRef != null) {
      oldValue = oldRef.get();
      reverseLookup.remove(oldRef);
    }

    // Add new mapping
    SoftReference<V> softRef = new SoftReference<>(value, queue);
    hash.put(key, softRef);
    reverseLookup.put(softRef, key);
    return oldValue;
  }

  @Override
  public V remove(Object key) {
    expungeStaleEntries();
    SoftReference<V> result = hash.remove(key);
    if (result != null) {
      reverseLookup.remove(result);
      return result.get();
    }
    return null;
  }

  @Override
  public void clear() {
    hash.clear();
    reverseLookup.clear();
    // Clear the reference queue as well
    while (queue.poll() != null) {
      // Drain the queue
    }
  }

  @Override
  public int size() {
    expungeStaleEntries();
    return hash.size();
  }

  @Override
  public boolean isEmpty() {
    expungeStaleEntries();
    return hash.isEmpty();
  }

  @Override
  public boolean containsValue(Object value) {
    if (value == null) {
      return false;
    }
    expungeStaleEntries();
    for (SoftReference<V> softRef : hash.values()) {
      V v = softRef.get();
      if (v != null && v.equals(value)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a copy of the key/values in the map at the point of calling. However, setValue still
   * sets the value in the actual SoftHashMap.
   */
  @Override
  public Set<Entry<K, V>> entrySet() {
    expungeStaleEntries();
    Set<Entry<K, V>> result = new LinkedHashSet<>();
    for (final Entry<K, SoftReference<V>> entry : hash.entrySet()) {
      final V value = entry.getValue().get();
      if (value != null) {
        result.add(new SoftEntry<>(entry.getKey(), value, this));
      }
    }
    return result;
  }

  /** Removes an element from both maps based on the soft reference. */
  @SuppressWarnings("SuspiciousMethodCalls")
  private void removeElement(Reference<? extends V> soft) {
    K key = reverseLookup.remove(soft);
    if (key != null) {
      hash.remove(key);
    }
  }

  /** Removes stale entries from the map by processing references queued for garbage collection. */
  private void expungeStaleEntries() {
    Reference<? extends V> sv;
    while ((sv = queue.poll()) != null) {
      removeElement(sv);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SoftHashMap<?, ?> that)) {
      return false;
    }
    expungeStaleEntries();
    that.expungeStaleEntries();

    if (hash.size() != that.hash.size()) {
      return false;
    }

    for (Entry<K, SoftReference<V>> entry : hash.entrySet()) {
      var value = entry.getValue().get();
      if (value == null) {
        return false;
      }

      var otherValue = that.get(entry.getKey());
      if (!Objects.equals(value, otherValue)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public int hashCode() {
    expungeStaleEntries();
    int hashCode = 0;
    for (Entry<K, SoftReference<V>> entry : hash.entrySet()) {
      V value = entry.getValue().get();
      if (value != null) {
        K key = entry.getKey();
        hashCode += Objects.hashCode(key) ^ Objects.hashCode(value);
      }
    }
    return hashCode;
  }

  /** A custom Entry implementation for the SoftHashMap. */
  private record SoftEntry<K, V>(K key, V value, SoftHashMap<K, V> map) implements Entry<K, V> {

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
      return map.put(key, newValue);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Entry<?, ?> entry)) {
        return false;
      }
      return (key == null ? entry.getKey() == null : key.equals(entry.getKey()))
          && (value == null ? entry.getValue() == null : value.equals(entry.getValue()));
    }

    @Override
    public int hashCode() {
      return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
    }
  }
}
