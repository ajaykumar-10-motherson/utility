package com.mtsl.mail.utility.service;

import javax.mail.Folder;
import javax.mail.Message;

public interface MarkMailAsUnread {

	public  void markMailAsUnread(Folder emailFolder, Message message);
	
	public  void markEmailAsRead(Folder emailFolder, Message message);
	
}
