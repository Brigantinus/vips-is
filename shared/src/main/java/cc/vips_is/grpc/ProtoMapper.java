package cc.vips_is.grpc;

import cc.vips_is.service.cache.DerivedKey;
import cc.vips_is.service.image.model.*;

public final class ProtoMapper {

    private ProtoMapper() {}

    // ── DerivedKey ↔ DerivedRequest ──────────────────────────────────────────

    public static DerivedKey toDerivedKey(DerivedRequest req) {
        return new DerivedKey(
                toRegionInfo(req.getRegion()),
                toSizeInfo(req.getSize()),
                toRotationInfo(req.getRotation()),
                QualityMode.valueOf(req.getQuality()),
                ImageFormat.valueOf(req.getFormat())
        );
    }

    public static DerivedRequest toDerivedRequest(String identifier, DerivedKey key) {
        return DerivedRequest.newBuilder()
                .setIdentifier(identifier)
                .setRegion(toRegionParam(key.region()))
                .setSize(toSizeParam(key.size()))
                .setRotation(toRotationParam(key.rotation()))
                .setQuality(key.quality().name())
                .setFormat(key.format().name())
                .build();
    }

    // ── RegionInfo ──────────────────────────────────────────────────────────

    private static RegionInfo toRegionInfo(RegionParam p) {
        return new RegionInfo(toRegionType(p.getType()), p.getX(), p.getY(), p.getWidth(), p.getHeight());
    }

    private static RegionParam toRegionParam(RegionInfo r) {
        return RegionParam.newBuilder()
                .setType(toProtoRegionType(r.type()))
                .setX(r.x()).setY(r.y()).setWidth(r.width()).setHeight(r.height())
                .build();
    }

    private static RegionType toRegionType(ProtoRegionType t) {
        return switch (t) {
            case FULL       -> RegionType.FULL;
            case SQUARE     -> RegionType.SQUARE;
            case PIXELS     -> RegionType.PIXELS;
            case PERCENTAGE -> RegionType.PERCENTAGE;
            default         -> throw new IllegalArgumentException("Unknown region type: " + t);
        };
    }

    private static ProtoRegionType toProtoRegionType(RegionType t) {
        return switch (t) {
            case FULL       -> ProtoRegionType.FULL;
            case SQUARE     -> ProtoRegionType.SQUARE;
            case PIXELS     -> ProtoRegionType.PIXELS;
            case PERCENTAGE -> ProtoRegionType.PERCENTAGE;
        };
    }

    // ── SizeInfo ─────────────────────────────────────────────────────────────

    private static SizeInfo toSizeInfo(SizeParam p) {
        return new SizeInfo(
                toSizeType(p.getType()),
                p.getWidth() == 0 ? null : p.getWidth(),
                p.getHeight() == 0 ? null : p.getHeight(),
                p.getPercentage() == 0.0 ? null : p.getPercentage(),
                p.getUpscalingAllowed(),
                p.getMaintainAspectRatio()
        );
    }

    private static SizeParam toSizeParam(SizeInfo s) {
        SizeParam.Builder b = SizeParam.newBuilder()
                .setType(toProtoSizeType(s.getType()))
                .setUpscalingAllowed(s.isUpscalingAllowed())
                .setMaintainAspectRatio(s.isMaintainAspectRatio());
        if (s.getWidth()      != null) b.setWidth(s.getWidth());
        if (s.getHeight()     != null) b.setHeight(s.getHeight());
        if (s.getPercentage() != null) b.setPercentage(s.getPercentage());
        return b.build();
    }

    private static SizeType toSizeType(ProtoSizeType t) {
        return switch (t) {
            case SIZE_MAX          -> SizeType.MAX;
            case SIZE_WIDTH_ONLY   -> SizeType.WIDTH_ONLY;
            case SIZE_HEIGHT_ONLY  -> SizeType.HEIGHT_ONLY;
            case SIZE_WIDTH_HEIGHT -> SizeType.WIDTH_HEIGHT;
            case SIZE_PERCENTAGE   -> SizeType.PERCENTAGE;
            default                -> throw new IllegalArgumentException("Unknown size type: " + t);
        };
    }

    private static ProtoSizeType toProtoSizeType(SizeType t) {
        return switch (t) {
            case MAX          -> ProtoSizeType.SIZE_MAX;
            case WIDTH_ONLY   -> ProtoSizeType.SIZE_WIDTH_ONLY;
            case HEIGHT_ONLY  -> ProtoSizeType.SIZE_HEIGHT_ONLY;
            case WIDTH_HEIGHT -> ProtoSizeType.SIZE_WIDTH_HEIGHT;
            case PERCENTAGE   -> ProtoSizeType.SIZE_PERCENTAGE;
        };
    }

    // ── RotationInfo ─────────────────────────────────────────────────────────

    private static RotationInfo toRotationInfo(RotationParam p) {
        return new RotationInfo(p.getMirrored(), p.getRotation());
    }

    private static RotationParam toRotationParam(RotationInfo r) {
        return RotationParam.newBuilder()
                .setMirrored(r.mirrored())
                .setRotation(r.rotation())
                .build();
    }

    // ── ImageInfo ↔ CachedImageInfo ──────────────────────────────────────────

    public static ImageInfo toImageInfo(CachedImageInfo proto) {
        ImageInfo info = new ImageInfo();
        info.setId(proto.getId());
        info.setWidth(proto.getWidth());
        info.setHeight(proto.getHeight());
        info.setTileWith(proto.getTileWidth());
        info.setTileHeight(proto.getTileHeight());
        info.setMaxWidth(proto.getMaxWidth());
        info.setMaxHeight(proto.getMaxHeight());
        info.setSizes(proto.getSizesList().stream()
                .map(s -> new Size(s.getWidth(), s.getHeight()))
                .toList());
        info.setTiles(proto.getTilesList().stream()
                .map(t -> new Tile(t.getWidth(), t.getHeight(), t.getScaleFactorsList()))
                .toList());
        return info;
    }

    public static CachedImageInfo toCachedImageInfo(ImageInfo info) {
        CachedImageInfo.Builder b = CachedImageInfo.newBuilder()
                .setId(info.getId() != null ? info.getId() : "")
                .setWidth(info.getWidth())
                .setHeight(info.getHeight())
                .setTileWidth(info.getTileWith())
                .setTileHeight(info.getTileHeight())
                .setMaxWidth(info.getMaxWidth() != null ? info.getMaxWidth() : 0)
                .setMaxHeight(info.getMaxHeight() != null ? info.getMaxHeight() : 0);

        if (info.getSizes() != null) {
            info.getSizes().forEach(s ->
                    b.addSizes(ImageSize.newBuilder().setWidth(s.width()).setHeight(s.height()).build()));
        }
        if (info.getTiles() != null) {
            info.getTiles().forEach(t -> {
                TileSpec.Builder tb = TileSpec.newBuilder()
                        .setWidth(t.width()).setHeight(t.height());
                t.scaleFactors().forEach(tb::addScaleFactors);
                b.addTiles(tb.build());
            });
        }
        return b.build();
    }
}
