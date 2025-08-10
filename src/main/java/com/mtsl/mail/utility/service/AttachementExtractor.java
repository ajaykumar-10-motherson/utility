package com.mtsl.mail.utility.service;

import java.io.IOException;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import com.mtsl.mail.utility.dto.MailDTO;

public interface AttachementExtractor{
	
	public boolean extractAttachments(MailDTO mailDTO) throws IOException, MessagingException;
	
}