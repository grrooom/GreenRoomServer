package com.greenroom.server.api.security.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Getter
public class OAuthUserDto {

    String provider;
    String email;
    String name;


    @Builder
    public OAuthUserDto(String provider, String email, String name) {
        this.provider = provider;
        this.email = email;
        this.name = name;
    }

    public static OAuthUserDto of (String registrationId, OAuth2User oAuth2User){

        String email=null;
        Optional<String> name = Optional.empty();

        if(registrationId.equals("kakao")){
            Map<String,String> kakaoAttributes =  oAuth2User.getAttribute("kakao_account");
            email =Objects.requireNonNull(kakaoAttributes).get("email");
            name = Optional.ofNullable(kakaoAttributes.get("name"));
        }
        else if(registrationId.equals("google")){

        }

        return OAuthUserDto.builder()
                .email(email)
                .name(name.orElse(null))
                .provider(registrationId)
                .build();
    }
}
