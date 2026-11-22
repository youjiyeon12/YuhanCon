package com.yuhancon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yuhancon.domain.Board;
import com.yuhancon.domain.BoardLike;
import com.yuhancon.domain.Member;

public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {

    boolean existsByMemberAndBoard(Member member, Board board);

    @Modifying
    @Query("DELETE FROM BoardLike l WHERE l.member = :member AND l.board = :board")
    int deleteByMemberAndBoard(@Param("member") Member member, @Param("board") Board board);

    @Modifying
    @Query("DELETE FROM BoardLike l WHERE l.board.id = :boardId")
    void deleteByBoardId(@Param("boardId") Long boardId);
}
