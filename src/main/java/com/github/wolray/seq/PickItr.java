package com.github.wolray.seq;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author wolray
 */
public abstract class PickItr<T> implements Iterator<T> {
    private T next;
    private State state = State.Unset;

    public static <T> PickItr<T> flat(Iterator<? extends Iterable<T>> iterator) {
        return new PickItr<T>() {
            Iterator<T> cur = Collections.emptyIterator();

            @Override
            public T pick() {
                while (!cur.hasNext()) {
                    if (!iterator.hasNext()) {
                        Seq.stop();
                    }
                    cur = iterator.next().iterator();
                }
                return cur.next();
            }
        };
    }

    public abstract T pick();

    @Override
    public boolean hasNext() {
        if (state == State.Unset) {
            try {
                next = pick();
                state = State.Cached;
            } catch (Seq.StopException e) {
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
