package cc.vips_is.service.image;

import cc.vips_is.service.image.model.ImageInfo;

public interface InfoService {

    /**
     * Get image information (info.json) for a given identifier
     * @param identifier The image identifier
     * @return Response with image information
     */
    ImageInfo getImageInfo(String identifier);

    boolean exists(String identifier);
}
