package com.couponissue.coupon;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class CouponIssueConsumer {
    private final CouponHistoryRepository historyRepository;
    private final CouponRedisRepository redisRepository;

    public CouponIssueConsumer(CouponHistoryRepository historyRepository, CouponRedisRepository redisRepository) {
        this.historyRepository = historyRepository;
        this.redisRepository = redisRepository;
    }

    @RetryableTopic(
            attempts = "2",
            backoff = @Backoff(delay = 1000),
            retryTopicSuffix = ".retry",
            dltTopicSuffix = ".dlt"
    )
    @KafkaListener(topics = CouponEventTopics.REQUEST, groupId = "coupon-issue-persistence")
    public void consume(CouponIssueEvent event) {
        // 재전달된 이벤트도 DB Unique 제약과 사전 확인으로 한 번만 저장합니다.
        if (!historyRepository.existsByCouponIdAndUserId(event.couponId(), event.userId())) {
            try {
                historyRepository.saveAndFlush(new CouponHistory(event.couponId(), event.userId()));
            } catch (DataIntegrityViolationException ignored) {
                // 동시 Consumer가 먼저 저장한 경우도 멱등 성공으로 취급합니다.
            }
        }
        // DB 영속화가 끝난 뒤에만 사용자에게 최종 성공 상태를 공개합니다.
        redisRepository.markSuccess(event.couponId(), event.userId());
    }

    @org.springframework.kafka.annotation.DltHandler
    public void handleDlt(CouponIssueEvent event, Exception exception) {
        // 최종 실패 이벤트는 Redis 자원을 되돌린 뒤 FAILED 상태로 종료합니다.
        redisRepository.compensateFailure(event.couponId(), event.userId());
    }
}
