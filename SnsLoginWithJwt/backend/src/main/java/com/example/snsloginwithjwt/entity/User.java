package com.example.snsloginwithjwt.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보를 저장하는 엔티티 클래스
 * JPA를 사용하여 데이터베이스와 매핑됨
 */
@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 사용자 고유 식별자

    @Column(nullable = false, unique = true)
    private String email;  // 사용자 이메일 (중복 불가)

    private String name;  // 사용자 이름

    @Enumerated(EnumType.STRING)
    private Provider provider;  // SNS 제공자 (GOOGLE, KAKAO)

    private String providerId;  // SNS 제공자에서 발급한 사용자 ID

    @Enumerated(EnumType.STRING)
    private Role role;  // 사용자 권한 (USER, ADMIN)

    /**
     * SNS 제공자 열거형
     */
    public enum Provider {
        GOOGLE, KAKAO
    }

    /**
     * 사용자 권한 열거형
     */
    public enum Role {
        USER, ADMIN
    }
} 