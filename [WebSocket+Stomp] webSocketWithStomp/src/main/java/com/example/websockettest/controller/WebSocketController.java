package com.example.websockettest.controller;

import com.example.websockettest.dto.RoomMessageDto;
import com.example.websockettest.service.ChatRoomService;
import com.example.websockettest.service.WebSocketService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import java.security.Principal;

/**
 * WebSocketController는 STOMP WebSocket 메시지를 처리하는 컨트롤러입니다.
 * Spring STOMP의 @MessageMapping을 활용하여 destination 기반 메시지 라우팅을 처리합니다.
 * 
 * 주요 기능:
 * 1. 클라이언트 메시지 수신 및 처리 (@MessageMapping)
 * 2. 브로드캐스트 메시지 전송 (@SendTo)
 * 3. 개별 사용자 메시지 전송 (@SendToUser)
 * 4. STOMP 세션 정보 활용
 * 
 * STOMP 메시지 라우팅:
 * - 클라이언트 → /app/message → handleMessage() → /topic/messages (브로드캐스트)
 * - 클라이언트 → /app/private → handlePrivateMessage() → /queue/reply (개별)
 * - 클라이언트 → /app/system → handleSystemMessage() → 시스템 처리
 * 
 * 모든 비즈니스 로직은 Service 계층(WebSocketService)에 위임하여
 * 관심사의 분리(Separation of Concerns) 원칙을 따릅니다.
 */
@Slf4j
@Controller
public class WebSocketController {

    /**
     * WebSocket 관련 비즈니스 로직을 처리하는 서비스
     * @Autowired를 통해 Spring이 자동으로 의존성을 주입합니다.
     */
    @Autowired
    private WebSocketService webSocketService;
    
    /**
     * 채팅방 관련 비즈니스 로직을 처리하는 서비스
     */
    @Autowired
    private ChatRoomService chatRoomService;
    
    /**
     * JSON 직렬화/역직렬화를 위한 ObjectMapper
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 일반 메시지를 처리하고 모든 구독자에게 브로드캐스트하는 메서드
     * 
     * STOMP 라우팅:
     * - 수신: /app/message
     * - 응답: /topic/messages (모든 구독자에게 전송)
     * 
     * @param message 클라이언트가 보낸 메시지 내용
     * @param headerAccessor STOMP 메시지 헤더 정보 (세션 ID, 사용자 정보 등)
     * @return 처리된 응답 메시지 (모든 구독자에게 브로드캐스트)
     * @throws Exception 메시지 처리 중 발생할 수 있는 예외
     */
    @MessageMapping("/message")
    @SendTo("/topic/messages")
    public String handleMessage(String message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        // 세션 ID 추출
        String sessionId = headerAccessor.getSessionId();
        
        log.info("📨 브로드캐스트 메시지 수신: sessionId={}, messageLength={}, payload={}", 
                sessionId, message.length(), message);
        
        try {
            // STOMP에서는 세션 관리가 자동으로 이루어지므로 메시지 처리만 수행
            // 실제로는 비즈니스 로직을 통해 메시지를 처리
            String processedMessage = processStompMessage(sessionId, message);
            
            log.info("📤 브로드캐스트 응답 전송: sessionId={}, response={}", sessionId, processedMessage);
            
            return processedMessage;
        } catch (Exception e) {
            log.error("❌ 브로드캐스트 메시지 처리 중 오류 발생: sessionId={}, payload={}, error={}", 
                    sessionId, message, e.getMessage(), e);
            
            // 에러 메시지를 브로드캐스트로 전송
            return "Error processing message: " + e.getMessage();
        }
    }

    /**
     * 개별 사용자에게 전송할 개인 메시지를 처리하는 메서드
     * 
     * STOMP 라우팅:
     * - 수신: /app/private
     * - 응답: /queue/reply (메시지를 보낸 사용자에게만 전송)
     * 
     * @param message 클라이언트가 보낸 개인 메시지 내용
     * @param headerAccessor STOMP 메시지 헤더 정보
     * @param principal 사용자 인증 정보 (로그인된 사용자 정보)
     * @return 처리된 개인 응답 메시지
     * @throws Exception 메시지 처리 중 발생할 수 있는 예외
     */
    @MessageMapping("/private")
    @SendToUser("/queue/reply")
    public String handlePrivateMessage(String message, SimpMessageHeaderAccessor headerAccessor, Principal principal) throws Exception {
        String sessionId = headerAccessor.getSessionId();
        String userId = principal != null ? principal.getName() : "anonymous";
        
        log.info("📨 개인 메시지 수신: sessionId={}, userId={}, messageLength={}, payload={}", 
                sessionId, userId, message.length(), message);
        
        try {
            // 개인 메시지 처리 로직
            String processedMessage = processPrivateStompMessage(sessionId, userId, message);
            
            log.info("📤 개인 응답 전송: sessionId={}, userId={}, response={}", sessionId, userId, processedMessage);
            
            return processedMessage;
        } catch (Exception e) {
            log.error("❌ 개인 메시지 처리 중 오류 발생: sessionId={}, userId={}, payload={}, error={}", 
                    sessionId, userId, message, e.getMessage(), e);
            
            return "Error processing private message: " + e.getMessage();
        }
    }

    /**
     * 시스템 관련 메시지를 처리하는 메서드
     * 관리자 명령, 시스템 상태 조회 등의 특별한 메시지를 처리합니다.
     * 
     * STOMP 라우팅:
     * - 수신: /app/system
     * - 응답: /topic/system (시스템 공지사항으로 브로드캐스트)
     * 
     * @param message 시스템 명령 또는 조회 메시지
     * @param headerAccessor STOMP 메시지 헤더 정보
     * @return 시스템 처리 결과 메시지
     * @throws Exception 시스템 메시지 처리 중 발생할 수 있는 예외
     */
    @MessageMapping("/system")
    @SendTo("/topic/system")
    public String handleSystemMessage(String message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        String sessionId = headerAccessor.getSessionId();
        
        log.info("🔧 시스템 메시지 수신: sessionId={}, payload={}", sessionId, message);
        
        try {
            // 시스템 메시지 처리 로직
            String processedMessage = processSystemStompMessage(sessionId, message);
            
            log.info("📢 시스템 공지 전송: sessionId={}, response={}", sessionId, processedMessage);
            
            return processedMessage;
        } catch (Exception e) {
            log.error("❌ 시스템 메시지 처리 중 오류 발생: sessionId={}, payload={}, error={}", 
                    sessionId, message, e.getMessage(), e);
            
            return "System error: " + e.getMessage();
        }
    }

    /**
     * 에코 메시지를 처리하는 메서드
     * 클라이언트의 연결 테스트 및 응답 시간 측정을 위한 단순 에코 기능
     * 
     * STOMP 라우팅:
     * - 수신: /app/echo
     * - 응답: /queue/echo (메시지를 보낸 사용자에게만 전송)
     * 
     * @param message 에코할 메시지 내용
     * @param headerAccessor STOMP 메시지 헤더 정보
     * @return 에코된 메시지 (원본 메시지 + 타임스탬프)
     */
    @MessageMapping("/echo")
    @SendToUser("/queue/echo")
    public String handleEcho(String message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        long timestamp = System.currentTimeMillis();
        
        log.debug("🔄 에코 메시지: sessionId={}, payload={}", sessionId, message);
        
        return String.format("Echo: %s (timestamp: %d)", message, timestamp);
    }

    /**
     * STOMP 일반 메시지를 처리하는 private 메서드
     * 
     * @param sessionId STOMP 세션 ID
     * @param message 클라이언트 메시지
     * @return 처리된 응답 메시지
     */
    private String processStompMessage(String sessionId, String message) {
        // 간단한 비즈니스 로직 처리
        if (message == null || message.trim().isEmpty()) {
            return "Empty message received";
        }
        
        // 메시지 처리 로직 (대문자 변환 + 인사말 추가)
        String processedMessage = "Hello, " + message.toUpperCase() + "!";
        
        // 브로드캐스트 메시지 생성
        webSocketService.broadcastMessage(String.format("[%s]: %s", 
                sessionId.substring(0, Math.min(8, sessionId.length())), message));
        
        return processedMessage;
    }

    /**
     * STOMP 개인 메시지를 처리하는 private 메서드
     * 
     * @param sessionId STOMP 세션 ID
     * @param userId 사용자 ID
     * @param message 클라이언트 메시지
     * @return 처리된 개인 응답 메시지
     */
    private String processPrivateStompMessage(String sessionId, String userId, String message) {
        // 개인 메시지 처리 로직
        if (message == null || message.trim().isEmpty()) {
            return "Empty private message received";
        }
        
        return String.format("Private reply to %s: %s", userId, message.toLowerCase());
    }

    /**
     * STOMP 시스템 메시지를 처리하는 private 메서드
     * 
     * @param sessionId STOMP 세션 ID
     * @param message 시스템 메시지
     * @return 처리된 시스템 응답 메시지
     */
    private String processSystemStompMessage(String sessionId, String message) {
        // 시스템 메시지 처리 로직
        if ("status".equalsIgnoreCase(message)) {
            int activeConnections = webSocketService.getActiveSessionCount();
            return String.format("System Status: %d active connections", activeConnections);
        }
        
        return String.format("System processed: %s", message);
    }
    
    // ==================== 채팅방 관련 메서드들 ====================
    
    /**
     * 특정 채팅방에 입장하는 메서드
     * 
     * STOMP 라우팅:
     * - 수신: /app/room/{roomId}/join
     * - 응답: 없음 (ChatRoomService에서 브로드캐스트 처리)
     * 
     * @param roomId 입장할 채팅방 ID
     * @param username 사용자명 (JSON 형태로 전송)
     * @param headerAccessor STOMP 메시지 헤더 정보
     */
    @MessageMapping("/room/{roomId}/join")
    public void handleRoomJoin(@DestinationVariable String roomId, String username, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        
        try {
            // JSON에서 사용자명 추출 (단순 문자열이거나 JSON 객체일 수 있음)
            String extractedUsername = extractUsernameFromJson(username);
            
            log.info("🚪 룸 입장 요청: roomId={}, sessionId={}, username={}", roomId, sessionId, extractedUsername);
            
            chatRoomService.joinRoom(roomId, sessionId, extractedUsername);
            
        } catch (Exception e) {
            log.error("❌ 룸 입장 처리 중 오류 발생: roomId={}, sessionId={}, username={}, error={}", 
                    roomId, sessionId, username, e.getMessage(), e);
        }
    }
    
    /**
     * 특정 채팅방에서 퇴장하는 메서드
     * 
     * STOMP 라우팅:
     * - 수신: /app/room/{roomId}/leave
     * - 응답: 없음 (ChatRoomService에서 브로드캐스트 처리)
     * 
     * @param roomId 퇴장할 채팅방 ID
     * @param headerAccessor STOMP 메시지 헤더 정보
     */
    @MessageMapping("/room/{roomId}/leave")
    public void handleRoomLeave(@DestinationVariable String roomId, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        
        log.info("🚪 룸 퇴장 요청: roomId={}, sessionId={}", roomId, sessionId);
        
        try {
            chatRoomService.leaveRoom(roomId, sessionId);
        } catch (Exception e) {
            log.error("❌ 룸 퇴장 처리 중 오류 발생: roomId={}, sessionId={}, error={}", 
                    roomId, sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 특정 채팅방에 메시지를 전송하는 메서드
     * 
     * STOMP 라우팅:
     * - 수신: /app/room/{roomId}/message
     * - 응답: 없음 (ChatRoomService에서 브로드캐스트 처리)
     * 
     * @param roomId 메시지를 전송할 채팅방 ID
     * @param message 전송할 메시지 내용
     * @param headerAccessor STOMP 메시지 헤더 정보
     */
    @MessageMapping("/room/{roomId}/message")
    public void handleRoomMessage(@DestinationVariable String roomId, String message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        
        log.info("💬 룸 메시지 수신: roomId={}, sessionId={}, messageLength={}", roomId, sessionId, message.length());
        
        try {
            // JSON에서 메시지 내용 추출 (단순 문자열이거나 JSON 객체일 수 있음)
            String extractedMessage = extractMessageFromJson(message);
            
            if (extractedMessage == null || extractedMessage.trim().isEmpty()) {
                log.warn("⚠️ 빈 메시지 수신: roomId={}, sessionId={}", roomId, sessionId);
                return;
            }
            
            chatRoomService.sendChatMessage(roomId, sessionId, extractedMessage);
            
        } catch (Exception e) {
            log.error("❌ 룸 메시지 처리 중 오류 발생: roomId={}, sessionId={}, message={}, error={}", 
                    roomId, sessionId, message, e.getMessage(), e);
        }
    }
    
    /**
     * JSON 문자열에서 사용자명을 추출하는 헬퍼 메서드
     * 
     * @param json JSON 문자열 또는 단순 문자열
     * @return 추출된 사용자명
     */
    private String extractUsernameFromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return "anonymous";
        }
        
        // JSON 형태인지 확인
        if (json.trim().startsWith("{") && json.trim().endsWith("}")) {
            try {
                // JSON 파싱을 시도하여 username 필드 추출
                var jsonNode = objectMapper.readTree(json);
                if (jsonNode.has("username")) {
                    return jsonNode.get("username").asText();
                }
                if (jsonNode.has("user")) {
                    return jsonNode.get("user").asText();
                }
                if (jsonNode.has("name")) {
                    return jsonNode.get("name").asText();
                }
            } catch (JsonProcessingException e) {
                log.warn("JSON 파싱 실패, 원본 문자열 사용: {}", json);
            }
        }
        
        // JSON이 아니거나 파싱 실패 시 원본 문자열 반환
        return json.trim();
    }
    
    /**
     * JSON 문자열에서 메시지 내용을 추출하는 헬퍼 메서드
     * 
     * @param json JSON 문자열 또는 단순 문자열
     * @return 추출된 메시지 내용
     */
    private String extractMessageFromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return "";
        }
        
        // JSON 형태인지 확인
        if (json.trim().startsWith("{") && json.trim().endsWith("}")) {
            try {
                // JSON 파싱을 시도하여 message 필드 추출
                var jsonNode = objectMapper.readTree(json);
                if (jsonNode.has("message")) {
                    return jsonNode.get("message").asText();
                }
                if (jsonNode.has("content")) {
                    return jsonNode.get("content").asText();
                }
                if (jsonNode.has("text")) {
                    return jsonNode.get("text").asText();
                }
            } catch (JsonProcessingException e) {
                log.warn("JSON 파싱 실패, 원본 문자열 사용: {}", json);
            }
        }
        
        // JSON이 아니거나 파싱 실패 시 원본 문자열 반환
        return json.trim();
    }
} 