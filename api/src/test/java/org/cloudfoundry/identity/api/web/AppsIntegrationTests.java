package org.cloudfoundry.identity.api.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.DefaultOAuth2SerializationService;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author Ryan Heaton
 * @author Dave Syer
 */
public class AppsIntegrationTests {

	@Rule
	public ServerRunning serverRunning = ServerRunning.isRunning();
	
	/**
	 * tests a happy-day flow of the native application profile.
	 */
	@Test
	public void testHappyDay() throws Exception {

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
		formData.add("grant_type", "password");
		formData.add("client_id", "vmc");
		formData.add("client_secret", "");
		formData.add("username", "marissa");
		formData.add("password", "koala");
		formData.add("scope", "read");

		ResponseEntity<String> response = serverRunning.postForString("/cloudfoundry-identity-uaa/oauth/token", formData);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("no-store", response.getHeaders().getFirst("Cache-Control"));

		DefaultOAuth2SerializationService serializationService = new DefaultOAuth2SerializationService();
		OAuth2AccessToken accessToken = serializationService.deserializeJsonAccessToken(new ByteArrayInputStream(
				response.getBody().getBytes()));

		// now try and use the token to access a protected resource.

		// first make sure the resource is actually protected.
		assertNotSame(HttpStatus.OK, serverRunning.getStatusCode("/cloudfoundry-identity-api/apps"));

		// then make sure an authorized request is valid.
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("Authorization", String.format("%s %s", OAuth2AccessToken.BEARER_TYPE, accessToken.getValue()));
		ResponseEntity<String> result = serverRunning.getForString("/cloudfoundry-identity-api/apps", headers);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		String body = result.getBody();
		assertTrue("Wrong response: "+body, body.contains("dsyerapi.cloudfoundry.com"));

	}

}
