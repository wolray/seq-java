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

/**
 * @author wolray
 */
public class TreeSearchTest {
    static int n = 6, m = 5, maxSize = n + m - 1;

    private Seq<int[]> next(int[] path) {
        int n = 7, m = 4;
        int len = path.length;
        int x = path[len - 2];
        int y = path[len - 1];
        return c -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (x < n) {
                int[] next = Arrays.copyOf(path, len + 2);
                next[len] = x + 1;
                next[len + 1] = y;
                c.accept(next);
            }
            if (y < m) {
                int[] next = Arrays.copyOf(path, len + 2);
                next[len] = x;
                next[len + 1] = y + 1;
                c.accept(next);
            }
        };
    }

    private int eval(int[] a) {
        return IntSeq.of(a).filter((i, t) -> (i & 1) > 0).sum(t -> t);
    }

    @Test
    @Benchmark
    public void testSync() {
        Seq.ofTree(new int[]{0, 0}, this::next)
            .filter(a -> a.length == maxSize)
            .min(this::eval);
    }

    @Test
    @Benchmark
    public void testAsync() {
        Seq.ofTreeParallel(new int[]{0, 0}, this::next)
            .filter(a -> a.length == maxSize)
            .minAsync(Integer.MAX_VALUE, this::eval);
    }

    @Test
    public void test() throws RunnerException {
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
