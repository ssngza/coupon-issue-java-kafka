# Project Structure

현재 저장소에 존재하는 파일과 디렉터리 구조입니다.

```text
coupon-system/
├── .gitignore
├── AGENTS.md
├── ARCHITECTURE.md
├── build.gradle
├── HARNESS_SPEC.md
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── PROJECT_STRUCTURE.md
├── README.md
├── gradlew
├── gradlew.bat
├── settings.gradle
├── docker/
│   ├── .env.example
│   ├── docker-compose.yml
│   └── nginx/
│       ├── html/
│       │   └── index.html
│       └── nginx.conf
├── harness/
│   └── load-test-plan.md
└── docs/
    ├── DESIGN.md
    ├── FRONTEND.md
    ├── PLANS.md
    ├── PRODUCT_SENSE.md
    ├── QUALITY_SCORE.md
    ├── RELIABILITY.md
    ├── SECURITY.md
    ├── design-docs/
    │   ├── core-beliefs.md
    │   └── index.md
    ├── exec-plans/
    │   └── tech-debt-tracker.md
    ├── generated/
    │   └── db-schema.md
    └── product-specs/
        ├── index.md
        └── new-user-onboarding.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/couponissue/
    │   │       ├── CouponIssueApplication.java
    │   │       └── coupon/
    │   │           ├── CouponIssueOutcome.java
    │   │           └── CouponRedisRepository.java
    │   └── resources/
    │       └── scripts/
    │           └── issue_coupon.lua
    └── test/
        └── java/
            └── com/couponissue/coupon/
                ├── CouponRedisRepositoryConcurrencyTest.java
                ├── CouponConsumerIntegrationTest.java
                ├── KafkaFalloutIntegrationTest.java
                └── RedisTestEnvironment.java
```

`harness/`와 `.github/workflows/`는 구현 계획에 포함되어 있지만 아직 생성되지 않았습니다.
