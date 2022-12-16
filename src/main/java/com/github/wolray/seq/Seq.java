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

    default <E> Folder<E, T> feed(E des, BiConsumer<E, T> consumer) {
        return new Folder<E, T>(this) {
            @Override
            public E get() {
                return des;
            }

            @Override
            public void accept(T t) {
                consumer.accept(des, t);
            }
        };
    }

    default <E> Folder<E, T> find(E ifNotFound, Predicate<T> predicate, Function<T, E> function) {
        return new Folder<E, T>(this) {
            E e = ifNotFound;

            @Override
            public E get() {
                return e;
            }

            @Override
            public void accept(T t) {
                if (predicate.test(t)) {
                    e = function.apply(t);
                    stop();
                }
            }
        };
    }

    default <E> Folder<E, T> fold(E init, BiFunction<E, T, E> function) {
        return new Folder<E, T>(this) {
            E e = init;

            @Override
            public E get() {
                return e;
            }

            @Override
            public void accept(T t) {
                e = function.apply(e, t);
            }
        };
    }

    default Folder<Integer, T> foldIndexed(IndexedConsumer<T> consumer) {
        return foldIndexed(0, consumer);
    }

    default Folder<Integer, T> foldIndexed(int start, IndexedConsumer<T> consumer) {
        return foldInt(start, (i, t) -> {
            consumer.accept(i, t);
            return i + 1;
        });
    }

    default Folder<Integer, T> foldInt(int init, IntBiFunction<T> function) {
        return new Folder<Integer, T>(this) {
            int i = init;

            @Override
            public Integer get() {
                return i;
            }

            @Override
            public void accept(T t) {
                i = function.apply(i, t);
            }
        };
    }

    default Folder<Double, T> foldDouble(double init, DoubleBiFunction<T> function) {
        return new Folder<Double, T>(this) {
            double d = init;

            @Override
            public Double get() {
                return d;
            }

            @Override
            public void accept(T t) {
                d = function.apply(d, t);
            }
        };
    }

    default Folder<Long, T> foldLong(long init, LongBiFunction<T> function) {
        return new Folder<Long, T>(this) {
            long i = init;

            @Override
            public Long get() {
                return i;
            }

            @Override
            public void accept(T t) {
                i = function.apply(i, t);
            }
        };
    }

    default Folder<Boolean, T> foldBool(boolean init, BoolBiFunction<T> function) {
        return new Folder<Boolean, T>(this) {
            boolean b = init;

            @Override
            public Boolean get() {
                return b;
            }

            @Override
            public void accept(T t) {
                b = function.apply(b, t);
            }
        };
    }

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

    default Seq<T> onEachIndexed(IndexedConsumer<T> consumer) {
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
        return c -> {
            Folder<T, T> folder = fold(null, (m, t) -> {
                c.accept(t);
                return t;
            });
            eval(folder);
            consumer.accept(folder.get());
        };
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
        return c -> {
            Folder<ArrayList<T>, T> folder = toArrayList();
            SeqUtil.permute(c, folder.eval(), 0, inplace);
        };
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

    default Folder<Boolean, T> any(boolean ifFound, Predicate<T> predicate) {
        return find(!ifFound, predicate, t -> ifFound);
    }

    default Folder<Boolean, T> any(Predicate<T> predicate) {
        return any(true, predicate);
    }

    default Folder<Boolean, T> anyNot(Predicate<T> predicate) {
        return any(predicate.negate());
    }

    default Folder<Boolean, T> all(Predicate<T> predicate) {
        return any(false, predicate.negate());
    }

    default Folder<Boolean, T> none(Predicate<T> predicate) {
        return any(false, predicate);
    }

    default Folder<T, T> first() {
        return find(null, t -> true, t -> t);
    }

    default Folder<T, T> firstNotNull() {
        return first(Objects::nonNull);
    }

    default Folder<T, T> first(Predicate<T> predicate) {
        return find(null, predicate, t -> t);
    }

    default Folder<T, T> firstNot(Predicate<T> predicate) {
        return first(predicate.negate());
    }

    default Folder<T, T> last() {
        return fold(null, (res, t) -> t);
    }

    default Folder<T, T> last(Predicate<T> predicate) {
        return fold(null, (res, t) -> predicate.test(t) ? t : res);
    }

    default Folder<T, T> lastNot(Predicate<T> predicate) {
        return last(predicate.negate());
    }

    default Folder<Integer, T> count() {
        return foldInt(0, (i, t) -> i + 1);
    }

    default Folder<Integer, T> count(Predicate<T> predicate) {
        return sumInt(t -> predicate.test(t) ? 1 : 0);
    }

    default Folder<Integer, T> countNot(Predicate<T> predicate) {
        return count(predicate.negate());
    }

    default Folder<Double, T> sum(ToDoubleFunction<T> function) {
        return foldDouble(0, (d, t) -> d + function.applyAsDouble(t));
    }

    default Folder<Integer, T> sumInt(ToIntFunction<T> function) {
        return foldInt(0, (i, t) -> i + function.applyAsInt(t));
    }

    default Folder<Long, T> sumLong(ToLongFunction<T> function) {
        return foldLong(0, (i, t) -> i + function.applyAsLong(t));
    }

    default Folder<Double, T> average(ToDoubleFunction<T> function) {
        return average(function, null);
    }

    default Folder<Double, T> average(ToDoubleFunction<T> function, ToDoubleFunction<T> weightFunction) {
        return new Folder<Double, T>(this) {
            double sum;
            double weight;

            @Override
            public Double get() {
                return weight != 0 ? sum / weight : 0;
            }

            @Override
            public void accept(T t) {
                if (weightFunction != null) {
                    double v = function.applyAsDouble(t);
                    double w = weightFunction.applyAsDouble(t);
                    sum += v * w;
                    weight += w;
                } else {
                    sum += function.applyAsDouble(t);
                    weight += 1;
                }
            }
        };
    }

    default Folder<T, T> max(Comparator<T> comparator) {
        return fold(null, (f, t) -> f == null || comparator.compare(f, t) < 0 ? t : f);
    }

    default <V extends Comparable<V>> Folder<Pair<T, V>, T> maxWith(Function<T, V> function) {
        return feed(new Pair<>(null, null), (p, t) -> {
            V v = function.apply(t);
            if (p.first == null || p.second.compareTo(v) < 0) {
                p.first = t;
                p.second = v;
            }
        });
    }

    default <V extends Comparable<V>> Folder<T, T> maxBy(Function<T, V> function) {
        return maxWith(function).map(p -> p.first);
    }

    default Folder<T, T> min(Comparator<T> comparator) {
        return fold(null, (f, t) -> f == null || comparator.compare(f, t) > 0 ? t : f);
    }

    default <V extends Comparable<V>> Pair<T, V> minWith(Function<T, V> function) {
        Pair<T, V> pair = new Pair<>(null, null);
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

    @SuppressWarnings("unchecked")
    default <E> E[] toArrayTo(IntFunction<E[]> generator) {
        List<T> list = toBatchList().eval();
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

    default Folder<ArrayList<T>, T> toArrayList() {
        return collectBy(ArrayList::new);
    }

    default Folder<SinglyList<T>, T> toSinglyList() {
        return collect(new SinglyList<>());
    }

    default Folder<BatchList<T>, T> toBatchList() {
        return collect(new BatchList<>());
    }

    default Folder<BatchList<T>, T> toBatchList(int batchSize) {
        return collect(new BatchList<>(batchSize));
    }

    default <C extends Collection<T>> Folder<C, T> collect(C des) {
        return feed(des, Collection::add);
    }

    default int sizeOrDefault() {
        return 10;
    }

    default Folder<SeqSet<T>, T> toSet() {
        return collectBy(HashSet::new).map(SeqSet::new);
    }

    default Folder<SeqList<T>, T> toList() {
        return collectBy(ArrayList::new).map(SeqList::new);
    }

    default <C extends Collection<T>> Folder<C, T> collectBy(IntFunction<C> bySize) {
        return feed(bySize.apply(sizeOrDefault()), Collection::add);
    }

    default Folder<Pair<BatchList<T>, BatchList<T>>, T> partition(Predicate<T> predicate) {
        return fold(new Pair<>(new BatchList<>(), new BatchList<>()), (p, t) -> {
            (predicate.test(t) ? p.first : p.second).add(t);
            return p;
        });
    }

    default Seq<T> cache() {
        return toBatchList().eval();
    }

    default <K, V> SeqMap<K, Supplier<V>> groupBy(Function<T, K> kFunction, Seq.ToFolder<T, V> toFolder) {
        return groupBy(kFunction, null, 10, toFolder);
    }

    default <K, V> SeqMap<K, Supplier<V>> groupBy(Function<T, K> kFunction, int groupSize, Seq.ToFolder<T, V> toFolder) {
        return groupBy(kFunction, null, groupSize, toFolder);
    }

    default <K, V> SeqMap<K, Supplier<V>> groupBy(Function<T, K> kFunction, Class<? extends Map<?, ?>> mapClass, Seq.ToFolder<T, V> toFolder) {
        return groupBy(kFunction, mapClass, 10, toFolder);
    }

    default <K, V> SeqMap<K, Supplier<V>> groupBy(Function<T, K> kFunction, Class<? extends Map<?, ?>> mapClass, int groupSize, Seq.ToFolder<T, V> toFolder) {
        Map<K, Supplier<V>> map = SeqMap.makeMap(groupSize, mapClass);
        eval(t -> {
            K k = kFunction.apply(t);
            Supplier<V> folder = map.computeIfAbsent(k, it -> toFolder.gen());
            ((Seq.Folder<V, T>)folder).accept(t);
        });
        return new SeqMap<>(map);
    }

    default <K, V> Folder<Map<K, V>, T> toMap(Function<T, K> kFunction, Function<T, V> vFunction) {
        return toMap(new HashMap<>(sizeOrDefault()), kFunction, vFunction);
    }

    default <K> Folder<Map<K, T>, T> toMapBy(Function<T, K> kFunction) {
        return toMapBy(new HashMap<>(sizeOrDefault()), kFunction);
    }

    default <V> Folder<Map<T, V>, T> toMapWith(Function<T, V> vFunction) {
        return toMapWith(new HashMap<>(sizeOrDefault()), vFunction);
    }

    default <K, V> Folder<Map<K, V>, T> toMap(Map<K, V> des, Function<T, K> kFunction, Function<T, V> vFunction) {
        return feed(des, (res, t) -> res.put(kFunction.apply(t), vFunction.apply(t)));
    }

    default <K> Folder<Map<K, T>, T> toMapBy(Map<K, T> des, Function<T, K> kFunction) {
        return feed(des, (res, t) -> res.put(kFunction.apply(t), t));
    }

    default <V> Folder<Map<T, V>, T> toMapWith(Map<T, V> des, Function<T, V> vFunction) {
        return feed(des, (res, t) -> res.put(t, vFunction.apply(t)));
    }

    default Folder<String, T> join(String sep) {
        return join(sep, String::valueOf);
    }

    default Folder<String, T> join(String sep, Function<T, String> function) {
        return feed(new StringJoiner(sep), (j, t) -> j.add(function.apply(t))).map(StringJoiner::toString);
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

    abstract class Folder<E, T> implements Consumer<T>, Supplier<E> {
        private final Seq<T> seq;

        public Folder(Seq<T> seq) {
            this.seq = seq;
        }

        public E eval() {
            seq.tillStop(this);
            return get();
        }

        public <R> Folder<R, T> map(Function<E, R> function) {
            return new Folder<R, T>(seq) {
                @Override
                public R get() {
                    return function.apply(Folder.this.get());
                }

                @Override
                public void accept(T t) {
                    Folder.this.accept(t);
                }
            };
        }
    }

    interface ToFolder<T, E> extends Function<Seq<T>, Folder<E, T>> {
        default Folder<E, T> gen() {
            return apply(Seq.empty());
        }
    }

    interface IndexedConsumer<T> {
        void accept(int i, T t);
    }

    interface IntBiFunction<T> {
        int apply(int i, T t);
    }

    interface DoubleBiFunction<T> {
        double apply(double i, T t);
    }

    interface LongBiFunction<T> {
        long apply(long i, T t);
    }

    interface BoolBiFunction<T> {
        boolean apply(boolean b, T t);
    }

    interface BoolFunction<T> {
        T apply(boolean b);
    }

    interface ParallelSeq<T> extends Seq<T> {}
}
