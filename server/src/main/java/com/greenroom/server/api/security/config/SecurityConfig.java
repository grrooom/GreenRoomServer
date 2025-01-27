package com.greenroom.server.api.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenroom.server.api.security.handler.*;
import com.greenroom.server.api.security.util.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JWTAccessDeniedHandler jwtAccessDeniedHandler;
    private final TokenProvider tokenProvider;
    private static final String[] ANONYMOUS_MATCHERS = {
            "/api/auth/**","/error","/login", "/docs/**","/admin/**","/.well-known/**","/"
    };

    @Order(1)
    @Bean
    SecurityFilterChain filterChain1(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        return http
                .securityMatcher(new AntPathRequestMatcher("/api/**"))
                .csrf(AbstractHttpConfigurer::disable)
                .headers((headerConfig) ->
                        headerConfig.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                )
                .authorizeHttpRequests((authorizeRequests) ->
                        authorizeRequests
                                .requestMatchers(
                                        Stream.of(ANONYMOUS_MATCHERS)
                                                .map(uri->new MvcRequestMatcher(introspector,uri))
                                                .toArray(MvcRequestMatcher[]::new)
                                ).permitAll()
                                .anyRequest().authenticated()
                )
                .sessionManagement((sessionManagement)->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(new JWTFilter(tokenProvider),UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtFilterExceptionHandler(new ObjectMapper()), JWTFilter.class)
                .exceptionHandling((exceptionHandling) ->
                        exceptionHandling
                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .build();
    }

//    @Order(1)
//    @Bean
//    SecurityFilterChain filterChain2(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
//
//        //Requests starting with /api/ are excluded from these security rules, possibly because they are handled differently
//        return http
//                .securityMatcher(new NegatedRequestMatcher(new AntPathRequestMatcher("/api/**")))
//                .authorizeHttpRequests((request)->request.requestMatchers(
//                        Stream.of(ANONYMOUS_MATCHERS)
//                                .map(uri->new MvcRequestMatcher(introspector,uri))
//                                .toArray(MvcRequestMatcher[]::new)
//                ).permitAll()
//                        .requestMatchers("/docs/**","/admin/data").hasAuthority("ADMIN").
//                        anyRequest().authenticated())
//                .formLogin(Customizer.withDefaults())
//
//                .build();
//    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers(new AntPathRequestMatcher( "/favicon.ico"))
                .requestMatchers(new AntPathRequestMatcher( "/css/**"))
                .requestMatchers(new AntPathRequestMatcher( "/js/**"))
                .requestMatchers(new AntPathRequestMatcher( "/image/**"));
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
