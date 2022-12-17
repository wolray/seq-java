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

    @Override
    public SeqSet<K> keySet() {
        return new SeqSet<>(map.keySet());
    }

    public Seq<V> valueSeq() {
        return map.values()::forEach;
    }

    public <E> SeqMap<E, V> mapKey(BiFunction<K, V, E> function) {
        return new SeqMap<>(toMap(newMap(map), e -> function.apply(e.getKey(), e.getValue()), Map.Entry::getValue).eval());
    }

    public <E> SeqMap<E, V> mapKey(Function<K, E> function) {
        return new SeqMap<>(toMap(newMap(map), e -> function.apply(e.getKey()), Map.Entry::getValue).eval());
    }

    public <E> SeqMap<K, E> mapValue(BiFunction<K, V, E> function) {
        return new SeqMap<>(toMap(newMap(map), Map.Entry::getKey, e -> function.apply(e.getKey(), e.getValue())).eval());
    }

    public <E> SeqMap<K, E> mapValue(Function<V, E> function) {
        return new SeqMap<>(toMap(newMap(map), Map.Entry::getKey, e -> function.apply(e.getValue())).eval());
    }

    @SuppressWarnings("unchecked")
    public <E> SeqMap<K, E> replaceValue(Function<V, E> function) {
        SeqMap<K, Object> map = (SeqMap<K, Object>)this;
        map.eval(e -> e.setValue(function.apply((V)e.getValue())));
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
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }
}
