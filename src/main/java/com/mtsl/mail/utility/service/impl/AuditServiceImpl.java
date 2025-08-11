package com.mtsl.mail.utility.service.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mtsl.mail.utility.dao.JdbcConnectionResource;
import com.mtsl.mail.utility.dto.EmailLogDTO;
import com.mtsl.mail.utility.dto.FileDetailsDTO;
import com.mtsl.mail.utility.service.AuditService;
import com.mtsl.mail.utility.sql.CommonSqlQuery;

@Repository
@Transactional
public class AuditServiceImpl extends JdbcConnectionResource implements AuditService {

	@Override
	public Long insertIntoEmailLog(EmailLogDTO emailLogDTO, Message message, Folder emailFolder) {
		long generatedEmailId = 0;
		try {
			// Step 1 — Check if a record already exists for this message
			generatedEmailId = getMailUploadLogId(message);
			if (generatedEmailId > 0) {
				return generatedEmailId;
			}
			// Step 2 — Insert new record and get generated key
			KeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(CommonSqlQuery.INSERT_INTO_ECRM_CA_EMAIL_LOG,
						Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, emailLogDTO.getBuId());
				ps.setString(2, emailLogDTO.getMailFrom());
				ps.setString(3, emailLogDTO.getSubject());
				ps.setString(4, emailLogDTO.getEntBy());
				ps.setInt(5, emailLogDTO.getEmailNumber());
				ps.setInt(6, emailLogDTO.getMailSize());

				java.util.Date utilDate = emailLogDTO.getMailReceivedDate();
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
				ps.setString(7, dateFormat.format(utilDate));

				return ps;
			}, keyHolder);

			if (keyHolder.getKey() != null) {
				generatedEmailId = keyHolder.getKey().longValue();
			}

		} catch (Exception exception) {
			exception.printStackTrace();
			// Keep your original error handling for marking the message as SEEN
			try {
				emailFolder.setFlags(new Message[] { message }, new Flags(Flags.Flag.SEEN), false);
			} catch (MessagingException e1) {
				e1.printStackTrace();
			}
		}
		return generatedEmailId;
	}

	@Override
	public Integer getMailUploadLogId(Message message) {
		return jdbcTemplate.query((PreparedStatementCreator) con -> {
			PreparedStatement ps = con.prepareStatement(CommonSqlQuery.GET_MAIL_UPLOAD_LOG_ID);
			ps.setInt(1, message.getMessageNumber());
			return ps;
		}, (ResultSetExtractor<Integer>) rs -> {
			if (rs.next()) {
				return rs.getInt(1);
			}
			return 0;
		});
	}

	@Override
	public void updateMailBodyNData(Long generatedEmailId, EmailLogDTO emailLogDTO) {

		jdbcTemplate.update(CommonSqlQuery.UPDATE_EMAIL_BODY_DATA_INTO_ECRM_CA_EMAIL_LOG, emailLogDTO.getEmailBody(),
				emailLogDTO.getEmailData(), generatedEmailId);

	}

	@Override
	public Integer getMailUploadLogFailCount(Message message) {
		return jdbcTemplate.query((PreparedStatementCreator) con -> {
			PreparedStatement ps = con.prepareStatement(CommonSqlQuery.GET_MAIL_UPLOAD_FAIL_COUNT);
			ps.setInt(1, message.getMessageNumber());
			return ps;
		}, (ResultSetExtractor<Integer>) rs -> {
			if (rs.next()) {
				return rs.getInt(1);
			}
			return 0;
		});
	}

	@Override
	public Integer updateMailUploadLogFailCount(Message message) {
		jdbcTemplate.update(CommonSqlQuery.UPDATE_MAIL_UPLOAD_FAIL_COUNT, message.getMessageNumber());
		// Fetch updated fail count
		return getMailUploadLogFailCount(message);
	}

	@Override
	public void updateMailLogDetail(Long generatedEmailId, ArrayList<String> attachmentNames, int fileUploadCount,
			int totalFile) {
		String attachmentFileName = String.join(", ", attachmentNames); 
		int status;
		if (totalFile == 0 || fileUploadCount == 0) {
			status = 799;
		} else if (totalFile > fileUploadCount) {
			status = 798;
		} else {
			status = 797;
		}

		String sql = "UPDATE ECRM_CA_GENERIC_EMAIL_UPLOAD_LOG "
				+ "SET TOTAL_ATTACHMENTS = ?, TOTAL_ATTACHMENT_UPLOADED = ?, STATUS = ? " + "WHERE ID = ?";

		try {
			jdbcTemplate.update(sql, totalFile, fileUploadCount, status, generatedEmailId);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void insertIntoEmailLogDetails(List<FileDetailsDTO> uploadedFileList, Long generatedEmailId) {

		if (uploadedFileList == null || uploadedFileList.isEmpty()) {
			return; // nothing to insert
		}
		jdbcTemplate.batchUpdate(CommonSqlQuery.INSERT_INTO_EMAIL_DETAILS_SQL, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				FileDetailsDTO row = uploadedFileList.get(i);
				ps.setLong(1, generatedEmailId);
				ps.setString(2, row.getFileName());
				ps.setLong(3, row.getFileSize());
			}

			@Override
			public int getBatchSize() {
				return uploadedFileList.size();
			}
		});
	}

	@Override
	public String getNewBatchIdForBulkUpload(String source, String status) {
		try {
			return jdbcTemplate.query(CommonSqlQuery.GET_NEW_BATCH_ID_BULK_UPLOAD, ps -> {
				ps.setString(1, source);
			}, rs -> {
				if (rs.next()) {
					String batchNumber = rs.getString("BATCHNUMBER");
					return (batchNumber == null) ? "1" : batchNumber;
				}
				return "1";
			});
		} catch (Exception e) {
			return "1";
		}
	}

	@Override
	public long insertIntoInvoiceQueue(String fileName, String extension, Long fileSize, String buId, String buCode,
			String buName, String companyId, String companyName, String docType, Long generatedEmailId) {
		long fileId = 0;

		try {
			// New style queryForObject (no deprecation warning)
			String invoiceName = jdbcTemplate.queryForObject(CommonSqlQuery.GET_NEW_INVOICE_NAME, String.class, buId,
					companyName, buCode);

			KeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(CommonSqlQuery.INSERT_INTO_INVOICE_QUEUE,
						Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, invoiceName);
				ps.setString(2, fileName);
				ps.setString(3, companyName);
				ps.setString(4, buName);
				ps.setString(5, buId);
				ps.setInt(6, 244);
				ps.setLong(7, fileSize);
				ps.setString(8, fileName);
				ps.setString(9, docType);
				ps.setInt(10, (extension.equalsIgnoreCase("PDF") || extension.equalsIgnoreCase("TIF")
						|| extension.equalsIgnoreCase("TIFF")) ? 334 : 283);
				ps.setInt(11, 1);
				ps.setInt(12, 1);
				ps.setInt(13, 0);
				ps.setLong(14, generatedEmailId);
				return ps;
			}, keyHolder);

			if (keyHolder.getKey() != null) {
				fileId = keyHolder.getKey().longValue();
			} else {
				throw new SQLException("Creating file id failed, no ID obtained.");
			}

			String ftpLocation = jdbcTemplate.queryForObject(CommonSqlQuery.GET_FTP_LOCATION, String.class, buCode);

			String filePhysicalPath = jdbcTemplate.queryForObject(CommonSqlQuery.GENERATE_FTP_PHYSICAL_PATH,
					String.class, "", companyId, buId, fileId, extension);

			jdbcTemplate.update(CommonSqlQuery.UPDATE_FILE_PHYSICAL_PATH, filePhysicalPath, ftpLocation, fileId);

			jdbcTemplate.update(CommonSqlQuery.INSERT_INTO_OCR_HORIZONTAL_DATA, fileId, buId, "0");

			String INSERT_INTO_BPMS_FILE_REPOSITORY = "INSERT INTO BPMS_FILE_REPOSITORY (File_ID, File_Name, Company_ID, Business_Unit_ID, "
					+ "Invoice_Assignement_Queue_Status, Physical_Path, Original_File_Name, File_Size, Ent_By, Ent_Stamp) "
					+ "VALUES (?, ?, ?, ?, 244, ?, ?, ?, 1, GETDATE())";

			jdbcTemplate.update(INSERT_INTO_BPMS_FILE_REPOSITORY, fileId, invoiceName, companyId, buId,
					filePhysicalPath, fileName, fileSize);

		} catch (SQLException e) {
			if (e.getErrorCode() == 2627 || e.getErrorCode() == 2) {
				fileId = -2627;
			} else {
				throw new RuntimeException("SQL Error: " + e.getMessage(), e);
			}
		} catch (Exception e) {
			throw new RuntimeException("Error inserting invoice queue", e);
		}

		return fileId;
	}

	@Override
	public void updateEmailLogDetailsEntry(String originalFileName, String newFileName, String uploadedFileStatus,
			String fileId, String generatedEmailId) {
		// Determine isDuplicate (though in current logic it’s unused)
		String isDuplicate = "0";
		if ("1".equalsIgnoreCase(uploadedFileStatus)) {
			isDuplicate = "0";
		} else if ("0".equalsIgnoreCase(uploadedFileStatus)) {
			isDuplicate = "1";
		}

		// Adjust fileId if it is "0"
		String fileIdValue = "0".equals(fileId) ? null : fileId;

		// Default status if blank
		if (uploadedFileStatus.isBlank()) {
			uploadedFileStatus = "0";
		}

		String sql = "UPDATE ECRM_CA_GENERIC_EMAIL_UPLOAD_DETAILS "
				+ "SET FILE_NAME = ?, STATUS = ?, FILE_ID = ?, UPD_STAMP = GETDATE() "
				+ "WHERE ORGINAL_FILE_NAME = ? AND EMAIL_DOC_LOG_ID = ?";

		jdbcTemplate.update(sql, newFileName, uploadedFileStatus, fileIdValue, originalFileName, generatedEmailId);

	}

	@Override
	public List<Map<String, String>> doLogSelectedFileForUpload(String buId, String companyName, String buName,
			String batchId, String uploadedFiles, String userName, long userId, String userIpAddress,
			String maxFileSize, int index, String ipAddress, String splitRequired, String splitPageCount, String name) {

		List<Map<String, String>> bulkUploadResponseList = new ArrayList<>();

		try {
			String insertImageUploadLog = "CREATE TABLE #TEMP_FILE_LIST(VALUE NVARCHAR(1000)) "
					+ "INSERT INTO #TEMP_FILE_LIST(VALUE) SELECT * FROM FN_BPO_SPLIT(N'"
					+ uploadedFiles.replace("'", "''") + "','|') "
					+ "INSERT INTO ECRM_CA_IMAGE_UPLOAD_LOG(Batch_No, BU_ID, Company_Name, BU_Name, File_Name, Original_File_Name, Size, "
					+ "Upload_By, Upload_By_ID, Upload_Stamp, Source, C_IP_Address, Operation_Type, Status, Page_Split_Size) "
					+ "SELECT '" + batchId + "' AS Batch_No, '" + buId + "' AS BU_ID, '" + companyName
					+ "' AS Company_Name, '" + buName + "' AS BU_Name, "
					+ "dbo.FN_ParseString(-1, ':', SPLIT.VALUE) File_Name, "
					+ "dbo.FN_ParseString(-4, ':', SPLIT.VALUE) Original_File_Name, "
					+ "dbo.FN_ParseString(-5, ':', SPLIT.VALUE) Size, '" + userName + "' AS Upload_By, '" + userId
					+ "' AS Upload_By_ID, " + "GETDATE() Upload_Stamp, 'WS' SOURCE, '" + ipAddress + "' C_IP_ADDRESS, "
					+ "CASE ISNULL(IUL.Image_Log_Id, 0) WHEN 0 THEN '512' ELSE '513' END OPERATION_TYPE, "
					+ "CASE WHEN CAST(dbo.FN_ParseString(-5, ':', SPLIT.VALUE) AS NUMERIC)=0 "
					+ "     OR CAST(dbo.FN_ParseString(-5, ':', SPLIT.VALUE) AS NUMERIC) > '" + maxFileSize
					+ "' THEN '509' "
					+ "     ELSE (CASE WHEN LEN(dbo.FN_ParseString(-4, ':', SPLIT.VALUE)) > 205 THEN '511' "
					+ "     ELSE (CASE ISNULL(IUL.Image_Log_Id, 0) WHEN 0 THEN '291' ELSE (CASE '" + buId
					+ "' WHEN '-5' THEN '291' ELSE '503' END) END) END) END STATUS, " + "CASE WHEN '" + splitRequired
					+ "' ='1' AND (dbo.FN_ParseString(-3, ':', SPLIT.VALUE) LIKE '.tif%' OR dbo.FN_ParseString(-3, ':', SPLIT.VALUE) LIKE '.pdf%') "
					+ "THEN '" + splitPageCount + "' ELSE NULL END Page_Split_Size " + "FROM #TEMP_FILE_LIST AS SPLIT "
					+ "LEFT JOIN ECRM_CA_IMAGE_UPLOAD_LOG IUL WITH (NOLOCK) ON (IUL.BU_ID = '" + buId + "' "
					+ "AND dbo.FN_ParseString(-1, '_', IUL.File_Name) + '_' + dbo.FN_ParseString(-2, '_', IUL.File_Name) + '_' + IUL.Original_File_Name = dbo.FN_ParseString(-2, ':', SPLIT.VALUE) "
					+ "AND (Status = 505 OR Status = 510))";

			// Run the query
			jdbcTemplate.execute(insertImageUploadLog);

		} catch (Exception ex) {
			ex.getMessage();
		}

		return bulkUploadResponseList;
	}

}
