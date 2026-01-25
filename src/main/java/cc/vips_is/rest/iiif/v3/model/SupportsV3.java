package cc.vips_is.rest.iiif.v3.model;

public enum SupportsV3 {
    // The canonical image URI HTTP link header is provided on image responses.
    canonicalLinkHeader,

    // The CORS HTTP headers are provided on all responses.
    cors,

    // The JSON-LD media type is provided when requested.
    jsonldMediaType,

    // The image may be rotated around the vertical axis, resulting in a left-to-right mirroring of the content.
    mirroring,

    // The profile HTTP link header is provided on image responses.
    profileLinkHeader,

    // Regions of the full image may be requested by percentage.
    regionByPct,

    // Regions of the full image may be requested by pixel dimensions.
    regionByPx,

    // A square region may be requested, where the width and height are equal to the shorter dimension of the full image.
    regionSquare,

    // Image rotation may be requested using values other than multiples of 90 degrees.
    rotationArbitrary,

    // Image rotation may be requested in multiples of 90 degrees.
    rotationBy90s,

    // Image size may be requested in the form !w,h.
    sizeByConfinedWh,

    // Image size may be requested in the form ,h.
    sizeByH,

    // Images size may be requested in the form pct:n.
    sizeByPct,

    // Image size may be requested in the form w,.
    sizeByW,

    // Image size may be requested in the form w,h.
    sizeByWh,

    // Image sizes prefixed with ^ may be requested.
    sizeUpscaling;
}