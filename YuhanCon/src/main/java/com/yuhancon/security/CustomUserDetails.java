package com.yuhancon.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.yuhancon.domain.Member;

// 로그인 정보에는 아이디·비밀번호 해시·권한만 담는다. 회원 정보는 요청마다 DB에서 다시 읽는다(CurrentMemberAdvice).
public class CustomUserDetails implements UserDetails {

    private final String loginId;
    private final String password;
    private final String role;
    private final boolean locked;

    public CustomUserDetails(Member member) {
        this.loginId = member.getLoginId();
        this.password = member.getPassword();
        this.role = member.getRole().name();
        this.locked = member.isLoginLocked();
    }

    // 잠긴 계정은 비밀번호를 확인하기 전에 Spring Security가 LockedException으로 막는다
    @Override public boolean isAccountNonLocked() { return !locked; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override public String getUsername() { return loginId; }
    @Override public String getPassword() { return password; }
}
