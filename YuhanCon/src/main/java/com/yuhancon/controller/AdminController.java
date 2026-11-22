package com.yuhancon.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yuhancon.domain.Concert;
import com.yuhancon.service.ConcertService;
import com.yuhancon.service.MemberService;
import com.yuhancon.service.UserFacingException;

import lombok.RequiredArgsConstructor;

// 관리자 화면: 공연 승인(전체 선택 일괄 승인), 홈 슬라이드 선택, 전체 공연 관리. /admin/**은 ADMIN만(SecurityConfig).
@Controller
@RequiredArgsConstructor
public class AdminController {

    private final ConcertService concertService;
    private final MemberService memberService;

    @GetMapping("/admin")
    public String admin(Model model) {
        List<Concert> candidates = concertService.slideCandidates();
        model.addAttribute("pending", concertService.pending());
        model.addAttribute("candidates", candidates);
        // 관리자가 정한 순서대로(화면의 순서 목록이 이 순서로 시작한다)
        model.addAttribute("selectedIds", candidates.stream().filter(c -> c.getSlideOrder() != null)
            .sorted(java.util.Comparator.comparing(Concert::getSlideOrder)).map(Concert::getId).toList());
        model.addAttribute("slides", concertService.slides());
        model.addAttribute("concerts", concertService.all());
        model.addAttribute("maxSlides", ConcertService.MAX_SLIDES);
        model.addAttribute("minSlides", ConcertService.MIN_SLIDES);
        return "admin";
    }

    @PostMapping("/admin/approve")
    public String approve(@RequestParam(required = false) List<Long> ids, Principal principal, RedirectAttributes ra) {
        try {
            int n = concertService.approve(ids, memberService.get(principal.getName()));
            ra.addFlashAttribute("successMessage", "공연 " + n + "개를 승인했습니다.");
        } catch (UserFacingException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/slides")
    public String slides(@RequestParam(required = false) List<Long> ids, Principal principal, RedirectAttributes ra) {
        try {
            concertService.setSlides(ids, memberService.get(principal.getName()));
            ra.addFlashAttribute("successMessage", "홈 슬라이드를 저장했습니다.");
        } catch (UserFacingException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin#slides";
    }
}
