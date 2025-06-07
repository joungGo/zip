package com.example.websockettest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * SessionCountService는 STOMP WebSocket 세션 수를 추적하고 관리하는 서비스입니다.
 * 
 * 주요 기능:
 * 1. 실시간 세션 수 추적
 * 2. 세션 연결/해제 시 카운트 업데이트
 * 3. 다른 서비스들에게 세션 수 정보 제공
 * 4. 순환 의존성 없는 독립적인 세션 관리
 * 
 * 이 서비스는 StompEventListener와 WebSocketService 사이의
 * 순환 의존성 문제를 해결하기 위해 만들어졌습니다.
 * 
 * @Service: Spring의 서비스 컴포넌트로 등록
 * @Slf4j: Lombok의 로깅 기능 사용
 */
@Service
@Slf4j
public class SessionCountService {

    /**
     * 현재 연결된 STOMP 세션 수를 추적하는 원자적 카운터
     * 멀티스레드 환경에서 안전한 카운팅을 보장
     */
    private final AtomicInteger connectedSessionCount = new AtomicInteger(0);

    /**
     * 세션이 연결될 때 호출되는 메서드
     * StompEventListener에서 세션 연결 시 호출합니다.
     * 
     * @param sessionId 연결된 세션 ID
     * @return 연결 후 총 세션 수
     */
    public int incrementSessionCount(String sessionId) {
        int newCount = connectedSessionCount.incrementAndGet();
        log.info("📈 세션 카운트 증가: sessionId={}, newCount={}", sessionId, newCount);
        return newCount;
    }

    /**
     * 세션이 해제될 때 호출되는 메서드
     * StompEventListener에서 세션 해제 시 호출합니다.
     * 
     * @param sessionId 해제된 세션 ID
     * @return 해제 후 총 세션 수
     */
    public int decrementSessionCount(String sessionId) {
        int newCount = connectedSessionCount.decrementAndGet();
        
        // 음수 방지 (안전장치)
        if (newCount < 0) {
            connectedSessionCount.set(0);
            newCount = 0;
            log.warn("⚠️ 세션 카운트가 음수가 되어 0으로 재설정: sessionId={}", sessionId);
        }
        
        log.info("📉 세션 카운트 감소: sessionId={}, newCount={}", sessionId, newCount);
        return newCount;
    }

    /**
     * 현재 연결된 세션 수를 반환하는 메서드
     * WebSocketService에서 활성 세션 수 조회 시 호출합니다.
     * 
     * @return 현재 연결된 STOMP 세션 수
     */
    public int getConnectedSessionCount() {
        int count = connectedSessionCount.get();
        log.debug("📊 현재 세션 수 조회: count={}", count);
        return count;
    }

    /**
     * 세션 카운트를 특정 값으로 설정하는 메서드
     * 주로 초기화나 관리자 기능에서 사용합니다.
     * 
     * @param count 설정할 세션 수
     * @return 설정된 세션 수
     */
    public int setSessionCount(int count) {
        if (count < 0) {
            count = 0;
            log.warn("⚠️ 음수 세션 수는 허용되지 않아 0으로 설정");
        }
        
        int oldCount = connectedSessionCount.getAndSet(count);
        log.info("🔄 세션 카운트 설정: oldCount={}, newCount={}", oldCount, count);
        return count;
    }

    /**
     * 세션 카운트를 0으로 초기화하는 메서드
     * 서버 재시작이나 관리자 기능에서 사용합니다.
     * 
     * @return 초기화 전 세션 수
     */
    public int resetSessionCount() {
        int oldCount = connectedSessionCount.getAndSet(0);
        log.info("🔄 세션 카운트 초기화: oldCount={}", oldCount);
        return oldCount;
    }

    /**
     * 세션 카운트가 0인지 확인하는 메서드
     * 
     * @return 세션이 없으면 true, 있으면 false
     */
    public boolean hasNoSessions() {
        return connectedSessionCount.get() == 0;
    }

    /**
     * 세션이 존재하는지 확인하는 메서드
     * 
     * @return 세션이 있으면 true, 없으면 false
     */
    public boolean hasSessions() {
        return connectedSessionCount.get() > 0;
    }
} 