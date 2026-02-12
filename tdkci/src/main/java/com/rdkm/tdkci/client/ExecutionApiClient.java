package com.rdkm.tdkci.client;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.rdkm.tdkci.config.AppConfig;
import com.rdkm.tdkci.dto.DeviceStatusWrapperDTO;
import com.rdkm.tdkci.dto.ExecutionResponseDTO;
import com.rdkm.tdkci.dto.ExecutionTriggerDTO;
import com.rdkm.tdkci.exception.ExecutionClientApiException;
import com.rdkm.tdkci.response.DataResponse;
import com.rdkm.tdkci.utils.Constants;
import com.rdkm.tdkci.utils.Utils;

@Component
public class ExecutionApiClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionApiClient.class);

	private static final String DEVICE_STATUS_ENDPOINT = "/tdkservice/api/v1/device/getDeviceStatusByIP?deviceIP=";
	private static final String EXECUTION_API_URL = "/tdkservice/execution/trigger";
	private static final String XCONF_API_URL = "xconfAdminService/xconfAdminService/updates/firmwares?applicationType=stb";

	private final RestTemplate restTemplate;

	public ExecutionApiClient(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	/**
	 * Triggers execution using the provided payload.
	 *
	 * @param payload the execution trigger data
	 * @return DataResponse containing the execution result
	 * @throws ExecutionClientApiException if the execution fails
	 * @throws IllegalArgumentException    if payload is null
	 */
	public ExecutionResponseDTO triggerExecution(ExecutionTriggerDTO payload) {
		if (payload == null) {
			throw new IllegalArgumentException("Execution payload cannot be null");
		}

		try {
			String triggerApiUrl = buildTriggerApiUrl();
			LOGGER.info("Triggering execution at URL: {}", triggerApiUrl);

			// Parse the wrapped response and extract the data
			DataResponse response = restTemplate.postForObject(triggerApiUrl, payload, DataResponse.class);
			LOGGER.info("Successfully received response from execution API");

			if (response == null || response.getData() == null) {
				throw new ExecutionClientApiException("Received null response from execution API");
			}

			// Convert the data object to ExecutionResponseDTO using ObjectMapper
			ObjectMapper mapper = new ObjectMapper();
			ExecutionResponseDTO executionResponse = mapper.convertValue(response.getData(),
					ExecutionResponseDTO.class);

			return executionResponse;

		} catch (RestClientException e) {
			LOGGER.error("Failed to trigger execution", e);
			throw new ExecutionClientApiException("Failed to trigger execution: " + e.getMessage(), e);
		} catch (Exception e) {
			LOGGER.error("Unexpected error during execution trigger", e);
			throw new ExecutionClientApiException("Unexpected error during execution trigger: " + e.getMessage(), e);
		}
	}

	/**
	 * Retrieves the status of a device by its IP address.
	 *
	 * @param deviceIp the IP address of the device
	 * @return the device status, or null if not available
	 * @throws ExecutionClientApiException if the status retrieval fails
	 * @throws IllegalArgumentException    if deviceIp is null or empty
	 */
	public String getDeviceStatus(String deviceIp) {
		if (deviceIp == null || deviceIp.trim().isEmpty()) {
			throw new IllegalArgumentException("Device IP cannot be null or empty");
		}

		try {
			String statusApiUrl = buildDeviceStatusUrl(deviceIp);
			LOGGER.info("Fetching device status from URL: {}", statusApiUrl);

			DeviceStatusWrapperDTO response = restTemplate.getForObject(statusApiUrl, DeviceStatusWrapperDTO.class);
			System.out.println("Response: " + response);
			String status = response.getData().getStatus();

			LOGGER.info("Received device status: {}", status);
			return status;

		} catch (RestClientException e) {
			LOGGER.error("Failed to fetch device status for IP: {}", deviceIp, e);
			throw new ExecutionClientApiException("Failed to fetch device status: " + e.getMessage(), e);
		} catch (Exception e) {
			LOGGER.error("Unexpected error while fetching device status for IP: {}", deviceIp, e);
			throw new ExecutionClientApiException("Unexpected error while fetching device status: " + e.getMessage(),
					e);
		}
	}

	/**
	 * Sets up Xconf upgrade using the provided API URL and payload.
	 *
	 * @param xconfApiUrl the Xconf API URL
	 * @param payload     the upgrade payload
	 * @return true if the upgrade setup was successful, false otherwise
	 * @throws ExecutionClientApiException if the setup fails
	 * @throws IllegalArgumentException    if parameters are null or empty
	 */
	public boolean setUpXconfUpgrade(Map<String, Object> payload) {
		if (payload == null) {
			throw new IllegalArgumentException("Payload cannot be null or empty");
		}
		String configFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + Constants.CI_CONFIG_FILE;
		File configFile = new File(configFilePath);
		if (!configFile.exists()) {
			throw new ExecutionClientApiException("Configuration file not found: " + configFilePath);
		}
		String baseUrl = Utils.getConfigProperty(configFile, Constants.XCONF_BASE_URL);

		if (baseUrl == null || baseUrl.trim().isEmpty()) {
			throw new ExecutionClientApiException("Xconf base URL not found in configuration");
		}
		String xconfApiUrl = baseUrl + XCONF_API_URL;

		try {
			LOGGER.info("Setting up Xconf upgrade at URL: {}", xconfApiUrl);
			restTemplate.put(xconfApiUrl, payload);
//			LOGGER.info("Received response from Xconf API: {}", response);

			// Return true if response is not null and not empty
			return true;

		} catch (RestClientException e) {
			LOGGER.error("Failed to setup Xconf upgrade at URL: {}", xconfApiUrl, e);
			throw new ExecutionClientApiException("Failed to setup Xconf upgrade: " + e.getMessage(), e);
		} catch (Exception e) {
			LOGGER.error("Unexpected error during Xconf upgrade setup at URL: {}", xconfApiUrl, e);
			throw new ExecutionClientApiException("Unexpected error during Xconf upgrade setup: " + e.getMessage(), e);
		}
	}

	/**
	 * Builds the trigger API URL by combining base URL with execution endpoint.
	 *
	 * @return the complete trigger API URL
	 * @throws ExecutionClientApiException if URL construction fails
	 */
	private String buildTriggerApiUrl() {
		try {
			String baseUrl = getBaseUrl();
			return baseUrl + EXECUTION_API_URL;
		} catch (Exception e) {
			LOGGER.error("Failed to build trigger API URL", e);
			throw new ExecutionClientApiException("Failed to build trigger API URL: " + e.getMessage(), e);
		}
	}

	/**
	 * Builds the device status URL by combining base URL with device status
	 * endpoint and IP.
	 *
	 * @param deviceIp the device IP address
	 * @return the complete device status URL
	 * @throws ExecutionClientApiException if URL construction fails
	 */
	private String buildDeviceStatusUrl(String deviceIp) {
		try {
			String baseUrl = getBaseUrl();
			return baseUrl + DEVICE_STATUS_ENDPOINT + deviceIp;
		} catch (Exception e) {
			LOGGER.error("Failed to build device status URL for IP: {}", deviceIp, e);
			throw new ExecutionClientApiException("Failed to build device status URL: " + e.getMessage(), e);
		}
	}

	/**
	 * Retrieves the base URL from configuration.
	 *
	 * @return the base URL
	 * @throws ExecutionClientApiException if base URL cannot be retrieved
	 */
	private String getBaseUrl() {
		try {
			String configFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
					+ Constants.CI_CONFIG_FILE;
			File configFile = new File(configFilePath);

			if (!configFile.exists()) {
				throw new ExecutionClientApiException("Configuration file not found: " + configFilePath);
			}

			String baseUrl = Utils.getConfigProperty(configFile, Constants.TM_BASE_URL);

			if (baseUrl == null || baseUrl.trim().isEmpty()) {
				throw new ExecutionClientApiException("Base URL not found in configuration");
			}

			return baseUrl;
		} catch (Exception e) {
			LOGGER.error("Failed to retrieve base URL from configuration", e);
			throw new ExecutionClientApiException("Failed to retrieve base URL: " + e.getMessage(), e);
		}
	}
}
