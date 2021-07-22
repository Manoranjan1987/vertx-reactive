package couchbase;

import api.codec.GenericModelCodec;
import api.model.Brewery;
import api.model.Geography;
import api.model.GetBreweryRequest;
import api.service.BreweryService;
import api.validation.BreweryValidator;
import com.couchbase.client.core.error.DocumentNotFoundException;
import couchbase.model.GetRequest;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
public class CouchbaseVerticleTest {
    static Vertx vertx = Vertx.vertx();
    static CouchbaseRepository couchbaseRepository;

    @BeforeAll
    static void setup() throws Throwable {
        couchbaseRepository = mock(CouchbaseRepository.class);
        VertxTestContext vertxTestContext = new VertxTestContext();
        CouchbaseVerticle couchbaseVerticle = new CouchbaseVerticle(couchbaseRepository);
        vertx.rxDeployVerticle(couchbaseVerticle)
                .subscribe(deploymentId -> vertxTestContext.completeNow(), vertxTestContext::failNow);
        vertx.eventBus().getDelegate()
                .registerDefaultCodec(GetRequest.class, new GenericModelCodec<>(GetRequest.class));

        vertxTestContext.awaitCompletion(1, TimeUnit.SECONDS);
        if (vertxTestContext.failed()) {
            throw vertxTestContext.causeOfFailure();
        }
    }

    @Test
    void getBrewerySuccessfully() throws Throwable {
        VertxTestContext vertxTestContext = new VertxTestContext();
        GetRequest getRequest = new GetRequest("beer-sample", "21st_amendment_brewery_cafe");

        when(couchbaseRepository.get(getRequest)).thenReturn(Single.just(getBreweryJson()));

        vertx.eventBus().rxRequest("couchbase.get", getRequest)
                .subscribe(message -> {
                    vertxTestContext.verify(() -> {
                        Assertions.assertEquals(JsonObject.class, message.body().getClass());
                        Assertions.assertEquals("21st Amendment Brewery Cafe", ((JsonObject) message.body()).getString("name"));
                    });
                    vertxTestContext.completeNow();
                }, vertxTestContext::failNow);

        vertxTestContext.awaitCompletion(1, TimeUnit.SECONDS);
        if (vertxTestContext.failed()) {
            throw vertxTestContext.causeOfFailure();
        }
    }


    @Test
    void getBreweryFailed() throws Throwable {
        VertxTestContext vertxTestContext = new VertxTestContext();
        GetRequest getRequest = new GetRequest("beer-sample", "21st_amendment_brewery_cafe");

        when(couchbaseRepository.get(getRequest)).thenReturn(Single.error(new DocumentNotFoundException(null)));

        vertx.eventBus().rxRequest("couchbase.get", getRequest)
                .subscribe(result -> vertxTestContext.failNow("got document successfully"),
                        cause -> {
                            vertxTestContext.verify(() -> {
                                Assertions.assertEquals(ReplyException.class, cause.getClass());
                                Assertions.assertEquals(404, ((ReplyException) cause).failureCode());
                            });
                            vertxTestContext.completeNow();
                        });

        vertxTestContext.awaitCompletion(1, TimeUnit.SECONDS);
        if (vertxTestContext.failed()) {
            throw vertxTestContext.causeOfFailure();
        }
    }


    private com.couchbase.client.java.json.JsonObject getBreweryJson() {
        return com.couchbase.client.java.json.JsonObject.fromJson("{\"geo\":{\"accuracy\":\"ROOFTOP\",\"lon\":-122.393,\"lat\":37.7825},\"country\":\"United States\",\"website\":\"http://www.21st-amendment.com/\",\"address\":[\"563 Second Street\"],\"code\":\"94107\",\"city\":\"San Francisco\",\"phone\":\"1-415-369-0900\",\"name\":\"21st Amendment Brewery Cafe\",\"description\":\"The 21st Amendment Brewery offers a variety of award winning house made brews and American grilled cuisine in a comfortable loft like setting. Join us before and after Giants baseball games in our outdoor beer garden. A great location for functions and parties in our semi-private Brewers Loft. See you soon at the 21A!\",\"state\":\"California\",\"type\":\"brewery\",\"updated\":\"2010-10-24 13:54:07\"}");

    }
}
