package cc.vips_is.service.image.core;

import cc.vips_is.service.image.exceptions.InvalidParameterException;
import cc.vips_is.service.image.model.Region;
import cc.vips_is.service.image.model.RegionInfo;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class RegionCalculator {

    public Region calculateRegion(RegionInfo regionInfo, int imageWidth, int imageHeight) {
        log.debug("calculateRegion: regionInfo: {}, imageWidth: {}, imageHeight: {}",
                regionInfo, imageWidth, imageHeight);

        Region region = switch (regionInfo.type()) {
            case FULL -> regionFull(imageWidth, imageHeight);
            case SQUARE -> regionSquare(imageWidth, imageHeight);
            case PIXELS -> regionPixel(regionInfo, imageWidth, imageHeight);
            case PERCENTAGE -> regionPercentage(regionInfo, imageWidth, imageHeight);
        };

        log.debug("calculateRegion: region={}", region);
        return region;
    }

    Region regionFull(int imageWidth, int imageHeight) {
        return new Region(0, 0, imageWidth, imageHeight);
    }

    Region regionSquare(int imageWidth, int imageHeight) {
        int size = Math.min(imageWidth, imageHeight);
        int x = (imageWidth - size) / 2;
        int y = (imageHeight - size) / 2;
        return new Region(x, y, size, size);
    }

    Region regionPixel(RegionInfo regionInfo, int imageWidth, int imageHeight) {
        int x = regionInfo.x();
        int y = regionInfo.y();

        if (x >= imageWidth || y >= imageHeight) {
            throw new InvalidParameterException("Region x,y coordinates are outside image bounds.");
        }

        int width = Math.min(regionInfo.width(), imageWidth - x);
        int height = Math.min(regionInfo.height(), imageHeight - y);

        return new Region(x, y, width, height);
    }

    Region regionPercentage(RegionInfo regionInfo, int imageWidth, int imageHeight) {
        int x = (int) Math.round(regionInfo.x() * imageWidth / 100.0);
        int y = (int) Math.round(regionInfo.y() * imageHeight / 100.0);
        int width = (int) Math.round(regionInfo.width() * imageWidth / 100.0);
        int height = (int) Math.round(regionInfo.height() * imageHeight / 100.0);

        return new Region(x, y, width, height);
    }
}
