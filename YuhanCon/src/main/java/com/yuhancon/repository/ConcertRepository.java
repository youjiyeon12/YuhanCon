package com.yuhancon.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yuhancon.domain.Concert;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

}
