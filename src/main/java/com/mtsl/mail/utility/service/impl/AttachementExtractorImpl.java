/**
 * 
 */
package com.mtsl.mail.utility.service.impl;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.search.FlagTerm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mtsl.mail.utility.dto.EmailLogDTO;
import com.mtsl.mail.utility.dto.FileDetailsDTO;
import com.mtsl.mail.utility.dto.MailDTO;
import com.mtsl.mail.utility.dto.SendMailDTO;
import com.mtsl.mail.utility.service.AttachementExtractor;
import com.mtsl.mail.utility.service.AuditService;
import com.mtsl.mail.utility.service.FileStorageService;
import com.mtsl.mail.utility.service.MailSenderService;

import lombok.RequiredArgsConstructor;

/**
 * @author ajay.kumar10
 *
 */
@Service
@RequiredArgsConstructor
public class AttachementExtractorImpl implements AttachementExtractor {

	private final AuditService auditService;
	private final FileStorageService fileStorageService;
	private final MailSenderService mailSenderService;

	String emailReceivedFrom = "";
	EmailLogDTO emailLogDTO = null;
	Message message = null;
	List<FileDetailsDTO> uploadedFileList = null;
	Long generatedEmailId = 0l;

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(AttachementExtractorImpl.class);

	/**
	 * accepts MailDTO
	 */
	@Override
	public boolean extractAttachments(MailDTO mailDTO) throws IOException, MessagingException {

		Message[] messages = mailDTO.getEmailFolder().search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
		// for prod testing messages.length -> limit
		int limit = 2;

		for (int mailIteration = 0; mailIteration < limit; mailIteration++) {

			message = messages[mailIteration];
			Object content = message.getContent();
			uploadedFileList = new ArrayList<>();
			if (content instanceof Multipart) {

				Multipart mp = (Multipart) content;
				BodyPart bodyPart1 = null;
				String fileName = null;
				long fileSize;

				emailReceivedFrom = message.getFrom()[0].toString();
				emailLogDTO = new EmailLogDTO();
				emailLogDTO.setBuId("1");
				emailLogDTO.setMailFrom(emailReceivedFrom);
				emailLogDTO.setSubject(message.getSubject());
				emailLogDTO.setEntBy("1");
				emailLogDTO.setMailSize(message.getSize());
				emailLogDTO.setMailReceivedDate(message.getReceivedDate());
				emailLogDTO.setEmailNumber(message.getMessageNumber());

				generatedEmailId = auditService.insertIntoEmailLog(emailLogDTO, message, mailDTO.getEmailFolder());

				for (int i1 = 1; i1 < mp.getCount(); i1++) {
					FileDetailsDTO dto = new FileDetailsDTO();
					bodyPart1 = mp.getBodyPart(i1);
					fileName = bodyPart1.getFileName();

					fileSize = bodyPart1.getSize();

					String strFileNameExtension = "";

					if (!fileName.isBlank()) {
						strFileNameExtension = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length())
								.trim();
						dto.setFileName(fileName);
						dto.setFileType(strFileNameExtension);
						dto.setFileSize(fileSize);

						StringBuilder mailBodyContent = new StringBuilder();
						for (int ii = 0; ii < mp.getCount() - 1; ii++) {
							BodyPart bodyPart = mp.getBodyPart(ii);
							String mailBody = getText(bodyPart);
							if (mailBody != null && !mailBody.isBlank()) {
								mailBodyContent.append(mailBodyContent).append(mailBody);
							}
						}
						emailLogDTO.setEmailBody(mailBodyContent.toString());
						uploadedFileList.add(dto);
					}
				}

				if (emailLogDTO != null) {
					Object objRef = message.getContent();

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					if (objRef instanceof Multipart) {

						((Multipart) content).writeTo(baos);
						byte[] bytes = baos.toByteArray();
						emailLogDTO.setEmailData(bytes);
					}

					auditService.updateMailBodyNData(generatedEmailId, emailLogDTO);

					
					try {
						auditService.insertIntoEmailLogDetails(uploadedFileList, generatedEmailId);

						boolean isSuccess = fileStorageService.downLoadAttachment(mailDTO);

						if (isSuccess) {
							SendMailDTO sendMailDTO = new SendMailDTO();
							sendMailDTO.setCommonEmailId("");
							sendMailDTO.setMailTo("");
							sendMailDTO.setCompanyBu("");
							sendMailDTO.setEmaliLogId(generatedEmailId);

							/*
							 * Set the following before service call... String commonEmailId, String
							 * companyBu, long emaliLogId, String mailTo
							 */
							mailSenderService.doSendMailForEmailLog(sendMailDTO);
							return true;
						}
					}

					catch (SQLException e) {
						markMailAsUnread(mailDTO.getEmailFolder(), message);
						e.printStackTrace();
					}

				}
			}
		}

		return false;
	}

	/**
	 * @param p
	 * @return
	 * @throws MessagingException
	 * @throws IOException
	 */
	private String getText(Part part) throws MessagingException, IOException {
		if (part.isMimeType("text/*")) {
			Object content = part.getContent();

			if (content instanceof String) {
				return (String) content; // Normal case
			} else if (content instanceof InputStream) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream) content))) {
					return reader.lines().collect(Collectors.joining("\n"));
				}
			}
			return null;
		}
		if (part.isMimeType("multipart/alternative")) {
			Multipart mp = (Multipart) part.getContent();
			String text = null;
			for (int i = 0; i < mp.getCount(); i++) {
				Part bp = mp.getBodyPart(i);
				if (bp.isMimeType("text/plain")) {
					if (text == null)
						text = getText(bp);
					continue;
				} else if (bp.isMimeType("text/html")) {
					String s = getText(bp);
					if (s != null)
						return s;
				} else {
					return getText(bp);
				}
			}
			return text;
		} else if (part.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) part.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				String s = getText(mp.getBodyPart(i));
				if (s != null)
					return s;
			}
		}
		return null;
	}

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
