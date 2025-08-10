package com.mtsl.mail.utility.dao;

import com.mtsl.mail.utility.dto.MailDTO;
import com.mtsl.mail.utility.dto.SystemConfigurationVO;

public interface CommonDao {

	public SystemConfigurationVO getSystemConfigurationByConfigKey(String configKey);
	
	public MailDTO getGenericDatafromDB(String email);
}
