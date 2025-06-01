package com.example.websockettest.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocketSessionRepository는 WebSocket 세션을 관리하는 리포지토리 계층입니다.
 * 
 * 주요 책임:
 * 1. WebSocket 세션의 저장, 조회, 삭제 (CRUD 연산)
 * 2. 세션 활동 시간 추적 및 관리
 * 3. 에러 세션 상태 관리
 * 4. 비활성 세션 정리 기능
 * 
 * 데이터 저장소:
 * - 메모리 기반 저장소 (ConcurrentHashMap 사용)
 * - Thread-safe 보장으로 동시성 문제 해결
 * - 애플리케이션 재시작 시 데이터 초기화됨
 * 
 * 향후 확장 가능:
 * - Redis, Database 등 영구 저장소 연동
 * - 클러스터 환경에서의 세션 공유
 * - 세션 백업 및 복구 기능
 * 
 * @Repository: Spring의 리포지토리 컴포넌트로 등록
 * @RequiredArgsConstructor: Lombok을 사용한 생성자 기반 의존성 주입
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class WebSocketSessionRepository {

    /**
     * 활성 WebSocket 세션을 저장하는 Thread-safe 맵
     * Key: 세션 ID (String), Value: WebSocket 세션 객체
     * ConcurrentHashMap 사용으로 멀티스레드 환경에서 안전한 동시 접근 보장
     */
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    
    /**
     * 각 세션의 마지막 활동 시간을 추적하는 Thread-safe 맵
     * Key: 세션 ID (String), Value: 마지막 활동 시간 (LocalDateTime)
     * 세션 타임아웃 관리 및 비활성 세션 정리에 사용
     */
    private final Map<String, LocalDateTime> sessionLastActivity = new ConcurrentHashMap<>();
    
    /**
     * 에러가 발생한 세션을 추적하는 Thread-safe 셋
     * 에러 세션 분석, 문제 해결, 통계 수집에 활용
     */
    private final Set<String> errorSessions = ConcurrentHashMap.newKeySet();

    /**
     * 새로운 WebSocket 세션을 저장소에 추가하는 메서드
     * 세션 연결 시 호출되어 활성 세션 목록에 등록합니다.
     * 
     * @param session 추가할 WebSocket 세션 객체
     */
    public void addSession(WebSocketSession session) {
        if (session == null || session.getId() == null) {
            log.warn("⚠️ 세션 추가 실패: null 세션 또는 세션 ID");
            return;
        }

        String sessionId = session.getId();
        
        log.debug("💾 세션 저장소 추가 시작: sessionId={}", sessionId);
        
        // 활성 세션 맵에 세션 추가
        activeSessions.put(sessionId, session);
        
        // 세션의 마지막 활동 시간을 현재 시간으로 설정
        sessionLastActivity.put(sessionId, LocalDateTime.now());
        
        // 에러 세션 목록에서 제거 (재연결된 경우)
        errorSessions.remove(sessionId);
        
        int totalSessions = activeSessions.size();
        
        log.info("✅ 세션 저장소 추가 완료: sessionId={}, totalActiveSessions={}", 
                sessionId, totalSessions);
    }

    /**
     * WebSocket 세션을 저장소에서 완전히 제거하는 메서드
     * 세션 연결 종료 시 호출되어 모든 관련 데이터를 정리합니다.
     * 
     * @param session 제거할 WebSocket 세션 객체
     */
    public void removeSession(WebSocketSession session) {
        if (session == null || session.getId() == null) {
            log.warn("⚠️ 세션 제거 실패: null 세션 또는 세션 ID");
            return;
        }

        String sessionId = session.getId();
        
        log.debug("🗑️ 세션 저장소 제거 시작: sessionId={}", sessionId);
        
        // 활성 세션 맵에서 제거
        WebSocketSession removedSession = activeSessions.remove(sessionId);
        
        // 세션 활동 시간 기록에서 제거
        LocalDateTime lastActivity = sessionLastActivity.remove(sessionId);
        
        // 에러 세션 목록에서도 제거
        boolean wasErrorSession = errorSessions.remove(sessionId);
        
        int remainingSessions = activeSessions.size();
        
        if (removedSession != null) {
            log.info("✅ 세션 저장소 제거 완료: sessionId={}, lastActivity={}, wasErrorSession={}, remainingSessions={}", 
                    sessionId, lastActivity, wasErrorSession, remainingSessions);
        } else {
            log.warn("⚠️ 세션 제거 시도했으나 저장소에 존재하지 않음: sessionId={}", sessionId);
        }
    }

    /**
     * 세션 ID로 특정 WebSocket 세션을 조회하는 메서드
     * 
     * @param sessionId 조회할 세션의 ID
     * @return 해당 세션 객체, 존재하지 않으면 null
     */
    public WebSocketSession getSession(String sessionId) {
        if (sessionId == null) {
            log.warn("⚠️ 세션 조회 실패: null 세션 ID");
            return null;
        }
        
        WebSocketSession session = activeSessions.get(sessionId);
        
        log.debug("🔍 세션 조회: sessionId={}, found={}", sessionId, session != null);
        
        return session;
    }

    /**
     * 현재 활성 상태인 모든 WebSocket 세션을 조회하는 메서드
     * 브로드캐스트 메시지 전송, 전체 세션 관리에 사용됩니다.
     * 
     * @return 모든 활성 세션의 컬렉션 (원본 데이터 보호를 위해 새로운 ArrayList로 반환)
     */
    public Collection<WebSocketSession> getAllActiveSessions() {
        Collection<WebSocketSession> sessions = new ArrayList<>(activeSessions.values());
        
        log.debug("📋 전체 활성 세션 조회: count={}", sessions.size());
        
        return sessions;
    }

    /**
     * 현재 활성 세션의 총 개수를 반환하는 메서드
     * 모니터링, 대시보드, 통계 수집에 사용됩니다.
     * 
     * @return 현재 연결된 활성 세션의 개수
     */
    public int getActiveSessionCount() {
        int count = activeSessions.size();
        
        log.debug("📊 활성 세션 수 조회: count={}", count);
        
        return count;
    }

    /**
     * 특정 세션의 마지막 활동 시간을 현재 시간으로 업데이트하는 메서드
     * 메시지 송수신, 사용자 상호작용 시 호출되어 세션의 활성 상태를 갱신합니다.
     * 
     * @param session 활동 시간을 업데이트할 WebSocket 세션
     */
    public void updateSessionLastActivity(WebSocketSession session) {
        // 세션과 세션 ID의 null 체크
        if (session != null && session.getId() != null) {
            String sessionId = session.getId();
            LocalDateTime now = LocalDateTime.now();
            
            log.debug("⏰ 세션 활동 시간 업데이트: sessionId={}, time={}", sessionId, now);
            
            // 현재 시간으로 마지막 활동 시간 업데이트
            sessionLastActivity.put(sessionId, now);
        } else {
            log.warn("⚠️ 세션 활동 시간 업데이트 실패: null 세션 또는 세션 ID");
        }
    }

    /**
     * 에러가 발생한 세션을 에러 세션 목록에 추가하는 메서드
     * 에러 분석, 문제 해결, 통계 수집에 활용됩니다.
     * 
     * @param session 에러가 발생한 WebSocket 세션
     */
    public void markSessionAsError(WebSocketSession session) {
        if (session != null && session.getId() != null) {
            String sessionId = session.getId();
            
            log.warn("⚠️ 세션 에러 마킹: sessionId={}", sessionId);
            
            // 에러 세션 목록에 추가
            errorSessions.add(sessionId);
            
            int totalErrorSessions = errorSessions.size();
            
            log.info("❌ 세션 에러 마킹 완료: sessionId={}, totalErrorSessions={}", 
                    sessionId, totalErrorSessions);
        } else {
            log.warn("⚠️ 세션 에러 마킹 실패: null 세션 또는 세션 ID");
        }
    }

    /**
     * 현재 에러 상태인 세션의 개수를 반환하는 메서드
     * 시스템 모니터링 및 에러 통계에 사용됩니다.
     * 
     * @return 에러 상태인 세션의 개수
     */
    public int getErrorSessionCount() {
        int count = errorSessions.size();
        
        log.debug("📊 에러 세션 수 조회: count={}", count);
        
        return count;
    }

    /**
     * 특정 시간 이전에 활동한 비활성 세션들을 정리하는 메서드
     * 주기적으로 호출되어 메모리 누수를 방지하고 시스템 성능을 유지합니다.
     * 
     * @param cutoffTime 이 시간 이전에 활동한 세션들을 비활성으로 간주
     * @return 정리된 세션의 개수
     */
    public int cleanupInactiveSessions(LocalDateTime cutoffTime) {
        log.info("🧹 비활성 세션 정리 시작: cutoffTime={}", cutoffTime);
        
        List<String> inactiveSessions = new ArrayList<>();
        
        // 비활성 세션 식별
        sessionLastActivity.entrySet().forEach(entry -> {
            String sessionId = entry.getKey();
            LocalDateTime lastActivity = entry.getValue();
            
            if (lastActivity.isBefore(cutoffTime)) {
                inactiveSessions.add(sessionId);
                log.debug("🧹 비활성 세션 발견: sessionId={}, lastActivity={}", sessionId, lastActivity);
            }
        });
        
        // 비활성 세션 제거
        int cleanedCount = 0;
        for (String sessionId : inactiveSessions) {
            WebSocketSession session = activeSessions.get(sessionId);
            if (session != null) {
                removeSession(session);
                cleanedCount++;
            }
        }
        
        log.info("✅ 비활성 세션 정리 완료: cleanedCount={}, remainingSessions={}", 
                cleanedCount, activeSessions.size());
        
        return cleanedCount;
    }
} 