package com.example.websockettest.service;

import com.example.websockettest.config.RedisChannelConfig;
import com.example.websockettest.dto.RoomMessageDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Pub/Sub을 통한 STOMP 메시지 발행 서비스
 * 
 * STOMP + Redis 하이브리드 아키텍처에서 로컬 처리된 메시지를 
 * Redis 채널로 발행하여 다른 서버 인스턴스들에게 전파합니다.
 * 
 * 메시지 플로우:
 * 1. 클라이언트 → STOMP → @MessageMapping → 로컬 처리 
 * 2. 로컬 처리 완료 → 이 Publisher → Redis 채널 발행
 * 3. 다른 서버들이 Redis 메시지 수신 → STOMP Topic 브로드캐스트
 * 
 * 지원하는 이벤트:
 * - 룸 채팅 메시지 브로드캐스트
 * - 룸 입장/퇴장 이벤트 전파
 * - 세션 연결/해제 이벤트 동기화
 * - 시스템 알림 전체 브로드캐스트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStompMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate; // Redis 메시지 발행용 템플릿
    private final ObjectMapper objectMapper; // JSON 직렬화용 ObjectMapper

    // ============ 룸 관련 메시지 발행 ============

    /**
     * 룸의 모든 메시지(채팅, 입장, 퇴장)를 다른 서버들에게 전파
     * 
     * @param roomId 룸 ID
     * @param message 룸 메시지 DTO (채팅, 입장, 퇴장 등)
     */
    public void publishRoomMessage(String roomId, RoomMessageDto message) {
        try {
            String channel = RedisChannelConfig.getRoomChannel(roomId); // stomp:room:{roomId} 통합 채널 생성
            String jsonMessage = objectMapper.writeValueAsString(message); // RoomMessageDto를 JSON으로 직렬화
            
            /**
             * convert는 두 번째 인자인 jsonMessage(Java 객체 또는 문자열 등)를 Redis에 전송 가능한 형태로 변환(직렬화)하는 역할을 합니다.
             * 즉, 내부적으로 RedisTemplate이 메시지를 바이트 배열 등 Redis가 처리할 수 있는 데이터로 변환한 뒤, 지정한 채널로 발행(send)합니다.
             */
            redisTemplate.convertAndSend(channel, jsonMessage); // Redis 채널로 메시지 발행
            
            log.debug("룸 메시지 Redis 발행 완료 - 채널: {}, 타입: {}, 발신자: {}", 
                     channel, message.getType(), message.getSender());
            
        } catch (JsonProcessingException e) {
            log.error("룸 메시지 JSON 직렬화 실패 - roomId: {}, message: {}", roomId, message, e);
        } catch (Exception e) {
            log.error("룸 메시지 Redis 발행 실패 - roomId: {}, message: {}", roomId, message, e);
        }
    }

    /**
     * 룸 입장 이벤트 메시지 생성 및 발행
     * 
     * @param roomId 룸 ID
     * @param username 입장한 사용자명
     * @param sessionId 세션 ID
     * @param participantCount 현재 참여자 수
     */
    public void publishJoinEvent(String roomId, String username, String sessionId, int participantCount) {
        try {
            RoomMessageDto joinMessage = RoomMessageDto.createJoinMessage(roomId, username, sessionId, participantCount);
            publishRoomMessage(roomId, joinMessage); // 통합 채널로 발행
            
            log.info("룸 입장 이벤트 Redis 발행 완료 - roomId: {}, user: {}, 참여자수: {}", 
                    roomId, username, participantCount);
            
        } catch (Exception e) {
            log.error("룸 입장 이벤트 Redis 발행 실패 - roomId: {}, user: {}", roomId, username, e);
        }
    }

    /**
     * 룸 퇴장 이벤트 메시지 생성 및 발행
     * 
     * @param roomId 룸 ID
     * @param username 퇴장한 사용자명
     * @param sessionId 세션 ID
     * @param participantCount 현재 참여자 수
     */
    public void publishLeaveEvent(String roomId, String username, String sessionId, int participantCount) {
        try {
            RoomMessageDto leaveMessage = RoomMessageDto.createLeaveMessage(roomId, username, sessionId, participantCount);
            publishRoomMessage(roomId, leaveMessage); // 통합 채널로 발행
            
            log.info("룸 퇴장 이벤트 Redis 발행 완료 - roomId: {}, user: {}, 참여자수: {}", 
                    roomId, username, participantCount);
            
        } catch (Exception e) {
            log.error("룸 퇴장 이벤트 Redis 발행 실패 - roomId: {}, user: {}", roomId, username, e);
        }
    }

    // ============ 세션 관련 이벤트 발행 ============

    /**
     * 세션 연결 이벤트를 다른 서버들에게 전파
     * 
     * @param sessionId 연결된 세션 ID
     * @param username 사용자명 (있는 경우)
     */
    public void publishSessionConnectEvent(String sessionId, String username) {
        try {
            String channel = RedisChannelConfig.CHANNEL_SESSION_CONNECT; // stomp:session:connect 채널
            
            Map<String, Object> connectEvent = new HashMap<>();
            connectEvent.put("type", "SESSION_CONNECT");
            connectEvent.put("sessionId", sessionId);
            connectEvent.put("username", username);
            connectEvent.put("timestamp", LocalDateTime.now().toString());
            connectEvent.put("serverId", RedisChannelConfig.getCurrentServerId());
            
            String jsonMessage = objectMapper.writeValueAsString(connectEvent);
            redisTemplate.convertAndSend(channel, jsonMessage);
            
            log.debug("세션 연결 이벤트 Redis 발행 완료 - sessionId: {}, user: {}", sessionId, username);
            
        } catch (Exception e) {
            log.error("세션 연결 이벤트 Redis 발행 실패 - sessionId: {}", sessionId, e);
        }
    }

    /**
     * 세션 해제 이벤트를 다른 서버들에게 전파
     * 
     * @param sessionId 해제된 세션 ID
     * @param username 사용자명 (있는 경우)
     * @param roomId 마지막 참여 룸 ID (있는 경우)
     */
    public void publishSessionDisconnectEvent(String sessionId, String username, String roomId) {
        try {
            String channel = RedisChannelConfig.CHANNEL_SESSION_DISCONNECT; // stomp:session:disconnect 채널
            
            Map<String, Object> disconnectEvent = new HashMap<>();
            disconnectEvent.put("type", "SESSION_DISCONNECT");
            disconnectEvent.put("sessionId", sessionId);
            disconnectEvent.put("username", username);
            disconnectEvent.put("roomId", roomId);
            disconnectEvent.put("timestamp", LocalDateTime.now().toString());
            disconnectEvent.put("serverId", RedisChannelConfig.getCurrentServerId());
            
            String jsonMessage = objectMapper.writeValueAsString(disconnectEvent);
            redisTemplate.convertAndSend(channel, jsonMessage);
            
            log.debug("세션 해제 이벤트 Redis 발행 완료 - sessionId: {}, user: {}, room: {}", 
                     sessionId, username, roomId);
            
        } catch (Exception e) {
            log.error("세션 해제 이벤트 Redis 발행 실패 - sessionId: {}", sessionId, e);
        }
    }

    // ============ 전역 브로드캐스트 ============

    /**
     * 전역 브로드캐스트 메시지를 모든 서버들에게 전파
     * 시스템 공지사항, 긴급 알림 등에 사용
     * 
     * @param message 브로드캐스트할 메시지
     * @param messageType 메시지 타입 (SYSTEM, ANNOUNCEMENT 등)
     */
    public void publishGlobalBroadcast(String message, String messageType) {
        try {
            String channel = RedisChannelConfig.CHANNEL_GLOBAL_BROADCAST; // stomp:broadcast:global 채널
            
            Map<String, Object> globalMessage = new HashMap<>();
            globalMessage.put("type", messageType);
            globalMessage.put("message", message);
            globalMessage.put("timestamp", LocalDateTime.now().toString());
            globalMessage.put("serverId", RedisChannelConfig.getCurrentServerId());
            
            String jsonMessage = objectMapper.writeValueAsString(globalMessage);
            redisTemplate.convertAndSend(channel, jsonMessage);
            
            log.info("전역 브로드캐스트 Redis 발행 완료 - 타입: {}, 메시지: {}", messageType, message);
            
        } catch (Exception e) {
            log.error("전역 브로드캐스트 Redis 발행 실패 - 메시지: {}", message, e);
        }
    }

    /**
     * 시스템 알림을 모든 서버들에게 전파
     * 서버 상태 변경, 유지보수 알림 등에 사용
     * 
     * @param notification 알림 내용
     * @param level 알림 레벨 (INFO, WARNING, ERROR)
     */
    public void publishSystemNotification(String notification, String level) {
        try {
            String channel = RedisChannelConfig.CHANNEL_SYSTEM_NOTIFICATIONS; // stomp:system:notifications 채널
            
            Map<String, Object> systemNotification = new HashMap<>();
            systemNotification.put("type", "SYSTEM_NOTIFICATION");
            systemNotification.put("notification", notification);
            systemNotification.put("level", level);
            systemNotification.put("timestamp", LocalDateTime.now().toString());
            systemNotification.put("serverId", RedisChannelConfig.getCurrentServerId());
            
            String jsonMessage = objectMapper.writeValueAsString(systemNotification);
            redisTemplate.convertAndSend(channel, jsonMessage);
            
            log.info("시스템 알림 Redis 발행 완료 - 레벨: {}, 알림: {}", level, notification);
            
        } catch (Exception e) {
            log.error("시스템 알림 Redis 발행 실패 - 알림: {}", notification, e);
        }
    }
} 