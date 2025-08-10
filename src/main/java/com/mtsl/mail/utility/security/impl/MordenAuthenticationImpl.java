/**
 * 
 */
package com.mtsl.mail.utility.security.impl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtsl.mail.utility.security.MordenAuthentication;

/**
 * @author ajay.kumar10
 *
 */
@Component
public class MordenAuthenticationImpl implements MordenAuthentication {

	@Override
	public String getAuthToken(String tanantId, String clientId, String clientSecret) throws Exception {
		try(CloseableHttpClient client = HttpClients.createDefault();){
		HttpPost loginPost = new HttpPost("https://login.microsoftonline.com/" + tanantId + "/oauth2/v2.0/token");
		String scopes = "https://outlook.office365.com/.default";
		String encodedBody = "client_id=" + clientId + "&scope=" + scopes + "&client_secret=" + clientSecret
				+ "&grant_type=client_credentials";
		loginPost.setEntity(new StringEntity(encodedBody, ContentType.APPLICATION_FORM_URLENCODED));
		loginPost.addHeader(new BasicHeader("cache-control", "no-cache"));
		CloseableHttpResponse loginResponse = client.execute(loginPost);
		InputStream inputStream = loginResponse.getEntity().getContent();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[16384];
		while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		byte[] response = buffer.toByteArray();
		ObjectMapper objectMapper = new ObjectMapper();
		JavaType type = objectMapper.constructType(
				objectMapper.getTypeFactory().constructParametricType(Map.class, String.class, String.class));
		Map<String, String> parsed = new ObjectMapper().readValue(response, type);
		return parsed.get("access_token");
		}
	}

}
