package cc.vips_is.service.cache;

import cc.vips_is.service.image.model.ImageFormat;
import cc.vips_is.service.image.model.QualityMode;
import cc.vips_is.service.image.model.RegionInfo;
import cc.vips_is.service.image.model.RotationInfo;
import cc.vips_is.service.image.model.SizeInfo;

public record DerivedKey(
        RegionInfo region,
        SizeInfo size,
        RotationInfo rotation,
        QualityMode quality,
        ImageFormat format
) {}
