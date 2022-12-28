package com.github.wolray.seq;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * @author wolray
 */
public abstract class IntFolder<E> implements IntConsumer, Supplier<E> {
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
            public void accept(int t) {
                IntFolder.this.accept(t);
            }

            @Override
            public R get() {
                return function.apply(IntFolder.this.get());
            }
        };
    }

    public IntFolder<String> format(String format) {
        return map(it -> String.format(format, it));
    }

    public IntFolder<E> then(Consumer<E> consumer) {
        return map(e -> {
            consumer.accept(e);
            return e;
        });
    }
}
