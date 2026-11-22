package com.yuhancon.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.yuhancon.domain.Member;
import com.yuhancon.service.BoardService;
import com.yuhancon.service.ConcertService;
import com.yuhancon.service.MemberService;
import com.yuhancon.service.ReserveService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MypageController {

    private final MemberService memberService;
    private final ReserveService reserveService;
    private final ConcertService concertService;
    private final BoardService boardService;

    @GetMapping("/mypage")
    public String mypage(Model model, Principal principal) {
        Member me = memberService.get(principal.getName());
        model.addAttribute("member", me);
        model.addAttribute("reserves", reserveService.mine(me));
        model.addAttribute("likes", concertService.likes(me));
        model.addAttribute("myPosts", boardService.mine(me));
        model.addAttribute("recentConcerts", concertService.recent(me));
        if (me.canManageConcerts()) model.addAttribute("myConcerts", concertService.mine(me));
        return "mypage";
    }
}
