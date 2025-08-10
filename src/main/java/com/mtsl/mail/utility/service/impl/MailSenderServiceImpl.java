package com.mtsl.mail.utility.service.impl;

import org.springframework.stereotype.Service;

import com.mtsl.mail.utility.dto.SendMailDTO;
import com.mtsl.mail.utility.service.MailSenderService;

/**
 * @author ajay.kumar10
 *
 */
@Service
public class MailSenderServiceImpl implements MailSenderService {

	@Override
	public boolean sendMail() {
		
		return false;
	}

	@Override
	public void doSendMailForEmailLog(SendMailDTO sendMailDTO) {


	}

}
