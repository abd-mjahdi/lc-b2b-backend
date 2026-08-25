package com.lesieurcristal.b2bportal.storage;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class FileDownloadResponses {

    private FileDownloadResponses() {
    }

    public static ResponseEntity<byte[]> attachment(byte[] content, String contentType, String filename) {
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream");
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        String safeName = filename == null ? "document" : filename.replace("\"", "");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + "\"")
                .contentType(mediaType)
                .contentLength(content.length)
                .body(content);
    }
}
