package cc.vips_is.service.image.core;

import app.photofox.vipsffm.Vips;
import app.photofox.vipsffm.VipsOption;
import app.photofox.vipsffm.enums.VipsForeignTiffCompression;
import app.photofox.vipsffm.enums.VipsForeignTiffPredictor;
import cc.vips_is.service.image.config.ImageServerConfig;
import cc.vips_is.service.image.model.ImageFormat;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.EnumMap;
import java.util.Map;

@ApplicationScoped
public class VipsSaveOptionsRegistry {

    private final Map<ImageFormat, VipsOption[]> cache = new EnumMap<>(ImageFormat.class);

    @Inject
    ImageServerConfig config;

    @PostConstruct
    void init() {
        Vips.disableOperationCache();

        var proc = config.processing();

        cache.put(ImageFormat.JPG, new VipsOption[] {
                VipsOption.Int("Q", proc.jpegQuality()),
                VipsOption.Boolean("optimize_coding", true)
        });

        cache.put(ImageFormat.PNG, new VipsOption[] {
                VipsOption.Int("compression", proc.pngCompression())
        });

        cache.put(ImageFormat.WEBP, new VipsOption[] {
                VipsOption.Int("Q", proc.webpQuality())
        });

        cache.put(ImageFormat.TIF, new VipsOption[] {
                VipsOption.Enum("compression", VipsForeignTiffCompression.FOREIGN_TIFF_COMPRESSION_LZW),
                VipsOption.Enum("predictor", VipsForeignTiffPredictor.FOREIGN_TIFF_PREDICTOR_HORIZONTAL),
                VipsOption.Boolean("bigtiff", true)
        });

        cache.put(ImageFormat.GIF, new VipsOption[0]);

        cache.put(ImageFormat.JP2, new VipsOption[] {
                VipsOption.Boolean("lossless", false)
        });
    }

    public VipsOption[] getOptions(ImageFormat format) {
        return cache.getOrDefault(format, new VipsOption[0]);
    }

}
