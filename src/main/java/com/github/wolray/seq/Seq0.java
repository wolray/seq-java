package com.github.wolray.seq;

/**
 * @author wolray
 */
public interface Seq0<C> {
    void supply(C consumer);

    default void tillStop(C consumer) {
        try {
            supply(consumer);
        } catch (StopException ignore) {}
    }

    static <T> T stop() {
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
