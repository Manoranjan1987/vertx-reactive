package api.validation;

import api.model.Brewery;
import api.model.UpdateBreweryRequest;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;

public class BreweryValidator {
    public Brewery validateAndMergeUpdateRequest(UpdateBreweryRequest updateBreweryRequest, Brewery existingBrewery) {
        Brewery newBrewery = updateBreweryRequest.getBrewery();
        if(!existingBrewery.getName().equalsIgnoreCase(newBrewery.getName())){
            throw new ReplyException(ReplyFailure.ERROR, 400, "Name can not be changed");
        }

        //TODO: more validation

        return newBrewery;
    }
}
