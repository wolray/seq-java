package com.github.wolray.seq;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.*;


/**
 * @author wolray
 */
public interface Seq<T> {
    void eval(Consumer<T> consumer);

    default void tillStop(Consumer<T> consumer) {
        try {
            eval(consumer);
        } catch (StopException ignore) {}
    }

    default int evalIndexed(IndexedConsumer<T> consumer) {
        return evalIndexed(0, consumer);
    }

    default int evalIndexed(int start, IndexedConsumer<T> consumer) {
        int[] a = new int[]{start};
        tillStop(t -> consumer.accept(a[0]++, t));
        return a[0];
    }

    default int evalOnInt(int init, BiConsumer<int[], T> consumer) {
        int[] a = new int[]{init};
        tillStop(t -> consumer.accept(a, t));
        return a[0];
    }

    default double evalOnDouble(double init, BiConsumer<double[], T> consumer) {
        double[] a = new double[]{init};
        tillStop(t -> consumer.accept(a, t));
        return a[0];
    }

    default long evalOnLong(long init, BiConsumer<long[], T> consumer) {
        long[] a = new long[]{init};
        tillStop(t -> consumer.accept(a, t));
        return a[0];
    }

    default boolean evalOnBool(boolean init, BiConsumer<boolean[], T> consumer) {
        boolean[] a = new boolean[]{init};
        tillStop(t -> consumer.accept(a, t));
        return a[0];
    }

    default <S> S evalStateful(S init, BiConsumer<Mutable<S>, T> consumer) {
        Mutable<S> m = new Mutable<>(init);
        tillStop(t -> consumer.accept(m, t));
        return m.it;
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

    static <T> Seq<T> empty() {
        return c -> {};
    }

    static <T> void nothing(T t) {}

    default IntSeq mapToInt(ToIntFunction<T> function) {
        return c -> eval(t -> c.accept(function.applyAsInt(t)));
    }

    default <E> Seq<E> map(Function<T, E> function) {
        return c -> eval(t -> c.accept(function.apply(t)));
    }

    default <E> Seq<E> map(int skip, ContextFunction<T, E> function) {
        return c -> evalIndexed((i, t) -> {
            if (i > skip) {
                c.accept(function.apply(t));
            } else if (i == skip) {
                function.onFirst(t);
            }
        });
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

    default Seq<T> onEachIndexed(IndexedConsumer<T> consumer) {
        return c -> evalIndexed((i, t) -> {
            consumer.accept(i, t);
            c.accept(t);
        });
    }

    default Seq<T> onFirst(Consumer<T> consumer) {
        return onFirst(1, consumer);
    }

    default Seq<T> onFirst(int n, Consumer<T> consumer) {
        return c -> evalIndexed((i, t) -> {
            if (i >= n) {
                c.accept(t);
            } else {
                consumer.accept(t);
                c.accept(t);
            }
        });
    }

    default Seq<T> onLast(Consumer<T> consumer) {
        return c -> consumer.accept(evalStateful(null, (m, t) -> {
            c.accept(t);
            m.it = t;
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
        return c -> evalIndexed((i, t) -> {
            if (i < n) {
                c.accept(t);
            } else {
                stop();
            }
        });
    }

    default Seq<T> drop(int n) {
        return forFirst(n, Seq::nothing);
    }

    default Seq<T> forFirst(Consumer<T> consumer) {
        return forFirst(1, consumer);
    }

    default Seq<T> forFirst(int n, Consumer<T> consumer) {
        return c -> evalIndexed((i, t) -> (i >= n ? c : consumer).accept(t));
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

    default Seq<T> dropWhile(Predicate<T> predicate) {
        return c -> evalOnBool(false, (a, t) -> {
            if (a[0]) {
                c.accept(t);
            } else if (!predicate.test(t)) {
                c.accept(t);
                a[0] = true;
            }
        });
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
        return c -> evalIndexed(start, (i, t) -> c.accept(new IntPair<>(i, t)));
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
            SeqList<T> last = evalStateful(new SeqList<>(new ArrayList<>(size)), (m, t) -> {
                if (m.it.size() >= size) {
                    c.accept(m.it);
                    m.it = new SeqList<>(new ArrayList<>(size));
                }
                m.it.add(t);
            });
            if (last.isNotEmpty()) {
                c.accept(last);
            }
        };
    }

    default <E> Seq<E> flatMap(Function<T, Seq<E>> function) {
        return c -> eval(t -> function.apply(t).eval(c));
    }

    default <E> Seq<E> runningFold(E init, BiFunction<E, T, E> function) {
        return c -> evalStateful(init, (m, t) -> c.accept(m.it = function.apply(m.it, t)));
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
        List<T> list = toArrayList();
        return c -> SeqUtil.permute(c, list, 0, inplace);
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

    default <E> E fold(E init, BiFunction<E, T, E> function) {
        return evalStateful(init, (acc, t) -> acc.it = function.apply(acc.it, t));
    }

    default <E> E feed(E des, BiConsumer<E, T> consumer) {
        eval(t -> consumer.accept(des, t));
        return des;
    }

    default boolean any(boolean ifFound, Predicate<T> predicate) {
        return evalOnBool(!ifFound, (a, t) -> {
            if (predicate.test(t)) {
                a[0] = ifFound;
                stop();
            }
        });
    }

    default boolean any(Predicate<T> predicate) {
        return any(true, predicate);
    }

    default boolean anyNot(Predicate<T> predicate) {
        return any(predicate.negate());
    }

    default boolean all(Predicate<T> predicate) {
        return any(false, predicate.negate());
    }

    default boolean none(Predicate<T> predicate) {
        return any(false, predicate);
    }

    default T first() {
        return evalStateful(null, (m, t) -> {
            m.it = t;
            stop();
        });
    }

    default T firstNotNull() {
        return first(Objects::nonNull);
    }

    default T first(Predicate<T> predicate) {
        return evalStateful(null, (m, t) -> {
            if (predicate.test(t)) {
                m.it = t;
                stop();
            }
        });
    }

    default T firstNot(Predicate<T> predicate) {
        return first(predicate.negate());
    }

    default T last() {
        return evalStateful(null, (m, t) -> m.it = t);
    }

    default int count() {
        return evalIndexed((i, t) -> {});
    }

    default int count(Predicate<T> predicate) {
        return sumInt(t -> predicate.test(t) ? 1 : 0);
    }

    default int countNot(Predicate<T> predicate) {
        return count(predicate.negate());
    }

    default double sum(ToDoubleFunction<T> function) {
        return evalOnDouble(0, (a, t) -> a[0] += function.applyAsDouble(t));
    }

    default int sumInt(ToIntFunction<T> function) {
        return evalOnInt(0, (a, t) -> a[0] += function.applyAsInt(t));
    }

    default long sumLong(ToLongFunction<T> function) {
        return evalOnLong(0, (a, t) -> a[0] += function.applyAsLong(t));
    }

    default double average(ToDoubleFunction<T> function) {
        return average(function, null);
    }

    default double average(ToDoubleFunction<T> function, ToDoubleFunction<T> weightFunction) {
        double[] sumWithWeight = new double[2];
        if (weightFunction != null) {
            eval(t -> {
                double v = function.applyAsDouble(t);
                double w = weightFunction.applyAsDouble(t);
                sumWithWeight[0] += v * w;
                sumWithWeight[1] += w;
            });
        } else {
            eval(t -> {
                sumWithWeight[0] += function.applyAsDouble(t);
                sumWithWeight[1] += 1;
            });
        }
        return sumWithWeight[1] > 0 ? sumWithWeight[0] / sumWithWeight[1] : 0;
    }

    default T max(Comparator<T> comparator) {
        return evalStateful(null, (m, t) -> {
            if (m.it == null || comparator.compare(m.it, t) < 0) {
                m.it = t;
            }
        });
    }

    default <V extends Comparable<V>> MutablePair<T, V> maxWith(Function<T, V> function) {
        MutablePair<T, V> pair = new MutablePair<>(null, null);
        eval(t -> {
            V v = function.apply(t);
            if (pair.first == null || pair.second.compareTo(v) < 0) {
                pair.first = t;
                pair.second = v;
            }
        });
        return pair;
    }

    default <V extends Comparable<V>> T maxBy(Function<T, V> function) {
        return maxWith(function).first;
    }

    default T min(Comparator<T> comparator) {
        return evalStateful(null, (m, t) -> {
            if (m.it == null || comparator.compare(m.it, t) > 0) {
                m.it = t;
            }
        });
    }

    default <V extends Comparable<V>> MutablePair<T, V> minWith(Function<T, V> function) {
        MutablePair<T, V> pair = new MutablePair<>(null, null);
        eval(t -> {
            V v = function.apply(t);
            if (pair.first == null || pair.second.compareTo(v) > 0) {
                pair.first = t;
                pair.second = v;
            }
        });
        return pair;
    }

    default <V extends Comparable<V>> T minBy(Function<T, V> function) {
        return minWith(function).first;
    }

    default SeqList<T> sort() {
        return sortOn(null);
    }

    default SeqList<T> sortOn(Comparator<T> comparator) {
        SeqList<T> list = toList();
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

    @SuppressWarnings("unchecked")
    default <E> E[] toArray(IntFunction<E[]> generator) {
        List<T> list = toBatchList();
        E[] res = generator.apply(list.size());
        int i = 0;
        for (T t : list) {
            res[i++] = (E)t;
        }
        return res;
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

    default int sizeOrDefault() {
        return 10;
    }

    default SeqSet<T> toSet() {
        return new SeqSet<>(collectBy(HashSet::new));
    }

    default SeqList<T> toList() {
        return new SeqList<>(toArrayList());
    }

    default ArrayList<T> toArrayList() {
        return collectBy(ArrayList::new);
    }

    default SinglyList<T> toSinglyList() {
        return collect(new SinglyList<>());
    }

    default BatchList<T> toBatchList() {
        return collect(new BatchList<>());
    }

    default BatchList<T> toBatchList(int batchSize) {
        return collect(new BatchList<>(batchSize));
    }

    default <C extends Collection<T>> C collect(C des) {
        return feed(des, Collection::add);
    }

    default <C extends Collection<T>> C collectBy(IntFunction<C> bySize) {
        return feed(bySize.apply(sizeOrDefault()), Collection::add);
    }

    default Pair<BatchList<T>, BatchList<T>> partition(Predicate<T> predicate) {
        BatchList<T> trueList = new BatchList<>();
        BatchList<T> falseList = new BatchList<>();
        eval(t -> (predicate.test(t) ? trueList : falseList).add(t));
        return new Pair<>(trueList, falseList);
    }

    default Seq<T> cache() {
        return of(toBatchList());
    }

    default <K, V> SeqMap<K, V> toMap(Function<T, K> kFunction, Function<T, V> vFunction) {
        return toMap(new HashMap<>(sizeOrDefault()), kFunction, vFunction);
    }

    default <K> SeqMap<K, T> toMapBy(Function<T, K> kFunction) {
        return toMapBy(new HashMap<>(sizeOrDefault()), kFunction);
    }

    default <V> SeqMap<T, V> toMapWith(Function<T, V> vFunction) {
        return toMapWith(new HashMap<>(sizeOrDefault()), vFunction);
    }

    default <K, V> SeqMap<K, V> toMap(Map<K, V> des, Function<T, K> kFunction, Function<T, V> vFunction) {
        return new SeqMap<>(feed(des, (res, t) -> res.put(kFunction.apply(t), vFunction.apply(t))));
    }

    default <K> SeqMap<K, T> toMapBy(Map<K, T> des, Function<T, K> kFunction) {
        return new SeqMap<>(feed(des, (res, t) -> res.put(kFunction.apply(t), t)));
    }

    default <V> SeqMap<T, V> toMapWith(Map<T, V> des, Function<T, V> vFunction) {
        return new SeqMap<>(feed(des, (res, t) -> res.put(t, vFunction.apply(t))));
    }

    default <K> Grouping<T, K> groupBy(Function<T, K> kFunction) {
        return new Grouping<>(this, kFunction);
    }

    default String join(String sep) {
        return join(sep, String::valueOf);
    }

    default String join(String sep, Function<T, String> function) {
        return feed(new StringJoiner(sep), (j, t) -> j.add(function.apply(t))).toString();
    }

    default void assertTo(String s) {
        assertTo(",", s);
    }

    default void assertTo(String sep, String s) {
        assert join(sep).equals(s);
    }

    default void printAll() {
        printAll(null);
    }

    default void printAll(String sep) {
        if (sep == null || "\n".equals(sep)) {
            eval(System.out::println);
        } else {
            System.out.println(join(sep));
        }
    }

    default ParallelSeq<T> parallel() {
        return this instanceof ParallelSeq ? (ParallelSeq<T>)this : c -> {
            ForkJoinPool pool = new ForkJoinPool();
            eval(t -> pool.submit(() -> c.accept(t)));
        };
    }

    class StopException extends RuntimeException {
        static final StopException INSTANCE = new StopException() {
            @Override
            public synchronized Throwable fillInStackTrace() {
                return this;
            }
        };
    }

    static <E> E stop() throws StopException {
        throw StopException.INSTANCE;
    }

    interface IndexedConsumer<T> {
        void accept(int i, T t);
    }

    interface ContextFunction<T, E> extends Function<T, E> {
        void onFirst(T t);
    }

    interface ParallelSeq<T> extends Seq<T> {}
}
