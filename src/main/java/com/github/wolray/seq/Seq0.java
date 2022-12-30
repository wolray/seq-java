package com.github.wolray.seq;

/**
 * @author wolray
 */
public interface Seq0<C> {
    void supply(C consumer);

    default <T> T stop() {
        throw StopException.INSTANCE;
    }

    default void tillStop(C consumer) {
        try {
            supply(consumer);
        } catch (StopException ignore) {}
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
