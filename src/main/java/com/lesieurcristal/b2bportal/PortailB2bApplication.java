package com.lesieurcristal.b2bportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PortailB2bApplication {

	public static void main(String[] args) {
		SpringApplication.run(PortailB2bApplication.class, args);
	}

}
