package org.example.springbootpubsub.redis.sub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springbootpubsub.domain.dto.MessageDto;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis Pub/Sub 메시지 구독(Subscribe) 및 처리를 담당하는 리스너 클래스
 * 
 * 이 클래스는 Redis 채널에서 발행되는 메시지를 실시간으로 수신하고 처리하는 
 * Subscriber 역할을 수행합니다. MessageListener 인터페이스를 구현하여
 * Redis 메시지 수신 시 자동으로 호출되는 콜백 메서드를 제공합니다.
 * 
 * 주요 기능:
 * - Redis 채널에서 메시지 실시간 수신
 * - JSON 형태의 메시지를 MessageDto 객체로 역직렬화
 * - 수신된 메시지 로깅 및 처리
 * - 에러 핸들링 및 예외 처리
 * 
 * 동작 원리:
 * 1. RedisMessageListenerContainer에 등록
 * 2. 구독 중인 채널에서 메시지 발생 시 onMessage() 자동 호출
 * 3. 메시지 역직렬화 및 비즈니스 로직 처리
 * 
 * @author Redis Pub/Sub Team
 * @version 1.0
 * @since 2024
 */
@Service // Spring 서비스 컴포넌트로 등록
@RequiredArgsConstructor // Lombok: final 필드를 매개변수로 받는 생성자 자동 생성
@Slf4j // Lombok: 로깅을 위한 Logger 자동 생성
public class RedisSubscribeListener implements MessageListener {

    /**
     * Redis 데이터 역직렬화를 위한 RedisTemplate 객체
     * 
     * 수신된 바이트 배열 형태의 메시지를 문자열로 변환하기 위해 사용됩니다.
     * RedisConfig에서 설정된 StringRedisSerializer를 통해 
     * 바이트 데이터를 문자열로 안전하게 변환합니다.
     */
    private final RedisTemplate<String, Object> template;

    /**
     * JSON과 Java 객체 간 변환을 담당하는 ObjectMapper
     * 
     * Jackson 라이브러리의 핵심 클래스로, 다음 기능을 제공합니다:
     * - JSON 문자열 -> Java 객체 (역직렬화)
     * - Java 객체 -> JSON 문자열 (직렬화)
     * 
     * 이 프로젝트에서는 Redis에서 수신한 JSON 형태의 메시지를
     * MessageDto 객체로 변환하는 데 사용됩니다.
     */
    private final ObjectMapper objectMapper;

    /**
     * Redis 메시지 수신 시 자동으로 호출되는 콜백 메서드
     * 
     * 이 메서드는 MessageListener 인터페이스의 구현체로,
     * Redis 채널에서 메시지가 발행될 때마다 자동으로 실행됩니다.
     * 
     * 처리 과정:
     * 1. 수신된 바이트 배열을 문자열로 변환
     * 2. JSON 문자열을 MessageDto 객체로 역직렬화
     * 3. 메시지 내용 로깅
     * 4. 추가 비즈니스 로직 처리 영역
     * 
     * 메시지 변환 예시:
     * 바이트 배열 -> "{\"message\":\"안녕\",\"sender\":\"user1\",\"roomId\":\"room1\"}"
     * JSON 문자열 -> MessageDto 객체
     * 
     * @param message Redis에서 수신된 메시지 객체 (바이트 배열 형태)
     * @param pattern 메시지를 수신한 채널의 패턴 정보 (채널명을 바이트 배열로 저장)
     * 
     * 주의사항:
     * - 이 메서드는 멀티스레드 환경에서 실행될 수 있으므로 스레드 안전성 고려 필요
     * - 예외 발생 시 로그로 기록하되 애플리케이션 중단은 방지
     * - 무거운 작업은 별도 스레드나 비동기 처리 권장
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // 1단계: 바이트 배열을 문자열로 역직렬화
            // Redis에서 수신된 메시지 본문(body)을 문자열로 변환
            // getStringSerializer(): RedisTemplate에 설정된 문자열 직렬화 방식 사용
            // deserialize(): 바이트 배열을 문자열로 변환하는 메서드
            String publishMessage = template
                    .getStringSerializer() // StringRedisSerializer 인스턴스 획득
                    .deserialize(message.getBody()); // 메시지 본문을 문자열로 변환

            // 2단계: JSON 문자열을 MessageDto 객체로 역직렬화
            // readValue(): JSON 문자열을 지정된 클래스 타입의 객체로 변환
            // 첫 번째 매개변수: 변환할 JSON 문자열
            // 두 번째 매개변수: 변환 대상 클래스 (MessageDto.class)
            MessageDto messageDto = objectMapper.readValue(publishMessage, MessageDto.class);

            // 3단계: 수신된 메시지 정보 로깅
            // 디버깅 및 모니터링을 위해 채널 정보와 메시지 내용을 로그에 기록
            log.info("Redis Subscribe Channel : " + messageDto.getRoomId());
            log.info("Redis SUB Message : {}", publishMessage);

            // 4단계: 추가 비즈니스 로직 처리 영역
            // 여기에 수신된 메시지를 처리하는 비즈니스 로직을 구현할 수 있습니다.
            // 
            // 구현 가능한 기능들:
            // - 데이터베이스에 메시지 저장
            // - 웹소켓을 통한 실시간 클라이언트 전송
            // - 외부 API 호출
            // - 이메일/SMS 알림 발송
            // - 메시지 필터링 및 라우팅
            // - 통계 정보 수집
            //
            // 예시 코드:
            // messageService.saveMessage(messageDto);           // DB 저장
            // webSocketService.broadcastToRoom(messageDto);     // 웹소켓 전송
            // notificationService.sendAlert(messageDto);        // 알림 발송
            
            // TODO: 여기에 실제 메시지 처리 로직 구현
            // 현재는 로깅만 수행하며, 필요에 따라 추가 처리 로직을 구현할 수 있습니다.

        } catch (JsonProcessingException e) {
            // JSON 파싱 오류 처리
            // 잘못된 형식의 JSON이 수신되었거나, MessageDto로 변환할 수 없는 경우 발생
            // 
            // 발생 가능한 상황:
            // - 잘못된 JSON 형식 수신
            // - MessageDto에 없는 필드 포함
            // - 데이터 타입 불일치
            // - 인코딩 문제
            
            log.error("JSON 파싱 오류 발생: {}", e.getMessage());
            log.error("수신된 메시지: {}", new String(message.getBody())); // 원본 메시지 로깅 (디버깅용)
            
            // 추가적인 에러 처리 로직을 여기에 구현할 수 있습니다:
            // - 에러 메트릭 수집
            // - 관리자 알림 발송
            // - 데드 레터 큐로 메시지 이동
            // - 재시도 로직 구현
            
        } catch (Exception e) {
            // 기타 예상치 못한 예외 처리
            // JSON 파싱 외의 다른 오류들을 포괄적으로 처리
            
            log.error("메시지 처리 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            
            // 예외 상황에서의 복구 로직을 여기에 구현할 수 있습니다:
            // - 시스템 안정성 확보를 위한 처리
            // - 장애 상황 모니터링 및 알림
        }
    }
}