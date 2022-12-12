package com.github.wolray.seq;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * @author wolray
 */
public interface AdderList<E> extends Seq<E>, List<E> {
    default String toStr() {
        if (isEmpty()) {
            return "[]";
        }
        return '[' + join(", ", e -> e == this ? "(this)" : String.valueOf(e)) + ']';
    }

    @Override
    default int sizeOrDefault() {
        return size();
    }

    default boolean isNotEmpty() {
        return !isEmpty();
    }

    @Override
    default boolean isEmpty() {
        return size() == 0;
    }

    @Override
    default boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    default Object[] toArray() {
        Object[] a = new Object[size()];
        int i = 0;
        for (E e : this) {
            a[i++] = e;
        }
        return a;
    }

    @Override
    default <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    default boolean addAll(Collection<? extends E> c) {
        for (E e : c) {
            add(e);
        }
        return true;
    }

    @Override
    default boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    default E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    default E remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    default int indexOf(Object o) {
        int i = 0;
        for (E e : this) {
            if (Objects.equals(e, o)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    @Override
    default int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    default ListIterator<E> listIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    default ListIterator<E> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    default List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }
}