package com.yuhancon.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yuhancon.domain.Board;
import com.yuhancon.domain.Member;

public interface BoardRepository extends JpaRepository<Board, Long> {

    List<Board> findTop5ByMemberOrderByIdDesc(Member member);

    @EntityGraph(attributePaths = "member")
    Page<Board> findAll(Pageable pageable);

    // 읽고 +1 해서 저장하면 동시 조회 때 증가분이 사라지므로 한 문장으로 올린다
    @Modifying
    @Query("UPDATE Board b SET b.cnt = b.cnt + 1 WHERE b.id = :id")
    void increaseViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Board b SET b.likeCount = b.likeCount + :delta WHERE b.id = :id")
    void addLikeCount(@Param("id") Long id, @Param("delta") int delta);
}
