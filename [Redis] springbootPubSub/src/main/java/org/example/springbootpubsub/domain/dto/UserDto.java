package org.example.springbootpubsub.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 사용자 정보를 담는 데이터 전송 객체 (DTO)
 * 
 * 이 클래스는 Redis Pub/Sub 시스템을 사용하는 사용자들의 
 * 기본 정보와 구독 상태 정보를 전송하기 위해 사용됩니다.
 * 
 * 주요 용도:
 * - 사용자 등록/로그인 정보 전달
 * - 사용자별 구독 채널 목록 관리
 * - 웹 UI와 서버 간 사용자 데이터 교환
 * - API 응답 데이터 구조화
 * 
 * @author Redis Pub/Sub Team
 * @version 1.0
 * @since 2024
 */
@Data // Lombok: getter, setter, toString, equals, hashCode 자동 생성
@NoArgsConstructor // Lombok: 기본 생성자 자동 생성
@AllArgsConstructor // Lombok: 모든 필드를 매개변수로 받는 생성자 자동 생성
public class UserDto {

    /**
     * 사용자 고유 식별자
     * 
     * 시스템 내에서 사용자를 구분하는 유일한 값입니다.
     * 세션 기반 인증에서는 세션 ID나 임시 사용자 ID로 사용됩니다.
     * 
     * 예시: "user123", "session_abc123", "guest_001"
     */
    private String userId;

    /**
     * 사용자 표시 이름
     * 
     * UI에서 사용자에게 표시되는 친화적인 이름입니다.
     * 실제 이름이나 닉네임을 저장합니다.
     * 
     * 예시: "홍길동", "개발자김씨", "ChatUser01"
     */
    private String userName;

    /**
     * 사용자 이메일 (선택사항)
     * 
     * 사용자 식별이나 알림 발송 용도로 사용할 수 있습니다.
     * 간단한 세션 기반 시스템에서는 필수가 아닙니다.
     * 
     * 예시: "user@example.com", null
     */
    private String email;

    /**
     * 사용자 상태
     * 
     * 현재 사용자의 온라인/오프라인 상태를 나타냅니다.
     * 
     * 가능한 값:
     * - "ONLINE": 현재 활성 상태
     * - "OFFLINE": 비활성 상태
     * - "AWAY": 자리비움 상태
     */
    private String status = "ONLINE";

    /**
     * 사용자가 현재 구독 중인 채널 목록
     * 
     * 이 사용자가 구독하고 있는 모든 Redis 채널들의 집합입니다.
     * 중복을 방지하기 위해 Set을 사용합니다.
     * 
     * 예시: {"chat-room-1", "notifications", "alerts"}
     */
    private Set<String> subscribedChannels;

    /**
     * 사용자 세션 생성 시간
     * 
     * 사용자가 시스템에 처음 접속한 시간을 기록합니다.
     * 세션 관리나 통계 수집에 활용할 수 있습니다.
     */
    private LocalDateTime joinTime;

    /**
     * 마지막 활동 시간
     * 
     * 사용자의 마지막 활동(메시지 발송, 구독 변경 등) 시간을 기록합니다.
     * 비활성 사용자 정리나 상태 관리에 사용됩니다.
     */
    private LocalDateTime lastActiveTime;

    /**
     * 사용자 역할 (선택사항)
     * 
     * 시스템 내에서 사용자의 권한 수준을 나타냅니다.
     * 
     * 가능한 값:
     * - "USER": 일반 사용자
     * - "ADMIN": 관리자
     * - "MODERATOR": 중재자
     */
    private String role = "USER";

    /**
     * 편의 생성자 - 기본 사용자 정보만으로 객체 생성
     * 
     * @param userId 사용자 ID
     * @param userName 사용자 이름
     */
    public UserDto(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.joinTime = LocalDateTime.now();
        this.lastActiveTime = LocalDateTime.now();
    }

    /**
     * 마지막 활동 시간을 현재 시간으로 업데이트하는 메서드
     */
    public void updateLastActiveTime() {
        this.lastActiveTime = LocalDateTime.now();
    }

    /**
     * 사용자가 특정 채널을 구독하고 있는지 확인하는 메서드
     * 
     * @param channel 확인할 채널명
     * @return 구독 중이면 true, 아니면 false
     */
    public boolean isSubscribedTo(String channel) {
        return subscribedChannels != null && subscribedChannels.contains(channel);
    }

    /**
     * 구독 중인 채널 수를 반환하는 메서드
     * 
     * @return 구독 중인 채널의 개수
     */
    public int getSubscribedChannelCount() {
        return subscribedChannels != null ? subscribedChannels.size() : 0;
    }
} 