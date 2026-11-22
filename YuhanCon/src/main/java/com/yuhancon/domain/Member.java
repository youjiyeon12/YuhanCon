package com.yuhancon.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 회원. 아이디(loginId)가 기본키다. 입력 검증은 web.SignupForm·MemberEditForm이 맡는다.
@Entity
@Getter @Setter @NoArgsConstructor
public class Member implements Persistable<String> {

    @Id
    @Column(length = 12)
    private String loginId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // BCrypt 해시

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 11)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    // 로그인 연속 실패 횟수와 잠금 해제 시각(MemberService.recordLoginFailure)
    @ColumnDefault("0") @Column(nullable = false)
    private int failedLoginCount;

    private LocalDateTime lockedUntil;

    public boolean isLoginLocked() { return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now()); }

    // 아이디가 기본키라 save()가 같은 키를 덮어쓰지(merge) 않고 새 행만 넣게(persist) 한다
    @Transient
    private boolean newEntity = true;

    @Override public String getId() { return loginId; }
    @Override public boolean isNew() { return newEntity; }
    @PostLoad @PostPersist void markNotNew() { newEntity = false; }

    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean canManageConcerts() { return role == Role.ADMIN || role == Role.PERFORMER; }
}
