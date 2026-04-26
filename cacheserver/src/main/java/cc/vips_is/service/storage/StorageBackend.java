package cc.vips_is.service.storage;

public interface StorageBackend {

    byte[] load(String identifier);

    boolean exists(String identifier);
}
