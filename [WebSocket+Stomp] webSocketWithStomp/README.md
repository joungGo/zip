# WebSocket + STOMP 실시간 채팅 시스템

Spring Boot 3.5.0과 STOMP 프로토콜을 활용한 실시간 WebSocket 통신 시스템입니다.

## 📋 목차

- [프로젝트 개요](#프로젝트-개요)
- [구현된 기능](#구현된-기능)
- [STOMP vs 기본 WebSocket 비교](#stomp-vs-기본-websocket-비교)
- [기술 스택](#기술-스택)
- [테스트 방법](#테스트-방법)
- [핵심 구현 코드](#핵심-구현-코드)

## 🎯 프로젝트 개요

이 프로젝트는 **기본 WebSocket**에서 **STOMP(Simple Text Oriented Messaging Protocol)** 로의 진화 과정을 보여주며, 다음과 같은 3가지 테스트 시나리오를 제공합니다:

1. **단일 세션 테스트** - 기본적인 STOMP 연결과 메시지 송수신
2. **다중 세션 테스트** - 여러 세션 동시 관리 및 브로드캐스트
3. **룸 기반 채팅 테스트** - 채팅방별 격리된 실시간 통신

## 🚀 구현된 기능

### 1. 기본 WebSocket 통신
- **실시간 양방향 통신**
- **세션 관리 및 모니터링**
- **브로드캐스트 메시지 전송**

### 2. STOMP 프로토콜 기반 고급 기능
- **토픽 기반 구독/발행 패턴**
- **룸별 완전 격리된 채팅**
- **실시간 참여자 관리**
- **자동 연결 해제 처리**

### 3. REST API 통합
- **세션 통계 조회**
- **룸 정보 관리**
- **참여자 목록 조회**

## 🔄 STOMP vs 기본 WebSocket 비교

### ❌ 기본 WebSocket의 한계점

**1. 복잡한 메시지 라우팅**
```java
// 기본 WebSocket - 모든 라우팅을 수동으로 처리
@OnMessage
public void onMessage(String message, Session session) {
    try {
        JsonNode jsonNode = objectMapper.readTree(message);
        String type = jsonNode.get("type").asText();
        String roomId = jsonNode.get("roomId").asText();
        
        // 수동으로 타입별 분기 처리
        switch (type) {
            case "JOIN_ROOM":
                handleJoinRoom(roomId, session);
                break;
            case "LEAVE_ROOM":
                handleLeaveRoom(roomId, session);
                break;
            case "CHAT_MESSAGE":
                handleChatMessage(jsonNode, session);
                break;
            default:
                // 에러 처리
        }
    } catch (Exception e) {
        // 복잡한 예외 처리
    }
}

// 룸별 메시지 전송도 수동으로 필터링
private void sendToRoom(String roomId, String message) {
    for (Session session : sessions) {
        String sessionRoomId = getUserRoom(session);
        if (roomId.equals(sessionRoomId)) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                // 개별 세션 에러 처리
            }
        }
    }
}
```

**2. 세션 관리의 복잡성**
```java
// 기본 WebSocket - 수동 세션 관리
private final Map<String, Set<Session>> roomSessions = new ConcurrentHashMap<>();
private final Map<Session, String> sessionUsers = new ConcurrentHashMap<>();
private final Map<Session, String> sessionRooms = new ConcurrentHashMap<>();

@OnClose
public void onClose(Session session) {
    // 모든 맵에서 세션 정보를 수동으로 제거
    String username = sessionUsers.remove(session);
    String roomId = sessionRooms.remove(session);
    
    if (roomId != null) {
        Set<Session> roomSessionSet = roomSessions.get(roomId);
        if (roomSessionSet != null) {
            roomSessionSet.remove(session);
            // 빈 룸 정리도 수동으로
            if (roomSessionSet.isEmpty()) {
                roomSessions.remove(roomId);
            }
        }
    }
    
    // 퇴장 메시지도 수동으로 브로드캐스트
    if (username != null && roomId != null) {
        broadcastLeaveMessage(roomId, username);
    }
}
```

### ✅ STOMP 사용 시의 장점

**1. 선언적 메시지 라우팅**
```java
// STOMP - 어노테이션 기반 자동 라우팅
@Controller
public class WebSocketController {
    
    @MessageMapping("/room/{roomId}/join")
    public void joinRoom(@DestinationVariable String roomId, 
                        @Payload Map<String, String> payload,
                        SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String username = payload.get("username");
        
        // 비즈니스 로직에만 집중
        chatRoomService.joinRoom(roomId, sessionId, username);
    }
    
    @MessageMapping("/room/{roomId}/message")
    public void sendMessage(@DestinationVariable String roomId,
                           @Payload Map<String, String> payload,
                           SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String message = payload.get("message");
        
        // 단순한 서비스 호출
        chatRoomService.sendChatMessage(roomId, sessionId, message);
    }
}
```

**2. 자동화된 세션 관리**
```java
// STOMP - 이벤트 기반 자동 세션 관리
@EventListener
public class StompEventListener {
    
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        // 연결 시 자동 처리
        String sessionId = getSessionId(event);
        webSocketService.addSession(sessionId);
        log.info("새로운 WebSocket 연결: {}", sessionId);
    }
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        // 연결 해제 시 자동 정리
        String sessionId = getSessionId(event);
        
        // 서비스에서 알아서 모든 정리 작업 수행
        webSocketService.removeSession(sessionId);
        chatRoomService.disconnectSession(sessionId);
        
        log.info("WebSocket 연결 해제: {}", sessionId);
    }
}
```

**3. 토픽 기반 브로드캐스트**
```java
// STOMP - 토픽 기반 자동 브로드캐스트
@Service
public class ChatRoomService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public void broadcastToRoom(String roomId, RoomMessageDto message) {
        // 토픽으로 간단한 브로드캐스트
        String destination = "/topic/room/" + roomId;
        messagingTemplate.convertAndSend(destination, message);
    }
    
    public void joinRoom(String roomId, String sessionId, String username) {
        // 비즈니스 로직 처리
        roomSessions.computeIfAbsent(roomId, k -> new ConcurrentSkipListSet<>()).add(sessionId);
        sessionUsers.put(sessionId, username);
        sessionRooms.put(sessionId, roomId);
        
        int participantCount = roomParticipantCounts
                .computeIfAbsent(roomId, k -> new AtomicInteger(0))
                .incrementAndGet();
        
        // 자동 브로드캐스트
        RoomMessageDto joinMessage = RoomMessageDto.createJoinMessage(roomId, username, sessionId, participantCount);
        broadcastToRoom(roomId, joinMessage);
    }
}
```

**4. 타입 안전한 메시지 처리**
```java
// STOMP - 구조화된 메시지 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomMessageDto {
    private String type;          // CHAT, JOIN, LEAVE, SYSTEM, NOTIFICATION
    private String roomId;
    private String message;
    private String sender;
    private String sessionId;
    private LocalDateTime timestamp;
    private Integer participantCount;
    
    // 정적 팩토리 메서드로 타입 안전성 보장
    public static RoomMessageDto createJoinMessage(String roomId, String username, String sessionId, int participantCount) {
        return RoomMessageDto.builder()
                .type("JOIN")
                .roomId(roomId)
                .message(username + "님이 입장했습니다.")
                .sender(username)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .participantCount(participantCount)
                .build();
    }
}
```

## 💼 기술 스택

- **Backend**: Spring Boot 3.5.0, Java 21
- **WebSocket**: Spring WebSocket + STOMP
- **Build Tool**: Gradle
- **Dependencies**: Lombok, Jackson, SockJS, STOMP.js

## 🧪 테스트 방법

### 1. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 2. 브라우저 접속
```
http://localhost:8080
```

### 3. 테스트 시나리오

#### A. 단일 세션 테스트
1. "단일 세션 테스트" 카드 클릭
2. WebSocket 연결
3. 메시지 송수신 테스트
4. REST API 호출 테스트

#### B. 다중 세션 테스트
1. "다중 세션 테스트" 카드 클릭
2. 여러 세션 생성 및 연결
3. 브로드캐스트 메시지 전송
4. 모든 세션에서 동시 수신 확인

#### C. 룸 채팅 테스트
1. "룸 기반 채팅 테스트" 카드 클릭
2. 여러 브라우저 탭으로 접속
3. 각각 다른 사용자명으로 연결
4. 같은 룸 또는 다른 룸에서 채팅
5. 룸별 격리 및 실시간 참여자 목록 확인

## 🔧 핵심 구현 코드

### 1. STOMP 설정
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지 브로커 활성화
        config.enableSimpleBroker("/topic", "/queue");
        // 클라이언트에서 서버로 메시지 전송 시 prefix
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP 엔드포인트 등록 (SockJS 지원)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

### 2. 룸 기반 채팅 서비스
```java
@Service
@Slf4j
public class ChatRoomService {
    
    // Thread-safe 데이터 구조
    private final ConcurrentMap<String, Set<String>> roomSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> sessionUsers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> sessionRooms = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicInteger> roomParticipantCounts = new ConcurrentHashMap<>();
    
    public void joinRoom(String roomId, String sessionId, String username) {
        // 기존 룸에서 자동 퇴장
        String currentRoom = sessionRooms.get(sessionId);
        if (currentRoom != null && !currentRoom.equals(roomId)) {
            leaveRoom(currentRoom, sessionId);
        }
        
        // 룸 참가 처리
        roomSessions.computeIfAbsent(roomId, k -> new ConcurrentSkipListSet<>()).add(sessionId);
        sessionUsers.put(sessionId, username);
        sessionRooms.put(sessionId, roomId);
        
        // 참가자 수 증가
        int participantCount = roomParticipantCounts
                .computeIfAbsent(roomId, k -> new AtomicInteger(0))
                .incrementAndGet();
        
        // 입장 알림 브로드캐스트
        RoomMessageDto joinMessage = RoomMessageDto.createJoinMessage(roomId, username, sessionId, participantCount);
        broadcastToRoom(roomId, joinMessage);
    }
    
    public void disconnectSession(String sessionId) {
        // 연결 해제 시 자동 정리
        String roomId = sessionRooms.get(sessionId);
        if (roomId != null) {
            leaveRoom(roomId, sessionId);
        }
        
        sessionUsers.remove(sessionId);
        sessionRooms.remove(sessionId);
    }
}
```

### 3. REST API 컨트롤러
```java
@RestController
@RequestMapping("/api/websocket")
@Slf4j
public class WebSocketRestController {
    
    @GetMapping("/sessions/count")
    public ResponseEntity<Map<String, Object>> getActiveSessionCount() {
        int activeSessionCount = webSocketService.getActiveSessionCount();
        
        Map<String, Object> response = new HashMap<>();
        response.put("activeSessionCount", activeSessionCount);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/rooms/{roomId}/participants")
    public ResponseEntity<Map<String, Object>> getRoomParticipants(@PathVariable String roomId) {
        Set<String> participants = chatRoomService.getRoomParticipants(roomId);
        int participantCount = chatRoomService.getRoomParticipantCount(roomId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("roomId", roomId);
        response.put("participants", participants);
        response.put("participantCount", participantCount);
        
        return ResponseEntity.ok(response);
    }
}
```

## 📊 STOMP 사용의 핵심 장점

### 1. **코드 복잡도 감소**
- 메시지 라우팅 자동화로 **70% 이상 코드 감소**
- 어노테이션 기반 선언적 프로그래밍

### 2. **안정성 향상**
- 자동 세션 관리로 **메모리 누수 방지**
- 구조화된 메시지 포맷으로 **런타임 에러 감소**

### 3. **확장성 개선**
- 토픽 기반 브로드캐스트로 **무제한 룸 확장 가능**
- 느슨한 결합으로 **새로운 기능 추가 용이**

### 4. **개발 효율성**
- 비즈니스 로직에 집중 가능
- Spring 생태계와의 완벽한 통합