package com.example.websockettest.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 채널 설정 클래스
 * 
 * STOMP + Redis Pub/Sub 하이브리드 방식을 위한 채널 패턴 정의
 * 
 * 채널 구조:
 * - stomp:broadcast:room:{roomId}      # 룸별 STOMP 메시지 릴레이
 * - stomp:broadcast:global             # 전역 STOMP 브로드캐스트
 * - stomp:event:join:{roomId}          # 룸 입장 이벤트
 * - stomp:event:leave:{roomId}         # 룸 퇴장 이벤트
 * - stomp:session:connect              # 세션 연결 이벤트
 * - stomp:session:disconnect           # 세션 해제 이벤트
 * - stomp:system:notifications         # 시스템 알림
 * 
 * 메시지 플로우:
 * 1. 클라이언트 → STOMP → @MessageMapping → 로컬 처리
 * 2. 로컬 처리 → Redis Pub/Sub → 다른 서버 인스턴스들
 * 3. 다른 서버에서 Redis 메시지 수신 → STOMP Topic 브로드캐스트
 */
@Slf4j
@Configuration
public class RedisChannelConfig {

    // ============ 채널 패턴 상수 정의 ============
    
    /**
     * 룸 통합 채널 패턴 (채팅, 입장, 퇴장 모든 메시지)
     * 사용: stomp:room:room1
     */
    public static final String CHANNEL_ROOM = "stomp:room:%s"; // 룸별 모든 메시지를 하나의 채널로 통합 처리
    
    /**
     * 전역 브로드캐스트 채널
     */
    public static final String CHANNEL_GLOBAL_BROADCAST = "stomp:broadcast:global"; // 시스템 공지사항이나 전체 사용자 대상 메시지를 모든 서버에 브로드캐스트할 때 사용
    
    /**
     * 세션 연결 이벤트 채널
     */
    public static final String CHANNEL_SESSION_CONNECT = "stomp:session:connect"; // WebSocket 세션이 연결될 때 서버 간 세션 정보 동기화를 위해 사용
    
    /**
     * 세션 해제 이벤트 채널
     */
    public static final String CHANNEL_SESSION_DISCONNECT = "stomp:session:disconnect"; // WebSocket 세션이 해제될 때 서버 간 세션 정보 정리를 위해 사용
    
    /**
     * 시스템 알림 채널
     */
    public static final String CHANNEL_SYSTEM_NOTIFICATIONS = "stomp:system:notifications"; // 서버 상태 변경, 긴급 공지 등 시스템 레벨 알림을 모든 서버에 전파할 때 사용

    // ============ 채널 패턴 생성 유틸리티 메서드 ============
    
    /**
     * 룸 통합 채널명 생성 (채팅, 입장, 퇴장 모든 메시지)
     * 
     * @param roomId 룸 ID
     * @return 채널명 (예: stomp:room:room1)
     */
    public static String getRoomChannel(String roomId) { // ChatRoomService에서 룸의 모든 메시지(채팅, 입장, 퇴장)를 Redis로 발행할 때 채널명 생성에 사용
        return String.format(CHANNEL_ROOM, roomId);
    }

    // ============ Redis 키 패턴 상수 정의 ============
    
    /**
     * 세션 정보 저장 키 패턴 (Hash)
     * 사용: stomp:sessions:sessionId123
     */
    public static final String KEY_SESSION_INFO = "stomp:sessions:%s"; // RedisSessionService에서 세션별 사용자명, 접속시간 등 상세 정보를 Hash로 저장할 때 사용
    
    /**
     * 활성 세션 목록 키 (Set)
     */
    public static final String KEY_ACTIVE_SESSIONS = "stomp:sessions:active"; // WebSocketService에서 현재 활성화된 모든 세션 ID를 Set으로 관리할 때 사용
    
    /**
     * 룸 참여자 목록 키 패턴 (Set)
     * 사용: stomp:rooms:room1:participants
     */
    public static final String KEY_ROOM_PARTICIPANTS = "stomp:rooms:%s:participants"; // ChatRoomService에서 룸별 참여자 세션 ID 목록을 Set으로 저장할 때 사용
    
    /**
     * 룸 정보 키 패턴 (Hash)
     * 사용: stomp:rooms:room1:info
     */
    public static final String KEY_ROOM_INFO = "stomp:rooms:%s:info"; // ChatRoomService에서 룸별 생성시간, 참여자 수, 최근 메시지 등을 Hash로 저장할 때 사용
    
    /**
     * 사용자별 현재 룸 키 패턴 (String)
     * 사용: stomp:users:sessionId123:current_room
     */
    public static final String KEY_USER_CURRENT_ROOM = "stomp:users:%s:current_room"; // ChatRoomService에서 사용자가 현재 참여 중인 룸 ID를 String으로 저장할 때 사용
    
    /**
     * 서버별 세션 목록 키 패턴 (Set)
     * 사용: stomp:servers:server1:sessions
     */
    public static final String KEY_SERVER_SESSIONS = "stomp:servers:%s:sessions"; // 분산 환경에서 서버별로 관리하는 세션 목록을 Set으로 저장할 때 사용

    // ============ Redis 키 생성 유틸리티 메서드 ============
    
    /**
     * 세션 정보 키 생성
     * 
     * @param sessionId 세션 ID
     * @return Redis 키 (예: stomp:sessions:sessionId123)
     */
    public static String getSessionInfoKey(String sessionId) { // StompEventListener에서 세션 연결/해제 시 세션 정보를 Redis에 저장/삭제할 때 사용
        return String.format(KEY_SESSION_INFO, sessionId);
    }
    
    /**
     * 룸 참여자 목록 키 생성
     * 
     * @param roomId 룸 ID
     * @return Redis 키 (예: stomp:rooms:room1:participants)
     */
    public static String getRoomParticipantsKey(String roomId) { // ChatRoomService에서 룸 입장/퇴장 시 참여자 목록을 Redis Set에서 추가/제거할 때 사용
        return String.format(KEY_ROOM_PARTICIPANTS, roomId);
    }
    
    /**
     * 룸 정보 키 생성
     * 
     * @param roomId 룸 ID
     * @return Redis 키 (예: stomp:rooms:room1:info)
     */
    public static String getRoomInfoKey(String roomId) { // WebSocketRestController에서 룸 통계 조회 API나 ChatRoomService에서 룸 메타데이터 관리할 때 사용
        return String.format(KEY_ROOM_INFO, roomId);
    }
    
    /**
     * 사용자 현재 룸 키 생성
     * 
     * @param sessionId 세션 ID
     * @return Redis 키 (예: stomp:users:sessionId123:current_room)
     */
    public static String getUserCurrentRoomKey(String sessionId) { // ChatRoomService에서 사용자가 룸 이동 시 이전 룸에서 자동 퇴장 처리할 때 현재 룸 확인용으로 사용
        return String.format(KEY_USER_CURRENT_ROOM, sessionId);
    }
    
    /**
     * 서버별 세션 목록 키 생성
     * 
     * @param serverId 서버 ID
     * @return Redis 키 (예: stomp:servers:server1:sessions)
     */
    public static String getServerSessionsKey(String serverId) { // 분산 환경에서 서버 장애 시 해당 서버의 세션들을 정리하거나 로드밸런싱 통계 수집할 때 사용
        return String.format(KEY_SERVER_SESSIONS, serverId);
    }

    /**
     * 현재 서버 ID 생성 (IP + 포트 기반)
     * 
     * @return 서버 ID
     */
    public static String getCurrentServerId() { // 서버 시작 시 자신의 고유 ID를 생성하여 Redis에 서버 정보 등록하거나 세션 소유권 관리할 때 사용
        // 실제 환경에서는 시스템 속성이나 환경 변수에서 가져올 수 있음
        return System.getProperty("server.id", "server-" + System.currentTimeMillis());
    }
} 