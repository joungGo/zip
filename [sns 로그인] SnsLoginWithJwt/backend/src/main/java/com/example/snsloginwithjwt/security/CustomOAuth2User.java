package com.example.snsloginwithjwt.security;

import com.example.snsloginwithjwt.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * OAuth2 사용자 정보를 커스터마이징하는 클래스
 * Spring Security의 OAuth2User 인터페이스를 구현하여
 * 사용자 권한과 속성을 관리
 */
@Getter
public class CustomOAuth2User implements OAuth2User {

    private final User user;  // 데이터베이스에 저장된 사용자 정보
    private final Map<String, Object> attributes;  // OAuth2 제공자로부터 받은 사용자 속성

    /**
     * 생성자
     * @param user 데이터베이스에 저장된 사용자 정보
     */
    public CustomOAuth2User(User user) {
        this.user = user;
        this.attributes = Collections.emptyMap();
    }

    /**
     * OAuth2 사용자 속성 반환
     * @return 사용자 속성 맵
     */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * 사용자 권한 반환
     * @return 사용자 권한 목록
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    /**
     * 사용자 이름 반환 (이메일 사용)
     * @return 사용자 이메일
     */
    @Override
    public String getName() {
        return user.getEmail();
    }
} 