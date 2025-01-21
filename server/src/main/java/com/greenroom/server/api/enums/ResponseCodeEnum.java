package com.greenroom.server.api.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public enum ResponseCodeEnum {

    //200~
    SUCCESS(HttpStatus.OK,"A000","요청에 대한 처리 성공",""),
    CREATED(HttpStatus.CREATED,"A001","요청 처리 성공 후 resource가 생성됨.",""),
    NO_CONTENT(HttpStatus.NO_CONTENT,"A002","요청 처리 성공 후 보낼 response가 없음",""),

    //400~
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND,"C000","요청과 관련한 resource를 찾을 수 없음.","적절한 요청값을 통해 다시 시도."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,"C001","요청과 관련한 user가 존재하지 않음.","적절한 user 정보를 통해 요청 다시 시도."),
    USER_ALREADY_EXIST(HttpStatus.CONFLICT, "C002","회원 가입된 user가 이미 존재함.",""),
    USER_ALREADY_EXIST_OTHER_OAUTH(HttpStatus.CONFLICT,"C003","같은 이름의 user가 oauth2에 이미 존재함",""),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "C004","access token이 만료됨.","토큰 갱신 또는 로그인 필요."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.CONFLICT,"C005","refresh token이 만료됨","재 로그인 필요"),
    TOKENS_NOT_FOUND(HttpStatus.UNAUTHORIZED,"C006","요청에 포함된 토큰을 찾을 수 없습니다.","access token을 header를 통해 전달."),
    REFRESH_TOKEN_NOT_MATCHED(HttpStatus.CONFLICT,"C007","전달된 refresh token이 서버가 갖고 있는 refresh token과 일치하지 않음.","로그아웃 후 다시 로그인 필요."),
    ACCESS_TOKEN_INVALID(HttpStatus.UNAUTHORIZED,"C008","access token이 유효하지 않음.","access token 갱신 또는 다시 로그인 필요"),
    REFRESH_TOKEN_INVALID(HttpStatus.CONFLICT,"C009","refresh token이 유효하지 않음.","다시 로그인 필요."),
    NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED,"C010","인증되지 않은 user가 접근을 시도함.","access token을 가지고 다시 요청 필요."),
    NOT_AUTHORIZATION(HttpStatus.FORBIDDEN,"C011","인증된 user이나 해당 자원에 대한 접근 권한이 없음.","user에게 접근 제한 알림 필요."),
    INVALID_JWT_SIGN(HttpStatus.UNAUTHORIZED,"C012","부적절한 jwt 시그니처",""),
    INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST,"C013","올바르지 못한 image format이 전달됨.","적절한 image format으로 다시 전송"),
    OAUTH_USER_ALREADY_EXIST_CONNECT_AVAILABLE(HttpStatus.CONFLICT,"C014","같은 이름의 user가 oauth2에 이미 존재. 연동 가능함.","사용자에게 연동 여부 질의 : 동의->계정 연동 api 요청 & 미동의->oauth 로그인 제한"),
    PASSWORD_NOT_MATCHED(HttpStatus.CONFLICT, "C015","로그인 password가 일치하지 않음.","password 다시 전송"),
    VERIFIED_USER_ALREADY_EXISTS(HttpStatus.CONFLICT,"C016","이미 해당 email로 가입된 회원이 존재함.","해당 email 회원가입 제한"),
    EXCEED_NUMBER_OF_TRIAL_VERIFICATION(HttpStatus.CONFLICT,"C017","인증 시도 횟수를 초과함","마지막 요청으로부터 15분 후 다시 시도 가능"),
    EMAIL_VERIFICATION_CODE_EXPIRED(HttpStatus.CONFLICT,"C018","email 인증 코드가 만료됨.","email 인증 다시 요청 필요"),
    EMAIL_VERIFICATION_CODE_NOT_MATCHED(HttpStatus.CONFLICT,"C019","email 인증 코드가 일치하지 않음.",""),
    EMAIL_NOT_VERIFIED(HttpStatus.CONFLICT,"C020","인증되지 않은 email로 회원가입을 시도함","회원가입 제한 & email 인증 요구"),
    EMAIL_ADDRESS_UNAVAILABLE(HttpStatus.CONFLICT,"C021","존재하지 않는 email 주소이거나 해당 email 주소의 메일박스 등의 문제로 email을 보낼 수 없음.","email을 보낼 수 있는 유효한 email 필요."),
    ALREADY_VERIFIED_EMAIL(HttpStatus.CONFLICT,"C022","이미 인증된 email로 이메일 인증을 시도함.",""),
    INVALID_REQUEST_ARGUMENT(HttpStatus.BAD_REQUEST,"C023","요청시 request body로 전달된 argument가 조건을 만족시키지 않음. ","명세된 request 조건에 맞는 요청 다시 전송 필요"),
    INVALID_EMAIL_VERIFICATION_TOKEN(HttpStatus.CONFLICT,"C024","부적절한 이메일 인증 토큰이 전송됨",""),
    //500~
    UNKNOWN_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"D000","정의되지 않은 알 수 없는 서버 에러가 발생함.","서버에 문의"),
    FAIL_DATA_PARSING(HttpStatus.INTERNAL_SERVER_ERROR,"D001","알 수 없는 서버 문제로 데이터 parsing에 실패함.",""),
    FAIL_TO_SEND_EMAIL(HttpStatus.INTERNAL_SERVER_ERROR,"D002","알 수 없는 서버 문제로 메일 전송에 실패함.","다시 시도"),
    INVALID_REQUEST_PARAMETER(HttpStatus.INTERNAL_SERVER_ERROR ,"D003","서버 내부 문제로 부적절한 request parameter가 전달됨.","서버에 문의");


    private final HttpStatus status;
    private final String code;
    private final String message;
    private final String postProcess;

    ResponseCodeEnum(HttpStatus status, String code, String message, String postProcess){
        this.status = status;
        this.code = code;
        this.message = message;
        this.postProcess = postProcess;

    }
}