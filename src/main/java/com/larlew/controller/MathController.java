package com.larlew.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Math", description = "Mathematical operations API")
public class MathController {

	private static final Logger logger = LoggerFactory.getLogger(MathController.class);

	@Operation(
		summary = "Calculate sum of two integers",
		description = "Adds two integer values and returns the result. This endpoint is publicly accessible without authentication."
	)
	@GetMapping("/math/sum")
	public int sum(
		@Parameter(description = "First integer value", example = "5") @RequestParam int a,
		@Parameter(description = "Second integer value", example = "10") @RequestParam int b
	) {
		logger.info("Calculating sum of {} and {}", a, b);
		return a + b;
	}
}
