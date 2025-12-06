package com.larlew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LarlewBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LarlewBackendApplication.class, args);
		System.out.println("Larlew Backend Service is running...");
	}

}
