package api;

import api.codec.GenericListCodec;
import api.codec.GenericModelCodec;
import api.factory.GuiceVerticleFactory;
import api.model.Brewery;
import api.model.Geography;
import api.model.GetBreweryRequest;
import api.model.UpdateBreweryRequest;
import api.module.CouchbaseRepositoryModule;
import api.module.ReactiveClusterModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import couchbase.model.GetRequest;
import couchbase.model.QueryRequest;
import couchbase.model.UpdateRequest;
import io.reactivex.rxjava3.core.Observable;
import io.vertx.core.Launcher;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.eventbus.EventBus;

import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        new Launcher().dispatch(args);
        Injector injector = setupJuice();
        Observable<String> observable = Observable.just(
                "guice:api.controller.BreweryController",
                "guice:api.service.BreweryService",
                "guice:couchbase.CouchbaseVerticle"
        );
        Vertx vertx = setUpVertx(injector);


        observable
                .flatMap(name -> vertx.rxDeployVerticle(name).toObservable())
                .doOnComplete(() -> System.out.println("All verticles deployed"))
                .subscribe(deploymentId -> System.out.println("deployed verticle: " + deploymentId),
                        cause -> System.out.println("failed to deploy verticle due to: " + cause.getMessage()));
    }

    private static Injector setupJuice() {
        return Guice.createInjector(
                new CouchbaseRepositoryModule(),
                new ReactiveClusterModule()
        );
    }

    private static Vertx setUpVertx(Injector injector) {
        Vertx vertx = Vertx.vertx();
        vertx.registerVerticleFactory(new GuiceVerticleFactory(injector));
        registerCodecs(vertx.eventBus());
        return vertx;
    }

    private static void registerCodecs(EventBus eventBus) {
        eventBus.getDelegate()
                .registerDefaultCodec(Geography.class, new GenericModelCodec<>(Geography.class))
                .registerDefaultCodec(Brewery.class, new GenericModelCodec<>(Brewery.class))
                .registerDefaultCodec(GetBreweryRequest.class, new GenericModelCodec<>(GetBreweryRequest.class))
                .registerDefaultCodec(GetRequest.class, new GenericModelCodec<>(GetRequest.class))
                .registerDefaultCodec(UpdateBreweryRequest.class, new GenericModelCodec<>(UpdateBreweryRequest.class))
                .registerDefaultCodec(UpdateRequest.class, new GenericModelCodec<>(UpdateRequest.class))
                .registerDefaultCodec(ArrayList.class, new GenericListCodec<>(ArrayList.class))
                .registerDefaultCodec(QueryRequest.class, new GenericModelCodec<>(QueryRequest.class));
    }
}
