package com.greenroom.server.api.security.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Getter
public class OAuth2Attribute {

    private Map<String,Object> attributes;
    private String nameAttributeKey;
    private String provider;
    private String name;
    private String email;

    @Builder
    public OAuth2Attribute(Map<String, Object> attributes, String nameAttributeKey, String name, String email, String provider) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.provider = provider;
    }

    public static OAuth2Attribute of(String userNameAttributeName,String registrationId, Map<String, Object> attributes) {


        if(registrationId.equals("kakao")){
            return ofKakao(userNameAttributeName, attributes, registrationId);
        }
        else if(registrationId.equals("google")) {
            return ofGoogle(userNameAttributeName, attributes,registrationId);
        }
        return null;
    }


    private static OAuth2Attribute ofKakao(String nameAttributeKey, Map<String,Object> attributes, String provider){
         Map<String,Object> kakaoAttributes = (Map<String, Object>) attributes.get("kakao_account");
        String email = (String) Objects.requireNonNull(kakaoAttributes).get("email");
        String name = String.valueOf(Optional.ofNullable(kakaoAttributes.get("name")));

        return OAuth2Attribute.builder()
                .name(name)
                .email(email)
                .attributes(attributes)
                .nameAttributeKey(nameAttributeKey)
                .provider(provider).build();


    }

    private static OAuth2Attribute ofGoogle(String nameAttributeKey, Map<String, Object> attributes,String provider) {
        return OAuth2Attribute.builder()
                .name(String.valueOf(attributes.get("name")))
                .email(String.valueOf(attributes.get("email")))
                .attributes(attributes)
                .nameAttributeKey(nameAttributeKey)
                .provider(provider).build();
    }

}
