package com.yuhancon.domain;

import org.springframework.data.annotation.Id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Data;

@Entity
@Data
public class Member {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(nullable=false, unique = true)
	private String email;
	@Column(nullable=false)
	private String password;
	@Column(nullable=false)
	private String name;
	@Column(nullable=false)
	private String phone;
	@Column(nullable=false)
	private String role = "USER";

}
