package com.couponissue.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CouponRedisRepositoryConcurrencyTest {

    private static final RedisTestEnvironment REDIS = RedisTestEnvironment.start();

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::host);
        registry.add("spring.data.redis.port", REDIS::port);
        // Redis 단위 테스트에서는 Kafka Consumer를 띄우지 않아 외부 브로커 의존성을 제거합니다.
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:redis-test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private CouponRedisRepository couponRedisRepository;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @AfterAll
    void tearDown() throws Exception {
        REDIS.close();
    }

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    void shouldIssueExactly100CouponsUnder1000ConcurrentRequests() throws Exception {
        long couponId = 1L;
        couponRedisRepository.seedStock(couponId, 100L);

        int requestCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CompletionService<CouponIssueOutcome> completionService = new java.util.concurrent.ExecutorCompletionService<>(executorService);
        CountDownLatch readyLatch = new CountDownLatch(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            List<Callable<CouponIssueOutcome>> tasks = IntStream.rangeClosed(1, requestCount)
                    .mapToObj(userId -> (Callable<CouponIssueOutcome>) () -> {
                        readyLatch.countDown();
                        startLatch.await();
                        return couponRedisRepository.issueCoupon(couponId, userId);
                    })
                    .collect(Collectors.toList());

            for (Callable<CouponIssueOutcome> task : tasks) {
                completionService.submit(task);
            }

            awaitLatch(readyLatch);
            startLatch.countDown();

            List<CouponIssueOutcome> outcomes = new ArrayList<>(requestCount);
            for (int i = 0; i < requestCount; i++) {
                Future<CouponIssueOutcome> future = completionService.take();
                outcomes.add(future.get());
            }

            long successCount = outcomes.stream().filter(outcome -> outcome == CouponIssueOutcome.SUCCESS).count();
            long duplicateCount = outcomes.stream().filter(outcome -> outcome == CouponIssueOutcome.DUPLICATE).count();
            long outOfStockCount = outcomes.stream().filter(outcome -> outcome == CouponIssueOutcome.OUT_OF_STOCK).count();

            assertThat(successCount).isEqualTo(100L);
            assertThat(duplicateCount).isZero();
            assertThat(outOfStockCount).isEqualTo(900L);
            assertThat(couponRedisRepository.getStock(couponId)).isZero();

            Set<String> issuedUsers = redisTemplate.opsForSet().members("coupon:" + couponId + ":users");
            assertThat(issuedUsers).hasSize(100);

            List<Long> successfulUsers = issuedUsers.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            assertThat(successfulUsers).hasSize(100);
            assertThat(successfulUsers).allSatisfy(user -> {
                assertThat(couponRedisRepository.isIssued(couponId, user)).isTrue();
                assertThat(couponRedisRepository.getIssueStatus(couponId, user)).isEqualTo("PENDING");
            });
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void sameUserShouldNotReceiveCouponTwice() {
        long couponId = 2L;
        long userId = 42L;
        couponRedisRepository.seedStock(couponId, 1L);

        CouponIssueOutcome first = couponRedisRepository.issueCoupon(couponId, userId);
        CouponIssueOutcome second = couponRedisRepository.issueCoupon(couponId, userId);

        assertThat(first).isEqualTo(CouponIssueOutcome.SUCCESS);
        assertThat(second).isEqualTo(CouponIssueOutcome.DUPLICATE);
        assertThat(couponRedisRepository.getStock(couponId)).isZero();
        assertThat(couponRedisRepository.getIssueStatus(couponId, userId)).isEqualTo("PENDING");
    }

    private static void awaitLatch(CountDownLatch latch) throws InterruptedException {
        while (!latch.await(100, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            // wait until all workers are ready
        }
    }
}
