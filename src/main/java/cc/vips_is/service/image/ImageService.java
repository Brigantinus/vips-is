package cc.vips_is.service.image;

import cc.vips_is.service.image.model.ImageRequest;
import jakarta.ws.rs.core.StreamingOutput;

public interface ImageService {
    
    /**
     * Process an image manipulation request according to IIIF Image API 3.0 specification
     * @param request The image manipulation request containing all parameters
     * @return Response with processing results
     */
    StreamingOutput processImage(ImageRequest request);

}