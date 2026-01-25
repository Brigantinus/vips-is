package cc.vips_is.service.image.model;

import lombok.NonNull;

public record ImageRequest(@NonNull String identifier,
                           @NonNull RegionInfo regionInfo,
                           @NonNull SizeInfo sizeInfo,
                           @NonNull QualityMode qualityMode,
                           @NonNull RotationInfo rotationInfo,
                           @NonNull ImageFormat imageFormat) {

}
