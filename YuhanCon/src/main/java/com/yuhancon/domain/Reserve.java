package com.yuhancon.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 한 회원은 한 공연에 예매 하나만 가진다(동시에 두 번 눌러도 DB가 막는다). 수정은 이 예매를 고친다.
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "concert_id"}))
@Getter @Setter @NoArgsConstructor
public class Reserve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    private int seatCount;

    @Column(length = 200)
    private String seats; // 좌석 지정 공연이면 "A1,A2", 아니면 null

    private LocalDateTime reservedAt;

    public String getTicketNo() { return String.format("YC-%06d", id); }
}
