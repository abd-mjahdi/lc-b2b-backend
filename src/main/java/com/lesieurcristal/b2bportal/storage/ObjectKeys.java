package com.lesieurcristal.b2bportal.storage;

import java.util.Locale;
import java.util.UUID;

/**
 * Server-side object keys. Never built from a client-supplied path.
 */
public final class ObjectKeys {

    private ObjectKeys() {
    }

    public static String invoice(String customerNumber, String invoiceNumber) {
        return "invoices/" + sanitizeSegment(customerNumber) + "/" + sanitizeSegment(invoiceNumber) + ".pdf";
    }

    public static String dispute(String customerNumber, long disputeId, String extension) {
        return "disputes/"
                + sanitizeSegment(customerNumber)
                + "/"
                + disputeId
                + "/"
                + UUID.randomUUID()
                + "."
                + sanitizeExtension(extension);
    }

    public static String claim(String customerNumber, long claimId, String extension) {
        return "claims/"
                + sanitizeSegment(customerNumber)
                + "/"
                + claimId
                + "/"
                + UUID.randomUUID()
                + "."
                + sanitizeExtension(extension);
    }

    public static boolean isLegacyFilesystemPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return path.startsWith("/documents/") || path.startsWith("documents/");
    }

    /** True when {@code path} is an object key we can GET from MinIO/S3. */
    public static boolean isStoredObjectKey(String path) {
        if (path == null || path.isBlank() || isLegacyFilesystemPath(path)) {
            return false;
        }
        String value = path.trim();
        return !value.startsWith("/uploads/")
                && !value.startsWith("http://")
                && !value.startsWith("https://")
                && !value.startsWith("/");
    }

    public static String extensionOf(String key) {
        if (key == null) {
            return "";
        }
        int slash = key.lastIndexOf('/');
        String name = slash >= 0 ? key.substring(slash + 1) : key;
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return sanitizeExtension(name.substring(dot + 1));
    }

    public static String contentTypeOf(String key) {
        return switch (extensionOf(key)) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> "application/octet-stream";
        };
    }

    public static String downloadFilename(String prefix, long id, String key) {
        String ext = extensionOf(key);
        if (ext.isBlank()) {
            return prefix + "-" + id;
        }
        return prefix + "-" + id + "." + ext;
    }

    static String sanitizeSegment(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Segment de clé de stockage manquant");
        }
        String cleaned = raw.replace('\\', '/');
        if (cleaned.contains("/") || cleaned.contains("..")) {
            throw new IllegalArgumentException("Segment de clé de stockage invalide");
        }
        return cleaned.trim();
    }

    private static String sanitizeExtension(String raw) {
        if (raw == null || raw.isBlank()) {
            return "bin";
        }
        String ext = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return switch (ext) {
            case "pdf", "png", "jpg", "jpeg" -> ext.equals("jpeg") ? "jpg" : ext;
            default -> "bin";
        };
    }
}
