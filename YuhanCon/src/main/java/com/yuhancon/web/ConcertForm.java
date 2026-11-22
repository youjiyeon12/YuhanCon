package com.yuhancon.web;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.yuhancon.domain.Category;
import com.yuhancon.domain.Concert;

import lombok.Getter;
import lombok.Setter;

// 공연 등록·수정 폼. 검사는 ConcertService.validate가 한다.
@Getter @Setter
public class ConcertForm {
    private String title;
    private String content;
    private String place;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    private Category category = Category.CONCERT;
    private String categoryEtc;
    private boolean seated;             // 좌석 지정 공연 여부
    private List<String> rowNames = new ArrayList<>();
    private List<Integer> rowCounts = new ArrayList<>();
    private Integer totalSeats;         // 자유석·스탠딩일 때 인원

    public static ConcertForm of(Concert c) {
        ConcertForm f = new ConcertForm();
        f.title = c.getTitle();
        f.content = c.getContent();
        f.place = c.getPlace();
        f.date = c.getDate();
        f.category = c.getCategory();
        f.categoryEtc = c.getCategoryEtc();
        f.seated = c.isSeated();
        c.getSeatRows().forEach((row, n) -> { f.rowNames.add(row); f.rowCounts.add(n); });
        f.totalSeats = c.getTotalSeats();
        return f;
    }
}
