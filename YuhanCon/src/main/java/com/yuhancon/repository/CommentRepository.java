package com.yuhancon.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yuhancon.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.member LEFT JOIN FETCH c.parent WHERE c.board.id = :boardId ORDER BY c.id")
    List<Comment> findByBoardWithMember(@Param("boardId") Long boardId);

    boolean existsByParentIdAndDeletedFalse(Long parentId);

    // 게시판 목록의 댓글 수(지운 댓글 제외): [게시글 id, 개수]
    @Query("SELECT c.board.id, COUNT(c) FROM Comment c WHERE c.board.id IN :ids AND c.deleted = false GROUP BY c.board.id")
    List<Object[]> countByBoardIds(@Param("ids") Collection<Long> ids);

    // 답글을 먼저 지워야 부모 댓글을 지울 수 있다(외래키)
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.board.id = :boardId AND c.parent IS NOT NULL")
    void deleteRepliesByBoardId(@Param("boardId") Long boardId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.board.id = :boardId")
    void deleteByBoardId(@Param("boardId") Long boardId);
}
