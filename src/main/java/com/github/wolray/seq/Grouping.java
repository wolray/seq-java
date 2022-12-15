package com.github.wolray.seq;

import java.util.Map;
import java.util.function.*;

/**
 * @author wolray
 */
public class Grouping<T, K> {
    private final Seq<T> empty = Seq.empty(), seq;
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

    private <E> Map<K, E> makeMap() {
        return SeqMap.makeMap(10, mapClass);
    }

    public <C> SeqMap<K, C> then(BiFunction<K, SeqList<T>, C> mapper) {
        return toObj(Seq::toList).replaceValues((k, v) -> mapper.apply(k, v.get()));
    }

    public <C> SeqMap<K, C> then(Function<SeqList<T>, C> mapper) {
        return toObj(Seq::toList).replaceValues(v -> mapper.apply(v.get()));
    }

    public <V> SeqMap<K, Supplier<V>> toObj(Seq.FeederFunction<T, V> feederFunction) {
        Map<K, Supplier<V>> map = makeMap();
        seq.eval(t -> {
            K k = function.apply(t);
            Supplier<V> feeder = map.computeIfAbsent(k, it -> feederFunction.toFeeder());
            ((Seq.Feeder<V, T>)feeder).accept(t);
        });
        return new SeqMap<>(map);
    }

    @SuppressWarnings("unchecked")
    public SeqMap<K, IntSupplier> toInt(Seq.IntFeederFunction<T> feederFunction) {
        Map<K, IntSupplier> map = makeMap();
        seq.eval(t -> {
            K k = function.apply(t);
            IntSupplier feeder = map.computeIfAbsent(k, it -> feederFunction.toFeeder());
            ((Seq.IntFeeder<T>)feeder).accept(t);
        });
        return new SeqMap<>(map);
    }

    @SuppressWarnings("unchecked")
    public SeqMap<K, DoubleSupplier> toDouble(Seq.DoubleFeederFunction<T> feederFunction) {
        Map<K, DoubleSupplier> map = makeMap();
        seq.eval(t -> {
            K k = function.apply(t);
            DoubleSupplier feeder = map.computeIfAbsent(k, it -> feederFunction.toFeeder());
            ((Seq.DoubleFeeder<T>)feeder).accept(t);
        });
        return new SeqMap<>(map);
    }

    @SuppressWarnings("unchecked")
    public SeqMap<K, LongSupplier> toLong(Seq.LongFeederFunction<T> feederFunction) {
        Map<K, LongSupplier> map = makeMap();
        seq.eval(t -> {
            K k = function.apply(t);
            LongSupplier feeder = map.computeIfAbsent(k, it -> feederFunction.toFeeder());
            ((Seq.LongFeeder<T>)feeder).accept(t);
        });
        return new SeqMap<>(map);
    }

    @SuppressWarnings("unchecked")
    public SeqMap<K, BooleanSupplier> toBool(Seq.BoolFeederFunction<T> feederFunction) {
        Map<K, BooleanSupplier> map = makeMap();
        seq.eval(t -> {
            K k = function.apply(t);
            BooleanSupplier feeder = map.computeIfAbsent(k, it -> feederFunction.toFeeder());
            ((Seq.BoolFeeder<T>)feeder).accept(t);
        });
        return new SeqMap<>(map);
    }
}
