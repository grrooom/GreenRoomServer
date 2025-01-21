package com.greenroom.server.api.security.handler;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenroom.server.api.enums.ResponseCodeEnum;
import com.greenroom.server.api.security.exception.JWTCustomException;
import com.greenroom.server.api.utils.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilterExceptionHandler extends OncePerRequestFilter {

    @Autowired
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        }
        catch (SecurityException | MalformedJwtException e) {
            log.info("invalidate jwt signature : " +e.getMessage());
            throwException(ResponseCodeEnum.ACCESS_TOKEN_INVALID,response);
        } catch (ExpiredJwtException e) {
            log.info("token이 만료되었습니다 : " +e.getMessage());
            throwException(ResponseCodeEnum.ACCESS_TOKEN_EXPIRED,response);
        } catch (UnsupportedJwtException e) {
            log.info("unsupported jwt token : "+e.getMessage());
            throwException(ResponseCodeEnum.ACCESS_TOKEN_INVALID,response);
        } catch (IllegalArgumentException e) {
            log.info("invalid jwt token : "+e.getMessage());
            throwException(ResponseCodeEnum.ACCESS_TOKEN_INVALID,response);
        }
        catch (JWTCustomException e){
            log.info("jwt 토큰을 찾을 수 없습니다.");
            throwException(e.getResponseCodeEnum(),response);
        }
    }
    public void throwException(ResponseCodeEnum e, HttpServletResponse response) throws IOException {

        ApiResponse apiResponse = ApiResponse.failed(e);
        response.setStatus(e.getStatus().value());
        response.setContentType(APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

}



