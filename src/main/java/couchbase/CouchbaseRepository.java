package couchbase;

import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.ReactiveCluster;
import com.couchbase.client.java.json.JsonObject;
import couchbase.model.GetRequest;
import couchbase.model.QueryRequest;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class CouchbaseRepository {
    private final ReactiveCluster reactiveCluster;

    public CouchbaseRepository() {
        ClusterOptions clusterOptions = ClusterOptions.clusterOptions ("test", "testing");
        reactiveCluster = ReactiveCluster.connect("localhost", clusterOptions);
    }

    @Inject
    public CouchbaseRepository(ReactiveCluster reactiveCluster) {
        this.reactiveCluster = reactiveCluster;
    }

    public Observable<JsonObject> query(QueryRequest query) {
        //converts reactor code to reactive. will help in vert.x i believe
        return Observable.create(emitter ->
                reactiveCluster
                        .query(query.getQuery())
                        .subscribe(queryResult ->
                                        queryResult
                                                .rowsAsObject()
                                                .switchIfEmpty(ignored -> emitter.onError(new NoDocumentsFoundException("No results found"))) //throw an error if no results. doing it here means we don't have to null check everywhere
                                                .doOnComplete(emitter::onComplete) //complete the emitter once all results are retrieved
                                                .subscribe(emitter::onNext) //emit each jsonObject as a value
                                , emitter::onError));
    }

    public Single<JsonObject> get(GetRequest getRequest) {
        //converts reactor code to reactive. This is required to allow us to be more in line with vert.x
        return Single.create(emitter ->
                reactiveCluster
                        .bucket(getRequest.getBucket())
                        .defaultCollection()
                        .get(getRequest.getKey())
                        .subscribe(getResult ->
                                emitter.onSuccess(getResult.contentAsObject()), emitter::onError));
    }

    public static void main(String[] args) throws InterruptedException {
        CouchbaseRepository couchbaseRepository = new CouchbaseRepository();
        couchbaseRepository.query(new QueryRequest("select `beer-sample`.* from `beer-sample` where type=\"brewery\" limit 5"))
                .delay(1, TimeUnit.SECONDS)
                .map(object -> new io.vertx.core.json.JsonObject(object.toString()))
                //.switchIfEmpty(Observable.error(new Exception("no results")))
                .subscribe(System.out::println, System.out::println);
        //first println is for success, second for error
        couchbaseRepository.get(new GetRequest("beer-sample", "21st_amendment_brewery_cafe"))
                .subscribe(System.out::println, System.out::println);
        //get is faster than query so we get the result for GET prior to the result for query...async :D

        System.out.println("done");
        Thread.sleep(10000);
    }
}
