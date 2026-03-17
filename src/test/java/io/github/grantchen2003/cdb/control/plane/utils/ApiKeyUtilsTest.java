package io.github.grantchen2003.cdb.control.plane.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyUtilsTest {

    @Test
    void generate_producesUniqueKeys() {
        assertThat(ApiKeyUtils.generate()).isNotEqualTo(ApiKeyUtils.generate());
    }

    @Test
    void hash_isSameForSameInput() {
        final String raw = ApiKeyUtils.generate();
        assertThat(ApiKeyUtils.hash(raw)).isEqualTo(ApiKeyUtils.hash(raw));
    }

    @Test
    void hash_isDifferentForDifferentInput() {
        assertThat(ApiKeyUtils.hash(ApiKeyUtils.generate()))
                .isNotEqualTo(ApiKeyUtils.hash(ApiKeyUtils.generate()));
    }

    @Test
    void hash_doesNotReturnRawKey() {
        final String raw = ApiKeyUtils.generate();
        assertThat(ApiKeyUtils.hash(raw)).isNotEqualTo(raw);
    }

    @Test
    void verify_returnsTrueForMatchingKey() {
        final String raw = ApiKeyUtils.generate();
        final String hash = ApiKeyUtils.hash(raw);
        assertThat(ApiKeyUtils.verify(raw, hash)).isTrue();
    }

    @Test
    void verify_returnsFalseForWrongKey() {
        final String hash = ApiKeyUtils.hash(ApiKeyUtils.generate());
        assertThat(ApiKeyUtils.verify(ApiKeyUtils.generate(), hash)).isFalse();
    }

    @Test
    void verify_returnsFalseForTamperedHash() {
        final String raw = ApiKeyUtils.generate();
        assertThat(ApiKeyUtils.verify(raw, "tampered")).isFalse();
    }
}
