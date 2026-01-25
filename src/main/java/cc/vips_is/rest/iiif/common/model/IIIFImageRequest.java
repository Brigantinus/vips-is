package cc.vips_is.rest.iiif.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.PathParam;
import lombok.Data;

@Data
public class IIIFImageRequest {
    
    @JsonProperty("identifier")
    @PathParam("identifier")
    private String identifier;
    
    @JsonProperty("region")
    @PathParam("region")
    private String region;
    
    @JsonProperty("size")
    @PathParam("size")
    private String size;
    
    @JsonProperty("rotation")
    @PathParam("rotation")
    private String rotation;
    
    @JsonProperty("quality")
    @PathParam("quality")
    private String quality;
    
    @JsonProperty("format")
    @PathParam("format")
    private String format;

}