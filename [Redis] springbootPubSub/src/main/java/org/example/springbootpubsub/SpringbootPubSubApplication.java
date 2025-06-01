package org.example.springbootpubsub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Redis Pub/Sub 테스트 애플리케이션의 메인 클래스
 * 
 * 이 애플리케이션은 Spring Boot와 Redis를 사용하여 
 * 실시간 메시지 발행(Publish)과 구독(Subscribe) 기능을 제공합니다.
 * 
 * 주요 기능:
 * - Redis 채널을 통한 메시지 발행
 * - 실시간 메시지 구독 및 수신
 * - 웹 UI를 통한 테스트 인터페이스
 * - REST API를 통한 프로그래밍 방식 접근
 * 
 * @author Redis Pub/Sub Team
 * @version 1.0
 * @since 2024
 */
@SpringBootApplication // Spring Boot 자동 설정 활성화 (컴포넌트 스캔, 자동 설정, 설정 프로퍼티 등)
public class SpringbootPubSubApplication {

	/**
	 * 애플리케이션의 진입점(Entry Point)
	 * Spring Boot 애플리케이션을 시작하고 내장 웹 서버를 구동합니다.
	 * 
	 * 실행 순서:
	 * 1. Spring 컨텍스트 초기화
	 * 2. Redis 연결 설정 적용
	 * 3. 컴포넌트 스캔 및 Bean 등록
	 * 4. 웹 서버 시작 (기본 포트: 8080)
	 * 
	 * @param args 명령줄 인수 (선택적으로 프로파일, 포트 등 설정 가능)
	 */
	public static void main(String[] args) {
		SpringApplication.run(SpringbootPubSubApplication.class, args);
	}

}
