package couchbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@DataObject(generateConverter = true)
public class ComplexModel {

    private String id;
    private List<String> items;
    private String user;

    public ComplexModel(JsonObject jsonObject) {
        ComplexModel tempModel = jsonObject.mapTo(ComplexModel.class);
        this.id = tempModel.getId();
        this.items = tempModel.getItems();
        this.user = tempModel.getUser();
    }

    public JsonObject toJson(){
        return JsonObject.mapFrom(this);
    }

}
