package com.mtsl.mail.utility.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * @author ajay.kumar10
 * This IMAP configuration.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mail.imap")
public class MailConfiguration {
	private String host;
	private int port;
	private String tanantId;
	private String clientId;
	private String clientSecret;
	private String email;
	private String folder;

}
