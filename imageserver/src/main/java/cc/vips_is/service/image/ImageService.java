package cc.vips_is.service.image;

import cc.vips_is.service.image.model.ImageInfo;
import cc.vips_is.service.image.model.ImageRequest;
import jakarta.ws.rs.core.StreamingOutput;

public interface ImageService {

    ImageInfo getImageInfo(String identifier);

    boolean exists(String identifier);

    StreamingOutput processImage(ImageRequest request);

}