package com.yuhancon.controller;

import java.security.Principal;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yuhancon.domain.Concert;
import com.yuhancon.domain.Member;
import com.yuhancon.domain.Reserve;
import com.yuhancon.service.MemberService;
import com.yuhancon.service.ReserveService;
import com.yuhancon.service.UserFacingException;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ReserveController {

    private static final String TAKEN = "방금 다른 분이 같은 좌석을 예매했습니다. 다른 좌석을 골라 주세요.";

    private final ReserveService reserveService;
    private final MemberService memberService;

    private Member me(Principal p) {
        return memberService.get(p.getName());
    }

    @PostMapping("/reserve")
    public String reserve(@RequestParam Long concertId, @RequestParam(defaultValue = "0") int seatCount,
                          @RequestParam(required = false) List<String> seats,
                          Principal principal, RedirectAttributes ra) {
        try {
            Reserve r = reserveService.reserve(concertId, seatCount, seats, me(principal));
            ra.addFlashAttribute("successMessage", "「" + r.getConcert().getTitle() + "」 " + r.getSeatCount() + "석 예매가 완료되었습니다.");
            return "redirect:/mypage";
        } catch (UserFacingException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("errorMessage", seats == null ? "이미 예매한 공연입니다." : TAKEN);
        }
        return "redirect:/concertDetail/" + concertId;
    }

    // 예매 수정 화면: 내 좌석은 고른 상태로, 남의 좌석은 막힌 상태로 보여 준다
    @GetMapping("/reserveEdit/{id}")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        Reserve r = reserveService.getOwn(id, me(principal));
        Concert c = r.getConcert();
        Set<String> taken = reserveService.takenSeats(c.getId());
        List<String> mine = r.getSeats() == null ? List.of() : List.of(r.getSeats().split(","));
        taken.removeAll(mine);
        model.addAttribute("reserve", r);
        model.addAttribute("concert", c);
        model.addAttribute("taken", taken);
        model.addAttribute("mySeats", mine);
        model.addAttribute("maxSeats", ReserveService.MAX_SEATS_PER_MEMBER);
        return "reserveEdit";
    }

    @PostMapping("/reserveEdit/{id}")
    public String edit(@PathVariable Long id, @RequestParam(defaultValue = "0") int seatCount,
                       @RequestParam(required = false) List<String> seats,
                       Principal principal, RedirectAttributes ra) {
        try {
            reserveService.update(id, seatCount, seats, me(principal));
            ra.addFlashAttribute("successMessage", "예매를 수정했습니다.");
            return "redirect:/mypage";
        } catch (UserFacingException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("errorMessage", TAKEN);
        }
        return "redirect:/reserveEdit/" + id;
    }

    @PostMapping("/cancelReserve/{id}")
    public String cancel(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            Concert c = reserveService.cancel(id, me(principal));
            ra.addFlashAttribute("successMessage", "「" + c.getTitle() + "」 예매를 취소했습니다.");
        } catch (UserFacingException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/mypage";
    }
}
