package com.yuhancon.controller;

import java.security.Principal;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yuhancon.domain.Board;
import com.yuhancon.domain.Member;
import com.yuhancon.service.BoardService;
import com.yuhancon.service.MemberService;
import com.yuhancon.service.UserFacingException;
import com.yuhancon.web.BoardForm;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final MemberService memberService;

    private Member me(Principal p) {
        return memberService.get(p.getName());
    }

    @GetMapping("/boardMain")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Board> boards = boardService.page(page);
        if (boards.getTotalPages() > 0 && page >= boards.getTotalPages()) {
            return "redirect:/boardMain?page=" + (boards.getTotalPages() - 1); // 없는 페이지면 마지막 페이지로
        }
        model.addAttribute("boardPage", boards);
        model.addAttribute("commentCounts", boardService.commentCounts(boards.getContent()));
        return "boardMain";
    }

    @GetMapping("/boardDetail/{id}")
    public String detail(@PathVariable Long id, Model model, Principal principal) {
        Member me = me(principal);
        Board b = boardService.get(id);
        // 좋아요·댓글을 남기고 돌아올 때(flash가 있을 때)는 조회수를 다시 올리지 않는다
        boolean returning = model.containsAttribute("successMessage") || model.containsAttribute("errorMessage");
        model.addAttribute("viewCount", returning ? b.getCnt() : boardService.view(b, me));
        model.addAttribute("board", b);
        model.addAttribute("canEdit", BoardService.canEdit(b, me));
        model.addAttribute("liked", boardService.liked(b, me));
        java.util.List<com.yuhancon.domain.Comment> all = boardService.comments(id);
        model.addAttribute("comments", all.stream().filter(c -> c.getParent() == null).toList());
        model.addAttribute("replies", all.stream().filter(c -> c.getParent() != null)
            .collect(java.util.stream.Collectors.groupingBy(c -> c.getParent().getId())));
        model.addAttribute("commentCount", all.stream().filter(c -> !c.isDeleted()).count());
        model.addAttribute("deletableIds", all.stream().filter(c -> BoardService.canDelete(c, me)).map(com.yuhancon.domain.Comment::getId).toList());
        return "boardDetail";
    }

    @PostMapping("/boardDetail/{id}/comment")
    public String comment(@PathVariable Long id, @RequestParam(required = false) Long parentId,
                          @RequestParam(defaultValue = "") String content, Principal principal, RedirectAttributes ra) {
        try {
            com.yuhancon.domain.Comment c = boardService.addComment(id, parentId, content, me(principal));
            return "redirect:/boardDetail/" + id + "#comment-" + c.getId();
        } catch (UserFacingException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            ra.addFlashAttribute("commentDraft", content); // 쓴 내용이 사라지지 않게
            ra.addFlashAttribute("draftParentId", parentId);
            return "redirect:/boardDetail/" + id + "#comments";
        }
    }

    @PostMapping("/comment/{id}/delete")
    public String deleteComment(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        Long boardId = boardService.deleteComment(id, me(principal));
        ra.addFlashAttribute("successMessage", "댓글을 삭제했습니다.");
        return "redirect:/boardDetail/" + boardId + "#comments";
    }

    @PostMapping("/board/{id}/like")
    public String like(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        boolean on;
        try {
            on = boardService.toggleLike(id, me(principal));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            on = true; // 두 번 빠르게 눌러 이미 저장된 경우
        }
        ra.addFlashAttribute("successMessage", on ? "이 글에 좋아요를 눌렀습니다." : "좋아요를 취소했습니다.");
        return "redirect:/boardDetail/" + id;
    }

    @GetMapping("/boardWrite")
    public String writeForm(Model model) {
        model.addAttribute("form", new BoardForm());
        model.addAttribute("board", null);
        return "boardForm";
    }

    @PostMapping("/boardWrite")
    public String write(@ModelAttribute("form") BoardForm form, Principal principal, Model model, RedirectAttributes ra) {
        try {
            Board b = boardService.create(form, me(principal));
            ra.addFlashAttribute("successMessage", "글을 등록했습니다.");
            return "redirect:/boardDetail/" + b.getId();
        } catch (UserFacingException e) {
            model.addAttribute("errorMessage", e.getMessage()); // 쓴 내용이 사라지지 않게 폼을 다시 보여 준다
            model.addAttribute("board", null);
            return "boardForm";
        }
    }

    @GetMapping("/boardEdit/{id}")
    public String editForm(@PathVariable Long id, Model model, Principal principal) {
        Board b = boardService.getEditable(id, me(principal));
        BoardForm form = new BoardForm();
        form.setTitle(b.getTitle());
        form.setContent(b.getContent());
        model.addAttribute("form", form);
        model.addAttribute("board", b);
        return "boardForm";
    }

    @PostMapping("/boardEdit/{id}")
    public String edit(@PathVariable Long id, @ModelAttribute("form") BoardForm form, Principal principal,
                       Model model, RedirectAttributes ra) {
        Member me = me(principal);
        try {
            boardService.update(id, form, me);
        } catch (UserFacingException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("board", boardService.getEditable(id, me));
            return "boardForm";
        }
        ra.addFlashAttribute("successMessage", "글을 수정했습니다.");
        return "redirect:/boardDetail/" + id;
    }

    @PostMapping("/boardDelete/{id}")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        boardService.delete(id, me(principal));
        ra.addFlashAttribute("successMessage", "글을 삭제했습니다.");
        return "redirect:/boardMain";
    }
}
