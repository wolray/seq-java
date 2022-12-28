package com.github.wolray.seq;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * @author wolray
 */
public interface IntFolder<E> extends IntConsumer, Supplier<E> {
    default <R> IntFolder<R> map(Function<E, R> function) {
        return new IntFolder<R>() {
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

    default IntFolder<String> format(String format) {
        return map(it -> String.format(format, it));
    }

    default IntFolder<E> then(Consumer<E> consumer) {
        return map(e -> {
            consumer.accept(e);
            return e;
        });
    }
}
