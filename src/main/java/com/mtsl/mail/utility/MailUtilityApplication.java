package com.mtsl.mail.utility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@PropertySource("file:C:\\mail\\config\\application.properties")
public class MailUtilityApplication {

	public static void main(String[] args) {
		SpringApplication.run(MailUtilityApplication.class, args);
	}

}
