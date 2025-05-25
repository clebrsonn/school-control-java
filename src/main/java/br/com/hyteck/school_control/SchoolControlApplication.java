package br.com.hyteck.school_control;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for the School Control system.
 * This class initializes and runs the Spring Boot application.
 * It enables asynchronous processing and retry mechanisms.
 */
@SpringBootApplication
@EnableAsync // Enables Spring's asynchronous method execution capability.
@EnableRetry // Enables Spring's retry functionality for failed operations.
public class SchoolControlApplication {

	/**
	 * The main entry point for the School Control application.
	 *
	 * @param args Command-line arguments passed to the application.
	 */
	public static void main(String[] args) {
		// Launches the Spring Boot application.
		SpringApplication.run(SchoolControlApplication.class, args);
	}

}
