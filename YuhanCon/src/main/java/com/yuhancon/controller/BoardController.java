package com.yuhancon.controller;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.yuhancon.domain.Post;
import com.yuhancon.repository.PostRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BoardController {
	
		private final PostRepository postRepository;
		
	 	//글쓰기
	 	@GetMapping("/write")
	    public String writeForm(Model model) {
	        model.addAttribute("post", new Post());
	        return "BoardWrite"; 
	    }
	 	
	 	//폼에서 입력한 데이터 받아옴
	    @PostMapping("/write")
	    public String writeSubmit(@ModelAttribute Post post) { //@ModelAttribute html에서 입력한 데이터가 자동으로 넘어옴
	    	
	    	
	        post.setBoardAt(LocalDate.now());   // 작성일임
	        post.setCnt(0);                     // 조회수임
	        post.setImage(null);               // 일단 null 값으로 해놨음 

	        // 멤버 로그인 연동이 필요하므로 지금은  나중에 연동
	        // post.setMember(loggedInMember); 

	        postRepository.save(post);
	        return "redirect:/list";
	    }

	    @GetMapping("/list")
	    public String list(Model model) {
	        model.addAttribute("postList", postRepository.findAll());
	        return "BoardList";
	    }

	    @GetMapping("/detail/{id}")
	    public String detail(@PathVariable Long id, Model model) {
	        Post post = postRepository.findById(id).orElseThrow();
	        model.addAttribute("post", post);
	        return "BoardDetail";
	    }
	    
	    

	
}
