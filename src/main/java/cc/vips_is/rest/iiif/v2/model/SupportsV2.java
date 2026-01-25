package cc.vips_is.rest.iiif.v2.model;

public enum SupportsV2 {

    /**
     * The base URI of the service will redirect to the image information document.
     */
    baseUriRedirect,

    /**
     * The canonical image URI HTTP link header is provided on image responses.
     */
    canonicalLinkHeader,

    /**
     * The CORS HTTP header is provided on all responses.
     */
    cors,

    /**
     * The JSON-LD media type is provided when JSON-LD is requested.
     */
    jsonldMediaType,

    /**
     * The image may be rotated around the vertical axis, resulting in a left-to-right mirroring of the content.
     */
    mirroring,

    /**
     * The profile HTTP link header is provided on image responses.
     */
    profileLinkHeader,

    /**
     * Regions of images may be requested by percentage.
     */
    regionByPct,

    /**
     * Regions of images may be requested by pixel dimensions.
     */
    regionByPx,

    /**
     * A square region where the width and height are equal to the shorter dimension of the complete image content.
     */
    regionSquare,

    /**
     * Rotation of images may be requested by degrees other than multiples of 90.
     */
    rotationArbitrary,

    /**
     * Rotation of images may be requested by degrees in multiples of 90.
     */
    rotationBy90s,

    /**
     * Size of images may be requested larger than the "full" size. See warning.
     */
    sizeAboveFull,

    /**
     * Size of images may be requested in the form "!w,h".
     */
    sizeByConfinedWh,

    /**
     * Size of images may be requested in the form "w,h", including sizes that would distort the image.
     */
    sizeByDistortedWh,

    /**
     * Size of images may be requested in the form ",h".
     */
    sizeByH,

    /**
     * Size of images may be requested in the form "pct:n".
     */
    sizeByPct,

    /**
     * Size of images may be requested in the form "w,".
     */
    sizeByW,

    /**
     * Size of images may be requested in the form "w,h" where the supplied w and h preserve the aspect ratio.
     */
    sizeByWh
}
