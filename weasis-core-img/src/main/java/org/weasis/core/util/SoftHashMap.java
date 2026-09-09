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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A map implementation that uses {@link SoftReference} to allow values to be garbage collected when
 * memory is low. The map entries are cleared when the associated {@link SoftReference} is cleared
 * by the garbage collector.
 *
 * <p>This implementation is thread-safe: every operation is guarded by an internal lock. Compound
 * actions performed through the {@link #entrySet()} view are not atomic, as the view iterates over
 * a snapshot of the reachable entries.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of values held by this map (must not be null)
 * @author Nicolas Roduit
 */
public final class SoftHashMap<K, V> extends AbstractMap<K, V> {

  private final Object lock = new Object();
  private final Map<K, SoftReference<V>> primaryMap = new HashMap<>();
  private final Map<SoftReference<V>, K> reverseLookup = new HashMap<>();
  private final ReferenceQueue<V> referenceQueue = new ReferenceQueue<>();

  @Override
  public V get(Object key) {
    if (key == null) {
      return null;
    }
    synchronized (lock) {
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
  }

  @Override
  public V put(K key, V value) {
    Objects.requireNonNull(key);
    if (value == null) {
      return remove(key);
    }
    synchronized (lock) {
      expungeStaleEntries();
      var oldValue = removeExistingMapping(key);
      addNewMapping(key, value);
      return oldValue;
    }
  }

  @Override
  public V remove(Object key) {
    if (key == null) {
      return null;
    }
    synchronized (lock) {
      expungeStaleEntries();
      var removedRef = primaryMap.remove(key);
      if (removedRef == null) {
        return null;
      }

      reverseLookup.remove(removedRef);
      return removedRef.get();
    }
  }

  @Override
  public void clear() {
    synchronized (lock) {
      primaryMap.clear();
      reverseLookup.clear();
      drainReferenceQueue();
    }
  }

  @Override
  public int size() {
    synchronized (lock) {
      expungeStaleEntries();
      return primaryMap.size();
    }
  }

  @Override
  public boolean isEmpty() {
    synchronized (lock) {
      expungeStaleEntries();
      return primaryMap.isEmpty();
    }
  }

  @Override
  public boolean containsKey(Object key) {
    if (key == null) {
      return false;
    }
    synchronized (lock) {
      expungeStaleEntries();
      return primaryMap.containsKey(key);
    }
  }

  /** A view backed by the map: removals through it, its iterator, keySet() or values() apply. */
  @Override
  public Set<Entry<K, V>> entrySet() {
    return new AbstractSet<>() {
      @Override
      public Iterator<Entry<K, V>> iterator() {
        return new EntryIterator();
      }

      @Override
      public int size() {
        return SoftHashMap.this.size();
      }

      @Override
      public void clear() {
        SoftHashMap.this.clear();
      }
    };
  }

  // Strong snapshot of the reachable entries, so values cannot vanish during an iteration
  private List<Entry<K, V>> liveEntries() {
    synchronized (lock) {
      expungeStaleEntries();
      var entries = new ArrayList<Entry<K, V>>(primaryMap.size());
      primaryMap.forEach(
          (key, ref) -> {
            var value = ref.get();
            if (value != null) {
              entries.add(new SoftEntry<>(key, value, this));
            }
          });
      return entries;
    }
  }

  private final class EntryIterator implements Iterator<Entry<K, V>> {
    private final Iterator<Entry<K, V>> snapshot = liveEntries().iterator();
    private Entry<K, V> current;

    @Override
    public boolean hasNext() {
      return snapshot.hasNext();
    }

    @Override
    public Entry<K, V> next() {
      current = snapshot.next();
      return current;
    }

    @Override
    public void remove() {
      if (current == null) {
        throw new IllegalStateException();
      }
      SoftHashMap.this.remove(current.getKey());
      current = null;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof SoftHashMap<?, ?> other)) return false;
    // Compare snapshots so that only one lock is ever held at a time
    var entries = liveEntries();
    var otherEntries = other.liveEntries();
    if (entries.size() != otherEntries.size()) {
      return false;
    }
    return entries.stream()
        .allMatch(entry -> Objects.equals(entry.getValue(), other.get(entry.getKey())));
  }

  @Override
  public int hashCode() {
    return liveEntries().stream().mapToInt(Entry::hashCode).sum();
  }

  // The helpers below touch the two maps directly: callers must hold the lock.

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
