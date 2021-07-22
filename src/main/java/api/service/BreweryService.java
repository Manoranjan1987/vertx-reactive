package api.service;

import api.model.Brewery;
import api.model.BreweryRequest;
import couchbase.model.GetRequest;
import couchbase.model.QueryRequest;
import io.reactivex.rxjava3.functions.Consumer;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.ReplyException;
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
        eventBus.consumer("Breweryservice.getBrewery", this::getBrewery);
        super.start(promise);
    }

    public void getBreweries(Message<JsonObject> message) {
        QueryRequest queryRequest = new QueryRequest("select `beer-sample`.*, meta().id from `beer-sample` where type=\"brewery\"");
        eventBus.<List<JsonObject>>rxRequest("couchbase.query", queryRequest)
                .map(Message::body)
                .map(list -> list.stream().map(jsonObject -> jsonObject.mapTo(Brewery.class)).collect(Collectors.toList()))
                .subscribe(message::reply, handleError(message));

    }

    public void getBrewery(Message<BreweryRequest> message){
        BreweryRequest request = message.body();
        GetRequest getRequest = new GetRequest("beer-sample", request.getId());
        eventBus.rxRequest("couchbase.get", getRequest)
                .map(Message::body)
                .subscribe(message::reply, handleError(message));

    }

    private <T> Consumer<Throwable> handleError(Message<T> message) {
        return cause -> {
            if(cause instanceof ReplyException) {
                ReplyException replyException = (ReplyException) cause;
                message.fail(replyException.failureCode(), replyException.getMessage());
            }else{
                message.fail(500, cause.getMessage());
            }
        };
    }
}
