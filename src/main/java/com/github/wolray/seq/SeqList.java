package com.github.wolray.seq;

import java.util.*;

/**
 * @author wolray
 */
public class SeqList<T> extends SeqCollection<T, List<T>> implements List<T> {
    SeqList(List<T> backer) {
        super(backer);
    }

    public static <T> SeqList<T> of(List<T> list) {
        return list instanceof SeqList ? (SeqList<T>)list : new SeqList<>(list);
    }

    @SafeVarargs
    public static <T> SeqList<T> of(T... ts) {
        return new SeqList<>(Arrays.asList(ts));
    }

    public static <T> SeqList<T> array() {
        return new SeqList<>(new ArrayList<>());
    }

    public static <T> SeqList<T> linked() {
        return new SeqList<>(new LinkedList<>());
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return backer.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return backer.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return backer.retainAll(c);
    }

    @Override
    public void clear() {
        backer.clear();
    }

    @Override
    public T get(int index) {
        return backer.get(index);
    }

    @Override
    public T set(int index, T element) {
        return backer.set(index, element);
    }

    @Override
    public void add(int index, T element) {
        backer.add(index, element);
    }

    @Override
    public T remove(int index) {
        return backer.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return backer.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return backer.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return backer.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return backer.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return backer.subList(fromIndex, toIndex);
    }
}
