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
    default Seq<T> source() {
        return this;
    }

    @Override
    default Consumer<T> apply(Consumer<T> consumer) {
        return consumer;
    }

    @SafeVarargs
    static <T> Seq<T> of(T... ts) {
        return of(Arrays.asList(ts));
    }

    static <T> Seq<T> of(Iterable<T> iterable) {
        return iterable instanceof Collection
            ? new BackedSeq<>((Collection<T>)iterable)
            : iterable::forEach;
    }

    static <K, V> SeqMap<K, V> of(Map<K, V> map) {
        return SeqMap.of(map);
    }

    static <N> Seq<N> ofTree(N node, Function<N, Seq<N>> sub) {
        return c -> SeqUtil.scanTree(c, node, sub);
    }

    static <T> Seq<T> tillNull(Supplier<T> supplier) {
        return c -> {
            T t;
            while ((t = supplier.get()) != null) {
                c.accept(t);
            }
        };
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

    static <T> Seq<T> repeat(int n, T t) {
        return c -> {
            for (int i = 0; i < n; i++) {
                c.accept(t);
            }
        };
    }

    default IntSeq mapToInt(ToIntFunction<T> function) {
        return c -> supply(t -> c.accept(function.applyAsInt(t)));
    }

    default Seq<T> circle() {
        return c -> {
            while (true) {
                supply(c);
            }
        };
    }

    default Seq<T> duplicateAll(int times) {
        return c -> {
            for (int i = 0; i < times; i++) {
                supply(c);
            }
        };
    }

    default Seq<SeqList<T>> chunked(int size) {
        return chunked(size, Transformer::toList);
    }

    default <E> Seq<E> chunked(int size, ToFolder<E, T> toFolder) {
        return c -> {
            IntPair<Folder<E, T>> last = feed(new IntPair<>(0, (Folder<E, T>)null), (p, t) -> {
                if (p.second == null) {
                    p.second = toFolder.gen();
                } else if (p.first >= size) {
                    c.accept(p.second.get());
                    p.first = 0;
                    p.second = toFolder.gen();
                }
                p.first++;
                p.second.accept(t);
            }).eval();
            c.accept(last.second.get());
        };
    }

    @SuppressWarnings("unchecked")
    default Seq<T> append(T t, T... more) {
        return c -> {
            supply(c);
            c.accept(t);
            for (T x : more) {
                c.accept(x);
            }
        };
    }

    default Seq<T> appendAll(Iterable<T> iterable) {
        return c -> {
            supply(c);
            iterable.forEach(c);
        };
    }

    default Seq<T> appendWith(Seq<T> seq) {
        return c -> {
            supply(c);
            seq.supply(c);
        };
    }

    default <E> void zipWith(Iterable<E> es, BiConsumer<T, E> consumer) {
        tillStop(feed(es.iterator(), (itr, t) -> {
            if (itr.hasNext()) {
                consumer.accept(t, itr.next());
            } else {
                Seq0.stop();
            }
        }));
    }

    default Seq<T> cacheBy(Cache<T> cache) {
        return cacheBy(BatchList.DEFAULT_BATCH_SIZE, cache);
    }

    default Seq<T> cacheBy(int batchSize, Cache<T> cache) {
        if (cache.exists()) {
            return cache.read();
        } else {
            Folder<BatchList<T>, T> folder = toBatchList(batchSize);
            return folder.then(ls -> {
                if (ls.isNotEmpty()) {
                    cache.write(ls);
                }
            }).eval();
        }
    }

    default Seq<T> cache() {
        return toBatchList().eval();
    }

    default ParallelSeq<T> parallel() {
        return this instanceof ParallelSeq ? (ParallelSeq<T>)this : c -> {
            ForkJoinPool pool = new ForkJoinPool();
            supply(t -> pool.submit(() -> c.accept(t)));
        };
    }

    default void printAll() {
        printAll(",");
    }

    default void printAll(String sep) {
        if ("\n".equals(sep)) {
            supply(System.out::println);
        } else {
            System.out.println(join(sep).eval());
        }
    }

    class Empty {
        static Seq<Object> emptySeq = c -> {};
        static Consumer<Object> nothing = t -> {};
    }

    @SuppressWarnings("unchecked")
    static <T> Seq<T> empty() {
        return (Seq<T>)Empty.emptySeq;
    }

    @SuppressWarnings("unchecked")
    static <T> Consumer<T> nothing() {
        return (Consumer<T>)Empty.nothing;
    }

    interface ParallelSeq<T> extends Seq<T> {}
}
