package com.couponissue.coupon;

import java.time.Instant;

// Kafka를 통해 Producer와 Consumer가 공유하는 최소 발급 이벤트입니다.
public record CouponIssueEvent(long couponId, long userId, Instant issuedAt) {
}
