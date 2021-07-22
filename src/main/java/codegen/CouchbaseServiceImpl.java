package codegen;

import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.ReactiveCluster;
import couchbase.NoDocumentsFoundException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;

public class CouchbaseServiceImpl implements CouchbaseService {
    private ReactiveCluster reactiveCluster;

    public CouchbaseServiceImpl() {
        ClusterOptions clusterOptions = ClusterOptions.clusterOptions("test", "testing");
        reactiveCluster = ReactiveCluster.connect("localhost", clusterOptions);
    }

    public CouchbaseServiceImpl(Vertx vertx) {
        this();
    }

    public Future<JsonArray> query(String query) {
        Promise<JsonArray> promise = Promise.promise();
        //converts reactor code to reactive. will help in vert.x i believe
        reactiveCluster
                .query(query)
                .subscribe(queryResult ->
                        queryResult
                                .rowsAsObject()
                                .map(object -> new JsonObject(object.toMap()))
                                .switchIfEmpty(ignored -> promise.fail(new NoDocumentsFoundException("No results found"))) //throw an error if no results. doing it here means we don't have to null check everywhere
                                .collectList()
                                .subscribe(JsonArray::new), promise::fail);
        return promise.future();
    }

    public Future<JsonObject> get(String bucket, String key) {
        //converts reactor code to reactive. This is required to allow us to be more in line with vert.x


        Promise<io.vertx.core.json.JsonObject> promise = Promise.promise();
        reactiveCluster
                .bucket(bucket)
                .defaultCollection()
                .get(key)
                .subscribe(getResult ->
                        promise.complete(new JsonObject(getResult.contentAsObject().toMap())), promise::fail);
        return promise.future();
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        CouchbaseService couchbaseService = new CouchbaseServiceImpl();

        new ServiceBinder(vertx)
                .setAddress("couchbase")
                .register(CouchbaseService.class, couchbaseService);

        CouchbaseService couchbaseServiceProxy = CouchbaseService.createProxy(vertx, "couchbase");

        couchbaseServiceProxy.get("beer-sample", "21st_amendment_brewery_cafe")
                .onSuccess(System.out::println)
                .onFailure(System.out::println);


    }
}
