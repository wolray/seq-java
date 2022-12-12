package com.github.wolray.seq;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author wolray
 */
public interface SeqReader<S, T> {
    Seq<T> toSeq(S source) throws Exception;

    default SafeSeq<T> read(S source) {
        return SafeSeq.of(c -> toSeq(source).eval(c));
    }

    interface Is<T> extends SeqReader<Text, T> {
        default SafeSeq<T> read(URL url) {
            return read(url::openStream);
        }

        default SafeSeq<T> read(String file) {
            return read(() -> Files.newInputStream(Paths.get(file)));
        }

        default SafeSeq<T> read(File file) {
            return read(() -> Files.newInputStream(file.toPath()));
        }

        default SafeSeq<T> read(Class<?> cls, String resource) {
            return read(() -> {
                InputStream res = cls.getResourceAsStream(resource);
                if (res == null) {
                    throw new FileNotFoundException(resource);
                }
                return res;
            });
        }
    }

    interface Text extends WithCe.Supplier<InputStream> {}

    class Str implements Is<String> {
        static final Str INSTANCE = new Str() {};

        @Override
        public Seq<String> toSeq(Text source) {
            return c -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(source.get()))) {
                    String s;
                    while ((s = reader.readLine()) != null) {
                        c.accept(s);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }

    static Is<String> str() {
        return Str.INSTANCE;
    }
}
