package api.module;

import api.controller.ResponseParser;
import com.google.inject.Provides;

import javax.inject.Singleton;

public class ResponseParserModule {

    @Provides
    @Singleton
    public ResponseParser providesResponseParser() {
        return new ResponseParser();
    }
}
