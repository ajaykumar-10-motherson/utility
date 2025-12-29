package com.mtsl.mail.utility.scheduler.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mtsl.mail.utility.scheduler.MailJobSchedular;
import com.mtsl.mail.utility.service.MailReaderService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MailJobSchedularImpl implements MailJobSchedular {

	@Value("${bpo.mail.service.enable}")
	private String mailServiceEnable;

	private int readMailRunningStatus = 0;

	private final MailReaderService receiveMail;

	@Override
	@Scheduled(cron = "${scheduler.cron.readMail.occrence}")
	public void readMail() {

		if ("true".equalsIgnoreCase(mailServiceEnable) && readMailRunningStatus == 0) {
			readMailRunningStatus = 1;

			readMailRunningStatus=receiveMail.readEmail();

			readMailRunningStatus = 0;
		}

	}

	@Override
	public void sendMail() {
   //sendMail
 }

}
