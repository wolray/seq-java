package com.github.wolray.seq;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author wolray
 */
public class BackedSeq<T, C extends Collection<T>> implements Seq<T> {
    public final C backer;

    public BackedSeq(C backer) {
        this.backer = backer;
    }

    @Override
    public void supply(Consumer<T> consumer) {
        backer.forEach(consumer);
    }

//    @Override
    public int sizeOrDefault() {
        return backer.size();
    }

    public boolean isNotEmpty() {
        return !backer.isEmpty();
    }
}
