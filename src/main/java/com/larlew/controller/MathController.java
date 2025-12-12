package com.larlew.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Math", description = "Mathematical operations API")
public class MathController {

	private static final Logger logger = LoggerFactory.getLogger(MathController.class);

	@Operation(
		summary = "Calculate sum of two integers",
		description = "Adds two integer values and returns the result."
	)
	@GetMapping(value = "/math/sum", produces = "application/json")
	public ResponseEntity<String> sum(
		@Parameter(description = "First integer value", example = "5") @RequestParam int a,
		@Parameter(description = "Second integer value", example = "10") @RequestParam int b
	) {
		logger.info("Calculating sum of {} and {}", a, b);
		int result = a + b;
		return ResponseEntity.ok(String.valueOf(result));
	}

	@Operation(
		summary = "Calculate product of two integers",
		description = "Multiplies two integer values and returns the result."
	)
	@GetMapping(value = "/math/product", produces = "application/json")
	public ResponseEntity<String> product(
		@Parameter(description = "First integer value", example = "5") @RequestParam int a,
		@Parameter(description = "Second integer value", example = "10") @RequestParam int b
	) {
		logger.info("Calculating product of {} and {}", a, b);
		int result = a * b;
		return ResponseEntity.ok(String.valueOf(result));
	}

	@Operation(
		summary = "Calculate difference of two integers",
		description = "Subtracts the second integer value from the first and returns the result."
	)
	@GetMapping(value = "/math/difference", produces = "application/json")
	public ResponseEntity<String> difference(
		@Parameter(description = "First integer value", example = "10") @RequestParam int a,
		@Parameter(description = "Second integer value", example = "5") @RequestParam int b
	) {
		logger.info("Calculating difference of {} and {}", a, b);
		int result = a - b;
		return ResponseEntity.ok(String.valueOf(result));
	}

	@Operation(
		summary = "Calculate quotient of two integers",
		description = "Divides the first integer value by the second and returns the result."
	)
	@GetMapping(value = "/math/quotient", produces = "application/json")
	public ResponseEntity<String> quotient(
		@Parameter(description = "First integer value", example = "10") @RequestParam int a,
		@Parameter(description = "Second integer value", example = "5") @RequestParam int b
	) {
		logger.info("Calculating quotient of {} and {}", a, b);
		if (b == 0) {
			return ResponseEntity.badRequest().body("Division by zero is not allowed.");
		}
		int result = a / b;
		return ResponseEntity.ok(String.valueOf(result));
	}
}