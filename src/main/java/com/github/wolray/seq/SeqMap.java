package com.github.wolray.seq;

import java.util.*;

/**
 * @author wolray
 */
public class SeqMap<K, V> extends BackedSeq<Map.Entry<K, V>, Set<Map.Entry<K, V>>> implements Map<K, V> {
    public final Map<K, V> map;

    SeqMap(Map<K, V> map) {
        super(map.entrySet());
        this.map = map;
    }

    public static <K, V> SeqMap<K, V> of(Map<K, V> map) {
        return map instanceof SeqMap ? (SeqMap<K, V>)map : new SeqMap<>(map);
    }

    public static <K, V> SeqMap<K, V> hash() {
        return new SeqMap<>(new HashMap<>());
    }

    public static <K, V> SeqMap<K, V> tree(Comparator<K> comparator) {
        return new SeqMap<>(new TreeMap<>(comparator));
    }

    @Override
    public SeqSet<K> keySet() {
        return new SeqSet<>(map.keySet());
    }

    public Seq<V> valueSeq() {
        return map.values()::forEach;
    }

    @Override
    public String toString() {
        return backer.toString();
    }

    @Override
    public int size() {
        return backer.size();
    }

    @Override
    public boolean isEmpty() {
        return backer.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        backer.clear();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }
}
