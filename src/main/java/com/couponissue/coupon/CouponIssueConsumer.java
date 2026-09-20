package com.couponissue.coupon;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CouponIssueConsumer {
    private static final Logger log = LoggerFactory.getLogger(CouponIssueConsumer.class);
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
        // 운영자가 DLT 원인과 대상 사용자를 찾을 수 있도록 보상 전후 정보를 구조화해 남깁니다.
        try {
            // 최종 실패 이벤트는 Redis 자원을 되돌린 뒤 FAILED 상태로 종료합니다.
            redisRepository.compensateFailure(event.couponId(), event.userId());
            log.error(
                    "coupon_issue_dlt couponId={} userId={} exceptionType={} exceptionMessage={} compensation=SUCCESS",
                    event.couponId(), event.userId(), exception.getClass().getName(), exception.getMessage()
            );
        } catch (RuntimeException compensationException) {
            // 보상 자체가 실패하면 운영자가 재처리해야 하므로 원인과 보상 실패를 함께 기록합니다.
            log.error(
                    "coupon_issue_dlt couponId={} userId={} exceptionType={} exceptionMessage={} compensation=FAILED",
                    event.couponId(), event.userId(), exception.getClass().getName(), exception.getMessage(), compensationException
            );
            throw compensationException;
        }
    }
}
