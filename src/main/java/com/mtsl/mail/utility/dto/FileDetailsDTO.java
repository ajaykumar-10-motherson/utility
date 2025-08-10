/**
 * 
 */
package com.mtsl.mail.utility.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * @author ajay.kumar10
 *
 */
@Getter
@Setter
public class FileDetailsDTO {
	private String fileName;
	private long fileSize;
	private String fileType;
	private String originalFileName;
}
