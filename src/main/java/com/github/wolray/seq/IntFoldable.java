package com.github.wolray.seq;

import java.util.function.*;

/**
 * @author wolray
 */
public interface IntFoldable extends Foldable0<IntConsumer> {
    default <E> IntFolder<E> find(E ifNotFound, IntPredicate predicate, IntFunction<E> function) {
        return new SimpleIntFolder<E>(this, ifNotFound) {
            @Override
            public void accept(int t) {
                if (predicate.test(t)) {
                    acc = function.apply(t);
                    stop();
                }
            }
        };
    }

    default <E> IntFolder<E> fold(E init, ObjIntToObj<E> function) {
        return new SimpleIntFolder<E>(this, init) {
            @Override
            public void accept(int t) {
                acc = function.apply(acc, t);
            }
        };
    }

    default IntFolder<Integer> foldIndexed(IndexIntConsumer consumer) {
        return foldIndexed(0, consumer);
    }

    default IntFolder<Integer> foldIndexed(int start, IndexIntConsumer consumer) {
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

    default IntFolder<Boolean> foldBool(boolean init, BoolIntToBool function) {
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

    default IntFolder<Integer> count(IntPredicate predicate) {
        return sum(t -> predicate.test(t) ? 1 : 0);
    }

    default IntFolder<Integer> countNot(IntPredicate predicate) {
        return count(predicate.negate());
    }

    default IntFolder<Integer> sum() {
        return foldInt(0, Integer::sum);
    }

    default IntFolder<Integer> sum(IntUnaryOperator function) {
        return foldInt(0, (i, t) -> i + function.applyAsInt(t));
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

    default IntFolder<int[]> toArray() {
        IntSeq seq = this::eval;
        BatchList<Integer> list = seq.boxed().toBatchList().eval();
        return new IntFolder<int[]>(this) {
            int i = 0;
            int[] a = new int[list.size()];

            @Override
            public void accept(int t) {
                a[i++] = t;
            }

            @Override
            public int[] get() {
                return a;
            }
        };
    }

    abstract class SimpleIntFolder<E> extends IntFolder<E> {
        protected E acc;

        public SimpleIntFolder(IntFoldable foldable, E acc) {
            super(foldable);
            this.acc = acc;
        }

        @Override
        public E get() {
            return acc;
        }
    }

    abstract class IntFolder<E> implements IntConsumer, Supplier<E> {
        private final IntFoldable foldable;

        public IntFolder(IntFoldable foldable) {
            this.foldable = foldable;
        }

        public E eval() {
            foldable.tillStop(this);
            return get();
        }

        public <R> IntFolder<R> map(Function<E, R> function) {
            return new IntFolder<R>(foldable) {
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

    interface ToFolder<T> extends Function<IntFoldable, IntFolder<T>> {
        default IntFolder<T> gen() {
            return apply(IntSeq.empty);
        }
    }

    interface IndexIntConsumer {
        void accept(int i, int t);
    }

    interface BoolIntToBool {
        boolean apply(boolean b, int t);
    }

    interface ObjIntToObj<E> {
        E apply(E e, int t);
    }
}
