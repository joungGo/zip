package com.example.websockettest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WebSocketTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebSocketTestApplication.class, args);
	}

}
