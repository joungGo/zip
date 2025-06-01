package org.example.springbootpubsub.global.redisConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
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
 * Redis 연결 및 Pub/Sub 설정을 담당하는 Configuration 클래스
 * 
 * 이 클래스는 Redis 서버와의 연결, 메시지 직렬화 방식, 
 * Pub/Sub 리스너 컨테이너 등 Redis 관련 모든 설정을 관리합니다.
 * 
 * 주요 설정 내용:
 * - Redis 연결 팩토리 (Lettuce 사용)
 * - RedisTemplate 설정 (Key: String, Value: JSON)
 * - Pub/Sub 메시지 리스너 컨테이너
 * 
 * @author Redis Pub/Sub Team
 * @version 1.0
 * @since 2024
 */
@Slf4j // Lombok: 로깅을 위한 Logger 자동 생성
@Configuration // Spring 설정 클래스임을 명시, Bean 정의를 포함
@RequiredArgsConstructor // Lombok: final 필드를 매개변수로 받는 생성자 자동 생성
@EnableRedisRepositories // Redis Repository 기능 활성화 (Redis를 DB처럼 사용 가능)
public class RedisConfig {

    /**
     * application.yml에서 정의된 Redis 연결 속성들을 주입받는 객체
     * 
     * 포함 정보:
     * - host: Redis 서버 주소 (localhost)
     * - port: Redis 서버 포트 (6379)
     * - password: Redis 인증 비밀번호 (pk2258)
     * - timeout: 연결 타임아웃 등
     */
    private final RedisProperties redisProperties;

    /**
     * Redis 연결 팩토리를 생성하는 Bean
     * 
     * application.yml의 모든 Redis 설정을 반영하여 연결을 생성합니다.
     * Lettuce 클라이언트를 사용하여 Redis 서버와의 연결을 관리합니다.
     * 
     * Lettuce vs Jedis:
     * - Lettuce: 비동기 지원, 더 나은 성능, Spring Boot 기본값
     * - Jedis: 동기 전용, 단순한 API, 레거시 프로젝트에서 사용
     * 
     * Redis 연결 설정:
     * - Host: localhost (application.yml에서 설정)
     * - Port: 6379 (application.yml에서 설정)
     * - Password: pk2258 (application.yml에서 설정)
     * - Database: 0 (기본값)
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
        
        // application.yml에서 설정된 password 정보 적용
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
        
        log.info("Redis 연결 팩토리 생성 완료 - {}:{}", 
                 redisProperties.getHost(), redisProperties.getPort());
        
        return factory;
    }

    /**
     * Redis 데이터 조작을 위한 RedisTemplate Bean 생성
     * 
     * RedisTemplate은 Redis와 상호작용하기 위한 고수준 추상화 계층입니다.
     * 이 설정에서는 Key는 String으로, Value는 JSON 형태로 직렬화합니다.
     * 
     * 직렬화 설정:
     * - Key Serializer: StringRedisSerializer (문자열 그대로 저장)
     * - Value Serializer: Jackson2JsonRedisSerializer (JSON 형태로 변환)
     * 
     * 사용 예시:
     * redisTemplate.opsForValue().set("key", object); // 객체를 JSON으로 저장
     * redisTemplate.opsForValue().get("key"); // JSON을 객체로 복원
     * 
     * 참고: String 타입만 사용하려면 StringRedisTemplate을 사용할 수도 있습니다.
     * 
     * @return RedisTemplate<?, ?> Redis 데이터 조작을 위한 템플릿
     */
    @Bean
    public RedisTemplate<?, ?> redisTemplate() {
        RedisTemplate<?, ?> redisTemplate = new RedisTemplate<>();
        
        // Redis 연결 팩토리 설정 (위에서 생성한 connectionFactory 사용)
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        
        // Key 직렬화: 문자열 그대로 저장 (예: "user:123")
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        
        // Value 직렬화: Java 객체를 JSON 문자열로 변환하여 저장
        // 예: MessageDto 객체 -> {"message":"안녕", "sender":"user1", "roomId":"room1"}
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(String.class));
        
        log.info("RedisTemplate 설정 완료 - Key: String, Value: JSON");
        
        return redisTemplate;
    }

    /**
     * Redis Pub/Sub 메시지 처리를 위한 리스너 컨테이너 Bean 생성
     * 
     * RedisMessageListenerContainer는 Redis 채널에서 발생하는 메시지를 
     * 수신하고 등록된 리스너들에게 전달하는 역할을 합니다.
     * 
     * 주요 기능:
     * - 채널 구독 관리 (addMessageListener/removeMessageListener)
     * - 메시지 수신 및 리스너에게 전달
     * - 연결 관리 및 재연결 처리
     * - 멀티스레드 환경에서 안전한 메시지 처리
     * 
     * 동작 원리:
     * 1. Redis 서버로부터 메시지 수신
     * 2. 등록된 MessageListener들에게 메시지 전달
     * 3. 각 리스너가 onMessage() 메서드를 통해 메시지 처리
     * 
     * 사용 예시:
     * container.addMessageListener(listener, new ChannelTopic("chat"));
     * -> "chat" 채널의 메시지를 listener가 처리
     * 
     * @return RedisMessageListenerContainer 메시지 수신 및 처리를 담당하는 컨테이너
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListener() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        
        // Redis 연결 팩토리 설정 (위에서 생성한 connectionFactory 사용)
        container.setConnectionFactory(redisConnectionFactory());
        
        log.info("Redis Message Listener Container 설정 완료");
        
        return container;
    }
}