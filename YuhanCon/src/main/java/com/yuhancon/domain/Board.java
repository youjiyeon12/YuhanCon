package com.yuhancon.domain;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Board {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
    private String title;
    
    @ManyToOne(fetch = FetchType.LAZY) //다대일 관계 설정
    @JoinColumn(name = "member_id") //외래키 이름을 명시적으로 설정
    private Member member;
    
    private String content;
    
    private LocalDate boardAt;
    
    private int cnt = 0;
    
    private String image;
    

}
