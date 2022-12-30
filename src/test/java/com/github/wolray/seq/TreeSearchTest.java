package com.github.wolray.seq;

import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author wolray
 */
public class TreeSearchTest {
    static int n = 6, m = 5, maxSize = 2 * (n + m - 1);

    private Seq<int[]> next(int[] path) {
        int len = path.length;
        int x = path[len - 2];
        int y = path[len - 1];
        return c -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (x < n - 1) {
                int[] next = Arrays.copyOf(path, len + 2);
                next[len] = x + 1;
                next[len + 1] = y;
                c.accept(next);
            }
            if (y < m - 1) {
                int[] next = Arrays.copyOf(path, len + 2);
                next[len] = x;
                next[len + 1] = y + 1;
                c.accept(next);
            }
        };
    }

    private int eval(int[] a) {
        return IntSeq.of(a).filterIndexed((i, t) -> (i & 1) > 0).sum(t -> t);
    }

    @Test
    @Benchmark
    public void testSync() {
        Pair<int[], Integer> pair = Seq.ofTree(new int[]{0, 0}, this::next)
            .filter(a -> a.length == maxSize)
            .min(this::eval);
        System.out.println(Arrays.toString(pair.first));
    }

    @Test
    @Benchmark
    public void testAsync() {
        AtomicReference<Pair<int[], Integer>> pair = Seq.ofTreeParallel(new int[]{0, 0}, this::next)
            .filter(a -> a.length == maxSize)
            .minAsync(Integer.MAX_VALUE, this::eval);
        System.out.println(Arrays.toString(pair.get().first));
    }

//    @Test
    public void benchmark() throws RunnerException {
        Options options = new OptionsBuilder()
            .include(TreeSearchTest.class.getSimpleName())
            .warmupIterations(1)
            .warmupTime(TimeValue.seconds(5))
            .measurementIterations(3)
            .mode(Mode.AverageTime)
            .forks(1)
            .build();
        new Runner(options).run();
    }
}
