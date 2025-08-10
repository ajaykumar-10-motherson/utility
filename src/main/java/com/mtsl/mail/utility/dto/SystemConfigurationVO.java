package com.mtsl.mail.utility.dto;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemConfigurationVO {

	private long configId;
	private String configKey;
	private String configValue;
	private String configDesc;
	private String enteredBy;
	private Date enteredOn;
	private String updatedBy;
	private Date updatedOn;
	private String status;
	private String isRolledOut;
	private String strEnteredBy;
	private String strUpdatedBy;

}
