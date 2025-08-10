package com.mtsl.mail.utility.dto;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailLogDTO {
	private String id;
	private String buId;
	private String mailFrom;
	private String subject;
	private String emailBody;
	private byte[] emailData;
	private String entBy;
	private Date entStamp;
	private int mailSize;
	private Date mailReceivedDate;
	private int emailNumber;
	private int failCount;
	private byte isNotificationSend;
}
