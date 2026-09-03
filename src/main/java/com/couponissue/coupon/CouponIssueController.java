package com.couponissue.coupon;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
public class CouponIssueController {
    private final CouponIssueService service;
    public CouponIssueController(CouponIssueService service) { this.service = service; }

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
