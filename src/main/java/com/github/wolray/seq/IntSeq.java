package com.github.wolray.seq;

import java.util.HashSet;
import java.util.Set;
import java.util.function.*;

/**
 * @author wolray
 */
public interface IntSeq {
    IntConsumer nothing = t -> {};
    IntSeq empty = c -> {};

    void eval(IntConsumer consumer);

    default void tillStop(IntConsumer consumer) {
        try {
            eval(consumer);
        } catch (Seq.StopException ignore) {}
    }

    default int evalIndexed(IndexedIntConsumer consumer) {
        return evalIndexed(0, consumer);
    }

    default int evalIndexed(int start, IndexedIntConsumer consumer) {
        int[] a = new int[]{start};
        tillStop(t -> consumer.accept(a[0]++, t));
        return a[0];
    }

    default int evalOnInt(int init, IntBiConsumer<int[]> consumer) {
        int[] a = new int[]{init};
        tillStop(t -> consumer.accept(a, t));
        return a[0];
    }

    default boolean evalOnBool(boolean init, IntBiConsumer<boolean[]> consumer) {
        boolean[] a = new boolean[]{init};
        tillStop(t -> consumer.accept(a, t));
        return a[0];
    }

    static IntSeq of(int... ts) {
        return c -> {
            for (int t : ts) {
                c.accept(t);
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

    static IntSeq range(int ub) {
        return range(0, ub, 1);
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

    static IntSeq repeat(int n, int value) {
        return c -> {
            for (int i = 0; i < n; i++) {
                c.accept(value);
            }
        };
    }

    default Seq<Integer> boxed() {
        return c -> eval(c::accept);
    }

    default <E> Seq<E> map(IntFunction<E> function) {
        return c -> eval(t -> c.accept(function.apply(t)));
    }

    default IntSeq onEach(IntConsumer consumer) {
        return c -> eval(consumer.andThen(c));
    }

    default IntSeq onEachIndexed(IndexedIntConsumer consumer) {
        return c -> evalIndexed((i, t) -> {
            consumer.accept(i, t);
            c.accept(t);
        });
    }

    default IntSeq filter(IntPredicate predicate) {
        return c -> eval(t -> {
            if (predicate.test(t)) {
                c.accept(t);
            }
        });
    }

    default IntSeq filterNot(IntPredicate predicate) {
        return filter(predicate.negate());
    }

    default IntSeq take(int n) {
        return c -> evalIndexed((i, t) -> {
            if (i < n) {
                c.accept(t);
            } else {
                Seq.stop();
            }
        });
    }

    default IntSeq drop(int n) {
        return forFirst(n, nothing);
    }

    default IntSeq forFirst(IntConsumer consumer) {
        return forFirst(1, consumer);
    }

    default IntSeq forFirst(int n, IntConsumer consumer) {
        return c -> evalIndexed((i, t) -> (i >= n ? c : consumer).accept(t));
    }

    default IntSeq takeWhile(IntPredicate predicate) {
        return c -> tillStop(t -> {
            if (predicate.test(t)) {
                c.accept(t);
            } else {
                Seq.stop();
            }
        });
    }

    default IntSeq dropWhile(IntPredicate predicate) {
        return c -> evalOnBool(false, (a, t) -> {
            if (a[0]) {
                c.accept(t);
            } else if (!predicate.test(t)) {
                c.accept(t);
                a[0] = true;
            }
        });
    }

    default IntSeq distinct() {
        return c -> {
            Set<Integer> set = new HashSet<>();
            eval(t -> {
                if (set.add(t)) {
                    c.accept(t);
                }
            });
        };
    }

    default IntSeq flatMap(IntFunction<IntSeq> function) {
        return c -> eval(t -> function.apply(t).eval(c));
    }

    default IntSeq append(int t, int... more) {
        return c -> {
            eval(c);
            c.accept(t);
            for (int x : more) {
                c.accept(x);
            }
        };
    }

    default IntSeq appendWith(IntSeq seq) {
        return c -> {
            eval(c);
            seq.eval(c);
        };
    }

    default boolean any(boolean ifFound, IntPredicate predicate) {
        return evalOnBool(!ifFound, (a, t) -> {
            if (predicate.test(t)) {
                a[0] = ifFound;
                Seq.stop();
            }
        });
    }

    default boolean any(IntPredicate predicate) {
        return any(true, predicate);
    }

    default boolean anyNot(IntPredicate predicate) {
        return any(predicate.negate());
    }

    default boolean all(IntPredicate predicate) {
        return any(false, predicate.negate());
    }

    default boolean none(IntPredicate predicate) {
        return any(false, predicate);
    }

    default int count() {
        return evalIndexed((i, t) -> {});
    }

    default int count(IntPredicate predicate) {
        return sum(t -> predicate.test(t) ? 1 : 0);
    }

    default int countNot(IntPredicate predicate) {
        return count(predicate.negate());
    }

    default int sum() {
        return evalOnInt(0, (a, t) -> a[0] += t);
    }

    default int sum(IntUnaryOperator function) {
        return evalOnInt(0, (a, t) -> a[0] += function.applyAsInt(t));
    }

    default double average() {
        return average(null);
    }

    default double average(IntToDoubleFunction weightFunction) {
        double[] sumWithWeight = new double[2];
        if (weightFunction != null) {
            eval(t -> {
                double w = weightFunction.applyAsDouble(t);
                sumWithWeight[0] += t * w;
                sumWithWeight[1] += w;
            });
        } else {
            eval(t -> {
                sumWithWeight[0] += t;
                sumWithWeight[1] += 1;
            });
        }
        return sumWithWeight[1] > 0 ? sumWithWeight[0] / sumWithWeight[1] : 0;
    }

    interface IndexedIntConsumer {
        void accept(int i, int t);
    }

    interface IntBiConsumer<E> {
        void accept(E e, int t);
    }
}
