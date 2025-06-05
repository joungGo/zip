package com.example.websockettest.controller;

import com.example.websockettest.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocketController는 WebSocket 연결과 메시지를 처리하는 컨트롤러입니다.
 * Spring WebSocket의 TextWebSocketHandler를 상속받아 WebSocket 이벤트를 처리합니다.
 * 
 * 주요 기능:
 * 1. WebSocket 연결 수립 처리
 * 2. 클라이언트로부터 받은 텍스트 메시지 처리
 * 3. WebSocket 연결 종료 처리
 * 4. WebSocket 전송 에러 처리
 * 
 * 모든 비즈니스 로직은 Service 계층(WebSocketService)에 위임하여
 * 관심사의 분리(Separation of Concerns) 원칙을 따릅니다.
 */
@Slf4j
public class WebSocketController extends TextWebSocketHandler {

    /**
     * WebSocket 관련 비즈니스 로직을 처리하는 서비스
     * @Autowired를 통해 Spring이 자동으로 의존성을 주입합니다.
     */
    @Autowired
    private WebSocketService webSocketService;

    /**
     * WebSocket 연결이 성공적으로 수립되었을 때 호출되는 메서드
     * 
     * @param session 새로 연결된 WebSocket 세션 객체
     * @throws Exception 연결 처리 중 발생할 수 있는 예외
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("🔗 WebSocket 연결 수립: sessionId={}, remoteAddress={}", 
                session.getId(), session.getRemoteAddress()); // getRemoteAddress()로 클라이언트 IP 주소 확인
        
        try {
            // 서비스 계층에 연결 처리 위임
            // 세션을 저장소에 추가하고 필요한 초기화 작업을 수행
            webSocketService.handleConnection(session);
            
            log.info("✅ WebSocket 연결 처리 완료: sessionId={}", session.getId());
        } catch (Exception e) {
            log.error("❌ WebSocket 연결 처리 중 오류 발생: sessionId={}, error={}", 
                    session.getId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 클라이언트로부터 텍스트 메시지를 받았을 때 호출되는 메서드
     * 
     * @param session 메시지를 보낸 클라이언트의 WebSocket 세션
     * @param message 클라이언트가 보낸 텍스트 메시지 객체
     * @throws Exception 메시지 처리 중 발생할 수 있는 예외
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 메시지 내용 추출
        String payload = message.getPayload();
        
        log.info("📨 메시지 수신: sessionId={}, messageLength={}, payload={}", 
                session.getId(), payload.length(), payload);
        
        try {
            // 서비스 계층에서 메시지 처리 로직 수행
            // 비즈니스 로직 적용, 데이터 변환, 세션 활동 시간 업데이트 등
            String response = webSocketService.processMessage(session, payload);
            
            log.debug("🔄 메시지 처리 완료: sessionId={}, responseLength={}", 
                    session.getId(), response.length());
            
            // 처리된 응답을 클라이언트에게 전송
            // TextMessage 객체로 래핑하여 전송
            session.sendMessage(new TextMessage(response));
            
            log.info("📤 응답 전송 완료: sessionId={}, response={}", 
                    session.getId(), response);
        } catch (Exception e) {
            log.error("❌ 메시지 처리 중 오류 발생: sessionId={}, payload={}, error={}", 
                    session.getId(), payload, e.getMessage(), e);
            
            // 에러 응답을 클라이언트에게 전송
            try {
                session.sendMessage(new TextMessage("Error processing message: " + e.getMessage()));
            } catch (Exception sendError) {
                log.error("❌ 에러 응답 전송 실패: sessionId={}, error={}", 
                        session.getId(), sendError.getMessage());
            }
        }
    }

    /**
     * WebSocket 연결이 종료되었을 때 호출되는 메서드
     * 클라이언트가 연결을 끊거나 네트워크 문제로 연결이 끊어진 경우 실행
     * 
     * @param session 종료된 WebSocket 세션
     * @param status 연결 종료 상태 정보 (정상 종료, 에러 등)
     * @throws Exception 연결 종료 처리 중 발생할 수 있는 예외
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        log.info("🔌 WebSocket 연결 종료: sessionId={}, closeCode={}, reason={}", 
                session.getId(), status.getCode(), status.getReason());
        
        try {
            // 서비스 계층에 연결 종료 처리 위임
            // 세션을 저장소에서 제거하고 정리 작업 수행
            webSocketService.handleDisconnection(session);
            
            log.info("✅ WebSocket 연결 종료 처리 완료: sessionId={}", session.getId());
        } catch (Exception e) {
            log.error("❌ WebSocket 연결 종료 처리 중 오류 발생: sessionId={}, error={}", 
                    session.getId(), e.getMessage(), e);
        }
    }

    /**
     * WebSocket 전송 중 에러가 발생했을 때 호출되는 메서드
     * 네트워크 문제, 메시지 전송 실패 등의 상황에서 실행
     * 
     * @param session 에러가 발생한 WebSocket 세션
     * @param exception 발생한 예외 객체
     * @throws Exception 에러 처리 중 발생할 수 있는 예외
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("❌ WebSocket 전송 에러: sessionId={}, errorType={}, message={}", 
                session.getId(), exception.getClass().getSimpleName(), exception.getMessage(), exception);
        
        try {
            // 서비스 계층에 에러 처리 위임
            // 에러 세션 마킹, 로깅, 필요시 세션 정리 등
            webSocketService.handleError(session, exception);
            
            log.info("✅ WebSocket 에러 처리 완료: sessionId={}", session.getId());
        } catch (Exception e) {
            log.error("❌ WebSocket 에러 처리 중 추가 오류 발생: sessionId={}, error={}", 
                    session.getId(), e.getMessage(), e);
        }
    }
} 