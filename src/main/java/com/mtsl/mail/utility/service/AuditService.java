package com.mtsl.mail.utility.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.Folder;
import javax.mail.Message;

import com.mtsl.mail.utility.dto.EmailLogDTO;
import com.mtsl.mail.utility.dto.FileDetailsDTO;

/**
 * @author ajay.kumar10 AuditService is for logging or persist audit
 *
 */
public interface AuditService {

	public Long insertIntoEmailLog(EmailLogDTO emailLogDTO, Message message, Folder emailFolder);

	public void updateMailBodyNData(Long generatedEmailId, EmailLogDTO emailLogFormBean);

	public Integer getMailUploadLogId(Message message);

	public Integer getMailUploadLogFailCount(Message message);

	public Integer updateMailUploadLogFailCount(Message message);

	public void updateMailLogDetail(Long generatedEmailId, ArrayList<String> AttachmentNames, int fileUploadCount,
			int totalFile);

	public void insertIntoEmailLogDetails(List<FileDetailsDTO> uploadedFileList, Long generatedEmailId)
			throws SQLException;

	public String getNewBatchIdForBulkUpload(String source, String status);

	public long insertIntoInvoiceQueue(String fileName, String extension, Long fileSize, String buId, String buCode,
			String buName, String companyId, String companyName, String docType, Long generatedEmailId);

	public void updateEmailLogDetailsEntry(String originalFileName, String newFileName, String uploadedFileStatus,
			String fileId, String generatedEmailId);
	public List<Map<String, String>> doLogSelectedFileForUpload(
	        String buId, String companyName, String buName, String batchId, String uploadedFiles,
	        String userName, long userId, String userIpAddress, String maxFileSize, int index,
	        String ipAddress, String splitRequired, String splitPageCount,
	        String name);

}
