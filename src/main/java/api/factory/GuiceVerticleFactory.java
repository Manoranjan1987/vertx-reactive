package api.factory;

import com.google.inject.Injector;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.spi.VerticleFactory;

import java.util.concurrent.Callable;

public class GuiceVerticleFactory implements VerticleFactory {
    private final Injector injector;

    public GuiceVerticleFactory(Injector injector) {
        this.injector = injector;
    }

    @Override
    public String prefix() {
        return "guice";
    }

    @Override
    public int order() {
        return -1;
    }

    @Override
    public void createVerticle(String verticleName, ClassLoader classLoader, Promise<Callable<Verticle>> promise) {
        String className = VerticleFactory.removePrefix(verticleName);
        try {
            AbstractVerticle abstractVerticle = (AbstractVerticle) injector.getInstance(classLoader.loadClass(className));
            promise.complete(() -> abstractVerticle);
        } catch (ClassNotFoundException e) {
            promise.fail(e);
        }
    }
}
