package com.yuhancon.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yuhancon.domain.Member;
import com.yuhancon.domain.Role;

public interface MemberRepository extends JpaRepository<Member, String> {
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
}
