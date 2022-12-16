package com.github.wolray.seq;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.*;

/**
 * @author wolray
 */
public interface Seq<T> extends Foldable<T> {
    @SafeVarargs
    static <T> Seq<T> of(T... ts) {
        return of(Arrays.asList(ts));
    }

    static <T> Seq<T> of(Iterable<T> iterable) {
        return iterable instanceof Collection ? new BackedSeq<>((Collection<T>)iterable) : iterable::forEach;
    }

    static <K, V> Seq<Map.Entry<K, V>> of(Map<K, V> map) {
        return of(map.entrySet());
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
        return c -> eval(t -> c.accept(function.applyAsInt(t)));
    }

    default <E> Seq<E> map(Function<T, E> function) {
        return c -> eval(t -> c.accept(function.apply(t)));
    }

    default <E> Seq<E> mapNotNull(Function<T, E> function) {
        return c -> eval(t -> {
            E e = function.apply(t);
            if (e != null) {
                c.accept(e);
            }
        });
    }

    default <E> Seq<E> mapCe(WithCe.Function<T, E> function) {
        return map(WithCe.mapper(function));
    }

    default Seq<T> onEach(Consumer<T> consumer) {
        return c -> eval(consumer.andThen(c));
    }

    default Seq<T> onEachIndexed(IndexObjConsumer<T> consumer) {
        return c -> eval(foldIndexed((i, t) -> {
            consumer.accept(i, t);
            c.accept(t);
        }));
    }

    default Seq<T> onFirst(Consumer<T> consumer) {
        return onFirst(1, consumer);
    }

    default Seq<T> onFirst(int n, Consumer<T> consumer) {
        return c -> eval(foldIndexed((i, t) -> {
            if (i >= n) {
                c.accept(t);
            } else {
                consumer.accept(t);
                c.accept(t);
            }
        }));
    }

    default Seq<T> onLast(Consumer<T> consumer) {
        return c -> eval(fold(null, (m, t) -> {
            c.accept(t);
            return t;
        }));
    }

    default Seq<T> filter(Predicate<T> predicate) {
        return c -> eval(t -> {
            if (predicate.test(t)) {
                c.accept(t);
            }
        });
    }

    default Seq<T> filterNot(Predicate<T> predicate) {
        return filter(predicate.negate());
    }

    default Seq<T> filterNotNull() {
        return filter(Objects::nonNull);
    }

    default Seq<T> filterIn(Collection<T> collection) {
        return filter(collection::contains);
    }

    default Seq<T> filterNotIn(Collection<T> collection) {
        return filterNot(collection::contains);
    }

    default Seq<T> filterIn(Map<T, ?> map) {
        return filter(map::containsKey);
    }

    default Seq<T> filterNotIn(Map<T, ?> map) {
        return filterNot(map::containsKey);
    }

    default Seq<T> take(int n) {
        return c -> eval(foldIndexed((i, t) -> {
            if (i < n) {
                c.accept(t);
            } else {
                stop();
            }
        }));
    }

    default Seq<T> drop(int n) {
        return forFirst(n, Seq.nothing());
    }

    default Seq<T> forFirst(Consumer<T> consumer) {
        return forFirst(1, consumer);
    }

    default Seq<T> forFirst(int n, Consumer<T> consumer) {
        return c -> eval(foldIndexed((i, t) -> (i >= n ? c : consumer).accept(t)));
    }

    default Seq<T> takeWhile(Predicate<T> predicate) {
        return c -> tillStop(t -> {
            if (predicate.test(t)) {
                c.accept(t);
            } else {
                stop();
            }
        });
    }

    default Seq<T> takeWhile(BiPredicate<T, T> testPrevCurr) {
        return takeWhile(t -> t, testPrevCurr);
    }

    default <E> Seq<T> takeWhile(Function<T, E> function, BiPredicate<E, E> testPrevCurr) {
        return c -> {
            IntPair<E> m = new IntPair<>(0, null);
            tillStop(t -> {
                E e = function.apply(t);
                if (m.first > 0) {
                    if (testPrevCurr.test(m.second, e)) {
                        c.accept(t);
                    } else {
                        stop();
                    }
                } else {
                    c.accept(t);
                    m.first = 1;
                }
                m.second = e;
            });
        };
    }

    default Seq<T> takeWhileEquals() {
        return takeWhile(t -> t, Objects::equals);
    }

    default <E> Seq<T> takeWhileEquals(Function<T, E> function) {
        return takeWhile(function, Objects::equals);
    }

    default Seq<T> dropWhile(Predicate<T> predicate) {
        return c -> eval(foldBool(false, (b, t) -> {
            if (b) {
                c.accept(t);
            } else if (!predicate.test(t)) {
                c.accept(t);
                return true;
            }
            return false;
        }));
    }

    default <E> Seq<Pair<T, E>> pairWith(Function<T, E> function) {
        return map(t -> new Pair<>(t, function.apply(t)));
    }

    default Seq<IntPair<T>> withInt(ToIntFunction<T> function) {
        return map(t -> new IntPair<>(function.applyAsInt(t), t));
    }

    default Seq<LongPair<T>> withLong(ToLongFunction<T> function) {
        return map(t -> new LongPair<>(function.applyAsLong(t), t));
    }

    default Seq<IntPair<T>> withIndex() {
        return withIndex(0);
    }

    default Seq<IntPair<T>> withIndex(int start) {
        return c -> eval(foldIndexed(start, (i, t) -> c.accept(new IntPair<>(i, t))));
    }

    default Seq<T> distinct() {
        return distinctBy(it -> it);
    }

    default <E> Seq<T> distinctBy(Function<T, E> function) {
        return c -> {
            Set<E> set = new HashSet<>();
            eval(t -> {
                if (set.add(function.apply(t))) {
                    c.accept(t);
                }
            });
        };
    }

    default Seq<SeqList<T>> chunked(int size) {
        return c -> {
            Folder<SeqList<T>, T> folder = fold(new SeqList<>(new ArrayList<>(size)), (ls, t) -> {
                if (ls.size() >= size) {
                    c.accept(ls);
                    ls = new SeqList<>(new ArrayList<>(size));
                }
                ls.add(t);
                return ls;
            });
            eval(folder);
            SeqList<T> last = folder.get();
            if (last.isNotEmpty()) {
                c.accept(last);
            }
        };
    }

    default <E> Seq<E> flatMap(Function<T, Seq<E>> function) {
        return c -> eval(t -> function.apply(t).eval(c));
    }

    default <E> Seq<E> runningFold(E init, BiFunction<E, T, E> function) {
        return c -> eval(fold(init, (e, t) -> {
            e = function.apply(e, t);
            c.accept(e);
            return e;
        }));
    }

    default <E> Seq<E> mapSub(T first, T last, ToFolder<T, E> function) {
        return mapSub(first::equals, last::equals, function);
    }

    default <E, V> Seq<E> mapSub(V first, V last, Function<T, V> function, ToFolder<T, E> toFolder) {
        return mapSub(t -> first.equals(function.apply(t)), t -> last.equals(function.apply(t)), toFolder);
    }

    default <E> Seq<E> mapSub(Predicate<T> first, Predicate<T> last, ToFolder<T, E> toFolder) {
        return c -> eval(fold((Folder<E, T>)null, (f, t) -> {
            if (f == null && first.test(t)) {
                f = toFolder.gen();
                f.accept(t);
            } else if (f != null) {
                f.accept(t);
                if (last.test(t)) {
                    c.accept(f.get());
                    return null;
                }
            }
            return f;
        }));
    }

    @SuppressWarnings("unchecked")
    default Seq<T> append(T t, T... more) {
        return c -> {
            eval(c);
            c.accept(t);
            for (T x : more) {
                c.accept(x);
            }
        };
    }

    default Seq<T> appendAll(Iterable<T> iterable) {
        return c -> {
            eval(c);
            iterable.forEach(c);
        };
    }

    default Seq<T> appendWith(Seq<T> seq) {
        return c -> {
            eval(c);
            seq.eval(c);
        };
    }

    default Seq<List<T>> permute(boolean inplace) {
        return c -> SeqUtil.permute(c, toArrayList().eval(), 0, inplace);
    }

    default <E> Seq<Pair<T, E>> zip(Iterable<E> iterable) {
        return zip(iterable, Pair::new);
    }

    default <E, R> Seq<R> zip(Iterable<E> iterable, BiFunction<T, E, R> function) {
        return c -> {
            Iterator<E> iterator = iterable.iterator();
            tillStop(t -> {
                if (iterator.hasNext()) {
                    c.accept(function.apply(t, iterator.next()));
                } else {
                    stop();
                }
            });
        };
    }

    default <B, C> Seq<Triple<T, B, C>> zip(Iterable<B> bs, Iterable<C> cs) {
        return c -> {
            Iterator<B> bi = bs.iterator();
            Iterator<C> ci = cs.iterator();
            tillStop(t -> {
                if (bi.hasNext() && ci.hasNext()) {
                    c.accept(new Triple<>(t, bi.next(), ci.next()));
                } else {
                    stop();
                }
            });
        };
    }

    default <E> void zipWith(Iterable<E> es, BiConsumer<T, E> consumer) {
        Iterator<E> ei = es.iterator();
        tillStop(t -> {
            if (ei.hasNext()) {
                consumer.accept(t, ei.next());
            } else {
                stop();
            }
        });
    }

    default SeqList<T> sort() {
        return sortOn(null);
    }

    default SeqList<T> sortOn(Comparator<T> comparator) {
        SeqList<T> list = toList().eval();
        list.backer.sort(comparator);
        return list;
    }

    default SeqList<T> sortDesc() {
        return sortOn(Collections.reverseOrder());
    }

    default SeqList<T> sortDesc(Comparator<T> comparator) {
        return sortOn(comparator.reversed());
    }

    default <E extends Comparable<E>> SeqList<T> sortBy(Function<T, E> function) {
        return sortOn(Comparator.comparing(function));
    }

    default <E extends Comparable<E>> SeqList<T> sortDescBy(Function<T, E> function) {
        return sortOn(Comparator.comparing(function).reversed());
    }

    default <E extends Comparable<E>> SeqList<Pair<T, E>> sortWith(Function<T, E> function) {
        return pairWith(function).sortBy(p -> p.second);
    }

    default <E extends Comparable<E>> SeqList<Pair<T, E>> sortDescWith(Function<T, E> function) {
        return pairWith(function).sortDescBy(p -> p.second);
    }

    default Seq<T> cacheBy(Cache<T> cache) {
        return cacheBy(BatchList.DEFAULT_BATCH_SIZE, cache);
    }

    default Seq<T> cacheBy(int batchSize, Cache<T> cache) {
        if (cache.exists()) {
            return cache.read();
        } else {
            BatchList<T> list = new BatchList<>(batchSize);
            eval(list::add);
            if (list.isNotEmpty()) {
                cache.write(list);
            }
            return list;
        }
    }

    default Seq<T> cache() {
        return toBatchList().eval();
    }

    default <K, V> SeqMap<K, Supplier<V>> groupBy(Function<T, K> kFunction, ToFolder<T, V> toFolder) {
        return groupBy(kFunction, null, 10, toFolder);
    }

    default <K, V> SeqMap<K, Supplier<V>> groupBy(Function<T, K> kFunction, int groupSize, ToFolder<T, V> toFolder) {
        return groupBy(kFunction, null, groupSize, toFolder);
    }

    @SuppressWarnings("rawtypes")
    default <K, V> SeqMap<K, Supplier<V>> groupBy(Function<T, K> kFunction, Class<? extends Map> mapClass, ToFolder<T, V> toFolder) {
        return groupBy(kFunction, mapClass, 10, toFolder);
    }

    @SuppressWarnings("rawtypes")
    default <K, V> SeqMap<K, Supplier<V>> groupBy(Function<T, K> kFunction, Class<? extends Map> mapClass, int groupSize, ToFolder<T, V> toFolder) {
        Map<K, Supplier<V>> map = SeqMap.makeMap(groupSize, mapClass);
        eval(t -> {
            K k = kFunction.apply(t);
            Supplier<V> folder = map.computeIfAbsent(k, it -> toFolder.gen());
            ((Folder<V, T>)folder).accept(t);
        });
        return new SeqMap<>(map);
    }

    default void assertTo(String s) {
        assertTo(",", s);
    }

    default void assertTo(String sep, String s) {
        assert join(sep).eval().equals(s);
    }

    default void printAll() {
        printAll(",");
    }

    default void printAll(String sep) {
        if ("\n".equals(sep)) {
            eval(System.out::println);
        } else {
            System.out.println(join(sep).eval());
        }
    }

    default ParallelSeq<T> parallel() {
        return this instanceof ParallelSeq ? (ParallelSeq<T>)this : c -> {
            ForkJoinPool pool = new ForkJoinPool();
            eval(t -> pool.submit(() -> c.accept(t)));
        };
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
