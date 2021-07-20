package test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class TestVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        super.start(startPromise);
        vertx.eventBus().consumer("tst", this::test);
    }

    private void test(Message<JsonObject> message) {
        getFuture(message.body())
                .compose(this::getFuture2)
                .compose(body -> vertx.eventBus().request("test", body))
                .onFailure(cause -> message.fail(500, cause.getMessage()))
                .onSuccess(result -> {
                    String correlationId = result.headers().get("correlationId");
                    message.reply(result.body());
                });
    }


    private Future<JsonObject> getFuture2(JsonObject body) {
        Promise<JsonObject> promise = Promise.promise();
        promise.fail(new Exception("broken"));
        return promise.future();
    }

    private Future<JsonObject> getFuture(JsonObject body) {
        Promise<JsonObject> promise = Promise.promise();
        promise.complete(body);
        return promise.future();
    }


    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        TestVerticle testVerticle = new TestVerticle();
        Future<String> future = vertx.deployVerticle(testVerticle);
        future.onSuccess(ignored -> {
            vertx.eventBus().request("tst", new JsonObject().put("testKey", "value"), result -> {
                if (result.succeeded()) {
                    System.out.println(result.result().body());
                } else {
                    System.out.println(result.cause());
                }
            });
        });
    }

}
