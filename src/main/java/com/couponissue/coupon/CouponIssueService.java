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
            // Kafka에 전달되지 않은 발급은 즉시 Redis 원상 복구합니다.
            redisRepository.compensateFailure(couponId, userId);
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

    public long stock(long couponId) {
        // 재고는 MySQL이 아니라 Redis에서 관리하므로 Repository에 위임합니다.
        return redisRepository.getStock(couponId);
    }

    public void seedStock(long couponId, long stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("stock must not be negative");
        }
        // 현재는 로컬 테스트용 설정 기능이므로 Redis 재고를 초기화합니다.
        redisRepository.seedStock(couponId, stock);
    }
}
