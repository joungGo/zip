package com.example.websockettest.service;

import com.example.websockettest.dto.StompMessage;
import com.example.websockettest.dto.SystemStatusDto;
import com.example.websockettest.repository.WebSocketSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocketService는 STOMP WebSocket 관련 비즈니스 로직을 처리하는 서비스 계층입니다.
 * 
 * 주요 책임:
 * 1. STOMP 메시지 처리 및 라우팅
 * 2. destination 기반 메시지 전송
 * 3. 브로드캐스트 및 개별 사용자 메시지 처리
 * 4. 세션 통계 정보 제공
 * 5. 시스템 상태 관리
 * 
 * STOMP 특화 기능:
 * - SimpMessagingTemplate을 통한 메시지 전송
 * - destination 패턴 기반 라우팅 (/topic, /queue, /user)
 * - 구조화된 메시지 포맷 (StompMessage DTO)
 * 
 * @Service: Spring의 서비스 컴포넌트로 등록
 * @RequiredArgsConstructor: Lombok을 사용한 생성자 기반 의존성 주입
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    /**
     * STOMP 메시지 전송을 위한 템플릿
     * Spring이 자동으로 주입하는 STOMP 메시징 템플릿
     */
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * WebSocket 세션 데이터를 관리하는 리포지토리
     * final 키워드와 @RequiredArgsConstructor로 불변성과 의존성 주입을 보장
     */
    private final WebSocketSessionRepository sessionRepository;
    
    /**
     * 처리된 총 메시지 수를 추적하는 원자적 카운터
     * 멀티스레드 환경에서 안전한 카운팅
     */
    private final AtomicLong totalMessagesProcessed = new AtomicLong(0);

    /**
     * 브로드캐스트 메시지를 모든 구독자에게 전송하는 메서드
     * /topic/messages destination으로 메시지를 전송합니다.
     * 
     * @param content 브로드캐스트할 메시지 내용
     */
    public void broadcastMessage(String content) {
        log.info("📢 브로드캐스트 메시지 전송 시작: content={}", content);
        
        try {
            // CHAT 타입의 STOMP 메시지 생성
            StompMessage stompMessage = StompMessage.createChatMessage("SYSTEM", content);
            stompMessage.setMessageId(generateMessageId());
            
            // /topic/messages로 브로드캐스트
            messagingTemplate.convertAndSend("/topic/messages", stompMessage);
            
            // 메시지 처리 카운터 증가
            totalMessagesProcessed.incrementAndGet();
            
            log.info("✅ 브로드캐스트 메시지 전송 완료: messageId={}, content={}", 
                    stompMessage.getMessageId(), content);
        } catch (Exception e) {
            log.error("❌ 브로드캐스트 메시지 전송 실패: content={}, error={}", 
                    content, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 특정 사용자에게 개인 메시지를 전송하는 메서드
     * /user/{userId}/queue/messages destination으로 메시지를 전송합니다.
     * 
     * @param userId 메시지를 받을 사용자 ID
     * @param content 메시지 내용
     */
    public void sendPrivateMessage(String userId, String content) {
        log.info("📩 개인 메시지 전송 시작: userId={}, content={}", userId, content);
        
        try {
            // PRIVATE 타입의 STOMP 메시지 생성
            StompMessage stompMessage = StompMessage.createPrivateMessage("SYSTEM", userId, content);
            stompMessage.setMessageId(generateMessageId());
            
            // 특정 사용자에게 개인 메시지 전송
            messagingTemplate.convertAndSendToUser(userId, "/queue/messages", stompMessage);
            
            // 메시지 처리 카운터 증가
            totalMessagesProcessed.incrementAndGet();
            
            log.info("✅ 개인 메시지 전송 완료: userId={}, messageId={}, content={}", 
                    userId, stompMessage.getMessageId(), content);
        } catch (Exception e) {
            log.error("❌ 개인 메시지 전송 실패: userId={}, content={}, error={}", 
                    userId, content, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 시스템 공지를 모든 구독자에게 전송하는 메서드
     * /topic/system destination으로 시스템 메시지를 전송합니다.
     * 
     * @param content 시스템 공지 내용
     */
    public void broadcastSystemMessage(String content) {
        log.info("📢 시스템 공지 전송 시작: content={}", content);
        
        try {
            // SYSTEM 타입의 STOMP 메시지 생성
            StompMessage stompMessage = StompMessage.createSystemMessage(content);
            stompMessage.setMessageId(generateMessageId());
            
            // /topic/system으로 시스템 공지 브로드캐스트
            messagingTemplate.convertAndSend("/topic/system", stompMessage);
            
            // 메시지 처리 카운터 증가
            totalMessagesProcessed.incrementAndGet();
            
            log.info("✅ 시스템 공지 전송 완료: messageId={}, content={}", 
                    stompMessage.getMessageId(), content);
        } catch (Exception e) {
            log.error("❌ 시스템 공지 전송 실패: content={}, error={}", 
                    content, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 알림 메시지를 모든 구독자에게 전송하는 메서드
     * /topic/notifications destination으로 알림을 전송합니다.
     * 
     * @param content 알림 내용
     * @param extraData 추가 데이터 (선택사항)
     */
    public void broadcastNotification(String content, Object extraData) {
        log.info("🔔 알림 메시지 전송 시작: content={}", content);
        
        try {
            // NOTIFICATION 타입의 STOMP 메시지 생성
            StompMessage stompMessage = StompMessage.createNotification(content, extraData);
            stompMessage.setMessageId(generateMessageId());
            
            // /topic/notifications로 알림 브로드캐스트
            messagingTemplate.convertAndSend("/topic/notifications", stompMessage);
            
            // 메시지 처리 카운터 증가
            totalMessagesProcessed.incrementAndGet();
            
            log.info("✅ 알림 메시지 전송 완료: messageId={}, content={}", 
                    stompMessage.getMessageId(), content);
        } catch (Exception e) {
            log.error("❌ 알림 메시지 전송 실패: content={}, error={}", 
                    content, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 시스템 상태 정보를 브로드캐스트하는 메서드
     * /topic/status destination으로 시스템 상태를 전송합니다.
     */
    public void broadcastSystemStatus() {
        log.info("📊 시스템 상태 브로드캐스트 시작");
        
        try {
            // 현재 시스템 상태 수집
            int activeSessions = getActiveSessionCount();
            SystemStatusDto statusData = SystemStatusDto.collectCurrentStatus(
                    activeSessions, totalMessagesProcessed.get());
            
            // STATUS 타입의 STOMP 메시지 생성
            StompMessage stompMessage = SystemStatusDto.createStatusMessage(statusData);
            stompMessage.setMessageId(generateMessageId());
            
            // /topic/status로 상태 정보 브로드캐스트
            messagingTemplate.convertAndSend("/topic/status", stompMessage);
            
            log.info("✅ 시스템 상태 브로드캐스트 완료: activeSessions={}, messageId={}", 
                    activeSessions, stompMessage.getMessageId());
        } catch (Exception e) {
            log.error("❌ 시스템 상태 브로드캐스트 실패: error={}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 에러 메시지를 특정 사용자에게 전송하는 메서드
     * 
     * @param userId 에러 메시지를 받을 사용자 ID
     * @param errorContent 에러 내용
     * @param sessionId 에러가 발생한 세션 ID
     */
    public void sendErrorMessage(String userId, String errorContent, String sessionId) {
        log.info("❌ 에러 메시지 전송 시작: userId={}, sessionId={}, error={}", 
                userId, sessionId, errorContent);
        
        try {
            // ERROR 타입의 STOMP 메시지 생성
            StompMessage stompMessage = StompMessage.createErrorMessage(errorContent, sessionId);
            stompMessage.setMessageId(generateMessageId());
            
            // 특정 사용자에게 에러 메시지 전송
            messagingTemplate.convertAndSendToUser(userId, "/queue/errors", stompMessage);
            
            log.info("✅ 에러 메시지 전송 완료: userId={}, messageId={}", 
                    userId, stompMessage.getMessageId());
        } catch (Exception e) {
            log.error("❌ 에러 메시지 전송 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * 현재 활성 세션 수를 반환하는 메서드
     * 
     * @return 활성 WebSocket 세션 수
     */
    public int getActiveSessionCount() {
        try {
            int count = sessionRepository.getActiveSessionCount();
            log.debug("📊 활성 세션 수 조회: count={}", count);
            return count;
        } catch (Exception e) {
            log.error("❌ 활성 세션 수 조회 실패: error={}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 총 처리된 메시지 수를 반환하는 메서드
     * 
     * @return 총 처리된 메시지 수
     */
    public long getTotalMessagesProcessed() {
        return totalMessagesProcessed.get();
    }

    /**
     * 메시지 처리 카운터를 초기화하는 메서드
     * 주로 관리자 기능이나 테스트 목적으로 사용
     */
    public void resetMessageCounter() {
        long oldCount = totalMessagesProcessed.getAndSet(0);
        log.info("🔄 메시지 카운터 초기화: oldCount={}", oldCount);
    }

    /**
     * 고유한 메시지 ID를 생성하는 private 메서드
     * 
     * @return UUID 기반의 고유 메시지 ID
     */
    private String generateMessageId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 하트비트 메시지를 전송하는 메서드
     * 주로 스케줄러에서 호출되어 클라이언트 연결 상태를 확인
     * 
     * @param intervalSeconds 하트비트 간격 (초)
     */
    public void sendHeartbeat(int intervalSeconds) {
        log.debug("💗 하트비트 전송 시작: interval={}초", intervalSeconds);
        
        try {
            int activeSessions = getActiveSessionCount();
            
            // 활성 세션이 있을 때만 하트비트 전송
            if (activeSessions > 0) {
                StompMessage heartbeatMessage = StompMessage.builder()
                        .type(StompMessage.MessageType.HEARTBEAT)
                        .senderId("SYSTEM")
                        .content("heartbeat")
                        .timestamp(System.currentTimeMillis())
                        .messageId(generateMessageId())
                        .priority(0)
                        .build();
                
                messagingTemplate.convertAndSend("/topic/heartbeat", heartbeatMessage);
                
                log.debug("✅ 하트비트 전송 완료: activeSessions={}, messageId={}", 
                        activeSessions, heartbeatMessage.getMessageId());
            } else {
                log.debug("⏭️ 활성 세션이 없어 하트비트 전송 생략");
            }
        } catch (Exception e) {
            log.error("❌ 하트비트 전송 실패: error={}", e.getMessage(), e);
        }
    }
} 