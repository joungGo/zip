package com.example.websockettest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅방 메시지를 위한 DTO 클래스
 * 룸별 메시지 송수신에 사용됩니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomMessageDto {
    
    /**
     * 메시지 타입 열거형
     */
    public enum MessageType {
        CHAT,           // 일반 채팅 메시지
        JOIN,           // 입장 메시지
        LEAVE,          // 퇴장 메시지
        SYSTEM,         // 시스템 메시지
        NOTIFICATION    // 알림 메시지
    }
    
    // 기본 필드들
    @JsonProperty("type")
    private MessageType type;
    
    @JsonProperty("roomId")
    private String roomId;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("sender")
    private String sender;
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    // 추가 데이터를 위한 필드 (참가자 수, 메타데이터 등)
    @JsonProperty("participantCount")
    private Integer participantCount;
    
    @JsonProperty("extraData")
    private Object extraData;
    
    /**
     * 채팅 메시지 생성을 위한 편의 메서드
     */
    public static RoomMessageDto createChatMessage(String roomId, String message, String sender, String sessionId) {
        return RoomMessageDto.builder()
                .type(MessageType.CHAT)
                .roomId(roomId)
                .message(message)
                .sender(sender)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 시스템 메시지 생성을 위한 편의 메서드
     */
    public static RoomMessageDto createSystemMessage(String roomId, String message) {
        return RoomMessageDto.builder()
                .type(MessageType.SYSTEM)
                .roomId(roomId)
                .message(message)
                .sender("System")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 입장 메시지 생성을 위한 편의 메서드
     */
    public static RoomMessageDto createJoinMessage(String roomId, String sender, String sessionId, int participantCount) {
        return RoomMessageDto.builder()
                .type(MessageType.JOIN)
                .roomId(roomId)
                .message(sender + "님이 채팅방에 입장하셨습니다.")
                .sender(sender)
                .sessionId(sessionId)
                .participantCount(participantCount)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 퇴장 메시지 생성을 위한 편의 메서드
     */
    public static RoomMessageDto createLeaveMessage(String roomId, String sender, String sessionId, int participantCount) {
        return RoomMessageDto.builder()
                .type(MessageType.LEAVE)
                .roomId(roomId)
                .message(sender + "님이 채팅방을 나가셨습니다.")
                .sender(sender)
                .sessionId(sessionId)
                .participantCount(participantCount)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 알림 메시지 생성을 위한 편의 메서드
     */
    public static RoomMessageDto createNotificationMessage(String roomId, String message) {
        return RoomMessageDto.builder()
                .type(MessageType.NOTIFICATION)
                .roomId(roomId)
                .message(message)
                .sender("System")
                .timestamp(LocalDateTime.now())
                .build();
    }
} 