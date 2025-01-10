package com.greenroom.server.api.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public enum ResponseCodeEnum {

    //200~
    SUCCESS(HttpStatus.OK,"A001","요청을 성공"),
    CREATED(HttpStatus.CREATED,"A001","요청을 성공 후 resource가 생성됨."),
    NO_CONTENT(HttpStatus.NO_CONTENT,"A002","요청 성공 후 보낼 response가 없음"),

    //400~
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND,"C00","해당 resource를 찾을 수 없음."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,"C001","user가 존재하지 않음."),
    USER_ALREADY_EXIST(HttpStatus.CONFLICT, "C002","user가 이미 존재합니다."),
    USER_ALREADY_EXIST_OTHER_OAUTH(HttpStatus.CONFLICT,"C003","같은 이름의 user가 oauth2에 이미 존재합니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "C004","access token이 만료됨."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.CONFLICT,"C005","refresh token이 만료됐습니다."),
    TOKENS_NOT_FOUND(HttpStatus.UNAUTHORIZED,"C006","토큰을 찾을 수 없습니다."),
    REFRESH_TOKEN_NOT_MATCHED(HttpStatus.CONFLICT,"C007","저장된 refresh token과 일치하지 않음. 다시 로그인 요구."),
    ACCESS_TOKEN_INVALID(HttpStatus.UNAUTHORIZED,"C008","access token이 유효하지 않음."),
    REFRESH_TOKEN_INVALID(HttpStatus.CONFLICT,"C009","refresh token이 유효하지 않음."),
    NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED,"C010","인증되지 않은 user입니다."),
    NOT_AUTHORIZATION(HttpStatus.FORBIDDEN,"C011","해당 자원에 대한 접근 권한이 없음."),
    INVALIDATE_JWT_SIGN(HttpStatus.UNAUTHORIZED,"C012","부적절한 jwt 시그니처"),
    INVALID_JWT_TOKEN(HttpStatus.UNAUTHORIZED,"C013","부적절한 jwt 토큰입니다"),
    INVALID_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST,"C014","부적절한 request parameter입니다."),
    INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST,"C015","올바르지 못한 image format 입니다."),
    INVALID_DATA_FORMAT(HttpStatus.BAD_REQUEST,"C016","올바르지 못한 data format입니다."),
    OAUTH_USER_ALREADY_EXIST_CONNECT_AVAILABLE(HttpStatus.CONFLICT,"C017","같은 이름의 user가 oauth2에 이미 존재. 연동 가능함."),
    PASSWORD_NOT_MATCHED(HttpStatus.CONFLICT, "C017","password가 일치하지 않음."),
    VERIFIED_USER_ALREADY_EXISTS(HttpStatus.CONFLICT,"C018","이미 해당 email로 가입된 회원이 존재함."),
    EXCEED_NUMBER_OF_TRIAL_VERIFICATION(HttpStatus.CONFLICT,"C019","인증 횟수를 초과 - 조금 있다 시도"),
    AUTHENTICATION_NEVER_TRIED(HttpStatus.BAD_REQUEST,"C020","인증을 시도한 적 없는 email에 대해 토큰 검증을 시도함."),
    EMAIL_VERIFICATION_CODE_EXPIRED(HttpStatus.CONFLICT,"C021","email 인증 코드가 만료됨"),
    EMAIL_VERIFICATION_CODE_NOT_MATCHED(HttpStatus.CONFLICT,"C022","email 인증 코드가 일치하지 않음."),
    EMAIL_NOT_VERIFIED(HttpStatus.CONFLICT,"C023","인증되지 않은 email로 회원가입을 시도함"),
    INVALID_EMAIL_CONTENT(HttpStatus.CONFLICT,"C024","email 주소,본문 등이 적절하지 않음."),
    ALREADY_VERIFIED_EMAIL(HttpStatus.CONFLICT,"C025","이미 인증된 email로 이메일 인증을 시도함."),
    REFRESH_TOKEN_NOT_EXISTS(HttpStatus.NOT_FOUND,"C026","사용자의 refresh token이 존재하지 않음."),
    INVALID_REQUEST_ARGUMENT(HttpStatus.BAD_REQUEST,"C027","요청시 전달된 argument가 부적절하거나 존재하지 않음"),

    //500~
    UNKNOWN_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"D000","알 수 없는 server error"),
    FAIL_DATA_PARSING(HttpStatus.INTERNAL_SERVER_ERROR,"D001","데이터 parsing에 실패했습니다."),
    FAIL_TO_SEND_EMAIL(HttpStatus.INTERNAL_SERVER_ERROR,"D002","메일 전송에 실패함.");


    private final HttpStatus status;
    private final String code;
    private final String message;

    ResponseCodeEnum(HttpStatus status, String code, String message){
        this.status = status;
        this.code = code;
        this.message = message;

    }
}