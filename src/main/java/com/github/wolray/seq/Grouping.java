package com.github.wolray.seq;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author wolray
 */
public class Grouping<T, K> {
    private final Seq<T> seq;
    private final Function<T, K> function;
    private Class<?> mapClass;

    public Grouping(Seq<T> seq, Function<T, K> function) {
        this.seq = seq;
        this.function = function;
    }

    @SuppressWarnings("rawtypes")
    public Grouping<T, K> destination(Class<? extends Map> mapClass) {
        this.mapClass = mapClass;
        return this;
    }

    public <V> SeqMap<K, Supplier<V>> eval(Seq.ToFolder<T, V> toFolder) {
        return eval(10, toFolder);
    }

    public <V> SeqMap<K, Supplier<V>> eval(int groupSize, Seq.ToFolder<T, V> toFolder) {
        Map<K, Supplier<V>> map = SeqMap.makeMap(groupSize, mapClass);
        seq.eval(t -> {
            K k = function.apply(t);
            Supplier<V> folder = map.computeIfAbsent(k, it -> toFolder.empty());
            ((Seq.Folder<V, T>)folder).accept(t);
        });
        return new SeqMap<>(map);
    }
}
