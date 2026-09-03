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
class KafkaFalloutIntegrationTest {
    private static final RedisTestEnvironment REDIS = RedisTestEnvironment.start();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::host);
        registry.add("spring.data.redis.port", REDIS::port);
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("spring.kafka.admin.auto-create", () -> "false");
        registry.add("spring.kafka.listener.missing-topics-fatal", () -> "false");
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:fallout-it;MODE=MySQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired CouponRedisRepository redis;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired CouponIssueConsumer consumer;

    @BeforeEach
    void reset() { redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb(); }

    @AfterAll
    void closeRedis() throws Exception { REDIS.close(); }

    @Test
    void dltRestoresStockAndRemovesIssuedUser() {
        redis.seedStock(11L, 1L);
        assertThat(redis.issueCoupon(11L, 21L)).isEqualTo(CouponIssueOutcome.SUCCESS);
        consumer.handleDlt(new CouponIssueEvent(11L, 21L, Instant.now()), new IllegalStateException("database failure"));
        assertThat(redis.getStock(11L)).isEqualTo(1L);
        assertThat(redis.isIssued(11L, 21L)).isFalse();
        assertThat(redis.getIssueStatus(11L, 21L)).isEqualTo("FAILED");
    }
}
