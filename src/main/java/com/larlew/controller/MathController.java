package com.larlew.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MathController {

    /** 
     * Endpoint to calculate the sum of two integers.
     * Accessible without authentication as per SecurityConfig.
     * 
     * Example: /math/sum?a=5&b=10 returns 15
     */
	@GetMapping("/math/sum")
	public int sum(@RequestParam int a, @RequestParam int b) {
		return a + b;
	}
}
