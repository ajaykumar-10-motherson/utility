package com.mtsl.mail.utility.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMailDTO {
	String commonEmailId;
	String companyBu;
	long emaliLogId;
	String mailTo;
}
