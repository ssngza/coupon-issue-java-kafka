package com.couponissue.coupon;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponHistoryRepository extends JpaRepository<CouponHistory, Long> {
    boolean existsByCouponIdAndUserId(long couponId, long userId);
}
