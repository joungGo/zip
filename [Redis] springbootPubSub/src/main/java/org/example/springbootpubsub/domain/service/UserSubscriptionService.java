package org.example.springbootpubsub.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springbootpubsub.domain.dto.MessageDto;
import org.example.springbootpubsub.domain.dto.UserDto;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 사용자별 구독 관리를 담당하는 서비스 클래스
 * 
 * 이 클래스는 다수의 사용자가 다양한 채널을 구독하는 환경에서
 * 사용자와 채널 간의 구독 관계를 관리하고 추적하는 역할을 담당합니다.
 * 
 * 주요 기능:
 * - 사용자 등록 및 세션 관리
 * - 사용자별 채널 구독/구독취소
 * - 채널별 구독자 목록 조회
 * - 사용자별 구독 채널 목록 조회
 * - 실시간 구독 상태 모니터링
 * 
 * 데이터 구조:
 * - users: 전체 사용자 정보 저장 (userId -> UserDto)
 * - channelSubscribers: 채널별 구독자 목록 (channel -> Set<userId>)
 * - userChannels: 사용자별 구독 채널 목록 (userId -> Set<channel>)
 * 
 * Thread Safety:
 * - ConcurrentHashMap 사용으로 멀티스레드 환경에서 안전
 * - 동시 접근 시 데이터 무결성 보장
 * 
 * @author Redis Pub/Sub Team
 * @version 1.0
 * @since 2024
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSubscriptionService {

    /**
     * Redis Pub/Sub 기본 기능을 제공하는 서비스
     * 실제 Redis 구독/발행 작업은 이 서비스를 통해 수행됩니다.
     */
    private final RedisPubService redisPubService;

    /**
     * 전체 사용자 정보를 저장하는 Thread-safe Map
     * Key: 사용자 ID (String)
     * Value: 사용자 정보 (UserDto)
     */
    private final Map<String, UserDto> users = new ConcurrentHashMap<>();

    /**
     * 채널별 구독자 목록을 저장하는 Thread-safe Map
     * Key: 채널명 (String)
     * Value: 해당 채널을 구독하는 사용자 ID들의 집합 (Set<String>)
     * 
     * 예시: {"chat-room-1" -> {"user1", "user2", "user3"}}
     */
    private final Map<String, Set<String>> channelSubscribers = new ConcurrentHashMap<>();

    /**
     * 사용자별 구독 채널 목록을 저장하는 Thread-safe Map
     * Key: 사용자 ID (String)
     * Value: 해당 사용자가 구독하는 채널들의 집합 (Set<String>)
     * 
     * 예시: {"user1" -> {"chat-room-1", "notifications"}}
     */
    private final Map<String, Set<String>> userChannels = new ConcurrentHashMap<>();

    /**
     * 새로운 사용자를 등록하거나 기존 사용자 정보를 업데이트하는 메서드
     * 
     * @param userId 사용자 ID
     * @param userName 사용자 이름
     * @return 등록된 사용자 정보 (UserDto)
     */
    public UserDto registerUser(String userId, String userName) {
        UserDto user = users.get(userId);
        
        if (user == null) {
            // 새로운 사용자 생성
            user = new UserDto(userId, userName);
            user.setSubscribedChannels(new HashSet<>());
            users.put(userId, user);
            userChannels.put(userId, new HashSet<>());
            
            log.info("새로운 사용자 등록: {} ({})", userName, userId);
        } else {
            // 기존 사용자 정보 업데이트
            user.setUserName(userName);
            user.updateLastActiveTime();
            user.setStatus("ONLINE");
            
            log.info("기존 사용자 재접속: {} ({})", userName, userId);
        }
        
        return user;
    }

    /**
     * 사용자가 특정 채널을 구독하는 메서드
     * 
     * @param userId 사용자 ID
     * @param channel 구독할 채널명
     * @param message 채널 구독과 함께 발송할 메시지 (선택사항)
     * @return 구독 성공 여부
     */
    public boolean subscribeUserToChannel(String userId, String channel, MessageDto message) {
        try {
            UserDto user = users.get(userId);
            if (user == null) {
                log.warn("존재하지 않는 사용자의 구독 요청: {}", userId);
                return false;
            }

            // 사용자별 구독 채널 목록에 추가
            Set<String> userChannelSet = userChannels.computeIfAbsent(userId, k -> new HashSet<>());
            boolean wasNewSubscription = userChannelSet.add(channel);

            // 채널별 구독자 목록에 추가
            Set<String> subscriberSet = channelSubscribers.computeIfAbsent(channel, k -> new HashSet<>());
            subscriberSet.add(userId);

            // 사용자 DTO의 구독 채널 목록 업데이트
            user.getSubscribedChannels().add(channel);
            user.updateLastActiveTime();

            if (wasNewSubscription) {
                // 실제 Redis 채널 구독 및 메시지 발행
                if (message != null) {
                    redisPubService.pubMsgChannel(channel, message);
                }
                
                log.info("사용자 [{}]가 채널 [{}] 구독 시작 (총 구독자: {}명)", 
                         userId, channel, subscriberSet.size());
            } else {
                log.debug("사용자 [{}]는 이미 채널 [{}]을 구독 중", userId, channel);
            }

            return true;

        } catch (Exception e) {
            log.error("사용자 구독 처리 중 오류 발생 - 사용자: {}, 채널: {}, 오류: {}", 
                     userId, channel, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 사용자가 특정 채널의 구독을 취소하는 메서드
     * 
     * @param userId 사용자 ID
     * @param channel 구독을 취소할 채널명
     * @return 구독 취소 성공 여부
     */
    public boolean unsubscribeUserFromChannel(String userId, String channel) {
        try {
            UserDto user = users.get(userId);
            if (user == null) {
                log.warn("존재하지 않는 사용자의 구독 취소 요청: {}", userId);
                return false;
            }

            // 사용자별 구독 채널 목록에서 제거
            Set<String> userChannelSet = userChannels.get(userId);
            boolean wasSubscribed = userChannelSet != null && userChannelSet.remove(channel);

            // 채널별 구독자 목록에서 제거
            Set<String> subscriberSet = channelSubscribers.get(channel);
            if (subscriberSet != null) {
                subscriberSet.remove(userId);
                
                // 구독자가 없는 채널은 맵에서 제거
                if (subscriberSet.isEmpty()) {
                    channelSubscribers.remove(channel);
                }
            }

            // 사용자 DTO의 구독 채널 목록 업데이트
            user.getSubscribedChannels().remove(channel);
            user.updateLastActiveTime();

            if (wasSubscribed) {
                // 만약 해당 채널에 더 이상 구독자가 없다면 Redis 구독도 취소
                if (subscriberSet == null || subscriberSet.isEmpty()) {
                    redisPubService.cancelSubChannel(channel);
                }
                
                log.info("사용자 [{}]가 채널 [{}] 구독 취소 (남은 구독자: {}명)", 
                         userId, channel, subscriberSet != null ? subscriberSet.size() : 0);
            } else {
                log.debug("사용자 [{}]는 채널 [{}]을 구독하지 않았음", userId, channel);
            }

            return true;

        } catch (Exception e) {
            log.error("사용자 구독 취소 처리 중 오류 발생 - 사용자: {}, 채널: {}, 오류: {}", 
                     userId, channel, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 특정 채널을 구독하는 사용자 목록을 조회하는 메서드
     * 
     * @param channel 채널명
     * @return 해당 채널을 구독하는 사용자들의 목록
     */
    public List<UserDto> getChannelSubscribers(String channel) {
        Set<String> subscriberIds = channelSubscribers.getOrDefault(channel, new HashSet<>());
        
        List<UserDto> subscribers = subscriberIds.stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        log.info("채널 [{}] 구독자 목록 조회 - {}명", channel, subscribers.size());
        
        return subscribers;
    }

    /**
     * 특정 사용자가 구독 중인 채널 목록을 조회하는 메서드
     * 
     * @param userId 사용자 ID
     * @return 해당 사용자가 구독하는 채널들의 집합
     */
    public Set<String> getUserSubscribedChannels(String userId) {
        Set<String> channels = userChannels.getOrDefault(userId, new HashSet<>());
        
        log.info("사용자 [{}] 구독 채널 목록 조회 - {}개", userId, channels.size());
        
        return new HashSet<>(channels); // 복사본 반환
    }

    /**
     * 모든 등록된 사용자 목록을 조회하는 메서드
     * 
     * @return 전체 사용자 목록
     */
    public List<UserDto> getAllUsers() {
        List<UserDto> userList = new ArrayList<>(users.values());
        
        // 마지막 활동 시간 기준으로 정렬 (최근 활동 순)
        userList.sort((u1, u2) -> u2.getLastActiveTime().compareTo(u1.getLastActiveTime()));
        
        log.info("전체 사용자 목록 조회 - {}명", userList.size());
        
        return userList;
    }

    /**
     * 현재 활성화된 모든 채널과 각 채널의 구독자 수를 조회하는 메서드
     * 
     * @return 채널별 구독자 수 정보 (Map<채널명, 구독자수>)
     */
    public Map<String, Integer> getChannelSubscriberCounts() {
        Map<String, Integer> channelCounts = new HashMap<>();
        
        for (Map.Entry<String, Set<String>> entry : channelSubscribers.entrySet()) {
            channelCounts.put(entry.getKey(), entry.getValue().size());
        }
        
        log.info("채널별 구독자 수 조회 - {}개 채널", channelCounts.size());
        
        return channelCounts;
    }

    /**
     * 특정 사용자 정보를 조회하는 메서드
     * 
     * @param userId 사용자 ID
     * @return 사용자 정보 (존재하지 않으면 null)
     */
    public UserDto getUser(String userId) {
        return users.get(userId);
    }

    /**
     * 사용자를 로그아웃 처리하는 메서드
     * 
     * @param userId 사용자 ID
     */
    public void logoutUser(String userId) {
        UserDto user = users.get(userId);
        if (user != null) {
            user.setStatus("OFFLINE");
            user.updateLastActiveTime();
            
            log.info("사용자 로그아웃 처리: {} ({})", user.getUserName(), userId);
        }
    }

    /**
     * 비활성 사용자들을 정리하는 메서드
     * 지정된 시간 이상 비활성 상태인 사용자들을 시스템에서 제거합니다.
     * 
     * @param inactiveMinutes 비활성 기준 시간 (분)
     * @return 정리된 사용자 수
     */
    public int cleanupInactiveUsers(int inactiveMinutes) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(inactiveMinutes);
        List<String> inactiveUserIds = new ArrayList<>();
        
        for (Map.Entry<String, UserDto> entry : users.entrySet()) {
            UserDto user = entry.getValue();
            if (user.getLastActiveTime().isBefore(cutoffTime)) {
                inactiveUserIds.add(entry.getKey());
            }
        }
        
        // 비활성 사용자들의 구독 정리
        for (String userId : inactiveUserIds) {
            Set<String> userChannelSet = userChannels.get(userId);
            if (userChannelSet != null) {
                for (String channel : userChannelSet) {
                    unsubscribeUserFromChannel(userId, channel);
                }
            }
            
            users.remove(userId);
            userChannels.remove(userId);
        }
        
        log.info("비활성 사용자 정리 완료 - {}명 제거 (기준: {}분 이상 비활성)", 
                 inactiveUserIds.size(), inactiveMinutes);
        
        return inactiveUserIds.size();
    }

    /**
     * 시스템 전체 통계 정보를 조회하는 메서드
     * 
     * @return 시스템 통계 정보
     */
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalUsers", users.size());
        stats.put("totalChannels", channelSubscribers.size());
        stats.put("totalSubscriptions", userChannels.values().stream()
                .mapToInt(Set::size)
                .sum());
        
        long onlineUsers = users.values().stream()
                .filter(user -> "ONLINE".equals(user.getStatus()))
                .count();
        stats.put("onlineUsers", onlineUsers);
        
        stats.put("channelSubscriberCounts", getChannelSubscriberCounts());
        
        log.info("시스템 통계 조회 - 사용자: {}명, 채널: {}개, 구독: {}건", 
                 users.size(), channelSubscribers.size(), 
                 userChannels.values().stream().mapToInt(Set::size).sum());
        
        return stats;
    }
} 