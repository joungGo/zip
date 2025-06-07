package com.example.websockettest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 시스템 상태 정보를 전송하는 메시지의 데이터 구조를 정의하는 DTO 클래스
 * STOMP 메시지의 extraData 필드에 포함되거나 독립적으로 사용됩니다.
 * 
 * JSON 직렬화/역직렬화를 위해 Jackson 라이브러리와 함께 사용됩니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusDto {
    
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
    
    /**
     * 서버 가동 시간 (밀리초)
     * JSON에서는 "uptime_ms"로 직렬화됨
     */
    @JsonProperty("uptime_ms")
    private Long uptimeMs;
    
    /**
     * CPU 사용률 (퍼센트)
     * JSON에서는 "cpu_usage_percent"로 직렬화됨
     */
    @JsonProperty("cpu_usage_percent")
    private Double cpuUsagePercent;
    
    /**
     * 총 처리된 메시지 수
     * JSON에서는 "total_messages_processed"로 직렬화됨
     */
    @JsonProperty("total_messages_processed")
    private Long totalMessagesProcessed;
    
    /**
     * 서버 상태 (RUNNING, MAINTENANCE, SHUTTING_DOWN 등)
     * JSON에서는 "server_status"로 직렬화됨
     */
    @JsonProperty("server_status")
    private String serverStatus;
    
    /**
     * 시스템 상태 정보를 포함한 STOMP 메시지를 생성하는 정적 팩토리 메서드
     * 
     * @param statusData 시스템 상태 데이터
     * @return STATUS 타입의 STOMP 메시지
     */
    public static StompMessage createStatusMessage(SystemStatusDto statusData) {
        return StompMessage.builder()
                .type(StompMessage.MessageType.STATUS)
                .senderId("SYSTEM")
                .content("System status information")
                .extraData(statusData)
                .timestamp(System.currentTimeMillis())
                .priority(1)
                .build();
    }
    
    /**
     * 현재 시스템 상태를 수집하는 정적 메서드
     * 
     * @param activeSessions 활성 세션 수
     * @param totalMessages 총 처리 메시지 수
     * @return 현재 시스템 상태 DTO
     */
    public static SystemStatusDto collectCurrentStatus(int activeSessions, long totalMessages) {
        Runtime runtime = Runtime.getRuntime();
        
        // 메모리 정보 계산
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double totalMemoryMb = totalMemory / (1024.0 * 1024.0);
        double usedMemoryMb = usedMemory / (1024.0 * 1024.0);
        double memoryUsagePercent = (usedMemoryMb / totalMemoryMb) * 100.0;
        
        // JVM 시작 시간으로부터 가동 시간 계산
        long uptimeMs = System.currentTimeMillis() - 
                       java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
        
        return SystemStatusDto.builder()
                .activeSessions(activeSessions)
                .memoryUsedMb(Math.round(usedMemoryMb * 100.0) / 100.0)
                .memoryTotalMb(Math.round(totalMemoryMb * 100.0) / 100.0)
                .memoryUsagePercent(Math.round(memoryUsagePercent * 100.0) / 100.0)
                .uptimeMs(uptimeMs)
                .cpuUsagePercent(0.0) // CPU 사용률은 복잡한 계산이 필요하므로 일단 0으로 설정
                .totalMessagesProcessed(totalMessages)
                .serverStatus("RUNNING")
                .build();
    }
} 