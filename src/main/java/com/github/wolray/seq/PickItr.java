package com.github.wolray.seq;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * @author wolray
 */
public abstract class PickItr<T> implements Seq<T>, Iterator<T> {
    private T next;
    private State state = State.Unset;

    public static <T> PickItr<T> flat(Iterator<? extends Iterable<T>> iterator) {
        return new PickItr<T>() {
            Iterator<T> cur = Collections.emptyIterator();

            @Override
            public T pick() {
                while (!cur.hasNext()) {
                    if (!iterator.hasNext()) {
                        stop();
                    }
                    cur = iterator.next().iterator();
                }
                return cur.next();
            }
        };
    }

    public abstract T pick();

    @Override
    public void supply(Consumer<T> consumer) {
        forEachRemaining(consumer);
    }

    @Override
    public boolean hasNext() {
        if (state == State.Unset) {
            try {
                next = pick();
                state = State.Cached;
            } catch (Foldable0.StopException e) {
                state = State.Done;
            }
        }
        return state == State.Cached;
    }

    @Override
    public T next() {
        if (hasNext()) {
            T res = next;
            next = null;
            state = State.Unset;
            return res;
        }
        throw new NoSuchElementException();
    }

    enum State {
        Unset,
        Cached,
        Done
    }
}
