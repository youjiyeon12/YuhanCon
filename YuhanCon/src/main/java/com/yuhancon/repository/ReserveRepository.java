package com.yuhancon.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yuhancon.domain.Reserve;

public interface ReserveRepository extends JpaRepository<Reserve, Long> {

}
