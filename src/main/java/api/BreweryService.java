package api;

import api.model.Brewery;
import couchbase.model.QueryRequest;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.core.eventbus.Message;

import java.util.List;
import java.util.stream.Collectors;

public class BreweryService extends AbstractVerticle {
    private EventBus eventBus;

    @Override
    public void start(Promise<Void> promise) throws Exception {
        eventBus = vertx.eventBus();
        eventBus.consumer("Breweryservice.getBreweries", this::getBreweries);
        super.start(promise);
    }

    public void getBreweries(Message<JsonObject> message) {
        QueryRequest queryRequest = new QueryRequest("select `beer-sample`.* from `beer-sample` where type=\"brewery\"");
        eventBus.<List<JsonObject>>request("couchbase.query", queryRequest)
                .map(Message::body)
                .map(list -> list.stream().map(jsonObject -> jsonObject.mapTo(Brewery.class)).collect(Collectors.toList()))
                .subscribe(message::reply, System.out::println);

    }
}
