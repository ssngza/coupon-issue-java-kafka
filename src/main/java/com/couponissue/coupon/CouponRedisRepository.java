package com.couponissue.coupon;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class CouponRedisRepository {

    private static final Duration PENDING_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> issueCouponScript;
    private final DefaultRedisScript<Long> compensateCouponScript;

    public CouponRedisRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.issueCouponScript = new DefaultRedisScript<>();
        this.issueCouponScript.setLocation(new ClassPathResource("scripts/issue_coupon.lua"));
        this.issueCouponScript.setResultType(Long.class);
        this.compensateCouponScript = new DefaultRedisScript<>();
        this.compensateCouponScript.setLocation(new ClassPathResource("scripts/compensate_coupon.lua"));
        this.compensateCouponScript.setResultType(Long.class);
    }

    public void seedStock(long couponId, long stock) {
        redisTemplate.opsForValue().set(stockKey(couponId), Long.toString(stock));
        redisTemplate.delete(List.of(usersKey(couponId)));
    }

    public long getStock(long couponId) {
        String value = redisTemplate.opsForValue().get(stockKey(couponId));
        return value == null ? 0L : Long.parseLong(value);
    }

    public boolean isIssued(long couponId, long userId) {
        Boolean member = redisTemplate.opsForSet().isMember(usersKey(couponId), Long.toString(userId));
        return Boolean.TRUE.equals(member);
    }

    public String getIssueStatus(long couponId, long userId) {
        return redisTemplate.opsForValue().get(issueStatusKey(couponId, userId));
    }

    public void markSuccess(long couponId, long userId) {
        // Consumer가 DB 저장을 완료한 시점에만 PENDING을 SUCCESS로 전환합니다.
        redisTemplate.opsForValue().set(issueStatusKey(couponId, userId), "SUCCESS", PENDING_TTL);
    }

    public void compensateFailure(long couponId, long userId) {
        // 중복 DLT 전달에도 이미 제거된 사용자만 재고를 복구해 이중 보상을 막습니다.
        redisTemplate.execute(
                compensateCouponScript,
                List.of(stockKey(couponId), usersKey(couponId), issueStatusKey(couponId, userId)),
                Long.toString(userId),
                Long.toString(PENDING_TTL.toSeconds())
        );
    }

    public CouponIssueOutcome issueCoupon(long couponId, long userId) {
        Long result = redisTemplate.execute(
                issueCouponScript,
                List.of(stockKey(couponId), usersKey(couponId), issueStatusKey(couponId, userId)),
                Long.toString(userId),
                Long.toString(PENDING_TTL.toSeconds())
        );

        if (Objects.equals(result, 1L)) {
            return CouponIssueOutcome.SUCCESS;
        }
        if (Objects.equals(result, -1L)) {
            return CouponIssueOutcome.DUPLICATE;
        }
        return CouponIssueOutcome.OUT_OF_STOCK;
    }

    private String stockKey(long couponId) {
        return "coupon:" + couponId + ":stock";
    }

    private String usersKey(long couponId) {
        return "coupon:" + couponId + ":users";
    }

    private String issueStatusKey(long couponId, long userId) {
        return "coupon:" + couponId + ":user:" + userId + ":issue-status";
    }
}
