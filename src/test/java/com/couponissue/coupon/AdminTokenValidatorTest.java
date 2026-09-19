package com.couponissue.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class AdminTokenValidatorTest {
    @Test
    void acceptsOnlyTheConfiguredToken() {
        AdminTokenValidator validator = new AdminTokenValidator("secret-token");

        assertThat(validator.isValid("secret-token")).isTrue();
        assertThat(validator.isValid("wrong-token")).isFalse();
        assertThat(validator.isValid(null)).isFalse();
    }

    @Test
    void rejectsEveryTokenWhenNoAdminTokenIsConfigured() {
        assertThat(new AdminTokenValidator("").isValid("secret-token")).isFalse();
    }
}
