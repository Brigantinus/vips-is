package cc.vips_is.rest.iiif.v2.model;

import cc.vips_is.service.image.model.Size;
import cc.vips_is.service.image.model.Tile;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

import static cc.vips_is.rest.iiif.v2.model.ProfileV2.IIIF_V2_PROFILE;

@JsonPropertyOrder({ "@context", "id", "type", "protocol", "profile", "width", "height", "sizes", "tiles" })
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"id"})
public class ImageInfoV2 {

    @JsonProperty("@context")
    private final String context = "http://iiif.io/api/image/2/context.json";

    @JsonProperty("@id")
    private String id;

    @JsonProperty("@type")
    private final String type = "iiif:ImageProfile";

    private final String protocol = "http://iiif.io/api/image";
    private final List<Object> profile = List.of(IIIF_V2_PROFILE, new ProfileV2());

    private int width;
    private int height;
    private List<Size> sizes;
    private List<Tile> tiles;
    private List<String> attribution;
    private List<String> license;
    private List<Object> service;
}

