package com.example.websockettest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 시스템 상태 정보를 전송하는 메시지의 데이터 구조를 정의하는 DTO 클래스
 * 
 * JSON 직렬화/역직렬화를 위해 Jackson 라이브러리와 함께 사용됩니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusDto {
    
    /**
     * 메시지 타입 구분자
     */
    private String type;
    
    /**
     * Unix 타임스탬프 (밀리초 단위)
     */
    private Long timestamp;
    
    /**
     * 현재 활성 WebSocket 세션 수
     * JSON에서는 "active_sessions"으로 직렬화됨
     */
    @JsonProperty("active_sessions")
    private Integer activeSessions;
    
    /**
     * 사용 중인 메모리 (MB 단위)
     * JSON에서는 "memory_used_mb"로 직렬화됨
     */
    @JsonProperty("memory_used_mb")
    private Double memoryUsedMb;
    
    /**
     * 전체 메모리 (MB 단위)
     * JSON에서는 "memory_total_mb"로 직렬화됨
     */
    @JsonProperty("memory_total_mb")
    private Double memoryTotalMb;
    
    /**
     * 메모리 사용률 (퍼센트)
     * JSON에서는 "memory_usage_percent"로 직렬화됨
     */
    @JsonProperty("memory_usage_percent")
    private Double memoryUsagePercent;
} 