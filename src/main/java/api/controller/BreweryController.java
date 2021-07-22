package api.controller;

import api.model.Brewery;
import api.model.ErrorResponse;
import api.model.GetBreweryRequest;
import api.model.UpdateBreweryRequest;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.RoutingContext;
import io.vertx.rxjava3.ext.web.handler.BodyHandler;

import javax.inject.Inject;
import java.util.List;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

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
        router.route().handler(BodyHandler.create());
        router.get("/breweries").handler(this::getBreweries);
        router.get("/breweries/:id").handler(this::getBrewery);
        router.put("/breweries/:id").handler(this::updateBrewery);
        return router;
    }

    private void getBreweries(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response().getDelegate();
        eventBus.<List<Brewery>>rxRequest("BreweryService.getBreweries", null)
                .subscribe(results -> {
                    response.putHeader(CONTENT_TYPE, "application/json").end(responseParser.parseObject(results.body()));
                }, cause -> {
                    ErrorResponse errorResponse = responseParser.parseError(cause);
                    response.setStatusCode(errorResponse.getStatusCode()).end(errorResponse.getBody());
                });
    }

    private void getBrewery(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response().getDelegate();
        String id = routingContext.pathParam("id");
        GetBreweryRequest getBreweryRequest = new GetBreweryRequest(id);
        eventBus.rxRequest("BreweryService.getBrewery", getBreweryRequest)
                .subscribe(results -> {
                    response.putHeader(CONTENT_TYPE, "application/json").end(responseParser.parseObject(results.body()));
                }, cause -> {
                    ErrorResponse errorResponse = responseParser.parseError(cause);
                    response.setStatusCode(errorResponse.getStatusCode()).end(errorResponse.getBody());
                });
    }

    private void updateBrewery(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response().getDelegate();
        String id = routingContext.pathParam("id");
        Brewery brewery = routingContext.getBodyAsJson().mapTo(Brewery.class);
        UpdateBreweryRequest updateBreweryRequest = new UpdateBreweryRequest(id, brewery);
        eventBus.rxRequest("BreweryService.updateBrewery", updateBreweryRequest)
                .subscribe(results -> {
                    response.putHeader(CONTENT_TYPE, "application/json").end(responseParser.parseObject(results.body()));
                }, cause -> {
                    ErrorResponse errorResponse = responseParser.parseError(cause);
                    response.setStatusCode(errorResponse.getStatusCode()).end(errorResponse.getBody());
                });
    }
}
