package com.couponissue.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CouponConsumerIntegrationTest {
    private static final RedisTestEnvironment REDIS = RedisTestEnvironment.start();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::host);
        registry.add("spring.data.redis.port", REDIS::port);
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("spring.kafka.admin.auto-create", () -> "false");
        registry.add("spring.kafka.listener.missing-topics-fatal", () -> "false");
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:consumer-it;MODE=MySQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired CouponRedisRepository redis;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired CouponHistoryRepository history;
    @Autowired CouponIssueConsumer consumer;

    @BeforeEach
    void reset() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        history.deleteAll();
    }

    @AfterAll
    void closeRedis() throws Exception { REDIS.close(); }

    @Test
    void eventIsPersistedAndRedisBecomesSuccess() {
        redis.seedStock(10L, 1L);
        assertThat(redis.issueCoupon(10L, 20L)).isEqualTo(CouponIssueOutcome.SUCCESS);
        consumer.consume(new CouponIssueEvent(10L, 20L, Instant.now()));
        assertThat(history.existsByCouponIdAndUserId(10L, 20L)).isTrue();
        assertThat(redis.getIssueStatus(10L, 20L)).isEqualTo("SUCCESS");
    }
}
