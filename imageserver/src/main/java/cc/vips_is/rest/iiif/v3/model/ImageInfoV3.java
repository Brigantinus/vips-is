package cc.vips_is.rest.iiif.v3.model;

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

@JsonPropertyOrder({ "@context", "id", "type", "protocol", "profile", "width", "height",
        "maxWidth", "maxHeight", "maxArea", "sizes", "tiles", "preferredFormats", "extraFormats",
        "extraFeatures", "extraQualities" })
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"id"})
public class ImageInfoV3 {

    @JsonProperty("@context")
    private String context;

    private String id;
    private final String type = "ImageService3";
    private final String protocol = "http://iiif.io/api/image";
    private final String profile = "level2";
    private int width;
    private int height;
    private int maxWidth;
    private int maxHeight;
    private long maxArea;
    private List<Size> sizes;
    private List<Tile> tiles;
    private final String[] preferredFormats = new String[] {"jpg", "png", "gif", "webp"};
    private final String[] extraFormats = new String[] {"tif", "jp2"};
    private final SupportsV3[] extraFeatures = SupportsV3.values();
    private final String[] extraQualities = new String[] {"bitonal"};

}
