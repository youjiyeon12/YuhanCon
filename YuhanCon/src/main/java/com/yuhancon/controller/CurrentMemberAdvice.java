package com.yuhancon.controller;

import java.security.Principal;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.yuhancon.domain.Member;
import com.yuhancon.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

// 모든 화면에 로그인한 회원(me, 없으면 null)을 넣는다. 메뉴·버튼을 권한에 맞게 보여 주는 데 쓴다.
@ControllerAdvice
@RequiredArgsConstructor
public class CurrentMemberAdvice {

    private final MemberRepository memberRepository;

    @ModelAttribute("me")
    public Member me(Principal principal) {
        return principal == null ? null : memberRepository.findById(principal.getName()).orElse(null);
    }
}
