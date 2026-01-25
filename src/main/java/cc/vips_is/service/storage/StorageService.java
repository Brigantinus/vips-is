package cc.vips_is.service.storage;

import java.nio.file.Path;

public interface StorageService {

    Path resolve(String identifier);
}
