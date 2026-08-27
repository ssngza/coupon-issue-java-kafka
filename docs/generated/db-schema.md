# Generated DB Schema

최종 데이터베이스 DDL 및 테이블 명세입니다.

`coupon.total_quantity`는 기준 수량을 보관한다. 선착순 재고 검증과 차감은 MySQL이 아니라 Redis에서 수행하며, `coupon_history`의 Unique Constraint는 Redis Set과 함께 발급 멱등성을 보강한다.

```sql
CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    total_quantity INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS coupon_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL, -- 'SUCCESS', 'FAILED'
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_coupon_user UNIQUE (coupon_id, user_id)
);
```
