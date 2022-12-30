package com.github.wolray.seq;

import java.util.HashSet;
import java.util.Set;
import java.util.function.*;

/**
 * @author wolray
 */
public interface IntSeq extends Seq0<IntConsumer> {
    IntSeq empty = c -> {};
    IntConsumer nothing = t -> {};

    static IntSeq gen(int seed, IntUnaryOperator operator) {
        return c -> {
            int t = seed;
            c.accept(t);
            while (true) {
                c.accept(t = operator.applyAsInt(t));
            }
        };
    }

    static IntSeq gen(int seed1, int seed2, IntBinaryOperator operator) {
        return c -> {
            int t1 = seed1, t2 = seed2;
            c.accept(t1);
            c.accept(t2);
            while (true) {
                c.accept(t2 = operator.applyAsInt(t1, t1 = t2));
            }
        };
    }

    static IntSeq gen(IntSupplier supplier) {
        return c -> {
            while (true) {
                c.accept(supplier.getAsInt());
            }
        };
    }

    static IntSeq of(CharSequence cs) {
        return c -> {
            for (int i = 0; i < cs.length(); i++) {
                c.accept(cs.charAt(i));
            }
        };
    }

    static IntSeq of(int... ts) {
        return c -> {
            for (int t : ts) {
                c.accept(t);
            }
        };
    }

    static IntSeq range(int start, int ub) {
        return range(start, ub, 1);
    }

    static IntSeq range(int start, int ub, int step) {
        return c -> {
            for (int i = start; i < ub; i += step) {
                c.accept(i);
            }
        };
    }

    static IntSeq range(int ub) {
        return range(0, ub, 1);
    }

    static IntSeq repeat(int n, int value) {
        return c -> {
            for (int i = 0; i < n; i++) {
                c.accept(value);
            }
        };
    }

    default boolean all(IntPredicate predicate) {
        return any(false, predicate.negate());
    }

    default boolean any(boolean ifFound, IntPredicate predicate) {
        return find(!ifFound, predicate, t -> ifFound);
    }

    default boolean any(IntPredicate predicate) {
        return any(true, predicate);
    }

    default boolean anyNot(IntPredicate predicate) {
        return any(predicate.negate());
    }

    default IntSeq append(int t, int... more) {
        return c -> {
            supply(c);
            c.accept(t);
            for (int x : more) {
                c.accept(x);
            }
        };
    }

    default IntSeq appendWith(IntSeq seq) {
        return c -> {
            supply(c);
            seq.supply(c);
        };
    }

    default double average() {
        return average(null);
    }

    default double average(IntToDoubleFunction weightFunction) {
        double[] a = new double[]{0, 0};
        supply(t -> {
            if (weightFunction != null) {
                double w = weightFunction.applyAsDouble(t);
                a[0] += t * w;
                a[1] += w;
            } else {
                a[0] += t;
                a[1] += 1;
            }
        });
        return a[1] != 0 ? a[0] / a[1] : 0;
    }

    default Seq<Integer> boxed() {
        return c -> supply(c::accept);
    }

    default IntSeq circle() {
        return c -> {
            while (true) {
                supply(c);
            }
        };
    }

    default int count() {
        return foldInt(0, (i, t) -> i + 1);
    }

    default int count(IntPredicate predicate) {
        return sum(t -> predicate.test(t) ? 1 : 0);
    }

    default int countNot(IntPredicate predicate) {
        return count(predicate.negate());
    }

    default IntSeq distinct() {
        return c -> {
            Set<Integer> set = new HashSet<>();
            supply(t -> {
                if (set.add(t)) {
                    c.accept(t);
                }
            });
        };
    }

    default IntSeq drop(int n) {
        return forFirst(n, nothing);
    }

    default IntSeq dropWhile(IntPredicate predicate) {
        return c -> foldBoolean(false, (b, t) -> {
            if (b || !predicate.test(t)) {
                c.accept(t);
                return true;
            }
            return false;
        });
    }

    default IntSeq duplicateAll(int times) {
        return c -> {
            for (int i = 0; i < times; i++) {
                supply(c);
            }
        };
    }

    default IntSeq duplicateEach(int times) {
        return c -> supply(t -> {
            for (int i = 0; i < times; i++) {
                c.accept(t);
            }
        });
    }

    default IntSeq duplicateIf(int times, IntPredicate predicate) {
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

    default IntSeq filter(IntPredicate predicate) {
        return c -> supply(t -> {
            if (predicate.test(t)) {
                c.accept(t);
            }
        });
    }

    default IntSeq filterIndexed(IndexIntPredicate predicate) {
        return c -> foldIndexed((i, t) -> {
            if (predicate.test(i, t)) {
                c.accept(t);
            }
        });
    }

    default IntSeq filterNot(IntPredicate predicate) {
        return filter(predicate.negate());
    }

    default <E> E find(E ifNotFound, IntPredicate predicate, IntFunction<E> function) {
        Mutable<E> m = new Mutable<>(ifNotFound);
        tillStop(t -> {
            if (predicate.test(t)) {
                m.it = function.apply(t);
                stop();
            }
        });
        return m.it;
    }

    default Integer first() {
        return find(null, t -> true, t -> t);
    }

    default Integer first(IntPredicate predicate) {
        return find(null, predicate, t -> t);
    }

    default Integer firstNot(IntPredicate predicate) {
        return first(predicate.negate());
    }

    default IntSeq flatMap(IntFunction<IntSeq> function) {
        return c -> supply(t -> function.apply(t).supply(c));
    }

    default <E> E fold(E init, ObjIntToObj<E> function) {
        Mutable<E> m = new Mutable<>(init);
        tillStop(t -> m.it = function.apply(m.it, t));
        return m.it;
    }

    default int foldInt(int init, IntBinaryOperator function) {
        int[] a = new int[]{init};
        tillStop(i -> a[0] = function.applyAsInt(a[0], i));
        return a[0];
    }

    default double foldDouble(double init, DoubleBinaryOperator function) {
        double[] a = new double[]{init};
        tillStop(i -> a[0] = function.applyAsDouble(a[0], i));
        return a[0];
    }

    default long foldLong(long init, LongIntToLong function) {
        long[] a = new long[]{init};
        tillStop(i -> a[0] = function.apply(a[0], i));
        return a[0];
    }

    default boolean foldBoolean(boolean init, BooleanIntToBoolean function) {
        boolean[] a = new boolean[]{init};
        tillStop(i -> a[0] = function.apply(a[0], i));
        return a[0];
    }

    default int foldIndexed(IndexIntConsumer consumer) {
        return foldIndexed(0, consumer);
    }

    default int foldIndexed(int start, IndexIntConsumer consumer) {
        return foldInt(start, (i, t) -> {
            consumer.accept(i, t);
            return i + 1;
        });
    }

    default IntSeq forFirst(int n, IntConsumer consumer) {
        return c -> foldIndexed((i, t) -> (i >= n ? c : consumer).accept(t));
    }

    default IntSeq forFirst(IntConsumer consumer) {
        return forFirst(1, consumer);
    }

    default Integer last() {
        return fold(null, (res, t) -> t);
    }

    default Integer last(IntPredicate predicate) {
        return fold(null, (res, t) -> predicate.test(t) ? t : res);
    }

    default Integer lastNot(IntPredicate predicate) {
        return last(predicate.negate());
    }

    default IntSeq map(IntUnaryOperator function) {
        return c -> supply(t -> c.accept(function.applyAsInt(t)));
    }

    default IntSeq mapIndexed(IndexIntToInt function) {
        return c -> foldIndexed((i, t) -> c.accept(function.apply(i, t)));
    }

    default <E> Seq<E> mapToObj(IntFunction<E> function) {
        return c -> supply(t -> c.accept(function.apply(t)));
    }

    default Integer max() {
        return fold(null, (f, t) -> f == null || f < t ? t : f);
    }

    default Integer min() {
        return fold(null, (f, t) -> f == null || f > t ? t : f);
    }

    default boolean none(IntPredicate predicate) {
        return any(false, predicate);
    }

    default IntSeq onEach(IntConsumer consumer) {
        return c -> supply(consumer.andThen(c));
    }

    default IntSeq onEachIndexed(IndexIntConsumer consumer) {
        return c -> foldIndexed((i, t) -> {
            consumer.accept(i, t);
            c.accept(t);
        });
    }

    default IntSeq runningFold(int init, IntBinaryOperator function) {
        return c -> foldInt(init, (acc, t) -> {
            acc = function.applyAsInt(acc, t);
            c.accept(acc);
            return acc;
        });
    }

    default int sum() {
        return foldInt(0, Integer::sum);
    }

    default int sum(IntUnaryOperator function) {
        return foldInt(0, (i, t) -> i + function.applyAsInt(t));
    }

    default IntSeq take(int n) {
        return c -> foldIndexed((i, t) -> {
            if (i < n) {
                c.accept(t);
            } else {
                stop();
            }
        });
    }

    default IntSeq takeWhile(IntPredicate predicate) {
        return c -> tillStop(t -> {
            if (predicate.test(t)) {
                c.accept(t);
            } else {
                stop();
            }
        });
    }

    interface LongIntToLong {
        long apply(long acc, int t);
    }

    interface BooleanIntToBoolean {
        boolean apply(boolean acc, int t);
    }

    interface IndexIntConsumer {
        void accept(int i, int t);
    }

    interface IndexIntPredicate {
        boolean test(int i, int t);
    }

    interface IndexIntToInt {
        int apply(int i, int t);
    }

    interface ObjIntToObj<E> {
        E apply(E e, int t);
    }
}
