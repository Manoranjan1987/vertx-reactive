package api.controller;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;

public class ResponseParser {

    public Buffer parseObject(Object result) {
        return Json.encodeToBuffer(result);
    }
}
