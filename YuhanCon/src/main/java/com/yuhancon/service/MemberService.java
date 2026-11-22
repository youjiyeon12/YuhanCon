package com.yuhancon.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuhancon.domain.Member;
import com.yuhancon.domain.Role;
import com.yuhancon.repository.MemberRepository;
import com.yuhancon.web.MemberEditForm;
import com.yuhancon.web.SignupForm;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    public static final int MAX_LOGIN_FAILURES = 5;
    public static final int LOCK_MINUTES = 10;

    public static final String LOGIN_ID_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,12}$";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public Member get(String loginId) {
        return memberRepository.findById(loginId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    // 아이디는 대소문자를 구분하지 않도록 소문자로 맞춰 저장·비교한다
    public static String normalizeId(String loginId) {
        return loginId == null ? "" : loginId.strip().toLowerCase(Locale.ROOT);
    }

    public String checkLoginId(String loginId) {
        String id = normalizeId(loginId);
        if (!id.matches(LOGIN_ID_PATTERN)) return "아이디는 영문과 숫자를 섞어 6~12자로 입력하세요.";
        if (memberRepository.existsById(id)) return "이미 사용 중인 아이디입니다.";
        return null;
    }

    // 문제가 있으면 UserFacingException(어느 칸인지 field로 구분)
    @Transactional
    public Member signup(SignupForm form) {
        String id = normalizeId(form.getLoginId());
        String email = form.getEmail().strip().toLowerCase(Locale.ROOT);
        if (!form.getPassword().equals(form.getPasswordConfirm())) throw new FieldException("passwordConfirm", "비밀번호가 일치하지 않습니다.");
        if (memberRepository.existsById(id)) throw new FieldException("loginId", "이미 사용 중인 아이디입니다.");
        if (memberRepository.existsByEmail(email)) throw new FieldException("email", "이미 등록된 이메일입니다.");
        // 관리자는 가입으로 만들 수 없다
        if (form.getRole() != Role.USER && form.getRole() != Role.PERFORMER) throw new FieldException("role", "회원 유형을 다시 고르세요.");

        Member m = new Member();
        m.setLoginId(id);
        m.setEmail(email);
        m.setName(form.getName().strip());
        m.setPhone(form.getPhone());
        m.setRole(form.getRole());
        m.setPassword(passwordEncoder.encode(form.getPassword()));
        return memberRepository.saveAndFlush(m); // Member는 새 객체면 persist라 같은 아이디 동시 가입은 기본키 제약이 막는다
    }

    @Transactional
    public void update(String loginId, MemberEditForm form) {
        Member m = get(loginId);
        if (!passwordEncoder.matches(form.getCurrentPassword(), m.getPassword())) {
            throw new FieldException("currentPassword", "현재 비밀번호가 일치하지 않습니다.");
        }
        m.setName(form.getName().strip());
        m.setPhone(form.getPhone());
        if (form.getNewPassword() != null && !form.getNewPassword().isEmpty()) {
            m.setPassword(passwordEncoder.encode(form.getNewPassword()));
        }
    }

    // 비밀번호가 틀리면 센다. 5번째면 10분 잠그고 true. 없는 아이디는 아무것도 하지 않는다.
    @Transactional
    public boolean recordLoginFailure(String rawLoginId) {
        Member m = memberRepository.findById(normalizeId(rawLoginId)).orElse(null);
        if (m == null) return false;
        if (m.getLockedUntil() != null && !m.isLoginLocked()) { // 잠금이 풀린 뒤 처음 틀리면 다시 1부터
            m.setLockedUntil(null);
            m.setFailedLoginCount(0);
        }
        m.setFailedLoginCount(m.getFailedLoginCount() + 1);
        if (m.getFailedLoginCount() >= MAX_LOGIN_FAILURES) {
            m.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            m.setFailedLoginCount(0);
            return true;
        }
        return false;
    }

    @Transactional
    public void recordLoginSuccess(String loginId) {
        memberRepository.findById(loginId).ifPresent(m -> {
            m.setFailedLoginCount(0);
            m.setLockedUntil(null);
        });
    }

    public static class FieldException extends UserFacingException {
        private final String field;
        public FieldException(String field, String message) { super(message); this.field = field; }
        public String getField() { return field; }
    }
}
