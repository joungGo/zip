package org.example.springbootpubsub.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springbootpubsub.domain.dto.MessageDto;
import org.example.springbootpubsub.redis.pub.RedisPublisher;
import org.example.springbootpubsub.redis.sub.RedisSubscribeListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis Pub/Sub 기능을 통합 관리하는 비즈니스 서비스 클래스
 * 
 * 이 클래스는 Redis의 발행(Publish)과 구독(Subscribe) 기능을 
 * 하나의 서비스로 통합하여 관리합니다. 채널별 메시지 전송과 
 * 구독 관리를 담당하는 핵심 비즈니스 로직을 포함합니다.
 * 
 * 주요 책임:
 * - 메시지 발행과 동시에 채널 구독 관리
 * - 채널별 구독자(Listener) 등록 및 해제
 * - Pub/Sub 워크플로우 조정
 * - 비즈니스 로직과 Redis 기술 계층 분리
 * - 구독 중인 채널 목록 관리 및 조회
 * 
 * 사용 시나리오:
 * - 실시간 채팅 시스템
 * - 알림 브로드캐스트 시스템
 * - 이벤트 기반 마이크로서비스 통신
 * - 실시간 데이터 동기화
 * 
 * @author Redis Pub/Sub Team
 * @version 1.0
 * @since 2024
 */
@Service // Spring 서비스 계층 컴포넌트로 등록
@RequiredArgsConstructor // Lombok: final 필드들을 매개변수로 받는 생성자 자동 생성
@Slf4j // Lombok: 로깅을 위한 Logger 자동 생성
public class RedisPubService {

    /**
     * Redis 메시지 리스너들을 관리하는 컨테이너
     * 
     * RedisMessageListenerContainer는 Redis Pub/Sub 구독을 관리하는 
     * Spring Data Redis의 핵심 컴포넌트입니다.
     * 
     * 주요 기능:
     * - 채널별 MessageListener 등록/해제
     * - 멀티스레드 환경에서 안전한 메시지 수신
     * - 연결 장애 시 자동 재연결
     * - 리스너 생명주기 관리
     * 
     * 내부 동작:
     * - Redis와의 Pub/Sub 연결 유지
     * - 등록된 리스너들에게 메시지 라우팅
     * - 스레드 풀을 통한 비동기 메시지 처리
     */
    private final RedisMessageListenerContainer redisMessageListenerContainer;

    /**
     * Redis 채널에 메시지를 발행하는 퍼블리셔
     * 
     * RedisPublisher는 메시지를 Redis 채널로 전송하는 역할을 담당합니다.
     * 다양한 타입의 메시지(객체, 문자열)를 직렬화하여 전송할 수 있습니다.
     * 
     * 지원 기능:
     * - MessageDto 객체 발행 (JSON 직렬화)
     * - 단순 문자열 메시지 발행
     * - 채널별 메시지 라우팅
     */
    private final RedisPublisher redisPublisher;

    /**
     * Redis 메시지를 수신하고 처리하는 리스너
     * 
     * RedisSubscribeListener는 구독 중인 채널에서 수신되는 
     * 모든 메시지를 처리하는 단일 리스너입니다.
     * 
     * 처리 과정:
     * - 수신된 메시지의 역직렬화
     * - 메시지 내용 로깅
     * - 추가 비즈니스 로직 실행
     * 
     * 특징:
     * - 모든 채널의 메시지를 통합 처리
     * - 예외 상황 안전 처리
     * - 확장 가능한 메시지 처리 로직
     */
    private final RedisSubscribeListener redisSubscribeListener;

    /**
     * 현재 구독 중인 채널 목록을 추적하는 Thread-safe 컬렉션
     * 
     * ConcurrentHashMap을 사용하여 멀티스레드 환경에서 안전하게
     * 구독 채널 정보를 관리합니다.
     * 
     * Key: 채널명 (String)
     * Value: 구독 시작 시간 (Long - System.currentTimeMillis())
     * 
     * 사용 목적:
     * - 현재 활성 구독 채널 추적
     * - 구독 시간 정보 제공
     * - 중복 구독 방지
     * - 채널별 구독 상태 관리
     */
    private final Map<String, Long> subscribedChannels = new ConcurrentHashMap<>();

    /**
     * 지정된 채널에 메시지를 발행하고 해당 채널을 구독하는 통합 메서드
     * 
     * 이 메서드는 Pub/Sub의 핵심 워크플로우를 구현합니다:
     * 1. 메시지를 받을 수 있도록 채널 구독 시작
     * 2. 해당 채널에 메시지 발행
     * 
     * 이러한 순서로 처리하는 이유:
     * - 메시지 손실 방지: 발행 전에 구독을 시작하여 메시지를 놓치지 않음
     * - 실시간 테스트: 발행과 동시에 수신 확인 가능
     * - 일관된 동작: 항상 같은 패턴으로 Pub/Sub 수행
     * 
     * 사용 예시:
     * ```java
     * MessageDto message = new MessageDto("안녕하세요", "user1", "room1");
     * pubMsgChannel("chat-room-1", message);
     * // -> "chat-room-1" 채널 구독 시작
     * // -> 메시지 발행
     * // -> RedisSubscribeListener에서 메시지 수신 및 로깅
     * ```
     * 
     * 주의사항:
     * - 동일한 채널에 여러 번 구독을 등록하면 중복 수신 발생 가능
     * - 구독 해제 전까지 계속해서 해당 채널의 모든 메시지 수신
     * - 메시지 발행은 구독자가 없어도 정상 처리됨
     * 
     * @param channel 메시지를 발행할 Redis 채널명 (예: "chat-room-1", "notifications")
     * @param message 발행할 메시지 데이터를 담은 MessageDto 객체
     * 
     * @throws org.springframework.data.redis.RedisConnectionFailureException Redis 연결 실패 시
     * @throws org.springframework.data.redis.serializer.SerializationException 메시지 직렬화 실패 시
     */
    public void pubMsgChannel(String channel, MessageDto message) {
        // 1단계: 요청한 채널을 구독 시작
        // ChannelTopic: Redis 채널을 나타내는 객체, 채널명을 래핑하여 타입 안전성 제공
        // addMessageListener(): 지정된 리스너를 특정 채널에 등록
        // 
        // 동작 과정:
        // - RedisMessageListenerContainer가 Redis 서버에 SUBSCRIBE 명령 전송
        // - 해당 채널에서 발생하는 모든 메시지를 redisSubscribeListener가 수신
        // - 멀티스레드 환경에서 안전하게 메시지 처리
        redisMessageListenerContainer.addMessageListener(redisSubscribeListener, new ChannelTopic(channel));

        // 구독 채널 추적 정보 업데이트
        if (!subscribedChannels.containsKey(channel)) {
            subscribedChannels.put(channel, System.currentTimeMillis());
            log.info("새로운 채널 구독 시작: {} (총 구독 채널 수: {})", channel, subscribedChannels.size());
        } else {
            log.debug("이미 구독 중인 채널: {}", channel);
        }

        // 2단계: 메시지 발행
        // RedisPublisher를 통해 실제 메시지를 Redis 채널로 전송
        // 
        // 동작 과정:
        // - MessageDto 객체를 JSON 문자열로 직렬화
        // - Redis PUBLISH 명령을 통해 채널에 메시지 전송
        // - 구독 중인 모든 클라이언트(리스너)가 메시지 수신
        redisPublisher.publish(new ChannelTopic(channel), message);

        // 로깅: 디버깅 및 모니터링을 위한 정보 기록
        log.info("메시지 발행 완료 - 채널: {}, 발신자: {}, 방ID: {}, 내용: {}", 
                 channel, message.getSender(), message.getRoomId(), message.getMessage());
    }

    /**
     * 지정된 채널의 구독을 취소하는 메서드
     * 
     * 이 메서드는 더 이상 특정 채널의 메시지를 수신하지 않도록 
     * 구독을 해제합니다. 리소스 관리와 불필요한 메시지 수신 방지를 위해 사용됩니다.
     * 
     * 동작 과정:
     * 1. RedisMessageListenerContainer에서 리스너 제거
     * 2. Redis 서버에 UNSUBSCRIBE 명령 전송
     * 3. 해당 채널의 메시지 수신 중단
     * 
     * 사용 시나리오:
     * - 채팅방 나가기
     * - 알림 구독 해제
     * - 임시 채널 정리
     * - 애플리케이션 종료 시 리소스 정리
     * 
     * 주의사항:
     * - 현재 구현에서는 특정 채널만 해제하는 것이 아니라 
     *   해당 리스너의 모든 구독을 해제함
     * - 개선 방안: 채널별 개별 해제 로직 구현 필요
     * 
     * 개선된 구현 예시:
     * ```java
     * // 특정 채널만 해제하는 방법
     * redisMessageListenerContainer.removeMessageListener(
     *     redisSubscribeListener, new ChannelTopic(channel)
     * );
     * ```
     * 
     * @param channel 구독을 취소할 Redis 채널명
     * 
     * TODO: 현재는 모든 구독을 해제하므로, 특정 채널만 해제하도록 개선 필요
     */
    public void cancelSubChannel(String channel) {
        try {
            // 특정 채널만 구독 해제 시도
            redisMessageListenerContainer.removeMessageListener(redisSubscribeListener, new ChannelTopic(channel));
            
            // 구독 채널 추적 정보에서 제거
            Long subscribeTime = subscribedChannels.remove(channel);
            if (subscribeTime != null) {
                long duration = System.currentTimeMillis() - subscribeTime;
                log.info("채널 구독 취소 완료 - 채널: {}, 구독 유지 시간: {}ms (총 구독 채널 수: {})", 
                         channel, duration, subscribedChannels.size());
            } else {
                log.warn("구독하지 않은 채널의 취소 요청: {}", channel);
            }
            
        } catch (Exception e) {
            log.error("채널 구독 취소 중 오류 발생 - 채널: {}, 오류: {}", channel, e.getMessage(), e);
            
            // 오류 발생시에도 추적 정보는 정리
            subscribedChannels.remove(channel);
        }
    }

    /**
     * 현재 구독 중인 채널 목록을 조회하는 메서드
     * 
     * 이 메서드는 현재 활성화된 구독 채널들의 목록을 반환합니다.
     * 내부적으로 관리하는 subscribedChannels Map을 기반으로 정보를 제공합니다.
     * 
     * 반환 데이터 형태:
     * - 빈 Set: 구독 중인 채널이 없음
     * - 채널명 Set: {"chat-room-1", "notifications", "events"}
     * 
     * 사용 시나리오:
     * - 관리자 페이지에서 현재 활성 채널 확인
     * - 디버깅 목적으로 구독 상태 점검
     * - 사용자에게 구독 중인 채널 목록 표시
     * - 시스템 모니터링 및 헬스체크
     * 
     * 특징:
     * - Thread-safe 하게 현재 상태 반환
     * - 실시간 구독 상태 반영
     * - 빠른 조회 성능 (메모리 기반)
     * 
     * @return Set<String> 현재 구독 중인 Redis 채널명들의 집합
     */
    public Set<String> getSubscribedChannels() {
        try {
            Set<String> channels = new HashSet<>(subscribedChannels.keySet()); // 현재 구독 중인 채널명만 추출
            log.info("현재 구독 중인 채널 목록 조회 - 총 {}개 채널: {}", channels.size(), channels);
            return channels;
            
        } catch (Exception e) {
            log.error("구독 채널 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return new HashSet<>();
        }
    }

    /**
     * 구독 상태 정보를 상세히 조회하는 메서드
     * 
     * 단순한 채널 목록이 아닌, 구독 상태에 대한 상세 정보를 제공합니다.
     * 관리자 또는 개발자가 시스템 상태를 파악하는 데 유용합니다.
     * 
     * 반환 정보:
     * - 총 구독 채널 수
     * - 채널별 구독 시간
     * - 리스너 컨테이너 상태
     * - 조회 시간 등
     * 
     * @return Map<String, Object> 구독 상태에 대한 상세 정보
     */
    public Map<String, Object> getSubscriptionStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // 현재 구독 중인 채널 목록
            Set<String> channels = getSubscribedChannels();
            status.put("subscribedChannels", channels);
            status.put("totalChannelCount", channels.size());
            
            // 채널별 구독 시간 정보
            Map<String, String> channelDetails = new HashMap<>();
            for (Map.Entry<String, Long> entry : subscribedChannels.entrySet()) {
                String channel = entry.getKey();
                Long subscribeTime = entry.getValue();
                long duration = System.currentTimeMillis() - subscribeTime;
                channelDetails.put(channel, duration + "ms");
            }
            status.put("channelSubscriptionDetails", channelDetails);
            
            // 리스너 컨테이너 상태 정보
            status.put("isRunning", redisMessageListenerContainer.isRunning());
            status.put("isActive", redisMessageListenerContainer.isActive());
            
            // 현재 시간 정보
            status.put("checkTime", java.time.LocalDateTime.now());
            
            log.info("구독 상태 조회 완료: {} 채널", channels.size());
            
        } catch (Exception e) {
            log.error("구독 상태 조회 중 오류 발생: {}", e.getMessage(), e);
            status.put("error", e.getMessage());
            status.put("errorTime", java.time.LocalDateTime.now());
        }
        
        return status;
    }
}