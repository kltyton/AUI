package io.github.kltyton.kltytonui.network.codec;

public interface StreamDecoder<I, T> {
    T decode(I object);
}
