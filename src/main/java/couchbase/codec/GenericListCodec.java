package couchbase.codec;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class GenericListCodec<T extends List> implements MessageCodec<T, T> {
    private final Class<T> clazz;

    public GenericListCodec(Class<T> clazz) {
        super();
        this.clazz = clazz;
    }

    @Override
    public void encodeToWire(Buffer buffer, T t) {
        buffer.appendBuffer(new JsonArray(t).toBuffer());
    }

    @Override
    public T decodeFromWire(int i, Buffer buffer) {
        return (T) buffer.toJsonArray().getList();
    }

    @Override
    public T transform(T t) {
        return t;
    }

    @Override
    public String name() {
        return clazz.getSimpleName() + "codec";
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
