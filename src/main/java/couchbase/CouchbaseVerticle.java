package couchbase;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.RxHelper;
import io.vertx.rxjava3.core.Vertx;

public class CouchbaseVerticle extends AbstractVerticle {
    private final CouchbaseRepository couchbaseRepository;

    public CouchbaseVerticle(CouchbaseRepository couchbaseRepository) {
        this.couchbaseRepository = couchbaseRepository;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        vertx.eventBus().consumer("couchbase.query", this::query);
        startPromise.complete();
    }

    private void query(Message<JsonObject> message) {
        JsonObject request = message.body();
        String query = request.getString("query");
        couchbaseRepository.query(query)
                .map(object -> new JsonObject(object.toMap())) //using map as it's more reliable than string
                .toList()
                .subscribe(
                        list -> message.reply(new JsonArray(list)),
                        cause -> {
                            if (cause instanceof NoDocumentsFoundException) {
                                message.fail(404, cause.getMessage());
                            } else {
                                message.fail(500, cause.getMessage());
                            }
                        });
    }

    public static void main(String[] args) {
        CouchbaseRepository couchbaseRepository = new CouchbaseRepository();
        CouchbaseVerticle couchbaseVerticle = new CouchbaseVerticle(couchbaseRepository);
        Vertx vertx = Vertx.vertx();


        //blocking this just so i can avoid a callback as in a normal system the two calls would be seperate
        RxHelper.deployVerticle(vertx, couchbaseVerticle)
                .blockingGet();

        //combine results, error if exception
/*        Single<JsonArray> query1= vertx.eventBus().<JsonArray>rxRequest("couchbase.query", new JsonObject().put("query", "select `beer-sample`.* from `beer-sample` where type=\"brewery\" limit 1"))
                .map(io.vertx.rxjava3.core.eventbus.Message::body);

        Single<JsonArray> query2= vertx.eventBus().<JsonArray>rxRequest("couchbase.query", new JsonObject().put("query", "select `beer-sample`.* from `beer-sample` where type=\"brewery\" limit 1 offset 2"))
                .map(io.vertx.rxjava3.core.eventbus.Message::body);

        Single.merge(query1, query2)
                .flatMap(resultSet -> Flowable.fromStream(resultSet.stream()))
                .cast(JsonObject.class)
                .switchIfEmpty(Flowable.error(new Exception("no results")))
                .toList()
                .subscribe(System.out::println, System.out::println);*/

        //combine results, ignore 404 errors
        Single<JsonArray> query1 = vertx.eventBus().<JsonArray>rxRequest("couchbase.query", new JsonObject().put("query", "select `beer-sample`.* from `beer-sample` where type=\"brewery\" limit 1"))
                .map(io.vertx.rxjava3.core.eventbus.Message::body);

        Single<JsonArray> query2 = vertx.eventBus().<JsonArray>rxRequest("couchbase.query", new JsonObject().put("query", "select `beer-sample`.* from `beer-sample` where type=\"brewery\" limit 1 offset 2"))
                .map(io.vertx.rxjava3.core.eventbus.Message::body);

        Single.merge(query1, query2)
                .flatMap(resultSet -> Flowable.fromStream(resultSet.stream()))
                .cast(JsonObject.class)
                .onErrorResumeNext(cause -> {
                    if (((ReplyException) cause).failureCode() != 404) {
                        return Flowable.error(cause);
                    } else {
                        return Flowable.empty();
                    }
                })
                .switchIfEmpty(Flowable.error(new NoDocumentsFoundException("no results")))
                .toList()
                .subscribe(System.out::println, System.out::println);
    }

}
