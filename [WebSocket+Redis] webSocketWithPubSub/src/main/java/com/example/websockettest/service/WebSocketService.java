package com.example.websockettest.service;

import com.example.websockettest.repository.WebSocketSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocketService는 WebSocket 관련 비즈니스 로직을 처리하는 서비스 계층입니다.
 * 
 * 주요 책임:
 * 1. WebSocket 세션의 생명주기 관리 (연결, 종료, 에러 처리)
 * 2. 클라이언트 메시지 처리 및 응답 생성
 * 3. 브로드캐스트 메시지 전송
 * 4. 세션 통계 정보 제공
 * 
 * Controller와 Repository 사이의 중간 계층으로서
 * 비즈니스 로직을 캡슐화하고 데이터 접근을 추상화합니다.
 * 
 * @Service: Spring의 서비스 컴포넌트로 등록
 * @RequiredArgsConstructor: Lombok을 사용한 생성자 기반 의존성 주입
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    /**
     * WebSocket 세션 데이터를 관리하는 리포지토리
     * final 키워드와 @RequiredArgsConstructor로 불변성과 의존성 주입을 보장
     */
    private final WebSocketSessionRepository sessionRepository;

    /**
     * 새로운 WebSocket 연결을 처리하는 메서드
     * 클라이언트가 WebSocket 연결을 수립했을 때 호출됩니다.
     * 
     * 처리 과정:
     * 1. 세션을 리포지토리에 저장
     * 2. 연결 성공 로그 출력
     * 3. 필요시 추가 초기화 작업 수행
     * 
     * @param session 새로 연결된 WebSocket 세션 객체
     */
    public void handleConnection(WebSocketSession session) {
        log.info("🔗 새 WebSocket 연결 처리 시작: sessionId={}, remoteAddress={}", 
                session.getId(), session.getRemoteAddress());
        
        try {
            // 세션을 활성 세션 저장소에 추가
            sessionRepository.addSession(session);
            
            // 현재 총 세션 수 조회
            int totalSessions = sessionRepository.getActiveSessionCount();
            
            log.info("✅ WebSocket 세션 저장 완료: sessionId={}, totalActiveSessions={}", 
                    session.getId(), totalSessions);
            
            // 향후 확장 가능: 사용자 인증, 권한 확인, 환영 메시지 전송 등
        } catch (Exception e) {
            log.error("❌ WebSocket 연결 처리 중 오류 발생: sessionId={}, error={}", 
                    session.getId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 클라이언트로부터 받은 메시지를 처리하는 메서드
     * 메시지 내용을 분석하고 적절한 응답을 생성합니다.
     * 
     * 처리 과정:
     * 1. 메시지에 대한 비즈니스 로직 적용
     * 2. 세션의 마지막 활동 시간 업데이트
     * 3. 처리된 응답 메시지 반환
     * 
     * @param session 메시지를 보낸 클라이언트의 세션
     * @param message 클라이언트가 보낸 원본 메시지
     * @return 클라이언트에게 전송할 응답 메시지
     */
    public String processMessage(WebSocketSession session, String message) {
        log.info("🔄 메시지 처리 시작: sessionId={}, messageLength={}", 
                session.getId(), message.length());
        log.debug("🔄 메시지 내용: sessionId={}, message={}", session.getId(), message);
        
        try {
            // 메시지에 대한 비즈니스 로직 처리
            // 실제 애플리케이션에서는 메시지 타입 분석, 데이터 검증, 
            // 외부 API 호출, 데이터베이스 조회 등의 복잡한 로직이 들어갈 수 있음
            String processedMessage = processBusinessLogic(message);
            
            log.debug("🔄 비즈니스 로직 처리 완료: sessionId={}, responseLength={}", 
                    session.getId(), processedMessage.length());
            
            // 세션의 마지막 활동 시간을 현재 시간으로 업데이트
            // 세션 타임아웃 관리 및 비활성 세션 정리에 사용
            sessionRepository.updateSessionLastActivity(session);
            
            log.info("✅ 메시지 처리 완료: sessionId={}, response={}", 
                    session.getId(), processedMessage);
            
            // 처리된 메시지를 클라이언트에게 반환
            return processedMessage;
        } catch (Exception e) {
            log.error("❌ 메시지 처리 중 오류 발생: sessionId={}, message={}, error={}", 
                    session.getId(), message, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * WebSocket 연결 종료를 처리하는 메서드
     * 클라이언트가 연결을 끊거나 네트워크 문제로 연결이 종료될 때 호출됩니다.
     * 
     * 처리 과정:
     * 1. 세션을 활성 세션 목록에서 제거
     * 2. 관련 리소스 정리
     * 3. 연결 종료 로그 출력
     * 
     * @param session 종료된 WebSocket 세션
     */
    public void handleDisconnection(WebSocketSession session) {
        log.info("🔌 WebSocket 연결 종료 처리 시작: sessionId={}", session.getId());
        
        try {
            // 세션을 모든 저장소에서 제거 (활성 세션, 활동 시간, 에러 세션 등)
            sessionRepository.removeSession(session);
            
            // 현재 남은 세션 수 조회
            int remainingSessions = sessionRepository.getActiveSessionCount();
            
            log.info("✅ WebSocket 세션 제거 완료: sessionId={}, remainingActiveSessions={}", 
                    session.getId(), remainingSessions);
            
            // 향후 확장 가능: 사용자 로그아웃 처리, 정리 작업, 통계 업데이트 등
        } catch (Exception e) {
            log.error("❌ WebSocket 연결 종료 처리 중 오류 발생: sessionId={}, error={}", 
                    session.getId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * WebSocket 에러를 처리하는 메서드
     * 전송 에러, 네트워크 문제 등이 발생했을 때 호출됩니다.
     * 
     * 처리 과정:
     * 1. 에러 로그 출력
     * 2. 세션을 에러 상태로 마킹
     * 3. 필요시 추가 에러 처리 로직 수행
     * 
     * @param session 에러가 발생한 WebSocket 세션
     * @param exception 발생한 예외 객체
     */
    public void handleError(WebSocketSession session, Throwable exception) {
        log.error("❌ WebSocket 에러 처리 시작: sessionId={}, errorType={}, message={}", 
                session.getId(), exception.getClass().getSimpleName(), exception.getMessage(), exception);
        
        try {
            // 에러가 발생한 세션을 별도로 추적하여 문제 분석에 활용
            sessionRepository.markSessionAsError(session);
            
            log.info("✅ WebSocket 에러 세션 마킹 완료: sessionId={}", session.getId());
            
            // 향후 확장 가능: 에러 알림, 자동 복구, 에러 통계 수집 등
        } catch (Exception e) {
            log.error("❌ WebSocket 에러 처리 중 추가 오류 발생: sessionId={}, originalError={}, newError={}", 
                    session.getId(), exception.getMessage(), e.getMessage(), e);
        }
    }

    /**
     * 메시지에 대한 실제 비즈니스 로직을 처리하는 private 메서드
     * 이 메서드에서 애플리케이션의 핵심 로직을 구현합니다.
     * 
     * 현재 구현:
     * - 빈 메시지 검증
     * - 메시지를 대문자로 변환
     * - 인사말 추가
     * 
     * 실제 애플리케이션에서는 다음과 같은 로직이 들어갈 수 있습니다:
     * - 메시지 타입별 분기 처리
     * - 데이터베이스 조회/저장
     * - 외부 API 호출
     * - 데이터 변환 및 검증
     * - 권한 확인
     * 
     * @param message 클라이언트가 보낸 원본 메시지
     * @return 처리된 응답 메시지
     */
    private String processBusinessLogic(String message) {
        log.debug("🔄 비즈니스 로직 처리 시작: messageLength={}", message.length());
        
        // 빈 메시지 또는 공백만 있는 메시지 검증
        if (message.trim().isEmpty()) {
            log.warn("⚠️ 빈 메시지 수신: 기본 응답 반환");
            return "Empty message received. Please send a valid message.";
        }
        
        // 간단한 예시: 메시지를 대문자로 변환하고 인사말 추가
        // 실제 애플리케이션에서는 더 복잡한 비즈니스 로직이 들어감
        String processedMessage = "Processed: " + message.toUpperCase() + " - Hello from WebSocket Service!";
        
        log.debug("✅ 비즈니스 로직 처리 완료: originalLength={}, processedLength={}", 
                message.length(), processedMessage.length());
        
        return processedMessage;
    }

    /**
     * 현재 활성 세션 수를 반환하는 메서드
     * 모니터링, 대시보드, 통계 목적으로 사용됩니다.
     * 
     * @return 현재 연결된 활성 세션의 개수
     */
    public int getActiveSessionCount() {
        int count = sessionRepository.getActiveSessionCount();
        
        log.debug("📊 활성 세션 수 조회: count={}", count);
        
        return count;
    }

    /**
     * 모든 활성 세션에 브로드캐스트 메시지를 전송하는 메서드
     * 공지사항, 실시간 알림, 시스템 메시지 등을 모든 연결된 클라이언트에게 전송할 때 사용합니다.
     * 
     * 처리 과정:
     * 1. 모든 활성 세션 목록 조회
     * 2. 각 세션에 대해 메시지 전송 시도
     * 3. 전송 실패 시 에러 로그 출력
     * 
     * @param message 모든 클라이언트에게 전송할 브로드캐스트 메시지
     */
    public void broadcastMessage(String message) {
        var activeSessions = sessionRepository.getAllActiveSessions();
        int totalSessions = activeSessions.size();
        
        log.info("📡 브로드캐스트 메시지 전송 시작: targetSessions={}, messageLength={}", 
                totalSessions, message.length());
        log.debug("📡 브로드캐스트 메시지 내용: message={}", message);
        
        int successCount = 0;
        int failureCount = 0;
        
        // 모든 활성 세션을 조회하여 각각에 메시지 전송
        for (var session : activeSessions) {
            try {
                // TextMessage 객체로 래핑하여 전송
                session.sendMessage(new org.springframework.web.socket.TextMessage(message));
                successCount++;
                
                log.debug("✅ 브로드캐스트 메시지 전송 성공: sessionId={}", session.getId());
            } catch (Exception e) {
                failureCount++;
                
                // 개별 세션 전송 실패 시 에러 로그 출력
                // 전체 브로드캐스트를 중단하지 않고 계속 진행
                log.error("❌ 브로드캐스트 메시지 전송 실패: sessionId={}, error={}", 
                        session.getId(), e.getMessage());
                
                // 향후 확장 가능: 실패한 세션 정리, 재시도 로직, 에러 통계 등
            }
        }
        
        log.info("✅ 브로드캐스트 메시지 전송 완료: totalSessions={}, success={}, failure={}", 
                totalSessions, successCount, failureCount);
    }
} 