package cc.vips_is.service.image.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class ImageInfo {
    private String id;
    private int width;
    private int height;
    private int tileWith;
    private int tileHeight;
    private List<Size> sizes;
    private List<Tile> tiles;
    private Integer maxWidth;
    private Integer maxHeight;
}