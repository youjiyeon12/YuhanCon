package com.yuhancon.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 바뀐 칸만 UPDATE한다. 남은 좌석·좋아요 수는 전용 UPDATE 문이 바꾸므로 옛 값으로 덮어쓰지 않게.
@Entity
@DynamicUpdate
@Getter @Setter @NoArgsConstructor
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 100)
    private String place;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category = Category.CONCERT;

    @Column(length = 30)
    private String categoryEtc; // 분류가 '기타'일 때 직접 입력한 이름

    // 좌석 지정 공연이면 "A:10,B:20"처럼 열 이름과 좌석 수, 자유석·스탠딩이면 null
    @Column(length = 500)
    private String seatLayout;

    @Column(nullable = false)
    private int totalSeats;

    @Column(nullable = false)
    private int availableSeats;

    @Column(nullable = false, updatable = false) // ConcertRepository.addLikeCount로만 바꾼다
    private int likeCount;

    private String image;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConcertStatus status = ConcertStatus.PENDING;

    private Integer slideOrder; // 관리자가 홈 슬라이드로 고르면 1~6, 아니면 null

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public boolean isSeated() { return seatLayout != null && !seatLayout.isBlank(); }
    public boolean isApproved() { return status == ConcertStatus.APPROVED; }
    public boolean isPast() { return date.isBefore(LocalDate.now()); }
    public boolean isSoldOut() { return availableSeats <= 0; }

    public String getCategoryName() {
        return category == Category.ETC && categoryEtc != null && !categoryEtc.isBlank() ? categoryEtc : category.getLabel();
    }

    // "A:10,B:20" → {A=10, B=20}
    public Map<String, Integer> getSeatRows() {
        Map<String, Integer> rows = new LinkedHashMap<>();
        if (!isSeated()) return rows;
        for (String part : seatLayout.split(",")) {
            String[] kv = part.split(":");
            rows.put(kv[0], Integer.parseInt(kv[1]));
        }
        return rows;
    }

    // 좌석 이름 전체 목록(A1, A2, ...)
    public List<String> getSeatLabels() {
        List<String> labels = new ArrayList<>();
        getSeatRows().forEach((row, n) -> { for (int i = 1; i <= n; i++) labels.add(row + i); });
        return labels;
    }
}
