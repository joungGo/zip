# WebSocket + STOMP 프로젝트 Postman 테스트 가이드

## 📋 목차
- [프로젝트 개요](#프로젝트-개요)
- [테스트 환경 설정](#테스트-환경-설정)
- [REST API 테스트](#rest-api-테스트)
- [WebSocket/STOMP 테스트](#websocketstomp-테스트)
- [Postman Collection 설정](#postman-collection-설정)
- [테스트 시나리오](#테스트-시나리오)

## 🎯 프로젝트 개요

이 프로젝트는 Spring Boot 3.5.0과 STOMP 프로토콜을 활용한 실시간 WebSocket 통신 시스템입니다.

**주요 기능:**
- 실시간 WebSocket 통신 (STOMP 프로토콜)
- 룸 기반 채팅 시스템
- REST API를 통한 상태 조회 및 관리
- 브로드캐스트 메시지 전송

**서버 정보:**
- 기본 포트: `8080`
- Base URL: `http://localhost:8080`
- WebSocket URL: `ws://localhost:8080/ws`

## ⚙️ 테스트 환경 설정

### 1. 서버 실행
```bash
# Gradle을 사용하여 서버 실행
./gradlew bootRun

# 또는 JAR 파일 실행
java -jar build/libs/websocket-test-*.jar
```

### 2. Postman 설정
- Postman 최신 버전 설치 (WebSocket 지원 버전)
- 새로운 Collection 생성: "WebSocket STOMP Test"

## 🌐 REST API 테스트

### 1. WebSocket 세션 관리 API

#### 1.1 활성 세션 수 조회
```http
GET http://localhost:8080/api/websocket/sessions/count
Content-Type: application/json
```

**예상 응답:**
```json
{
    "activeSessionCount": 0,
    "timestamp": 1699123456789
}
```

#### 1.2 WebSocket 서비스 상태 확인
```http
GET http://localhost:8080/api/websocket/status
Content-Type: application/json
```

**예상 응답:**
```json
{
    "service": "WebSocket Service",
    "status": "running",
    "activeConnections": 0,
    "timestamp": 1699123456789
}
```

#### 1.3 브로드캐스트 메시지 전송
```http
POST http://localhost:8080/api/websocket/broadcast
Content-Type: application/json

{
    "message": "시스템 공지: 모든 사용자에게 전송되는 메시지입니다."
}
```

**예상 응답:**
```json
{
    "success": true,
    "message": "Broadcast message sent successfully",
    "sentTo": 3,
    "timestamp": 1699123456789
}
```

### 2. 채팅방 관리 API

#### 2.1 모든 채팅방 정보 조회
```http
GET http://localhost:8080/api/websocket/rooms
Content-Type: application/json
```

**예상 응답:**
```json
{
    "totalRooms": 2,
    "rooms": {
        "room1": {
            "participantCount": 3,
            "participants": ["user1", "user2", "user3"]
        },
        "room2": {
            "participantCount": 1,
            "participants": ["user4"]
        }
    },
    "timestamp": 1699123456789
}
```

#### 2.2 특정 채팅방 참여자 조회
```http
GET http://localhost:8080/api/websocket/rooms/{roomId}/participants
Content-Type: application/json
```

**예시:** `GET http://localhost:8080/api/websocket/rooms/room1/participants`

**예상 응답:**
```json
{
    "roomId": "room1",
    "participantCount": 3,
    "participants": ["user1", "user2", "user3"],
    "timestamp": 1699123456789
}
```

#### 2.3 특정 채팅방 상태 조회
```http
GET http://localhost:8080/api/websocket/rooms/{roomId}/status
Content-Type: application/json
```

**예시:** `GET http://localhost:8080/api/websocket/rooms/room1/status`

**예상 응답:**
```json
{
    "roomId": "room1",
    "participantCount": 3,
    "isActive": true,
    "timestamp": 1699123456789
}
```

## 🔌 WebSocket/STOMP 테스트

### 1. Postman에서 WebSocket 연결 설정

#### 1.1 새 WebSocket 요청 생성
1. Postman에서 "New" → "WebSocket Request" 선택
2. URL 입력: `ws://localhost:8080/ws`
3. "Connect" 버튼 클릭

#### 1.2 STOMP 프로토콜 설정
WebSocket 연결 후, STOMP 프로토콜을 사용하여 통신합니다.

**STOMP 연결 메시지:**
```
CONNECT
accept-version:1.0,1.1,2.0
heart-beat:10000,10000

```

### 2. 기본 STOMP 기능 테스트

#### 2.1 브로드캐스트 메시지 테스트

**1단계: 토픽 구독**
```
SUBSCRIBE
id:sub-1
destination:/topic/messages

```

**2단계: 메시지 전송**
```
SEND
destination:/app/message
content-type:text/plain

Hello, WebSocket World!
```

**예상 수신 메시지:**
```
MESSAGE
destination:/topic/messages
content-type:text/plain
subscription:sub-1
message-id:1

Hello, HELLO, WEBSOCKET WORLD!!
```

#### 2.2 개인 메시지 테스트

**1단계: 개인 큐 구독**
```
SUBSCRIBE
id:sub-2
destination:/user/queue/reply

```

**2단계: 개인 메시지 전송**
```
SEND
destination:/app/private
content-type:text/plain

Private message test
```

**예상 수신 메시지:**
```
MESSAGE
destination:/user/queue/reply
content-type:text/plain
subscription:sub-2
message-id:2

Private reply to anonymous: private message test
```

#### 2.3 시스템 메시지 테스트

**1단계: 시스템 토픽 구독**
```
SUBSCRIBE
id:sub-3
destination:/topic/system

```

**2단계: 시스템 상태 조회**
```
SEND
destination:/app/system
content-type:text/plain

status
```

**예상 수신 메시지:**
```
MESSAGE
destination:/topic/system
content-type:text/plain
subscription:sub-3
message-id:3

System Status: 1 active connections
```

#### 2.4 에코 테스트

**1단계: 에코 큐 구독**
```
SUBSCRIBE
id:sub-4
destination:/user/queue/echo

```

**2단계: 에코 메시지 전송**
```
SEND
destination:/app/echo
content-type:text/plain

Echo test message
```

**예상 수신 메시지:**
```
MESSAGE
destination:/user/queue/echo
content-type:text/plain
subscription:sub-4
message-id:4

Echo: Echo test message (timestamp: 1699123456789)
```

### 3. 채팅방 기능 테스트

#### 3.1 채팅방 입장

**1단계: 채팅방 토픽 구독**
```
SUBSCRIBE
id:sub-room1
destination:/topic/room/room1

```

**2단계: 채팅방 참가**
```
SEND
destination:/app/room/room1/join
content-type:application/json

{"username": "testuser1"}
```

**예상 수신 메시지:**
```
MESSAGE
destination:/topic/room/room1
content-type:application/json
subscription:sub-room1
message-id:5

{
    "type": "USER_JOIN",
    "roomId": "room1",
    "username": "testuser1",
    "message": "testuser1님이 채팅방에 입장했습니다.",
    "participantCount": 1,
    "timestamp": "2023-11-05T10:30:45"
}
```

#### 3.2 채팅방 메시지 전송

```
SEND
destination:/app/room/room1/message
content-type:application/json

{"message": "안녕하세요! 첫 번째 메시지입니다."}
```

**예상 수신 메시지:**
```
MESSAGE
destination:/topic/room/room1
content-type:application/json
subscription:sub-room1
message-id:6

{
    "type": "CHAT",
    "roomId": "room1",
    "username": "testuser1",
    "message": "안녕하세요! 첫 번째 메시지입니다.",
    "timestamp": "2023-11-05T10:31:00"
}
```

#### 3.3 채팅방 퇴장

```
SEND
destination:/app/room/room1/leave
content-type:text/plain

```

**예상 수신 메시지:**
```
MESSAGE
destination:/topic/room/room1
content-type:application/json
subscription:sub-room1
message-id:7

{
    "type": "USER_LEAVE",
    "roomId": "room1",
    "username": "testuser1",
    "message": "testuser1님이 채팅방을 떠났습니다.",
    "participantCount": 0,
    "timestamp": "2023-11-05T10:32:00"
}
```

## 📦 Postman Collection 설정

### Collection 생성
다음 JSON을 Postman에 Import하여 완전한 테스트 환경을 구성할 수 있습니다:

```json
{
    "info": {
        "name": "WebSocket STOMP Test Collection",
        "description": "Spring Boot WebSocket + STOMP 프로젝트 테스트용 Collection"
    },
    "variable": [
        {
            "key": "baseUrl",
            "value": "http://localhost:8080"
        },
        {
            "key": "wsUrl",
            "value": "ws://localhost:8080/ws"
        }
    ],
    "item": [
        {
            "name": "REST API Tests",
            "item": [
                {
                    "name": "Get Active Session Count",
                    "request": {
                        "method": "GET",
                        "header": [],
                        "url": "{{baseUrl}}/api/websocket/sessions/count"
                    }
                },
                {
                    "name": "Get WebSocket Status",
                    "request": {
                        "method": "GET",
                        "header": [],
                        "url": "{{baseUrl}}/api/websocket/status"
                    }
                },
                {
                    "name": "Send Broadcast Message",
                    "request": {
                        "method": "POST",
                        "header": [
                            {
                                "key": "Content-Type",
                                "value": "application/json"
                            }
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"message\": \"Broadcast test message\"\n}"
                        },
                        "url": "{{baseUrl}}/api/websocket/broadcast"
                    }
                },
                {
                    "name": "Get All Rooms",
                    "request": {
                        "method": "GET",
                        "header": [],
                        "url": "{{baseUrl}}/api/websocket/rooms"
                    }
                },
                {
                    "name": "Get Room Participants",
                    "request": {
                        "method": "GET",
                        "header": [],
                        "url": "{{baseUrl}}/api/websocket/rooms/room1/participants"
                    }
                },
                {
                    "name": "Get Room Status",
                    "request": {
                        "method": "GET",
                        "header": [],
                        "url": "{{baseUrl}}/api/websocket/rooms/room1/status"
                    }
                }
            ]
        }
    ]
}
```

## 🧪 테스트 시나리오

### 시나리오 1: 단일 사용자 기본 기능 테스트

1. **서버 상태 확인**
   - `GET /api/websocket/status` 호출
   - 서버가 정상 동작하는지 확인

2. **WebSocket 연결**
   - `ws://localhost:8080/ws`로 연결
   - STOMP CONNECT 메시지 전송

3. **기본 기능 테스트**
   - 에코 테스트: `/app/echo` → `/user/queue/echo`
   - 브로드캐스트 테스트: `/app/message` → `/topic/messages`
   - 개인 메시지 테스트: `/app/private` → `/user/queue/reply`

### 시나리오 2: 다중 사용자 브로드캐스트 테스트

1. **여러 WebSocket 연결 생성**
   - 여러 Postman 탭에서 동시에 WebSocket 연결

2. **토픽 구독**
   - 모든 연결에서 `/topic/messages` 구독

3. **브로드캐스트 테스트**
   - 한 연결에서 `/app/message`로 메시지 전송
   - 모든 연결에서 메시지 수신 확인

4. **REST API로 확인**
   - `GET /api/websocket/sessions/count`로 연결 수 확인

### 시나리오 3: 채팅방 격리 테스트

1. **두 개의 채팅방 생성**
   - room1, room2 각각에 사용자 입장

2. **채팅방별 토픽 구독**
   - 사용자1: `/topic/room/room1` 구독
   - 사용자2: `/topic/room/room2` 구독

3. **격리 테스트**
   - room1에서 메시지 전송 → room1 구독자만 수신
   - room2에서 메시지 전송 → room2 구독자만 수신

4. **참여자 관리 확인**
   - `GET /api/websocket/rooms/room1/participants`
   - `GET /api/websocket/rooms/room2/participants`

### 시나리오 4: 연결 해제 및 정리 테스트

1. **사용자 입장**
   - 채팅방에 여러 사용자 입장

2. **연결 해제**
   - WebSocket 연결 끊기 (Disconnect)

3. **자동 정리 확인**
   - `GET /api/websocket/rooms/{roomId}/participants`로 참여자 목록 확인
   - 연결이 끊어진 사용자가 자동으로 제거되었는지 확인

## 🔧 트러블슈팅

### 일반적인 문제들

1. **WebSocket 연결 실패**
   - 서버가 실행 중인지 확인
   - 포트 8080이 사용 가능한지 확인
   - 방화벽 설정 확인

2. **STOMP 메시지 형식 오류**
   - 메시지 끝에 NULL 문자(`\0`) 추가 필요
   - 헤더와 본문 사이에 빈 줄 필요

3. **브로드캐스트 메시지가 수신되지 않음**
   - 토픽 구독이 올바른지 확인
   - 구독 ID가 유니크한지 확인

4. **채팅방 메시지 격리 실패**
   - 올바른 roomId 사용 확인
   - 채팅방별 토픽 구독 확인

### 디버깅 도구

1. **서버 로그 확인**
   - 애플리케이션 실행 시 콘솔 로그 모니터링
   - 에러 메시지 및 연결 상태 확인

2. **브라우저 개발자 도구**
   - Network 탭에서 WebSocket 연결 상태 확인
   - Console에서 JavaScript 에러 확인

3. **Postman Console**
   - WebSocket 메시지 송수신 로그 확인
   - 연결 상태 및 에러 메시지 확인

## 📞 추가 지원

더 자세한 테스트가 필요하거나 문제가 발생한 경우:

1. **프로젝트 README.md** 참조
2. **서버 로그** 확인
3. **Spring Boot WebSocket/STOMP 공식 문서** 참조

이 가이드를 통해 FE 개발자가 효율적으로 WebSocket STOMP 기능을 테스트하고 개발할 수 있습니다. 