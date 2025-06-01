package org.example.springbootpubsub.domain.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springbootpubsub.domain.dto.MessageDto;
import org.example.springbootpubsub.domain.service.RedisPubService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Redis Pub/Sub 기능을 웹 UI로 제공하는 MVC 컨트롤러
 * 
 * 이 컨트롤러는 사용자가 웹 브라우저를 통해 Redis Pub/Sub 기능을 
 * 쉽게 테스트할 수 있도록 타임리프 기반의 웹 인터페이스를 제공합니다.
 * 
 * 주요 기능:
 * - 메시지 발송 폼 제공
 * - 구독 취소 폼 제공  
 * - 실시간 피드백 (성공/오류 메시지)
 * - 사용자 친화적인 UI
 * 
 * MVC 패턴 적용:
 * - Model: 뷰에 전달할 데이터 (MessageDto, 메시지 등)
 * - View: 타임리프 템플릿 (index.html)
 * - Controller: 사용자 요청 처리 및 응답 (이 클래스)
 * 
 * 사용 대상:
 * - 개발자 (테스트 및 디버깅)
 * - QA 엔지니어 (기능 검증)
 * - 시스템 관리자 (운영 모니터링)
 * - 데모 및 교육 목적
 * 
 * @author Redis Pub/Sub Team
 * @version 1.0
 * @since 2024
 */
@Controller // Spring MVC 컨트롤러임을 명시, 뷰 이름 반환 가능
@RequiredArgsConstructor // Lombok: final 필드를 매개변수로 받는 생성자 자동 생성
@Slf4j // Lombok: 로깅을 위한 Logger 자동 생성
public class WebController {

    /**
     * Redis Pub/Sub 비즈니스 로직을 처리하는 서비스
     * 
     * 웹 컨트롤러와 REST 컨트롤러가 동일한 서비스를 사용하여
     * 비즈니스 로직의 일관성과 코드 재사용성을 보장합니다.
     */
    private final RedisPubService redisPubService;

    /**
     * 메인 페이지를 렌더링하는 엔드포인트
     * 
     * 이 메서드는 사용자가 루트 URL(/)에 접근했을 때 호출되며,
     * Redis Pub/Sub 테스트를 위한 메인 페이지를 표시합니다.
     * 
     * 처리 과정:
     * 1. 빈 MessageDto 객체를 모델에 추가 (폼 바인딩용)
     * 2. "index" 뷰 이름 반환 (templates/index.html 렌더링)
     * 3. 타임리프가 HTML 페이지 생성하여 응답
     * 
     * 모델 데이터:
     * - messageDto: 메시지 발송 폼과 바인딩될 빈 객체
     * 
     * 렌더링 결과:
     * - 메시지 발송 폼
     * - 구독 취소 폼
     * - 사용법 안내
     * - API 문서
     * 
     * @param model Spring MVC의 Model 객체, 뷰에 전달할 데이터 저장
     * @return 뷰 이름 ("index" -> templates/index.html 렌더링)
     */
    @GetMapping("/") // HTTP GET 메서드로 루트 경로 매핑
    public String index(Model model) {
        // 타임리프 폼에서 사용할 빈 MessageDto 객체 추가
        // th:object="${messageDto}"와 바인딩됩니다
        model.addAttribute("messageDto", new MessageDto());
        
        log.info("메인 페이지 요청 처리");
        
        // 뷰 이름 반환 (src/main/resources/templates/index.html)
        return "index";
    }

    /**
     * 웹 폼을 통한 메시지 발송 처리 엔드포인트
     * 
     * 이 메서드는 사용자가 웹 폼에서 메시지 정보를 입력하고 
     * "메시지 발송" 버튼을 클릭했을 때 호출됩니다.
     * 
     * 처리 과정:
     * 1. 폼 데이터에서 채널명과 MessageDto 추출
     * 2. RedisPubService를 통해 메시지 발행 및 채널 구독
     * 3. 성공/실패 메시지를 모델에 추가
     * 4. 폼 초기화를 위한 새로운 MessageDto 객체 추가
     * 5. 동일한 페이지로 리다이렉트 (PRG 패턴)
     * 
     * 폼 바인딩:
     * - @RequestParam channel: name="channel" 폼 필드와 매핑
     * - @ModelAttribute messageDto: th:object="${messageDto}"와 매핑
     * 
     * 에러 처리:
     * - try-catch로 예외 상황 처리
     * - 사용자에게 친화적인 에러 메시지 제공
     * - 로그에 상세한 에러 정보 기록
     * 
     * 사용자 피드백:
     * - 성공 시: "메시지가 성공적으로 전송되었습니다!"
     * - 실패 시: "메시지 전송 중 오류가 발생했습니다: {오류내용}"
     * 
     * @param channel 폼에서 입력받은 Redis 채널명
     * @param messageDto 폼에서 입력받은 메시지 정보 (발신자, 방ID, 메시지 내용)
     * @param model 뷰에 전달할 데이터를 저장하는 Model 객체
     * @return 뷰 이름 ("index" -> templates/index.html 렌더링)
     */
    @PostMapping("/send-message") // HTTP POST 메서드로 /send-message 경로 매핑
    public String sendMessage(@RequestParam String channel, // 폼의 name="channel" 필드와 매핑
                            @ModelAttribute MessageDto messageDto, // 폼의 MessageDto 객체와 매핑
                            Model model) { // 뷰에 전달할 데이터를 저장하는 Model 객체
        try {
            // 메시지 발송 요청 로깅 (디버깅 및 모니터링)
            log.info("Sending message to channel: {}, message: {}", channel, messageDto);
            
            // 실제 Redis Pub/Sub 처리 (서비스 계층으로 위임)
            // 1. 채널 구독 시작
            // 2. 메시지 발행
            redisPubService.pubMsgChannel(channel, messageDto);
            
            // 성공 메시지를 모델에 추가 (타임리프에서 표시)
            model.addAttribute("successMessage", "메시지가 성공적으로 전송되었습니다!");
            
            log.info("웹 폼을 통한 메시지 발송 성공 - 채널: {}", channel);
            
        } catch (Exception e) {
            // 예외 상황 처리
            // Redis 연결 오류, 직렬화 오류 등 다양한 예외 가능
            log.error("Error sending message", e);
            
            // 사용자 친화적인 에러 메시지 생성
            model.addAttribute("errorMessage", "메시지 전송 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        // 폼 초기화를 위한 새로운 빈 객체 추가
        // 이전 입력값이 남아있지 않도록 처리
        model.addAttribute("messageDto", new MessageDto());
        
        // 동일한 페이지로 돌아가서 결과 표시
        // PRG(Post-Redirect-Get) 패턴으로 새로고침 시 중복 전송 방지
        return "index";
    }

    /**
     * 웹 폼을 통한 채널 구독 취소 처리 엔드포인트
     * 
     * 이 메서드는 사용자가 구독 취소 폼에서 채널명을 입력하고
     * "구독 취소" 버튼을 클릭했을 때 호출됩니다.
     * 
     * 처리 과정:
     * 1. 폼 데이터에서 채널명 추출
     * 2. RedisPubService를 통해 채널 구독 해제
     * 3. 성공/실패 메시지를 모델에 추가
     * 4. 동일한 페이지로 리다이렉트
     * 
     * 에러 처리:
     * - Redis 연결 오류 시 사용자에게 알림
     * - 존재하지 않는 채널 구독 취소 시도 등 예외 처리
     * 
     * 사용자 피드백:
     * - 성공 시: "채널 구독이 취소되었습니다!"
     * - 실패 시: "구독 취소 중 오류가 발생했습니다: {오류내용}"
     * 
     * 주의사항:
     * - 현재 구현에서는 모든 채널 구독이 해제될 수 있음
     * - 사용자에게 이에 대한 적절한 안내 필요
     * 
     * @param channel 폼에서 입력받은 구독을 취소할 Redis 채널명
     * @param model 뷰에 전달할 데이터를 저장하는 Model 객체
     * @return 뷰 이름 ("index" -> templates/index.html 렌더링)
     */
    @PostMapping("/cancel-subscription") // HTTP POST 메서드로 /cancel-subscription 경로 매핑
    public String cancelSubscription(@RequestParam String channel, // 폼의 name="channel" 필드와 매핑
                                   Model model) { // 뷰에 전달할 데이터 컨테이너
        try {
            // 구독 취소 요청 로깅
            log.info("Canceling subscription for channel: {}", channel);
            
            // 실제 구독 취소 처리 (서비스 계층으로 위임)
            // RedisMessageListenerContainer에서 리스너 제거
            redisPubService.cancelSubChannel(channel);
            
            // 성공 메시지를 모델에 추가
            model.addAttribute("successMessage", "채널 구독이 취소되었습니다!");
            
            log.info("웹 폼을 통한 구독 취소 성공 - 채널: {}", channel);
            
        } catch (Exception e) {
            // 예외 상황 처리
            log.error("Error canceling subscription", e);
            
            // 사용자 친화적인 에러 메시지 생성
            model.addAttribute("errorMessage", "구독 취소 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        // 폼 초기화를 위한 새로운 빈 객체 추가
        model.addAttribute("messageDto", new MessageDto());
        
        // 현재 구독 중인 채널 목록 추가
        try {
            Set<String> subscribedChannels = redisPubService.getSubscribedChannels();
            model.addAttribute("subscribedChannels", subscribedChannels);
        } catch (Exception e) {
            log.error("Error getting subscribed channels", e);
            model.addAttribute("subscribedChannels", new HashSet<>());
        }
        
        // 동일한 페이지로 돌아가서 결과 표시
        return "index";
    }

    /**
     * 구독 중인 채널 목록을 조회하는 웹 API 엔드포인트
     * 
     * 이 엔드포인트는 웹 페이지에서 Ajax 요청을 통해
     * 현재 구독 중인 채널 목록을 실시간으로 조회할 수 있도록 합니다.
     * 
     * HTTP 요청 형태:
     * GET /subscribed-channels
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
     * - 웹 페이지에서 실시간 구독 상태 업데이트
     * - JavaScript를 통한 동적 UI 구성
     * - 페이지 새로고침 없이 구독 상태 확인
     * 
     * @return Set<String> 현재 구독 중인 채널명들의 집합
     */
    @GetMapping("/subscribed-channels")
    @ResponseBody // JSON 응답을 위한 어노테이션
    public Set<String> getSubscribedChannels() {
        log.info("웹에서 구독 채널 목록 조회 요청");
        
        try {
            Set<String> subscribedChannels = redisPubService.getSubscribedChannels();
            log.info("웹 구독 채널 목록 조회 성공 - {}개 채널", subscribedChannels.size());
            return subscribedChannels;
            
        } catch (Exception e) {
            log.error("웹 구독 채널 목록 조회 실패", e);
            return new HashSet<>();
        }
    }

    /**
     * 구독 상태 상세 정보를 조회하는 웹 API 엔드포인트
     * 
     * 이 엔드포인트는 웹 페이지에서 Ajax 요청을 통해
     * 구독 상태에 대한 상세 정보를 조회할 수 있도록 합니다.
     * 
     * HTTP 요청 형태:
     * GET /subscription-status
     * 
     * @return Map<String, Object> 구독 상태에 대한 상세 정보
     */
    @GetMapping("/subscription-status")
    @ResponseBody // JSON 응답을 위한 어노테이션
    public Map<String, Object> getSubscriptionStatus() {
        log.info("웹에서 구독 상태 상세 정보 조회 요청");
        
        try {
            Map<String, Object> subscriptionStatus = redisPubService.getSubscriptionStatus();
            log.info("웹 구독 상태 상세 정보 조회 성공");
            return subscriptionStatus;
            
        } catch (Exception e) {
            log.error("웹 구독 상태 상세 정보 조회 실패", e);
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("error", e.getMessage());
            errorStatus.put("errorTime", java.time.LocalDateTime.now());
            return errorStatus;
        }
    }
} 