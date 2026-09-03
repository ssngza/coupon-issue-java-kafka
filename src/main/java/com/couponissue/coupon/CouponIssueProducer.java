package com.couponissue.coupon;

import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class CouponIssueProducer {
    private final KafkaTemplate<String, CouponIssueEvent> kafkaTemplate;

    public CouponIssueProducer(KafkaTemplate<String, CouponIssueEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<?> publish(CouponIssueEvent event) {
        // couponId를 Kafka key로 사용해 같은 쿠폰 이벤트의 파티션 순서를 유지합니다.
        return kafkaTemplate.send(CouponEventTopics.REQUEST, Long.toString(event.couponId()), event);
    }
}
