package com.yuhancon.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.yuhancon.repository.MemberRepository;
import com.yuhancon.service.MemberService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberDetailService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return memberRepository.findById(MemberService.normalizeId(username))
            .map(CustomUserDetails::new)
            .orElseThrow(() -> new UsernameNotFoundException("아이디가 없습니다."));
    }
}
