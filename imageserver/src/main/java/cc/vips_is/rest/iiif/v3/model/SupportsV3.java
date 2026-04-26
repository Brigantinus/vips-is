package cc.vips_is.rest.iiif.v3.model;

public enum SupportsV3 {
    // The JSON-LD media type is provided when requested.
    jsonldMediaType,

    // The image may be rotated around the vertical axis, resulting in a left-to-right mirroring of the content.
    mirroring,

    // Image rotation may be requested using values other than multiples of 90 degrees.
    rotationArbitrary,

    // Image sizes prefixed with ^ may be requested.
    sizeUpscaling;
}