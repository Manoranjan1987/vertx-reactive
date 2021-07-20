package couchbase.codec;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

public class GenericModelCodec<T> implements MessageCodec<T,T> {
    private final Class<T> clazz;

    public GenericModelCodec(Class<T> clazz) {
        super();
        this.clazz = clazz;
    }

    @Override
    public void encodeToWire(Buffer buffer, T t) {
        buffer.appendBuffer(JsonObject.mapFrom(t).toBuffer());
    }

    @Override
    public T decodeFromWire(int i, Buffer buffer) {
        return buffer.toJsonObject().mapTo(clazz);
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
