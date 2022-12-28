package com.github.wolray.seq;

import java.util.*;
import java.util.function.*;

/**
 * @author wolray
 */
public interface Transformer<S, T> extends Function<Consumer<T>, Consumer<S>> {
    Seq<S> source();

    default Seq<T> commit() {
        return c -> source().tillStop(apply(c));
    }

    default <E> Transformer<S, E> transform(Function<Consumer<E>, Consumer<S>> function) {
        return new Transformer<S, E>() {
            @Override
            public Seq<S> source() {
                return Transformer.this.source();
            }

            @Override
            public Consumer<S> apply(Consumer<E> consumer) {
                return function.apply(consumer);
            }
        };
    }

    default <E> Transformer<S, E> map(Function<T, E> function) {
        return transform(c -> apply(t -> c.accept(function.apply(t))));
    }

    default <E> Transformer<S, E> mapCe(WithCe.Function<T, E> function) {
        return map(WithCe.mapper(function));
    }

    default <E> Transformer<S, E> mapNotNll(Function<T, E> function) {
        return transform(c -> apply(t -> {
            E e = function.apply(t);
            if (e != null) {
                c.accept(e);
            }
        }));
    }

    default Transformer<S, Pair<T, T>> mapPair(boolean overlapping) {
        return mapPair(overlapping, Pair::new);
    }

    default <E> Transformer<S, E> mapPair(boolean overlapping, BiFunction<T, T, E> function) {
        return transform(c -> fold((T)null, (last, t) -> {
            if (last != null) {
                c.accept(function.apply(last, t));
                return overlapping ? t : null;
            }
            return t;
        }));
    }

    default Transformer<S, T> duplicateEach(int times) {
        return transform(c -> apply(t -> {
            for (int i = 0; i < times; i++) {
                c.accept(t);
            }
        }));
    }

    default Transformer<S, T> duplicateIf(int times, Predicate<T> predicate) {
        return transform(c -> apply(t -> {
            if (predicate.test(t)) {
                for (int i = 0; i < times; i++) {
                    c.accept(t);
                }
            } else {
                c.accept(t);
            }
        }));
    }

    default Transformer<S, T> filter(Predicate<T> predicate) {
        return transform(c -> apply(t -> {
            if (predicate.test(t)) {
                c.accept(t);
            }
        }));
    }

    default Transformer<S, T> filterNot(Predicate<T> predicate) {
        return filter(predicate.negate());
    }

    default Transformer<S, T> filterNotNull() {
        return filter(Objects::nonNull);
    }

    default Transformer<S, T> filterIn(Collection<T> collection) {
        return filter(collection::contains);
    }

    default Transformer<S, T> filterNotIn(Collection<T> collection) {
        return filterNot(collection::contains);
    }

    default Transformer<S, T> filterIn(Map<T, ?> map) {
        return filter(map::containsKey);
    }

    default Transformer<S, T> filterNotIn(Map<T, ?> map) {
        return filterNot(map::containsKey);
    }

    default Transformer<S, T> take(int n) {
        return transform(c -> foldIndexed((i, t) -> {
            if (i < n) {
                c.accept(t);
            } else {
                Seq0.stop();
            }
        }));
    }

    default Transformer<S, T> drop(int n) {
        return forFirst(n, Seq.nothing());
    }

    default Transformer<S, T> forFirst(Consumer<T> consumer) {
        return forFirst(1, consumer);
    }

    default Transformer<S, T> forFirst(int n, Consumer<T> consumer) {
        return transform(c -> foldIndexed((i, t) -> (i >= n ? c : consumer).accept(t)));
    }

    default Transformer<S, T> takeWhile(Predicate<T> predicate) {
        return transform(c -> apply(t -> {
            if (predicate.test(t)) {
                c.accept(t);
            } else {
                Seq0.stop();
            }
        }));
    }

    default Transformer<S, T> takeWhile(BiPredicate<T, T> testPrevCurr) {
        return takeWhile(t -> t, testPrevCurr);
    }

    default <E> Transformer<S, T> takeWhile(Function<T, E> function, BiPredicate<E, E> testPrevCurr) {
        return transform(c -> fold((E)null, (last, t) -> {
            E e = function.apply(t);
            if (last == null || testPrevCurr.test(last, e)) {
                c.accept(t);
                return e;
            } else {
                return Seq0.stop();
            }
        }));
    }

    default Transformer<S, T> takeWhileEquals() {
        return takeWhile(t -> t, Objects::equals);
    }

    default <E> Transformer<S, T> takeWhileEquals(Function<T, E> function) {
        return takeWhile(function, Objects::equals);
    }

    default Transformer<S, T> dropWhile(Predicate<T> predicate) {
        return transform(c -> foldBoolean(false, (b, t) -> {
            if (b || !predicate.test(t)) {
                c.accept(t);
                return true;
            }
            return false;
        }));
    }

    default <E> Transformer<S, T> onFolder(ToFolder<E, T> toFolder) {
        return transform(c -> apply(toFolder.gen().andThen(c)));
    }

    default Transformer<S, T> onEach(Consumer<T> consumer) {
        return transform(c -> apply(consumer.andThen(c)));
    }

    default Transformer<S, T> onFirst(Consumer<T> consumer) {
        return onFirst(1, consumer);
    }

    default Transformer<S, T> onFirst(int n, Consumer<T> consumer) {
        return transform(c -> foldIndexed((i, t) -> {
            if (i >= n) {
                c.accept(t);
            } else {
                consumer.accept(t);
                c.accept(t);
            }
        }));
    }

    default <E> Transformer<S, Pair<T, E>> pairWith(Function<T, E> function) {
        return map(t -> new Pair<>(t, function.apply(t)));
    }

    default Transformer<S, IntPair<T>> withInt(ToIntFunction<T> function) {
        return map(t -> new IntPair<>(function.applyAsInt(t), t));
    }

    default Transformer<S, LongPair<T>> withLong(ToLongFunction<T> function) {
        return map(t -> new LongPair<>(function.applyAsLong(t), t));
    }

    default Transformer<S, IntPair<T>> withIndex() {
        return withIndex(0);
    }

    default Transformer<S, IntPair<T>> withIndex(int start) {
        return transform(c -> foldIndexed(start, (i, t) -> c.accept(new IntPair<>(i, t))));
    }

    default Transformer<S, T> distinct() {
        return distinctBy(it -> it);
    }

    default <E> Transformer<S, T> distinctBy(Function<T, E> function) {
        return transform(c -> feed(new HashSet<>(), (set, t) -> {
            if (set.add(function.apply(t))) {
                c.accept(t);
            }
        }));
    }

    default <E> Transformer<S, E> flatMap(Function<T, Seq<E>> function) {
        return transform(c -> apply(t -> function.apply(t).supply(c)));
    }

    default <E> Transformer<S, E> runningFold(E init, BiFunction<E, T, E> function) {
        return transform(c -> fold(init, (e, t) -> {
            e = function.apply(e, t);
            c.accept(e);
            return e;
        }));
    }

    default <E> Transformer<S, E> mapSub(T first, T last, ToFolder<E, T> function) {
        return mapSub(first::equals, last::equals, function);
    }

    default <E, V> Transformer<S, E> mapSub(V first, V last, Function<T, V> function, ToFolder<E, T> toFolder) {
        return mapSub(t -> first.equals(function.apply(t)), t -> last.equals(function.apply(t)), toFolder);
    }

    default <E> Transformer<S, E> mapSub(Predicate<T> first, Predicate<T> last, ToFolder<E, T> toFolder) {
        return transform(c -> fold((Folder<E, T>)null, (f, t) -> {
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

    default <E> Transformer<S, Pair<T, E>> zip(Iterable<E> iterable) {
        return zip(iterable, Pair::new);
    }

    default <E, R> Transformer<S, R> zip(Iterable<E> iterable, BiFunction<T, E, R> function) {
        return transform(c -> feed(iterable.iterator(), (itr, t) -> {
            if (itr.hasNext()) {
                c.accept(function.apply(t, itr.next()));
            } else {
                Seq0.stop();
            }
        }));
    }

    default <B, C> Transformer<S, Triple<T, B, C>> zip(Iterable<B> bs, Iterable<C> cs) {
        return transform(c -> {
            Iterator<B> bi = bs.iterator();
            Iterator<C> ci = cs.iterator();
            return apply(t -> {
                if (bi.hasNext() && ci.hasNext()) {
                    c.accept(new Triple<>(t, bi.next(), ci.next()));
                } else {
                    Seq0.stop();
                }
            });
        });
    }

    default <E> Folder<E, S> mapFolder(Folder<E, T> folder) {
        Consumer<S> consumer = apply(folder);
        return new Folder<E, S>(source()) {
            @Override
            public void accept(S s) {
                consumer.accept(s);
            }

            @Override
            public E get() {
                return folder.get();
            }
        };
    }

    default <E> Folder<E, S> feed(E des, BiConsumer<E, T> consumer) {
        return mapFolder(new Folder.AccFolder<E, T>(Seq.empty(), des) {
            @Override
            public void accept(T t) {
                consumer.accept(des, t);
            }
        });
    }

    default <E> Folder<E, S> find(E ifNotFound, Predicate<T> predicate, Function<T, E> function) {
        return mapFolder(new Folder.AccFolder<E, T>(Seq.empty(), ifNotFound) {
            @Override
            public void accept(T t) {
                if (predicate.test(t)) {
                    acc = function.apply(t);
                    Seq0.stop();
                }
            }
        });
    }

    default <E> Folder<E, S> fold(E init, BiFunction<E, T, E> function) {
        return mapFolder(new Folder.AccFolder<E, T>(Seq.empty(), init) {
            @Override
            public void accept(T t) {
                acc = function.apply(acc, t);
            }
        });
    }

    default Folder<Integer, S> foldIndexed(IndexObjConsumer<T> consumer) {
        return foldIndexed(0, consumer);
    }

    default Folder<Integer, S> foldIndexed(int start, IndexObjConsumer<T> consumer) {
        return foldInt(start, (i, t) -> {
            consumer.accept(i, t);
            return i + 1;
        });
    }

    default Folder<Integer, S> foldInt(int init, IntObjToInt<T> function) {
        return mapFolder(new Folder<Integer, T>(Seq.empty()) {
            int acc = init;

            @Override
            public Integer get() {
                return acc;
            }

            @Override
            public void accept(T t) {
                acc = function.apply(acc, t);
            }
        });
    }

    default Folder<Double, S> foldDouble(double init, DoubleObjToDouble<T> function) {
        return mapFolder(new Folder<Double, T>(Seq.empty()) {
            double acc = init;

            @Override
            public Double get() {
                return acc;
            }

            @Override
            public void accept(T t) {
                acc = function.apply(acc, t);
            }
        });
    }

    default Folder<Long, S> foldLong(long init, LongObjToLong<T> function) {
        return mapFolder(new Folder<Long, T>(Seq.empty()) {
            long acc = init;

            @Override
            public Long get() {
                return acc;
            }

            @Override
            public void accept(T t) {
                acc = function.apply(acc, t);
            }
        });
    }

    default Folder<Boolean, S> foldBoolean(boolean init, BooleanObjToBoolean<T> function) {
        return mapFolder(new Folder<Boolean, T>(Seq.empty()) {
            boolean acc = init;

            @Override
            public Boolean get() {
                return acc;
            }

            @Override
            public void accept(T t) {
                acc = function.apply(acc, t);
            }
        });
    }

    default Folder<Boolean, S> any(boolean ifFound, Predicate<T> predicate) {
        return find(!ifFound, predicate, t -> ifFound);
    }

    default Folder<Boolean, S> any(Predicate<T> predicate) {
        return any(true, predicate);
    }

    default Folder<Boolean, S> anyNot(Predicate<T> predicate) {
        return any(predicate.negate());
    }

    default Folder<Boolean, S> all(Predicate<T> predicate) {
        return any(false, predicate.negate());
    }

    default Folder<Boolean, S> none(Predicate<T> predicate) {
        return any(false, predicate);
    }

    default Folder<T, S> first() {
        return find(null, t -> true, t -> t);
    }

    default Folder<T, S> firstNotNull() {
        return first(Objects::nonNull);
    }

    default Folder<T, S> first(Predicate<T> predicate) {
        return find(null, predicate, t -> t);
    }

    default Folder<T, S> firstNot(Predicate<T> predicate) {
        return first(predicate.negate());
    }

    default Folder<T, S> last() {
        return fold(null, (res, t) -> t);
    }

    default Folder<T, S> last(Predicate<T> predicate) {
        return fold(null, (res, t) -> predicate.test(t) ? t : res);
    }

    default Folder<T, S> lastNot(Predicate<T> predicate) {
        return last(predicate.negate());
    }

    default Folder<Integer, S> count() {
        return foldInt(0, (i, t) -> i + 1);
    }

    default Folder<Integer, S> count(Predicate<T> predicate) {
        return sumInt(t -> predicate.test(t) ? 1 : 0);
    }

    default Folder<Integer, S> countNot(Predicate<T> predicate) {
        return count(predicate.negate());
    }

    default Folder<Double, S> sum(ToDoubleFunction<T> function) {
        return foldDouble(0, (d, t) -> d + function.applyAsDouble(t));
    }

    default Folder<Integer, S> sumInt(ToIntFunction<T> function) {
        return foldInt(0, (i, t) -> i + function.applyAsInt(t));
    }

    default Folder<Long, S> sumLong(ToLongFunction<T> function) {
        return foldLong(0, (i, t) -> i + function.applyAsLong(t));
    }

    default Folder<Double, S> average(ToDoubleFunction<T> function) {
        return average(function, null);
    }

    default Folder<Double, S> average(ToDoubleFunction<T> function, ToDoubleFunction<T> weightFunction) {
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
        return feed(new double[]{0, 0}, biConsumer).map(a -> a[1] != 0 ? a[0] / a[1] : 0);
    }

    default Folder<T, S> max(Comparator<T> comparator) {
        return fold(null, (f, t) -> f == null || comparator.compare(f, t) < 0 ? t : f);
    }

    default <V extends Comparable<V>> Folder<Pair<T, V>, S> maxWith(Function<T, V> function) {
        return feed(new Pair<>(null, null), (p, t) -> {
            V v = function.apply(t);
            if (p.first == null || p.second.compareTo(v) < 0) {
                p.first = t;
                p.second = v;
            }
        });
    }

    default <V extends Comparable<V>> Folder<T, S> maxBy(Function<T, V> function) {
        return maxWith(function).map(p -> p.first);
    }

    default Folder<T, S> min(Comparator<T> comparator) {
        return fold(null, (f, t) -> f == null || comparator.compare(f, t) > 0 ? t : f);
    }

    default <V extends Comparable<V>> Folder<Pair<T, V>, S> minWith(Function<T, V> function) {
        return feed(new Pair<>(null, null), (p, t) -> {
            V v = function.apply(t);
            if (p.first == null || p.second.compareTo(v) > 0) {
                p.first = t;
                p.second = v;
            }
        });
    }

    default <V extends Comparable<V>> Folder<T, S> minBy(Function<T, V> function) {
        return minWith(function).map(p -> p.first);
    }

    default Folder<T[], S> toObjArray(IntFunction<T[]> initializer) {
        return toBatchList().map(ls -> {
            T[] a = initializer.apply(ls.size());
            ls.supply(ls.foldIndexed((i, t) -> a[i] = t));
            return a;
        });
    }

    default Folder<int[], S> toIntArray(ToIntFunction<T> function) {
        return toBatchList().map(ls -> {
            int[] a = new int[ls.size()];
            ls.supply(ls.foldIndexed((i, t) -> a[i] = function.applyAsInt(t)));
            return a;
        });
    }

    default Folder<double[], S> toDoubleArray(ToDoubleFunction<T> function) {
        return toBatchList().map(ls -> {
            double[] a = new double[ls.size()];
            ls.supply(ls.foldIndexed((i, t) -> a[i] = function.applyAsDouble(t)));
            return a;
        });
    }

    default Folder<long[], S> toLongArray(ToLongFunction<T> function) {
        return toBatchList().map(ls -> {
            long[] a = new long[ls.size()];
            ls.supply(ls.foldIndexed((i, t) -> a[i] = function.applyAsLong(t)));
            return a;
        });
    }

    default Folder<boolean[], S> toBooleanArray(Predicate<T> function) {
        return toBatchList().map(ls -> {
            boolean[] a = new boolean[ls.size()];
            ls.supply(ls.foldIndexed((i, t) -> a[i] = function.test(t)));
            return a;
        });
    }

    default Folder<SeqList<T>, S> sorted() {
        return sorted((Comparator<T>)null);
    }

    default Folder<SeqList<T>, S> sortedDesc() {
        return sorted(Collections.reverseOrder());
    }

    default Folder<SeqList<T>, S> sorted(Comparator<T> comparator) {
        return toList().then(it -> it.backer.sort(comparator));
    }

    default Folder<SeqList<T>, S> sortedDesc(Comparator<T> comparator) {
        return sorted(comparator.reversed());
    }

    default <E extends Comparable<E>> Folder<SeqList<T>, S> sorted(Function<T, E> function) {
        return sorted(Comparator.comparing(function));
    }

    default <E extends Comparable<E>> Folder<SeqList<T>, S> sortedDesc(Function<T, E> function) {
        return sorted(Comparator.comparing(function).reversed());
    }

    default Folder<SeqList<T>, S> reverse() {
        return toList().then(it -> Collections.reverse(it.backer));
    }

    default int sizeOrDefault() {
        return 10;
    }

    default Folder<SinglyList<T>, S> toSinglyList() {
        return collect(new SinglyList<>());
    }

    default Folder<BatchList<T>, S> toBatchList() {
        return collect(new BatchList<>());
    }

    default Folder<BatchList<T>, S> toBatchList(int batchSize) {
        return collect(new BatchList<>(batchSize));
    }

    default <C extends Collection<T>> Folder<C, S> collect(C des) {
        return feed(des, Collection::add);
    }

    default Folder<ArrayList<T>, S> toArrayList() {
        return collectBy(ArrayList::new);
    }

    default Folder<SeqList<T>, S> toList() {
        return collectBy(ArrayList::new).map(SeqList::new);
    }

    default Folder<SeqSet<T>, S> toSet() {
        return collectBy(HashSet::new).map(SeqSet::new);
    }

    default <C extends Collection<T>> Folder<C, S> collectBy(IntFunction<C> bySize) {
        return feed(bySize.apply(sizeOrDefault()), Collection::add);
    }

    default Folder<Pair<BatchList<T>, BatchList<T>>, S> partition(Predicate<T> predicate) {
        return partition(predicate, Transformer::toBatchList);
    }

    default <E> Folder<Pair<E, E>, S> partition(Predicate<T> predicate, Function<Seq<T>, Folder<E, T>> toFolder) {
        return feed(new Pair<>(toFolder.apply(Seq.empty()), toFolder.apply(Seq.empty())), (p, t) ->
            (predicate.test(t) ? p.first : p.second).accept(t))
            .map(p -> new Pair<>(p.first.get(), p.second.get()));
    }

    default <K, V> Folder<SeqMap<K, V>, S> toMap(Function<T, K> kFunction, Function<T, V> vFunction) {
        return toMap(new HashMap<>(sizeOrDefault()), kFunction, vFunction).map(SeqMap::new);
    }

    default <K> Folder<SeqMap<K, T>, S> toMapBy(Function<T, K> kFunction) {
        return toMapBy(new HashMap<>(sizeOrDefault()), kFunction).map(SeqMap::new);
    }

    default <V> Folder<SeqMap<T, V>, S> toMapWith(Function<T, V> vFunction) {
        return toMapWith(new HashMap<>(sizeOrDefault()), vFunction).map(SeqMap::new);
    }

    default <K, V> Folder<Map<K, V>, S> toMap(Map<K, V> des, Function<T, K> kFunction, Function<T, V> vFunction) {
        return feed(des, (res, t) -> res.put(kFunction.apply(t), vFunction.apply(t)));
    }

    default <K> Folder<Map<K, T>, S> toMapBy(Map<K, T> des, Function<T, K> kFunction) {
        return feed(des, (res, t) -> res.put(kFunction.apply(t), t));
    }

    default <V> Folder<Map<T, V>, S> toMapWith(Map<T, V> des, Function<T, V> vFunction) {
        return feed(des, (res, t) -> res.put(t, vFunction.apply(t)));
    }

    default Folder<Pair<T, T>, S> firstAndLast() {
        return feed(new Pair<>(null, null), (p, t) -> {
            if (p.first == null) {
                p.first = t;
            }
            p.second = t;
        });
    }

    default <K, V> Folder<SeqMap<K, V>, S> groupBy(Function<T, K> kFunction, ToFolder<V, T> toFolder) {
        return groupBy(new HashMap<>(), kFunction, toFolder);
    }

    default <K, V> Folder<SeqMap<K, V>, S> groupBy(Map<K, Supplier<V>> map,
        Function<T, K> kFunction, ToFolder<V, T> toFolder) {
        Function<K, Supplier<V>> mappingFunction = k -> toFolder.gen();
        return feed(map, (m, t) -> {
            K k = kFunction.apply(t);
            Supplier<V> folder = m.computeIfAbsent(k, mappingFunction);
            ((Folder<V, T>)folder).accept(t);
        }).map(m -> new SeqMap<>(m).replaceValue(Supplier::get));
    }

    default Folder<String, S> join(String sep) {
        return join(sep, String::valueOf);
    }

    default Folder<String, S> join(String sep, Function<T, String> function) {
        return feed(new StringJoiner(sep), (j, t) -> j.add(function.apply(t)))
            .map(StringJoiner::toString);
    }

    default void assertTo(String s) {
        assertTo(",", s);
    }

    default void assertTo(String sep, String s) {
        String result = join(sep).eval();
        assert result.equals(s) : result;
    }

    interface ToFolder<E, T> extends Function<Seq<T>, Folder<E, T>> {
        default Folder<E, T> gen() {
            return apply(Seq.empty());
        }
    }

    interface IndexObjConsumer<T> {
        void accept(int i, T t);
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
}
