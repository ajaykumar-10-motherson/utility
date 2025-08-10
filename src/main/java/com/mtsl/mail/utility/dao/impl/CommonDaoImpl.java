package com.mtsl.mail.utility.dao.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.mtsl.mail.utility.dao.CommonDao;
import com.mtsl.mail.utility.dao.JdbcConnectionResource;
import com.mtsl.mail.utility.dto.SystemConfigurationVO;

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

}
