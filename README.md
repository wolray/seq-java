# Seq for Java

This project provides a new streaming API, called `Seq`. It looks very much like `Sequence` of the Kotlin language, while implemented by a totally different mechanism and wrote in pure Java.

## Definition

Unlike most streaming API implementations, `Seq` is not based on `Iterator`. It only requires a single `Consumer -> void` interface. While `Iterator` needs both `next` and `hasNext`, which makes them hard to adapt for some procedural languages. Here is the definition of `Seq`:
```java
public interface Seq<T> {
    void supply(Consumer<T> consumer);
}
```
This `supply` is the only abstract interface of `Seq`, and is naturally implemented (and inspired) by `Iterable`'s `forEach`, meaning "supplying elements to the consumer". It is so simple and low-level that it can be implemented to nearly any language, as long as the language supports closures.

## Features

* Powerful and complete streaming API, **FASTER** than Java `Stream` and Kotlin `Sequence`.
* **GENERATOR** ability like `yield` in Python, Javascript, Kotlin, C# and so on. Coroutines or CPS (Continuation-Passing-Style) transformation are **NOT** required.

For the first time, you can do actual generator `yield` in Java, and not just Java. This API is applicable to any closure-supported language.

## API

### Initialization
#### From literal
```java
Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
```
#### From collection
```java
Seq<Integer> seq = Seq.of(Arrays.asList(1, 2, 3, 4, 5));
```
#### By repeating
```java
Seq<String> seq = Seq.repeat(3, "a"); // "a", "a", "a"
```
#### By range
Returns a primitive `IntSeq` rather than `Seq<Integer>`.
```java
IntSeq seq = IntSeq.range(4); // 0, 1, 2, 3
IntSeq seq = IntSeq.range(1, 4); // start from 1 => 1, 2, 3
IntSeq seq = IntSeq.range(1, 4, 2); // with step 2 => 1, 3
```
#### Endless supplying
```java
Seq<Integer> seq = Seq.tillNull(() -> 1); // 1, 1, 1, ...
```
#### Endless single-seed recursion
```java
Seq<Integer> seq = Seq.gen(1, i -> i * 2); // 1, 2, 4, 8, 16, ...
```
#### Endless two-seeds recursion
Fibonnaci numbers.
```java
Seq<Integer> seq = Seq.gen(1, 1, Integer::sum); // 1, 1, 2, 3, 5, 8, ...
```
#### By GENERATOR
This is my favorite part, and the most beautiful feature. Using a generator, all you need to do is write a simple lambda block accepting a normal `Consumer c` as parameter. The `Consumer`'s `accept` method will play the `yield` role.
```java
// 1, 2, 4, 8, 16, ...
Seq<Integer> seq = c -> {
    int i = 1;
    c.accept(i);
    // don't worry to write an endless loop inside the generator
    while (true) {
        c.accept(i = i * 2);
    }
};
```
See? Still your familiar old classic Java, just with a brand-new meaning now.

#### By a tree
Preorder traversing a tree in one line, which is also implemented by generator. In fact, any recursive procedure like DFS can be easily convert into a `seq`.
```java
Seq<Node> seq = Seq.ofTree(root, n -> Seq.of(n.left, n.right));
```
### Lazy mapping operations
As a streaming API, `Seq` provides standard lazy mapping functions for chaining operations. Given an example of `[1, 1, 2, 3, 4]`.
```java
Seq<Integer> seq = Seq.of(1, 1, 2, 3, 4);
```
#### map
Same as Java `stream.map` and Kotlin `sequence.map`. See also `mapNotNull`.
```java
seq = seq.map(i -> i + 10); // 11, 11, 12, 13, 14
```
#### mapPair
Map to paired sequence.
```java
Seq<Pair<Integer, Integer>> adjacent = seq.mapPair(false); // (1,1), (2,3)
Seq<Pair<Integer, Integer>> overlapping = seq.mapPair(true); // (1,1), (1,2), (2,3), (3,4)
```
#### circle
Returns an endless sequence like a circle.
```java
seq = seq.circle(); // 1, 1, 2, 3, 4, 1, 1, 2, 3, 4, ...
```
#### duplicateAll
Duplicate the sequence for `n` times.
```java
seq = seq.duplicateAll(2); // 1, 1, 2, 3, 4, 1, 1, 2, 3, 4
```
#### duplicateEach
Duplicate each element for `n` times.
```java
seq = seq.duplicateEach(2); // 1, 1, 1, 1, 2, 2, 3, 3, 4, 4
```
#### duplicateIf
Duplicate each element for `n` times if tested as true.
```java
seq = seq.duplicateIf(3, i -> i % 2 == 0); // 1, 1, 2, 2, 2, 3, 4, 4, 4
```
#### filter
Same as Java `stream.filter` and Kotlin `sequence.filter`. See also `filterNot`, `filterNotNull`, `filterIn`, `filterNotIn`.
```java
seq = seq.filter(i -> i % 2 > 0); // 1, 1, 3
```
#### take
Same as Java `stream.limit` and Kotlin `sequence.take`.
```java
seq = seq.take(3); // 1, 1, 2
```
#### drop
Same as Java `stream.skip` and Kotlin `sequence.drop`.
```java
seq = seq.drop(3); // 3, 4
```
#### takeWhile
Same as Java `stream.takeWhile` and Kotlin `sequence.takeWhile`. See also `takeWhileEquals`.
```java
seq = seq.takeWhile(i -> i < 3); // 1, 1, 2
```
#### dropWhile
Same as Java `stream.dropWhile` and Kotlin `sequence.dropWhile`.
```java
seq = seq.dropWhile(i -> i < 3); // 3, 4
```
#### distinct
Same as Java `stream.distinct` and Kotlin `sequence.distinct`.
```java
seq = seq.distinct(); // 1, 2, 3, 4
```
#### distinctBy
Same as Kotlin `sequence.distinctBy`.
```java
seq = seq.distinctBy(i -> i % 2); // 1, 2
```
#### flatMap
Same as Java `stream.flatMap` and Kotlin `sequence.flatMap`.
```java
seq = seq.flatMap(i -> Seq.repeat(2, i)); // 1, 1, 1, 1, 2, 2, 3, 3, 4, 4
```
#### runningFold
Accumulation on every element. Same as Kotlin `sequence.runningFold`.
```java
seq = seq.runningFold(0, Integer::sum); // 1, 2, 4, 7, 11
```
#### withIndex
Same as Kotlin `sequence.withIndex`.
```java
Seq<IntPair<Integer>> newSeq = seq.withIndex(); // (0,1), (1,1), (2,2), (3,3), (4,4)
```
#### pairWith
Attach another value with each element as a pair.
```java
Seq<Pair<Integer, String>> newSeq = seq.pairWith(i -> i.toString()); // (1,"1"), (1,"1"), (2,"2"), (3,"3"), (4,"4")
```
#### zip
Combine with another iterable's element to pairs one-by-one. Same as Kotlin `sequence.zip`.
```java
Seq<Pair<Integer, String>> s1 = seq.zip(Arrays.asList("a", "b", "c", "d", "e")); // (1,"a"), (1,"b"), (2,"c"), (3,"d"), (4,"e") 
Seq<String> s2 = seq.zip(Arrays.asList("a", "b", "c", "d", "e"), (i, s) -> i + s); // "1a", "1b", "2c", "3d", "4e"
```
#### onEach
Same as Java `stream.peek` and Kotlin `sequence.onEach`.
```java
seq = seq.onEach(System.out::println); // same as old but also println each element
```
#### onFirst
```java
seq = seq.onFirst(System.out::println); // same as old but also (lazily) println the first element
```
#### onLast
```java
seq = seq.onLast(System.out::println); // same as old but also (lazily) println the last element
```
#### cache
Though `Seq` is always resuable unlike disposable Java `Stream`, it might be high-cost sometimes (usually from IO). You can cache it for once then use it anywhere else.
```java
seq = seq.cache();
```
#### parallel
Same as Java `stream.parallel` to create a new parallelized `seq`, usually for multiple IO tasks.
```java
seq = seq.parallel();
```
### Terminal operations
Like any other streaming API, a `seq` is not evaluated until calling terminal operations.

#### supply
Same as Java `stream.forEach` and Kotlin `sequence.forEach`.
```java
seq.supply(System.out::println);
```
#### fold
Same as Kotlin `sequence.fold` and a little like Java `stream.reduce`.
```java
Integer sum1 = seq.fold(0, (acc, i) -> acc + i); // sum by folding each element with initial 0
int sum2 = seq.foldInt(0, (acc, i) -> acc + i); // sum by folding each element with initial 0
```
#### foldIndexed
Same as Kotlin `sequence.forEachIndexed`.
```java
seq.foldIndexed((index, i) -> System.out.println(index + " " + i));
```
#### feed
Giving a destination then feed all elements to it. Works like `fold` but requires a `BiConsumer` rather than `BiFunction`.
```java
List<Integer> list = seq.feed(new ArrayList<>(), List::add); // collect to list
```
#### collect
Same as Java `stream.collect(Collectors.toCollection(des))` and Kotlin `sequence.toCollection(des)`, implemented by `feed`. See also `collectBy`.
```java
List<Integer> list = seq.collect(new LinkedList<>());
```
#### toList
Same as Java `stream.collect(Collectors.toList())` and Kotlin `sequence.toList()`, implemented by `collect`.
```java
List<Integer> list = seq.toList();
```
#### toSet
Same as Java `stream.collect(Collectors.toSet())` and Kotlin `sequence.toSet()`, implemented by `collect`.
```java
Set<Integer> set = seq.toSet();
```
#### toMap
Same as Java `stream.collect(Collectors.toMap(...))` and Kotlin `sequence.toMap(...)`, implemented by `feed`.
```java
Map<Integer, String> map = seq.toMap(i -> i, i -> i.toString());
```
#### toMapBy & toMapWith
Same as Kotlin `sequence.toMapBy` (creating keys) and `sequence.toMapWith` (creating values), implemented by `feed`.
```java
Map<String, Integer> newKeysMap = seq.toMapBy(i -> i.toString());
Map<Integer, String> newValuesMap = seq.toMapWith(i -> i.toString());
```
#### groupBy
Much like Java `stream.collect(Collectors.groupingBy(...))` and Kotlin `sequence.groupingBy`.
```java
Map<Integer, List<Integer>> listMap = seq.groupBy(i -> i % 2, f -> f.toList());
```
#### join
Same as Java `stream.collect(Collectors.joining(sep))` and Kotlin `sequence.joiningToString`, implemented by `feed`.
```java
String s1 = seq.join(",");
String s2 = seq.join(",", i -> "10" + i);
```
#### first
Same as Java `stream.findFirst` and Kotlin `sequence.first`. See also `firstNot`, `firstNotNull`.
```java
Integer first = seq.first();
Integer firstOdd = seq.first(i -> i % 2 > 0);
```
#### last
Same as Kotlin `sequence.last`.
```java
Integer last = seq.last();
```
#### any & anyNot & all & none
Same as Java `stream.anyMatch`, `stream.allMatch`, `stream.noneMatch` and Kotlin `sequence.any`, `sequence.all`, `sequence.none`.
```java
boolean anyOdd = seq.any(i -> i % 2 > 0);
boolean anyEven = seq.anyNot(i -> i % 2 > 0);
boolean allOdd = seq.all(i -> i % 2 > 0);
boolean noneOdd = seq.none(i -> i % 2 > 0);
```
#### count & sum & average
Same as Java `stream.count`, `stream.mapToInt(...).sum`, `stream.mapToInt(...).average` and Kotlin `sequence.count`, `sequence.sum`, `sequence.average`.
```java
int count = seq.count();
int countOdd = seq.count(i -> i % 2 > 0);
int sum = seq.sum(i -> i);
double avg = seq.average(i -> i);
double weightedAvg = seq.average(i -> i, i -> i);
```
#### max & min
Same as Java `stream.max` and Kotlin `sequence.max`. See also `maxBy`, `maxWith`.
```java
Integer max = seq.max(Integer::compareTo);
Integer min = seq.maxBy(i -> -i);
Pair<Integer, Integer> minWithValue = seq.maxWith(i -> -i);
```
#### sort & sortOn & sortBy
Same as Java `stream.sort` and Kotlin `sequence.sort` series.
```java
Seq<Integer> s1 = seq.sort(); // 1, 1, 2, 3, 4
Seq<Integer> s2 = seq.sortOn(Integer::compareTo); // 1, 1, 2, 3, 4
Seq<Integer> s3 = seq.sortDesc(); // 4, 3, 2, 1, 1
Seq<Integer> s4 = seq.sortDesc(Integer::compareTo); // 4, 3, 2, 1, 1
Seq<Integer> s5 = seq.sortBy(i -> 10 - i); // 4, 3, 2, 1, 1
Seq<Integer> s6 = seq.sortDescBy(i -> 10 - i); // 1, 1, 2, 3, 4
```

## Usage
Add [jitpack](https://www.jitpack.io/#wolray/seq-java) repo into your maven `pom.xml` or the gradle equivalent.
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://www.jitpack.io</url>
    </repository>
</repositories>
```
Then add the dependency.
```xml
<dependency>
    <groupId>com.github.wolray</groupId>
    <artifactId>seq-java</artifactId>
    <version>1.3.5</version>
</dependency>
```
