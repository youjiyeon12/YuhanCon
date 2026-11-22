package com.yuhancon.web;

import com.yuhancon.domain.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SignupForm {

    @NotBlank(message = "아이디를 입력하세요.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,12}$", message = "아이디는 영문과 숫자를 섞어 6~12자로 입력하세요.")
    private String loginId;

    @NotBlank(message = "이름을 입력하세요.")
    @Pattern(regexp = "^[가-힣a-zA-Z]{2,30}$", message = "이름은 공백 없이 한글 또는 영문 2자 이상 입력하세요.")
    private String name;

    @NotBlank(message = "이메일을 입력하세요.")
    @Email(message = "이메일 형식을 확인하세요.")
    @Size(max = 100, message = "이메일은 100자 이하로 입력하세요.")
    private String email;

    @NotBlank(message = "비밀번호를 입력하세요.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,64}$", message = "비밀번호는 영문과 숫자를 포함해 공백 없이 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호를 한 번 더 입력하세요.")
    private String passwordConfirm;

    @NotBlank(message = "전화번호를 입력하세요.")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "전화번호는 \"-\" 없이 숫자만 10~11자리 입력하세요.")
    private String phone;

    @NotNull(message = "회원 유형을 고르세요.")
    private Role role = Role.USER;
}
