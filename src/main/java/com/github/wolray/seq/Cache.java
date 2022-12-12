package com.github.wolray.seq;

import java.util.List;

/**
 * @author wolray
 */
public interface Cache<T> {
    boolean exists();

    Seq<T> read();

    void write(List<T> ts);
}
