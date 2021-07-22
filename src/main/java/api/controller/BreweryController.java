package api.controller;

import api.model.Brewery;
import api.model.BreweryRequest;
import api.model.ErrorResponse;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.RoutingContext;

import javax.inject.Inject;
import java.util.List;

public class BreweryController extends AbstractVerticle {
    private final ResponseParser responseParser;
    private EventBus eventBus;

    @Inject
    public BreweryController(ResponseParser responseParser) {
        this.responseParser = responseParser;
    }

    @Override
    public void start(Promise<Void> promise) {
        eventBus = vertx.eventBus();
        Router router = createRouter();
        createHttpServer(router, promise);
    }

    private void createHttpServer(Router router, Promise<Void> promise) {
        vertx.createHttpServer()
                .requestHandler(router)
                .rxListen(8080)
                .subscribe(server -> promise.complete(), promise::fail);
    }

    private Router createRouter() {
        Router router = Router.router(vertx);
        router.get("/breweries").handler(this::getBreweries);
        router.get("/breweries/:id").handler(this::getBrewery);
        return router;
    }

    private void getBreweries(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response().getDelegate();
        eventBus.<List<Brewery>>rxRequest("Breweryservice.getBreweries", null)
                .subscribe(results -> {
                    response.end(responseParser.parseObject(results.body()));
                }, cause -> {
                    ErrorResponse errorResponse = responseParser.parseError(cause);
                    response.setStatusCode(errorResponse.getStatusCode()).end(errorResponse.getBody());
                });
    }

    private void getBrewery(RoutingContext routingContext){
        HttpServerResponse response = routingContext.response().getDelegate();
        String id = routingContext.pathParam("id");
        BreweryRequest breweryRequest = new BreweryRequest(id);
        eventBus.<List<Brewery>>rxRequest("Breweryservice.getBrewery", breweryRequest)
                .subscribe(results -> {
                    response.end(responseParser.parseObject(results.body()));
                }, cause -> {
                    ErrorResponse errorResponse = responseParser.parseError(cause);
                    response.setStatusCode(errorResponse.getStatusCode()).end(errorResponse.getBody());
                });

    }


}
