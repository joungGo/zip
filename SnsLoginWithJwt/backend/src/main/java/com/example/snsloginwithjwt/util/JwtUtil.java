package com.example.snsloginwithjwt.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 토큰 생성 및 검증을 위한 유틸리티 클래스
 * application.yml에서 설정된 secret과 expiration을 사용
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;  // JWT 서명에 사용되는 비밀키

    @Value("${jwt.expiration}")
    private Long expiration;  // 토큰 만료 시간 (밀리초)

    /**
     * JWT 서명에 사용할 키 생성
     * @return HMAC-SHA 알고리즘을 사용한 키
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 사용자 정보로 JWT 토큰 생성
     * @param userDetails 사용자 정보
     * @return 생성된 JWT 토큰
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    /**
     * 클레임과 주제로 JWT 토큰 생성
     * @param claims 토큰에 포함될 클레임
     * @param subject 토큰 주제 (일반적으로 사용자 이메일)
     * @return 생성된 JWT 토큰
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰 유효성 검증
     * @param token 검증할 JWT 토큰
     * @param userDetails 사용자 정보
     * @return 토큰이 유효하면 true, 아니면 false
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * 토큰에서 사용자 이름(이메일) 추출
     * @param token JWT 토큰
     * @return 사용자 이름(이메일)
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 토큰에서 만료 시간 추출
     * @param token JWT 토큰
     * @return 토큰 만료 시간
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 토큰에서 특정 클레임 추출
     * @param token JWT 토큰
     * @param claimsResolver 클레임 추출 함수
     * @return 추출된 클레임 값
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 토큰에서 모든 클레임 추출
     * @param token JWT 토큰
     * @return 모든 클레임
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 토큰 만료 여부 확인
     * @param token JWT 토큰
     * @return 토큰이 만료되었으면 true, 아니면 false
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
} 