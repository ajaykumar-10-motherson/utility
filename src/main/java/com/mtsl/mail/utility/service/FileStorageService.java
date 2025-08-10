package com.mtsl.mail.utility.service;

import java.io.IOException;

import javax.mail.MessagingException;

import com.mtsl.mail.utility.dto.MailDTO;

/**
 * @author ajay.kumar10
 * Store extracted attachments to network location
 */
public interface FileStorageService {

	boolean downLoadAttachment(MailDTO mailDTO)throws IOException, MessagingException;
	
}
