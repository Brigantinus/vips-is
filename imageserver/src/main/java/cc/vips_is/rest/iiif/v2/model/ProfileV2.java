package cc.vips_is.rest.iiif.v2.model;

import lombok.Getter;

import java.util.List;

@Getter
public class ProfileV2 {

    public static final String IIIF_V2_PROFILE = "http://iiif.io/api/image/2/level2.json";

    private final List<String> formats = List.of("jpg","tif","png","gif","jp2","pdf","webp");
    private final List<String> qualities = List.of("color", "gray", "bitonal");
    private final SupportsV2[] supports = SupportsV2.values();

}
