package com.github.wolray.seq;

import java.util.HashSet;
import java.util.Set;
import java.util.function.*;

/**
 * @author wolray
 */
public interface IntSeq extends IntFoldable {
    IntSeq empty = c -> {};
    IntConsumer nothing = t -> {};

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
        return c -> supply(c::accept);
    }

    default <E> Seq<E> mapToObj(IntFunction<E> function) {
        return c -> supply(t -> c.accept(function.apply(t)));
    }

    default IntSeq map(IntUnaryOperator function) {
        return c -> supply(t -> c.accept(function.applyAsInt(t)));
    }

    default IntSeq circle() {
        return c -> {
            while (true) {
                supply(c);
            }
        };
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

    default IntSeq onEach(IntConsumer consumer) {
        return c -> supply(consumer.andThen(c));
    }

    default IntSeq onEachIndexed(IndexIntConsumer consumer) {
        return onEach(foldIndexed(consumer));
    }

    default IntSeq filter(IntPredicate predicate) {
        return c -> supply(t -> {
            if (predicate.test(t)) {
                c.accept(t);
            }
        });
    }

    default IntSeq filterNot(IntPredicate predicate) {
        return filter(predicate.negate());
    }

    default IntSeq take(int n) {
        return c -> supply(foldIndexed((i, t) -> {
            if (i < n) {
                c.accept(t);
            } else {
                stop();
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
        return c -> supply(foldIndexed((i, t) -> (i >= n ? c : consumer).accept(t)));
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

    default IntSeq dropWhile(IntPredicate predicate) {
        return c -> supply(foldBool(false, (b, t) -> {
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
            supply(t -> {
                if (set.add(t)) {
                    c.accept(t);
                }
            });
        };
    }

    default IntSeq flatMap(IntFunction<IntSeq> function) {
        return c -> supply(t -> function.apply(t).supply(c));
    }

    default IntSeq runningFold(int init, IntBinaryOperator function) {
        return c -> supply(fold(init, (acc, t) -> {
            acc = function.applyAsInt(acc, t);
            c.accept(acc);
            return acc;
        }));
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
}
