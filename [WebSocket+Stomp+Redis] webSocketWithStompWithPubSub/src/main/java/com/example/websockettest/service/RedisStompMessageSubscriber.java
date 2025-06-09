package com.example.websockettest.service;

import com.example.websockettest.config.RedisChannelConfig;
import com.example.websockettest.dto.RoomMessageDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Redis Pub/Sub 메시지 구독 및 STOMP 브로드캐스트 서비스
 * 
 * STOMP + Redis 하이브리드 아키텍처에서 다른 서버 인스턴스들이 
 * Redis 채널로 발행한 메시지를 수신하여 현재 서버의 STOMP 클라이언트들에게 
 * 실시간으로 브로드캐스트합니다.
 * 
 * 메시지 플로우:
 * 1. 다른 서버 → Redis 채널 발행
 * 2. 이 Subscriber → Redis 메시지 수신
 * 3. 메시지 타입별 처리 → STOMP Topic 브로드캐스트
 * 4. 현재 서버의 클라이언트들 → 실시간 메시지 수신
 * 
 * 지원하는 메시지:
 * - 룸 채팅 메시지 릴레이
 * - 룸 입장/퇴장 이벤트 전파
 * - 세션 이벤트 동기화
 * - 시스템 알림 브로드캐스트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStompMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate; // STOMP 메시지 전송용 템플릿
    private final ObjectMapper objectMapper; // JSON 역직렬화용 ObjectMapper

    /**
     * Redis 메시지 수신 시 호출되는 메인 핸들러
     * 채널별로 적절한 처리 메서드로 라우팅
     * 
     * @param message Redis 메시지 (body + channel 정보)
     * @param pattern 구독 패턴 (채널명)
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(pattern); // 채널명 추출
            String messageBody = new String(message.getBody()); // 메시지 본문 추출
            
            log.debug("Redis 메시지 수신 - 채널: {}, 메시지: {}", channel, messageBody);
            
            // 채널 패턴에 따라 적절한 처리 메서드 호출
            if (channel.startsWith("stomp:room:")) {
                handleRoomMessage(channel, messageBody);
            } else if (channel.equals(RedisChannelConfig.CHANNEL_SESSION_CONNECT)) {
                handleSessionConnectEvent(messageBody);
            } else if (channel.equals(RedisChannelConfig.CHANNEL_SESSION_DISCONNECT)) {
                handleSessionDisconnectEvent(messageBody);
            } else if (channel.equals(RedisChannelConfig.CHANNEL_GLOBAL_BROADCAST)) {
                handleGlobalBroadcast(messageBody);
            } else if (channel.equals(RedisChannelConfig.CHANNEL_SYSTEM_NOTIFICATIONS)) {
                handleSystemNotification(messageBody);
            } else {
                log.warn("알 수 없는 Redis 채널 메시지 수신 - 채널: {}", channel);
            }
            
        } catch (Exception e) {
            log.error("Redis 메시지 처리 중 오류 발생", e);
        }
    }

    // ============ 룸 관련 메시지 처리 ============

    /**
     * 룸 통합 메시지 처리 (채팅, 입장, 퇴장 모든 메시지)
     * stomp:room:{roomId} 채널의 메시지를 /topic/room/{roomId}로 STOMP 브로드캐스트
     * 
     * @param channel Redis 채널명
     * @param messageBody JSON 메시지 본문
     */
    private void handleRoomMessage(String channel, String messageBody) {
        try {
            // 채널명에서 roomId 추출 (stomp:room:room1 → room1)
            String roomId = channel.substring("stomp:room:".length());
            
            // JSON을 RoomMessageDto로 역직렬화
            RoomMessageDto roomMessage = objectMapper.readValue(messageBody, RoomMessageDto.class);
            
            // STOMP Topic으로 브로드캐스트 (/topic/room/room1)
            String stompDestination = "/topic/room/" + roomId;
            messagingTemplate.convertAndSend(stompDestination, roomMessage);
            
            log.debug("룸 메시지 STOMP 브로드캐스트 완료 - 룸: {}, 타입: {}, 발신자: {}", 
                     roomId, roomMessage.getType(), roomMessage.getSender());
            
        } catch (JsonProcessingException e) {
            log.error("룸 메시지 JSON 파싱 실패 - 채널: {}, 메시지: {}", channel, messageBody, e);
        } catch (Exception e) {
            log.error("룸 메시지 처리 실패 - 채널: {}", channel, e);
        }
    }

    // ============ 세션 관련 이벤트 처리 ============

    /**
     * 세션 연결 이벤트 처리 (현재는 로깅만, 필요시 확장 가능)
     * 
     * @param messageBody JSON 메시지 본문
     */
    private void handleSessionConnectEvent(String messageBody) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> connectEvent = objectMapper.readValue(messageBody, Map.class);
            
            log.debug("분산 세션 연결 이벤트 수신 - 세션: {}, 사용자: {}, 서버: {}", 
                     connectEvent.get("sessionId"), connectEvent.get("username"), connectEvent.get("serverId"));
            
            // 필요시 세션 통계 업데이트, 전역 알림 등 추가 처리 가능
            
        } catch (Exception e) {
            log.error("세션 연결 이벤트 처리 실패", e);
        }
    }

    /**
     * 세션 해제 이벤트 처리 (현재는 로깅만, 필요시 확장 가능)
     * 
     * @param messageBody JSON 메시지 본문
     */
    private void handleSessionDisconnectEvent(String messageBody) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> disconnectEvent = objectMapper.readValue(messageBody, Map.class);
            
            log.debug("분산 세션 해제 이벤트 수신 - 세션: {}, 사용자: {}, 룸: {}, 서버: {}", 
                     disconnectEvent.get("sessionId"), disconnectEvent.get("username"), 
                     disconnectEvent.get("roomId"), disconnectEvent.get("serverId"));
            
            // 필요시 세션 정리, 통계 업데이트 등 추가 처리 가능
            
        } catch (Exception e) {
            log.error("세션 해제 이벤트 처리 실패", e);
        }
    }

    // ============ 전역 브로드캐스트 처리 ============

    /**
     * 전역 브로드캐스트 메시지 처리
     * 모든 연결된 클라이언트에게 시스템 메시지 전송
     * 
     * @param messageBody JSON 메시지 본문
     */
    private void handleGlobalBroadcast(String messageBody) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> globalMessage = objectMapper.readValue(messageBody, Map.class);
            
            // 전역 브로드캐스트를 /topic/global로 전송
            messagingTemplate.convertAndSend("/topic/global", globalMessage);
            
            log.info("전역 브로드캐스트 STOMP 전송 완료 - 타입: {}, 메시지: {}", 
                    globalMessage.get("type"), globalMessage.get("message"));
            
        } catch (Exception e) {
            log.error("전역 브로드캐스트 처리 실패", e);
        }
    }

    /**
     * 시스템 알림 처리
     * 관리자나 시스템 모니터링 채널로 알림 전송
     * 
     * @param messageBody JSON 메시지 본문
     */
    private void handleSystemNotification(String messageBody) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> notification = objectMapper.readValue(messageBody, Map.class);
            
            // 시스템 알림을 /topic/system으로 전송
            messagingTemplate.convertAndSend("/topic/system", notification);
            
            log.info("시스템 알림 STOMP 전송 완료 - 레벨: {}, 알림: {}", 
                    notification.get("level"), notification.get("notification"));
            
        } catch (Exception e) {
            log.error("시스템 알림 처리 실패", e);
        }
    }
} 