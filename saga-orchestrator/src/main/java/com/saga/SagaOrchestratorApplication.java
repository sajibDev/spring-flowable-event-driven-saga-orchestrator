package com.saga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.saga", "com.saga.config"})
public class SagaOrchestratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(SagaOrchestratorApplication.class, args);
		System.out.println("Saga Orchestrator Application Started");

	}

}
