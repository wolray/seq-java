package com.github.wolray.seq;

import java.util.function.Consumer;

/**
 * @author wolray
 */
public abstract class SafeSeq<T> implements Seq<T> {
    Class<? extends Exception> errorType;

    public Seq<T> ignore(Class<? extends Exception> type) {
        errorType = type;
        return this;
    }

    public static <T> SafeSeq<T> of(WithCe.Consumer<Consumer<T>> seq) {
        return new SafeSeq<T>() {
            @Override
            public void eval(Consumer<T> consumer) {
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
}
