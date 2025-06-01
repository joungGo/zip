package org.example.springbootpubsub.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Redis Pub/Sub 메시지 전송을 위한 데이터 전송 객체(Data Transfer Object)
 * 
 * 이 클래스는 Redis 채널을 통해 전송되는 메시지의 구조를 정의합니다.
 * Serializable 인터페이스를 구현하여 Redis에서 직렬화/역직렬화가 가능합니다.
 * 
 * 사용 예시:
 * - 채팅 메시지 전송
 * - 알림 메시지 브로드캐스트
 * - 실시간 이벤트 전달
 * 
 * @author Redis Pub/Sub Team
 * @version 1.0
 * @since 2024
 */
@Data // Lombok: getter, setter, toString, equals, hashCode 자동 생성
@NoArgsConstructor // Lombok: 기본 생성자 자동 생성 (JSON 역직렬화 시 필요)
@AllArgsConstructor // Lombok: 모든 필드를 매개변수로 받는 생성자 자동 생성
public class MessageDto implements Serializable {

    /**
     * 직렬화 버전 관리를 위한 UID
     * 
     * 직렬화된 객체를 역직렬화할 때 클래스 버전 호환성을 확인하는 데 사용됩니다.
     * 클래스 구조가 변경되어도 이 값이 같으면 호환성을 유지합니다.
     */
    @Serial // Java 14+에서 직렬화 관련 필드임을 명시하는 어노테이션
    private static final long serialVersionUID = 1L;

    /**
     * 전송할 메시지의 실제 내용
     * 
     * 채팅 메시지, 알림 텍스트, 시스템 메시지 등 
     * 사용자에게 전달하고자 하는 핵심 정보를 담습니다.
     * 
     * 예시: "안녕하세요!", "새로운 알림이 있습니다.", "시스템 점검 안내"
     */
    private String message;

    /**
     * 메시지를 발송한 사용자의 식별자
     * 
     * 메시지의 출처를 명확히 하기 위해 사용됩니다.
     * 사용자 이름, 사용자 ID, 시스템명 등이 올 수 있습니다.
     * 
     * 예시: "홍길동", "user123", "SYSTEM", "admin"
     */
    private String sender;

    /**
     * 메시지가 전송될 대상 방/채널의 식별자
     * 
     * Redis의 채널명과 연동되어 특정 그룹이나 방으로 메시지를 전달할 때 사용됩니다.
     * 채팅방 ID, 알림 그룹 ID, 이벤트 채널 ID 등으로 활용 가능합니다.
     * 
     * 예시: "chat-room-1", "notification-group", "event-channel-A"
     */
    private String roomId;
}