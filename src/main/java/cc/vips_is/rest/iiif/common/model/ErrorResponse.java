package cc.vips_is.rest.iiif.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class ErrorResponse {
    private final UUID id;
    private final String error;
    private final String details;
    private final String timestamp;
    private final String path;

    public ErrorResponse(String error, String details, String path) {
        this.id = UUID.randomUUID();
        this.error = error;
        this.details = details;
        this.path = path;
        this.timestamp = Instant.now().toString();
    }
}
