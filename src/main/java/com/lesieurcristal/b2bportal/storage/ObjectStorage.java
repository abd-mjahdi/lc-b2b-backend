package com.lesieurcristal.b2bportal.storage;

import java.util.Optional;

/**
 * Object-store facade. Production uses S3/MinIO. Tests may mock this.
 */
public interface ObjectStorage {

    void put(String key, byte[] content, String contentType);

    Optional<byte[]> get(String key);

    boolean exists(String key);

    void delete(String key);
}
