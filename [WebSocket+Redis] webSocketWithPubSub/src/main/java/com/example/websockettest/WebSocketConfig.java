package com.example.websockettest;

import com.example.websockettest.controller.WebSocketController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocketConfig는 Spring WebSocket을 설정하는 구성 클래스입니다.
 * 
 * 주요 기능:
 * 1. WebSocket 기능 활성화
 * 2. WebSocket 핸들러 등록 및 URL 매핑
 * 3. CORS 설정 및 보안 정책 구성
 * 4. WebSocket 연결 엔드포인트 정의
 * 
 * 설정 내용:
 * - WebSocket 연결 경로: "/my-websocket"
 * - 모든 Origin 허용 (개발 환경용, 운영 환경에서는 제한 필요)
 * - WebSocketController를 통한 연결 처리
 * 
 * Spring WebSocket 구성 요소:
 * - @Configuration: Spring 설정 클래스로 등록
 * - @EnableWebSocket: WebSocket 기능 활성화
 * - WebSocketConfigurer: WebSocket 설정을 위한 인터페이스 구현
 * 
 * 보안 고려사항:
 * - 운영 환경에서는 setAllowedOrigins("*") 대신 특정 도메인만 허용
 * - 인증/인가 로직 추가 고려
 * - Rate Limiting 및 Connection Limit 설정 고려
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    /**
     * WebSocketController 인스턴스를 Spring Bean으로 등록하는 메서드
     * 
     * Spring Bean 등록 이유:
     * 1. Spring의 의존성 주입 기능 활용
     * 2. WebSocketController 내부의 @Autowired 어노테이션 동작 보장
     * 3. Spring 컨테이너에 의한 생명주기 관리
     * 4. AOP, 트랜잭션 등 Spring 기능 활용 가능
     * 
     * @Bean 어노테이션:
     * - 메서드가 반환하는 객체를 Spring 컨테이너에 Bean으로 등록
     * - 싱글톤 패턴으로 관리 (기본값)
     * - 다른 Bean에서 의존성 주입 시 이 인스턴스 사용
     * 
     * @return WebSocketController 인스턴스
     */
    @Bean
    public WebSocketController webSocketController() {
        // WebSocketController 인스턴스 생성 및 반환
        // Spring이 이 객체를 관리하며, 필요한 의존성을 자동 주입
        return new WebSocketController();
    }

    /**
     * WebSocket 핸들러를 등록하고 URL 매핑을 설정하는 메서드
     * WebSocketConfigurer 인터페이스의 필수 구현 메서드입니다.
     * 
     * 설정 내용:
     * 1. WebSocket 연결 엔드포인트 정의
     * 2. 각 엔드포인트에 대한 핸들러 지정
     * 3. CORS 정책 설정
     * 4. 기타 WebSocket 연결 옵션 구성
     * 
     * 연결 흐름:
     * 1. 클라이언트가 "ws://domain/my-websocket"으로 연결 요청
     * 2. Spring이 이 요청을 WebSocketController로 라우팅
     * 3. WebSocketController가 연결을 처리하고 세션 관리
     * 
     * @param registry WebSocket 핸들러를 등록하기 위한 레지스트리 객체
     *                Spring이 제공하며, 핸들러와 URL 매핑을 관리
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // WebSocket 핸들러 등록 및 설정
        registry
            // WebSocketController를 "/my-websocket" 경로에 매핑
            // 클라이언트는 이 경로로 WebSocket 연결을 수립
            .addHandler(webSocketController(), "/my-websocket")
            
            // CORS (Cross-Origin Resource Sharing) 설정
            // "*": 모든 Origin에서의 연결을 허용
            // 개발 환경용 설정이며, 운영 환경에서는 보안을 위해 특정 도메인만 허용 권장
            // 예: .setAllowedOrigins("https://mydomain.com", "https://www.mydomain.com")
            .setAllowedOrigins("*");
        
        // 추가 설정 가능 옵션들 (필요시 체이닝으로 추가):
        // .setAllowedOriginPatterns("https://*.mydomain.com")  // 패턴 기반 Origin 허용
        // .withSockJS()  // SockJS 폴백 지원 (WebSocket 미지원 브라우저 대응)
        // .setHandshakeInterceptors(...)  // 핸드셰이크 인터셉터 추가 // 이 기능은 WebSocket 연결 시 추가적인 인증이나 로깅 등을 처리할 수 있다.
        // .setAllowedHeaders(...)  // 허용할 HTTP 헤더 설정 // WebSocket 연결 시 클라이언트가 보낼 수 있는 HTTP 헤더를 제한할 수 있다.
    }
}