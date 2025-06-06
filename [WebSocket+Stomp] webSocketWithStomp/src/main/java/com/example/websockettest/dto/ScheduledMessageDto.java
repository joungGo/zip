package com.example.websockettest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 스케줄러에서 전송하는 메시지의 데이터 구조를 정의하는 DTO 클래스
 * STOMP 메시지의 extraData 필드에 포함되거나 독립적으로 사용됩니다.
 * 
 * JSON 직렬화/역직렬화를 위해 Jackson 라이브러리와 함께 사용됩니다.
 * 
 * @Data: Lombok - getter, setter, toString, equals, hashCode 자동 생성
 * @Builder: Lombok - 빌더 패턴 지원
 * @NoArgsConstructor: Lombok - 기본 생성자 생성 (Jackson 역직렬화용)
 * @AllArgsConstructor: Lombok - 모든 필드를 받는 생성자 생성
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledMessageDto {
    
    /**
     * 스케줄된 메시지의 타입
     */
    public enum ScheduleType {
        HEARTBEAT,      // 하트비트 메시지
        STATUS_UPDATE,  // 상태 업데이트
        ANNOUNCEMENT,   // 공지사항
        REMINDER,       // 리마인더
        MAINTENANCE     // 유지보수 알림
    }
    
    /**
     * 스케줄 메시지 타입
     */
    @JsonProperty("schedule_type")
    private ScheduleType scheduleType;
    
    /**
     * 사람이 읽기 쉬운 형태의 서버 시간
     * 예: "2024-01-15 14:30:25"
     * JSON에서는 "server_time"으로 직렬화됨
     */
    @JsonProperty("server_time")
    private String serverTime;
    
    /**
     * 현재 활성 WebSocket 세션 수
     * JSON에서는 "active_sessions"으로 직렬화됨
     */
    @JsonProperty("active_sessions")
    private Integer activeSessions;
    
    /**
     * 메시지 내용
     */
    private String message;
    
    /**
     * 스케줄러 실행 간격 (초)
     * JSON에서는 "interval_seconds"로 직렬화됨
     */
    @JsonProperty("interval_seconds")
    private Integer intervalSeconds;
    
    /**
     * 다음 실행 예정 시간
     * JSON에서는 "next_execution"으로 직렬화됨
     */
    @JsonProperty("next_execution")
    private String nextExecution;
    
    /**
     * 하트비트 메시지를 생성하는 정적 팩토리 메서드
     *
     * @param activeSessions 현재 활성 세션 수
     * @param intervalSeconds 하트비트 간격
     * @return HEARTBEAT 타입의 STOMP 메시지
     */
    public static StompMessage createHeartbeatMessage(int activeSessions, int intervalSeconds) {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String nextTime = LocalDateTime.now().plusSeconds(intervalSeconds)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        ScheduledMessageDto data = ScheduledMessageDto.builder()
                .scheduleType(ScheduleType.HEARTBEAT)
                .serverTime(currentTime)
                .activeSessions(activeSessions)
                .message("Server heartbeat - system is running normally")
                .intervalSeconds(intervalSeconds)
                .nextExecution(nextTime)
                .build();
        
        return StompMessage.builder()
                .type(StompMessage.MessageType.HEARTBEAT)
                .senderId("SCHEDULER")
                .content("Heartbeat")
                .extraData(data)
                .timestamp(System.currentTimeMillis())
                .priority(0)
                .build();
    }
    
    /**
     * 상태 업데이트 메시지를 생성하는 정적 팩토리 메서드
     * 
     * @param activeSessions 현재 활성 세션 수
     * @param customMessage 사용자 정의 메시지
     * @return STATUS 타입의 STOMP 메시지
     */
    public static StompMessage createStatusUpdateMessage(int activeSessions, String customMessage) {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        ScheduledMessageDto data = ScheduledMessageDto.builder()
                .scheduleType(ScheduleType.STATUS_UPDATE)
                .serverTime(currentTime)
                .activeSessions(activeSessions)
                .message(customMessage != null ? customMessage : "Scheduled status update")
                .build();
        
        return StompMessage.builder()
                .type(StompMessage.MessageType.STATUS)
                .senderId("SCHEDULER")
                .content("Status Update")
                .extraData(data)
                .timestamp(System.currentTimeMillis())
                .priority(1)
                .build();
    }
    
    /**
     * 공지사항 메시지를 생성하는 정적 팩토리 메서드
     * 
     * @param announcement 공지 내용
     * @param priority 우선순위 (0: 낮음, 1: 보통, 2: 높음)
     * @return NOTIFICATION 타입의 STOMP 메시지
     */
    public static StompMessage createAnnouncementMessage(String announcement, int priority) {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        ScheduledMessageDto data = ScheduledMessageDto.builder()
                .scheduleType(ScheduleType.ANNOUNCEMENT)
                .serverTime(currentTime)
                .message(announcement)
                .build();
        
        return StompMessage.builder()
                .type(StompMessage.MessageType.NOTIFICATION)
                .senderId("SCHEDULER")
                .content(announcement)
                .extraData(data)
                .timestamp(System.currentTimeMillis())
                .priority(priority)
                .build();
    }
    
    /**
     * 유지보수 알림 메시지를 생성하는 정적 팩토리 메서드
     * 
     * @param maintenanceMessage 유지보수 관련 메시지
     * @param scheduledTime 예정된 유지보수 시간
     * @return NOTIFICATION 타입의 STOMP 메시지
     */
    public static StompMessage createMaintenanceMessage(String maintenanceMessage, String scheduledTime) {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        ScheduledMessageDto data = ScheduledMessageDto.builder()
                .scheduleType(ScheduleType.MAINTENANCE)
                .serverTime(currentTime)
                .message(maintenanceMessage)
                .nextExecution(scheduledTime)
                .build();
        
        return StompMessage.builder()
                .type(StompMessage.MessageType.NOTIFICATION)
                .senderId("SCHEDULER")
                .content("Maintenance Notification")
                .extraData(data)
                .timestamp(System.currentTimeMillis())
                .priority(2)
                .build();
    }
} 