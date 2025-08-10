/**
 * 
 */
package com.mtsl.mail.utility.service.impl;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mtsl.mail.utility.config.MailConfiguration;
import com.mtsl.mail.utility.dao.CommonDao;
import com.mtsl.mail.utility.dto.MailDTO;
import com.mtsl.mail.utility.dto.SystemConfigurationVO;
import com.mtsl.mail.utility.security.MordenAuthentication;
import com.mtsl.mail.utility.service.AttachementExtractor;
import com.mtsl.mail.utility.service.MailReaderService;

import lombok.RequiredArgsConstructor;

/**
 * @author ajay.kumar10
 *
 */
@Service
@RequiredArgsConstructor
public class MailReaderServiceImpl implements MailReaderService {

	@Value("${email.temp.folder.location}")
	private String emailTempFolderLocation;

	@Value("${ocr.source.path}")
	private String ocrSourcePath;

	@Value("${bpo.certralized.repository.path}")
	private String bpoCetralizedRepoPath;

	private final CommonDao commonDao;

	private final MordenAuthentication mordenAuthentication;

	private final MailConfiguration mailConfiguration;

	private final AttachementExtractor attachementExtractor;

	private Properties properties = (Properties) System.getProperties().clone();

	String bpoEmailTempLocationAtHTTPServer = "";

	Session session = null;
	Store emailStore = null;
	Folder emailFolder = null;
	Message message = null;

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(MailReaderServiceImpl.class);

	@Override
	public int readEmail() {
		try {

			systemConfigurationSetUpFromDatabase();
			setUpMailProtocols(mailConfiguration.getEmail());
			String token = mordenAuthentication.getAuthToken(mailConfiguration.getTanantId(),
					mailConfiguration.getClientId(), mailConfiguration.getClientSecret());

			session = Session.getInstance(properties);
			session.setDebug(false);
			emailStore = session.getStore("imap");

			emailStore.connect(mailConfiguration.getHost(), mailConfiguration.getEmail(), token);

			emailFolder = emailStore.getFolder(mailConfiguration.getFolder());
			emailFolder.open(Folder.READ_WRITE);

			MailDTO mailDTO =null;
			mailDTO =commonDao.getGenericDatafromDB(mailConfiguration.getEmail());

			if(mailDTO==null) {
				logger.error("Data base values are not configured for User {}",mailConfiguration.getEmail());
			}else {
			mailDTO.setEmailFolder(emailFolder);
			mailDTO.setBpoEmailTempLocationAtHTTPServer(emailTempFolderLocation);
			mailDTO.setBpoCentralizedRepositoryArchive(bpoCetralizedRepoPath);			
			mailDTO.setOcrUnStructuredSourcePath(ocrSourcePath);
			
			attachementExtractor.extractAttachments(mailDTO);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}

		return 0;
	}


	private void systemConfigurationSetUpFromDatabase() {

		SystemConfigurationVO emailConfig = commonDao.getSystemConfigurationByConfigKey("EMAIL_TEMP_FOLDER_LOCATION");

		SystemConfigurationVO ocrSourcePathConfig = commonDao
				.getSystemConfigurationByConfigKey("OCR_UnStructured_Source_Path");

		SystemConfigurationVO bpoCentralizedRepositoryPathConfig = commonDao
				.getSystemConfigurationByConfigKey("BPO_Centralized_Repository_Archive");

		if (emailConfig != null && emailConfig.getConfigValue() != null) {
			emailTempFolderLocation = emailConfig.getConfigValue();
		} else {
			logger.warn("EMAIL_TEMP_FOLDER_LOCATION not found in Database system configuration");
		}

		if (ocrSourcePathConfig != null && ocrSourcePathConfig.getConfigValue() != null) {
			ocrSourcePath = ocrSourcePathConfig.getConfigValue();
		} else {
			logger.warn("OCR_UnStructured_Source_Path not found in Database system configuration");
		}

		if (bpoCentralizedRepositoryPathConfig != null && bpoCentralizedRepositoryPathConfig.getConfigValue() != null) {
			bpoCetralizedRepoPath = bpoCentralizedRepositoryPathConfig.getConfigValue();
		} else {
			logger.warn("BPO_Centralized_Repository_Archive not found in Database system configuration");
		}
	}

	private void setUpMailProtocols(String mailAddress) {
		System.setProperty("https.protocols", "TLSv1.2");
		System.setProperty("mail.imap.ssl.protocols", "TLSv1.2");
		properties.put("mail.store.protocol", "imap");
		properties.put("mail.imap.host", mailConfiguration.getHost());
		properties.put("mail.imap.port", mailConfiguration.getPort());
		properties.put("mail.imap.ssl.enable", "true");
		properties.put("mail.imap.starttls.enable", "true");
		properties.put("mail.imap.auth.mechanisms", "XOAUTH2");
		properties.put("mail.imap.auth", "true");
		properties.put("mail.imap.user", mailAddress);
		properties.put("mail.debug", "false");
	}

}
