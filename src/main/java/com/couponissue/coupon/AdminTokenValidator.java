package com.couponissue.coupon;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminTokenValidator {
    private final String configuredToken;

    public AdminTokenValidator(@Value("${coupon.admin.token:}") String configuredToken) {
        this.configuredToken = configuredToken;
    }

    public boolean isValid(String requestToken) {
        if (configuredToken.isBlank() || requestToken == null || requestToken.isBlank()) {
            return false;
        }
        // 토큰 길이가 달라도 동일한 비교 함수를 사용해 단순한 문자열 비교를 피합니다.
        return MessageDigest.isEqual(
                configuredToken.getBytes(StandardCharsets.UTF_8),
                requestToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
