package com.yuhancon.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yuhancon.domain.Category;
import com.yuhancon.domain.Concert;
import com.yuhancon.domain.ConcertStatus;
import com.yuhancon.domain.Member;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    // 승인된 다가오는 공연. category가 null이면 전체
    @Query("SELECT c FROM Concert c WHERE c.status = com.yuhancon.domain.ConcertStatus.APPROVED"
         + " AND c.date >= :today AND (:category IS NULL OR c.category = :category)")
    List<Concert> findUpcoming(@Param("today") LocalDate today, @Param("category") Category category, Sort sort, org.springframework.data.domain.Limit limit);

    List<Concert> findTop8ByStatusAndDateLessThanOrderByDateDesc(ConcertStatus status, LocalDate today);

    List<Concert> findByStatusAndDateBetweenOrderByDateAsc(ConcertStatus status, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = "member")
    List<Concert> findByStatusOrderByCreatedAtAsc(ConcertStatus status);

    @EntityGraph(attributePaths = "member")
    List<Concert> findAllByOrderByDateDesc();

    List<Concert> findByMemberOrderByCreatedAtDesc(Member member);

    // 홈 슬라이드: 관리자가 고른 공연(끝나지 않은 것) / 부족할 때 채울 가까운 공연
    List<Concert> findByStatusAndDateGreaterThanEqualAndSlideOrderIsNotNullAndImageIsNotNullOrderBySlideOrderAsc(ConcertStatus status, LocalDate today);
    List<Concert> findByStatusAndDateGreaterThanEqualAndImageIsNotNullOrderByDateAsc(ConcertStatus status, LocalDate today);

    // 공연 수정은 이 행을 잠그고 읽는다. 예매·좋아요의 UPDATE는 같은 행 잠금을 기다리므로 서로 덮어쓰지 않는다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Concert c WHERE c.id = :id")
    java.util.Optional<Concert> findForUpdate(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Concert c SET c.status = com.yuhancon.domain.ConcertStatus.APPROVED WHERE c.id IN :ids")
    int approveAll(@Param("ids") Collection<Long> ids);

    // 이미 읽어 둔 공연 객체에 예전 순서가 남으면 같은 순서를 다시 저장할 때 UPDATE가 빠지므로 컨텍스트를 비운다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Concert c SET c.slideOrder = NULL WHERE c.slideOrder IS NOT NULL")
    void clearSlides();

    // 남은 좌석 확인과 차감을 한 문장으로 처리해 동시에 예매해도 음수가 되지 않는다. 차감했으면 1
    @Modifying
    @Query("UPDATE Concert c SET c.availableSeats = c.availableSeats - :count WHERE c.id = :id AND c.availableSeats >= :count")
    int decreaseAvailableSeats(@Param("id") Long id, @Param("count") int count);

    @Modifying
    @Query("UPDATE Concert c SET c.availableSeats = c.availableSeats + :count WHERE c.id = :id")
    int increaseAvailableSeats(@Param("id") Long id, @Param("count") int count);

    @Modifying
    @Query("UPDATE Concert c SET c.likeCount = c.likeCount + :delta WHERE c.id = :id")
    void addLikeCount(@Param("id") Long id, @Param("delta") int delta);
}
