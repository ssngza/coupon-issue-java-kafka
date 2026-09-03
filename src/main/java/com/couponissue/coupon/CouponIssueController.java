package com.couponissue.coupon;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
public class CouponIssueController {
    private final CouponIssueService service;
    public CouponIssueController(CouponIssueService service) { this.service = service; }

    @GetMapping("/{couponId}/stock")
    public ResponseEntity<Map<String, Long>> stock(@PathVariable long couponId) {
        // 부하 테스트와 운영 화면이 동일한 Redis 원장 값을 확인하도록 노출합니다.
        return ResponseEntity.ok(Map.of("stock", service.stock(couponId)));
    }

    @PostMapping("/{couponId}/issue")
    public ResponseEntity<Map<String, String>> issue(@PathVariable long couponId, @RequestBody IssueRequest request) {
        return ResponseEntity.ok(Map.of("status", service.issue(couponId, request.userId())));
    }

    @GetMapping("/{couponId}/issues/{userId}/status")
    public ResponseEntity<Map<String, String>> status(@PathVariable long couponId, @PathVariable long userId) {
        return ResponseEntity.ok(Map.of("status", service.status(couponId, userId)));
    }

    public record IssueRequest(long userId) { }
}
