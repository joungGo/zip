package org.example.springbootpubsub.redis.pub;

import org.example.springbootpubsub.domain.dto.MessageDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

/**
 * Redis Pub/Sub 메시지 발행(Publish)을 담당하는 서비스 클래스
 * 
 * 이 클래스는 Redis 채널에 메시지를 발행하는 Publisher 역할을 수행합니다.
 * 다양한 타입의 메시지(객체, 문자열)를 지정된 채널로 전송할 수 있습니다.
 * 
 * 주요 기능:
 * - MessageDto 객체를 JSON으로 직렬화하여 발행
 * - 일반 문자열 메시지 발행
 * - 채널별 메시지 라우팅
 * 
 * 사용 시나리오:
 * - 실시간 채팅 메시지 전송
 * - 시스템 알림 브로드캐스트
 * - 이벤트 기반 통신
 * 
 * @author Redis Pub/Sub Team
 * @version 1.0
 * @since 2024
 */
@Service // Spring 서비스 컴포넌트로 등록, 비즈니스 로직을 담당
public class RedisPublisher {

    /**
     * Redis 메시지 발행을 위한 RedisTemplate 객체
     * 
     * RedisTemplate은 Redis 서버와의 상호작용을 위한 고수준 추상화 계층입니다.
     * 이 템플릿을 통해 메시지를 직렬화하고 지정된 채널로 전송합니다.
     * 
     * 주요 기능:
     * - convertAndSend(): 객체를 직렬화하여 메시지 발송
     * - 트랜잭션 지원
     * - 연결 풀 관리
     */
    private final RedisTemplate<String, Object> template;

    /**
     * RedisPublisher 생성자
     * 
     * 생성자 주입(Constructor Injection)을 통해 RedisTemplate을 주입받습니다.
     * 이는 Spring의 권장하는 의존성 주입 방식으로, 
     * 불변성(immutability)과 테스트 용이성을 보장합니다.
     * 
     * @param template Redis 작업을 위한 템플릿 객체
     */
    public RedisPublisher(RedisTemplate<String, Object> template) {
        this.template = template;
    }

    /**
     * MessageDto 객체를 지정된 채널에 발행하는 메서드
     * 
     * 이 메서드는 구조화된 메시지 객체를 Redis 채널로 전송합니다.
     * 객체는 자동으로 JSON 형태로 직렬화되어 전송됩니다.
     * 
     * 동작 과정:
     * 1. MessageDto 객체를 JSON 문자열로 직렬화
     * 2. Redis의 PUBLISH 명령을 통해 지정된 채널로 전송
     * 3. 해당 채널을 구독하는 모든 클라이언트가 메시지 수신
     * 
     * 직렬화 예시:
     * MessageDto {message: "안녕", sender: "user1", roomId: "room1"}
     * -> JSON: {"message":"안녕","sender":"user1","roomId":"room1"}
     * 
     * Redis 명령어 예시:
     * PUBLISH chat-room-1 "{\"message\":\"안녕\",\"sender\":\"user1\",\"roomId\":\"room1\"}"
     * 
     * @param topic 메시지를 발행할 Redis 채널 정보를 담는 ChannelTopic 객체
     * @param dto 전송할 메시지 데이터를 담는 MessageDto 객체
     * 
     * @throws org.springframework.data.redis.RedisConnectionFailureException Redis 연결 실패 시
     * @throws org.springframework.data.redis.serializer.SerializationException 직렬화 실패 시
     */
    public void publish(ChannelTopic topic, MessageDto dto) {
        // convertAndSend()는 다음 작업을 순차적으로 수행:
        // 1. MessageDto 객체를 설정된 Serializer(Jackson2JsonRedisSerializer)로 직렬화
        // 2. Redis PUBLISH 명령을 실행하여 메시지 전송
        // 3. 전송 결과 반환 (구독자 수)
        template.convertAndSend(topic.getTopic(), dto);
        
        // 참고 링크: https://grok.com/share/bGVnYWN5_f23a8595-481b-4774-af90-8d503b1e8f48
        // -> Redis Pub/Sub 메커니즘에 대한 자세한 설명
    }

    /**
     * 문자열 메시지를 지정된 채널에 발행하는 메서드
     * 
     * 단순한 텍스트 메시지를 전송할 때 사용합니다.
     * 구조화된 데이터가 필요하지 않은 간단한 알림이나 
     * 시스템 메시지 전송에 적합합니다.
     * 
     * 사용 예시:
     * - 시스템 점검 알림: "시스템 점검이 시작됩니다."
     * - 서버 상태 메시지: "SERVER_RESTART"
     * - 간단한 명령어: "REFRESH_CACHE"
     * 
     * 동작 과정:
     * 1. 문자열 데이터를 그대로 Redis 채널로 전송
     * 2. 별도의 직렬화 과정 없이 문자열 형태로 전달
     * 
     * Redis 명령어 예시:
     * PUBLISH system-alerts "서버 재시작이 예정되어 있습니다."
     * 
     * @param topic 메시지를 발행할 Redis 채널 정보를 담는 ChannelTopic 객체
     * @param data 전송할 문자열 메시지
     * 
     * @throws org.springframework.data.redis.RedisConnectionFailureException Redis 연결 실패 시
     */
    public void publish(ChannelTopic topic, String data) {
        // 문자열 데이터를 지정된 채널로 직접 전송
        template.convertAndSend(topic.getTopic(), data);
    }
}