package com.couponissue.coupon;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
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
}
