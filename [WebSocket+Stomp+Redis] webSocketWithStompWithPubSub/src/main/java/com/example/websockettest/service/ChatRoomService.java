package com.example.websockettest.service;

import com.example.websockettest.config.RedisListenerConfig;
import com.example.websockettest.dto.RoomMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * 채팅방 관리 서비스 (Redis Pub/Sub 통합)
 * 룸별 세션 관리, 메시지 전송, 참가자 관리를 담당합니다.
 * 
 * Redis 통합 기능:
 * - 룸 입장 시 Redis 채널 동적 구독
 * - 룸 퇴장 시 Redis 채널 구독 해제 (마지막 사용자일 때)
 * - 모든 메시지를 Redis Pub/Sub으로 발행하여 다중 서버 동기화
 * - 로컬 메모리와 Redis 하이브리드 방식으로 성능 최적화
 */
@Slf4j
@Service
public class ChatRoomService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisStompMessagePublisher redisStompMessagePublisher;
    private final RedisListenerConfig redisListenerConfig;
    
    // 룸별 세션 정보 저장 (roomId -> Set<sessionId>)
    private final ConcurrentMap<String, Set<String>> roomSessions = new ConcurrentHashMap<>();
    
    // 세션별 사용자 정보 저장 (sessionId -> username)
    private final ConcurrentMap<String, String> sessionUsers = new ConcurrentHashMap<>();
    
    // 세션별 현재 룸 정보 저장 (sessionId -> roomId)
    private final ConcurrentMap<String, String> sessionRooms = new ConcurrentHashMap<>();
    
    // 룸별 참가자 수 카운터
    private final ConcurrentMap<String, AtomicInteger> roomParticipantCounts = new ConcurrentHashMap<>();
    
    public ChatRoomService(SimpMessagingTemplate messagingTemplate,
                          RedisStompMessagePublisher redisStompMessagePublisher,
                          RedisListenerConfig redisListenerConfig) {
        this.messagingTemplate = messagingTemplate;
        this.redisStompMessagePublisher = redisStompMessagePublisher;
        this.redisListenerConfig = redisListenerConfig;
    }
    
    /**
     * 사용자가 특정 룸에 입장
     * Redis 동적 구독 및 입장 이벤트 발행 포함
     */
    public void joinRoom(String roomId, String sessionId, String username) {
        log.info("사용자 {}(세션: {})가 룸 {}에 입장 요청", username, sessionId, roomId);
        
        // 기존에 다른 룸에 있었다면 먼저 나가기
        String currentRoom = sessionRooms.get(sessionId);
        if (currentRoom != null && !currentRoom.equals(roomId)) {
            leaveRoom(currentRoom, sessionId);
        }
        
        // 룸이 처음 생성되는지 확인 (첫 번째 참가자)
        boolean isFirstParticipant = !roomSessions.containsKey(roomId) || 
                                   roomSessions.get(roomId).isEmpty();
        
        // 룸 세션 리스트에 추가
        roomSessions.computeIfAbsent(roomId, k -> new ConcurrentSkipListSet<>()).add(sessionId);
        
        // 세션-사용자 매핑 저장
        sessionUsers.put(sessionId, username);
        
        // 세션-룸 매핑 저장
        sessionRooms.put(sessionId, roomId);
        
        // 참가자 수 증가
        int participantCount = roomParticipantCounts
                .computeIfAbsent(roomId, k -> new AtomicInteger(0))
                .incrementAndGet();
        
        // 🚀 첫 번째 참가자인 경우 Redis 채널 동적 구독
        if (isFirstParticipant) {
            redisListenerConfig.subscribeToRoomChannels(roomId);
            log.info("룸 {} 첫 번째 참가자 입장으로 Redis 채널 구독 시작", roomId);
        }
        
        log.info("룸 {} 입장 완료. 현재 참가자 수: {}", roomId, participantCount);
        
        // 입장 메시지 생성 및 Redis 발행
        RoomMessageDto joinMessage = RoomMessageDto.createJoinMessage(roomId, username, sessionId, participantCount);
        
        // 🌟 Redis Pub/Sub으로 입장 이벤트 발행 (다중 서버 동기화)
        redisStompMessagePublisher.publishRoomMessage(roomId, joinMessage);
        
        log.debug("룸 {} 입장 이벤트 Redis 발행 완료: {}", roomId, username);
    }
    
    /**
     * 사용자가 특정 룸에서 퇴장
     * Redis 구독 해제 및 퇴장 이벤트 발행 포함
     */
    public void leaveRoom(String roomId, String sessionId) {
        String username = sessionUsers.get(sessionId);
        if (username == null) {
            log.warn("세션 {}에 대한 사용자 정보를 찾을 수 없습니다", sessionId);
            return;
        }
        
        log.info("사용자 {}(세션: {})가 룸 {}에서 퇴장 요청", username, sessionId, roomId);
        
        // 룸 세션 리스트에서 제거
        Set<String> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(sessionId);
            
            // 룸이 비어있으면 정리 및 Redis 구독 해제
            if (sessions.isEmpty()) {
                roomSessions.remove(roomId);
                roomParticipantCounts.remove(roomId);
                
                // 🚀 마지막 참가자 퇴장으로 Redis 채널 구독 해제
                redisListenerConfig.unsubscribeFromRoomChannels(roomId);
                
                log.info("룸 {}가 비어서 정리되었습니다. Redis 채널 구독 해제", roomId);
                return;
            }
        }
        
        // 세션-룸 매핑 제거
        sessionRooms.remove(sessionId);
        
        // 참가자 수 감소
        AtomicInteger counter = roomParticipantCounts.get(roomId);
        int participantCount = counter != null ? counter.decrementAndGet() : 0;
        
        log.info("룸 {} 퇴장 완료. 현재 참가자 수: {}", roomId, participantCount);
        
        // 퇴장 메시지 생성 및 Redis 발행
        RoomMessageDto leaveMessage = RoomMessageDto.createLeaveMessage(roomId, username, sessionId, participantCount);
        
        // 🌟 Redis Pub/Sub으로 퇴장 이벤트 발행 (다중 서버 동기화)
        redisStompMessagePublisher.publishRoomMessage(roomId, leaveMessage);
        
        log.debug("룸 {} 퇴장 이벤트 Redis 발행 완료: {}", roomId, username);
    }
    
    /**
     * 세션 연결 해제 시 정리 (모든 룸에서 퇴장)
     */
    public void disconnectSession(String sessionId) {
        String roomId = sessionRooms.get(sessionId);
        if (roomId != null) {
            leaveRoom(roomId, sessionId);
        }
        
        // 세션 관련 정보 정리
        sessionUsers.remove(sessionId);
        sessionRooms.remove(sessionId);
        
        log.info("세션 {} 연결 해제 정리 완료", sessionId);
    }
    
    /**
     * 특정 룸에 메시지 브로드캐스트 (로컬 전용 - 기존 호환성 유지)
     * Redis 통합 후에는 이 메서드보다 Redis 발행을 권장
     */
    public void broadcastToRoom(String roomId, RoomMessageDto message) {
        String destination = "/topic/room/" + roomId;
        messagingTemplate.convertAndSend(destination, message);
        
        log.debug("룸 {}에 로컬 메시지 브로드캐스트: {}", roomId, message.getMessage());
    }
    
    /**
     * 특정 룸에 채팅 메시지 전송
     * Redis Pub/Sub 통합으로 다중 서버 동기화
     */
    public void sendChatMessage(String roomId, String sessionId, String message) {
        String username = sessionUsers.get(sessionId);
        if (username == null) {
            log.warn("세션 {}에 대한 사용자 정보를 찾을 수 없습니다", sessionId);
            return;
        }
        
        // 세션이 해당 룸에 속해있는지 확인
        String currentRoom = sessionRooms.get(sessionId);
        if (!roomId.equals(currentRoom)) {
            log.warn("세션 {}는 룸 {}에 속해있지 않습니다", sessionId, roomId);
            return;
        }
        
        // 채팅 메시지 생성
        RoomMessageDto chatMessage = RoomMessageDto.createChatMessage(roomId, message, username, sessionId);
        
        // 🌟 Redis Pub/Sub으로 채팅 메시지 발행 (다중 서버 동기화)
        redisStompMessagePublisher.publishRoomMessage(roomId, chatMessage);
        
        log.debug("룸 {} 채팅 메시지 Redis 발행 완료: {} - {}", roomId, username, message);
    }
    
    /**
     * 특정 룸의 참가자 수 조회
     */
    public int getRoomParticipantCount(String roomId) {
        AtomicInteger counter = roomParticipantCounts.get(roomId);
        return counter != null ? counter.get() : 0;
    }
    
    /**
     * 특정 룸의 참가자 목록 조회
     */
    public Set<String> getRoomParticipants(String roomId) {
        Set<String> sessions = roomSessions.get(roomId);
        if (sessions == null) {
            return Set.of();
        }
        
        return sessions.stream()
                .map(sessionUsers::get)
                .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * 전체 룸 목록 조회
     */
    public Set<String> getAllRooms() {
        return roomSessions.keySet();
    }
    
    /**
     * 세션의 현재 룸 조회
     */
    public String getCurrentRoom(String sessionId) {
        return sessionRooms.get(sessionId);
    }
    
    /**
     * 세션의 사용자명 조회
     */
    public String getUsername(String sessionId) {
        return sessionUsers.get(sessionId);
    }
} 