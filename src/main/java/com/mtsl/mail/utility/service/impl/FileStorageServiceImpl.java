package com.mtsl.mail.utility.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mtsl.mail.utility.dto.FileDetailsDTO;
import com.mtsl.mail.utility.dto.MailDTO;
import com.mtsl.mail.utility.service.AuditService;
import com.mtsl.mail.utility.service.FileStorageService;
import com.mtsl.mail.utility.service.MarkMailAsUnread;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService{
	
	@Value("${bpo.invalid.character}")
	private String invalidCharacter;
	
	@Value("${bpo.valid.extension}")
	private String validExtension;
	
	public static final String FILE_SEPERATOR = "\\";
	
	private final AuditService auditService;
	private final MarkMailAsUnread markMailAsUnread;  

	@Override
	public boolean downLoadAttachment(MailDTO fileAttachementDTO) throws IOException, MessagingException {

		Multipart multipart = null;
		String fileName = null;
		String strFileNameExtension = null;
		InputStream is = null;
		boolean isSuccess = true;
		Object objRef = fileAttachementDTO.getObjRef();
		if (objRef instanceof Multipart) {
			multipart = (Multipart) fileAttachementDTO.getMessage().getContent();
		}
		ArrayList<String> uploadFileData = new ArrayList<>();
		List<FileDetailsDTO> uploadedFileList = new ArrayList<>();
		FileDetailsDTO dto = null;
		File f = null;
		int totalFile = 0;
		int totalvalidFile = 0;	
		String[] invalidCharArray;
		String[] validExtensionArray;
		try {
			invalidCharArray = invalidCharacter.split(";");
			validExtensionArray = validExtension.split(";");
			List<String> validFileExt = Arrays.asList(validExtensionArray);
			// loop started to download all attachment
			if (multipart != null) {
				for (int i1 = 1; i1 < multipart.getCount(); i1++) {
					dto = new FileDetailsDTO();
					BodyPart bodyPart = multipart.getBodyPart(i1);
					fileName = bodyPart.getFileName();
					dto.setOriginalFileName(bodyPart.getFileName());
					if (fileName.isBlank()) {
						continue;
					}

					for (int j = 0; j < invalidCharArray.length; j++) {
						if (fileName.contains(invalidCharArray[j])) {
							fileName = fileName.replace(invalidCharArray[j], "");
						}
					}

					dto.setFileName(fileName);

					if (!fileName.isBlank()) {

						strFileNameExtension = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length())
								.trim();

						dto.setFileType(strFileNameExtension);

						if (validFileExt.contains(strFileNameExtension.toLowerCase())) {
							totalvalidFile++;
						}

						is = bodyPart.getInputStream();
						f = new File(fileAttachementDTO.getBpoEmailTempLocationAtHTTPServer()+ FILE_SEPERATOR + fileName);
						if (f.exists()) {
							f.delete();
						}
						try (FileOutputStream fos = new FileOutputStream(f)) {
							byte[] buf = new byte[4096];
							int bytesRead;
							while ((bytesRead = is.read(buf)) != -1) {
								fos.write(buf, 0, bytesRead);
							}
						}
						uploadFileData.add(fileName);
						uploadedFileList.add(dto);
						totalFile++;
					}
				}
			}

			isSuccess = uploadFile(fileAttachementDTO,uploadFileData, totalFile, totalvalidFile, uploadedFileList);

		} catch (Exception ex) {
			markMailAsUnread.markMailAsUnread(fileAttachementDTO.getEmailFolder(), fileAttachementDTO.getMessage());
			isSuccess = false;
		} finally {
			if (is != null)
				is.close();

		}
		return isSuccess;
	
	}
	
	private boolean uploadFile(MailDTO fileAttachementDTO, ArrayList<String> uploadFileData, int totalFile,
			int totalvalidFile, List<FileDetailsDTO> uploadedFileList) {
		Folder emailFolder = fileAttachementDTO.getEmailFolder();
		Message message = fileAttachementDTO.getMessage();
		String companyId=fileAttachementDTO.getCompanyId();
		String companyName = fileAttachementDTO.getCompanyName();
		String buId = fileAttachementDTO.getBuId();
		String buName = fileAttachementDTO.getBuName();
		String buCode = fileAttachementDTO.getBuCode();
		Long generatedEmailId = fileAttachementDTO.getGeneratedEmailId();
		String bpoEmailTempLocationAtHTTPServer = fileAttachementDTO.getBpoEmailTempLocationAtHTTPServer();
		String ocrUnStructuredSourcePath = fileAttachementDTO.getOcrUnStructuredSourcePath();
		String bpoCentralizedRepositoryArchive = fileAttachementDTO.getBpoCentralizedRepositoryArchive();
		String ftpUploadLocation = fileAttachementDTO.getFtpUploadLocation();
		String defDocType = fileAttachementDTO.getDefDocType();
		
		ArrayList<String> uploadedAttachmentName = new ArrayList<>();
		String[] validExtensionArray;
		int fileUploadCount = 0;
		String batchId = null;
		boolean isSuccess = true;
		try{
			validExtensionArray = validExtension.split(";");
			batchId = auditService.getNewBatchIdForBulkUpload("WS", "504");

			for (int i = 0; i < uploadFileData.size(); i++) {

				FileDetailsDTO dto = uploadedFileList.get(i);

				String fileExt = dto.getFileType();

				Long fileSize = 0l;

				String fileNameWithOutExt = FilenameUtils.removeExtension(dto.getFileName());
				String docType = defDocType;
				String docId = defDocType;
				String userId = "1";
				String userName = "";
				String companyIdWithPrefix;
				String buIdWithPrefix;
				String docIdWithPrefix;
				String userIdWithPrefix;
				String uploadedFiles;
				String uploadedFileStatus = null;
				String fileNameWithoutSpecialChar = "";

				fileNameWithoutSpecialChar = dto.getFileName();

				String newFileName = "";

				companyIdWithPrefix = "000".substring(0, "000".length() - companyId.length()) + companyId;

				buIdWithPrefix = "000".substring(0, "000".length() - buId.length()) + buId;

				docIdWithPrefix = "00000".substring(0, "00000".length() - docId.length()) + docId;

				userIdWithPrefix = "000000".substring(0, "000000".length() - userId.length()) + userId;

				newFileName = buIdWithPrefix + "_" + docIdWithPrefix + "_" + userIdWithPrefix + "_" + fileNameWithOutExt
						+ "_" + batchId + "_email_bulk" + "." + fileExt;

				String newFileNameWithoutUserIdAndBulk = buIdWithPrefix + "_" + docIdWithPrefix + "_"
						+ fileNameWithoutSpecialChar;

				File tempFileWithLocation = new File(
						bpoEmailTempLocationAtHTTPServer + FILE_SEPERATOR + fileNameWithoutSpecialChar);

				fileSize = tempFileWithLocation.length();

				uploadedFiles = newFileName + ":" + newFileNameWithoutUserIdAndBulk + ":" + "." + fileExt + ":"
						+ fileNameWithoutSpecialChar + ":" + fileSize;

				long generatedFileId = 0;

				List<String> extList = Arrays.asList(validExtensionArray);

				if (extList.contains(fileExt.toLowerCase())) {
					generatedFileId = auditService.insertIntoInvoiceQueue(newFileName, fileExt, fileSize, buId,
							buCode, buName, companyId, companyName, docType, generatedEmailId);
				}

				if (generatedFileId > 0l) {
					uploadedFileStatus = "1";
				} else {
					uploadedFileStatus = "0";
				}

				auditService.updateEmailLogDetailsEntry(dto.getOriginalFileName(), dto.getFileName(),
						uploadedFileStatus, Long.toString(generatedFileId),
						Long.toString(generatedEmailId));

				auditService.doLogSelectedFileForUpload(buId, companyName, buName, batchId, uploadedFiles,
						userName, Long.parseLong(userId), "IpAddress",
						"4194304", 10, "IpAddress", "0", "0", uploadFileData.get(i));
				if (extList.contains(fileExt.toLowerCase())) {
					fileUploadCount++;
				}

				if (generatedFileId == 0) {

					uploadedFileStatus = "0";

					auditService.updateEmailLogDetailsEntry(dto.getOriginalFileName(), dto.getFileName(),
							uploadedFileStatus, Long.toString(generatedFileId),
							Long.toString(generatedEmailId));
				}

				Date todayDate = new Date();
				String yyyymm = new SimpleDateFormat("yyyyMM").format(todayDate);
				String phyficalPathForNewFileStr = ftpUploadLocation + FILE_SEPERATOR + companyIdWithPrefix + "_"
						+ buIdWithPrefix + FILE_SEPERATOR + yyyymm;
				File phyficalPathForNewFile = new File(phyficalPathForNewFileStr);

				if (!phyficalPathForNewFile.exists()) {
					phyficalPathForNewFile.mkdirs();
				}

				// generatedFileId as file name
				String renamedFile = FILE_SEPERATOR + generatedFileId + "." + fileExt;
				if (tempFileWithLocation.exists()) {

					if ("PDF".equalsIgnoreCase(fileExt) || "TIF".equalsIgnoreCase(fileExt)
							|| "TIFF".equalsIgnoreCase(fileExt)) {
						copyFileToOCREngine(emailFolder, message, ocrUnStructuredSourcePath, tempFileWithLocation,
								renamedFile);
					}

					moveFileToFileServer(emailFolder, message, tempFileWithLocation, phyficalPathForNewFile,
							renamedFile);

					moveFileToCentralizedArchiveFolder(emailFolder, message, bpoCentralizedRepositoryArchive,
							fileNameWithoutSpecialChar, tempFileWithLocation);

				}
				uploadedAttachmentName.add(uploadFileData.get(i));

			}

		} catch (Exception exception) {
			isSuccess = false;
			markMailAsUnread.markMailAsUnread(emailFolder, message);

		} finally {
			auditService.updateMailLogDetail(generatedEmailId, uploadedAttachmentName, fileUploadCount, totalFile);
		}
		return isSuccess;
	}

	
	private void moveFileToFileServer(Folder emailFolder, Message message, File tempFileWithLocation,
			File phyficalPathForNewFile, String renamedFile) {
		try {
			FileUtils.copyFile(tempFileWithLocation, new File(phyficalPathForNewFile + FILE_SEPERATOR + renamedFile));
		} catch (IOException ioe) {
			markMailAsUnread.markMailAsUnread(emailFolder, message);
		}
	}
	
	private  void moveFileToCentralizedArchiveFolder(Folder emailFolder, Message message,
			String bpoCentralizedRepositoryArchive, String fileNameWithoutSpecialChar, File tempFileWithLocation)
			throws MessagingException {
		try {
			FileUtils.copyFile(tempFileWithLocation,
					new File(bpoCentralizedRepositoryArchive + FILE_SEPERATOR + fileNameWithoutSpecialChar));

		} catch (IOException ioe) {
			emailFolder.setFlags(new Message[] { message }, new Flags(Flags.Flag.SEEN), false);

		}
	}
	
	private  void copyFileToOCREngine(Folder emailFolder, Message message, String ocrUnStructuredSourcePath,
			File tempFileWithLocation, String renamedFile) {
		try {
			// FOR OCR
			FileUtils.copyFile(tempFileWithLocation, new File(ocrUnStructuredSourcePath + renamedFile));
		} catch (IOException ioe) {
			markMailAsUnread.markMailAsUnread(emailFolder, message);
		}
	}

}
