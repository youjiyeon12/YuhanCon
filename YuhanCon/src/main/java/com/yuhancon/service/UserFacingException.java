package com.yuhancon.service;

// 사용자에게 그대로 보여 줄 수 있는 안내(입력 오류, 규칙 위반). 컨트롤러가 잡아서 화면에 띄운다.
public class UserFacingException extends RuntimeException {
    public UserFacingException(String message) { super(message); }
}
