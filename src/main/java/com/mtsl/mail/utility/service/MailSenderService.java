package com.mtsl.mail.utility.service;

import com.mtsl.mail.utility.dto.SendMailDTO;

public interface MailSenderService {
	
	boolean sendMail();
	void doSendMailForEmailLog(SendMailDTO sendMailDTO);
	
}