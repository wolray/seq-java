package com.github.wolray.seq;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author wolray
 */
public class SeqList<T> extends BackedSeq<T, List<T>> implements List<T> {
    SeqList(List<T> backer) {
        super(backer);
    }

    @Override
    public void eval(Consumer<T> consumer) {
        backer.forEach(consumer);
    }

    @Override
    public String toString() {
        return backer.toString();
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
