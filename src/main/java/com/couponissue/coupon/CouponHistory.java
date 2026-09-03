package com.couponissue.coupon;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "coupon_history", uniqueConstraints = @UniqueConstraint(name = "uk_coupon_user", columnNames = {"coupon_id", "user_id"}))
public class CouponHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private long couponId;
    private long userId;
    private String status;

    protected CouponHistory() { }

    public CouponHistory(long couponId, long userId) {
        this.couponId = couponId;
        this.userId = userId;
        this.status = "SUCCESS";
    }

    public long getCouponId() { return couponId; }
    public long getUserId() { return userId; }
}
