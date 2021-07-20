package api.module;

import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.ReactiveCluster;
import com.google.inject.AbstractModule;

public class ReactiveClusterModule extends AbstractModule {
    @Override
    protected void configure() {
        ClusterOptions clusterOptions = ClusterOptions.clusterOptions("test", "testing");
        ReactiveCluster reactiveCluster = ReactiveCluster.connect("localhost", clusterOptions);
        bind(ReactiveCluster.class).toInstance(reactiveCluster);
    }
}
