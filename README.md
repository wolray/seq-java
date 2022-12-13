# Seq for Java

This project provides a new `Stream` like API, called `Seq`. It is base on a brand new and advanced mechanism and is
very much like `Sequence` of Kotlin language, while purely implemented by Java.

This new mechanism is so flexible and low-level that can nearly be implemented to any language, as long as the language
supports closure.

## Features

* Powerful and complete streaming API, **FASTER** than Java `Stream` and Kotlin `Sequence`.
* **Generator** ability like `yield` in Python/Javascript/Kotlin, `yield return` in C#, etc. Coroutines or CPS (Continuation-Passing-Style) are **NOT** required.
* Applicable to any closure-enabled language.

## API

### Initialization
By literal
```java
Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
```
By collection
```java
Seq<Integer> seq = Seq.of(Arrays.asList(1, 2, 3, 4, 5));
```
By repeating
```java
Seq<String> seq = Seq.repeat(3, "a"); // "a", "a", "a"
```
By range (returns a primitive `IntSeq` rather than `Seq<Integer>`)
```java
IntSeq seq = IntSeq.range(4); // 0, 1, 2, 3
IntSeq seq = IntSeq.range(1, 4); // start from 1 => 1, 2, 3
IntSeq seq = IntSeq.range(1, 4, 2); // with step 2 => 1, 3
```
Endless seq by supplier (zero-seed recursion)
```java
Seq<Integer> seq = Seq.tillNull(() -> 1); // 1, 1, 1, ...
```
Endless seq by single-seed recursion
```java
Seq<Integer> seq = Seq.gen(1, i -> i * 2); // 1, 2, 4, 8, 16, ...
```
Endless fibonnaci numbers (two-seeds recursion)
```java
Seq<Integer> seq = Seq.gen(1, 1, Integer::sum); // 1, 1, 2, 3, 5, 8, ...
```
By generator (no keyword like `yield` is needed, just a simple lambda)
```java
// 1, 2, 4, 8, 16, ...
Seq<Integer> seq = c -> {
    int i = 1;
    c.accept(i);
    while (true) {
        c.accept(i = i * 2);
    }
};
```

By a tree (preorder traversal)
```java
Seq<Node> seq = Seq.ofTree(root, n -> Seq.of(n.left, n.right));
```
### Lazy mapping operations
Give a `seq` of `[1, 1, 2, 3, 4]`
```java
Seq<Integer> seq = Seq.of(1, 1, 2, 3, 4);
```
map
```java
Seq<Integer> newSeq = seq.map(i -> i + 10); // 11, 11, 12, 13, 14
```
filter
```java
Seq<Integer> newSeq = seq.filter(i -> i % 2 > 0); // 1, 1, 3
```
take
```java
Seq<Integer> newSeq = seq.take(3); // 1, 1, 2
```
drop
```java
Seq<Integer> newSeq = seq.drop(3); // 3, 4
```
takeWhile
```java
Seq<Integer> newSeq = seq.takeWhile(i -> i < 3); // 1, 1, 2
```
dropWhile
```java
Seq<Integer> newSeq = seq.dropWhile(i -> i < 3); // 3, 4
```
distinct
```java
Seq<Integer> newSeq = seq.distinct(); // 1, 2, 3, 4
```
distinctBy
```java
Seq<Integer> newSeq = seq.distinctBy(i -> i % 2); // 1, 2
```
flatMap
```java
Seq<Integer> newSeq = seq.flatMap(i -> Seq.repeat(2, i)); // 1, 1, 1, 1, 2, 2, 3, 3, 4, 4
```
runningFold (accumulation on every element)
```java
Seq<Integer> newSeq = seq.runningFold(0, Integer::sum); // 1, 2, 4, 7, 11
```
withIndex
```java
Seq<IntPair<Integer>> newSeq = seq.withIndex(); // (0,1), (1,1), (2,2), (3,3), (4,4)
```
pairWith (attach another value with each element as a pair)
```java
Seq<Pair<Integer, String>> newSeq = seq.pairWith(i -> i.toString()); // (1,"1"), (1,"1"), (2,"2"), (3,"3"), (4,"4")
```
zip
```java
Seq<Pair<Integer, String>> s1 = seq.zip(Arrays.asList("a", "b", "c", "d", "e")); // (1,"a"), (1,"b"), (2,"c"), (3,"d"), (4,"e") 
Seq<String> s2 = seq.zip(Arrays.asList("a", "b", "c", "d", "e"), (i, s) -> i + s); // "1a", "1b", "2c", "3d", "4e"
```
onEach
```java
Seq<Integer> newSeq = seq.onEach(System.out::println); // same as old but also println each element
```
onFirst
```java
Seq<Integer> newSeq = seq.onFirst(System.out::println); // same as old but also (lazily) println the first element
```
onLast
```java
Seq<Integer> newSeq = seq.onLast(System.out::println); // same as old but also (lazily) println the last element
```
sort
```java
Seq<Integer> s1 = seq.sort(); // 1, 1, 2, 3, 4
Seq<Integer> s2 = seq.sortOn(Integer::compareTo); // 1, 1, 2, 3, 4
Seq<Integer> s3 = seq.sortDesc(); // 4, 3, 2, 1, 1
Seq<Integer> s4 = seq.sortDesc(Integer::compareTo); // 4, 3, 2, 1, 1
Seq<Integer> s5 = seq.sortBy(i -> 10 - i); // 4, 3, 2, 1, 1
Seq<Integer> s6 = seq.sortDescBy(i -> 10 - i); // 1, 1, 2, 3, 4
Seq<Pair<Integer, Integer>> s7 = seq.sortWith(i -> 10 - i); // (4,6), (3,7), (2,8), (1,9), (1,9)
Seq<Pair<Integer, Integer>> s8 = seq.sortDescWith(i -> 10 - i); // (1,9), (1,9), (2,8), (3,7), (4,6)
```
