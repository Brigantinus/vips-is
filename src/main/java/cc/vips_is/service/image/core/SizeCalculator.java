package cc.vips_is.service.image.core;

import cc.vips_is.service.image.model.*;
import cc.vips_is.service.image.model.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class SizeCalculator {

    static Size calculateSize(ImageRequest request, int imageWidth, int imageHeight, int maxWidth, int maxHeight, long maxArea) {
        log.debug("calculateSize: imageWidth={}, imageHeight={}", imageWidth, imageHeight);

        Size size = calculateSourceRegion(request.regionInfo(), imageWidth, imageHeight);
        size = calculateScaledSizes(request.sizeInfo(), size, maxWidth, maxHeight, maxArea);
        size = calculateRotationBoundingBox(size, request.rotationInfo());

        log.debug("calculateSize: size={}", size);

        return size;
    }

    static Size calculateSourceRegion(RegionInfo region, int imageWidth, int imageHeight) {
        log.debug("calculateSourceRegion: regionInfo={}, imageWidth={}", region, imageWidth);

        return switch (region.type()) {
            case FULL -> new Size(imageWidth, imageHeight);
            case SQUARE -> {
                int minSide = Math.min(imageWidth, imageHeight);
                yield new Size(minSide, minSide);
            }
            case PIXELS -> new Size(region.width(), region.height());
            case PERCENTAGE -> new Size(
                    (int) Math.round(imageWidth * region.width() / 100.0),
                    (int) Math.round(imageHeight * region.height() / 100.0)
            );
        };
    }

    static Size calculateScaledSizes(SizeInfo size, Size source, int maxWidth, int maxHeight, long maxArea) {
        log.debug("calculateScaledSizes: size={}, source={}", size, source);
        int targetW, targetH;

        switch (size.getType()) {
            case MAX -> {
                targetW = source.width();
                targetH = source.height();

                if (size.isUpscalingAllowed()) {
                    double scaleW = (double) maxWidth / targetW;
                    double scaleH = (double) maxHeight / targetH;
                    double scale = Math.min(scaleW, scaleH);

                    double areaScale = Math.sqrt((double) maxArea / (targetW * targetH));
                    scale = Math.min(scale, areaScale);

                    targetW = (int) Math.floor(targetW * scale);
                    targetH = (int) Math.floor(targetH * scale);
                }
            }
            case WIDTH_ONLY -> {
                targetW = size.getWidth();
                targetH = (int) Math.round((double) source.height() * targetW / source.width());
            }
            case HEIGHT_ONLY -> {
                targetH = size.getHeight();
                targetW = (int) Math.round((double) source.width() * targetH / source.height());
            }
            case PERCENTAGE -> {
                double scale = size.getPercentage() / 100.0;
                targetW = (int) Math.round(source.width() * scale);
                targetH = (int) Math.round(source.height() * scale);
            }
            case WIDTH_HEIGHT -> {
                if (size.isMaintainAspectRatio()) {
                    // "Best fit" logic (!w,h)
                    double scale = Math.min(
                            (double) size.getWidth() / source.width(),
                            (double) size.getHeight() / source.height()
                    );
                    targetW = (int) Math.round(source.width() * scale);
                    targetH = (int) Math.round(source.height() * scale);
                } else {
                    // Forced sizes (w,h)
                    targetW = size.getWidth();
                    targetH = size.getHeight();
                }
            }
            default -> throw new IllegalArgumentException("Unsupported SizeType: " + size.getType());
        }

        if (!size.isUpscalingAllowed()) {
            if (targetW > source.width() || targetH > source.height()) {
                double k = Math.min(1.0, Math.min(
                        (double) source.width() / targetW,
                        (double) source.height() / targetH
                ));
                targetW = (int) Math.round(targetW * k);
                targetH = (int) Math.round(targetH * k);
            }
        }

        return new Size(Math.max(1, targetW), Math.max(1, targetH));
    }

    static Size calculateRotationBoundingBox(Size current, RotationInfo rotation) {
        log.debug("calculateRotationBoundingBox: current={}, rotation={}", current, rotation);

        if (rotation.rotation() == 0 || rotation.rotation() == 180) return current;
        if (rotation.rotation() == 90 || rotation.rotation() == 270)
            return new Size(current.height(), current.width());

        double rad = Math.toRadians(rotation.rotation());
        double cos = Math.abs(Math.cos(rad));
        double sin = Math.abs(Math.sin(rad));

        int newW = (int) Math.round(current.width() * cos + current.height() * sin);
        int newH = (int) Math.round(current.width() * sin + current.height() * cos);

        return new Size(newW, newH);
    }
}
