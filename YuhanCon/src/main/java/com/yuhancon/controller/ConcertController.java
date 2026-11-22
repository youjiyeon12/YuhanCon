package com.yuhancon.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yuhancon.domain.Category;
import com.yuhancon.domain.Concert;
import com.yuhancon.domain.Member;
import com.yuhancon.service.ConcertService;
import com.yuhancon.service.MemberService;
import com.yuhancon.service.ReserveService;
import com.yuhancon.service.UserFacingException;
import com.yuhancon.web.ConcertForm;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;
    private final ReserveService reserveService;
    private final MemberService memberService;

    private Member me(Principal p) {
        return p == null ? null : memberService.get(p.getName());
    }

    @GetMapping("/concertList")
    public String list(@RequestParam(required = false) String category,
                       @RequestParam(defaultValue = "latest") String sort,
                       Model model, Principal principal) {
        Category cat = parseCategory(category);
        Member me = me(principal);
        model.addAttribute("concerts", concertService.upcoming(cat, sort));
        model.addAttribute("pastConcerts", concertService.pastTop8());
        model.addAttribute("categories", Category.values());
        model.addAttribute("category", cat);
        model.addAttribute("sort", sort);
        model.addAttribute("likedIds", concertService.likedIds(me));
        if (me != null && me.canManageConcerts()) {
            model.addAttribute("myPending", concertService.mine(me).stream().filter(c -> !c.isApproved()).toList());
        }
        return "concertList";
    }

    private static Category parseCategory(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Category.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null; // 모르는 분류면 전체
        }
    }

    @GetMapping("/concertDetail/{id}")
    public String detail(@PathVariable Long id, Model model, Principal principal) {
        Member me = me(principal);
        Concert c = concertService.getVisible(id, me);
        model.addAttribute("concert", c);
        model.addAttribute("canEdit", ConcertService.canEdit(c, me));
        model.addAttribute("liked", concertService.likedIds(me).contains(id));
        model.addAttribute("myReserve", reserveService.find(me, c).orElse(null));
        model.addAttribute("taken", c.isSeated() ? reserveService.takenSeats(id) : java.util.Set.of());
        model.addAttribute("maxSeats", ReserveService.MAX_SEATS_PER_MEMBER);
        if (me != null) concertService.recordView(c, me);
        return "concertDetail";
    }

    @GetMapping("/concertWrite")
    public String writeForm(Model model) {
        return form(model, new ConcertForm(), null, null);
    }

    @PostMapping("/concert/save")
    public String save(@ModelAttribute("form") ConcertForm form, @RequestParam(value = "imageFile", required = false) MultipartFile image,
                       Principal principal, Model model, RedirectAttributes ra) {
        Member me = me(principal);
        Concert c;
        try {
            c = concertService.create(form, image, me);
        } catch (UserFacingException e) {
            model.addAttribute("errorMessage", e.getMessage()); // 입력한 내용이 사라지지 않게 폼을 다시 보여 준다
            return form(model, form, null, null);
        }
        ra.addFlashAttribute("successMessage", c.isApproved()
            ? "공연을 등록했습니다."
            : "공연을 등록했습니다. 관리자가 승인하면 공연 정보에 공개됩니다.");
        return "redirect:/concertDetail/" + c.getId();
    }

    @GetMapping("/concertEdit/{id}")
    public String editForm(@PathVariable Long id, Model model, Principal principal) {
        Concert c = concertService.getEditable(id, me(principal));
        return form(model, ConcertForm.of(c), c, concertService.reservedSeats(id));
    }

    @PostMapping("/concertEdit/{id}")
    public String edit(@PathVariable Long id, @ModelAttribute("form") ConcertForm form,
                       @RequestParam(value = "imageFile", required = false) MultipartFile image,
                       Principal principal, Model model, RedirectAttributes ra) {
        Member me = me(principal);
        try {
            concertService.update(id, form, image, me);
        } catch (UserFacingException e) {
            Concert c = concertService.getEditable(id, me);
            model.addAttribute("errorMessage", e.getMessage());
            return form(model, form, c, concertService.reservedSeats(id));
        }
        ra.addFlashAttribute("successMessage", "공연 정보를 수정했습니다.");
        return "redirect:/concertDetail/" + id;
    }

    @PostMapping("/concertDelete/{id}")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra,
                         @RequestParam(defaultValue = "/concertList") String back) {
        try {
            concertService.delete(id, me(principal));
        } catch (UserFacingException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/concertDetail/" + id;
        }
        ra.addFlashAttribute("successMessage", "공연을 삭제했습니다.");
        return "redirect:" + ("/admin".equals(back) ? "/admin" : "/concertList"); // 다른 사이트로 보내지 않도록 정해진 곳만
    }

    @PostMapping("/concert/{id}/like")
    public String like(@PathVariable Long id, Principal principal, RedirectAttributes ra,
                       @RequestParam(defaultValue = "detail") String back) {
        boolean on;
        try {
            on = concertService.toggleLike(id, me(principal));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            on = true; // 두 번 빠르게 눌러 이미 좋아요가 저장된 경우
        }
        ra.addFlashAttribute("successMessage", on ? "좋아요한 공연에 담았습니다." : "좋아요를 취소했습니다.");
        return switch (back) {
            case "list" -> "redirect:/concertList";
            case "mypage" -> "redirect:/mypage";
            case "main" -> "redirect:/main";
            default -> "redirect:/concertDetail/" + id;
        };
    }

    private String form(Model model, ConcertForm form, Concert concert, Integer reserved) {
        model.addAttribute("form", form);
        model.addAttribute("concert", concert); // 수정이면 기존 공연, 등록이면 null
        model.addAttribute("reservedSeats", reserved == null ? 0 : reserved);
        model.addAttribute("categories", Category.values());
        return "concertForm";
    }
}
