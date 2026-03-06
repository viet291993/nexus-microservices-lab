package com.nexus.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.server.EnableConfigServer;

import com.nexus.configserver.config.SecurityProperties;

@SpringBootApplication
@EnableConfigServer
@EnableConfigurationProperties(SecurityProperties.class)
public class ConfigServerApplication {

	/**
	 * Bootstrap the Spring Boot application and initialize the Spring Cloud Config Server.
	 *
	 * @param args command-line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
	}

}
