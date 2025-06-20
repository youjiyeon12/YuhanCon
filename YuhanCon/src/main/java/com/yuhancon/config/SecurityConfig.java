package com.yuhancon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.yuhancon.security.MemberDetailService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final MemberDetailService memberDetailService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { 
        http //http 객체를 통해 CSRF,인증,인가,로그인,로그아웃 등을 설정 
            .csrf(csrf -> csrf.disable()) //CRSF를 꺼둠 (개발 및 테스트용)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/main", "/signup", "/login", "/css/**", "/js/**").permitAll() //누구나 접근 가능
                .requestMatchers("/admin/**").hasRole("ADMIN") //ADMIN 권한이 있어야 접근 가능 
                .anyRequest().authenticated() //로그인된 사용자만 접근 가능
                //.anyRequest().permitAll()//모두 접근 가능 
            )
            .formLogin(form -> form
                .loginPage("/login") //로그인 페이지
                .defaultSuccessUrl("/main", true) //로그인 성공시 메인으로 리다이렉트 
                .usernameParameter("email") // spring security는 username이 기본 값이라 username = email로 인식시킴 
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login") //로그아웃하면 로그인 페이지로 
                .invalidateHttpSession(true) //로그아웃 시 세션 완전 초기화
            );

        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() { // 비밀번호 암호화/복호화에 사용
        return new BCryptPasswordEncoder();
    }
}
