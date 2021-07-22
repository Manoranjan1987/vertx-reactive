package api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateBreweryRequest {
    private String id;
    private Brewery brewery;
}
