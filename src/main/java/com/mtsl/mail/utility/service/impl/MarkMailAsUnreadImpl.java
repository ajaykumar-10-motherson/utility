package com.mtsl.mail.utility.service.impl;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mtsl.mail.utility.service.AuditService;
import com.mtsl.mail.utility.service.MarkMailAsUnread;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarkMailAsUnreadImpl implements MarkMailAsUnread {

	private final AuditService auditService;
	
	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(MarkMailAsUnreadImpl.class);
	/**
	 * method will set the flag as SEEN if any exception is occurred for 3 times.
	 * 
	 * @param emailFolder
	 * @param message
	 */
	@Override
	public void markMailAsUnread(Folder emailFolder, Message message) {
		try {
			int failCount = auditService.updateMailUploadLogFailCount(message);
			if (failCount < 3) { // make this count also configurable...
				emailFolder.setFlags(new Message[] { message }, new Flags(Flags.Flag.SEEN), false);
			}
			logger.warn("fail count is ::{} of message::{} ", failCount, message);
		} catch (MessagingException e1) {
			logger.error(e1.toString());
		}
	}

}
