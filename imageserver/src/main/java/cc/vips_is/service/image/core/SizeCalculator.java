package cc.vips_is.service.image.core;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import cc.vips_is.service.image.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class SizeCalculator {

    public Size calculateSize(ImageRequest request, int imageWidth, int imageHeight, int maxWidth, int maxHeight, long maxArea) {
        log.debug("calculateSize: imageWidth={}, imageHeight={}", imageWidth, imageHeight);

        Size size = calculateSourceRegion(request.regionInfo(), imageWidth, imageHeight);
        size = calculateScaledSizes(request.sizeInfo(), size, maxWidth, maxHeight, maxArea);

        log.debug("calculateSize: size={}", size);

        return size;
    }

    Size calculateSourceRegion(RegionInfo region, int imageWidth, int imageHeight) {
        log.debug("calculateSourceRegion: regionInfo={}, imageWidth={}", region, imageWidth);

        return switch (region.type()) {
            case FULL -> new Size(imageWidth, imageHeight);
            case SQUARE -> {
                int minSide = Math.min(imageWidth, imageHeight);
                yield new Size(minSide, minSide);
            }
            case PIXELS -> new Size((int) region.width(), (int) region.height());
            case PERCENTAGE -> new Size(
                    (int) Math.round(imageWidth * region.width() / 100.0),
                    (int) Math.round(imageHeight * region.height() / 100.0)
            );
        };
    }

    Size calculateScaledSizes(SizeInfo sizeInfo, Size source, int maxWidth, int maxHeight, long maxArea) {
        log.debug("calculateScaledSizes: sizeInfo={}, source={}", sizeInfo, source);

        Size target = switch (sizeInfo.getType()) {
            case MAX          -> scaleMax(source, maxWidth, maxHeight, maxArea, sizeInfo.isUpscalingAllowed());
            case WIDTH_ONLY   -> scaleByWidth(source, sizeInfo.getWidth());
            case HEIGHT_ONLY  -> scaleByHeight(source, sizeInfo.getHeight());
            case PERCENTAGE   -> scaleByFactor(source, sizeInfo.getPercentage() / 100.0);
            case WIDTH_HEIGHT -> scaleByWidthAndHeight(source, sizeInfo);
        };

        preventUpscaling(sizeInfo, target, source);

        return new Size(Math.max(1, target.width()), Math.max(1, target.height()));
    }

    private Size scaleMax(Size source, int maxWidth, int maxHeight, long maxArea, boolean allowUpscale) {
        if (!allowUpscale) return source;

        double scale = Math.min(
                (double) maxWidth / source.width(),
                (double) maxHeight / source.height()
        );

        double areaScale = Math.sqrt((double) maxArea / (source.width() * source.height()));
        scale = Math.min(scale, areaScale);

        return scaleByFactor(source, scale);
    }

    private Size scaleByWidth(Size source, int targetW) {
        double scale = (double) targetW / source.width();
        return new Size(targetW, (int) Math.round(source.height() * scale));
    }

    private Size scaleByHeight(Size source, int targetH) {
        double scale = (double) targetH / source.height();
        return new Size((int) Math.round(source.width() * scale), targetH);
    }

    private Size scaleByFactor(Size source, double scale) {
        return new Size(
                (int) Math.round(source.width() * scale),
                (int) Math.round(source.height() * scale)
        );
    }

    private Size scaleByWidthAndHeight(Size source, SizeInfo size) {
        if (!size.isMaintainAspectRatio()) {
            return new Size(size.getWidth(), size.getHeight());
        }
        double scale = Math.min(
                (double) size.getWidth() / source.width(),
                (double) size.getHeight() / source.height()
        );
        return scaleByFactor(source, scale);
    }

    private void preventUpscaling(SizeInfo sizeInfo, Size target, Size source) {
        if (!sizeInfo.isUpscalingAllowed()) {
            if (target.width() > source.width() || target.height() > source.height()) {
                throw new InvalidParameterException("Requested size requires upscaling.");
            }
        }
    }

}
