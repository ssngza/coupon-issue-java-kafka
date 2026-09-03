package com.couponissue.coupon;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class CouponIssueConsumerTest {
    @Test
    void persistsEventAndMarksRedisSuccess() {
        CouponHistoryRepository history = mock(CouponHistoryRepository.class);
        CouponRedisRepository redis = mock(CouponRedisRepository.class);
        when(history.existsByCouponIdAndUserId(7L, 11L)).thenReturn(false);
        CouponIssueConsumer consumer = new CouponIssueConsumer(history, redis);

        consumer.consume(new CouponIssueEvent(7L, 11L, Instant.now()));

        verify(history).saveAndFlush(any(CouponHistory.class));
        verify(redis).markSuccess(7L, 11L);
    }

    @Test
    void duplicateEventDoesNotInsertAgain() {
        CouponHistoryRepository history = mock(CouponHistoryRepository.class);
        CouponRedisRepository redis = mock(CouponRedisRepository.class);
        when(history.existsByCouponIdAndUserId(7L, 11L)).thenReturn(true);
        CouponIssueConsumer consumer = new CouponIssueConsumer(history, redis);

        consumer.consume(new CouponIssueEvent(7L, 11L, Instant.now()));

        verify(history, never()).saveAndFlush(any(CouponHistory.class));
        verify(redis).markSuccess(7L, 11L);
    }

    @Test
    void dltEventCompensatesRedisAndMarksFailure() {
        CouponHistoryRepository history = mock(CouponHistoryRepository.class);
        CouponRedisRepository redis = mock(CouponRedisRepository.class);
        CouponIssueConsumer consumer = new CouponIssueConsumer(history, redis);

        consumer.handleDlt(new CouponIssueEvent(7L, 11L, Instant.now()), new IllegalStateException("db down"));

        verify(redis).compensateFailure(7L, 11L);
        verify(redis, never()).markSuccess(7L, 11L);
    }

    @Test
    void producerFailureCompensatesRedis() {
        CouponRedisRepository redis = mock(CouponRedisRepository.class);
        CouponIssueProducer producer = mock(CouponIssueProducer.class);
        when(redis.issueCoupon(7L, 11L)).thenReturn(CouponIssueOutcome.SUCCESS);
        when(producer.publish(any())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));
        CouponIssueService service = new CouponIssueService(redis, producer);

        org.assertj.core.api.Assertions.assertThat(service.issue(7L, 11L)).isEqualTo("FAILED");
        verify(redis).compensateFailure(7L, 11L);
    }
}
