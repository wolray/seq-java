package com.github.wolray.seq;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author wolray
 */
public abstract class SafeSeq<T> implements Seq<T> {
    Class<? extends Exception> errorType;

    public SafeSeq<T> ignore(Class<? extends Exception> type) {
        errorType = type;
        return this;
    }

    public static <T> SafeSeq<T> of(WithCe.Seq<T> seq) {
        return new SafeSeq<T>() {
            @Override
            public void supply(Consumer<T> consumer) {
                try {
                    seq.accept(consumer);
                } catch (Exception e) {
                    if (errorType != null && errorType.isAssignableFrom(e.getClass())) {
                        return;
                    }
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    public <E> Transformer<T, E> map(Function<T, E> function) {
        return map(0, function);
    }

    public <E> Transformer<T, E> map(int skip, Function<T, E> function) {
        return mapping(c -> foldIndexed((i, t) -> {
            if (i > skip || i == skip && !isProcessed(function, t)) {
                c.accept(function.apply(t));
            }
        }));
    }

    private static <T, E> boolean isProcessed(Function<T, E> function, T t) {
        if (function instanceof ContextFunction) {
            return ((ContextFunction<T, E>)function).preprocess(t);
        }
        return false;
    }

    public interface ContextFunction<T, E> extends Function<T, E> {
        boolean preprocess(T t);
    }
}
