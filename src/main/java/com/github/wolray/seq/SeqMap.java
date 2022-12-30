package com.github.wolray.seq;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author wolray
 */
public class SeqMap<K, V> extends BackedSeq<Map.Entry<K, V>, Set<Map.Entry<K, V>>> implements Map<K, V> {
    public final Map<K, V> map;

    SeqMap(Map<K, V> map) {
        super(map.entrySet());
        this.map = map;
    }

    @Override
    public SeqSet<K> keySet() {
        return new SeqSet<>(map.keySet());
    }

    @Override
    public Values<V> values() {
        return new Values<>(map.values());
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

    public static <K, V> Map<K, V> newMap(Map<?, ?> map) {
        if (map instanceof LinkedHashMap) {
            return new LinkedHashMap<>(map.size());
        }
        if (map instanceof HashMap) {
            return new HashMap<>(map.size());
        }
        if (map instanceof TreeMap) {
            return new TreeMap<>();
        }
        if (map instanceof ConcurrentHashMap) {
            return new ConcurrentHashMap<>(map.size());
        }
        return new HashMap<>(map.size());
    }

    public <E> SeqMap<E, V> mapByKey(BiFunction<K, V, E> function) {
        return new SeqMap<>(toMap(newMap(map), e -> function.apply(e.getKey(), e.getValue()), Map.Entry::getValue));
    }

    public <E> SeqMap<E, V> mapByKey(Function<K, E> function) {
        return new SeqMap<>(toMap(newMap(map), e -> function.apply(e.getKey()), Map.Entry::getValue));
    }

    public <E> SeqMap<K, E> mapByValue(BiFunction<K, V, E> function) {
        return new SeqMap<>(toMap(newMap(map), Map.Entry::getKey, e -> function.apply(e.getKey(), e.getValue())));
    }

    public <E> SeqMap<K, E> mapByValue(Function<V, E> function) {
        return new SeqMap<>(toMap(newMap(map), Map.Entry::getKey, e -> function.apply(e.getValue())));
    }

    @SuppressWarnings("unchecked")
    public <E> SeqMap<K, E> replaceValue(BiFunction<K, V, E> function) {
        SeqMap<K, Object> map = (SeqMap<K, Object>)this;
        map.supply(e -> e.setValue(function.apply(e.getKey(), (V)e.getValue())));
        return (SeqMap<K, E>)map;
    }

    @SuppressWarnings("unchecked")
    public <E> SeqMap<K, E> replaceValue(Function<V, E> function) {
        SeqMap<K, Object> map = (SeqMap<K, Object>)this;
        map.supply(e -> e.setValue(function.apply((V)e.getValue())));
        return (SeqMap<K, E>)map;
    }

    @Override
    public String toString() {
        return map.toString();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
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
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    public static class Values<T> extends SeqCollection<T, Collection<T>> {
        public Values(Collection<T> backer) {
            super(backer);
        }
    }
}
