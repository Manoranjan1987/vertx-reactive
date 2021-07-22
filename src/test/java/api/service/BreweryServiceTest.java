package api.service;

import api.codec.GenericModelCodec;
import api.model.Brewery;
import api.model.Geography;
import api.model.GetBreweryRequest;
import api.validation.BreweryValidator;
import couchbase.model.GetRequest;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.eventbus.MessageConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

@ExtendWith(VertxExtension.class)
public class BreweryServiceTest {
    static Vertx vertx = Vertx.vertx();

    @BeforeAll
    static void setup() throws Throwable {
        VertxTestContext vertxTestContext = new VertxTestContext();
        BreweryService breweryService = new BreweryService(new BreweryValidator());
        vertx.rxDeployVerticle(breweryService)
                .subscribe(deploymentId -> vertxTestContext.completeNow(), vertxTestContext::failNow);
        vertx.eventBus().getDelegate()
                .registerDefaultCodec(Geography.class, new GenericModelCodec<>(Geography.class))
                .registerDefaultCodec(Brewery.class, new GenericModelCodec<>(Brewery.class))
                .registerDefaultCodec(GetBreweryRequest.class, new GenericModelCodec<>(GetBreweryRequest.class))
                .registerDefaultCodec(GetRequest.class, new GenericModelCodec<>(GetRequest.class));

        vertxTestContext.awaitCompletion(1, TimeUnit.SECONDS);
        if (vertxTestContext.failed()) {
            throw vertxTestContext.causeOfFailure();
        }
    }


    @Test
    void getBrewerySuccessfully() throws Throwable {
        VertxTestContext vertxTestContext = new VertxTestContext();
        GetBreweryRequest getBreweryRequest = new GetBreweryRequest("21st_amendment_brewery_cafe");

        MessageConsumer<GetRequest> consumer = vertx.eventBus()
                .consumer("couchbase.get", message -> message.reply(getBreweryJson()));

        vertx.eventBus().rxRequest("BreweryService.getBrewery", getBreweryRequest)
                .subscribe(message -> {
                    vertxTestContext.verify(() -> {
                        Assertions.assertEquals(Brewery.class, message.body().getClass());
                        Assertions.assertEquals("21st Amendment Brewery Cafe", ((Brewery) message.body()).getName());
                    });
                    consumer.unregister();
                    vertxTestContext.completeNow();
                }, vertxTestContext::failNow);

        vertxTestContext.awaitCompletion(1, TimeUnit.SECONDS);
        if (vertxTestContext.failed()) {
            throw vertxTestContext.causeOfFailure();
        }
    }

    @Test
    void getBreweryFailureDueToCouchbaseOutage() throws Throwable {
        VertxTestContext vertxTestContext = new VertxTestContext();
        GetBreweryRequest getBreweryRequest = new GetBreweryRequest("21st_amendment_brewery_cafe");

        MessageConsumer<GetRequest> consumer = vertx.eventBus()
                .consumer("couchbase.get", message -> message.fail(500, "couchbase is broken"));

        vertx.eventBus().rxRequest("BreweryService.getBrewery", getBreweryRequest)
                .subscribe(result -> vertxTestContext.failNow("got document successfully"),
                        cause -> {
                            vertxTestContext.verify(() -> {
                                Assertions.assertEquals(ReplyException.class, cause.getClass());
                                Assertions.assertEquals(500, ((ReplyException) cause).failureCode());
                            });
                            consumer.unregister();
                            vertxTestContext.completeNow();
                        });

        vertxTestContext.awaitCompletion(1, TimeUnit.SECONDS);
        if (vertxTestContext.failed()) {
            throw vertxTestContext.causeOfFailure();
        }
    }

    @Test
    void getBreweryFailureDueToDocumentNotFound() throws Throwable {
        VertxTestContext vertxTestContext = new VertxTestContext();
        GetBreweryRequest getBreweryRequest = new GetBreweryRequest("21st_amendment_brewery_cafe");

        MessageConsumer<GetRequest> consumer = vertx.eventBus()
                .consumer("couchbase.get", message -> message.fail(404, "Doucment Not Found"));

        vertx.eventBus().rxRequest("BreweryService.getBrewery", getBreweryRequest)
                .subscribe(result -> vertxTestContext.failNow("got document successfully"),
                        cause -> {
                            vertxTestContext.verify(() -> {
                                Assertions.assertEquals(ReplyException.class, cause.getClass());
                                Assertions.assertEquals(404, ((ReplyException) cause).failureCode());
                            });
                            consumer.unregister();
                            vertxTestContext.completeNow();
                        });

        vertxTestContext.awaitCompletion(1, TimeUnit.SECONDS);
        if (vertxTestContext.failed()) {
            throw vertxTestContext.causeOfFailure();
        }
    }

    private JsonObject getBreweryJson() {
        return new JsonObject("{\"geo\":{\"accuracy\":\"ROOFTOP\",\"lon\":-122.393,\"lat\":37.7825},\"country\":\"United States\",\"website\":\"http://www.21st-amendment.com/\",\"address\":[\"563 Second Street\"],\"code\":\"94107\",\"city\":\"San Francisco\",\"phone\":\"1-415-369-0900\",\"name\":\"21st Amendment Brewery Cafe\",\"description\":\"The 21st Amendment Brewery offers a variety of award winning house made brews and American grilled cuisine in a comfortable loft like setting. Join us before and after Giants baseball games in our outdoor beer garden. A great location for functions and parties in our semi-private Brewers Loft. See you soon at the 21A!\",\"state\":\"California\",\"type\":\"brewery\",\"updated\":\"2010-10-24 13:54:07\"}");

    }
}
