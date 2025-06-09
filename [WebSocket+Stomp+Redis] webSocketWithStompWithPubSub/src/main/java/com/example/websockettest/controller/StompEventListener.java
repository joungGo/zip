package com.example.websockettest.controller;

import com.example.websockettest.dto.StompMessage;
import com.example.websockettest.service.ChatRoomService;
import com.example.websockettest.service.RedisStompMessagePublisher;
import com.example.websockettest.service.SessionCountService;
import com.example.websockettest.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * STOMP WebSocket 세션 이벤트를 처리하는 리스너 클래스 (Redis 통합)
 * 
 * 주요 기능:
 * 1. 세션 연결 이벤트 처리 (@EventListener) >> 즉, 사용자가 직접 이벤트를 발행하는 코드는 없고, Spring이 WebSocket 생명주기에 따라 자동으로 발행하는 시스템 이벤트들입니다.
 * 2. 세션 해제 이벤트 처리
 * 3. destination 구독/구독해제 이벤트 처리
 * 4. 사용자 세션 매핑 관리
 * 5. 연결 상태 알림 브로드캐스트
 * 6. Redis Pub/Sub을 통한 세션 이벤트 다중 서버 동기화
 * 
 * Spring의 이벤트 기반 아키텍처를 활용하여
 * STOMP 세션 생명주기를 관리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompEventListener {

    /**
     * WebSocket 관련 비즈니스 로직을 처리하는 서비스
     */
    private final WebSocketService webSocketService;

    /**
     * 세션 카운트 관리 서비스
     * 순환 의존성 없이 세션 수를 추적
     */
    private final SessionCountService sessionCountService;
    
    /**
     * 채팅방 관리 서비스
     * 세션 해제 시 채팅방에서 정리 처리
     */
    private final ChatRoomService chatRoomService;
    
    /**
     * Redis Pub/Sub 메시지 발행 서비스
     * 세션 이벤트를 다른 서버들과 동기화
     */
    private final RedisStompMessagePublisher redisStompMessagePublisher;

    /**
     * 세션 ID와 사용자 정보를 매핑하는 동시성 안전 맵
     * Key: 세션 ID, Value: 사용자 정보 (사용자명, 연결 시간 등)
     */
    private final ConcurrentMap<String, UserSessionInfo> sessionUserMap = new ConcurrentHashMap<>();

    /**
     * 사용자별 구독 정보를 관리하는 맵
     * Key: 세션 ID, Value: 구독 중인 destination 목록
     */
    private final ConcurrentMap<String, ConcurrentMap<String, String>> sessionSubscriptions = new ConcurrentHashMap<>();

    /**
     * STOMP 세션 연결 이벤트 핸들러
     * 클라이언트가 WebSocket 연결을 수립하고 STOMP 핸드셰이크가 완료되었을 때 호출됩니다.
     * STOMP 핸드셰이크란 클라이언트와 서버 간의 STOMP 프로토콜을 사용한 초기 연결 설정 과정입니다.
     * 
     * 발생 예시: 브라우저에서 `stompClient.connect({}, callback)` 실행 시 자동 발행
     * @param event 세션 연결 이벤트 객체
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        Principal user = headerAccessor.getUser();
        String username = user != null ? user.getName() : "anonymous";

        log.info("🔗 STOMP 세션 연결: sessionId={}, username={}", 
                sessionId, username);

        try {
            // 사용자 세션 정보 저장
            UserSessionInfo userInfo = UserSessionInfo.builder()
                    .sessionId(sessionId)
                    .username(username)
                    .connectedAt(System.currentTimeMillis())
                    .remoteAddress("unknown") // STOMP에서는 직접 접근 불가
                    .build();

            sessionUserMap.put(sessionId, userInfo);

            // 세션 카운트 증가
            int totalSessions = sessionCountService.incrementSessionCount(sessionId);

            // 구독 정보 초기화
            sessionSubscriptions.put(sessionId, new ConcurrentHashMap<>());

            // 사용자 입장 알림 브로드캐스트
            String joinMessage = String.format("사용자 '%s'님이 입장하셨습니다.", username);
            StompMessage userJoinMessage = StompMessage.builder()
                    .type(StompMessage.MessageType.USER_JOIN)
                    .senderId("SYSTEM")
                    .content(joinMessage)
                    .timestamp(System.currentTimeMillis())
                    .extraData(userInfo)
                    .priority(1)
                    .build();

            webSocketService.broadcastNotification(joinMessage, userInfo);
            
            // 🌟 Redis Pub/Sub으로 세션 연결 이벤트 발행 (다중 서버 동기화)
            redisStompMessagePublisher.publishSessionConnectEvent(sessionId, username);

            log.info("✅ STOMP 세션 연결 처리 완료: sessionId={}, username={}, totalSessions={}", 
                    sessionId, username, totalSessions);

        } catch (Exception e) {
            log.error("❌ STOMP 세션 연결 처리 중 오류 발생: sessionId={}, username={}, error={}", 
                    sessionId, username, e.getMessage(), e);
        }
    }

    /**
     * STOMP 세션 해제 이벤트 핸들러
     * 클라이언트가 연결을 끊거나 네트워크 문제로 연결이 종료되었을 때 호출됩니다.
     * 
     * 발생 예시: 브라우저에서 `stompClient.disconnect()` 실행 또는 브라우저 탭 종료 시 자동 발행
     * @param event 세션 해제 이벤트 객체
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("🔌 STOMP 세션 해제: sessionId={}, closeStatus={}", 
                sessionId, event.getCloseStatus());

        try {
            // 사용자 세션 정보 조회
            UserSessionInfo userInfo = sessionUserMap.remove(sessionId);
            if (userInfo != null) {
                String username = userInfo.getUsername();
                long connectedDuration = System.currentTimeMillis() - userInfo.getConnectedAt();

                // 세션 카운트 감소
                int remainingSessions = sessionCountService.decrementSessionCount(sessionId);

                // 구독 정보 정리
                sessionSubscriptions.remove(sessionId);
                
                // 채팅방에서 세션 정리 (채팅방에 참여 중이었다면 퇴장 처리)
                chatRoomService.disconnectSession(sessionId);

                // 사용자 퇴장 알림 브로드캐스트
                String leaveMessage = String.format("사용자 '%s'님이 퇴장하셨습니다. (연결 시간: %d초)", 
                        username, connectedDuration / 1000);
                
                StompMessage userLeaveMessage = StompMessage.builder()
                        .type(StompMessage.MessageType.USER_LEAVE)
                        .senderId("SYSTEM")
                        .content(leaveMessage)
                        .timestamp(System.currentTimeMillis())
                        .extraData(userInfo)
                        .priority(1)
                        .build();

                webSocketService.broadcastNotification(leaveMessage, userInfo);
                
                // 🌟 Redis Pub/Sub으로 세션 해제 이벤트 발행 (다중 서버 동기화)
                String currentRoomId = chatRoomService.getCurrentRoom(sessionId);
                redisStompMessagePublisher.publishSessionDisconnectEvent(sessionId, username, currentRoomId);

                log.info("✅ STOMP 세션 해제 처리 완료: sessionId={}, username={}, duration={}ms, remainingSessions={}", 
                        sessionId, username, connectedDuration, remainingSessions);
            } else {
                log.warn("⚠️ 해제된 세션의 사용자 정보를 찾을 수 없음: sessionId={}", sessionId);
            }

        } catch (Exception e) {
            log.error("❌ STOMP 세션 해제 처리 중 오류 발생: sessionId={}, error={}", 
                    sessionId, e.getMessage(), e);
        }
    }

    /**
     * STOMP destination 구독 이벤트 핸들러
     * 클라이언트가 특정 destination을 구독했을 때 호출됩니다.
     * 
     * 발생 예시: 브라우저에서 `stompClient.subscribe('/topic/messages', callback)` 실행 시 자동 발행
     * @param event 구독 이벤트 객체
     */
    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        String subscriptionId = headerAccessor.getSubscriptionId();

        log.info("📡 destination 구독: sessionId={}, destination={}, subscriptionId={}", 
                sessionId, destination, subscriptionId);

        try {
            // 사용자 세션 정보 조회
            UserSessionInfo userInfo = sessionUserMap.get(sessionId);
            if (userInfo != null) {
                // 구독 정보 저장
                ConcurrentMap<String, String> subscriptions = sessionSubscriptions.get(sessionId);
                if (subscriptions != null) {
                    subscriptions.put(subscriptionId, destination);
                }

                log.debug("✅ destination 구독 처리 완료: sessionId={}, username={}, destination={}", 
                        sessionId, userInfo.getUsername(), destination);

                // 특정 destination에 대한 환영 메시지 (선택사항)
                if ("/topic/messages".equals(destination)) {
                    String welcomeMessage = String.format("'%s'님이 채팅방에 참여했습니다.", userInfo.getUsername());
                    webSocketService.broadcastMessage(welcomeMessage);
                }
            } else {
                log.warn("⚠️ 구독 이벤트의 사용자 정보를 찾을 수 없음: sessionId={}", sessionId);
            }

        } catch (Exception e) {
            log.error("❌ destination 구독 처리 중 오류 발생: sessionId={}, destination={}, error={}", 
                    sessionId, destination, e.getMessage(), e);
        }
    }

    /**
     * STOMP destination 구독 해제 이벤트 핸들러
     * 클라이언트가 특정 destination 구독을 해제했을 때 호출됩니다.
     * 
     * 발생 예시: 브라우저에서 `subscription.unsubscribe()` 실행 또는 연결 해제 시 자동 발행
     * @param event 구독 해제 이벤트 객체
     */
    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String subscriptionId = headerAccessor.getSubscriptionId();

        log.info("📡 destination 구독 해제: sessionId={}, subscriptionId={}", 
                sessionId, subscriptionId);

        try {
            // 사용자 세션 정보 조회
            UserSessionInfo userInfo = sessionUserMap.get(sessionId);
            if (userInfo != null) {
                // 구독 정보 제거
                ConcurrentMap<String, String> subscriptions = sessionSubscriptions.get(sessionId);
                if (subscriptions != null) {
                    String destination = subscriptions.remove(subscriptionId);
                    
                    log.debug("✅ destination 구독 해제 처리 완료: sessionId={}, username={}, destination={}", 
                            sessionId, userInfo.getUsername(), destination);
                }
            } else {
                log.warn("⚠️ 구독 해제 이벤트의 사용자 정보를 찾을 수 없음: sessionId={}", sessionId);
            }

        } catch (Exception e) {
            log.error("❌ destination 구독 해제 처리 중 오류 발생: sessionId={}, subscriptionId={}, error={}", 
                    sessionId, subscriptionId, e.getMessage(), e);
        }
    }

    /**
     * 현재 연결된 모든 사용자 정보를 반환하는 메서드
     * 
     * @return 연결된 사용자 정보 맵
     */
    public ConcurrentMap<String, UserSessionInfo> getConnectedUsers() {
        return new ConcurrentHashMap<>(sessionUserMap);
    }

    /**
     * 특정 세션의 사용자 정보를 반환하는 메서드
     * 
     * @param sessionId 조회할 세션 ID
     * @return 사용자 정보 (없으면 null)
     */
    public UserSessionInfo getUserInfo(String sessionId) {
        return sessionUserMap.get(sessionId);
    }

    /**
     * 특정 세션의 구독 정보를 반환하는 메서드
     * 
     * @param sessionId 조회할 세션 ID
     * @return 구독 정보 맵 (구독 ID → destination)
     */
    public ConcurrentMap<String, String> getSessionSubscriptions(String sessionId) {
        return sessionSubscriptions.getOrDefault(sessionId, new ConcurrentHashMap<>());
    }

    /**
     * 현재 연결된 세션 수를 반환하는 메서드
     * SessionCountService를 통해 정확한 세션 수를 반환합니다.
     * 
     * @return 연결된 세션 수
     */
    public int getConnectedSessionCount() {
        return sessionCountService.getConnectedSessionCount();
    }

    /**
     * 사용자 세션 정보를 저장하는 내부 클래스
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserSessionInfo {
        private String sessionId;
        private String username;
        private long connectedAt;
        private String remoteAddress;
        private String userAgent;
        private Object additionalInfo;
    }
} 