package com.yuhancon.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MemberEditForm {

    @NotBlank(message = "이름을 입력하세요.")
    @Pattern(regexp = "^[가-힣a-zA-Z]{2,30}$", message = "이름은 공백 없이 한글 또는 영문 2자 이상 입력하세요.")
    private String name;

    @NotBlank(message = "전화번호를 입력하세요.")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "전화번호는 \"-\" 없이 숫자만 10~11자리 입력하세요.")
    private String phone;

    // 비워 두면 지금 비밀번호를 그대로 쓴다
    @Pattern(regexp = "^$|^(?=.*[A-Za-z])(?=.*\\d)\\S{8,64}$", message = "새 비밀번호는 영문과 숫자를 포함해 공백 없이 8자 이상이어야 합니다.")
    private String newPassword;

    @NotBlank(message = "현재 비밀번호를 입력하세요.")
    private String currentPassword;
}
