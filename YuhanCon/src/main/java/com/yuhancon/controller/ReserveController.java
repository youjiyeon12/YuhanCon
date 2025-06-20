package com.yuhancon.controller;

import java.time.LocalDate;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.yuhancon.domain.Concert;
import com.yuhancon.domain.Member;
import com.yuhancon.domain.Reserve;
import com.yuhancon.repository.ConcertRepository;
import com.yuhancon.repository.ReserveRepository;
import com.yuhancon.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ReserveController {

    private final ReserveRepository reserveRepository;
    private final ConcertRepository concertRepository;

    @PostMapping("/reserve")
    public String reserve(
        @RequestParam Long concertId,
        @RequestParam int seatCount,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Member member = userDetails.getMember();
        Concert concert = concertRepository.findById(concertId).orElseThrow();

        if (concert.getAvailableSeats() < seatCount) {
            throw new IllegalArgumentException("남은 좌석이 부족합니다.");
        }

        concert.setAvailableSeats(concert.getAvailableSeats() - seatCount);

        Reserve reserve = new Reserve();
        reserve.setMember(member);
        reserve.setConcert(concert);
        reserve.setSeatCount(seatCount);
        reserve.setReservedAt(LocalDate.now());

        reserveRepository.save(reserve);
        concertRepository.save(concert);

        return "redirect:/concertList";
    }
}

