package com.example.websockettest.service;

import com.example.websockettest.dto.ScheduledMessageDto;
import com.example.websockettest.dto.SystemStatusDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * WebSocketSchedulerService는 주기적으로 WebSocket 메시지를 전송하는 스케줄러 서비스입니다.
 * 
 * 주요 기능:
 * 1. 고정 간격으로 모든 활성 세션에게 메시지 브로드캐스트
 * 2. 실시간 서버 시간 정보 제공
 * 3. 연결된 클라이언트 수 정보 제공
 * 
 * Spring의 @Scheduled 어노테이션을 사용하여 백그라운드에서 자동 실행됩니다.
 * Jackson ObjectMapper를 사용하여 타입 안전한 JSON 생성을 지원합니다.
 * 
 * @Service: Spring의 서비스 컴포넌트로 등록
 * @RequiredArgsConstructor: Lombok을 사용한 생성자 기반 의존성 주입
 * @Slf4j: Lombok의 로깅 기능 사용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketSchedulerService {

    /**
     * WebSocket 관련 비즈니스 로직을 처리하는 서비스
     * broadcastMessage 메서드를 통해 모든 활성 세션에 메시지 전송
     */
    private final WebSocketService webSocketService;

    /**
     * Jackson ObjectMapper - JSON 직렬화/역직렬화 담당
     * Spring Boot에서 자동으로 Bean으로 등록되어 의존성 주입됨
     */
    private final ObjectMapper objectMapper;

    /**
     * 날짜/시간 포맷터 (한국 시간 형식)
     * 예: 2024-01-15 14:30:25
     */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 5초마다 모든 활성 WebSocket 세션에게 실시간 정보를 브로드캐스트하는 메서드
     * 
     * @Scheduled 어노테이션 옵션:
     * - fixedRate = 5000: 5000ms(5초) 간격으로 실행
     * - 이전 실행이 완료되지 않아도 다음 실행이 시작됨
     * 
     * 대안 옵션들:
     * - fixedDelay = 5000: 이전 실행 완료 후 5초 대기
     * - cron = "0/5 * * * * *": cron 표현식 사용 (5초마다)
     * 
     * 개선사항:
     * - DTO 클래스와 ObjectMapper 사용으로 타입 안전성 확보
     * - 하드코딩된 JSON 문자열 생성 방식 제거
     * - 코드 가독성 및 유지보수성 향상
     */
    @Scheduled(fixedRate = 5000)
    public void sendScheduledMessage() {
        try {
            // 현재 활성 세션 수 조회
            int activeSessionCount = webSocketService.getActiveSessionCount();
            
            // 활성 세션이 없으면 브로드캐스트 건너뛰기
            if (activeSessionCount == 0) {
                log.debug("⏱️ 스케줄러: 활성 세션이 없어 브로드캐스트를 건너뜁니다.");
                return;
            }

            // 현재 시간 정보
            LocalDateTime now = LocalDateTime.now();
            String currentTime = now.format(FORMATTER);
            
            // DTO 객체를 사용하여 메시지 데이터 구성 (Builder 패턴 사용)
            ScheduledMessageDto messageDto = ScheduledMessageDto.builder()
                    .type("scheduled_message")
                    .timestamp(System.currentTimeMillis())
                    .serverTime(currentTime)
                    .activeSessions(activeSessionCount)
                    .message("🔔 5초 주기 자동 메시지입니다!")
                    .build();

            // ObjectMapper를 사용하여 DTO를 JSON 문자열로 변환
            String jsonMessage = objectMapper.writeValueAsString(messageDto);

            log.info("⏱️ 스케줄러: 브로드캐스트 메시지 전송 시작 - 활성세션: {}, 시간: {}", 
                    activeSessionCount, currentTime);
            log.debug("⏱️ 스케줄러: 전송할 JSON 메시지: {}", jsonMessage);

            // 모든 활성 세션에게 메시지 브로드캐스트
            webSocketService.broadcastMessage(jsonMessage);

            log.info("✅ 스케줄러: 브로드캐스트 메시지 전송 완료 - 대상세션: {}", activeSessionCount);

        } catch (JsonProcessingException e) {
            log.error("❌ 스케줄러: JSON 직렬화 오류 발생: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ 스케줄러: 브로드캐스트 메시지 전송 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 매 분마다 시스템 상태 정보를 브로드캐스트하는 메서드 (선택사항)
     * 
     * 더 상세한 시스템 정보를 주기적으로 전송하고 싶을 때 사용
     * 현재는 주석 처리되어 있으며, 필요시 활성화 가능
     * 
     * 개선사항:
     * - DTO 클래스와 ObjectMapper 사용
     * - 타입 안전성 및 코드 가독성 향상
     */
    // @Scheduled(fixedRate = 60000) // 1분마다
    public void sendSystemStatusMessage() {
        try {
            int activeSessionCount = webSocketService.getActiveSessionCount();
            
            if (activeSessionCount == 0) {
                return;
            }

            // 시스템 정보 수집
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            // DTO 객체를 사용하여 시스템 상태 데이터 구성
            SystemStatusDto statusDto = SystemStatusDto.builder()
                    .type("system_status")
                    .timestamp(System.currentTimeMillis())
                    .activeSessions(activeSessionCount)
                    .memoryUsedMb(usedMemory / 1024.0 / 1024.0)
                    .memoryTotalMb(totalMemory / 1024.0 / 1024.0)
                    .memoryUsagePercent(((double) usedMemory / totalMemory) * 100)
                    .build();

            // ObjectMapper를 사용하여 DTO를 JSON 문자열로 변환
            String jsonMessage = objectMapper.writeValueAsString(statusDto);

            webSocketService.broadcastMessage(jsonMessage);
            
            log.info("📊 시스템 상태 메시지 브로드캐스트 완료 - 대상세션: {}", activeSessionCount);
            log.debug("📊 시스템 상태 JSON: {}", jsonMessage);

        } catch (JsonProcessingException e) {
            log.error("❌ 시스템 상태 메시지: JSON 직렬화 오류: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ 시스템 상태 메시지 브로드캐스트 중 오류: {}", e.getMessage(), e);
        }
    }
} 