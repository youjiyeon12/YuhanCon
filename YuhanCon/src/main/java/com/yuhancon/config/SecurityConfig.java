package com.yuhancon.config;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import com.yuhancon.security.MemberDetailService;
import com.yuhancon.service.MemberService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final MemberDetailService memberDetailService;

    // 로그인 저장(remember-me) 쿠키 서명 키. 설정이 없으면 실행할 때마다 새로 만든다(재시작하면 저장된 로그인이 풀린다).
    @Value("${yuhancon.remember-me-key:}")
    private String rememberMeKey;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, MemberService memberService) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler toTarget = new SavedRequestAwareAuthenticationSuccessHandler();
        toTarget.setDefaultTargetUrl("/main"); // 로그인이 필요해 들어온 경우는 보던 화면으로
        http
            // CSRF는 기본값(켜짐). th:action 폼에는 토큰이 자동으로 들어간다.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/main", "/signup", "/signup/check-id", "/login", "/css/**", "/js/**", "/fonts/**",
                                 "/images/**", "/uploaded-images/**", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/concertList", "/concertDetail/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/concertWrite", "/concert/save", "/concertEdit/**", "/concertDelete/**").hasAnyRole("PERFORMER", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("loginId")
                .successHandler((req, res, auth) -> {
                    memberService.recordLoginSuccess(auth.getName()); // 성공하면 실패 횟수를 지운다
                    toTarget.onAuthenticationSuccess(req, res, auth);
                })
                .failureHandler((req, res, ex) -> {
                    // 다시 입력하지 않도록 아이디를 기억해 로그인 화면에 채운다(비밀번호는 남기지 않는다)
                    String id = req.getParameter("loginId");
                    req.getSession().setAttribute(LAST_LOGIN_ID, id == null ? "" : id.strip());
                    // 5번 연속 틀리면 10분 잠근다. 잠긴 동안에는 비밀번호를 확인하지 않고 LockedException이 온다.
                    boolean locked = ex instanceof LockedException
                        || (ex instanceof BadCredentialsException && memberService.recordLoginFailure(id));
                    res.sendRedirect(req.getContextPath() + (locked ? "/login?locked" : "/login?error"));
                })
                .permitAll()
            )
            .rememberMe(rm -> rm
                .key(rememberMeKey.isBlank() ? UUID.randomUUID().toString() : rememberMeKey)
                .rememberMeParameter("remember")
                .tokenValiditySeconds(14 * 24 * 60 * 60)
                .userDetailsService(memberDetailService)
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login?required")))
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
            )
            // 스크립트·스타일은 우리 서버 파일만, 다른 사이트 안에 넣어 보여 주는 것(클릭재킹)은 막는다
            .headers(h -> h
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; font-src 'self';"
                    + " object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'"))
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            );
        return http.build();
    }

    public static final String LAST_LOGIN_ID = "LAST_LOGIN_ID";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
