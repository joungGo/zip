package com.example.snsloginwithjwt.service;

import com.example.snsloginwithjwt.entity.User;
import com.example.snsloginwithjwt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * OAuth2 인증을 처리하는 서비스 클래스
 * Google과 Kakao 로그인을 지원하며, 사용자 정보를 처리하고 저장
 */
@Service
@RequiredArgsConstructor
public class OAuth2Service extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     * OAuth2 사용자 정보를 로드하고 처리
     * @param userRequest OAuth2 사용자 요청 정보
     * @return 처리된 OAuth2 사용자 정보
     * @throws OAuth2AuthenticationException 인증 처리 중 발생한 예외
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = getEmail(provider, attributes);
        String name = getName(provider, attributes);
        String providerId = getProviderId(provider, attributes);

        User user = userRepository.findByProviderAndProviderId(User.Provider.valueOf(provider.toUpperCase()), providerId)
                .orElseGet(() -> createUser(email, name, provider, providerId));

        return new CustomOAuth2User(user);
    }

    /**
     * SNS 제공자별로 이메일 정보 추출
     * @param provider SNS 제공자 (google, kakao)
     * @param attributes OAuth2 사용자 속성
     * @return 사용자 이메일
     */
    private String getEmail(String provider, Map<String, Object> attributes) {
        if (provider.equals("google")) {
            return (String) attributes.get("email");
        } else if (provider.equals("kakao")) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            return (String) kakaoAccount.get("email");
        }
        return null;
    }

    /**
     * SNS 제공자별로 이름 정보 추출
     * @param provider SNS 제공자 (google, kakao)
     * @param attributes OAuth2 사용자 속성
     * @return 사용자 이름
     */
    private String getName(String provider, Map<String, Object> attributes) {
        if (provider.equals("google")) {
            return (String) attributes.get("name");
        } else if (provider.equals("kakao")) {
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            return (String) properties.get("nickname");
        }
        return null;
    }

    /**
     * SNS 제공자별로 사용자 ID 추출
     * @param provider SNS 제공자 (google, kakao)
     * @param attributes OAuth2 사용자 속성
     * @return SNS 제공자에서 발급한 사용자 ID
     */
    private String getProviderId(String provider, Map<String, Object> attributes) {
        if (provider.equals("google")) {
            return (String) attributes.get("sub");
        } else if (provider.equals("kakao")) {
            return String.valueOf(attributes.get("id"));
        }
        return null;
    }

    /**
     * 새로운 사용자 생성
     * @param email 사용자 이메일
     * @param name 사용자 이름
     * @param provider SNS 제공자
     * @param providerId SNS 제공자에서 발급한 사용자 ID
     * @return 생성된 사용자 객체
     */
    private User createUser(String email, String name, String provider, String providerId) {
        User user = User.builder()
                .email(email)
                .name(name)
                .provider(User.Provider.valueOf(provider.toUpperCase()))
                .providerId(providerId)
                .role(User.Role.USER)
                .build();
        return userRepository.save(user);
    }
} 