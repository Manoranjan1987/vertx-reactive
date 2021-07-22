package api.controller;

import api.model.ErrorResponse;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class ResponseParser {

    public Buffer parseObject(Object result) {
        return Json.encodeToBuffer(result);
    }

    public ErrorResponse parseError(Throwable throwable){
        ReplyException replyException;
        if(throwable instanceof ReplyException){
            replyException = (ReplyException) throwable;
        }else{
            replyException = new ReplyException(ReplyFailure.ERROR, 500, throwable.getMessage());
        }

        JsonObject responseBody = new JsonObject().put("error_description", replyException.getMessage())
                .put("error_code", replyException.failureCode())
                .put("error_type", replyException.failureType().name());
        return new ErrorResponse(replyException.failureCode(), responseBody.toBuffer());
    }
}
