package com.yuhancon.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yuhancon.domain.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

}
