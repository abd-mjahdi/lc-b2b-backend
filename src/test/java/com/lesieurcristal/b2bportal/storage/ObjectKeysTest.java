package com.lesieurcristal.b2bportal.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectKeysTest {

    @Test
    void storedKeysAreUsable() {
        assertThat(ObjectKeys.isStoredObjectKey("invoices/CUST0001/900010001.pdf")).isTrue();
        assertThat(ObjectKeys.isStoredObjectKey("disputes/CUST0001/1/abc.pdf")).isTrue();
        assertThat(ObjectKeys.isStoredObjectKey("claims/CUST0001/2/xyz.png")).isTrue();
    }

    @Test
    void legacyAndExternalPathsAreNotStoredKeys() {
        assertThat(ObjectKeys.isStoredObjectKey(null)).isFalse();
        assertThat(ObjectKeys.isStoredObjectKey("")).isFalse();
        assertThat(ObjectKeys.isStoredObjectKey("/documents/invoices/900010001.pdf")).isFalse();
        assertThat(ObjectKeys.isStoredObjectKey("documents/invoices/900010001.pdf")).isFalse();
        assertThat(ObjectKeys.isStoredObjectKey("/uploads/photo.png")).isFalse();
        assertThat(ObjectKeys.isStoredObjectKey("https://example.com/file.pdf")).isFalse();
    }
}
