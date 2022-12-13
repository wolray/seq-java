package com.github.wolray.seq;

import java.util.Iterator;

/**
 * @author wolray
 */
public interface WithCe {
    interface Function<T, V> {
        V apply(T t) throws Exception;
    }

    interface BiFunction<T, V, R> {
        R apply(T t, V v) throws Exception;
    }

    interface Consumer<T> {
        void accept(T t) throws Exception;
    }

    interface BiConsumer<T, V> {
        void accept(T t, V v) throws Exception;
    }

    interface Supplier<T> {
        T get() throws Exception;
    }

    interface Seq<T> {
        void accept(java.util.function.Consumer<T> consumer) throws Exception;
    }

    interface Iterable<T> {
        Iterator<T> iterator() throws Exception;
    }

    static <T, V> java.util.function.Function<T, V> mapper(Function<T, V> function) {
        return it -> {
            try {
                return function.apply(it);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    static <T, V, R> java.util.function.BiFunction<T, V, R> mapper(BiFunction<T, V, R> function) {
        return (t, v) -> {
            try {
                return function.apply(t, v);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    static <T> java.util.function.Consumer<T> acceptor(Consumer<T> consumer) {
        return it -> {
            try {
                consumer.accept(it);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    static <T, V> java.util.function.BiConsumer<T, V> acceptor(BiConsumer<T, V> consumer) {
        return (t, v) -> {
            try {
                consumer.accept(t, v);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    static <T> java.util.function.Supplier<T> getter(Supplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
