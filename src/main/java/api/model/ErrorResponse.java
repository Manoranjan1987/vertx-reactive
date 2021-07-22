package api.model;

import io.vertx.core.buffer.Buffer;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private Integer statusCode;
    private Buffer body;
}
