package codegen;

import couchbase.CouchbaseServiceVertxEBProxy;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ProxyGen
public interface CouchbaseService {


    static CouchbaseService create(Vertx vertx){
        return new CouchbaseServiceImpl(vertx);
    }

    static CouchbaseService createProxy(Vertx vertx, String address){
        return new CouchbaseServiceVertxEBProxy(vertx, address);
    }

    Future<JsonObject> get(String bucket, String key);
    Future<JsonArray> query(String query);
}
