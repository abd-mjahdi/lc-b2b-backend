package com.lesieurcristal.b2bportal.storage;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;

@Component
public class UploadValidator {

    public static final long MAX_BYTES = 5L * 1024 * 1024;

    public Optional<ValidatedUpload> validateIfPresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(validate(file));
    }

    public ValidatedUpload validate(MultipartFile file) {
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Fichier trop volumineux (maximum 5 Mo)");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le fichier uploadé", e);
        }
        DetectedType detected = detect(bytes);
        if (detected == null) {
            throw new IllegalArgumentException(
                    "Type de fichier non autorisé. Formats acceptés : PDF, JPEG, PNG.");
        }
        return new ValidatedUpload(bytes, detected.contentType(), detected.extension());
    }

    private static DetectedType detect(byte[] bytes) {
        if (bytes.length >= 4
                && bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) {
            return new DetectedType("application/pdf", "pdf");
        }
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return new DetectedType("image/jpeg", "jpg");
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return new DetectedType("image/png", "png");
        }
        return null;
    }

    public record ValidatedUpload(byte[] bytes, String contentType, String extension) {
    }

    private record DetectedType(String contentType, String extension) {
    }
}
