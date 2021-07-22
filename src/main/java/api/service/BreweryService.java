package api.service;

import api.model.Brewery;
import api.model.GetBreweryRequest;
import api.model.UpdateBreweryRequest;
import api.validation.BreweryValidator;
import couchbase.model.GetRequest;
import couchbase.model.QueryRequest;
import couchbase.model.UpdateRequest;
import io.reactivex.rxjava3.functions.Consumer;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.core.eventbus.Message;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class BreweryService extends AbstractVerticle {
    private EventBus eventBus;
    private final BreweryValidator breweryValidator;

    @Inject
    public BreweryService(BreweryValidator breweryValidator) {
        this.breweryValidator = breweryValidator;
    }



    @Override
    public void start(Promise<Void> promise) throws Exception {
        eventBus = vertx.eventBus();
        eventBus.consumer("BreweryService.getBreweries", this::getBreweries);
        eventBus.consumer("BreweryService.getBrewery", this::getBrewery);
        eventBus.consumer("BreweryService.updateBrewery", this::updateBrewery);
        super.start(promise);
    }

    public void getBreweries(Message<JsonObject> message) {
        QueryRequest queryRequest = new QueryRequest("select `beer-sample`.*, meta().id from `beer-sample` where type=\"brewery\"");
        eventBus.<List<JsonObject>>rxRequest("couchbase.query", queryRequest)
                .map(Message::body)
                .map(list -> list.stream().map(jsonObject -> jsonObject.mapTo(Brewery.class)).collect(Collectors.toList()))
                .subscribe(message::reply, handleError(message));

    }

    public void getBrewery(Message<GetBreweryRequest> message){
        GetBreweryRequest request = message.body();
        GetRequest getRequest = new GetRequest("beer-sample", request.getId());
        eventBus.<JsonObject>rxRequest("couchbase.get", getRequest)
                .map(Message::body)
                .map(result -> result.mapTo(Brewery.class))
                .subscribe(message::reply, handleError(message));

    }

    private void updateBrewery(Message<UpdateBreweryRequest> message) {
        UpdateBreweryRequest updateBreweryRequest = message.body();
        GetRequest getRequest = new GetRequest("beer-sample", updateBreweryRequest.getId());
        eventBus.<JsonObject>rxRequest("couchbase.get", getRequest)
                .map(Message::body)
                .map(result -> result.mapTo(Brewery.class))
                .map(existingDocument -> breweryValidator.validateAndMergeUpdateRequest(updateBreweryRequest, existingDocument))
                .flatMap(newBrewery -> {
                    UpdateRequest updateRequest = new UpdateRequest("beer-sample", updateBreweryRequest.getId(), JsonObject.mapFrom(newBrewery));
                    return eventBus.<JsonObject>rxRequest("couchbase.update", updateRequest)
                            .map(Message::body);
                })
                .map(result -> result.mapTo(Brewery.class))
                .subscribe(message::reply, handleError(message));
    }


    private <T> Consumer<Throwable> handleError(Message<T> message) {
        return cause -> {
            if(cause instanceof ReplyException) {
                ReplyException replyException = (ReplyException) cause;
                message.fail(replyException.failureCode(), replyException.getMessage());
            }else{
                //this may never be called as the return from event bus should always be a reply exception
                //left just in case it isn't
                message.fail(500, cause.getMessage());
            }
        };
    }
}
