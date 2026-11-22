package com.yuhancon.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yuhancon.config.SecurityConfig;
import com.yuhancon.domain.Member;
import com.yuhancon.domain.Role;
import com.yuhancon.service.MemberService;
import com.yuhancon.service.MemberService.FieldException;
import com.yuhancon.web.MemberEditForm;
import com.yuhancon.web.SignupForm;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/login")
    public String login(HttpSession session, Model model) {
        // 로그인에 실패했으면 입력했던 아이디를 다시 채운다(한 번만)
        Object last = session.getAttribute(SecurityConfig.LAST_LOGIN_ID);
        session.removeAttribute(SecurityConfig.LAST_LOGIN_ID);
        model.addAttribute("lastLoginId", last == null ? "" : last);
        return "login";
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("form", new SignupForm());
        model.addAttribute("roles", new Role[] { Role.USER, Role.PERFORMER });
        return "signup";
    }

    // 아이디 중복 확인(가입 화면의 '중복 확인' 버튼)
    @GetMapping("/signup/check-id")
    @ResponseBody
    public Map<String, Object> checkId(@RequestParam(defaultValue = "") String loginId) {
        String error = memberService.checkLoginId(loginId);
        return Map.of("available", error == null, "message", error == null ? "사용할 수 있는 아이디입니다." : error);
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("form") SignupForm form, BindingResult result, Model model) {
        model.addAttribute("roles", new Role[] { Role.USER, Role.PERFORMER });
        if (result.hasErrors()) return "signup";
        try {
            memberService.signup(form);
        } catch (FieldException e) {
            result.rejectValue(e.getField(), "invalid", e.getMessage());
            return "signup";
        } catch (DataIntegrityViolationException e) {
            result.rejectValue("loginId", "duplicate", "방금 같은 아이디나 이메일로 가입한 사람이 있습니다. 다시 확인해 주세요.");
            return "signup";
        }
        return "redirect:/login?signup";
    }

    @GetMapping("/editMember")
    public String editForm(Principal principal, Model model) {
        Member m = memberService.get(principal.getName());
        MemberEditForm form = new MemberEditForm();
        form.setName(m.getName());
        form.setPhone(m.getPhone());
        model.addAttribute("form", form);
        return "editMember";
    }

    @PostMapping("/editMember")
    public String edit(@Valid @ModelAttribute("form") MemberEditForm form, BindingResult result,
                       Principal principal, RedirectAttributes ra) {
        if (result.hasErrors()) return "editMember";
        try {
            memberService.update(principal.getName(), form);
        } catch (FieldException e) {
            result.rejectValue(e.getField(), "invalid", e.getMessage());
            return "editMember";
        }
        ra.addFlashAttribute("successMessage", "회원 정보를 수정했습니다.");
        return "redirect:/mypage";
    }
}
