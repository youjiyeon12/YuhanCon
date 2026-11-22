package com.yuhancon.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yuhancon.domain.ReservedSeat;

public interface ReservedSeatRepository extends JpaRepository<ReservedSeat, Long> {

    @Query("SELECT s.label FROM ReservedSeat s WHERE s.concert.id = :concertId")
    List<String> findLabelsByConcert(@Param("concertId") Long concertId);

    @Modifying
    @Query("DELETE FROM ReservedSeat s WHERE s.reserve.id = :reserveId")
    void deleteByReserveId(@Param("reserveId") Long reserveId);

    @Modifying
    @Query("DELETE FROM ReservedSeat s WHERE s.concert.id = :concertId")
    void deleteByConcertId(@Param("concertId") Long concertId);
}
