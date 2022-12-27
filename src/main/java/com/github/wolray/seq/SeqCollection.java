package com.github.wolray.seq;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * @author wolray
 */
public class SeqCollection<T, C extends Collection<T>> extends BackedSeq<T, C> implements Collection<T> {
    public SeqCollection(C backer) {
        super(backer);
    }

    @Override
    public void supply(Consumer<T> consumer) {
        backer.forEach(consumer);
    }

    @Override
    public String toString() {
        return backer.toString();
    }

    @Override
    public int size() {
        return backer.size();
    }

    @Override
    public boolean isEmpty() {
        return backer.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return backer.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return backer.iterator();
    }

    @Override
    public Object[] toArray() {
        return backer.toArray();
    }

    @Override
    public <E> E[] toArray(E[] a) {
        return backer.toArray(a);
    }

    @Override
    public boolean add(T t) {
        return backer.add(t);
    }

    @Override
    public boolean remove(Object o) {
        return backer.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return backer.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return backer.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return backer.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return backer.removeAll(c);
    }

    @Override
    public void clear() {
        backer.clear();
    }
}
