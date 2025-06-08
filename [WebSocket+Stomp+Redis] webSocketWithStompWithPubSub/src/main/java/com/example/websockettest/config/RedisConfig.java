package com.example.websockettest.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 연결 및 STOMP + Pub/Sub 하이브리드 설정을 담당하는 Configuration 클래스
 * 
 * 이 클래스는 STOMP와 Redis Pub/Sub을 연동하여 분산 환경에서의 
 * 실시간 메시지 처리를 위한 모든 Redis 관련 설정을 관리합니다.
 * 
 * 주요 설정 내용:
 * - Redis 연결 팩토리 (Lettuce 사용)
 * - RedisTemplate 설정 (Key: String, Value: JSON)
 * - Pub/Sub 메시지 리스너 컨테이너
 * - STOMP 메시지 브로드캐스트를 위한 분산 처리
 * 
 * 하이브리드 아키텍처:
 * 클라이언트 ↔ STOMP ↔ 로컬 처리 ↔ Redis Pub/Sub ↔ 다른 서버 인스턴스들
 * 
 * @author WebSocket STOMP Redis Team
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableCaching
@EnableRedisRepositories
public class RedisConfig {

    /**
     * application.yml에서 정의된 Redis 연결 속성들을 주입받는 객체
     * 
     * 포함 정보:
     * - host: Redis 서버 주소 (localhost)
     * - port: Redis 서버 포트 (6382)
     * - timeout: 연결 타임아웃 (2000ms)
     * - lettuce.pool: 연결 풀 설정
     */
    private final RedisProperties redisProperties;

    /**
     * Redis 연결 팩토리를 생성하는 Bean
     * 
     * application.yml의 모든 Redis 설정을 자동으로 반영하여 연결을 생성합니다.
     * Lettuce 클라이언트를 사용하여 고성능 비동기 Redis 연결을 관리합니다.
     * 
     * Lettuce 장점:
     * - 비동기 지원으로 높은 성능
     * - Connection Pool 최적화
     * - Spring Boot 기본 클라이언트
     * - Pub/Sub에 최적화
     * 
     * STOMP + Redis 연동:
     * - STOMP 메시지를 Redis 채널로 전파
     * - 분산 서버 간 실시간 동기화
     * - 세션 정보 중앙 관리
     * 
     * @return RedisConnectionFactory Redis 연결을 생성하고 관리하는 팩토리
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Redis 독립 실행형 서버 설정 생성
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        
        // application.yml에서 설정된 host 정보 적용
        redisConfig.setHostName(redisProperties.getHost());
        log.info("Redis Host 설정: {}", redisProperties.getHost());
        
        // application.yml에서 설정된 port 정보 적용
        redisConfig.setPort(redisProperties.getPort());
        log.info("Redis Port 설정: {}", redisProperties.getPort());
        
        // password 설정 (있는 경우에만)
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            redisConfig.setPassword(redisProperties.getPassword());
            log.info("Redis Password 설정 완료 (보안상 값은 로그에 표시하지 않음)");
        } else {
            log.info("Redis Password 설정 없음 (인증 없는 연결)");
        }
        
        // 데이터베이스 번호 설정 (기본값: 0)
        redisConfig.setDatabase(redisProperties.getDatabase());
        log.info("Redis Database 설정: {}", redisProperties.getDatabase());
        
        // LettuceConnectionFactory 생성 및 설정 적용
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig);
        
        // 연결 검증 활성화
        factory.setValidateConnection(true);
        factory.setShareNativeConnection(true);
        
        log.info("Lettuce Redis 연결 팩토리 생성 완료 - {}:{}", 
                 redisProperties.getHost(), redisProperties.getPort());
        
        return factory;
    }

    /**
     * STOMP + Redis 하이브리드를 위한 RedisTemplate Bean 생성
     * 
     * RedisTemplate은 Redis와 상호작용하기 위한 고수준 추상화 계층입니다.
     * STOMP 메시지를 JSON으로 직렬화하여 다른 서버 인스턴스와 공유합니다.
     * 
     * 직렬화 설정:
     * - Key Serializer: StringRedisSerializer (채널명, 키명 등)
     * - Value Serializer: Jackson2JsonRedisSerializer (STOMP 메시지, 세션 정보 등)
     * 
     * 하이브리드 사용 예시:
     * // 룸 메시지를 다른 서버로 전파
     * redisTemplate.convertAndSend("stomp:broadcast:room:room1", roomMessage);
     * 
     * // 세션 정보를 Redis에 저장
     * redisTemplate.opsForHash().put("stomp:sessions:sessionId", "username", "user1");
     * 
     * @return RedisTemplate<String, Object> Redis 데이터 조작을 위한 템플릿
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        
        // Redis 연결 팩토리 설정
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        
        // Key 직렬화: 문자열 그대로 저장 
        // 예: "stomp:broadcast:room:room1", "stomp:sessions:sessionId123"
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        
        // Value 직렬화: Java 객체를 JSON 문자열로 변환하여 저장
        // 예: RoomMessageDto 객체 -> {"type":"CHAT","roomId":"room1","message":"안녕"}
        // jsonSerializer는 Jackson 라이브러리를 사용하여 Java 객체를 JSON 문자열로 변환하여 Redis에 저장하고, 다시 읽을 때는 JSON을 Java 객체로 역직렬화합니다.
        Jackson2JsonRedisSerializer<Object> jsonSerializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        /**
         * 처리 흐름
         *
         * 1. Jackson2JsonRedisSerializer<Object> 타입의 jsonSerializer를 생성합니다.
         * 2. redisTemplate의 해시 값 직렬화기로 jsonSerializer를 지정합니다.
         * 3. 해시 자료구조(opsForHash())에 값을 저장할 때 Java 객체가 JSON 문자열로 변환되어 Redis에 저장됩니다.
         * 4. 해시 값을 읽을 때는 JSON 문자열이 다시 Java 객체로 역직렬화됩니다.
         * redisTemplate.setValueSerializer(jsonSerializer);로 설정하면 Redis에 저장된 JSON 문자열을 다시 조회할 때 Jackson2JsonRedisSerializer가 자동으로 JSON을 Java 객체로 역직렬화합니다.
         *
         * 즉, 저장할 때는 Java 객체 → JSON, 조회할 때는 JSON → Java 객체로 변환됩니다.
         */
        redisTemplate.setValueSerializer(jsonSerializer); // 일반 값(예: String, DTO 등)을 JSON으로 저장/조회합니다.
        redisTemplate.setHashValueSerializer(jsonSerializer); // 해시 자료구조의 값도 JSON으로 저장/조회합니다.
        
        // 트랜잭션 지원 활성화
        /**
         * 트랜잭션 지원 활성화 필요성
         *
         * 이 설정이 없으면 @Transactional이나 multi/exec 블록 내에서 Redis 명령이 하나의 트랜잭션으로 처리되지 않고, 각 명령이 개별적으로 실행됩니다.
         *
         * 즉, 여러 Redis 연산을 하나의 원자적 작업(atomic operation)으로 묶어야 할 때 필요합니다.
         * 트랜잭션이 필요 없다면 생략해도 무방하지만, 데이터 일관성 보장이 중요한 경우에는 활성화하는 것이 좋습니다.
         */
        redisTemplate.setEnableTransactionSupport(true);
        
        // 설정 완료 후 초기화
        redisTemplate.afterPropertiesSet();
        
        log.info("STOMP + Redis 하이브리드 RedisTemplate 설정 완료 - Key: String, Value: JSON");
        
        return redisTemplate;
    }

    /**
     * String 전용 RedisTemplate (간단한 문자열 데이터용)
     * 
     * @return RedisTemplate<String, String> 문자열 전용 템플릿
     */
    @Bean
    public RedisTemplate<String, String> stringRedisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();
        
        log.info("String 전용 RedisTemplate 설정 완료");
        return template;
    }

    /**
     * Redis Pub/Sub 메시지 처리를 위한 리스너 컨테이너 Bean 생성
     * 
     * RedisMessageListenerContainer는 Redis 채널에서 발생하는 메시지를 
     * 수신하고 등록된 리스너들에게 전달하여 STOMP로 브로드캐스트합니다.
     * 
     * STOMP + Redis 하이브리드 동작:
     * 1. 다른 서버에서 Redis 채널로 메시지 발행
     * 2. 이 컨테이너가 메시지 수신
     * 3. 등록된 리스너가 메시지를 STOMP Topic으로 브로드캐스트
     * 4. 해당 서버의 클라이언트들이 실시간으로 메시지 수신
     * 
     * 채널 구독 예시:
     * - stomp:broadcast:room:room1 → /topic/room/room1 STOMP 브로드캐스트
     * - stomp:event:join:room1 → 입장 이벤트 처리 후 STOMP 전송
     * 
     * @return RedisMessageListenerContainer 분산 메시지 수신 및 STOMP 브로드캐스트
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        
        // Redis 연결 팩토리 설정
        container.setConnectionFactory(redisConnectionFactory());
        
        // 스레드 풀 설정 (기본값 사용)
        container.setTaskExecutor(null);
        container.setSubscriptionExecutor(null);
        
        log.info("STOMP + Redis 하이브리드 Message Listener Container 설정 완료");
        log.info("분산 환경에서 Redis Pub/Sub → STOMP 브로드캐스트 준비 완료");
        
        return container;
    }
} 