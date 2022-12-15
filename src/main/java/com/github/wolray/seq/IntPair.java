package com.github.wolray.seq;

/**
 * @author wolray
 */
public class IntPair<T> {
    public int first;
    public T second;

    public IntPair(int first, T second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return String.format("(%d,%s)", first, second);
    }
}
