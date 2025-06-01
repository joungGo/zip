package org.example.springbootpubsub.domain.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springbootpubsub.domain.dto.MessageDto;
import org.example.springbootpubsub.domain.service.RedisPubService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * Redis Pub/Sub 기능을 제공하는 REST API 컨트롤러
 * 
 * 이 컨트롤러는 외부 시스템이나 클라이언트가 HTTP 요청을 통해 
 * Redis Pub/Sub 기능을 사용할 수 있도록 REST API를 제공합니다.
 * 
 * 주요 기능:
 * - 특정 채널에 메시지 발행
 * - 채널 구독 취소
 * - JSON 형태의 요청/응답 처리
 * - 에러 핸들링 및 로깅
 * 
 * API 설계 특징:
 * - RESTful API 규칙 준수
 * - JSON 데이터 형식 사용
 * - 명확한 HTTP 메서드 활용 (POST)
 * - 쿼리 파라미터와 요청 본문 구분
 * 
 * 사용 대상:
 * - 외부 애플리케이션
 * - 마이크로서비스 간 통신
 * - API 테스트 도구 (Postman, curl 등)
 * - 자동화 스크립트
 * 
 * @author Redis Pub/Sub Team
 * @version 1.0
 * @since 2024
 */
@RestController // REST API 컨트롤러임을 명시, @Controller + @ResponseBody 합성
@RequiredArgsConstructor // Lombok: final 필드를 매개변수로 받는 생성자 자동 생성
@Slf4j // Lombok: 로깅을 위한 Logger 자동 생성
@RequestMapping("/redis/pubsub") // 모든 엔드포인트의 공통 경로 prefix 설정
public class RedisPubSubController {

    /**
     * Redis Pub/Sub 비즈니스 로직을 처리하는 서비스
     * 
     * RedisPubService는 실제 Redis와의 상호작용을 담당하며,
     * 이 컨트롤러는 HTTP 요청을 받아 서비스 계층으로 위임하는 역할을 합니다.
     * 
     * 계층 분리의 이점:
     * - 비즈니스 로직과 웹 계층 분리
     * - 테스트 용이성 향상
     * - 코드 재사용성 증대
     * - 관심사의 분리 (Separation of Concerns)
     */
    private final RedisPubService redisSubscribeService;

    /**
     * 특정 Redis 채널에 메시지를 발행하는 API 엔드포인트
     * 
     * 이 엔드포인트는 클라이언트로부터 채널명과 메시지 데이터를 받아
     * Redis 채널에 메시지를 발행합니다. 발행과 동시에 해당 채널을 구독하여
     * 실시간으로 메시지 수신을 확인할 수 있습니다.
     * 
     * HTTP 요청 형태:
     * POST /redis/pubsub/send?channel=chat-room-1
     * Content-Type: application/json
     * Body: {
     *   "message": "안녕하세요",
     *   "sender": "user1", 
     *   "roomId": "room1"
     * }
     * 
     * 처리 과정:
     * 1. 클라이언트로부터 HTTP POST 요청 수신
     * 2. URL 쿼리 파라미터에서 채널명 추출
     * 3. 요청 본문에서 MessageDto 객체 역직렬화
     * 4. RedisPubService를 통해 메시지 발행 및 채널 구독
     * 5. 성공 시 200 OK 응답, 실패 시 에러 응답
     * 
     * 사용 시나리오:
     * - 채팅 애플리케이션에서 메시지 전송
     * - 알림 시스템에서 브로드캐스트 메시지 발송
     * - 마이크로서비스 간 이벤트 전달
     * - 실시간 데이터 동기화
     * 
     * @param channel 메시지를 발행할 Redis 채널명 (필수 파라미터)
     *                예: "chat-room-1", "notifications", "events"
     * @param message 발행할 메시지 데이터를 담은 MessageDto 객체
     *                JSON 형태로 요청 본문에 포함
     * 
     * @throws org.springframework.web.bind.MethodArgumentNotValidException 유효하지 않은 요청 데이터
     * @throws org.springframework.data.redis.RedisConnectionFailureException Redis 연결 실패
     * @throws org.springframework.http.converter.HttpMessageNotReadableException JSON 파싱 오류
     */
    @PostMapping("/send") // HTTP POST 메서드로 /redis/pubsub/send 경로 매핑
    public void sendMessage(@RequestParam(required = true) String channel, // URL 쿼리 파라미터에서 채널명 추출 (필수)
                           @RequestBody MessageDto message) { // HTTP 요청 본문을 MessageDto 객체로 변환
        
        // 요청 정보 로깅 (디버깅 및 모니터링 목적)
        log.info("Redis Pub MSG Channel = {}", channel);
        log.info("메시지 발행 요청 - 채널: {}, 발신자: {}, 방ID: {}, 내용: {}", 
                 channel, message.getSender(), message.getRoomId(), message.getMessage());
        
        // 실제 비즈니스 로직 실행 (서비스 계층으로 위임)
        // 1. 지정된 채널 구독 시작
        // 2. 메시지 발행
        // 3. 결과 로깅
        redisSubscribeService.pubMsgChannel(channel, message);
        
        // 성공 응답 (void 반환이므로 200 OK 상태 코드 자동 반환)
        log.info("메시지 발행 API 처리 완료");
    }

    /**
     * 특정 Redis 채널의 구독을 해제하는 API 엔드포인트
     * 
     * 이 엔드포인트는 더 이상 특정 채널의 메시지를 수신하지 않도록 
     * 구독을 취소합니다. 리소스 정리와 불필요한 메시지 수신 방지를 위해 사용됩니다.
     * 
     * HTTP 요청 형태:
     * POST /redis/pubsub/cancle?channel=chat-room-1
     * 
     * 처리 과정:
     * 1. 클라이언트로부터 HTTP POST 요청 수신
     * 2. URL 쿼리 파라미터에서 취소할 채널명 추출
     * 3. RedisPubService를 통해 채널 구독 해제
     * 4. 성공 시 200 OK 응답
     * 
     * 사용 시나리오:
     * - 채팅방에서 나가기
     * - 알림 구독 해제
     * - 임시 이벤트 채널 정리
     * - 애플리케이션 종료 시 리소스 해제
     * 
     * 주의사항:
     * - URL에 오타가 있습니다: "cancle" -> "cancel"이 올바름
     * - 현재 구현에서는 모든 채널 구독이 해제될 수 있음
     * 
     * @param channel 구독을 취소할 Redis 채널명
     *                예: "chat-room-1", "notifications"
     * 
     * @throws org.springframework.web.bind.MissingServletRequestParameterException 채널 파라미터 누락
     * @throws org.springframework.data.redis.RedisConnectionFailureException Redis 연결 실패
     * 
     * TODO: URL 경로 수정 ("/cancle" -> "/cancel")
     */
    @PostMapping("/cancel") // TODO: 오타 수정 필요 -> "/cancel"
    public void cancelSubChannel(@RequestParam String channel) {
        // 요청 정보 로깅
        log.info("Redis SUB Cancel Channel = {}", channel);
        
        // 실제 비즈니스 로직 실행 (서비스 계층으로 위임)
        // RedisMessageListenerContainer에서 지정된 채널의 구독 해제
        redisSubscribeService.cancelSubChannel(channel);
        
        // 성공 응답 (void 반환이므로 200 OK 상태 코드 자동 반환)
        log.info("구독 취소 API 처리 완료");
    }

    /**
     * 현재 구독 중인 Redis 채널 목록을 조회하는 API 엔드포인트
     * 
     * 이 엔드포인트는 현재 애플리케이션에서 구독하고 있는 
     * 모든 Redis 채널들의 목록을 반환합니다.
     * 
     * HTTP 요청 형태:
     * GET /redis/pubsub/subscribed-channels
     * 
     * 응답 형태:
     * ```json
     * [
     *   "chat-room-1",
     *   "notifications", 
     *   "events"
     * ]
     * ```
     * 
     * 사용 시나리오:
     * - 현재 활성 채널 확인
     * - 관리자 대시보드에서 구독 상태 모니터링
     * - 디버깅 및 시스템 상태 점검
     * - 클라이언트에서 구독 채널 목록 표시
     * 
     * 특징:
     * - 실시간 구독 상태 반영
     * - 빠른 응답 속도 (메모리 기반 조회)
     * - JSON 형태로 구조화된 응답
     * 
     * @return Set<String> 현재 구독 중인 채널명들의 집합
     * 
     * @throws org.springframework.data.redis.RedisConnectionFailureException Redis 연결 실패 시
     */
    @GetMapping("/subscribed-channels")
    public Set<String> getSubscribedChannels() {
        // 요청 로깅
        log.info("구독 중인 채널 목록 조회 요청");
        
        // 구독 채널 목록 조회 (서비스 계층으로 위임)
        Set<String> subscribedChannels = redisSubscribeService.getSubscribedChannels();
        
        // 응답 로깅
        log.info("구독 채널 목록 API 응답 - 총 {}개 채널: {}", 
                 subscribedChannels.size(), subscribedChannels);
        
        return subscribedChannels;
    }

    /**
     * 구독 상태에 대한 상세 정보를 조회하는 API 엔드포인트
     * 
     * 이 엔드포인트는 단순한 채널 목록이 아닌, 구독 상태에 대한
     * 종합적인 정보를 제공합니다. 관리자나 개발자가 시스템 상태를
     * 자세히 파악하는 데 유용합니다.
     * 
     * HTTP 요청 형태:
     * GET /redis/pubsub/subscription-status
     * 
     * 응답 형태:
     * ```json
     * {
     *   "subscribedChannels": ["chat-room-1", "notifications"],
     *   "totalChannelCount": 2,
     *   "channelSubscriptionDetails": {
     *     "chat-room-1": "1500ms",
     *     "notifications": "3000ms"
     *   },
     *   "isRunning": true,
     *   "isActive": true,
     *   "checkTime": "2024-01-15T10:30:45"
     * }
     * ```
     * 
     * 포함 정보:
     * - subscribedChannels: 현재 구독 중인 채널 목록
     * - totalChannelCount: 총 구독 채널 수
     * - channelSubscriptionDetails: 채널별 구독 유지 시간
     * - isRunning: 리스너 컨테이너 실행 상태
     * - isActive: 리스너 컨테이너 활성 상태
     * - checkTime: 조회 시간
     * 
     * 사용 시나리오:
     * - 시스템 헬스체크
     * - 성능 모니터링
     * - 장애 진단
     * - 관리자 대시보드 데이터 제공
     * 
     * @return Map<String, Object> 구독 상태에 대한 상세 정보
     */
    @GetMapping("/subscription-status")
    public Map<String, Object> getSubscriptionStatus() {
        // 요청 로깅
        log.info("구독 상태 상세 정보 조회 요청");
        
        // 구독 상태 상세 정보 조회 (서비스 계층으로 위임)
        Map<String, Object> subscriptionStatus = redisSubscribeService.getSubscriptionStatus();
        
        // 응답 로깅
        log.info("구독 상태 상세 정보 API 응답 완료");
        
        return subscriptionStatus;
    }
}