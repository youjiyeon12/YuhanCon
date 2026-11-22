package com.yuhancon.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yuhancon.domain.Concert;
import com.yuhancon.domain.Member;
import com.yuhancon.domain.RecentConcertView;

public interface RecentConcertViewRepository extends JpaRepository<RecentConcertView, Long> {

    @Query("SELECT v FROM RecentConcertView v JOIN FETCH v.concert WHERE v.member = :member ORDER BY v.viewedAt DESC")
    List<RecentConcertView> findRecentWithConcert(@Param("member") Member member, Pageable pageable);

    @Modifying
    @Query("DELETE FROM RecentConcertView v WHERE v.member = :member AND v.concert = :concert")
    void deleteByMemberAndConcert(@Param("member") Member member, @Param("concert") Concert concert);

    @Modifying
    @Query("DELETE FROM RecentConcertView v WHERE v.concert.id = :concertId")
    void deleteByConcertId(@Param("concertId") Long concertId);
}
