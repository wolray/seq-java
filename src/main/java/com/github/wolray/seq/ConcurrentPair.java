package com.github.wolray.seq;

/**
 * @author wolray
 */
public class ConcurrentPair<A, B> {
    private A first;
    private B second;

    public ConcurrentPair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public synchronized A getFirst() {
        return first;
    }

    public synchronized void setFirst(A first) {
        this.first = first;
    }

    public synchronized B getSecond() {
        return second;
    }

    public synchronized void setSecond(B second) {
        this.second = second;
    }
}
