# Redis Pub/Sub 테스트 애플리케이션

Spring Boot와 Redis를 사용한 실시간 메시지 발행/구독(Pub/Sub) 시스템 테스트 도구입니다.

## 🚀 기능

- **실시간 메시지 발행**: Redis 채널에 메시지를 발행
- **자동 구독**: 메시지 발행 시 해당 채널을 자동으로 구독
- **구독 취소**: 특정 채널의 구독을 취소
- **웹 UI**: 타임리프 기반의 사용자 친화적인 테스트 인터페이스
- **REST API**: 프로그래밍 방식으로 접근 가능한 API 제공

## 📋 사전 요구사항

1. **Java 21** 이상
2. **Redis 서버** (localhost:6379에서 실행 중)
3. **Gradle** (또는 내장된 gradlew 사용)

## 🛠️ 설치 및 실행

### 1. Redis 서버 시작
```bash
# Windows (Redis 설치 후)
redis-server

# macOS (Homebrew 사용)
brew services start redis

# Docker 사용
docker run -d -p 6379:6379 redis:latest
```

### 2. 애플리케이션 실행
```bash
# Gradle Wrapper 사용 (권장)
./gradlew bootRun

# 또는 IDE에서 SpringbootPubSubApplication.java 실행
```

### 3. 웹 브라우저에서 접속
```
http://localhost:8080
```

## 🎯 사용 방법

### 웹 UI 사용

1. **메시지 발송**:
   - 채널명 입력 (예: `chat-room-1`)
   - 발신자 이름 입력
   - 방 ID 입력
   - 메시지 내용 입력
   - "메시지 발송" 버튼 클릭

2. **구독 취소**:
   - 취소할 채널명 입력
   - "구독 취소" 버튼 클릭

3. **로그 확인**:
   - 애플리케이션 콘솔에서 실시간 로그 확인
   - 메시지 발송/수신 상태 모니터링

### REST API 사용

#### 메시지 발송
```bash
curl -X POST "http://localhost:8080/redis/pubsub/send?channel=test-channel" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "안녕하세요!",
    "sender": "사용자1",
    "roomId": "room-123"
  }'
```

#### 구독 취소
```bash
curl -X POST "http://localhost:8080/redis/pubsub/cancle?channel=test-channel"
```

## 📁 프로젝트 구조

```
src/main/java/org/example/springbootpubsub/
├── SpringbootPubSubApplication.java          # 메인 애플리케이션
├── domain/
│   ├── controller/
│   │   ├── RedisPubSubController.java        # REST API 컨트롤러
│   │   └── WebController.java                # 웹 페이지 컨트롤러
│   ├── service/
│   │   └── RedisPubService.java              # Pub/Sub 비즈니스 로직
│   └── dto/
│       └── MessageDto.java                   # 메시지 데이터 전송 객체
├── redis/
│   ├── pub/
│   │   └── RedisPublisher.java               # Redis 메시지 발행자
│   └── sub/
│       └── RedisSubscribeListener.java       # Redis 메시지 구독자
└── global/
    └── redisConfig/
        └── RedisConfig.java                  # Redis 설정
```

## ⚙️ 설정

### application.yml
```yaml
server:
  port: 8080

spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: pk2258  # Redis 비밀번호 (필요시 수정)
```

## 🔍 동작 원리

1. **메시지 발행**: 
   - 사용자가 웹 UI 또는 API를 통해 메시지 전송
   - `RedisPublisher`가 지정된 채널에 메시지 발행
   - 동시에 해당 채널을 구독 시작

2. **메시지 구독**:
   - `RedisSubscribeListener`가 구독 중인 채널의 메시지 수신
   - 수신된 메시지를 JSON에서 Java 객체로 변환
   - 콘솔에 로그 출력

3. **구독 관리**:
   - `RedisMessageListenerContainer`가 구독자 관리
   - 채널별로 구독 추가/제거 가능

## 🧪 테스트 시나리오

### 시나리오 1: 단일 채널 테스트
1. 채널 `test-1`에 메시지 발송
2. 콘솔에서 메시지 수신 확인
3. 구독 취소 후 메시지 발송
4. 메시지가 수신되지 않음을 확인

### 시나리오 2: 다중 채널 테스트
1. 여러 채널(`chat-1`, `chat-2`, `notification`)에 메시지 발송
2. 각 채널별로 메시지 수신 확인
3. 특정 채널만 구독 취소
4. 나머지 채널의 메시지만 수신됨을 확인

## 🐛 문제 해결

### Redis 연결 오류
- Redis 서버가 실행 중인지 확인
- `application.yml`의 Redis 설정 확인
- 방화벽 설정 확인

### 메시지가 수신되지 않는 경우
- 채널명이 정확한지 확인
- 구독이 활성화되어 있는지 확인
- Redis 서버 로그 확인

## 📝 라이센스

이 프로젝트는 학습 목적으로 제작되었습니다.

## 🤝 기여

버그 리포트나 기능 제안은 이슈로 등록해 주세요. 