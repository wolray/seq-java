package com.github.wolray.seq;

import java.util.HashSet;
import java.util.Set;
import java.util.function.*;

/**
 * @author wolray
 */
public interface IntSeq {
    IntSeq empty = c -> {};
    IntConsumer nothing = t -> {};

    void eval(IntConsumer consumer);

    default void tillStop(IntConsumer consumer) {
        try {
            eval(consumer);
        } catch (Seq.StopException ignore) {}
    }

    default <E> IntFolder<E> find(E ifNotFound, IntPredicate predicate, IntFunction<E> function) {
        return new IntFolder<E>(this) {
            E e = ifNotFound;

            @Override
            public E get() {
                return e;
            }

            @Override
            public void accept(int t) {
                if (predicate.test(t)) {
                    e = function.apply(t);
                    Seq.stop();
                }
            }
        };
    }

    default <E> IntFolder<E> fold(E init, IntBiFunction<E> function) {
        return new IntFolder<E>(this) {
            E e = init;

            @Override
            public E get() {
                return e;
            }

            @Override
            public void accept(int t) {
                e = function.apply(e, t);
            }
        };
    }

    default IntFolder<Integer> foldIndexed(IndexedIntConsumer consumer) {
        return foldIndexed(0, consumer);
    }

    default IntFolder<Integer> foldIndexed(int start, IndexedIntConsumer consumer) {
        return foldInt(start, (i, t) -> {
            consumer.accept(i, t);
            return i + 1;
        });
    }

    default IntFolder<Integer> foldInt(int init, IntBinaryOperator function) {
        return new IntFolder<Integer>(this) {
            int i = init;

            @Override
            public Integer get() {
                return i;
            }

            @Override
            public void accept(int t) {
                i = function.applyAsInt(i, t);
            }
        };
    }

    default IntFolder<Boolean> foldBool(boolean init, BoolBiFunction function) {
        return new IntFolder<Boolean>(this) {
            boolean b = init;

            @Override
            public Boolean get() {
                return b;
            }

            @Override
            public void accept(int t) {
                b = function.apply(b, t);
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

    static IntSeq of(CharSequence cs) {
        return c -> {
            for (int i = 0; i < cs.length(); i++) {
                c.accept(cs.charAt(i));
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

    default IntSeq map(IntUnaryOperator function) {
        return c -> eval(t -> c.accept(function.applyAsInt(t)));
    }

    default <E> Seq<E> mapToObj(IntFunction<E> function) {
        return c -> eval(t -> c.accept(function.apply(t)));
    }

    default IntSeq onEach(IntConsumer consumer) {
        return c -> eval(consumer.andThen(c));
    }

    default IntSeq onEachIndexed(IndexedIntConsumer consumer) {
        return c -> eval(foldIndexed((i, t) -> {
            consumer.accept(i, t);
            c.accept(t);
        }));
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
        return c -> eval(foldIndexed((i, t) -> {
            if (i < n) {
                c.accept(t);
            } else {
                Seq.stop();
            }
        }));
    }

    default IntSeq drop(int n) {
        return forFirst(n, nothing);
    }

    default IntSeq forFirst(IntConsumer consumer) {
        return forFirst(1, consumer);
    }

    default IntSeq forFirst(int n, IntConsumer consumer) {
        return c -> eval(foldIndexed((i, t) -> (i >= n ? c : consumer).accept(t)));
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

    default IntSeq runningFold(int init, IntBinaryOperator function) {
        return c -> eval(fold(init, (acc, t) -> {
            acc = function.applyAsInt(acc, t);
            c.accept(acc);
            return acc;
        }));
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

    default IntFolder<Boolean> any(boolean ifFound, IntPredicate predicate) {
        return find(!ifFound, predicate, t -> ifFound);
    }

    default IntFolder<Boolean> any(IntPredicate predicate) {
        return any(true, predicate);
    }

    default IntFolder<Boolean> anyNot(IntPredicate predicate) {
        return any(predicate.negate());
    }

    default IntFolder<Boolean> all(IntPredicate predicate) {
        return any(false, predicate.negate());
    }

    default IntFolder<Boolean> none(IntPredicate predicate) {
        return any(false, predicate);
    }

    default IntFolder<Integer> first() {
        return find(null, t -> true, t -> t);
    }

    default IntFolder<Integer> first(IntPredicate predicate) {
        return find(null, predicate, t -> t);
    }

    default IntFolder<Integer> firstNot(IntPredicate predicate) {
        return first(predicate.negate());
    }

    default IntFolder<Integer> last() {
        return fold(null, (res, t) -> t);
    }

    default IntFolder<Integer> last(IntPredicate predicate) {
        return fold(null, (res, t) -> predicate.test(t) ? t : res);
    }

    default IntFolder<Integer> lastNot(IntPredicate predicate) {
        return last(predicate.negate());
    }

    default IntFolder<Integer> count() {
        return foldInt(0, (i, t) -> i + 1);
    }

    default int count(IntPredicate predicate) {
        return sum(t -> predicate.test(t) ? 1 : 0);
    }

    default int countNot(IntPredicate predicate) {
        return count(predicate.negate());
    }

    default int sum() {
        return foldInt(0, Integer::sum).eval();
    }

    default int sum(IntUnaryOperator function) {
        return foldInt(0, (i, t) -> i + function.applyAsInt(t)).eval();
    }

    default IntFolder<Double> average() {
        return average(null);
    }

    default IntFolder<Double> average(IntToDoubleFunction weightFunction) {
        return new IntFolder<Double>(this) {
            double sum;
            double weight;

            @Override
            public Double get() {
                return weight != 0 ? sum / weight : 0;
            }

            @Override
            public void accept(int t) {
                if (weightFunction != null) {
                    double w = weightFunction.applyAsDouble(t);
                    sum += t * w;
                    weight += w;
                } else {
                    sum += t;
                    weight += 1;
                }
            }
        };
    }

    default IntFolder<Integer> max() {
        return fold(null, (f, t) -> f == null || f < t ? t : f);
    }

    default IntFolder<Integer> min() {
        return fold(null, (f, t) -> f == null || f > t ? t : f);
    }

    default int[] toArray() {
        BatchList<Integer> list = boxed().toBatchList().eval();
        int[] res = new int[list.size()];
        list.foldIndexed((i, t) -> res[i] = t).eval();
        return res;
    }

    abstract class IntFolder<E> implements IntConsumer, Supplier<E> {
        private final IntSeq seq;

        public IntFolder(IntSeq seq) {
            this.seq = seq;
        }

        public E eval() {
            seq.tillStop(this);
            return get();
        }

        public <R> IntFolder<R> map(Function<E, R> function) {
            return new IntFolder<R>(seq) {
                @Override
                public R get() {
                    return function.apply(IntFolder.this.get());
                }

                @Override
                public void accept(int t) {
                    IntFolder.this.accept(t);
                }
            };
        }
    }

    interface ToIntFolder<T> extends Function<IntSeq, IntFolder<T>> {
        default IntFolder<T> gen() {
            return apply(empty);
        }
    }

    interface IndexedIntConsumer {
        void accept(int i, int t);
    }

    interface BoolBiFunction {
        boolean apply(boolean b, int t);
    }

    interface IntBiConsumer<E> {
        void accept(E e, int t);
    }

    interface IntBiFunction<E> {
        E apply(E e, int t);
    }
}
