package com.example.websockettest.controller;

import com.example.websockettest.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocketRestController는 WebSocket 관련 REST API를 제공하는 컨트롤러입니다.
 * 
 * 주요 기능:
 * 1. WebSocket 세션 정보 조회 (활성 세션 수, 서비스 상태)
 * 2. 브로드캐스트 메시지 전송
 * 3. WebSocket 서비스 모니터링 및 관리
 * 
 * API 설계 원칙:
 * - RESTful API 설계 패턴 준수
 * - JSON 형태의 응답 제공
 * - 적절한 HTTP 상태 코드 사용
 * - 에러 처리 및 검증 로직 포함
 * 
 * 사용 목적:
 * - 관리자 대시보드에서 WebSocket 상태 모니터링
 * - 외부 시스템에서 브로드캐스트 메시지 전송
 * - WebSocket 서비스 헬스 체크
 * 
 * @RestController: Spring MVC의 REST 컨트롤러로 등록 (JSON 응답 자동 변환)
 * @RequiredArgsConstructor: Lombok을 사용한 생성자 기반 의존성 주입
 * @RequestMapping: 모든 API의 기본 경로를 "/api/websocket"으로 설정
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/websocket")
@Slf4j
public class WebSocketRestController {

    /**
     * WebSocket 관련 비즈니스 로직을 처리하는 서비스
     * final 키워드와 @RequiredArgsConstructor로 불변성과 의존성 주입을 보장
     */
    private final WebSocketService webSocketService;

    /**
     * 현재 활성 WebSocket 세션 수를 조회하는 GET API
     * 
     * 기능:
     * - 현재 연결된 WebSocket 세션의 총 개수 반환
     * - 응답에 타임스탬프 포함으로 데이터 신선도 확인 가능
     * 
     * 사용 사례:
     * - 관리자 대시보드에서 실시간 연결 수 모니터링
     * - 시스템 부하 분석 및 용량 계획
     * - 서비스 사용량 통계 수집
     * 
     * HTTP Method: GET
     * URL: /api/websocket/sessions/count
     * 
     * @return ResponseEntity<Map<String, Object>> JSON 형태의 응답
     *         - activeSessionCount: 현재 활성 세션 수 (Integer)
     *         - timestamp: 응답 생성 시간 (Long, Unix timestamp)
     */
    @GetMapping("/sessions/count")
    public ResponseEntity<Map<String, Object>> getActiveSessionCount() {
        log.info("🔍 API 호출: 활성 세션 수 조회 요청");
        
        try {
            // 서비스에서 현재 활성 세션 수 조회
            int activeSessionCount = webSocketService.getActiveSessionCount();
            
            log.info("📊 활성 세션 수 조회 완료: count={}", activeSessionCount);
            
            // 응답 데이터를 담을 맵 생성
            Map<String, Object> response = new HashMap<>();
            response.put("activeSessionCount", activeSessionCount);
            
            // 응답 생성 시간을 Unix timestamp로 추가 (데이터 신선도 확인용)
            response.put("timestamp", System.currentTimeMillis());
            
            log.debug("✅ 활성 세션 수 조회 API 응답 완료: response={}", response);
            
            // HTTP 200 OK 상태와 함께 JSON 응답 반환
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ 활성 세션 수 조회 중 오류 발생: error={}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get active session count");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 모든 활성 WebSocket 세션에 브로드캐스트 메시지를 전송하는 POST API
     * 
     * 기능:
     * - 요청 본문에서 메시지 내용 추출
     * - 메시지 유효성 검증 (빈 메시지 체크)
     * - 모든 연결된 클라이언트에게 동시 메시지 전송
     * - 전송 결과 및 대상 세션 수 응답
     * 
     * 사용 사례:
     * - 시스템 공지사항 전파
     * - 긴급 알림 메시지 전송
     * - 실시간 이벤트 알림
     * - 서버 점검 안내
     * 
     * HTTP Method: POST
     * URL: /api/websocket/broadcast
     * Content-Type: application/json
     * 
     * 요청 본문 예시:
     * {
     *   "message": "시스템 점검이 10분 후 시작됩니다."
     * }
     * 
     * @param request 요청 본문을 Map으로 받음 (message 키에 전송할 메시지 포함)
     * @return ResponseEntity<Map<String, Object>> JSON 형태의 응답
     *         성공 시:
     *         - success: true
     *         - message: 성공 메시지
     *         - sentTo: 메시지가 전송된 세션 수
     *         실패 시:
     *         - error: 에러 메시지
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> broadcastMessage(@RequestBody Map<String, String> request) {
        // 요청 본문에서 "message" 키의 값 추출
        String message = request.get("message");
        
        log.info("📡 API 호출: 브로드캐스트 메시지 전송 요청, messageLength={}", 
                message != null ? message.length() : 0);
        log.debug("📡 브로드캐스트 메시지 내용: message={}", message);
        
        // 메시지 유효성 검증: null 또는 빈 문자열(공백 포함) 체크
        if (message == null || message.trim().isEmpty()) {
            log.warn("⚠️ 브로드캐스트 메시지 검증 실패: 빈 메시지");
            
            // 에러 응답 생성
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Message cannot be empty");
            
            // HTTP 400 Bad Request 상태와 함께 에러 응답 반환
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            // 브로드캐스트 전송 전 활성 세션 수 조회
            int sessionCountBefore = webSocketService.getActiveSessionCount();
            
            log.info("📡 브로드캐스트 전송 시작: targetSessions={}, message={}", 
                    sessionCountBefore, message);
            
            // 서비스를 통해 모든 활성 세션에 브로드캐스트 메시지 전송
            webSocketService.broadcastMessage(message);
            
            // 브로드캐스트 전송 후 활성 세션 수 조회 (전송 완료 확인)
            int sessionCountAfter = webSocketService.getActiveSessionCount();
            
            log.info("✅ 브로드캐스트 전송 완료: sentTo={} sessions", sessionCountAfter);
            
            // 성공 응답 데이터 생성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Broadcast message sent successfully");
            
            // 메시지가 전송된 세션 수 포함 (전송 완료 후 세션 수 조회)
            response.put("sentTo", sessionCountAfter);
            response.put("timestamp", System.currentTimeMillis());
            
            log.debug("✅ 브로드캐스트 API 응답 완료: response={}", response);
            
            // HTTP 200 OK 상태와 함께 성공 응답 반환
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ 브로드캐스트 메시지 전송 중 오류 발생: message={}, error={}", 
                    message, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to send broadcast message");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * WebSocket 서비스의 현재 상태를 조회하는 GET API
     * 
     * 기능:
     * - WebSocket 서비스 운영 상태 확인
     * - 현재 활성 연결 수 포함
     * - 서비스 헬스 체크 정보 제공
     * 
     * 사용 사례:
     * - 서비스 모니터링 시스템에서 헬스 체크
     * - 로드 밸런서의 헬스 체크 엔드포인트
     * - 관리자 대시보드에서 서비스 상태 확인
     * - 장애 감지 및 알림 시스템 연동
     * 
     * HTTP Method: GET
     * URL: /api/websocket/status
     * 
     * @return ResponseEntity<Map<String, Object>> JSON 형태의 응답
     *         - service: 서비스 이름
     *         - status: 서비스 상태 ("running", "stopped" 등)
     *         - activeConnections: 현재 활성 연결 수
     *         - timestamp: 응답 생성 시간
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getWebSocketStatus() {
        log.info("🔍 API 호출: WebSocket 서비스 상태 조회 요청");
        
        try {
            // 현재 활성 연결 수 (실시간 데이터)
            int activeConnections = webSocketService.getActiveSessionCount();
            
            log.info("📊 WebSocket 서비스 상태 조회 완료: activeConnections={}", activeConnections);
            
            // 서비스 상태 응답 데이터 생성
            Map<String, Object> response = new HashMap<>();
            
            // 서비스 기본 정보
            response.put("service", "WebSocket Service");
            response.put("status", "running");  // 현재는 고정값, 향후 실제 상태 체크 로직 추가 가능
            response.put("activeConnections", activeConnections);
            
            // 응답 생성 시간 (모니터링 시스템에서 데이터 신선도 확인용)
            response.put("timestamp", System.currentTimeMillis());
            
            log.debug("✅ WebSocket 상태 조회 API 응답 완료: response={}", response);
            
            // HTTP 200 OK 상태와 함께 상태 정보 응답 반환
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ WebSocket 서비스 상태 조회 중 오류 발생: error={}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get WebSocket service status");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
} 