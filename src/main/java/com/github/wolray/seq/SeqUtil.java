package com.github.wolray.seq;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author wolray
 */
public class SeqUtil {
    public static <T> Seq<T> seq(Iterable<T> iterable) {
        return Seq.of(iterable);
    }

    @SafeVarargs
    public static <T> Seq<T> seq(T... ts) {
        return Seq.of(Arrays.asList(ts));
    }

    public static <T> SeqList<T> seq(List<T> list) {
        return SeqList.of(list);
    }

    public static <T> SeqSet<T> seq(Set<T> set) {
        return SeqSet.of(set);
    }

    public static <K, V> SeqMap<K, V> seq(Map<K, V> map) {
        return SeqMap.of(map);
    }

    public static <N> void scanTree(Consumer<N> c, N node, Function<N, Seq<N>> sub) {
        if (node != null) {
            c.accept(node);
            sub.apply(node).supply(n -> scanTree(c, n, sub));
        }
    }

    public static <N> void scanTreeParallel(Consumer<N> c, ForkJoinPool pool, N node, Function<N, Seq<N>> sub) {
        pool.submit(() -> c.accept(node)).join();
        sub.apply(node)
            .mapIfExists(n -> pool.submit(() -> scanTreeParallel(c, pool, n, sub)))
            .cache()
            .supply(ForkJoinTask::join);
    }

    public static <T> void permute(Consumer<List<T>> c, ArrayList<T> list, int i, boolean inplace) {
        int n = list.size();
        if (i == n) {
            c.accept(inplace ? list : new ArrayList<>(list));
            return;
        }
        for (int j = i; j < n; j++) {
            swap(list, i, j);
            permute(c, list, i + 1, inplace);
            swap(list, i, j);
        }
    }

    public static <T> void swap(List<T> list, int i, int j) {
        T t = list.get(i);
        list.set(i, list.get(j));
        list.set(j, t);
    }
}
