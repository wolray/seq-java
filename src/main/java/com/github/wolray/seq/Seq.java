package com.github.wolray.seq;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.function.*;

/**
 * @author wolray
 */
public interface Seq<T> extends Seq0<Consumer<T>>, Transformer<T, T> {
    void supply(Consumer<T> consumer);

    @Override
    default Consumer<T> apply(Consumer<T> c) {
        return c;
    }

    @Override
    default Seq<T> source() {
        return this;
    }

    static <T> Seq<T> gen(T seed, UnaryOperator<T> operator) {
        return c -> {
            T t = seed;
            c.accept(t);
            while (true) {
                c.accept(t = operator.apply(t));
            }
        };
    }

    static <T> Seq<T> gen(T seed1, T seed2, BinaryOperator<T> operator) {
        return c -> {
            T t1 = seed1, t2 = seed2;
            c.accept(t1);
            c.accept(t2);
            while (true) {
                c.accept(t2 = operator.apply(t1, t1 = t2));
            }
        };
    }

    static <K, V> SeqMap<K, V> of(Map<K, V> map) {
        return SeqMap.of(map);
    }

    static <T> Seq<T> of(Iterable<T> iterable) {
        return iterable instanceof Collection
            ? new BackedSeq<>((Collection<T>)iterable)
            : iterable::forEach;
    }

    @SafeVarargs
    static <T> Seq<T> of(T... ts) {
        return of(Arrays.asList(ts));
    }

    static <N> Seq<N> ofTree(N node, Function<N, Seq<N>> sub) {
        return c -> SeqUtil.scanTree(c, node, sub);
    }

    static <T> Seq<T> repeat(int n, T t) {
        return c -> {
            for (int i = 0; i < n; i++) {
                c.accept(t);
            }
        };
    }

    static <T> Seq<T> tillNull(Supplier<T> supplier) {
        return c -> {
            T t;
            while ((t = supplier.get()) != null) {
                c.accept(t);
            }
        };
    }
}
