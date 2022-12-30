package com.github.wolray.seq;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.*;

/**
 * @author wolray
 */
public interface Seq<T> extends Seq0<Consumer<T>> {
    @SuppressWarnings("unchecked")
    static <T> Seq<T> empty() {
        return (Seq<T>)Empty.emptySeq;
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

    @SuppressWarnings("unchecked")
    static <T> Consumer<T> nothing() {
        return (Consumer<T>)Empty.nothing;
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

    static <N> Seq<N> ofTreeParallel(N root, Function<N, Seq<N>> sub) {
        return c -> SeqUtil.scanTreeParallel(c, new ForkJoinPool(), root, sub);
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

    default boolean all(Predicate<T> predicate) {
        return any(false, predicate.negate());
    }

    default boolean any(boolean ifFound, Predicate<T> predicate) {
        return find(!ifFound, predicate, t -> ifFound);
    }

    default boolean any(Predicate<T> predicate) {
        return any(true, predicate);
    }

    default boolean anyNot(Predicate<T> predicate) {
        return any(predicate.negate());
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

    default void assertTo(String s) {
        assertTo(",", s);
    }

    default void assertTo(String sep, String s) {
        String result = join(sep);
        assert result.equals(s) : result;
    }

    default double average(ToDoubleFunction<T> function) {
        return average(function, null);
    }

    default double average(ToDoubleFunction<T> function, ToDoubleFunction<T> weightFunction) {
        BiConsumer<double[], T> biConsumer;
        if (weightFunction != null) {
            biConsumer = (a, t) -> {
                double v = function.applyAsDouble(t);
                double w = weightFunction.applyAsDouble(t);
                a[0] += v * w;
                a[1] += w;
            };
        } else {
            biConsumer = (a, t) -> {
                a[0] += function.applyAsDouble(t);
                a[1] += 1;
            };
        }
        double[] a = feed(new double[]{0, 0}, biConsumer);
        return a[1] != 0 ? a[0] / a[1] : 0;
    }

    default Seq<T> cache() {
        return cache(BatchList.DEFAULT_BATCH_SIZE);
    }

    default Seq<T> cache(int batchSize) {
        if (this instanceof BackedSeq || this instanceof AdderList) {
            return this;
        }
        return toBatchList(batchSize);
    }

    default Seq<T> cacheBy(Cache<T> cache) {
        return cacheBy(BatchList.DEFAULT_BATCH_SIZE, cache);
    }

    default Seq<T> cacheBy(int batchSize, Cache<T> cache) {
        if (cache.exists()) {
            return cache.read();
        } else {
            BatchList<T> ts = toBatchList(batchSize);
            if (ts.isNotEmpty()) {
                cache.write(ts);
            }
            return ts;
        }
    }

    default Seq<SeqList<T>> chunked(int size) {
        return c -> {
            SeqList<T> last = fold(null, (ts, t) -> {
                if (ts == null) {
                    ts = new SeqList<>(new ArrayList<>(size));
                } else if (ts.size() >= size) {
                    c.accept(ts);
                    ts = new SeqList<>(new ArrayList<>(size));
                }
                ts.backer.add(t);
                return ts;
            });
            c.accept(last);
        };
    }

    default Seq<T> circle() {
        return c -> {
            while (true) {
                supply(c);
            }
        };
    }

    default <C extends Collection<T>> C collect(C des) {
        return feed(des, Collection::add);
    }

    default <C extends Collection<T>> C collectBy(IntFunction<C> bySize) {
        return feed(bySize.apply(sizeOrDefault()), Collection::add);
    }

    default Integer count() {
        return foldInt(0, (i, t) -> i + 1);
    }

    default Integer count(Predicate<T> predicate) {
        return sumInt(t -> predicate.test(t) ? 1 : 0);
    }

    default Integer countNot(Predicate<T> predicate) {
        return count(predicate.negate());
    }

    default Seq<T> distinct() {
        return distinctBy(it -> it);
    }

    default <E> Seq<T> distinctBy(Function<T, E> function) {
        return c -> feed(new HashSet<>(), (set, t) -> {
            if (set.add(function.apply(t))) {
                c.accept(t);
            }
        });
    }

    default Seq<T> drop(int n) {
        return forFirst(n, nothing());
    }

    default Seq<T> dropWhile(Predicate<T> predicate) {
        return c -> foldBoolean(false, (b, t) -> {
            if (b || !predicate.test(t)) {
                c.accept(t);
                return true;
            }
            return false;
        });
    }

    default Seq<T> duplicateAll(int times) {
        return c -> {
            for (int i = 0; i < times; i++) {
                supply(c);
            }
        };
    }

    default Seq<T> duplicateEach(int times) {
        return c -> supply(t -> {
            for (int i = 0; i < times; i++) {
                c.accept(t);
            }
        });
    }

    default Seq<T> duplicateIf(int times, Predicate<T> predicate) {
        return c -> supply(t -> {
            if (predicate.test(t)) {
                for (int i = 0; i < times; i++) {
                    c.accept(t);
                }
            } else {
                c.accept(t);
            }
        });
    }

    default <E> E feed(E des, BiConsumer<E, T> consumer) {
        tillStop(t -> consumer.accept(des, t));
        return des;
    }

    default Seq<T> filter(Predicate<T> predicate) {
        return c -> supply(t -> {
            if (predicate.test(t)) {
                c.accept(t);
            }
        });
    }

    default Seq<T> filterIn(Collection<T> collection) {
        return filter(collection::contains);
    }

    default Seq<T> filterIn(Map<T, ?> map) {
        return filter(map::containsKey);
    }

    default Seq<T> filterIndexed(IndexObjPredicate<T> predicate) {
        return c -> foldIndexed((i, t) -> {
            if (predicate.test(i, t)) {
                c.accept(t);
            }
        });
    }

    default Seq<T> filterNot(Predicate<T> predicate) {
        return filter(predicate.negate());
    }

    default Seq<T> filterNotIn(Collection<T> collection) {
        return filterNot(collection::contains);
    }

    default Seq<T> filterNotIn(Map<T, ?> map) {
        return filterNot(map::containsKey);
    }

    default Seq<T> filterNotNull() {
        return filter(Objects::nonNull);
    }

    default <E> E find(E ifNotFound, Predicate<T> predicate, Function<T, E> function) {
        Mutable<E> m = new Mutable<>(ifNotFound);
        tillStop(t -> {
            if (predicate.test(t)) {
                m.it = function.apply(t);
                stop();
            }
        });
        return m.it;
    }

    default T first() {
        return find(null, t -> true, t -> t);
    }

    default T first(Predicate<T> predicate) {
        return find(null, predicate, t -> t);
    }

    default Pair<T, T> firstAndLast() {
        return feed(new Pair<>(null, null), (p, t) -> {
            if (p.first == null) {
                p.first = t;
            }
            p.second = t;
        });
    }

    default T firstNot(Predicate<T> predicate) {
        return first(predicate.negate());
    }

    default T firstNotNull() {
        return first(Objects::nonNull);
    }

    default <E> Seq<E> flatMap(Function<T, Seq<E>> function) {
        return c -> supply(t -> function.apply(t).supply(c));
    }

    default <E> E fold(E init, BiFunction<E, T, E> function) {
        Mutable<E> m = new Mutable<>(init);
        tillStop(t -> m.it = function.apply(m.it, t));
        return m.it;
    }

    default int foldInt(int init, IntObjToInt<T> function) {
        int[] a = new int[]{init};
        tillStop(t -> a[0] = function.apply(a[0], t));
        return a[0];
    }

    default double foldDouble(double init, DoubleObjToDouble<T> function) {
        double[] a = new double[]{init};
        tillStop(t -> a[0] = function.apply(a[0], t));
        return a[0];
    }

    default long foldLong(long init, LongObjToLong<T> function) {
        long[] a = new long[]{init};
        tillStop(t -> a[0] = function.apply(a[0], t));
        return a[0];
    }

    default boolean foldBoolean(boolean init, BooleanObjToBoolean<T> function) {
        boolean[] a = new boolean[]{init};
        tillStop(t -> a[0] = function.apply(a[0], t));
        return a[0];
    }

    default int foldIndexed(IndexObjConsumer<T> consumer) {
        return foldIndexed(0, consumer);
    }

    default int foldIndexed(int start, IndexObjConsumer<T> consumer) {
        return foldInt(start, (i, t) -> {
            consumer.accept(i, t);
            return i + 1;
        });
    }

    default Seq<T> forFirst(Consumer<T> consumer) {
        return forFirst(1, consumer);
    }

    default Seq<T> forFirst(int n, Consumer<T> consumer) {
        return c -> foldIndexed((i, t) -> (i >= n ? c : consumer).accept(t));
    }

    default <K, V> SeqMap<K, BatchList<V>> groupBy(Function<T, K> kFunction, Function<T, V> vFunction) {
        Function<K, BatchList<V>> mappingFunction = k -> new BatchList<>();
        Map<K, BatchList<V>> map = feed(new HashMap<>(), (m, t) -> {
            BatchList<V> list = m.computeIfAbsent(kFunction.apply(t), mappingFunction);
            list.add(vFunction.apply(t));
        });
        return new SeqMap<>(map);
    }

    default String join(String sep) {
        return join(sep, String::valueOf);
    }

    default String join(String sep, Function<T, String> function) {
        StringJoiner joiner = feed(new StringJoiner(sep), (j, t) -> j.add(function.apply(t)));
        return joiner.toString();
    }

    default T last() {
        return fold(null, (res, t) -> t);
    }

    default T last(Predicate<T> predicate) {
        return fold(null, (res, t) -> predicate.test(t) ? t : res);
    }

    default T lastNot(Predicate<T> predicate) {
        return last(predicate.negate());
    }

    default <E> Seq<E> map(Function<T, E> function) {
        return c -> supply(t -> c.accept(function.apply(t)));
    }

    default <E> Seq<E> mapIndexed(IndexObjFunction<T, E> function) {
        return c -> foldIndexed((i, t) -> c.accept(function.apply(i, t)));
    }

    default <E> Seq<E> mapNotNll(Function<T, E> function) {
        return c -> supply(t -> {
            E e = function.apply(t);
            if (e != null) {
                c.accept(e);
            }
        });
    }

    default <E> Seq<E> mapPair(boolean overlapping, BiFunction<T, T, E> function) {
        return c -> fold((T)null, (last, t) -> {
            if (last != null) {
                c.accept(function.apply(last, t));
                return overlapping ? t : null;
            }
            return t;
        });
    }

    default Seq<Pair<T, T>> mapPair(boolean overlapping) {
        return mapPair(overlapping, Pair::new);
    }

    default Seq<BatchList<T>> mapSub(Predicate<T> first, Predicate<T> last) {
        return c -> fold((BatchList<T>)null, (ls, t) -> {
            if (ls == null && first.test(t)) {
                ls = new BatchList<>();
                ls.add(t);
            } else if (ls != null) {
                ls.add(t);
                if (last.test(t)) {
                    c.accept(ls);
                    return null;
                }
            }
            return ls;
        });
    }

    default Seq<BatchList<T>> mapSub(T first, T last) {
        return mapSub(first::equals, last::equals);
    }

    default IntSeq mapToInt(ToIntFunction<T> function) {
        return c -> supply(t -> c.accept(function.applyAsInt(t)));
    }

    default <V extends Comparable<V>> Pair<T, V> max(Function<T, V> function) {
        return feed(new Pair<>(null, null), (p, t) -> {
            V v = function.apply(t);
            if (p.first == null || p.second.compareTo(v) < 0) {
                p.first = t;
                p.second = v;
            }
        });
    }

    default T max(Comparator<T> comparator) {
        return fold(null, (f, t) -> f == null || comparator.compare(f, t) < 0 ? t : f);
    }

    default <V extends Comparable<V>> ConcurrentPair<T, V> maxAsync(V initValue, Function<T, V> function) {
        return feed(new ConcurrentPair<>(null, initValue), (p, t) -> {
            V v = function.apply(t);
            if (p.getSecond().compareTo(v) < 0) {
                p.setFirst(t);
                p.setSecond(v);
            }
        });
    }

    default <V extends Comparable<V>> Pair<T, V> min(Function<T, V> function) {
        return feed(new Pair<>(null, null), (p, t) -> {
            V v = function.apply(t);
            if (p.first == null || p.second.compareTo(v) > 0) {
                p.first = t;
                p.second = v;
            }
        });
    }

    default T min(Comparator<T> comparator) {
        return fold(null, (f, t) -> f == null || comparator.compare(f, t) > 0 ? t : f);
    }

    default <V extends Comparable<V>> ConcurrentPair<T, V> minAsync(V initValue, Function<T, V> function) {
        return feed(new ConcurrentPair<>(null, initValue), (p, t) -> {
            V v = function.apply(t);
            if (p.getSecond().compareTo(v) > 0) {
                p.setFirst(t);
                p.setSecond(v);
            }
        });
    }

    default boolean none(Predicate<T> predicate) {
        return any(false, predicate);
    }

    default Seq<T> onEach(Consumer<T> consumer) {
        return c -> tillStop(consumer.andThen(c));
    }

    default Seq<T> onEachIndexed(IndexObjConsumer<T> consumer) {
        return c -> foldIndexed(consumer);
    }

    default Seq<T> onFirst(Consumer<T> consumer) {
        return onFirst(1, consumer);
    }

    default Seq<T> onFirst(int n, Consumer<T> consumer) {
        return c -> foldIndexed((i, t) -> {
            if (i >= n) {
                c.accept(t);
            } else {
                consumer.accept(t);
                c.accept(t);
            }
        });
    }

    default <E> Seq<Pair<T, E>> pairWith(Function<T, E> function) {
        return map(t -> new Pair<>(t, function.apply(t)));
    }

    default Seq<T> parallel() {
        return c -> {
            ForkJoinPool pool = new ForkJoinPool();
            map(t -> pool.submit(() -> c.accept(t)))
                .cache()
                .supply(WithCe.acceptor(ForkJoinTask::join));
        };
    }

    default Pair<BatchList<T>, BatchList<T>> partition(Predicate<T> predicate) {
        return feed(new Pair<>(new BatchList<>(), new BatchList<>()), (p, t) ->
            (predicate.test(t) ? p.first : p.second).add(t));
    }

    default void printAll(String sep) {
        if ("\n".equals(sep)) {
            println();
        } else {
            System.out.println(join(sep));
        }
    }

    default void println() {
        supply(System.out::println);
    }

    default SeqList<T> reverse() {
        SeqList<T> ts = toList();
        Collections.reverse(ts.backer);
        return ts;
    }

    default <E> Seq<E> runningFold(E init, BiFunction<E, T, E> function) {
        return c -> fold(init, (e, t) -> {
            e = function.apply(e, t);
            c.accept(e);
            return e;
        });
    }

    default int sizeOrDefault() {
        return 10;
    }

    default <E extends Comparable<E>> SeqList<T> sorted(Function<T, E> function) {
        return sorted(Comparator.comparing(function));
    }

    default SeqList<T> sorted() {
        return sorted((Comparator<T>)null);
    }

    default SeqList<T> sorted(Comparator<T> comparator) {
        SeqList<T> ts = toList();
        ts.backer.sort(comparator);
        return ts;
    }

    default <E extends Comparable<E>> SeqList<T> sortedDesc(Function<T, E> function) {
        return sorted(Comparator.comparing(function).reversed());
    }

    default SeqList<T> sortedDesc() {
        return sorted(Collections.reverseOrder());
    }

    default SeqList<T> sortedDesc(Comparator<T> comparator) {
        return sorted(comparator.reversed());
    }

    default double sum(ToDoubleFunction<T> function) {
        return foldDouble(0, (d, t) -> d + function.applyAsDouble(t));
    }

    default Integer sumInt(ToIntFunction<T> function) {
        return foldInt(0, (i, t) -> i + function.applyAsInt(t));
    }

    default long sumLong(ToLongFunction<T> function) {
        return foldLong(0, (i, t) -> i + function.applyAsLong(t));
    }

    default Seq<T> take(int n) {
        return c -> foldIndexed((i, t) -> {
            if (i < n) {
                c.accept(t);
            } else {
                stop();
            }
        });
    }

    default <E> Seq<T> takeWhile(Function<T, E> function, BiPredicate<E, E> testPrevCurr) {
        return c -> fold((E)null, (last, t) -> {
            E e = function.apply(t);
            if (last == null || testPrevCurr.test(last, e)) {
                c.accept(t);
                return e;
            } else {
                return stop();
            }
        });
    }

    default Seq<T> takeWhile(BiPredicate<T, T> testPrevCurr) {
        return takeWhile(t -> t, testPrevCurr);
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

    default <E> Seq<T> takeWhileEquals(Function<T, E> function) {
        return takeWhile(function, Objects::equals);
    }

    default Seq<T> takeWhileEquals() {
        return takeWhile(t -> t, Objects::equals);
    }

    default int[] toIntArray(ToIntFunction<T> function) {
        BatchList<T> ts = toBatchList();
        int[] a = new int[ts.size()];
        ts.foldIndexed((i, t) -> a[i] = function.applyAsInt(t));
        return a;
    }

    default double[] toDoubleArray(ToDoubleFunction<T> function) {
        BatchList<T> ts = toBatchList();
        double[] a = new double[ts.size()];
        ts.foldIndexed((i, t) -> a[i] = function.applyAsDouble(t));
        return a;
    }

    default long[] toLongArray(ToLongFunction<T> function) {
        BatchList<T> ts = toBatchList();
        long[] a = new long[ts.size()];
        ts.foldIndexed((i, t) -> a[i] = function.applyAsLong(t));
        return a;
    }

    default boolean[] toBooleanArray(Predicate<T> function) {
        BatchList<T> ts = toBatchList();
        boolean[] a = new boolean[ts.size()];
        ts.foldIndexed((i, t) -> a[i] = function.test(t));
        return a;
    }

    default ArrayList<T> toArrayList() {
        return collectBy(ArrayList::new);
    }

    default BatchList<T> toBatchList() {
        return collect(new BatchList<>());
    }

    default BatchList<T> toBatchList(int batchSize) {
        return collect(new BatchList<>(batchSize));
    }

    default SeqList<T> toList() {
        return new SeqList<>(collectBy(ArrayList::new));
    }

    default <K, V> Map<K, V> toMap(Map<K, V> des, Function<T, K> kFunction, Function<T, V> vFunction) {
        return feed(des, (res, t) -> res.put(kFunction.apply(t), vFunction.apply(t)));
    }

    default <K, V> SeqMap<K, V> toMap(Function<T, K> kFunction, Function<T, V> vFunction) {
        return new SeqMap<>(toMap(new HashMap<>(sizeOrDefault()), kFunction, vFunction));
    }

    default <K> Map<K, T> toMapBy(Map<K, T> des, Function<T, K> kFunction) {
        return feed(des, (res, t) -> res.put(kFunction.apply(t), t));
    }

    default <K> SeqMap<K, T> toMapBy(Function<T, K> kFunction) {
        return new SeqMap<>(toMapBy(new HashMap<>(sizeOrDefault()), kFunction));
    }

    default <V> Map<T, V> toMapWith(Map<T, V> des, Function<T, V> vFunction) {
        return feed(des, (res, t) -> res.put(t, vFunction.apply(t)));
    }

    default <V> SeqMap<T, V> toMapWith(Function<T, V> vFunction) {
        return new SeqMap<>(toMapWith(new HashMap<>(sizeOrDefault()), vFunction));
    }

    default T[] toObjArray(IntFunction<T[]> initializer) {
        BatchList<T> ts = toBatchList();
        T[] a = initializer.apply(ts.size());
        ts.foldIndexed((i, t) -> a[i] = t);
        return a;
    }

    default SeqSet<T> toSet() {
        return new SeqSet<>(collectBy(HashSet::new));
    }

    default SinglyList<T> toSinglyList() {
        return collect(new SinglyList<>());
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
        return c -> foldIndexed(start, (i, t) -> c.accept(new IntPair<>(i, t)));
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

    default <E, R> Seq<R> zip(Iterable<E> iterable, BiFunction<T, E, R> function) {
        return c -> feed(iterable.iterator(), (itr, t) -> {
            if (itr.hasNext()) {
                c.accept(function.apply(t, itr.next()));
            } else {
                stop();
            }
        });
    }

    default <E> Seq<Pair<T, E>> zip(Iterable<E> iterable) {
        return zip(iterable, Pair::new);
    }

    default <E> void zipWith(Iterable<E> es, BiConsumer<T, E> consumer) {
        feed(es.iterator(), (itr, t) -> {
            if (itr.hasNext()) {
                consumer.accept(t, itr.next());
            } else {
                stop();
            }
        });
    }

    class Empty {
        static Seq<Object> emptySeq = c -> {};
        static Consumer<Object> nothing = t -> {};
    }

    interface IntObjToInt<T> {
        int apply(int acc, T t);
    }

    interface DoubleObjToDouble<T> {
        double apply(double acc, T t);
    }

    interface LongObjToLong<T> {
        long apply(long acc, T t);
    }

    interface BooleanObjToBoolean<T> {
        boolean apply(boolean acc, T t);
    }

    interface IndexObjConsumer<T> {
        void accept(int i, T t);
    }

    interface IndexObjFunction<T, E> {
        E apply(int i, T t);
    }

    interface IndexObjPredicate<T> {
        boolean test(int i, T t);
    }
}
