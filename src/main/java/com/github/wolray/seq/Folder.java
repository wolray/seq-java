package com.github.wolray.seq;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author wolray
 */
public abstract class Folder<E, T> implements Consumer<T>, Supplier<E> {
    private final Foldable<T> foldable;

    public Folder(Foldable<T> foldable) {
        this.foldable = foldable;
    }

    public E eval() {
        foldable.tillStop(this);
        return get();
    }

    public <R> Folder<R, T> map(Function<E, R> function) {
        return new Folder<R, T>(foldable) {
            @Override
            public void accept(T t) {
                Folder.this.accept(t);
            }

            @Override
            public R get() {
                return function.apply(Folder.this.get());
            }
        };
    }

    public Folder<String, T> format(String format) {
        return map(it -> String.format(format, it));
    }

    public Folder<E, T> then(Consumer<E> consumer) {
        return map(e -> {
            consumer.accept(e);
            return e;
        });
    }
}