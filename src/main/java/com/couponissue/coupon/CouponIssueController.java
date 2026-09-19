package com.couponissue.coupon;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
public class CouponIssueController {
    private final CouponIssueService service;
    private final AdminTokenValidator adminTokenValidator;

    public CouponIssueController(CouponIssueService service, AdminTokenValidator adminTokenValidator) {
        this.service = service;
        this.adminTokenValidator = adminTokenValidator;
    }

    @GetMapping("/{couponId}/stock")
    public ResponseEntity<Map<String, Long>> stock(@PathVariable long couponId) {
        // 부하 테스트와 운영 화면이 동일한 Redis 원장 값을 확인하도록 노출합니다.
        return ResponseEntity.ok(Map.of("stock", service.stock(couponId)));
    }

    @PutMapping("/{couponId}/stock")
    public ResponseEntity<?> seedStock(
            @PathVariable long couponId,
            @RequestBody StockRequest request,
            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken
    ) {
        if (!adminTokenValidator.isValid(adminToken)) {
            // 재고 변경은 운영 데이터에 영향을 주므로 토큰이 없거나 틀리면 즉시 차단합니다.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "유효한 관리자 인증 토큰이 필요합니다."));
        }
        service.seedStock(couponId, request.stock());
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
    public record StockRequest(long stock) { }
}
