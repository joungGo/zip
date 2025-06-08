package org.example.springbootpubsub.global.redisConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 연결 상태를 확인하는 유틸리티 클래스
 * 
 * 애플리케이션 시작 시 Redis 서버와의 연결 상태를 확인하고
 * 연결 정보를 로그로 출력합니다.
 * 
 * 주요 기능:
 * - Redis 서버 연결 상태 확인
 * - Redis 서버 정보 조회 및 로깅
 * - 연결 실패 시 에러 메시지 출력
 * 
 * @author Redis Pub/Sub Team
 * @version 1.0
 * @since 2024
 */
@Component // Spring 컴포넌트로 등록하여 자동 실행
@RequiredArgsConstructor // Lombok: final 필드를 매개변수로 받는 생성자 자동 생성
@Slf4j // Lombok: 로깅을 위한 Logger 자동 생성
/**
 * pring Boot 애플리케이션이 완전히 시작된 후 실행할 코드를 작성할 수 있게 해주는 기능을 제공합니다.
 * 구현체의 run(String... args) 메서드는 애플리케이션 구동 직후 한 번 자동으로 호출되며, 주로 초기화 작업, 외부 시스템 점검, 데이터 로딩 등에 사용됩니다.
 * 즉, 스프링 컨텍스트가 준비된 뒤 실행되는 "애플리케이션 시작 후 후처리"용 인터페이스입니다.
 */
public class RedisConnectionChecker implements CommandLineRunner {

    /**
     * Redis 연결을 생성하고 관리하는 팩토리
     */
    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * Redis 데이터 조작을 위한 템플릿
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * application.yml의 Redis 설정 정보
     */
    private final RedisProperties redisProperties;

    /**
     * 애플리케이션 시작 후 자동으로 실행되는 메서드
     * 
     * CommandLineRunner 인터페이스를 구현하여 Spring Boot 애플리케이션이
     * 완전히 시작된 후에 Redis 연결 상태를 확인합니다.
     * 
     * @param args 명령줄 인수 (사용하지 않음)
     */
    @Override
    public void run(String... args) {
        checkRedisConnection();
    }

    /**
     * Redis 연결 상태를 확인하고 결과를 로그로 출력하는 메서드
     * 
     * 확인 항목:
     * - Redis 서버 연결 가능 여부
     * - Redis 서버 기본 정보
     * - Pub/Sub 기능 테스트
     */
    private void checkRedisConnection() {
        log.info("=".repeat(60));
        log.info("🔍 Redis 연결 상태 확인을 시작합니다...");
        log.info("=".repeat(60));

        // 설정 정보 출력
        printRedisConfiguration();

        try {
            // Redis 연결 획득
            RedisConnection connection = redisConnectionFactory.getConnection();
            
            if (connection != null) {
                log.info("✅ Redis 서버 연결 성공!");
                
                try {
                    // Ping 테스트
                    testRedisPing(connection);
                    
                    // RedisTemplate 테스트
                    testRedisTemplate();
                    
                } finally {
                    // 연결 종료
                    connection.close();
                    log.info("🔗 Redis 연결이 정상적으로 종료되었습니다.");
                }
                
            } else {
                log.error("❌ Redis 연결을 얻을 수 없습니다.");
            }
            
        } catch (Exception e) {
            log.error("❌ Redis 연결 실패: {}", e.getMessage());
            log.error("💡 해결 방법:");
            log.error("   1. Redis 서버가 실행 중인지 확인하세요 (redis-server)");
            log.error("   2. application.yml의 Redis 설정을 확인하세요");
            log.error("   3. 네트워크 연결 상태를 확인하세요");
            log.error("   4. Redis 비밀번호가 올바른지 확인하세요");
            log.error("   5. 포트 {}가 사용 중인지 확인하세요", redisProperties.getPort());
        }
        
        log.info("=".repeat(60));
        log.info("🏁 Redis 연결 상태 확인 완료");
        log.info("=".repeat(60));
    }

    /**
     * Redis 설정 정보를 출력하는 메서드
     */
    private void printRedisConfiguration() {
        log.info("📋 Redis 설정 정보:");
        log.info("   • Host: {}", redisProperties.getHost());
        log.info("   • Port: {}", redisProperties.getPort());
        log.info("   • Database: {}", redisProperties.getDatabase());
        
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            log.info("   • Password: 설정됨 (보안상 표시하지 않음)");
        } else {
            log.info("   • Password: 설정되지 않음");
        }
        
        if (redisProperties.getTimeout() != null) {
            log.info("   • Timeout: {}", redisProperties.getTimeout());
        }
    }

    /**
     * Redis PING 명령을 테스트하는 메서드
     * 
     * @param connection Redis 연결 객체
     */
    private void testRedisPing(RedisConnection connection) {
        try {
            // PING 명령 실행
            String pong = new String(connection.ping());
            log.info("🏓 PING 테스트: {} (정상)", pong);
            
        } catch (Exception e) {
            log.error("❌ PING 테스트 실패: {}", e.getMessage());
        }
    }

    /**
     * RedisTemplate 기능을 테스트하는 메서드
     */
    private void testRedisTemplate() {
        try {
            // 테스트 키-값 설정
            String testKey = "redis:connection:test";
            String testValue = "Redis Pub/Sub Test - " + System.currentTimeMillis();
            
            // 값 저장
            redisTemplate.opsForValue().set(testKey, testValue);
            log.info("📝 RedisTemplate 쓰기 테스트: 성공");
            
            // 값 조회
            Object retrievedValue = redisTemplate.opsForValue().get(testKey);
            if (testValue.equals(retrievedValue)) {
                log.info("📖 RedisTemplate 읽기 테스트: 성공");
            } else {
                log.warn("⚠️ RedisTemplate 읽기 테스트: 값이 일치하지 않음");
                log.warn("   예상값: {}", testValue);
                log.warn("   실제값: {}", retrievedValue);
            }
            
            // 테스트 키 삭제
            Boolean deleted = redisTemplate.delete(testKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("🗑️ 테스트 데이터 정리 완료");
            } else {
                log.warn("⚠️ 테스트 데이터 삭제 실패");
            }
            
        } catch (Exception e) {
            log.error("❌ RedisTemplate 테스트 실패: {}", e.getMessage());
        }
    }
} 