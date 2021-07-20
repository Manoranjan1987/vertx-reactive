package api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Brewery {
    private List<String> address;
    private String city;
    private String code;
    private String country;
    private String description;
    private Geography geo;
    private String id;
    private String name;
    private String phone;
    private String state;
    private String type;
    private String updated;
    private String website;
}
