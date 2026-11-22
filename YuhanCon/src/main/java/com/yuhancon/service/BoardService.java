package com.yuhancon.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuhancon.domain.Board;
import com.yuhancon.domain.BoardLike;
import com.yuhancon.domain.Comment;
import com.yuhancon.domain.Member;
import com.yuhancon.repository.BoardLikeRepository;
import com.yuhancon.repository.BoardRepository;
import com.yuhancon.repository.CommentRepository;
import com.yuhancon.web.BoardForm;

import com.yuhancon.security.SecurityLog;
import lombok.RequiredArgsConstructor;

// 게시판: 누구나(로그인) 쓰고, 본인 글만 고치거나 지운다. 관리자는 모든 글을 고치거나 지울 수 있다.
// 글은 화면에서 th:text로만 출력하므로 <script>를 써도 글자로 보일 뿐 실행되지 않는다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    public static final int PAGE_SIZE = 10;
    public static final int MAX_TITLE = 100;
    public static final int MAX_CONTENT = 5000;
    public static final int MAX_COMMENT = 1000;

    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;
    private final BoardLikeRepository likeRepository;

    public Page<Board> page(int page) {
        return boardRepository.findAll(PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id")));
    }

    public List<Board> mine(Member member) {
        return boardRepository.findTop5ByMemberOrderByIdDesc(member);
    }

    public Board get(Long id) {
        return boardRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public static boolean canEdit(Board b, Member m) {
        return m != null && (m.isAdmin() || b.getMember().getLoginId().equals(m.getLoginId()));
    }

    public Board getEditable(Long id, Member m) {
        Board b = get(id);
        if (!canEdit(b, m)) throw new AccessDeniedException("본인 글만 고치거나 지울 수 있습니다.");
        return b;
    }

    // 작성자가 아니면 조회수를 올린다. 화면에 보일 조회수를 돌려준다(UPDATE는 읽어 둔 객체에 반영되지 않으므로).
    @Transactional
    public int view(Board b, Member viewer) {
        boolean self = b.getMember().getLoginId().equals(viewer.getLoginId());
        if (!self) boardRepository.increaseViewCount(b.getId());
        return b.getCnt() + (self ? 0 : 1);
    }

    @Transactional
    public Board create(BoardForm form, Member member) {
        Board b = new Board();
        apply(form, b);
        b.setMember(member);
        b.setBoardAt(LocalDate.now());
        return boardRepository.save(b);
    }

    @Transactional
    public Board update(Long id, BoardForm form, Member member) {
        Board b = getEditable(id, member);
        apply(form, b);
        return b;
    }

    // 댓글(답글 먼저)과 좋아요를 지운 뒤 글을 지운다(cascade가 없으므로 직접)
    @Transactional
    public void delete(Long id, Member member) {
        Board b = getEditable(id, member);
        commentRepository.detachParentsByBoardId(id);
        commentRepository.deleteByBoardId(id);
        likeRepository.deleteByBoardId(id);
        boardRepository.delete(b);
        if (member.isAdmin() && !b.getMember().getLoginId().equals(member.getLoginId()))
            SecurityLog.LOG.info("ADMIN_BOARD_DELETE by={} board={} author={}", member.getLoginId(), id, b.getMember().getLoginId());
    }

    // ---------- 댓글·대댓글 ----------

    public List<Comment> comments(Long boardId) {
        return commentRepository.findByBoardWithMember(boardId);
    }

    // 게시판 목록에 보여 줄 글별 댓글 수
    public Map<Long, Long> commentCounts(List<Board> boards) {
        Map<Long, Long> counts = new HashMap<>();
        if (boards.isEmpty()) return counts;
        commentRepository.countByBoardIds(boards.stream().map(Board::getId).toList())
            .forEach(row -> counts.put((Long) row[0], (Long) row[1]));
        return counts;
    }

    public static boolean canDelete(Comment c, Member m) {
        return m != null && !c.isDeleted() && (m.isAdmin() || c.getMember().getLoginId().equals(m.getLoginId()));
    }

    // parentId가 있으면 그 댓글의 답글. 답글에도 답글을 달 수 있다(단계 제한 없음, 화면 들여쓰기만 3단계까지).
    @Transactional
    public Comment addComment(Long boardId, Long parentId, String rawContent, Member member) {
        Board b = get(boardId);
        String content = rawContent == null ? "" : rawContent.replace("\r\n", "\n").strip();
        if (content.isEmpty()) throw new UserFacingException("댓글 내용을 입력하세요.");
        if (content.length() > MAX_COMMENT) throw new UserFacingException("댓글은 " + MAX_COMMENT + "자 이하로 입력하세요.");
        Comment c = new Comment();
        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId)
                .filter(p -> p.getBoard().getId().equals(boardId))
                .orElseThrow(() -> new UserFacingException("답글을 달 댓글이 없습니다."));
            if (parent.isDeleted()) throw new UserFacingException("삭제된 댓글에는 답글을 달 수 없습니다.");
            c.setParent(parent);
        }
        c.setBoard(b);
        c.setMember(member);
        c.setContent(content);
        c.setCreatedAt(LocalDateTime.now());
        return commentRepository.save(c);
    }

    // 본인 댓글만(관리자는 전부). 답글이 달린 댓글은 "삭제된 댓글"로만 표시하고, 아니면 행을 지운다.
    // 행을 지운 뒤 답글이 없어진 "삭제된 댓글" 조상도 위로 올라가며 지운다. 글 id를 돌려준다.
    @Transactional
    public Long deleteComment(Long commentId, Member member) {
        Comment c = commentRepository.findById(commentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!canDelete(c, member)) throw new AccessDeniedException("본인 댓글만 지울 수 있습니다.");
        Long boardId = c.getBoard().getId();
        Comment parent = c.getParent();
        if (member.isAdmin() && !c.getMember().getLoginId().equals(member.getLoginId()))
            SecurityLog.LOG.info("ADMIN_COMMENT_DELETE by={} comment={} author={}", member.getLoginId(), commentId, c.getMember().getLoginId());
        if (commentRepository.existsByParentId(c.getId())) {
            c.setDeleted(true);
            c.setContent("");
            return boardId;
        }
        commentRepository.delete(c);
        commentRepository.flush();
        while (parent != null && parent.isDeleted() && !commentRepository.existsByParentId(parent.getId())) {
            Comment up = parent.getParent();
            commentRepository.delete(parent);
            commentRepository.flush();
            parent = up;
        }
        return boardId;
    }

    // ---------- 좋아요 ----------

    public boolean liked(Board b, Member m) {
        return m != null && likeRepository.existsByMemberAndBoard(m, b);
    }

    // 누르면 켜고 다시 누르면 끈다. 켜졌으면 true
    @Transactional
    public boolean toggleLike(Long boardId, Member member) {
        Board b = get(boardId);
        if (likeRepository.deleteByMemberAndBoard(member, b) > 0) {
            boardRepository.addLikeCount(boardId, -1);
            return false;
        }
        BoardLike like = new BoardLike();
        like.setMember(member);
        like.setBoard(b);
        like.setLikedAt(LocalDateTime.now());
        likeRepository.saveAndFlush(like);
        boardRepository.addLikeCount(boardId, 1);
        return true;
    }

    // 브라우저는 줄바꿈을 CRLF(2자)로 보내므로 화면의 글자 수와 맞도록 LF로 바꾼 뒤 센다
    private void apply(BoardForm f, Board b) {
        String title = f.getTitle() == null ? "" : f.getTitle().strip();
        String content = f.getContent() == null ? "" : f.getContent().replace("\r\n", "\n");
        if (title.isEmpty()) throw new UserFacingException("제목을 입력하세요.");
        if (title.length() > MAX_TITLE) throw new UserFacingException("제목은 " + MAX_TITLE + "자 이하로 입력하세요.");
        if (content.isBlank()) throw new UserFacingException("내용을 입력하세요.");
        if (content.length() > MAX_CONTENT) throw new UserFacingException("내용은 " + MAX_CONTENT + "자 이하로 입력하세요.");
        b.setTitle(title);
        b.setContent(content);
    }
}
