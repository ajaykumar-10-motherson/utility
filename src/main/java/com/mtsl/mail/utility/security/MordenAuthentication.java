package com.mtsl.mail.utility.security;

public interface MordenAuthentication {
	public String getAuthToken(String tanantId, String clientId, String clientSecret)throws Exception;
	
}
