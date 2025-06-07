package com.example.websockettest;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocketConfig는 Spring WebSocket STOMP를 설정하는 구성 클래스입니다.
 * 
 * 주요 기능:
 * 1. STOMP 메시지 브로커 활성화
 * 2. STOMP 엔드포인트 등록 및 URL 매핑
 * 3. 메시지 브로커 설정 (destination 패턴)
 * 4. CORS 설정 및 보안 정책 구성
 * 
 * STOMP 설정 내용:
 * - WebSocket 연결 경로: "/ws"
 * - 브로커 destination: "/topic" (브로드캐스트), "/queue" (개별 메시지)
 * - 애플리케이션 destination: "/app" (클라이언트→서버 메시지)
 * - 모든 Origin 허용 (개발 환경용, 운영 환경에서는 제한 필요)
 * 
 * STOMP 아키텍처:
 * - 클라이언트 → /app/xxx → @MessageMapping 메서드
 * - 서버 → /topic/xxx → 구독 중인 모든 클라이언트
 * - 서버 → /queue/xxx → 특정 사용자
 * 
 * 보안 고려사항:
 * - 운영 환경에서는 setAllowedOrigins("*") 대신 특정 도메인만 허용
 * - 인증/인가 로직 추가 고려 (WebSocket 세션 인터셉터)
 * - Rate Limiting 및 Connection Limit 설정 고려
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 메시지 브로커를 설정하는 메서드
     * STOMP destination 패턴과 브로커 타입을 정의합니다.
     * 
     * 브로커 설정:
     * 1. Simple Broker: Spring 내장 브로커 사용
     * 2. Destination 패턴:
     *    - /topic: 브로드캐스트 메시지 (1:N 통신)
     *    - /queue: 개별 사용자 메시지 (1:1 통신)
     * 3. 애플리케이션 Destination Prefix: /app
     * 
     * 메시지 흐름:
     * - 클라이언트 → /app/chat → @MessageMapping("/chat") → 처리 후 /topic/messages
     * - 클라이언트 → /app/private → @MessageMapping("/private") → 처리 후 /queue/reply-{userId}
     * 
     * @param registry 메시지 브로커 레지스트리 객체
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple Message Broker 활성화
        // /topic: 브로드캐스트 메시지용 (채팅방, 공지사항 등)
        // /queue: 개별 사용자 메시지용 (1:1 메시지, 알림 등)
        registry.enableSimpleBroker("/topic", "/queue");
        
        // 클라이언트에서 서버로 보내는 메시지의 destination prefix 설정
        // 예: 클라이언트가 /app/chat으로 메시지 전송 → @MessageMapping("/chat") 메서드 호출
        registry.setApplicationDestinationPrefixes("/app");
        
        // 사용자별 개별 메시지 전송을 위한 prefix 설정
        // /queue/reply-{userId} 형태로 개별 사용자에게 메시지 전송 가능
        registry.setUserDestinationPrefix("/queue");
        
        // 외부 메시지 브로커 설정 (Redis, RabbitMQ 등) - 필요시 활성화
        // registry.enableStompBrokerRelay("/topic", "/queue")
        //     .setRelayHost("localhost")
        //     .setRelayPort(61613)
        //     .setClientLogin("guest")
        //     .setClientPasscode("guest");
    }

    /**
     * STOMP 엔드포인트를 등록하고 설정하는 메서드
     * 클라이언트가 WebSocket 연결을 수립할 때 사용하는 엔드포인트를 정의합니다.
     * 
     * 엔드포인트 설정:
     * 1. WebSocket 연결 경로: "/ws"
     * 2. SockJS 지원: WebSocket을 지원하지 않는 브라우저를 위한 폴백
     * 3. CORS 정책: 모든 Origin 허용 (개발환경용)
     * 
     * 연결 흐름:
     * 1. 클라이언트가 "ws://domain/ws"로 WebSocket 연결 요청
     * 2. STOMP 핸드셰이크 수행
     * 3. STOMP 세션 수립 완료
     * 4. destination 구독 및 메시지 송수신 시작
     * 
     * @param registry STOMP 엔드포인트 레지스트리 객체
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP 엔드포인트 등록
        registry
            // "/ws" 경로로 STOMP WebSocket 연결 엔드포인트 설정
            .addEndpoint("/ws")
            
            // CORS (Cross-Origin Resource Sharing) 설정
            // "*": 모든 Origin에서의 연결을 허용
            // 개발 환경용 설정이며, 운영 환경에서는 보안을 위해 특정 도메인만 허용 권장
            // 예: .setAllowedOrigins("https://mydomain.com", "https://www.mydomain.com")
            .setAllowedOriginPatterns("*")
            
            // SockJS 지원 활성화
            // WebSocket을 지원하지 않는 구형 브라우저에서 폴백 옵션 제공
            // HTTP 롱 폴링, JSONP 폴링 등의 대체 전송 방법 사용
            .withSockJS();

        // Postman용 Native WebSocket 엔드포인트 추가
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
        
        // 핸드셰이크 인터셉터 설정 (인증, 로깅 등):
        // .setHandshakeInterceptors(new HttpSessionHandshakeInterceptor())
        
        // WebSocket 전송 옵션 설정:
        // .setTransportHandlers(...)  // 사용자 정의 전송 핸들러
        // .setTaskScheduler(...)      // 스케줄러 설정
    }
}