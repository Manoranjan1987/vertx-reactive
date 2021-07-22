package api.module;

import com.couchbase.client.java.ReactiveCluster;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import couchbase.CouchbaseRepository;

import javax.inject.Singleton;

public class CouchbaseRepositoryModule extends AbstractModule {

    @Provides
    @Singleton
    public CouchbaseRepository providesCouchbaseRepository(ReactiveCluster reactiveCluster) {
        return new CouchbaseRepository(reactiveCluster);
    }

}
