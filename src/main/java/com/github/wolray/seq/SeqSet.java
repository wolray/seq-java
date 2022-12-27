package com.github.wolray.seq;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author wolray
 */
public class SeqSet<T> extends SeqCollection<T, Set<T>> implements Set<T> {
    SeqSet(Set<T> backer) {
        super(backer);
    }

    public static <T> SeqSet<T> of(Set<T> set) {
        return set instanceof SeqSet ? (SeqSet<T>)set : new SeqSet<>(set);
    }

    public static <T> SeqSet<T> hash() {
        return new SeqSet<>(new HashSet<>());
    }

    public static <T> SeqSet<T> tree(Comparator<T> comparator) {
        return new SeqSet<>(new TreeSet<>(comparator));
    }
}
