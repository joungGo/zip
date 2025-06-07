package com.example.websockettest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * STOMP 메시지의 기본 구조를 정의하는 DTO 클래스
 * 모든 STOMP 메시지의 공통 필드를 포함합니다.
 * 
 * JSON 직렬화/역직렬화를 위해 Jackson 라이브러리와 함께 사용됩니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StompMessage {
    
    /**
     * 메시지 타입 열거형
     * 클라이언트가 메시지 종류를 구분하기 위해 사용
     */
    public enum MessageType {
        CHAT,           // 일반 채팅 메시지
        PRIVATE,        // 개인 메시지
        SYSTEM,         // 시스템 메시지
        NOTIFICATION,   // 알림 메시지
        STATUS,         // 상태 정보
        ERROR,          // 에러 메시지
        ECHO,           // 에코 메시지
        USER_JOIN,      // 사용자 입장
        USER_LEAVE,     // 사용자 퇴장
        HEARTBEAT       // 하트비트
    }
    
    /**
     * 메시지 타입
     */
    private MessageType type;
    
    /**
     * 메시지를 보낸 사용자 ID
     */
    @JsonProperty("sender_id")
    private String senderId;
    
    /**
     * 메시지를 받을 사용자 ID (개인 메시지의 경우)
     */
    @JsonProperty("receiver_id")
    private String receiverId;
    
    /**
     * 메시지 내용
     */
    private String content;
    
    /**
     * Unix 타임스탬프 (밀리초 단위)
     */
    private Long timestamp;
    
    /**
     * 세션 ID (서버에서 추가)
     */
    @JsonProperty("session_id")
    private String sessionId;
    
    /**
     * 메시지 ID (고유 식별자)
     */
    @JsonProperty("message_id")
    private String messageId;
    
    /**
     * 추가 데이터 (JSON 객체로 직렬화)
     * 메시지 타입에 따라 다른 추가 정보를 포함할 수 있음
     */
    @JsonProperty("extra_data")
    private Object extraData;
    
    /**
     * 메시지 우선순위 (0: 낮음, 1: 보통, 2: 높음)
     */
    private Integer priority;
    
    /**
     * TTL (Time To Live) - 메시지 유효 시간 (밀리초)
     */
    private Long ttl;
    
    /**
     * 채팅 메시지 생성을 위한 정적 팩토리 메서드
     */
    public static StompMessage createChatMessage(String senderId, String content) {
        return StompMessage.builder()
                .type(MessageType.CHAT)
                .senderId(senderId)
                .content(content)
                .timestamp(System.currentTimeMillis())
                .priority(1)
                .build();
    }
    
    /**
     * 개인 메시지 생성을 위한 정적 팩토리 메서드
     */
    public static StompMessage createPrivateMessage(String senderId, String receiverId, String content) {
        return StompMessage.builder()
                .type(MessageType.PRIVATE)
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .timestamp(System.currentTimeMillis())
                .priority(1)
                .build();
    }
    
    /**
     * 시스템 메시지 생성을 위한 정적 팩토리 메서드
     */
    public static StompMessage createSystemMessage(String content) {
        return StompMessage.builder()
                .type(MessageType.SYSTEM)
                .senderId("SYSTEM")
                .content(content)
                .timestamp(System.currentTimeMillis())
                .priority(2)
                .build();
    }
    
    /**
     * 알림 메시지 생성을 위한 정적 팩토리 메서드
     */
    public static StompMessage createNotification(String content, Object extraData) {
        return StompMessage.builder()
                .type(MessageType.NOTIFICATION)
                .senderId("SYSTEM")
                .content(content)
                .extraData(extraData)
                .timestamp(System.currentTimeMillis())
                .priority(1)
                .build();
    }
    
    /**
     * 에러 메시지 생성을 위한 정적 팩토리 메서드
     */
    public static StompMessage createErrorMessage(String content, String sessionId) {
        return StompMessage.builder()
                .type(MessageType.ERROR)
                .senderId("SYSTEM")
                .content(content)
                .sessionId(sessionId)
                .timestamp(System.currentTimeMillis())
                .priority(2)
                .build();
    }
} 