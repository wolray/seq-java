package com.github.wolray.seq;

import java.util.Map;
import java.util.function.*;

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

    private <E> Map<K, E> makeMap() {
        return SeqMap.makeMap(10, mapClass);
    }

//    public <C> SeqMap<K, C> then(BiFunction<K, SeqList<T>, C> mapper) {
//        Supplier<Seq.Feeder<SeqList<T>, T>> toList = Seq::toList;
//        toList()
//        return eval(toList).replaceValues((k, v) -> mapper.apply(k, v.get()));
//    }

//    public <C> SeqMap<K, C> then(Function<SeqList<T>, C> mapper) {
//        Supplier<Seq.Feeder<SeqList<T>, T>> toList = Seq::toList;
//        return eval(toList).replaceValues(v -> mapper.apply(v.get()));
//    }

    public <V> SeqMap<K, Supplier<V>> eval(Supplier<Seq.Feeder<V, T>> supplier) {
        Map<K, Supplier<V>> map = makeMap();
        seq.eval(t -> {
            K k = function.apply(t);
            Supplier<V> feeder = map.computeIfAbsent(k, it -> supplier.get());
            ((Seq.Feeder<V, T>)feeder).accept(t);
        });
        return new SeqMap<>(map);
    }

    @SuppressWarnings("unchecked")
    public SeqMap<K, IntSupplier> evalInt(Supplier<Seq.IntFeeder<T>> supplier) {
        Map<K, IntSupplier> map = makeMap();
        seq.eval(t -> {
            K k = function.apply(t);
            IntSupplier feeder = map.computeIfAbsent(k, it -> supplier.get());
            ((Seq.IntFeeder<T>)feeder).accept(t);
        });
        return new SeqMap<>(map);
    }

    @SuppressWarnings("unchecked")
    public SeqMap<K, DoubleSupplier> evalDouble(Supplier<Seq.DoubleFeeder<T>> supplier) {
        Map<K, DoubleSupplier> map = makeMap();
        seq.eval(t -> {
            K k = function.apply(t);
            DoubleSupplier feeder = map.computeIfAbsent(k, it -> supplier.get());
            ((Seq.DoubleFeeder<T>)feeder).accept(t);
        });
        return new SeqMap<>(map);
    }

    @SuppressWarnings("unchecked")
    public SeqMap<K, LongSupplier> evalLong(Supplier<Seq.LongFeeder<T>> supplier) {
        Map<K, LongSupplier> map = makeMap();
        seq.eval(t -> {
            K k = function.apply(t);
            LongSupplier feeder = map.computeIfAbsent(k, it -> supplier.get());
            ((Seq.LongFeeder<T>)feeder).accept(t);
        });
        return new SeqMap<>(map);
    }

    @SuppressWarnings("unchecked")
    public SeqMap<K, BooleanSupplier> evalBool(Supplier<Seq.BoolFeeder<T>> supplier) {
        Map<K, BooleanSupplier> map = makeMap();
        seq.eval(t -> {
            K k = function.apply(t);
            BooleanSupplier feeder = map.computeIfAbsent(k, it -> supplier.get());
            ((Seq.BoolFeeder<T>)feeder).accept(t);
        });
        return new SeqMap<>(map);
    }
}
