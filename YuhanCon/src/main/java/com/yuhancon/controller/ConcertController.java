package com.yuhancon.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.yuhancon.domain.Concert;
import com.yuhancon.domain.Member;
import com.yuhancon.domain.RecentConcertView;
import com.yuhancon.repository.ConcertRepository;
import com.yuhancon.repository.MemberRepository;
import com.yuhancon.repository.RecentConcertViewRepository;
import com.yuhancon.repository.ReserveRepository;

import jakarta.transaction.Transactional;

@Controller
public class ConcertController {

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private ReserveRepository reserveRepository;
    
    @Autowired
    private RecentConcertViewRepository recentConcertViewRepository;

    @Autowired
    private MemberRepository memberRepository;
    
    // 이미지 파일 저장 메서드
    public String saveImage(MultipartFile imageFile) throws IOException {
        if (imageFile.isEmpty()) return null;

        String originalName = imageFile.getOriginalFilename();
        String extension = originalName.substring(originalName.lastIndexOf("."));
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;
        fileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");

        // 실시간 반영 가능한 외부 폴더
        String uploadDir = System.getProperty("user.dir") + "/uploaded-images";
        File saveFile = new File(uploadDir, fileName);
        
        if (!saveFile.getParentFile().exists()) {
            saveFile.getParentFile().mkdirs();
        }

        imageFile.transferTo(saveFile);

        // 경로 리턴은 브라우저가 접근할 수 있게
        return "/uploaded-images/" + fileName;
    }


    // 공연 목록
    @GetMapping("/concertList")
    public String showConcertList(Model model) {
        model.addAttribute("concertList", concertRepository.findAll());
        return "concertList";
    }

    // 공연 등록 폼
    @GetMapping("/concertWrite")
    public String showConcertForm(Model model) {
        model.addAttribute("concert", new Concert());
        return "concertWrite";
    }

    // 공연 저장 처리
    @PostMapping("/concert/save")
    public String saveConcert(
            @ModelAttribute Concert concert,
            @RequestParam("imageFile") MultipartFile imageFile,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            concert.setAvailableSeats(concert.getTotalSeats());

            if (userDetails != null) {
                Member member = memberRepository.findByEmail(userDetails.getUsername())
                        .orElseThrow();
                concert.setMember(member);
            }

            String savedPath = saveImage(imageFile);
            if (savedPath != null) {
                concert.setImage(savedPath);
            }

            concertRepository.save(concert);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/concertList";
    }


    // 공연 상세 보기
    @GetMapping("/concertDetail/{id}")
    @Transactional
    public String showConcertDetail(@PathVariable Long id,
                                    Model model,
                                    @AuthenticationPrincipal UserDetails userDetails) {

        Concert concert = concertRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 공연이 없습니다. id=" + id));
        model.addAttribute("concert", concert);

        boolean isOwner = false;

        if (userDetails != null && concert.getMember() != null) {
            String loginEmail = userDetails.getUsername();
            String writerEmail = concert.getMember().getEmail();
            isOwner = loginEmail.equals(writerEmail);
        }

        model.addAttribute("isOwner", isOwner);
        
        // 로그인된 사용자만 기록
        if (userDetails != null) {
            Member member = memberRepository.findByEmail(userDetails.getUsername()).orElseThrow();

            // 중복 저장 방지: 동일 공연 이미 본 기록 있으면 삭제
            recentConcertViewRepository.deleteByMemberAndConcert(member, concert);

            // 새로운 최근본 기록 저장
            RecentConcertView view = new RecentConcertView();
            view.setMember(member);
            view.setConcert(concert);
            view.setViewedAt(LocalDateTime.now());
            recentConcertViewRepository.save(view);
        }

        return "concertDetail";
    }


    // 공연 수정 폼
    @GetMapping("/concertEdit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Concert concert = concertRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 공연이 없습니다."));
        model.addAttribute("concert", concert);
        return "concertEdit";
    }

    // 공연 수정 처리
    @PostMapping("/concertEdit/{id}")
    public String editConcert(
            @PathVariable Long id,
            @ModelAttribute Concert updatedConcert,
            @RequestParam("imageFile") MultipartFile imageFile
    ) {
        Concert concert = concertRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 공연이 없습니다."));

        concert.setTitle(updatedConcert.getTitle());
        concert.setContent(updatedConcert.getContent());
        concert.setDate(updatedConcert.getDate());
        concert.setPlace(updatedConcert.getPlace());
        concert.setTotalSeats(updatedConcert.getTotalSeats());
        concert.setAvailableSeats(updatedConcert.getAvailableSeats());

        try {
            String savedPath = saveImage(imageFile);
            if (savedPath != null) {
                concert.setImage(savedPath); // 새 이미지 업로드 시에만 덮어씀
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        concertRepository.save(concert);
        return "redirect:/concertList";
    }

    // 공연 삭제 처리
    @PostMapping("/concertDelete/{id}")
    @Transactional
    public String deleteConcert(@PathVariable Long id) {
    	
    	recentConcertViewRepository.deleteByConcertId(id);
    	reserveRepository.deleteByConcertId(id);
    	concertRepository.deleteById(id);
        return "redirect:/concertList";
    }
}
