package com.yuhancon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yuhancon.domain.Reserve;

public interface ReserveRepository extends JpaRepository<Reserve, Long> {
	@Modifying
	@Query("DELETE FROM Reserve r WHERE r.concert.id = :concertId")
	void deleteByConcertId(@Param("concertId") Long concertId);
}
