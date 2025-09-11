/*
* If not stated otherwise in this file or this component's Licenses.txt file the
* following copyright and licenses apply:
*
* Copyright 2024 RDK Management
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*
http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.rdkm.tdkservice.serviceimpl;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.rdkm.tdkservice.exception.UserInputException;

/*
 * The HttpService class provides methods for sending HTTP requests.
 */
@Service
public class HttpService {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpService.class);

	private final RestTemplate restTemplate;

	public HttpService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	/**
	 * Sends a GET request to the specified URL with the provided headers.
	 *
	 * @param url     the URL to send the GET request to; must not be null or empty
	 * @param headers a map of headers to include in the request; can be null
	 * @return a ResponseEntity containing the response as a String
	 * @throws IllegalArgumentException if the URL is null or empty
	 * @throws RuntimeException         if an error occurs during the GET request
	 */
	public ResponseEntity<String> sendGetRequest(String url, Map<String, String> headers) {
		if (url == null || url.isEmpty()) {
			LOGGER.error("URL is null or empty");
			throw new IllegalArgumentException("URL must not be null or empty");
		}

		HttpHeaders httpHeaders = new HttpHeaders();
		if (headers != null) {
			headers.forEach(httpHeaders::set);
		}
		HttpEntity<String> entity = new HttpEntity<>(httpHeaders);

		try {
			LOGGER.info("Sending GET request to URL: {}", url);
			return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		} catch (RestClientException e) {
			LOGGER.error("Error during GET request to URL: {}", url, e);
			throw new RuntimeException("Error during GET request", e);
		}
	}

	/**
	 * Sends a POST request to the specified URL with the given request body and
	 * headers.
	 *
	 * @param url         the URL to send the POST request to; must not be null or
	 *                    empty
	 * @param requestBody the body of the request; must not be null
	 * @param headers     a map of headers to include in the request; can be null
	 * @return a ResponseEntity containing the response as a String
	 * @throws UserInputException       any user input related exception
	 * @throws IllegalArgumentException if the URL or request body is null or empty
	 * @throws RuntimeException         if an error occurs during the POST request
	 */
	public ResponseEntity<String> sendPostRequest(String url, Object requestBody, Map<String, String> headers) {
		if (url == null || url.isEmpty()) {
			LOGGER.error("URL is null or empty");
			throw new IllegalArgumentException("URL must not be null or empty");
		}
		if (requestBody == null) {
			LOGGER.error("Request body is null");
			throw new IllegalArgumentException("Request body must not be null");
		}

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		if (headers != null) {
			headers.forEach(httpHeaders::set);
		}
		HttpEntity<Object> entity = new HttpEntity<>(requestBody, httpHeaders);

		try {
			LOGGER.info("Sending POST request to URL: {}", url);
			return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
		} catch (HttpClientErrorException.Forbidden e) {
			LOGGER.error("Access denied to this jira or Account has been blocked", e.getMessage());
			throw new UserInputException("Access denied to this jira or Account has been blocked");
		} catch (HttpClientErrorException.Unauthorized e) {
			LOGGER.error("Authentication with Jira failed. Incorrect username or password", e.getMessage());
			throw new UserInputException(
					"Authentication with Jira failed. Incorrect username or password.Multiple attempts with incorrect credentials will lock the account.");
		} catch (HttpClientErrorException.BadRequest e) {
			LOGGER.error("Please check the jira field selection", e.getMessage());
			String error = e.getMessage();
			Pattern pattern = Pattern.compile("\\{\"message\":\"(.*?)\"\\}");
			Matcher matcher = pattern.matcher(error);
			if (matcher.find()) {
				String errorMessage = matcher.group(1);
				throw new UserInputException(errorMessage);
			} else {
				throw new UserInputException(error);
			}

		} catch (RestClientException e) {
			LOGGER.error("Error during POST request to URL: {}", url, e);
			throw new RuntimeException("Error during POST request", e);

		}
	}

	/**
	 * Adds an attachment to a ticket by sending a POST request to the specified
	 * URL.
	 *
	 * @param url       the URL to send the request to; must not be null or empty
	 * @param ticketKey the key of the ticket to add the attachment to; must not be
	 *                  null or empty
	 * @param file      the file to attach; must not be null and must exist
	 * @param user      the username for authentication; must not be null or empty
	 * @param password  the password for authentication; must not be null or empty
	 * @return a ResponseEntity containing the response from the server
	 * @throws IllegalArgumentException if any of the input parameters are invalid
	 * @throws RuntimeException         if an error occurs during the request
	 */
	public ResponseEntity<String> addAttachmentToTicket(String url, String ticketKey, File file, String user,
			String password) {
		// Validate input parameters
		if (url == null || url.isEmpty()) {
			LOGGER.error("URL is null or empty");
			throw new IllegalArgumentException("URL must not be null or empty");
		}
		if (ticketKey == null || ticketKey.isEmpty()) {
			LOGGER.error("Ticket key is null or empty");
			throw new IllegalArgumentException("Ticket key must not be null or empty");
		}
		if (file == null || !file.exists()) {
			LOGGER.error("File is null or does not exist");
			throw new IllegalArgumentException("File must not be null and must exist");
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		headers.setBasicAuth(user, password);

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("ticketKey", ticketKey);
		body.add("file", new FileSystemResource(file));
		body.add("user", user);
		body.add("password", password);

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

		try {
			LOGGER.info("Sending attachment to ticket: {}", ticketKey);
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
			LOGGER.info("Attachment sent successfully to ticket: {}", ticketKey);
			return response;
		} catch (RestClientException e) {
			LOGGER.error("Error during attachment to ticket: {}", ticketKey, e);
			throw new RuntimeException("Error during attachment to ticket", e);
		}
	}

}
