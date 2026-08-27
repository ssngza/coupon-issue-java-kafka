# Generated DB Schema

최종 데이터베이스 DDL 및 테이블 명세입니다.

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
