package com.couponissue.coupon;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class CouponIssueService {
    private static final Duration MAX_WAIT = Duration.ofSeconds(5);
    private final CouponRedisRepository redisRepository;
    private final CouponIssueProducer producer;

    public CouponIssueService(CouponRedisRepository redisRepository, CouponIssueProducer producer) {
        this.redisRepository = redisRepository;
        this.producer = producer;
    }

    public String issue(long couponId, long userId) {
        CouponIssueOutcome outcome = redisRepository.issueCoupon(couponId, userId);
        if (outcome != CouponIssueOutcome.SUCCESS) return outcome.name();
        try {
            producer.publish(new CouponIssueEvent(couponId, userId, Instant.now())).join();
        } catch (RuntimeException exception) {
            return "FAILED";
        }
        // Consumer가 빠르게 처리하면 최종 결과를 반환하고, 아니면 PENDING으로 전환합니다.
        long deadline = System.nanoTime() + MAX_WAIT.toNanos();
        while (System.nanoTime() < deadline) {
            String status = redisRepository.getIssueStatus(couponId, userId);
            if ("SUCCESS".equals(status) || "FAILED".equals(status)) return status;
            Thread.onSpinWait();
        }
        return "PENDING";
    }

    public String status(long couponId, long userId) {
        String status = redisRepository.getIssueStatus(couponId, userId);
        return status == null ? "NOT_FOUND" : status;
    }
}
