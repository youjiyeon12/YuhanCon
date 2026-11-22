package com.yuhancon.domain;

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

// 좌석 하나를 한 행으로 저장한다. (공연, 좌석) 유일 제약이 같은 자리를 두 사람이 동시에 잡는 것을 막는다.
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"concert_id", "label"}))
@Getter @Setter @NoArgsConstructor
public class ReservedSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserve_id", nullable = false)
    private Reserve reserve;

    @Column(nullable = false, length = 10)
    private String label;
}
