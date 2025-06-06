package com.example.websockettest.service;

import com.example.websockettest.dto.ScheduledMessageDto;
import com.example.websockettest.dto.StompMessage;
import com.example.websockettest.dto.SystemStatusDto;
import com.example.websockettest.controller.StompEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * WebSocketSchedulerService는 주기적으로 STOMP WebSocket 메시지를 전송하는 스케줄러 서비스입니다.
 * 
 * 주요 기능:
 * 1. 고정 간격으로 모든 활성 세션에게 하트비트 메시지 브로드캐스트
 * 2. 실시간 서버 시간 및 시스템 상태 정보 제공
 * 3. 연결된 클라이언트 수 정보 제공
 * 4. STOMP destination 기반 메시지 라우팅
 * 
 * STOMP 개선사항:
 * - destination 패턴 기반 메시지 전송 (/topic/heartbeat, /topic/status)
 * - 구조화된 StompMessage DTO 사용
 * - 메시지 타입별 분리 처리
 * - 우선순위 기반 메시지 분류
 * 
 * Spring의 @Scheduled 어노테이션을 사용하여 백그라운드에서 자동 실행됩니다.
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
     * STOMP WebSocket 관련 비즈니스 로직을 처리하는 서비스
     * STOMP destination 기반 메시지 전송을 담당
     */
    private final WebSocketService webSocketService;

    /**
     * STOMP 이벤트 리스너 - 실시간 세션 정보 조회용
     * 더 정확한 연결 상태 파악을 위해 사용
     */
    private final StompEventListener stompEventListener;

    /**
     * 날짜/시간 포맷터 (한국 시간 형식)
     * 예: 2024-01-15 14:30:25
     */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 하트비트 간격 (초)
     */
    private static final int HEARTBEAT_INTERVAL_SECONDS = 5;

    /**
     * 5초마다 모든 활성 STOMP 세션에게 하트비트 메시지를 브로드캐스트하는 메서드
     * 
     * STOMP 개선사항:
     * - /topic/heartbeat destination으로 전송
     * - ScheduledMessageDto의 정적 팩토리 메서드 활용
     * - 구조화된 메시지 포맷으로 클라이언트 처리 용이
     * 
     * @Scheduled 어노테이션 옵션:
     * - fixedRate = 5000: 5000ms(5초) 간격으로 실행
     * - 이전 실행이 완료되지 않아도 다음 실행이 시작됨
     */
    @Scheduled(fixedRate = 5000)
    public void sendHeartbeatMessage() {
        try {
            // 현재 활성 세션 수 조회 (두 가지 소스에서 확인) >> why? Client가 연결/해제 할 경우 Event 발생시 count한 값 vs
            int serviceSessionCount = webSocketService.getActiveSessionCount();
            int listenerSessionCount = stompEventListener.getConnectedSessionCount();
            
            // 더 정확한 세션 수 사용 (리스너가 더 실시간)
            int activeSessionCount = Math.max(serviceSessionCount, listenerSessionCount);
            
            // 활성 세션이 없으면 하트비트 전송 건너뛰기
            if (activeSessionCount == 0) {
                log.debug("💗 하트비트 스케줄러: 활성 세션이 없어 하트비트를 건너뜁니다.");
                return;
            }

            log.info("💗 하트비트 스케줄러: 하트비트 전송 시작 - 활성세션: {} (service: {}, listener: {})", 
                    activeSessionCount, serviceSessionCount, listenerSessionCount);

            // ScheduledMessageDto의 정적 팩토리 메서드를 사용하여 하트비트 메시지 생성
            StompMessage heartbeatMessage = ScheduledMessageDto.createHeartbeatMessage(
                    activeSessionCount, HEARTBEAT_INTERVAL_SECONDS);

            // /topic/heartbeat destination으로 하트비트 브로드캐스트
            webSocketService.sendHeartbeat(HEARTBEAT_INTERVAL_SECONDS);

            log.info("✅ 하트비트 스케줄러: 하트비트 전송 완료 - 대상세션: {}, messageId: {}", 
                    activeSessionCount, heartbeatMessage.getMessageId());

        } catch (Exception e) {
            log.error("❌ 하트비트 스케줄러: 하트비트 전송 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 30초마다 시스템 상태 정보를 브로드캐스트하는 메서드
     * 
     * STOMP 개선사항:
     * - /topic/status destination으로 전송
     * - SystemStatusDto의 정적 팩토리 메서드 활용
     * - 자동 시스템 정보 수집 및 전송
     * 
     * @Scheduled 어노테이션:
     * - fixedRate = 30000: 30초 간격으로 실행
     */
    @Scheduled(fixedRate = 30000)
    public void sendSystemStatusMessage() {
        try {
            // 현재 활성 세션 수 조회
            int serviceSessionCount = webSocketService.getActiveSessionCount();
            int listenerSessionCount = stompEventListener.getConnectedSessionCount();
            int activeSessionCount = Math.max(serviceSessionCount, listenerSessionCount);
            
            // 활성 세션이 없으면 상태 정보 전송 건너뛰기
            if (activeSessionCount == 0) {
                log.debug("📊 시스템 상태 스케줄러: 활성 세션이 없어 상태 정보 전송을 건너뜁니다.");
                return;
            }

            log.info("📊 시스템 상태 스케줄러: 시스템 상태 브로드캐스트 시작 - 활성세션: {}", 
                    activeSessionCount);

            // WebSocketService의 브로드캐스트 메서드를 통해 시스템 상태 전송
            // 내부적으로 SystemStatusDto.collectCurrentStatus()와 createStatusMessage() 사용
            webSocketService.broadcastSystemStatus();

            log.info("✅ 시스템 상태 스케줄러: 시스템 상태 브로드캐스트 완료 - 대상세션: {}", 
                    activeSessionCount);

        } catch (Exception e) {
            log.error("❌ 시스템 상태 스케줄러: 시스템 상태 브로드캐스트 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 5분마다 서버 공지사항을 브로드캐스트하는 메서드
     * 
     * STOMP 개선사항:
     * - /topic/notifications destination으로 전송
     * - 우선순위 기반 메시지 분류
     * - 공지사항 타입 메시지 전송
     * 
     * @Scheduled 어노테이션:
     * - fixedRate = 300000: 5분(300초) 간격으로 실행
     */
    @Scheduled(fixedRate = 300000)
    public void sendPeriodicAnnouncement() {
        try {
            int activeSessionCount = Math.max(
                    webSocketService.getActiveSessionCount(), 
                    stompEventListener.getConnectedSessionCount());
            
            // 활성 세션이 없으면 공지사항 전송 건너뛰기
            if (activeSessionCount == 0) {
                log.debug("📢 공지사항 스케줄러: 활성 세션이 없어 공지사항 전송을 건너뜁니다.");
                return;
            }

            String currentTime = LocalDateTime.now().format(FORMATTER);
            String announcement = String.format(
                    "📢 정기 공지: 현재 시간 %s, 접속자 %d명이 온라인입니다. 서버가 정상 운영 중입니다.", 
                    currentTime, activeSessionCount);

            log.info("📢 공지사항 스케줄러: 정기 공지사항 브로드캐스트 시작 - 활성세션: {}", 
                    activeSessionCount);

            // ScheduledMessageDto의 정적 팩토리 메서드를 사용하여 공지사항 생성 및 전송
            StompMessage announcementMessage = ScheduledMessageDto.createAnnouncementMessage(
                    announcement, 1); // 보통 우선순위

            // /topic/notifications destination으로 공지사항 브로드캐스트
            webSocketService.broadcastNotification(announcement, null);

            log.info("✅ 공지사항 스케줄러: 정기 공지사항 브로드캐스트 완료 - 대상세션: {}, messageId: {}", 
                    activeSessionCount, announcementMessage.getMessageId());

        } catch (Exception e) {
            log.error("❌ 공지사항 스케줄러: 정기 공지사항 브로드캐스트 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 매일 오전 9시에 일일 리포트를 브로드캐스트하는 메서드 (선택사항)
     * 
     * STOMP 개선사항:
     * - cron 표현식을 사용한 정확한 시간 스케줄링
     * - 높은 우선순위의 시스템 메시지
     * 
     * @Scheduled 어노테이션:
     * - cron = "0 0 9 * * ?": 매일 오전 9시 실행
     * - 현재는 주석 처리되어 있으며, 필요시 활성화 가능
     */
    // @Scheduled(cron = "0 0 9 * * ?")
    public void sendDailyReport() {
        try {
            int activeSessionCount = Math.max(
                    webSocketService.getActiveSessionCount(), 
                    stompEventListener.getConnectedSessionCount());

            String dailyReport = String.format(
                    "🌅 일일 리포트: 오늘도 좋은 하루 되세요! 현재 %d명이 접속 중입니다. " +
                    "총 처리된 메시지: %d건", 
                    activeSessionCount, webSocketService.getTotalMessagesProcessed());

            log.info("🌅 일일 리포트 스케줄러: 일일 리포트 브로드캐스트 시작");

            // 높은 우선순위의 일일 리포트 전송
            StompMessage dailyReportMessage = ScheduledMessageDto.createAnnouncementMessage(
                    dailyReport, 2); // 높은 우선순위

            webSocketService.broadcastNotification(dailyReport, null);

            log.info("✅ 일일 리포트 스케줄러: 일일 리포트 브로드캐스트 완료 - messageId: {}", 
                    dailyReportMessage.getMessageId());

        } catch (Exception e) {
            log.error("❌ 일일 리포트 스케줄러: 일일 리포트 브로드캐스트 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 유지보수 알림을 전송하는 메서드 (수동 호출용)
     * 
     * 관리자가 수동으로 호출하거나 외부 시스템에서 트리거할 수 있는 메서드
     * 
     * @param maintenanceTime 예정된 유지보수 시간
     * @param message 유지보수 관련 메시지
     */
    public void sendMaintenanceNotification(String maintenanceTime, String message) {
        try {
            int activeSessionCount = Math.max(
                    webSocketService.getActiveSessionCount(), 
                    stompEventListener.getConnectedSessionCount());

            if (activeSessionCount == 0) {
                log.info("🔧 유지보수 알림: 활성 세션이 없어 알림을 건너뜁니다.");
                return;
            }

            log.info("🔧 유지보수 알림: 유지보수 알림 브로드캐스트 시작 - 활성세션: {}", 
                    activeSessionCount);

            // ScheduledMessageDto의 정적 팩토리 메서드를 사용하여 유지보수 알림 생성
            StompMessage maintenanceMessage = ScheduledMessageDto.createMaintenanceMessage(
                    message, maintenanceTime);

            // 높은 우선순위로 유지보수 알림 브로드캐스트
            webSocketService.broadcastNotification(message, maintenanceMessage.getExtraData());

            log.info("✅ 유지보수 알림: 유지보수 알림 브로드캐스트 완료 - 대상세션: {}, messageId: {}", 
                    activeSessionCount, maintenanceMessage.getMessageId());

        } catch (Exception e) {
            log.error("❌ 유지보수 알림: 유지보수 알림 브로드캐스트 중 오류 발생: {}", e.getMessage(), e);
        }
    }
} 