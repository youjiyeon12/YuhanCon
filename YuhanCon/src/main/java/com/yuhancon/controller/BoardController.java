package com.yuhancon.controller;

import java.time.LocalDate;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.yuhancon.domain.Board;
import com.yuhancon.domain.Member;
import com.yuhancon.repository.BoardRepository;
import com.yuhancon.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BoardController {

    private final BoardRepository boardRepository;

    // 글쓰기 폼
    @GetMapping("/boardWrite")
    public String writeForm(Model model) {
        model.addAttribute("board", new Board());  
        return "boardWrite";
    }

    // 글쓰기 저장
    @PostMapping("/boardWrite")
    public String writeSubmit(
        @ModelAttribute Board board,  
        @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Member loginMember = customUserDetails.getMember();
        board.setMember(loginMember);
        board.setBoardAt(LocalDate.now());
        board.setCnt(0);
        board.setImage(null); 

        boardRepository.save(board);  
        return "redirect:/boardMain";
    }

    // 게시판 메인
    @GetMapping("/boardMain")
    public String list(Model model) {

        model.addAttribute("boardlist", boardRepository.findAll()); //게시글 리스트 가져오려면 boardlist 사용. 
        return "boardMain";
    }

    @GetMapping("/boardDetail/{id}")
    public String detail(@PathVariable Long id,
                         Model model,
                         @AuthenticationPrincipal UserDetails userDetails) {

        Board board = boardRepository.findById(id).orElseThrow();

        // 조회수 증가
        board.setCnt(board.getCnt() + 1);
        boardRepository.save(board);

        // 로그인 사용자와 작성자 비교
        boolean isOwner = false;
        if (userDetails != null && board.getMember() != null) {
            String loginEmail = userDetails.getUsername(); // 로그인한 사용자 이메일
            String writerEmail = board.getMember().getEmail(); // 작성자 이메일
            isOwner = loginEmail.equals(writerEmail);
        }

        model.addAttribute("board", board);
        model.addAttribute("isOwner", isOwner);

        return "boardDetail";
    }

    // 수정 view
    @GetMapping("/boardEdit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Board board = boardRepository.findById(id).orElseThrow();
        model.addAttribute("board", board);
        return "boardEdit"; // 수정 폼 HTML
    }

    
    //글 수정 데이터 처리 
    @PostMapping("/boardEdit/{id}")
    public String editSubmit(@PathVariable Long id, @ModelAttribute Board updatedBoard) {
        Board board = boardRepository.findById(id).orElseThrow();

        // 수동으로 기존 board에 필드 덮어쓰기
        board.setTitle(updatedBoard.getTitle());
        board.setContent(updatedBoard.getContent());
        board.setImage(updatedBoard.getImage()); // 나중에 이미지도 수정할 경우 대비

        boardRepository.save(board); // id가 존재하므로 update 실행됨
        return "redirect:/boardDetail/" + id;
    }
    
    //글 삭제
    @PostMapping("/boardDelete/{id}")
    public String delete(@PathVariable Long id) {
        boardRepository.deleteById(id);
        return "redirect:/boardMain";
    }
    
}
