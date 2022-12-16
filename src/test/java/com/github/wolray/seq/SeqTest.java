package com.github.wolray.seq;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author wolray
 */
public class SeqTest {
    @Test
    public void testResult() {
        Seq<Integer> seq = Seq.of(0, 2, 4, 1, 6, 3, 5, 7, 10, 11, 12);
        Seq<Integer> filtered = seq.take(5);
        filtered.assertTo("0,2,4,1,6");
        filtered.assertTo("0,2,4,1,6");
        Predicate<Integer> predicate = i -> (i & 1) == 0;
        seq.dropWhile(predicate).assertTo("1,6,3,5,7,10,11,12");
        seq.takeWhile(predicate).assertTo("0,2,4");
        seq.take(5).assertTo("0,2,4,1,6");
        seq.take(5).drop(2).assertTo("4,1,6");

        Seq<Integer> token1 = Seq.tillNull(() -> 1).take(5);
        token1.assertTo("1,1,1,1,1");
        token1.assertTo("1,1,1,1,1");
        IntSeq token2 = IntSeq.gen(() -> 1).take(5);
        token2.boxed().assertTo("1,1,1,1,1");
        token2.boxed().assertTo("1,1,1,1,1");

        Seq.repeat(5, 1).assertTo("1,1,1,1,1");
        IntSeq.repeat(5, 1).boxed().assertTo("1,1,1,1,1");
        IntSeq.range(0, 10, 2).boxed().assertTo("0,2,4,6,8");
    }

    @Test
    public void testRunningFold() {
        Seq<Integer> seq = Seq.of(0, 2, 4, 1, 6, 3, 5, 7, 10, 11, 12);
        seq.runningFold(0, Integer::sum).assertTo("0,2,6,7,13,16,21,28,38,49,61");
    }

    @Test
    public void testChunked() {
        List<Integer> list = Arrays.asList(0, 2, 4, 1, 6, 3, 5, 7, 10, 11, 12);
        Seq.of(list).chunked(2).map(s -> s.join(",").eval()).assertTo("|", "0,2|4,1|6,3|5,7|10,11|12");
        Seq.of(list).chunked(3).map(s -> s.join(",").eval()).assertTo("|", "0,2,4|1,6,3|5,7,10|11,12");
        Seq.of(list).chunked(4).map(s -> s.join(",").eval()).assertTo("|", "0,2,4,1|6,3,5,7|10,11,12");
        Seq.of(list).chunked(5).map(s -> s.join(",").eval()).assertTo("|", "0,2,4,1,6|3,5,7,10,11|12");
    }

    @Test
    public void testYield() {
        Seq<Integer> fib1 = Seq.gen(1, 1, Integer::sum).take(10);
        fib1.assertTo("1,1,2,3,5,8,13,21,34,55");
        fib1.assertTo("1,1,2,3,5,8,13,21,34,55");
        IntSeq fib2 = IntSeq.gen(1, 1, Integer::sum).take(10);
        fib2.boxed().assertTo("1,1,2,3,5,8,13,21,34,55");
        fib2.boxed().assertTo("1,1,2,3,5,8,13,21,34,55");

        Seq<Integer> quad1 = Seq.gen(1, i -> i * 2).take(10);
        quad1.assertTo("1,2,4,8,16,32,64,128,256,512");
        quad1.assertTo("1,2,4,8,16,32,64,128,256,512");
        IntSeq quad2 = IntSeq.gen(1, i -> i * 2).take(10);
        quad2.boxed().assertTo("1,2,4,8,16,32,64,128,256,512");
        quad2.boxed().assertTo("1,2,4,8,16,32,64,128,256,512");

        List<Integer> list1 = Arrays.asList(10, 20, 30);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        Seq<Integer> cart1 = c -> {
            for (Integer i1 : list1) {
                for (Integer i2 : list2) {
                    c.accept(i1 + i2);
                }
            }
        };
        cart1.assertTo("11,12,13,21,22,23,31,32,33");
        cart1.assertTo("11,12,13,21,22,23,31,32,33");
        IntSeq cart2 = c -> {
            for (Integer i1 : list1) {
                for (Integer i2 : list2) {
                    c.accept(i1 + i2);
                }
            }
        };
        cart2.boxed().assertTo("11,12,13,21,22,23,31,32,33");
        cart2.boxed().assertTo("11,12,13,21,22,23,31,32,33");
    }

    @Test
    public void testSeqList() {
        SeqList<Integer> list = SeqList.array();
        list.add(1);
        list.add(2);
        list.add(3);
        list.set(0, 6);
        list.add(2, 10);
        list.assertTo("6,2,10,3");
    }

    @Test
    public void testQueue() {
        SinglyList<Integer> list = new SinglyList<>();
        list.offer(2);
        list.offer(3);
        list.offer(4);
        list.offer(5);
        list.assertTo("2,3,4,5");
        assert list.size() == 4;
        Integer head = list.remove();
        assert head == 2;
        assert list.size() == 3;
        list.assertTo("3,4,5");
    }

    @Test
    public void testSubLists() {
        IntSeq.of("233(ab:c)114514(d:e:f:g)42")
            .mapToObj(i -> (char)i)
            .mapSub('(', ')', s -> s.join(""))
            .printAll();
    }

    @Test
    public void testWhileEquals() {
        Seq<Integer> seq = Seq.of(1, 1, 2, 3, 4, 6);
        seq.takeWhileEquals().assertTo("1,1");
        seq.takeWhileEquals(i -> i / 4).assertTo("1,1,2,3");
        seq.drop(1).takeWhile((i, j) -> i + 1 == j).assertTo("1,2,3,4");
    }

    @Test
    public void testTree() {
        Node n0 = new Node(0);
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Node n3 = new Node(3);
        Node n4 = new Node(4);
        Node n5 = new Node(5);
        n0.left = n1;
        n0.right = n2;
        n1.left = n3;
        n1.right = n4;
        n2.left = n5;
        Seq<Node> seq = Seq.ofTree(n0, n -> Seq.of(n.left, n.right));
        seq.map(n -> n.value).assertTo("0,1,3,4,2,5");
    }

    @Test
    public void testSubCollect() {
        SeqMap<String, Integer> map = Seq.of(
                new Triple<>("john", 2015, "success"),
                new Triple<>("john", 2013, "fail"),
                new Triple<>("chris", 2013, "success"),
                new Triple<>("john", 2012, "fail"),
                new Triple<>("john", 2009, "success"),
                new Triple<>("chris", 2007, "fail"),
                new Triple<>("john", 2005, "fail"))
            .groupBy(r -> r.first, HashMap.class, Seq::count)
            .mapValue(Supplier::get);
        System.out.println(map);
    }

    static class Node {
        final int value;
        Node left;
        Node right;

        Node(int value) {
            this.value = value;
        }
    }
}
