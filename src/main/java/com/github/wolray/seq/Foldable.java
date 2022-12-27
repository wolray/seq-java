package com.github.wolray.seq;

import java.util.*;
import java.util.function.*;

/**
 * @author wolray
 */
public interface Foldable<T> extends Foldable0<Consumer<T>> {
    default <E> Folder<E, T> feed(E des, BiConsumer<E, T> consumer) {
        return new AccFolder<E, T>(this, des) {
            @Override
            public void accept(T t) {
                consumer.accept(des, t);
            }
        };
    }

    default <E> Folder<E, T> find(E ifNotFound, Predicate<T> predicate, Function<T, E> function) {
        return new AccFolder<E, T>(this, ifNotFound) {
            @Override
            public void accept(T t) {
                if (predicate.test(t)) {
                    acc = function.apply(t);
                    stop();
                }
            }
        };
    }

    default <E> Folder<E, T> fold(E init, BiFunction<E, T, E> function) {
        return new AccFolder<E, T>(this, init) {
            @Override
            public void accept(T t) {
                acc = function.apply(acc, t);
            }
        };
    }

    default Folder<Integer, T> foldIndexed(IndexObjConsumer<T> consumer) {
        return foldIndexed(0, consumer);
    }

    default Folder<Integer, T> foldIndexed(int start, IndexObjConsumer<T> consumer) {
        return foldInt(start, (i, t) -> {
            consumer.accept(i, t);
            return i + 1;
        });
    }

    default Folder<Integer, T> foldInt(int init, IntObjToInt<T> function) {
        return new Folder<Integer, T>(this) {
            int acc = init;

            @Override
            public Integer get() {
                return acc;
            }

            @Override
            public void accept(T t) {
                acc = function.apply(acc, t);
            }
        };
    }

    default Folder<Double, T> foldDouble(double init, DoubleObjToDouble<T> function) {
        return new Folder<Double, T>(this) {
            double acc = init;

            @Override
            public Double get() {
                return acc;
            }

            @Override
            public void accept(T t) {
                acc = function.apply(acc, t);
            }
        };
    }

    default Folder<Long, T> foldLong(long init, LongObjToLong<T> function) {
        return new Folder<Long, T>(this) {
            long acc = init;

            @Override
            public Long get() {
                return acc;
            }

            @Override
            public void accept(T t) {
                acc = function.apply(acc, t);
            }
        };
    }

    default Folder<Boolean, T> foldBoolean(boolean init, BooleanObjToBoolean<T> function) {
        return new Folder<Boolean, T>(this) {
            boolean acc = init;

            @Override
            public Boolean get() {
                return acc;
            }

            @Override
            public void accept(T t) {
                acc = function.apply(acc, t);
            }
        };
    }

    default Folder<T, T> foldPair(boolean overlapping, BiConsumer<T, T> consumer) {
        return fold(null, (last, t) -> {
            if (last != null) {
                consumer.accept(last, t);
                return overlapping ? t : null;
            }
            return t;
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

    default <V extends Comparable<V>> Folder<Pair<T, V>, T> minWith(Function<T, V> function) {
        return feed(new Pair<>(null, null), (p, t) -> {
            V v = function.apply(t);
            if (p.first == null || p.second.compareTo(v) > 0) {
                p.first = t;
                p.second = v;
            }
        });
    }

    default <V extends Comparable<V>> Folder<T, T> minBy(Function<T, V> function) {
        return minWith(function).map(p -> p.first);
    }

    default Folder<T[], T> toObjArray(IntFunction<T[]> initializer) {
        return toBatchList().map(ls -> {
            T[] a = initializer.apply(ls.size());
            ls.foldIndexed((i, t) -> a[i] = t).eval();
            return a;
        });
    }

    default Folder<int[], T> toIntArray(ToIntFunction<T> function) {
        return toBatchList().map(ls -> {
            int[] a = new int[ls.size()];
            ls.foldIndexed((i, t) -> a[i] = function.applyAsInt(t)).eval();
            return a;
        });
    }

    default Folder<double[], T> toDoubleArray(ToDoubleFunction<T> function) {
        return toBatchList().map(ls -> {
            double[] a = new double[ls.size()];
            ls.foldIndexed((i, t) -> a[i] = function.applyAsDouble(t)).eval();
            return a;
        });
    }

    default Folder<long[], T> toLongArray(ToLongFunction<T> function) {
        return toBatchList().map(ls -> {
            long[] a = new long[ls.size()];
            ls.foldIndexed((i, t) -> a[i] = function.applyAsLong(t)).eval();
            return a;
        });
    }

    default Folder<boolean[], T> toBooleanArray(Predicate<T> function) {
        return toBatchList().map(ls -> {
            boolean[] a = new boolean[ls.size()];
            ls.foldIndexed((i, t) -> a[i] = function.test(t)).eval();
            return a;
        });
    }

    default Folder<SeqList<T>, T> sorted() {
        return sorted((Comparator<T>)null);
    }

    default Folder<SeqList<T>, T> sortedDesc() {
        return sorted(Collections.reverseOrder());
    }

    default Folder<SeqList<T>, T> sorted(Comparator<T> comparator) {
        return toList().then(it -> it.backer.sort(comparator));
    }

    default Folder<SeqList<T>, T> sortedDesc(Comparator<T> comparator) {
        return sorted(comparator.reversed());
    }

    default <E extends Comparable<E>> Folder<SeqList<T>, T> sorted(Function<T, E> function) {
        return sorted(Comparator.comparing(function));
    }

    default <E extends Comparable<E>> Folder<SeqList<T>, T> sortedDesc(Function<T, E> function) {
        return sorted(Comparator.comparing(function).reversed());
    }

    default Folder<SeqList<T>, T> reverse() {
        return toList().then(it -> Collections.reverse(it.backer));
    }

    default int sizeOrDefault() {
        return 10;
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

    default Folder<ArrayList<T>, T> toArrayList() {
        return collectBy(ArrayList::new);
    }

    default Folder<SeqList<T>, T> toList() {
        return collectBy(ArrayList::new).map(SeqList::new);
    }

    default Folder<SeqSet<T>, T> toSet() {
        return collectBy(HashSet::new).map(SeqSet::new);
    }

    default <C extends Collection<T>> Folder<C, T> collectBy(IntFunction<C> bySize) {
        return feed(bySize.apply(sizeOrDefault()), Collection::add);
    }

    default Folder<Pair<BatchList<T>, BatchList<T>>, T> partition(Predicate<T> predicate) {
        return partition(predicate, Foldable::toBatchList);
    }

    default <E> Folder<Pair<E, E>, T> partition(Predicate<T> predicate, ToFolder<E, T> toFolder) {
        return feed(new Pair<>(toFolder.gen(), toFolder.gen()), (p, t) ->
            (predicate.test(t) ? p.first : p.second).accept(t))
            .map(p -> new Pair<>(p.first.get(), p.second.get()));
    }

    default <K, V> Folder<SeqMap<K, V>, T> toMap(Function<T, K> kFunction, Function<T, V> vFunction) {
        return toMap(new HashMap<>(sizeOrDefault()), kFunction, vFunction).map(SeqMap::new);
    }

    default <K> Folder<SeqMap<K, T>, T> toMapBy(Function<T, K> kFunction) {
        return toMapBy(new HashMap<>(sizeOrDefault()), kFunction).map(SeqMap::new);
    }

    default <V> Folder<SeqMap<T, V>, T> toMapWith(Function<T, V> vFunction) {
        return toMapWith(new HashMap<>(sizeOrDefault()), vFunction).map(SeqMap::new);
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

    default Folder<Pair<T, T>, T> firstAndLast() {
        return feed(new Pair<>(null, null), (p, t) -> {
            if (p.first == null) {
                p.first = t;
            }
            p.second = t;
        });
    }

    default <K, V> Folder<SeqMap<K, V>, T> groupBy(Function<T, K> kFunction, ToFolder<V, T> toFolder) {
        return groupBy(new HashMap<>(), kFunction, toFolder);
    }

    default <K, V> Folder<SeqMap<K, V>, T> groupBy(Map<K, Supplier<V>> map,
        Function<T, K> kFunction, ToFolder<V, T> toFolder) {
        Function<K, Supplier<V>> mappingFunction = k -> toFolder.gen();
        return feed(map, (m, t) -> {
            K k = kFunction.apply(t);
            Supplier<V> folder = m.computeIfAbsent(k, mappingFunction);
            ((Folder<V, T>)folder).accept(t);
        }).map(m -> new SeqMap<>(m).replaceValue(Supplier::get));
    }

    default Folder<String, T> join(String sep) {
        return join(sep, String::valueOf);
    }

    default Folder<String, T> join(String sep, Function<T, String> function) {
        return feed(new StringJoiner(sep), (j, t) -> j.add(function.apply(t)))
            .map(StringJoiner::toString);
    }

    abstract class AccFolder<E, T> extends Folder<E, T> {
        protected E acc;

        public AccFolder(Foldable<T> foldable, E init) {
            super(foldable);
            acc = init;
        }

        @Override
        public E get() {
            return acc;
        }
    }

    static <T, R, E> ToFolder<E, T> mapping(Function<T, R> function, ToFolder<E, R> toFolder) {
        return f -> new Folder<E, T>(Seq.empty()) {
            Folder<E, R> folder = toFolder.gen();

            @Override
            public void accept(T r) {
                folder.accept(function.apply(r));
            }

            @Override
            public E get() {
                return folder.get();
            }
        };
    }

    interface ToFolder<E, T> extends Function<Foldable<T>, Folder<E, T>> {
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
