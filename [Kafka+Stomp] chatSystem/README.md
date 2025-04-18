# Kafka-STOMP 채팅 시스템

## 1. 프로젝트 소개

이 프로젝트는 Kafka와 STOMP 프로토콜을 활용한 실시간 채팅 시스템입니다. 사용자들은 채팅방을 생성하고 참여하여 실시간으로 메시지를 주고받을 수 있습니다. 채팅방 정보는 MySQL에 저장되고, 채팅 메시지는 MongoDB에 저장됩니다. Kafka를 통해 메시지 처리의 확장성과 안정성을 확보했습니다.

### 주요 기능
- 채팅방 생성 및 참여
- 실시간 메시지 전송 및 수신
- 채팅 기록 저장 및 조회
- 사용자 참여/퇴장 알림

## 2. 사용 기술

### 백엔드
- **Java 21**
- **Spring Boot 3.4.4**
- **WebSocket & STOMP**: 실시간 양방향 통신
- **Apache Kafka**: 메시지 큐잉 및 처리
- **MySQL**: 채팅방 정보 저장
- **MongoDB**: 채팅 메시지 저장
- **Gradle**: 빌드 도구

### 프론트엔드
- **Next.js**: React 기반 프레임워크
- **TypeScript**: 타입 안정성 확보
- **STOMP.js**: WebSocket 클라이언트
- **SockJS**: WebSocket 폴백 지원
- **Date-fns**: 날짜 및 시간 처리
- **Axios**: HTTP 클라이언트
- **TailwindCSS**: 스타일링

## 3. 프로젝트 클론 후 설치 및 사용 방법

### 사전 요구사항
- Java 21
- Node.js 및 npm
- MySQL
- MongoDB
- Kafka

### 백엔드 설정 및 실행

1. MySQL 설정
```sql
CREATE DATABASE chatdb;
```

2. Kafka 설정
- Kafka 서버 실행 및 필요한 토픽 생성:
```bash
# Kafka 서버 시작
./bin/kafka-server-start.sh ./config/server.properties

# 토픽 생성
./bin/kafka-topics.sh --create --topic chat-messages --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
./bin/kafka-topics.sh --create --topic chat-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

3. 백엔드 애플리케이션 실행
```bash
cd backend
./gradlew bootRun
```

### 프론트엔드 설정 및 실행

1. 의존성 설치
```bash
cd frontend
npm install
```

2. 개발 서버 실행
```bash
npm run dev
```

3. 브라우저에서 접속
```
http://localhost:3000
```

## 4. 사용한 기술에 대한 개념 및 설명

### WebSocket 및 STOMP
WebSocket은 HTTP와 다르게 한 번 연결이 수립되면 계속 유지되는 양방향 통신 프로토콜입니다. STOMP(Simple Text Oriented Messaging Protocol)는 WebSocket 위에서 동작하는 메시징 프로토콜로, 메시지를 목적지(destination)에 따라 라우팅할 수 있고 구독(subscribe) 기능을 제공합니다.

### Kafka
Kafka는 고성능 분산 이벤트 스트리밍 플랫폼입니다. 본 프로젝트에서는 다음과 같은 이유로 Kafka를 활용했습니다:
- **확장성**: 대량의 메시지를 처리할 수 있습니다.
- **안정성**: 메시지가 손실되지 않도록 보장합니다.
- **분산 처리**: 여러 서버에서 메시지를 처리할 수 있습니다.
- **비동기 처리**: 메시지 생산자와 소비자를 분리하여 시스템의 유연성을 높입니다.

### MySQL과 MongoDB의 활용
- **MySQL**: 정형화된 데이터인 채팅방 정보(ID, 이름, 생성 시간, 참가자 등)를 저장합니다. 관계형 데이터베이스의 트랜잭션과 ACID 특성을 활용합니다.
- **MongoDB**: 비정형 데이터인 채팅 메시지를 저장합니다. 대량의 메시지 기록을 효율적으로 저장하고 조회할 수 있습니다.

### Next.js와 React
Next.js는 서버 사이드 렌더링(SSR), 정적 사이트 생성(SSG) 등 다양한 렌더링 방식을 지원하는 React 기반 프레임워크입니다. 본 프로젝트에서는 클라이언트 컴포넌트를 활용하여 실시간 채팅 기능을 구현했습니다.

## 5. 구현한 기능에 대한 설명 및 사용한 방법

### 전체 시스템 아키텍처
![채팅 시스템 아키텍처](https://i.imgur.com/JQhtZWa.png)

### 메시지 흐름
1. 클라이언트가 STOMP를 통해 메시지 전송
2. 서버가 메시지를 Kafka 토픽에 발행
3. Kafka 컨슈머가 메시지를 처리하고 MongoDB에 저장
4. 서버가 채팅방 구독자들에게 메시지 전달

### 주요 컴포넌트 설명

#### 백엔드

##### WebSocket 설정
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
```

##### Kafka 프로듀서
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, ChatMessageDto> kafkaTemplate;

    @Value("${app.kafka.topics.chat-messages}")
    private String chatMessagesTopic;

    public void sendChatMessage(ChatMessageDto message) {
        kafkaTemplate.send(chatMessagesTopic, message);
    }
}
```

##### Kafka 컨슈머
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "${app.kafka.topics.chat-messages}")
    public void consumeChatMessage(ChatMessageDto message) {
        // 메시지 저장
        chatMessageService.saveMessage(message);
        
        // 실시간 전송
        messagingTemplate.convertAndSend("/topic/chat/" + message.getRoomId(), message);
    }
}
```

##### 데이터 모델 설계
- **ChatRoom (MySQL)**: 채팅방 정보 및 참가자 목록 저장
- **ChatMessage (MongoDB)**: 채팅 메시지 내용, 발신자, 시간 등 저장

#### 프론트엔드

##### WebSocket 서비스
```typescript
export class WebSocketService {
    private client: Client | null = null;
    
    // WebSocket 연결 설정
    constructor() {
        this.client = new Client({
            webSocketFactory: () => new SockJS(SOCKET_URL),
            reconnectDelay: 5000
        });
    }
    
    // 메시지 전송
    public sendMessage(content: string) {
        this.client?.publish({
            destination: `/app/chat/send/${this.roomId}`,
            body: JSON.stringify(message)
        });
    }
    
    // 채팅방 구독
    public joinRoom(roomId: number) {
        this.client?.subscribe(`/topic/chat/${roomId}`, this.handleMessage.bind(this));
    }
}
```

##### 채팅 UI 구성
- **ChatRoomList**: 채팅방 목록 표시 및 새 채팅방 생성
- **ChatRoom**: 현재 선택된 채팅방의 메시지 표시 및 입력
- **ChatMessage**: 개별 메시지 표시 (본인/상대방 구분, 시스템 메시지 등)

### 기능 흐름

1. **사용자 로그인**
   - 사용자 이름 입력 시 localStorage에 저장
   - WebSocket 연결 수립

2. **채팅방 생성 및 참여**
   - 채팅방 목록에서 새 채팅방 생성
   - 채팅방 선택 시 해당 채팅방 주제 구독
   - 입장 메시지 전송 (JOIN 타입)

3. **메시지 전송 및 수신**
   - 메시지 작성 후 전송 시 WebSocket을 통해 서버로 전달
   - 서버는 메시지를 Kafka로 발행
   - Kafka 컨슈머가 메시지 처리 후 MongoDB에 저장
   - 처리된 메시지는 해당 채팅방 구독자들에게 실시간 전달

4. **채팅방 나가기**
   - 나가기 버튼 클릭 시 퇴장 메시지 전송 (LEAVE 타입)
   - 해당 채팅방 구독 해제
   - 사용자 목록에서 제거

이 프로젝트는 Kafka와 STOMP를 활용하여 확장 가능하고 안정적인 실시간 채팅 시스템을 구현했습니다. MySQL과 MongoDB를 적절히 활용하여 각 데이터의 특성에 맞는 저장소를 선택했으며, 프론트엔드는 Next.js와 TypeScript를 통해 타입 안전성이 보장된 애플리케이션으로 구현했습니다. 