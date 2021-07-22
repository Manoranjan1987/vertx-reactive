package couchbase;

import api.codec.GenericListCodec;
import api.codec.GenericModelCodec;
import couchbase.model.GetRequest;
import couchbase.model.QueryRequest;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.RxHelper;
import io.vertx.rxjava3.core.Vertx;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class CouchbaseVerticle extends AbstractVerticle {
    private final CouchbaseRepository couchbaseRepository;

    @Inject
    public CouchbaseVerticle(CouchbaseRepository couchbaseRepository) {
        this.couchbaseRepository = couchbaseRepository;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        vertx.eventBus().consumer("couchbase.query", this::query);
        vertx.eventBus().consumer("couchbase.get", this::get);
        startPromise.complete();
    }

    private void query(Message<QueryRequest> message) {
        QueryRequest queryRequest = message.body();
        couchbaseRepository.query(queryRequest)
                .map(object -> new JsonObject(object.toMap())) //using map as it's more reliable than string
                .toList()
                .subscribe(
                        message::reply,
                        cause -> {
                            if (cause instanceof NoDocumentsFoundException) {
                                message.fail(404, cause.getMessage());
                            } else {
                                message.fail(500, cause.getMessage());
                            }
                        });
    }

    private void get(Message<GetRequest> message) {
        GetRequest getRequest = message.body();
        couchbaseRepository.get(getRequest)
                .map(jsonObject -> new JsonObject(jsonObject.toMap()))
                .subscribe(message::reply,
                        cause -> message.fail(500, cause.getMessage()));
    }

    public static void main(String[] args) {
        CouchbaseRepository couchbaseRepository = new CouchbaseRepository();
        CouchbaseVerticle couchbaseVerticle = new CouchbaseVerticle(couchbaseRepository);
        Vertx vertx = Vertx.vertx();


        vertx.eventBus().getDelegate().registerDefaultCodec(QueryRequest.class, new GenericModelCodec<>(QueryRequest.class))
                .registerDefaultCodec(GetRequest.class, new GenericModelCodec<>(GetRequest.class))
                .registerDefaultCodec(ArrayList.class, new GenericListCodec<>(ArrayList.class));

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

        QueryRequest request1 = new QueryRequest("select `beer-sample`.* from `beer-sample` where type=\"brewery\" limit 1");
        QueryRequest request2 = new QueryRequest("select `beer-sample`.* from `beer-sample` where type=\"brewery\" limit 1 offset 2");

        //combine results, ignore 404 errors
        Single<List<JsonObject>> query1 = vertx.eventBus().<List<JsonObject>>rxRequest("couchbase.query", request1)
                .map(io.vertx.rxjava3.core.eventbus.Message::body);

        Single<List<JsonObject>> query2 = vertx.eventBus().<List<JsonObject>>rxRequest("couchbase.query", request2)
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
