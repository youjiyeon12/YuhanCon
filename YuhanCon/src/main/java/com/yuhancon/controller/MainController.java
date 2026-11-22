package com.yuhancon.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.yuhancon.domain.Category;
import com.yuhancon.service.ConcertService;
import com.yuhancon.service.MemberService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final ConcertService concertService;
    private final MemberService memberService;

    @GetMapping("/")
    public String root() {
        return "redirect:/main";
    }

    @GetMapping("/main")
    public String main(Model model, Principal principal) {
        model.addAttribute("slides", concertService.slides());
        model.addAttribute("week", concertService.thisWeek());
        model.addAttribute("today", java.time.LocalDate.now());
        model.addAttribute("groups", concertService.homeGroups());
        model.addAttribute("categories", Category.values());
        model.addAttribute("likedIds", principal == null ? java.util.Set.of() : concertService.likedIds(memberService.get(principal.getName())));
        return "main";
    }
}
