package com.lesieurcristal.b2bportal.storage;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadValidatorTest {

    private final UploadValidator validator = new UploadValidator();

    @Test
    void acceptsPdfMagicBytes() {
        byte[] pdf = "%PDF-1.4 fake".getBytes();
        UploadValidator.ValidatedUpload result = validator.validate(
                new MockMultipartFile("file", "x.bin", "application/octet-stream", pdf));
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.extension()).isEqualTo("pdf");
    }

    @Test
    void rejectsUnknownBytes() {
        assertThatThrownBy(() -> validator.validate(
                new MockMultipartFile("file", "x.txt", "text/plain", "hello".getBytes())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non autorisé");
    }

    @Test
    void rejectsOversizedFile() {
        byte[] huge = new byte[(int) UploadValidator.MAX_BYTES + 1];
        huge[0] = 0x25;
        huge[1] = 0x50;
        huge[2] = 0x44;
        huge[3] = 0x46;
        assertThatThrownBy(() -> validator.validate(
                new MockMultipartFile("file", "big.pdf", "application/pdf", huge)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void emptyFileIsAbsent() {
        assertThat(validator.validateIfPresent(
                new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0])))
                .isEmpty();
    }
}
