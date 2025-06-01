package com.example.snsloginwithjwt.config;

import com.example.snsloginwithjwt.security.CustomOAuth2User;
import com.example.snsloginwithjwt.service.OAuth2Service;
import com.example.snsloginwithjwt.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Spring Security 설정 클래스
 * OAuth2 로그인과 JWT 인증을 구성
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    /**
     * 보안 필터 체인 설정
     * CSRF 비활성화, 세션 관리, 인증 요청 설정, OAuth2 로그인 설정
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()  // CSRF 보호 비활성화
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // 세션 사용 안함
            .and()
            .authorizeHttpRequests()
                .requestMatchers("/", "/login/**", "/oauth2/**").permitAll()  // 인증 없이 접근 가능한 경로
                .anyRequest().authenticated()  // 나머지 모든 요청은 인증 필요
            .and()
            .oauth2Login()  // OAuth2 로그인 설정
                .userInfoEndpoint()
                    .userService(oAuth2UserService())  // OAuth2 사용자 서비스 설정
                .and()
                .successHandler(authenticationSuccessHandler());  // 로그인 성공 핸들러 설정

        return http.build();
    }

    /**
     * OAuth2 사용자 서비스 빈 생성
     * @return OAuth2UserService 구현체
     */
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
        return new OAuth2Service();
    }

    /**
     * 인증 성공 핸들러 빈 생성
     * 로그인 성공 시 JWT 토큰을 생성하고 리다이렉트
     * @return AuthenticationSuccessHandler 구현체
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
            String token = jwtUtil.generateToken(oAuth2User);
            response.sendRedirect("/?token=" + token);
        };
    }
} 