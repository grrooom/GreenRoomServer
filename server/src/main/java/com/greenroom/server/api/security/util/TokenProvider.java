package com.greenroom.server.api.security.util;

import com.greenroom.server.api.security.dto.TokenDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TokenProvider implements InitializingBean {

    private final String SECRET_KEY;
    private static final String AUTHORITIES_KEY = "auth";
    private final long accessTokenValidityInMilliSeconds;
    private final long refreshTokenValidityInMilliSeconds;
    private final long emailVerificationTokenValidityInMiliSeconds;
    private Key key;

    public TokenProvider(
            @Value("${jwt.secret-key}") String secretKey,
            @Value("${jwt.token-validity-in-seconds}") long tokenValidityInSeconds){
        this.SECRET_KEY = secretKey;
        this.accessTokenValidityInMilliSeconds = tokenValidityInSeconds*1000;   //60분
        this.refreshTokenValidityInMilliSeconds = accessTokenValidityInMilliSeconds*336;  //14일
        this.emailVerificationTokenValidityInMiliSeconds = 15 * 60*1000; //15분
    }

    // BeanFactory에 의해 모든 property 가 설정되고 난 뒤 실행되는 메소드
    //secret key 주입
    @Override
    public void afterPropertiesSet() {
        byte[] keyBytes = Decoders.BASE64.decode(this.SECRET_KEY);
        key = Keys.hmacShaKeyFor(keyBytes);
    }

    public TokenDto createAllToken(Authentication authentication) { // 토큰 생성

        //비공개키 암호화 방식 사용
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        return TokenDto.builder()
                .accessToken(
                        Jwts.builder()
                                .setSubject(authentication.getName())
                                .claim(AUTHORITIES_KEY, authorities)
                                .signWith(key, SignatureAlgorithm.HS512)
                                .setExpiration(createTokenValidity(this.accessTokenValidityInMilliSeconds))
                                .compact()
                )
                .refreshToken(
                        Jwts.builder()
                                .setSubject(authentication.getName())
                                .signWith(key, SignatureAlgorithm.HS512)
                                .setExpiration(createTokenValidity(this.refreshTokenValidityInMilliSeconds))
                                .compact()
                )
                .email(authentication.getName())
                .build();
    }

    private Date createTokenValidity(long milliseconds){
        return new Date((new Date()).getTime() + milliseconds);
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();


        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        User user = new User(claims.getSubject(),"password",authorities);

        return new UsernamePasswordAuthenticationToken(user,token, authorities);
    }

    public String getPrincipalEmail(String token){
        Claims claims = Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // exception 발생 안하면 true 반환
    // exception 반환시 JwtFilterExceptionHandler 에서 처리
    public boolean validateToken(String token) throws ExpiredJwtException, io.jsonwebtoken.security.SecurityException, MalformedJwtException ,UnsupportedJwtException,IllegalArgumentException{
        Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
        return true;
    }

    public String createVerificationToken(String email){

        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("guest"));

        return Jwts.builder()
                .setSubject(email)
                .claim(AUTHORITIES_KEY, authorities)
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(createTokenValidity(this.emailVerificationTokenValidityInMiliSeconds))
                .compact();
    }

}

