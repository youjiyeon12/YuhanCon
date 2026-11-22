package com.yuhancon.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yuhancon.domain.Concert;
import com.yuhancon.domain.ConcertLike;
import com.yuhancon.domain.Member;

public interface ConcertLikeRepository extends JpaRepository<ConcertLike, Long> {

    @Modifying
    @Query("DELETE FROM ConcertLike l WHERE l.member = :member AND l.concert = :concert")
    int deleteByMemberAndConcert(@Param("member") Member member, @Param("concert") Concert concert);

    @Query("SELECT l.concert.id FROM ConcertLike l WHERE l.member = :member")
    Set<Long> findConcertIdsByMember(@Param("member") Member member);

    @Query("SELECT l FROM ConcertLike l JOIN FETCH l.concert WHERE l.member = :member ORDER BY l.likedAt DESC")
    List<ConcertLike> findByMemberWithConcert(@Param("member") Member member);

    @Modifying
    @Query("DELETE FROM ConcertLike l WHERE l.concert.id = :concertId")
    void deleteByConcertId(@Param("concertId") Long concertId);
}
