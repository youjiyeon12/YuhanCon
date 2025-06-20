package com.yuhancon.controller;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.yuhancon.domain.Concert;
import com.yuhancon.repository.ConcertRepository;

@Controller
public class ConcertController {

    @Autowired
    private ConcertRepository concertRepository;

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
            @RequestParam("imageFile") MultipartFile imageFile
    ) {
        try {
            concert.setAvailableSeats(concert.getTotalSeats());

            if (!imageFile.isEmpty()) {
                String fileName = imageFile.getOriginalFilename();
                // 파일명 정제 (띄어쓰기, 한글, 특수문자 제거 → _ 대체)
                fileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");

                // 저장 경로
                String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/images";
                File saveFile = new File(uploadDir, fileName);
                saveFile.getParentFile().mkdirs();
                imageFile.transferTo(saveFile);

                // DB에 저장될 웹 접근 경로
                concert.setImage("/images/" + fileName);
            }

            concertRepository.save(concert);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/concertList";
    }

    // 공연 상세 보기
    @GetMapping("/concertDetail/{id}")
    public String showConcertDetail(@PathVariable Long id, Model model) {
        Concert concert = concertRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 공연이 없습니다. id=" + id));
        model.addAttribute("concert", concert);
        return "concertDetail"; // templates/concertDetail.html
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
    public String editConcert(@PathVariable Long id, @ModelAttribute Concert updatedConcert) {
        Concert concert = concertRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 공연이 없습니다."));

        concert.setTitle(updatedConcert.getTitle());
        concert.setContent(updatedConcert.getContent());
        concert.setPlace(updatedConcert.getPlace());
        concert.setDate(updatedConcert.getDate());
        concert.setTotalSeats(updatedConcert.getTotalSeats());
        concert.setAvailableSeats(updatedConcert.getAvailableSeats());
        concert.setImage(updatedConcert.getImage());

        concertRepository.save(concert);
        return "redirect:/concertinfo/" + id;
    }

    // 공연 삭제 처리
    @PostMapping("/concertDelete/{id}")
    public String deleteConcert(@PathVariable Long id) {
        concertRepository.deleteById(id);
        return "redirect:/concertList";
    }
}
