package com.mtsl.mail.utility.dao.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.mtsl.mail.utility.dao.CommonDao;
import com.mtsl.mail.utility.dao.JdbcConnectionResource;
import com.mtsl.mail.utility.dto.MailDTO;
import com.mtsl.mail.utility.dto.SystemConfigurationVO;
import com.mtsl.mail.utility.sql.CommonSqlQuery;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CommonDaoImpl extends JdbcConnectionResource implements CommonDao {

	private final SystemConfigurationRowMapper rowMapper;

	@Override
	public SystemConfigurationVO getSystemConfigurationByConfigKey(String configKey) {

		String sql = "SELECT * FROM ECRM_CA_SYS_CONFIG WITH(NOLOCK) WHERE Config_Key = ? AND Status = 1";

		List<SystemConfigurationVO> results = jdbcTemplate.query(sql, rowMapper, configKey);

		return results.isEmpty() ? null : results.get(0);
	}

	@Override
	public MailDTO getGenericDatafromDB(String email) {
	    String sql = CommonSqlQuery.GENERIC_EMAIL_ID_TO_DOWNLOAD_ATTACHMENT;

	    List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, email);

	    if (results.isEmpty()) {
	        return null;
	    }
	    Map<String, Object> row = results.get(0);

	    MailDTO mailDTO = new MailDTO();
	    mailDTO.setDefDocType(String.valueOf(row.get("Default_Doc_Type")));
	    mailDTO.setCompanyId(String.valueOf(row.get("COMPANYID")));
	    mailDTO.setImap(String.valueOf(row.get("IMAP_IP")));
	    mailDTO.setCompanyName(String.valueOf(row.get("COMPANY_NAME")));
	    mailDTO.setBuId(String.valueOf(row.get("BUSINESS_UNIT_ID")));
	    mailDTO.setBuName(String.valueOf(row.get("UNIT_NAME")));
	    mailDTO.setBuCode(String.valueOf(row.get("UNIT_CODE")));
	    return mailDTO;

	}



}
