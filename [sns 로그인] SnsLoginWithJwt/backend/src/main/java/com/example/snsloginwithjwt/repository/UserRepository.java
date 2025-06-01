package com.example.snsloginwithjwt.repository;

import com.example.snsloginwithjwt.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 사용자 데이터베이스 작업을 위한 JPA 리포지토리 인터페이스
 * 기본적인 CRUD 작업과 커스텀 쿼리 메서드를 제공
 */
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * 이메일로 사용자 조회
     * @param email 조회할 사용자의 이메일
     * @return Optional로 감싸진 사용자 객체
     */
    Optional<User> findByEmail(String email);

    /**
     * SNS 제공자와 제공자 ID로 사용자 조회
     * @param provider SNS 제공자 (GOOGLE, KAKAO)
     * @param providerId SNS 제공자에서 발급한 사용자 ID
     * @return Optional로 감싸진 사용자 객체
     */
    Optional<User> findByProviderAndProviderId(User.Provider provider, String providerId);
} 