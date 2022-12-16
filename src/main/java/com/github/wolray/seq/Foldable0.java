package com.github.wolray.seq;

/**
 * @author wolray
 */
public interface Foldable0<C> {
    void eval(C consumer);

    default void tillStop(C consumer) {
        try {
            eval(consumer);
        } catch (StopException ignore) {}
    }

    default <E> E stop() throws StopException {
        throw StopException.INSTANCE;
    }

    class StopException extends RuntimeException {
        public static final StopException INSTANCE = new StopException() {
            @Override
            public synchronized Throwable fillInStackTrace() {
                return this;
            }
        };
    }
}