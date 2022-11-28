package com.aidyn.slideshow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidyn.slideshow.dto.ResponseObject;
import com.aidyn.slideshow.service.SlideService;

@RestController
public class SlideController {
	@Autowired
	private SlideService service;

	@GetMapping("/hello")
	public String getImage() {
		return "Hello World!";
	}

	@GetMapping("/next")
	@CrossOrigin(origins = "http://localhost:3000")
	public ResponseObject getNextImage(@RequestParam String code) {
		return service.getNextImage(code);
	}
}
