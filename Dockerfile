# 1단계: Gradle Wrapper로 의존성을 내려받고 실행 가능한 Spring Boot JAR를 만듭니다.
FROM gradle:8.10.2-jdk17 AS builder
WORKDIR /workspace
COPY . .
# Windows 체크아웃의 CRLF와 실행 권한 차이를 컨테이너 안에서 정규화합니다.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew bootJar --no-daemon

# 2단계: 실행에 필요한 JRE만 포함해 이미지 크기와 공격 표면을 줄입니다.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
