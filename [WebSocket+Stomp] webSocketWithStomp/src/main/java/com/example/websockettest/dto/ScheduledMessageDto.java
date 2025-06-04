package com.example.websockettest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 스케줄러에서 전송하는 메시지의 데이터 구조를 정의하는 DTO 클래스
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
     * 메시지 타입 구분자
     * 클라이언트에서 메시지 종류를 구분하기 위해 사용
     */
    private String type;
    
    /**
     * Unix 타임스탬프 (밀리초 단위)
     * 메시지 생성 시점의 정확한 시간
     */
    private Long timestamp;
    
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
     * 사용자에게 표시할 메시지 내용
     */
    private String message;
} 