package org.example.springbootpubsub.domain.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springbootpubsub.domain.dto.MessageDto;
import org.example.springbootpubsub.domain.dto.UserDto;
import org.example.springbootpubsub.domain.service.UserSubscriptionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 사용자 관리와 채널별 구독자 관리를 담당하는 컨트롤러
 * 
 * 이 컨트롤러는 다수의 사용자가 참여하는 Redis Pub/Sub 환경에서
 * 사용자 등록, 로그인, 구독 관리 등의 기능을 웹 인터페이스로 제공합니다.
 * 
 * 주요 기능:
 * - 사용자 등록 및 세션 관리
 * - 사용자별 채널 구독/구독취소
 * - 채널별 구독자 목록 조회
 * - 사용자 관리 페이지 제공
 * - 채널 관리 페이지 제공
 * 
 * @author Redis Pub/Sub Team
 * @version 1.0
 * @since 2024
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/users")
public class UserManagementController {

    private final UserSubscriptionService userSubscriptionService;

    /**
     * 사용자 관리 메인 페이지를 표시하는 엔드포인트
     * 
     * @param model 뷰에 전달할 데이터
     * @return 사용자 관리 페이지 뷰 이름
     */
    @GetMapping("/")
    public String userManagementPage(Model model, HttpSession session) {
        // 현재 로그인한 사용자 정보
        String currentUserId = (String) session.getAttribute("userId");
        if (currentUserId != null) {
            UserDto currentUser = userSubscriptionService.getUser(currentUserId);
            model.addAttribute("currentUser", currentUser);
        }
        
        // 전체 사용자 목록
        List<UserDto> allUsers = userSubscriptionService.getAllUsers();
        model.addAttribute("allUsers", allUsers);
        
        // 채널별 구독자 수
        Map<String, Integer> channelCounts = userSubscriptionService.getChannelSubscriberCounts();
        model.addAttribute("channelCounts", channelCounts);
        
        // 시스템 통계
        Map<String, Object> statistics = userSubscriptionService.getSystemStatistics();
        model.addAttribute("statistics", statistics);
        
        log.info("사용자 관리 페이지 요청 - 현재 사용자: {}", currentUserId);
        return "user-management";
    }

    /**
     * 사용자 로그인/등록 처리 엔드포인트
     * 
     * @param userName 사용자 이름
     * @param session HTTP 세션
     * @param model 뷰에 전달할 데이터
     * @return 리다이렉트 URL
     */
    @PostMapping("/login")
    public String loginUser(@RequestParam String userName, HttpSession session, Model model) {
        try {
            // 세션에서 기존 사용자 ID 확인하거나 새로 생성
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
                session.setAttribute("userId", userId);
            }
            
            // 사용자 등록/업데이트
            UserDto user = userSubscriptionService.registerUser(userId, userName);
            session.setAttribute("userName", userName);
            
            model.addAttribute("successMessage", "환영합니다, " + userName + "님!");
            
            log.info("사용자 로그인 처리 완료: {} ({})", userName, userId);
            
        } catch (Exception e) {
            log.error("사용자 로그인 처리 중 오류 발생", e);
            model.addAttribute("errorMessage", "로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "redirect:/users/";
    }

    /**
     * 사용자 로그아웃 처리 엔드포인트
     * 
     * @param session HTTP 세션
     * @return 리다이렉트 URL
     */
    @PostMapping("/logout")
    public String logoutUser(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        String userName = (String) session.getAttribute("userName");
        
        if (userId != null) {
            userSubscriptionService.logoutUser(userId);
            session.invalidate();
            
            log.info("사용자 로그아웃 완료: {} ({})", userName, userId);
        }
        
        return "redirect:/";
    }

    /**
     * 사용자가 채널을 구독하는 엔드포인트
     * 
     * @param channel 구독할 채널명
     * @param messageDto 함께 보낼 메시지 (선택사항)
     * @param session HTTP 세션
     * @param model 뷰에 전달할 데이터
     * @return 리다이렉트 URL
     */
    @PostMapping("/subscribe")
    public String subscribeToChannel(@RequestParam String channel, 
                                   @ModelAttribute MessageDto messageDto,
                                   HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        String userName = (String) session.getAttribute("userName");
        
        if (userId == null) {
            model.addAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/users/";
        }
        
        try {
            // 메시지 발신자 정보 설정
            if (messageDto.getMessage() != null && !messageDto.getMessage().trim().isEmpty()) {
                messageDto.setSender(userName);
                if (messageDto.getRoomId() == null || messageDto.getRoomId().trim().isEmpty()) {
                    messageDto.setRoomId(channel);
                }
            } else {
                messageDto = null; // 빈 메시지는 null로 처리
            }
            
            boolean success = userSubscriptionService.subscribeUserToChannel(userId, channel, messageDto);
            
            if (success) {
                model.addAttribute("successMessage", 
                    String.format("채널 '%s' 구독이 시작되었습니다!", channel));
            } else {
                model.addAttribute("errorMessage", "채널 구독에 실패했습니다.");
            }
            
        } catch (Exception e) {
            log.error("채널 구독 처리 중 오류 발생", e);
            model.addAttribute("errorMessage", "채널 구독 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "redirect:/users/";
    }

    /**
     * 사용자가 채널 구독을 취소하는 엔드포인트
     * 
     * @param channel 구독을 취소할 채널명
     * @param session HTTP 세션
     * @param model 뷰에 전달할 데이터
     * @return 리다이렉트 URL
     */
    @PostMapping("/unsubscribe")
    public String unsubscribeFromChannel(@RequestParam String channel, 
                                       HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        
        if (userId == null) {
            model.addAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/users/";
        }
        
        try {
            boolean success = userSubscriptionService.unsubscribeUserFromChannel(userId, channel);
            
            if (success) {
                model.addAttribute("successMessage", 
                    String.format("채널 '%s' 구독이 취소되었습니다.", channel));
            } else {
                model.addAttribute("errorMessage", "채널 구독 취소에 실패했습니다.");
            }
            
        } catch (Exception e) {
            log.error("채널 구독 취소 처리 중 오류 발생", e);
            model.addAttribute("errorMessage", "채널 구독 취소 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "redirect:/users/";
    }

    /**
     * 특정 채널의 구독자 목록을 조회하는 엔드포인트
     * 
     * @param channel 조회할 채널명
     * @return 채널 구독자 목록 JSON 응답
     */
    @GetMapping("/channel/{channel}/subscribers")
    @ResponseBody
    public List<UserDto> getChannelSubscribers(@PathVariable String channel) {
        log.info("채널 구독자 목록 조회 요청: {}", channel);
        return userSubscriptionService.getChannelSubscribers(channel);
    }

    /**
     * 특정 사용자의 구독 채널 목록을 조회하는 엔드포인트
     * 
     * @param userId 조회할 사용자 ID
     * @return 사용자 구독 채널 목록 JSON 응답
     */
    @GetMapping("/user/{userId}/channels")
    @ResponseBody
    public Set<String> getUserChannels(@PathVariable String userId) {
        log.info("사용자 구독 채널 목록 조회 요청: {}", userId);
        return userSubscriptionService.getUserSubscribedChannels(userId);
    }

    /**
     * 현재 로그인한 사용자의 구독 채널 목록을 조회하는 엔드포인트
     * 
     * @param session HTTP 세션
     * @return 현재 사용자 구독 채널 목록 JSON 응답
     */
    @GetMapping("/my-channels")
    @ResponseBody
    public Set<String> getMyChannels(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        
        if (userId == null) {
            return Set.of(); // 빈 집합 반환
        }
        
        return userSubscriptionService.getUserSubscribedChannels(userId);
    }

    /**
     * 전체 사용자 목록을 조회하는 엔드포인트
     * 
     * @return 전체 사용자 목록 JSON 응답
     */
    @GetMapping("/all")
    @ResponseBody
    public List<UserDto> getAllUsers() {
        log.info("전체 사용자 목록 조회 요청");
        return userSubscriptionService.getAllUsers();
    }

    /**
     * 채널별 구독자 수 통계를 조회하는 엔드포인트
     * 
     * @return 채널별 구독자 수 JSON 응답
     */
    @GetMapping("/channel-stats")
    @ResponseBody
    public Map<String, Integer> getChannelStats() {
        log.info("채널별 구독자 수 통계 조회 요청");
        return userSubscriptionService.getChannelSubscriberCounts();
    }

    /**
     * 시스템 전체 통계를 조회하는 엔드포인트
     * 
     * @return 시스템 통계 JSON 응답
     */
    @GetMapping("/system-stats")
    @ResponseBody
    public Map<String, Object> getSystemStats() {
        log.info("시스템 통계 조회 요청");
        return userSubscriptionService.getSystemStatistics();
    }

    /**
     * 채널별 구독자 상세 정보를 조회하는 웹 API 엔드포인트
     * 
     * @param channel 조회할 채널명
     * @return 채널 상세 정보 JSON 응답
     */
    @GetMapping("/channel-details")
    @ResponseBody
    public Map<String, Object> getChannelDetails(@RequestParam String channel) {
        Map<String, Object> details = new HashMap<>();
        
        try {
            List<UserDto> subscribers = userSubscriptionService.getChannelSubscribers(channel);
            details.put("channel", channel);
            details.put("subscriberCount", subscribers.size());
            details.put("subscribers", subscribers);
            
            log.info("채널 상세 정보 조회 완료: {} ({}명 구독)", channel, subscribers.size());
            
        } catch (Exception e) {
            log.error("채널 상세 정보 조회 실패: {}", channel, e);
            details.put("error", e.getMessage());
        }
        
        return details;
    }

    /**
     * 비활성 사용자 정리를 수행하는 관리자 엔드포인트
     * 
     * @param minutes 비활성 기준 시간 (분, 기본값: 30분)
     * @return 정리 결과 JSON 응답
     */
    @PostMapping("/cleanup")
    @ResponseBody
    public Map<String, Object> cleanupInactiveUsers(@RequestParam(defaultValue = "30") int minutes) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            int cleanedCount = userSubscriptionService.cleanupInactiveUsers(minutes);
            result.put("success", true);
            result.put("cleanedUserCount", cleanedCount);
            result.put("message", String.format("%d명의 비활성 사용자를 정리했습니다.", cleanedCount));
            
            log.info("비활성 사용자 정리 완료: {}명 (기준: {}분)", cleanedCount, minutes);
            
        } catch (Exception e) {
            log.error("비활성 사용자 정리 실패", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
} 