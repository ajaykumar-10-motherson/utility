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
import com.mtsl.mail.utility.service.MarkMailAsUnread;

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
	private final MarkMailAsUnread markMailAsUnread;  

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
		int limit = messages.length;
		boolean result =false;

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
				emailLogDTO.setBuId(mailDTO.getBuId());
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

					if (fileName != null && !fileName.isBlank()) {
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
								mailBodyContent.append(mailBody);

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
						mailDTO.setObjRef(objRef);
					}

					auditService.updateMailBodyNData(generatedEmailId, emailLogDTO);

					
					try {
						auditService.insertIntoEmailLogDetails(uploadedFileList, generatedEmailId);
						mailDTO.setMessage(message);
						mailDTO.setGeneratedEmailId(generatedEmailId);

						boolean isSuccess = fileStorageService.downLoadAttachment(mailDTO);

						if (isSuccess) {
							result=true;
							SendMailDTO sendMailDTO = new SendMailDTO();
							sendMailDTO.setCommonEmailId("ajay.kumar10@Motherson.com");
							sendMailDTO.setMailTo("ajay.kumar10@Motherson.com");
							sendMailDTO.setCompanyBu("1");
							sendMailDTO.setEmaliLogId(generatedEmailId);

							/*
							 * Set the following before service call... String commonEmailId, String
							 * companyBu, long emaliLogId, String mailTo
							 */
							mailSenderService.doSendMailForEmailLog(sendMailDTO);
						}
					}

					catch (SQLException e) {
						markMailAsUnread.markMailAsUnread(mailDTO.getEmailFolder(), message);
						e.printStackTrace();
					}

				}
			}
		}

		return result;
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

	
}
