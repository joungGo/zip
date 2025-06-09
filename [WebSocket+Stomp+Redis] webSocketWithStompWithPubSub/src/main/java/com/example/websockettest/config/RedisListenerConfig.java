package com.example.websockettest.config;

import com.example.websockettest.service.RedisStompMessageSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis Pub/Sub 채널 구독 설정 클래스 (하이브리드 구독 전략)
 * 
 * STOMP + Redis 하이브리드 아키텍처에서 효율적인 채널 구독을 위해
 * 전역 이벤트와 룸별 이벤트를 다르게 처리합니다.
 * 
 * 구독 전략:
 * 1. 전역 이벤트 (자동 구독): 세션 이벤트, 시스템 알림 등
 * 2. 룸별 이벤트 (동적 구독): 룸 입장 시 구독, 퇴장 시 해제
 * 
 * 장점:
 * - 리소스 효율성: 필요한 채널만 구독
 * - 확장성: 룸 수가 증가해도 성능 저하 없음
 * - 네트워크 최적화: 불필요한 메시지 수신 방지
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisListenerConfig {

    private final RedisMessageListenerContainer redisMessageListenerContainer; // Redis 메시지 리스너 컨테이너
    private final RedisStompMessageSubscriber redisStompMessageSubscriber; // Redis 메시지 구독자
    
    // 현재 구독 중인 룸 채널들을 추적 (중복 구독 방지)
    private final Set<String> subscribedRoomChannels = ConcurrentHashMap.newKeySet();

    /**
     * 애플리케이션 시작 시 전역 채널만 자동 구독하는 ApplicationRunner
     * 룸별 채널은 동적 구독으로 처리
     * 
     * @return ApplicationRunner 서버 시작 시 실행되는 초기화 로직
     */
    @Bean
    public ApplicationRunner redisChannelSubscriptionRunner() {
        return args -> {
            log.info("Redis 전역 채널 구독 설정 시작 (하이브리드 전략)");
            
            // ============ 전역 이벤트 자동 구독 (모든 서버가 항상 알아야 하는 정보) ============
            
            // 세션 연결 이벤트 채널 구독 (전체 세션 통계용)
            ChannelTopic sessionConnectTopic = new ChannelTopic(RedisChannelConfig.CHANNEL_SESSION_CONNECT);
            redisMessageListenerContainer.addMessageListener(redisStompMessageSubscriber, sessionConnectTopic);
            log.info("전역 채널 구독: {}", sessionConnectTopic.getTopic());
            
            // 세션 해제 이벤트 채널 구독 (전체 세션 통계용)
            ChannelTopic sessionDisconnectTopic = new ChannelTopic(RedisChannelConfig.CHANNEL_SESSION_DISCONNECT);
            redisMessageListenerContainer.addMessageListener(redisStompMessageSubscriber, sessionDisconnectTopic);
            log.info("전역 채널 구독: {}", sessionDisconnectTopic.getTopic());
            
            // 전역 브로드캐스트 채널 구독 (시스템 공지사항)
            ChannelTopic globalBroadcastTopic = new ChannelTopic(RedisChannelConfig.CHANNEL_GLOBAL_BROADCAST);
            redisMessageListenerContainer.addMessageListener(redisStompMessageSubscriber, globalBroadcastTopic);
            log.info("전역 채널 구독: {}", globalBroadcastTopic.getTopic());
            
            // 시스템 알림 채널 구독 (시스템 알림)
            ChannelTopic systemNotificationsTopic = new ChannelTopic(RedisChannelConfig.CHANNEL_SYSTEM_NOTIFICATIONS);
            redisMessageListenerContainer.addMessageListener(redisStompMessageSubscriber, systemNotificationsTopic);
            log.info("전역 채널 구독: {}", systemNotificationsTopic.getTopic());
            
            log.info("Redis 전역 채널 구독 완료 - 룸별 채널은 동적 구독으로 처리");
            log.info("하이브리드 STOMP + Redis 분산 메시지 처리 준비 완료");
        };
    }
    
    /**
     * 특정 룸의 통합 채널을 동적으로 구독하는 메서드
     * 사용자가 룸에 입장할 때 호출하여 해당 룸의 모든 메시지를 수신
     * 
     * @param roomId 구독할 룸 ID
     */
    public void subscribeToRoomChannels(String roomId) {
        try {
            // 중복 구독 방지
            if (subscribedRoomChannels.contains(roomId)) {
                log.debug("이미 구독 중인 룸 채널: {}", roomId);
                return;
            }
            
            // 룸 통합 채널 구독 (채팅, 입장, 퇴장 모든 메시지)
            String roomChannel = RedisChannelConfig.getRoomChannel(roomId); // 1. roomId를 이용해 채널명 생성
            ChannelTopic roomTopic = new ChannelTopic(roomChannel); // 2. Redis 채널 토픽 생성 >> 토픽이란 Redis에서 메시지를 구독할 때 사용하는 채널의 이름
            redisMessageListenerContainer.addMessageListener(redisStompMessageSubscriber, roomTopic); // 3. 룸 채널에 메시지 리스너 등록
            
            // 구독 완료 기록
            subscribedRoomChannels.add(roomId);
            
            log.info("룸 채널 동적 구독 완료 - roomId: {}, 채널: {}", roomId, roomChannel);
            
        } catch (Exception e) {
            log.error("룸 채널 구독 실패 - roomId: {}", roomId, e);
        }
    }
    
    /**
     * 특정 룸의 통합 채널 구독을 해제하는 메서드
     * 해당 룸에 더 이상 참여자가 없을 때 호출하여 리소스 절약
     * 
     * @param roomId 구독 해제할 룸 ID
     */
    public void unsubscribeFromRoomChannels(String roomId) {
        try {
            // 구독하지 않은 룸은 무시
            if (!subscribedRoomChannels.contains(roomId)) {
                log.debug("구독하지 않은 룸 채널: {}", roomId);
                return;
            }
            
            // 룸 통합 채널 구독 해제
            String roomChannel = RedisChannelConfig.getRoomChannel(roomId);
            ChannelTopic roomTopic = new ChannelTopic(roomChannel);
            redisMessageListenerContainer.removeMessageListener(redisStompMessageSubscriber, roomTopic);
            
            // 구독 해제 기록
            subscribedRoomChannels.remove(roomId);
            
            log.info("룸 채널 구독 해제 완료 - roomId: {}, 채널: {}", roomId, roomChannel);
            
        } catch (Exception e) {
            log.error("룸 채널 구독 해제 실패 - roomId: {}", roomId, e);
        }
    }
    
    /**
     * 현재 구독 중인 룸 목록 조회
     * 
     * @return 구독 중인 룸 ID 집합
     */
    public Set<String> getSubscribedRooms() {
        return Set.copyOf(subscribedRoomChannels);
    }
    
    /**
     * 특정 룸이 구독 중인지 확인
     * 
     * @param roomId 확인할 룸 ID
     * @return 구독 중이면 true, 아니면 false
     */
    public boolean isRoomSubscribed(String roomId) {
        return subscribedRoomChannels.contains(roomId);
    }
} 