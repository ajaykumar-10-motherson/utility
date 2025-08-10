package com.mtsl.mail.utility.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.mtsl.mail.utility.dto.SystemConfigurationVO;

@Component
public class SystemConfigurationRowMapper implements RowMapper<SystemConfigurationVO> {

    @Override
    public SystemConfigurationVO mapRow(ResultSet rs, int rowNum) throws SQLException {
        SystemConfigurationVO sysConfigVO = new SystemConfigurationVO();
        sysConfigVO.setConfigId(rs.getLong("Config_ID"));
        sysConfigVO.setConfigKey(rs.getString("Config_Key"));
        sysConfigVO.setConfigValue(rs.getString("Config_Value"));
        sysConfigVO.setConfigDesc(rs.getString("Description"));
        return sysConfigVO;
    }
}
