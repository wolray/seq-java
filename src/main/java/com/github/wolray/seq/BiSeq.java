package com.github.wolray.seq;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * @author wolray
 */
public interface BiSeq<K, V> extends Seq0<BiConsumer<K, V>> {
    default BiSeq<K, V> filter(BiPredicate<K, V> predicate) {
        return c -> supply((k, v) -> {
            if (predicate.test(k, v)) {
                c.accept(k, v);
            }
        });
    }

    default Seq<K> keepFirst() {
        return c -> supply((k, v) -> c.accept(k));
    }

    default Seq<V> keepSecond() {
        return c -> supply((k, v) -> c.accept(v));
    }

    default <T> Seq<T> map(BiFunction<K, V, T> function) {
        return c -> supply((k, v) -> c.accept(function.apply(k, v)));
    }

    default <T> BiSeq<T, V> mapFirst(BiFunction<K, V, T> function) {
        return c -> supply((k, v) -> c.accept(function.apply(k, v), v));
    }

    default <T> BiSeq<V, T> mapSecond(BiFunction<K, V, T> function) {
        return c -> supply((k, v) -> c.accept(v, function.apply(k, v)));
    }

    default Seq<Pair<K, V>> paired() {
        return map(Pair::new);
    }
}
