package api;

import api.codec.GenericListCodec;
import api.model.Brewery;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.RoutingContext;

import javax.inject.Inject;
import java.util.List;

public class BreweryController extends AbstractVerticle {
    private EventBus eventBus;
    private GenericListCodec<List> genericListCodec;

    @Inject
    public BreweryController() {
        this.genericListCodec = new GenericListCodec<>(List.class);
    }

    @Override
    public void start(Promise<Void> promise) throws Exception {
        eventBus = vertx.eventBus();
        Router router = createRouter();
        createHttpServer(router, promise);
    }

    private void createHttpServer(Router router, Promise<Void> promise) {
        vertx.createHttpServer()
                .requestHandler(router)
                .rxListen(8080)
                .subscribe(server -> promise.complete(), System.out::println);
    }

    private Router createRouter() {
        Router router = Router.router(vertx);
        router
                .get("/breweries").handler(this::getBreweries);
        return router;
    }

    private void getBreweries(RoutingContext routingContext) {
        eventBus.<List<Brewery>>rxRequest("Breweryservice.getBreweries", new JsonObject())
                .subscribe(results -> {
                    //not in love with this
                    Buffer buffer = Buffer.buffer();
                    genericListCodec.encodeToWire(buffer.getDelegate(), results.body());
                    routingContext.response().end(buffer);
                }, cause -> {
                    System.out.println(cause);
                    routingContext.response().setStatusCode(500).end("unable to service request");
                });
    }
}
