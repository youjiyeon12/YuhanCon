package com.yuhancon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yuhancon.domain.Concert;
import com.yuhancon.domain.Member;
import com.yuhancon.domain.Reserve;

public interface ReserveRepository extends JpaRepository<Reserve, Long> {

    // 예매 수정·취소는 이 행을 잠그고 읽어, 두 번 제출해도 차례로 처리되고 두 번째는 바뀐 값을 본다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reserve r WHERE r.id = :id")
    Optional<Reserve> findForUpdate(@Param("id") Long id);

    Optional<Reserve> findByMemberAndConcert(Member member, Concert concert);

    @Query("SELECT r FROM Reserve r JOIN FETCH r.concert WHERE r.member = :member ORDER BY r.concert.date ASC, r.id DESC")
    List<Reserve> findByMemberWithConcert(@Param("member") Member member);

    @Query("SELECT COALESCE(SUM(r.seatCount), 0) FROM Reserve r WHERE r.concert.id = :concertId")
    int sumSeatsByConcert(@Param("concertId") Long concertId);

    // 같은 예매를 동시에 두 번 취소해도 한 번만 지워지도록 지운 행 수를 돌려준다
    @Modifying
    @Query("DELETE FROM Reserve r WHERE r.id = :id")
    int deleteReserveById(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM Reserve r WHERE r.concert.id = :concertId")
    void deleteByConcertId(@Param("concertId") Long concertId);
}
