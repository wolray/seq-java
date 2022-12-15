package com.github.wolray.seq;

/**
 * @author wolray
 */
public class LongPair<T> {
    public long first;
    public T second;

    public LongPair(long first, T second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return String.format("(%d,%s)", first, second);
    }
}
