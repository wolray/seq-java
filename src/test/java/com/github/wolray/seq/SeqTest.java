package com.github.wolray.seq;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author wolray
 */
public class SeqTest {
    @Test
    public void testResult() {
        Seq<Integer> seq = Seq.of(0, 2, 4, 1, 6, 3, 5, 7, 10, 11, 12);
        Seq<Integer> filtered = seq.take(5);
        filtered.assertTo(",", "0,2,4,1,6");
        filtered.assertTo(",", "0,2,4,1,6");
        Predicate<Integer> predicate = i -> (i & 1) == 0;
        seq.dropWhile(predicate).assertTo(",", "1,6,3,5,7,10,11,12");
        seq.takeWhile(predicate).assertTo(",", "0,2,4");
        seq.take(5).assertTo(",", "0,2,4,1,6");
        seq.take(5).drop(2).assertTo(",", "4,1,6");
        Seq.tillNull(() -> 1).take(4).assertTo(",", "1,1,1,1");
        Seq.tillNull(() -> 1).take(5).assertTo(",", "1,1,1,1,1");
        Seq.repeat(5, 1).assertTo(",", "1,1,1,1,1");
        Seq.range(0, 10, 2).assertTo(",", "0,2,4,6,8");
    }

    @Test
    public void testRunningFold() {
        Seq<Integer> seq = Seq.of(0, 2, 4, 1, 6, 3, 5, 7, 10, 11, 12);
        seq.runningFold(0, Integer::sum).assertTo(",", "0,2,6,7,13,16,21,28,38,49,61");
    }

    @Test
    public void testChunked() {
        List<Integer> list = Arrays.asList(0, 2, 4, 1, 6, 3, 5, 7, 10, 11, 12);
        Seq.of(list).chunked(2).map(s -> s.join(",")).assertTo("|", "0,2|4,1|6,3|5,7|10,11|12");
        Seq.of(list).chunked(3).map(s -> s.join(",")).assertTo("|", "0,2,4|1,6,3|5,7,10|11,12");
        Seq.of(list).chunked(4).map(s -> s.join(",")).assertTo("|", "0,2,4,1|6,3,5,7|10,11,12");
        Seq.of(list).chunked(5).map(s -> s.join(",")).assertTo("|", "0,2,4,1,6|3,5,7,10,11|12");
    }

    @Test
    public void testYield() {
        Seq<Integer> fib = Seq.gen(1, 1, Integer::sum).take(10);
        fib.assertTo(",", "1,1,2,3,5,8,13,21,34,55");
        fib.assertTo(",", "1,1,2,3,5,8,13,21,34,55");
        Seq<Integer> quad = Seq.gen(1, i -> i * 2).take(10);
        quad.assertTo(",", "1,2,4,8,16,32,64,128,256,512");
        quad.assertTo(",", "1,2,4,8,16,32,64,128,256,512");

        List<Integer> list1 = Arrays.asList(10, 20, 30);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        Seq<Integer> seq = c -> {
            for (Integer i1 : list1) {
                for (Integer i2 : list2) {
                    c.accept(i1 + i2);
                }
            }
        };
        seq.assertTo(",", "11,12,13,21,22,23,31,32,33");
        seq.assertTo(",", "11,12,13,21,22,23,31,32,33");
    }

    @Test
    public void testSeqList() {
        SeqList<Integer> list = SeqList.array();
        list.add(1);
        list.add(2);
        list.add(3);
        list.set(0, 6);
        list.add(2, 10);
        list.assertTo(",", "6,2,10,3");
    }

    @Test
    public void testQueue() {
        SinglyList<Integer> list = new SinglyList<>();
        list.offer(2);
        list.offer(3);
        list.offer(4);
        list.offer(5);
        list.assertTo(",", "2,3,4,5");
        assert list.size() == 4;
        Integer head = list.remove();
        assert head == 2;
        assert list.size() == 3;
        list.assertTo(",", "3,4,5");
    }
}
