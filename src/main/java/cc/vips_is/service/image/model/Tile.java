package cc.vips_is.service.image.model;

import java.util.List;

public record Tile(int width, int height, List<Integer> scaleFactors) {

}
