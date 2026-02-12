package com.rdkm.tdkci.serviceimpl;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.rdkm.tdkci.client.ExecutionApiClient;
import com.rdkm.tdkci.dto.ExecutionDTO;
import com.rdkm.tdkci.dto.ExecutionResponseDTO;
import com.rdkm.tdkci.dto.ExecutionTriggerDTO;
import com.rdkm.tdkci.enums.Category;
import com.rdkm.tdkci.enums.ExecutionStatus;
import com.rdkm.tdkci.enums.ExecutionTriggerStatus;
import com.rdkm.tdkci.exception.ResourceNotFoundException;
import com.rdkm.tdkci.model.Device;
import com.rdkm.tdkci.model.Execution;
import com.rdkm.tdkci.model.XconfConfig;
import com.rdkm.tdkci.repository.DeviceRepository;
import com.rdkm.tdkci.repository.ExecutionRepository;
import com.rdkm.tdkci.response.Response;
import com.rdkm.tdkci.service.IExecutionService;
import com.rdkm.tdkci.utils.CommandExecutor;

@Service
@Qualifier("tdk")
public class TdkExecutionService implements IExecutionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TdkExecutionService.class);

	@Autowired
	private DeviceRepository deviceRepository;

	@Autowired
	private ExecutionRepository executionRepository;

	@Autowired
	private ExecutionApiClient executionApiClient;

	@Autowired
	private CommandExecutor commandExecutor;

	@Override
	public Response upgradeDeviceAndTriggerCIExecution(ExecutionDTO execTriggerDTO) {
		LOGGER.info("Executing TDK upgrade and CI trigger for buildName: {}", execTriggerDTO.getBuildName());

		// Early validation
		Device device = deviceRepository.findByName(execTriggerDTO.getDeviceName());
		if (device == null) {
			LOGGER.error("Device with name {} not found.", execTriggerDTO.getDeviceName());
			throw new ResourceNotFoundException("Device not found", execTriggerDTO.getDeviceName());
		}

		// Extract common variables
		String buildName = execTriggerDTO.getBuildName();
		Category category = device.getCategory();
		XconfConfig xconfConfig = device.getXconfConfig();

		// Handle RDKV prefix validation
		if ("RDKV".equalsIgnoreCase(category.toString())) {
			return handleRDKVDevice(device, buildName, category, xconfConfig, execTriggerDTO);
		}

		// Handle RDKB (no prefix validation needed)
		if ("RDKB".equalsIgnoreCase(category.toString())) {
			return handleRDKBDevice(device, buildName, category, xconfConfig, execTriggerDTO);
		}

		LOGGER.warn("Unsupported device category: {}", category);
		return new Response("Unsupported device category", 400);
	}

	private Response handleRDKVDevice(Device device, String buildName, Category category, XconfConfig xconfConfig,
			ExecutionDTO execTriggerDTO) {

		List<String> devicePrefixes = device.getImagePrefixes();
		String matchedPrefix = findMatchingPrefix(buildName, devicePrefixes);

		if (matchedPrefix == null) {
			return createDeviceNotConfiguredResponse(device, buildName, category);
		}

		return processDeviceExecution(device, buildName, matchedPrefix, category, xconfConfig, execTriggerDTO);
	}

	private Response handleRDKBDevice(Device device, String buildName, Category category, XconfConfig xconfConfig,
			ExecutionDTO execTriggerDTO) {

		// RDKB doesn't need prefix matching
		return processDeviceExecution(device, buildName, null, category, xconfConfig, execTriggerDTO);
	}

	private Response processDeviceExecution(Device device, String buildName, String matchedPrefix, Category category,
			XconfConfig xconfConfig, ExecutionDTO execTriggerDTO) {

		String imageVersion = this.getImageVersion(buildName, matchedPrefix);
		String upgradeImageFileName = this.getUpgradeImageFileName(buildName, device.getFileExtension());

		// Check if upgrade is required
		if (!device.isUpgradeRequired()) {
			return handleSkippedUpgrade(device, buildName, imageVersion, upgradeImageFileName, category, xconfConfig,
					execTriggerDTO);
		}

		// // Check if same image version (no upgrade needed)
		// if (isSameImageVersion(device, imageVersion)) {
		// LOGGER.info("Device {} already has the requested image version: {}",
		// device.getName(), imageVersion);
		// return new Response("Device already has the requested image version", 200);
		// }

		return handleImageUpgrade(device, buildName, imageVersion, upgradeImageFileName, category, xconfConfig,
				execTriggerDTO);
	}

	private Response handleImageUpgrade(Device device, String buildName, String imageVersion,
			String upgradeImageFileName, Category category, XconfConfig xconfConfig, ExecutionDTO execTriggerDTO) {

		String deviceAvailability = executionApiClient.getDeviceStatus(device.getIp());

		if (!"FREE".equalsIgnoreCase(deviceAvailability)) {
			return createUnavailableDeviceResponse(buildName, imageVersion, upgradeImageFileName, category, device);
		}

		return startImageUpgrade(device, buildName, imageVersion, upgradeImageFileName, category, xconfConfig,
				execTriggerDTO);
	}

	/**
	 * Handle skipped upgrade scenario - trigger CI execution directly without
	 * upgrade
	 */
	private Response handleSkippedUpgrade(Device device, String buildName, String imageVersion,
			String upgradeImageFileName, Category category, XconfConfig xconfConfig, ExecutionDTO execTriggerDTO) {

		LOGGER.info("Image upgrade is marked as skipped for device {}. Triggering execution without upgrade.",
				device.getName());

		String deviceAvailability = executionApiClient.getDeviceStatus(device.getIp());

		if ("FREE".equalsIgnoreCase(deviceAvailability)) {
			Execution execution = createExecution(buildName, imageVersion, upgradeImageFileName, category, device,
					ExecutionStatus.PENDING);

			// Trigger CI execution directly without upgrade
			ExecutionStatus status = triggerCIExecution(execution, device, xconfConfig, execTriggerDTO);

			if (status.equals(ExecutionStatus.TRIGGERED)) {
				return new Response(
						"Image upgrade is marked as skipped for the device, So triggered execution without Image upgrade",
						200);
			} else {
				return new Response("Image upgrade is marked as skipped for the device ,But Execution trigger failed",
						502);
			}
		} else {
			LOGGER.error("Device {} is not available for execution.", device.getName());
			createExecution(buildName, imageVersion, upgradeImageFileName, category, device,
					ExecutionStatus.DEVICE_UNAVAILABLE);
			return new Response("Device is not available for execution in the tool", 503);
		}
	}

	/**
	 * Start image upgrade process
	 */
	private Response startImageUpgrade(Device device, String buildName, String imageVersion,
			String upgradeImageFileName, Category category, XconfConfig xconfConfig, ExecutionDTO execTriggerDTO) {

		LOGGER.info("Device {} is available. Starting image upgrade process.", device.getName());

		Map<String, Object> buildFirmwarePayload = this.buildFirmwarePayload(xconfConfig.getXconfigId(),
				xconfConfig.getXconfigName(), xconfConfig.getXconfigDescription(), upgradeImageFileName, imageVersion);
		System.out.println("The buildFirmwarePayload" + buildFirmwarePayload.toString());

		executionApiClient.setUpXconfUpgrade(buildFirmwarePayload);

		Execution execution = createExecution(buildName, imageVersion, upgradeImageFileName, category, device,
				ExecutionStatus.IMAGE_UPGRADE_INPROGRESS);

		// Start asynchronous monitoring - this won't block the response
		startAsyncUpgradeMonitoring(execution.getId(), device, imageVersion, execTriggerDTO, xconfConfig);

		// Return immediately with execution ID for tracking
		Map<String, Object> responseData = new HashMap<>();
		responseData.put("executionId", execution.getId());
		responseData.put("status", "IMAGE_UPGRADE_INPROGRESS");
		responseData.put("message",
				"Image upgrade started successfully. You can track progress using the executionId.");

		return new Response("Image upgrade started successfully.", 200);
	}

	private Execution createExecution(String buildName, String imageVersion, String upgradeImageFileName,
			Category category, Device device, ExecutionStatus status) {

		Execution execution = new Execution();
		execution.setRequestImageName(buildName);
		execution.setRequestImageVersion(imageVersion);
		execution.setUpgradeImageFileName(upgradeImageFileName);
		execution.setCategory(category);
		execution.setDevice(device);
		execution.setExecutionStatus(status);

		return executionRepository.save(execution);
	}

	private Response createDeviceNotConfiguredResponse(Device device, String buildName, Category category) {
		String imageVersion = this.getImageVersion(buildName, null);
		String upgradeImageFileName = this.getUpgradeImageFileName(buildName, device.getFileExtension());

		createExecution(buildName, imageVersion, upgradeImageFileName, category, device,
				ExecutionStatus.DEVICE_NOTCONFIGURED);

		return new Response("Device for this build is not configured in the tool, Please contact admin.", 500);
	}

	private Response createUnavailableDeviceResponse(String buildName, String imageVersion, String upgradeImageFileName,
			Category category, Device device) {

		createExecution(buildName, imageVersion, upgradeImageFileName, category, device,
				ExecutionStatus.DEVICE_UNAVAILABLE);

		return new Response("Device is not available for execution in the tool", 503);
	}

	private String findMatchingPrefix(String buildName, List<String> devicePrefixes) {
		if (devicePrefixes == null || devicePrefixes.isEmpty()) {
			return null;
		}

		return devicePrefixes.stream().filter(prefix -> buildName.startsWith(prefix)).findFirst().orElse(null);
	}

	/**
	 * Start asynchronous upgrade monitoring This method runs in a separate thread
	 * and doesn't block the main response
	 */
	@Async
	public CompletableFuture<Void> startAsyncUpgradeMonitoring(UUID executionId, Device device,
			String expectedImageVersion, ExecutionDTO execTriggerDTO, XconfConfig xconfConfig) {

		LOGGER.info("Starting async upgrade monitoring for execution ID: {} on device: {}", executionId,
				device.getName());

		return CompletableFuture.runAsync(() -> {
			monitorUpgradeProcess(executionId, device, expectedImageVersion, execTriggerDTO, xconfConfig);
		});
	}

	/**
	 * Monitor the upgrade process for 1 hour with 5-minute intervals
	 */
	private void monitorUpgradeProcess(UUID executionId, Device device, String expectedImageVersion,
			ExecutionDTO execTriggerDTO, XconfConfig xconfConfig) {

		final int CHECK_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes in milliseconds
		final int MAX_CHECKS = 12; // 60 minutes / 5 minutes = 12 checks
		int checkCount = 0;

		while (checkCount < MAX_CHECKS) {
			try {
				Thread.sleep(CHECK_INTERVAL_MS);
				checkCount++;

				LOGGER.info("Upgrade check #{} for execution ID: {} on device: {}", checkCount, executionId,
						device.getName());

				// Refresh execution from database to get latest status
				Execution execution = executionRepository.findById(executionId).orElse(null);
				if (execution == null) {
					LOGGER.error("Execution with ID {} not found. Stopping monitoring.", executionId);
					return;
				}

				// Check if monitoring was cancelled or execution failed for other reasons
				if (execution.getExecutionStatus() != ExecutionStatus.IMAGE_UPGRADE_INPROGRESS) {
					LOGGER.info("Execution {} is no longer in progress. Current status: {}. Stopping monitoring.",
							executionId, execution.getExecutionStatus());
					return;
				}

				// Check if image upgrade is complete
				boolean upgradeComplete = isSameImageVersion(device, expectedImageVersion);

				if (upgradeComplete) {
					LOGGER.info("Image upgrade completed successfully for execution ID: {}", executionId);
					// Trigger CI execution
					triggerCIExecution(execution, device, xconfConfig, execTriggerDTO);
					return; // Exit monitoring
				}

				LOGGER.info("Image upgrade still in progress for execution ID: {}. Check {}/{}", executionId,
						checkCount, MAX_CHECKS);

			} catch (InterruptedException e) {
				LOGGER.warn("Upgrade monitoring interrupted for execution ID: {}", executionId);
				Thread.currentThread().interrupt();
				return;
			} catch (Exception e) {
				LOGGER.error("Error during upgrade monitoring for execution ID {}: {}", executionId, e.getMessage());
				// Continue monitoring unless it's a critical error
			}
		}

		// If we reach here, timeout occurred
		LOGGER.error("Image upgrade timeout (1 hour) reached for execution ID: {}. Setting status as failed.",
				executionId);

		Execution execution = executionRepository.findById(executionId).orElse(null);
		if (execution != null) {
			execution.setExecutionStatus(ExecutionStatus.IMAGE_UPGRADE_FAILED);
			executionRepository.save(execution);
		}
	}

	/**
	 * Trigger CI execution after successful image upgrade
	 */
	private ExecutionStatus triggerCIExecution(Execution execution, Device device, XconfConfig xconfConfig,
			ExecutionDTO execTriggerDTO) {

		try {
			LOGGER.info("Triggering CI execution for execution ID: {} on device: {}", execution.getId(),
					device.getName());

			ExecutionTriggerDTO executionTriggerDTO = executionTriggerPayload(device, xconfConfig, execTriggerDTO);
			System.out.println("dto" + executionTriggerDTO);
			ExecutionResponseDTO response = executionApiClient.triggerExecution(executionTriggerDTO);
			System.out.println("Resposne" + response);

			// Check both the enum status and the message content for success
			boolean isTriggered = (response.getExecutionTriggerStatus() == ExecutionTriggerStatus.TRIGGERED)
					|| (response.getMessage() != null
							&& (response.getMessage().toLowerCase().contains("triggered successfully")
									|| response.getMessage().toLowerCase().contains("execution triggered")));

			if (isTriggered) {
				LOGGER.info("CI execution triggered successfully for execution ID: {}", execution.getId());
				execution.setExecutionStatus(ExecutionStatus.TRIGGERED);
			} else {
				LOGGER.error("Failed to trigger CI execution for execution ID: {}. Status: {}, Message: {}",
						execution.getId(), response.getExecutionTriggerStatus(), response.getMessage());
				execution.setExecutionStatus(ExecutionStatus.TRIGGER_FAILED);
			}

			executionRepository.save(execution);

		} catch (Exception e) {
			LOGGER.error("Error triggering CI execution for execution ID {}: {}", execution.getId(), e.getMessage());
			execution.setExecutionStatus(ExecutionStatus.TRIGGER_FAILED);
			executionRepository.save(execution);
		}
		return execution.getExecutionStatus();
	}

	private String getImageVersion(String buildName, String matchedPrefix) {
		LOGGER.debug("Processing build name: {} with matched prefix: {}", buildName, matchedPrefix);

		String processedBuildName = buildName;

		// Remove extensions (.img and .tar)
		if (processedBuildName.endsWith(".img")) {
			processedBuildName = processedBuildName.substring(0, processedBuildName.length() - 4);
			LOGGER.debug("Removed .img extension: {}", processedBuildName);
		} else if (processedBuildName.endsWith(".tar")) {
			processedBuildName = processedBuildName.substring(0, processedBuildName.length() - 4);
			LOGGER.debug("Removed .tar extension: {}", processedBuildName);
		}

		// Check and remove suffixes (_DEV_NG or _NG)
		if (processedBuildName.endsWith("_DEV_NG")) {
			processedBuildName = processedBuildName.substring(0, processedBuildName.length() - 8);
			LOGGER.debug("Removed _DEV_NG suffix: {}", processedBuildName);
		} else if (processedBuildName.endsWith("_NG")) {
			processedBuildName = processedBuildName.substring(0, processedBuildName.length() - 3);
			LOGGER.debug("Removed _NG suffix: {}", processedBuildName);
		}

		// Step 4: Remove the prefix
		if (null != matchedPrefix && !matchedPrefix.isEmpty()) {
			processedBuildName = processedBuildName.substring(matchedPrefix.length());
			LOGGER.debug("Removed prefix '{}': {}", matchedPrefix, processedBuildName);
		}

		LOGGER.debug("Final image version: {}", processedBuildName);
		return processedBuildName;
	}

	private String getUpgradeImageFileName(String buildName, String fileExtension) {
		if (buildName.endsWith(fileExtension)) {
			return buildName;
		}
		return buildName + fileExtension;
	}

	/**
	 * Helper method to check if the requested image version is same as the current
	 * device image version
	 * 
	 * @param device                The device object containing connection details
	 * @param requestedImageVersion The image version from the request
	 * @return true if both image versions are same, false otherwise
	 */
	private boolean isSameImageVersion(Device device, String requestedImageVersion) {
		try {
			LOGGER.info("Checking if device {} already has image version: {}", device.getName(), requestedImageVersion);

			String currentDeviceImageName = getImageNameUsingSSH(device);
			LOGGER.info("currentImage Name" + currentDeviceImageName);

			if (currentDeviceImageName == null || currentDeviceImageName.trim().isEmpty()) {
				LOGGER.warn("Could not retrieve current image name from device {}. Assuming different version.",
						device.getName());
				return false;
			}

			LOGGER.info("Current device image version: {}, Requested image version: {}", requestedImageVersion,
					requestedImageVersion);

			boolean isSame = requestedImageVersion != null && requestedImageVersion.equals(currentDeviceImageName);
			LOGGER.info("Image version comparison result: {}", isSame ? "SAME" : "DIFFERENT");

			return isSame;

		} catch (Exception e) {
			LOGGER.error("Error comparing image versions for device {}: {}", device.getName(), e.getMessage());
			return false; // In case of error, assume different version to be safe
		}
	}

	/**
	 * Get current image name from device using SSH
	 * 
	 * @param device The device object containing IP and connection details
	 * @return Current image name from device
	 * @throws UnknownHostException
	 */
	private String getImageNameUsingSSH(Device device) throws UnknownHostException {
		String imageName = "";
		String stbIp = device.getIp(); // Assuming device.getIp() gives the STB IP
		String scriptName = "ssh";

		InetAddress addr = InetAddress.getByName(stbIp.trim());

		if (addr instanceof Inet6Address) {
			scriptName = "sshv6";
		}

		try {
			String[] keyGenCommand = new String[] { "ssh-keygen", "-f", "/root/.ssh/known_hosts", "-R", stbIp };
			LOGGER.debug("Executing SSH keygen command for device {}", device.getName());
			commandExecutor.executeScript(keyGenCommand, 0);
			Thread.sleep(5000);

			String[] command = new String[] { scriptName, "-o", "StrictHostKeyChecking=no", "root@" + stbIp,
					"head /version.txt | grep imagename" };
			LOGGER.debug("Executing SSH command to get image name for device {}", device.getName());
			String output = commandExecutor.executeScript(command, 0);

			if (output != null) {
				LOGGER.debug("SSH output for device {}: {}", device.getName(), output);
				String token = null;
				if (output.contains("imagename:")) {
					token = "imagename:";
				} else if (output.contains("imagename=")) {
					token = "imagename=";
				}

				if (token != null) {
					int indx1 = output.indexOf(token);
					if (indx1 >= 0) {
						indx1 = indx1 + token.length();
						int indx2 = output.length() - 1;
						imageName = output.substring(indx1, indx2).trim();
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error getting image name using SSH for device {}: {}", device.getName(), e.getMessage());
		}
		LOGGER.info("imageName" + imageName);

		return imageName;
	}

	private Map<String, Object> buildFirmwarePayload(String id, String model, String description, String imageName,
			String imageVersion) {

		Map<String, Object> payload = new HashMap<>();
		payload.put("id", id);
		payload.put("updated", System.currentTimeMillis());
		List<String> supportedModelIds = new ArrayList<>();
		supportedModelIds.add(model);
		payload.put("supportedModelIds", supportedModelIds);
		payload.put("firmwareDownloadProtocol", "http");
		payload.put("firmwareFilename", imageName);
		payload.put("firmwareVersion", imageVersion);
		payload.put("description", description);
		payload.put("rebootImmediately", false);

		return payload;
	}

	private ExecutionTriggerDTO executionTriggerPayload(Device device, XconfConfig xconfConfig,
			ExecutionDTO execTriggerDTO) {
		List<String> deviceList = new ArrayList<>();
		deviceList.add(device.getName());
		ExecutionTriggerDTO executionTriggerDTO = new ExecutionTriggerDTO();
		executionTriggerDTO.setTestType("CI");
		executionTriggerDTO.setDeviceList(deviceList);
		executionTriggerDTO.setTestSuite(device.getDeviceSuites());
		// if script list is empty or null set the script list as empty list
		if (executionTriggerDTO.getScriptList() == null) {
			executionTriggerDTO.setScriptList(new ArrayList<>());
		}
		executionTriggerDTO.setUser("ci-tdk-user");
		executionTriggerDTO.setCategory(device.getCategory().toString());
		executionTriggerDTO.setExecutionName("CI_Execution_" + System.currentTimeMillis());
		executionTriggerDTO.setRepeatCount(1);
		executionTriggerDTO.setRerunOnFailure(false);
		executionTriggerDTO.setDeviceLogsNeeded(false);
		executionTriggerDTO.setPerformanceLogsNeeded(false);
		executionTriggerDTO.setDiagnosticLogsNeeded(false);
		executionTriggerDTO.setIndividualRepeatExecution(false);
		return executionTriggerDTO;
	}

}
