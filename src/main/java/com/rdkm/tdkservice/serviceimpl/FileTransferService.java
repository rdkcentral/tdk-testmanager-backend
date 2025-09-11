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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.model.Device;
import com.rdkm.tdkservice.model.Execution;
import com.rdkm.tdkservice.model.ExecutionDevice;
import com.rdkm.tdkservice.model.ExecutionResult;
import com.rdkm.tdkservice.model.Module;
import com.rdkm.tdkservice.repository.DeviceRepositroy;
import com.rdkm.tdkservice.repository.ExecutionResultRepository;
import com.rdkm.tdkservice.service.IFileService;
import com.rdkm.tdkservice.service.IScriptService;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.service.utilservices.ScriptExecutorService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.Utils;

/**
 * This method is used to transfer the log files for thunder enabled/disabled
 * devices
 */
@Service
public class FileTransferService implements IFileService {

	public static final Logger LOGGER = LoggerFactory.getLogger(FileTransferService.class);

	@Autowired
	private DeviceRepositroy deviceRepository;

	@Autowired
	private CommonService commonService;

	@Autowired
	private ScriptExecutorService scriptExecutorService;

	@Autowired
	private ExecutionResultRepository executionResultRepository;

	@Autowired
	private IScriptService scriptService;

	/**
	 * This method is to transfer the device logs for the execution
	 * 
	 * @param execution       - the execution entity
	 * @param executionResult - the execution result entity
	 * @param device          - the device entity
	 */
	public void transferDeviceLogs(Execution execution, ExecutionResult executionResult, Device device) {
		LOGGER.info("Starting transferDeviceLogs for the execution: {} and execution result {}", execution.getName(),
				executionResult.getId().toString());
		if (device.isThunderEnabled()) {
			transferDeviceLogsForThunderEnabled(execution, executionResult, device);
		} else {
			transferDeviceLogsForTDKEnabled(execution, executionResult, device);
		}
	}

	/**
	 * This method is to transfer the crash logs for the execution
	 * 
	 * @param execution       - the execution entity
	 * @param executionResult - the execution result entity
	 * @param device          - the device entity
	 */
	public void transferCrashLogs(Execution execution, ExecutionResult executionResult, Device device) {
		LOGGER.info("Starting transferCrashLogs for the execution: {} and execution result {}", execution.getName(),
				executionResult.getId().toString());
		if (device.isThunderEnabled()) {
			transferCrashLogsForThunderEnabled(execution, executionResult, device);
		} else {
			transferCrashLogsForTDKEnabled(execution, executionResult, device);
		}
	}

	/**
	 * This method is to transfer the crash logs for Thunder enabled devices, this
	 * is same like device log transfer and is based on the crash logs path
	 * configured with modules
	 * 
	 * @param Execution
	 * @param Execution Result
	 * @param device
	 */
	public void transferCrashLogsForThunderEnabled(Execution execution, ExecutionResult executionResult,
			Device device) {
		try {
			LOGGER.info("Starting transferCrashLogsForThunderEnabled for the execution: {} and execution result {}",
					execution.getName(), executionResult.getId().toString());
			String scriptName = executionResult.getScript();
			Module module = scriptService.getModuleByScriptName(scriptName);
			if (module == null) {
				LOGGER.error("Module not found for script: {}", scriptName);
				return;
			}
			List<String> crashLogFilesPathsInDevice = new ArrayList<>(module.getCrashLogFiles());
			if (crashLogFilesPathsInDevice.isEmpty()) {
				LOGGER.warn("No crash log files found for the module: {}", module.getName());
				return;
			}
			String baseLogPath = commonService.getBaseLogPath();
			String crashLogFilesPath = commonService.getCrashLogsPathForTheExecution(execution.getId().toString(),
					executionResult.getId().toString(), baseLogPath);
			new File(crashLogFilesPath).mkdirs();
			for (String crashLogFilePathInDevice : crashLogFilesPathsInDevice) {
				transferLogFilesForThunder(crashLogFilePathInDevice, device, crashLogFilesPath);
			}
		} catch (Exception e) {
			LOGGER.error("Error in transferCrashLogsForThunderEnabled: {}", e.getMessage(), e);
		}
	}

	/**
	 * This method is to transfer the device logs for Thunder enabled devices
	 * 
	 * @param moduleName
	 * @param deviceIp
	 * @param execId
	 * @param execResultId
	 */
	public void transferDeviceLogsForThunderEnabled(Execution execution, ExecutionResult executionResult,
			Device device) {
		try {
			LOGGER.info("Starting transferDeviceLogsForThunderEnabled for the execution: {} and execution result {}",
					execution.getName(), executionResult.getId().toString());
			String scriptName = executionResult.getScript();
			Module module = scriptService.getModuleByScriptName(scriptName);
			if (module == null) {
				LOGGER.error("Module not found for script: {}", scriptName);
				return;
			}
			List<String> deviceLogFiles = new ArrayList<>(module.getLogFileNames());
			if (deviceLogFiles.isEmpty()) {
				LOGGER.warn("No log files found for the module: {}", module.getName());
				return;
			}
			// Base log path
			String baseLogPath = commonService.getBaseLogPath();
			// Custom path for this execution
			String deviceLogFilesPath = commonService.getDeviceLogsPathForTheExecution(execution.getId().toString(),
					executionResult.getId().toString(), baseLogPath);
			new File(deviceLogFilesPath).mkdirs();
			for (String name : deviceLogFiles) {
				// The absolute path of the file in the device
				for (String deviceLogPath : deviceLogFiles) {
					transferLogFilesForThunder(deviceLogPath, device, deviceLogFilesPath);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error in transferDeviceLogsForThunderEnabled: {}", e.getMessage(), e);
		}
	}

	/**
	 * Processes a log file by executing a transfer script and handling any errors
	 * that occur.
	 *
	 * @param filePathInDevice The absolute path of the log in device
	 * @param device           The device entity containing device details.
	 * @param logFilesPath     The path where the log files should be stored.
	 */
	private void transferLogFilesForThunder(String filePathInDevice, Device device, String logFilesPath) {
		try {

			// The python library file for transfering logs in thunder enabled
			File logTransferScriptFile = new File(AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
					+ Constants.FILE_TRANSFER_SCRIPT_RDKSERVICE);
			String logTransferScriptFilePath = logTransferScriptFile.getAbsolutePath();
			String[] fnameArray = filePathInDevice.split("/");

			String fileName = fnameArray[fnameArray.length - 1];
			if (!logTransferScriptFilePath.isEmpty()) {
				String[] cmd = commandForTransferThunderEnabledLogFiles(commonService.getPythonCommandFromConfig(),
						logTransferScriptFilePath, device.getIp(), filePathInDevice, logFilesPath, fileName);
				scriptExecutorService.executeScript(cmd, 60);
			} else {
				LOGGER.warn("Device log transfer script file path is empty, skipping execution.");
			}
		} catch (Exception e) {
			LOGGER.error("Error processing log file {}: {}", filePathInDevice, e.getMessage(), e);
		}
	}

	/**
	 * Method to transfer Diagnosis logs
	 * 
	 * @param execution       - Execution object
	 * @param executionResult - Execution Result object
	 * @param device          - the device object
	 */
	public void transferDiagnosisLogs(Execution execution, ExecutionResult executionResult, Device device) {
		LOGGER.info("Starting transferDiagnosisLogs for the execution: {} and execution result {}", execution.getName(),
				executionResult.getId().toString());
		try {
			File diagnosisLogsPath = new File(
					AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + Constants.RDK_DIAGNOSIS_LOG_SCRIPT);
			String diagnosisLogScriptFilePath = diagnosisLogsPath.getAbsolutePath();

			String[] command = { commonService.getPythonCommandFromConfig(), diagnosisLogScriptFilePath,
					device.getIp() };
			String outputData = scriptExecutorService.executeScript(command, 60);

			String baseLogPath = commonService.getBaseLogPath();
			String diagnosisLogsFilePath = commonService.getDeviceLogsPathForTheExecution(execution.getId().toString(),
					executionResult.getId().toString(), baseLogPath);
			if (!new File(diagnosisLogsFilePath).exists()) {
				new File(diagnosisLogsFilePath).mkdirs();
			}
			// TODO - Filename no need of execution result ID
			String diagnosisLogsFileName = executionResult.getScript() + executionResult.getId().toString()
					+ "_RdkCertificationDiagnosislogs.txt";

			File diagnosisLogsFile = new File(diagnosisLogsFilePath + diagnosisLogsFileName);
			if (!diagnosisLogsFile.exists()) {
				diagnosisLogsFile.createNewFile();
			}
			BufferedWriter writer = new BufferedWriter(new FileWriter(diagnosisLogsFile));
			writer.write(outputData);
			writer.close();

		} catch (Exception e) {
			LOGGER.error("Error occurred during diagnosis logs transfer script execution: {}", e.getMessage(), e);

		}
	}

	/**
	 * This method is to transfer the device logs for TDK enabled devices
	 * 
	 * @param moduleName
	 * @param deviceIp
	 * @param execId
	 * @param execResultId
	 */
	public void transferDeviceLogsForTDKEnabled(Execution execution, ExecutionResult executionResult, Device device) {
		try {
			String baseLogPath = commonService.getBaseLogPath();
			String scriptName = executionResult.getScript();
			Module module = scriptService.getModuleByScriptName(scriptName);
			if (module == null) {
				LOGGER.error("Module not found for script: {}", scriptName);
				return;
			}
			Set<String> deviceLogFiles = module.getLogFileNames().stream().map(Object::toString)
					.collect(Collectors.toSet());
			if (deviceLogFiles.isEmpty()) {
				LOGGER.warn("No log files found for the module: {}", module.getName());
				return;
			}
			String deviceDestinationFilePath = commonService.getDeviceLogsPathForTheExecution(
					execution.getId().toString(), executionResult.getId().toString(), baseLogPath);
			String transferScriptName = getFileTransferScriptNameForTDKEnabled();
			String fileTransferScriptPath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
					+ transferScriptName;
			String tmUrl = commonService.getTMUrlFromConfigFile();
			for (String logFileName : deviceLogFiles) {
				processTDKLogFile(logFileName, device, deviceDestinationFilePath, fileTransferScriptPath, tmUrl,
						executionResult.getId().toString());
			}
		} catch (Exception e) {
			LOGGER.error("Error in transferDeviceLogsForTDKEnabled: {}", e.getMessage(), e);
		}
	}

	/**
	 * This method is to transfer the crash logs for TDK enabled devices
	 * 
	 * @param moduleName
	 * @param deviceIp
	 * @param execId
	 * @param execResultId
	 */
	public void transferCrashLogsForTDKEnabled(Execution execution, ExecutionResult executionResult, Device device) {
		try {
			String baseLogPath = commonService.getBaseLogPath();
			String scriptName = executionResult.getScript();
			Module module = scriptService.getModuleByScriptName(scriptName);
			if (module == null) {
				LOGGER.error("Module not found for script: {}", scriptName);
				return;
			}
			Set<String> crashLogFiles = module.getCrashLogFiles().stream().map(Object::toString)
					.collect(Collectors.toSet());
			if (crashLogFiles.isEmpty()) {
				LOGGER.warn("No crash log files found for the module: {}", module.getName());
				return;
			}
			String crashDestinationFilePath = commonService.getCrashLogsPathForTheExecution(
					execution.getId().toString(), executionResult.getId().toString(), baseLogPath);
			String transferScriptName = getFileTransferScriptNameForTDKEnabled();
			String fileTransferScriptPath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
					+ transferScriptName;
			String tmUrl = commonService.getTMUrlFromConfigFile();
			for (String logFileName : crashLogFiles) {
				processTDKLogFile(logFileName, device, crashDestinationFilePath, fileTransferScriptPath, tmUrl,
						executionResult.getId().toString());
			}
		} catch (Exception e) {
			LOGGER.error("Error in transferCrashLogsForTDKEnabled: {}", e.getMessage(), e);
		}
	}

	/**
	 * Processes a TDK log file by sanitizing the file name, building a command
	 * list, executing the script, and copying the logs into the specified
	 * directory.
	 *
	 * @param logFileName         The name of the log file to process.
	 * @param device              The device entity containing device details.
	 * @param destinationFilePath The path where the logs should be copied.
	 * @param scriptPath          The path to the script used for file transfer.
	 * @param tmUrl               The URL from the configuration file.
	 * @param executionResultId   The ID of the execution result.
	 */
	private void processTDKLogFile(String logFileName, Device device, String destinationFilePath, String scriptPath,
			String tmUrl, String executionResultId) {
		try {
			String sanitizedFileName = logFileName.replaceAll("//", "_").replaceAll("/", "_");
			List<String> cmdList = new ArrayList<>(Arrays.asList(commonService.getPythonCommandFromConfig(), scriptPath,
					device.getIp(), device.getAgentMonitorPort(), logFileName,
					executionResultId + "_" + sanitizedFileName, tmUrl));
			scriptExecutorService.executeScript(cmdList.toArray(new String[0]), 60);
			Thread.sleep(1000);
			copyLogsIntoDir(destinationFilePath, executionResultId);
		} catch (Exception e) {
			LOGGER.error("Error processing log file {}: {}", logFileName, e.getMessage(), e);
		}
	}

	/**
	 * This method retrieves device details from a version file associated with a
	 * given execution ID. It constructs the file path using the execution ID,
	 * checks if the file exists, and reads its contents.
	 *
	 * @param executionId The unique identifier for the execution.
	 * @return A string containing the contents of the version file, or an error
	 *         message if the file does not exist or cannot be read.
	 */
	public String getDeviceDetailsFromVersionFile(String executionId) {
		LOGGER.info("Getting device details from version file for execution ID: {}", executionId);
		String versionFilePath = commonService.getVersionLogFilePathForTheExecution(executionId);
		String versionFileName = executionId + "_version.txt";
		String versionFileAbsolutePath = versionFilePath + versionFileName;

		File versionFile = new File(versionFileAbsolutePath);
		if (!versionFile.exists()) {
			return "Device version details not available";
		}

		StringBuilder deviceDetails = new StringBuilder();

		try (BufferedReader br = new BufferedReader(new FileReader(versionFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				deviceDetails.append(line).append(System.lineSeparator());
			}
		} catch (IOException e) {
			// Log the error and return a user-friendly message
			LOGGER.error("Error reading file: " + e.getMessage());
			return "Error reading version file";
		}

		if (deviceDetails.length() == 0) {
			return "Device version details not available";
		}
		LOGGER.info("Device version details: {}", deviceDetails.toString());

		return deviceDetails.toString();
	}

	/**
	 * This method is to transfer the version.txt file for the device used in the
	 * execution
	 * 
	 * @param executionID - execution ID of the execution entity
	 * @param device      - the device entity
	 */
	public boolean getVersionFileForTheDevice(String executionID, Device device) {
		LOGGER.info("Starting getVersionFileForTheDevice for request: {} and device {}", executionID, device.getIp());
		if (device.isThunderEnabled()) {
			return createVersionFileForThunderEnabled(executionID, device);
		} else {
			return createVersionFileForThunderDisabled(executionID, device);
		}
	}

	/**
	 * This method is to transfer the version.txt file for thunder enabled devices
	 * to the destination location
	 * 
	 * @param executionId
	 * @param deviceIP
	 * @return boolean
	 */
	private boolean createVersionFileForThunderEnabled(String executionId, Device device) {
		LOGGER.info("Creating version file for thunder enabled devices");
		try {

			boolean versionFileStatus = false;

			String versionFilePath = commonService.getVersionLogFilePathForTheExecution(executionId);
			LOGGER.info("Creating version file path: {}", versionFilePath);

			if (createDirectories(versionFilePath)) {

				String versionFileName = executionId + "_version.txt";
				String versionFileAbsolutePath = versionFilePath + versionFileName;

				File versionFile = new File(versionFileAbsolutePath);

				boolean versionFileTransferredStatus = transferFileForThunderEnabled(device.getIp(), versionFilePath,
						versionFileName, Constants.SLASH_VERSION_TXT_FILE, 30);
				LOGGER.info("transferThunderFile status: {}", versionFileTransferredStatus);

				if (!versionFileTransferredStatus) {
					String thunderVersionDetails = retrieveFirmwareVersionForThunderEnabled(device);

					versionFileStatus = createVersionFile(versionFile, thunderVersionDetails);
					LOGGER.info("Version file created: {}", versionFileStatus);
				} else {
					LOGGER.info("Version file transferred successfully.");
					versionFileStatus = true;
				}
			}
			return versionFileStatus;
		} catch (Exception e) {
			LOGGER.error("Exception in createThunderVersionFile", e);
			return false;
		}
	}

	/**
	 * This method is to do version transfer for thunder disabled devices
	 * 
	 * @param executionID - execution ID of the execution entity
	 * @param device      - the device entity
	 * @return boolean status of the version file creation
	 */
	private boolean createVersionFileForThunderDisabled(String executionID, Device device) {
		try {
			String versionFileName = executionID + Constants.UNDERSCORE + "version.txt";
			String versionFilePath = commonService.getVersionLogFilePathForTheExecution(executionID);

			// Build the command list for file transfer
			List<String> cmdList = buildCommandListForFileTransfer(device, "/version.txt", versionFileName);

			// Log the command being executed
			LOGGER.info("Executing command: {}", String.join(" ", cmdList));

			// Execute the script
			String outputData = scriptExecutorService.executeScript(cmdList.toArray(new String[0]), 20);
			LOGGER.info("Script executed successfully. Output data: {}", outputData);

			// Copy version logs into the specified directory
			copyVersionLogsIntoDir(versionFilePath, executionID);

			return true;
		} catch (Exception e) {
			LOGGER.error("Error occurred during version transfer script execution: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Get image name from file
	 * 
	 * @param executionId
	 * @return image name
	 */
	public String getImageName(String executionId) {
		LOGGER.info("Getting image name for executionId: {}", executionId);
		String imageNameValue = "";
		try {
			String filePath = commonService.getVersionLogFilePathForTheExecution(executionId);
			String versionFileName = executionId + Constants.UNDERSCORE + "version.txt";
			filePath = filePath + versionFileName;
			File file = new File(filePath);

			if (file.exists() && file.isFile()) { // Check if it's a file
				try (BufferedReader br = new BufferedReader(new FileReader(file))) {
					String line;
					while ((line = br.readLine()) != null) {
						if (line.startsWith("imagename:")) {
							imageNameValue = line.split(":", 2)[1].trim();
							break; // Exit the loop once the imagename is found
						} else if (line.startsWith("currentFWVersion:")) {
							imageNameValue = line.split(":", 2)[1].trim();
							break; // Exit the loop once the currentFWVersion is found
						}
					}
				}
			} else {
				if (file.exists()) {
					LOGGER.warn("The path points to a directory, not a file: {}", filePath);
				} else {
					LOGGER.warn("File not found: {}", filePath);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error reading image name: {}", e.getMessage(), e);
		}

		// Log the obtained image name value
		if (!imageNameValue.isEmpty()) {
			LOGGER.info("Image name obtained: {}", imageNameValue);
		} else {
			LOGGER.info("No valid image name or firmware version found in file: {}", executionId);
		}

		return imageNameValue;
	}

	/**
	 * Prepare command for file transfer of thunder enabled device logs
	 * 
	 * @param pythonCommand
	 * @param scriptFilePath
	 * @param deviceIP
	 * @param fileNameToBeTransferred
	 * @param destinationPath
	 * @param fileNameToBeSaved
	 * @return Array of the python command
	 */
	private String[] commandForTransferThunderEnabledLogFiles(String pythonCommand, String scriptFilePath,
			String deviceIP, String fileNameToBeTransferred, String destinationPath, String fileNameToBeSaved) {
		LOGGER.debug(
				"Preparing command with parameters: pythonCommand={}, scriptFilePath={}, deviceIP={}, fileNameToBeTransferred={}, destinationPath={}, fileNameToBeSaved={}",
				pythonCommand, scriptFilePath, deviceIP, fileNameToBeTransferred, destinationPath, fileNameToBeSaved);
		return new String[] { pythonCommand, scriptFilePath, deviceIP, Constants.ROOT_STRING, Constants.NONE_STRING,
				fileNameToBeTransferred, destinationPath, fileNameToBeSaved };
	}

	/**
	 * This method is used to get the device log files available for the execution
	 * say wpeframework logs, thunder logs etc.
	 * 
	 * @param executionId
	 * @param executionResId return list of filenames
	 */
	@Override
	public List<String> getDeviceLogFileNames(String executionResId) {
		try {
			LOGGER.debug("Getting log file names with parameters:executionResId={}", executionResId);
			ExecutionResult executionResult = executionResultRepository.findById(UUID.fromString(executionResId))
					.orElseThrow(() -> new ResourceNotFoundException("Execution Result ID ", executionResId));
			String executionId = executionResult.getExecution().getId().toString();
			String baselogpath = commonService.getBaseLogPath();
			String logsDirectory = commonService.getDeviceLogsPathForTheExecution(executionId, executionResId,
					baselogpath);
			List<String> fileNames = commonService.getFilenamesFromDirectory(logsDirectory, executionId);
			LOGGER.debug("Log file names: {}", fileNames);
			return fileNames;
		} catch (Exception e) {
			LOGGER.error("Error in getLogFileNames: {}", e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	/**
	 * This method is used to get the crash log files available for the execution
	 * say wpeframework logs, thunder logs etc.
	 * 
	 * @param executionResultId
	 * @return List of filenames
	 */
	@Override
	public List<String> getCrashLogFileNames(String executionResultId) {
		try {
			LOGGER.debug("Getting crash log file names with parameters:executionResultId={}", executionResultId);
			ExecutionResult executionResult = executionResultRepository.findById(UUID.fromString(executionResultId))
					.orElseThrow(() -> new ResourceNotFoundException("Execution Result ID ", executionResultId));
			String executionId = executionResult.getExecution().getId().toString();
			String baseLogPath = commonService.getBaseLogPath();
			String logsDirectory = commonService.getCrashLogsPathForTheExecution(executionId, executionResultId,
					baseLogPath);
			List<String> fileNames = commonService.getFilenamesFromDirectory(logsDirectory, executionId);
			LOGGER.debug("Crash log file names: {}", fileNames);
			return fileNames;
		} catch (Exception e) {
			LOGGER.error("Error in getCrashLogFileNames: {}", e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	/**
	 * Method to download a log file
	 *
	 * @param executionId    The execution ID
	 * @param executionResId The execution resource ID
	 * @param fileName       The name of the file to download
	 * @return Resource
	 */
	@Override
	public Resource downloadDeviceLogFile(String executionResId, String fileName) {
		LOGGER.info("Inside downloadDeviceLogFile method with fileName: {}", fileName);
		ExecutionResult executionResult = executionResultRepository.findById(UUID.fromString(executionResId))
				.orElseThrow(() -> new ResourceNotFoundException("Execution Result ID ", executionResId));
		String executionId = executionResult.getExecution().getId().toString();
		// Get log file names for the execution
		List<String> logFileNames = this.getDeviceLogFileNames(executionResId);
		if (!logFileNames.contains(fileName)) {
			LOGGER.error("Log file not found: {}", fileName);
			throw new ResourceNotFoundException("Log file", fileName);
		}

		// Determine the path for the log file
		String baseLogPath = commonService.getBaseLogPath();
		String logsDirectory = commonService.getDeviceLogsPathForTheExecution(executionId, executionResId, baseLogPath);

		// Use the helper method to get the resource
		return getFileAsResource(logsDirectory, fileName);
	}

	@Override
	public Resource downloadCrashLogFile(String executionResId, String fileName) {
		LOGGER.info("Inside downloadCrashLogFile method with fileName: {}", fileName);
		ExecutionResult executionResult = executionResultRepository.findById(UUID.fromString(executionResId))
				.orElseThrow(() -> new ResourceNotFoundException("Execution Result ID ", executionResId));
		String executionId = executionResult.getExecution().getId().toString();
		// Get log file names for the execution
		List<String> logFileNames = this.getCrashLogFileNames(executionResId);
		if (!logFileNames.contains(fileName)) {
			LOGGER.error("Log file not found: {}", fileName);
			throw new ResourceNotFoundException("Log file", fileName);
		}
		String baseLogPath = commonService.getBaseLogPath();
		String logsDirectory = commonService.getCrashLogsPathForTheExecution(executionId, executionResId, baseLogPath);
		return getFileAsResource(logsDirectory, fileName);
	}

	/**
	 * Download all log files This method is used to download all the log files for
	 * the given execution ID
	 * 
	 * @param
	 * @param executionResultId return byte[]
	 */
	@Override
	public byte[] downloadAllDeviceLogFiles(String executionResultId) throws IOException {
		LOGGER.info("Inside downloadDeviceLogFile method with executionResultId: {}", executionResultId);
		ExecutionResult executionResult = executionResultRepository.findById(UUID.fromString(executionResultId))
				.orElseThrow(() -> new ResourceNotFoundException("Execution Result ID ", executionResultId));
		String executionId = executionResult.getExecution().getId().toString();

		List<String> logFileNames = this.getDeviceLogFileNames(executionResultId);

		if (logFileNames.isEmpty()) {
			throw new FileNotFoundException("No log files found for the given execution ID and executionRes ID.");
		}

		String baselogpath = commonService.getBaseLogPath();
		String logsDirectory = commonService.getDeviceLogsPathForTheExecution(executionId, executionResultId,
				baselogpath);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(baos)) {
			for (String fileName : logFileNames) {
				String filePath = logsDirectory + Constants.FILE_PATH_SEPERATOR + fileName;

				File file = new File(filePath);
				if (file.exists() && file.isFile()) {
					try (FileInputStream fis = new FileInputStream(file)) {
						ZipEntry zipEntry = new ZipEntry(file.getName());
						zos.putNextEntry(zipEntry);

						byte[] buffer = new byte[1024];
						int bytesRead;
						while ((bytesRead = fis.read(buffer)) != -1) {
							zos.write(buffer, 0, bytesRead);
						}
						zos.closeEntry();
					}
				}
			}
		}
		return baos.toByteArray();
	}

	/**
	 * This method is transfer Agent logs
	 * 
	 * @param executionID
	 * @param executionResultID
	 * @param Device
	 */
	public void transferAgentLogs(Device device, String executionID, String executionResultID) {
		try {
			String baseLogPath = commonService.getBaseLogPath();

			// The script which transfers the Agent console log
			String scriptName = this.getAgentConsoleFileTransferScriptName();
			String scriptPath = Paths.get(AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + scriptName)
					.toString();
			LOGGER.info("Script path for Agent log transfer: {}", scriptPath);

			// The file name with which the Agent log is going to be uploaded to the TM
			String agentlogTransferFileName = executionResultID + Constants.UNDERSCORE
					+ Constants.AGENT_CONSOLE_LOG_FILE;

			// The command for the transfer
			// eg: python3 callConsoleLogUpload.py <IP> <Port> "AgentConsole.log"
			// "AgentConsole.log" <TM base URL>
			List<String> cmdList = buildCommandListForAgentLogTransfer(scriptPath, device.getIp(),
					device.getAgentMonitorPort(), Constants.AGENT_CONSOLE_LOG_FILE, agentlogTransferFileName, device);

			scriptExecutorService.executeScript(cmdList.toArray(new String[0]), 60);

			// The folder specific to the execution to copy the Agent log from the
			// uploadedLogs
			// location where file is pushed from the device
			String deviceLogsPath = commonService.getDeviceLogsPathForTheExecution(executionID, executionResultID,
					baseLogPath);
			copyAgentconsoleLogIntoDir(deviceLogsPath, executionResultID);

		} catch (Exception e) {
			LOGGER.error("Error during log transfer: {}", e.getMessage(), e);
		}
	}

	/**
	 * Get Agent console file transfer script name
	 * 
	 * @param device -- the device entity
	 * @return Agent console file transfer pyhton script name
	 */
	private String getAgentConsoleFileTransferScriptName() {
		// TODO CONSOLE_FILE_TRANSFER_SCRIPT is for tftp, now we are not implementing
		return Constants.CONSOLE_FILE_UPLOAD_SCRIPT;
	}

	/**
	 * Copy agent console log into the specified directory
	 *
	 * @param logTransferFilePath
	 * @param executionResultId
	 */
	private void copyAgentconsoleLogIntoDir(String logTransferFilePath, String executionResultId) {
		try {
			// Get the base file path for upload log
			String getBaseFilePathForUploadLogAPI = commonService.getBaseFilePathForUploadLogAPI();
			File logDir = new File(getBaseFilePathForUploadLogAPI);
			LOGGER.info("Base directory for upload log: {}", logDir);
			if (logDir.isDirectory()) {
				for (File file : Objects.requireNonNull(logDir.listFiles())) {
					if (file.getName().contains(Constants.AGENT_CONSOLE_LOG_FILE)) {
						String[] logFileName = file.getName().split(Constants.UNDERSCORE);
						if (logFileName.length >= 2 && executionResultId.equals(logFileName[0])) {
							new File(logTransferFilePath).mkdirs();
							File logTransferPath = new File(logTransferFilePath);
							if (file.exists()) {
								file.renameTo(new File(logTransferPath, Constants.AGENT_CONSOLE_LOG_FILE));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Build command list This method is to build the command list for agent log
	 * transfer
	 * 
	 * @param scriptPath
	 * @param deviceIP
	 * @param agentMonitorPort
	 * @param sourceFileName
	 * @param targetFileName
	 * @param device
	 * @return List of command
	 */
	private List<String> buildCommandListForAgentLogTransfer(String scriptPath, String deviceIP,
			String agentMonitorPort, String sourceFileName, String targetFileName, Device device) {
		List<String> cmdList = new ArrayList<>();
		cmdList.add(commonService.getPythonCommandFromConfig());
		cmdList.add(scriptPath);
		cmdList.add(deviceIP);
		cmdList.add(agentMonitorPort);
		cmdList.add(sourceFileName);
		cmdList.add(targetFileName);
		String urlFromConfigFile = commonService.getTMUrlFromConfigFile();
		cmdList.add(urlFromConfigFile);
		LOGGER.info("Script cmdList path: {}", cmdList);
		return cmdList;
	}

	/**
	 * This method is for uploading the files to the destination location via api
	 * call
	 * 
	 * @param logFile  - log file
	 * @param fileName - name in which the file needs to be saved
	 * @return
	 */
	@Override
	public String uploadLogs(MultipartFile logFile, String fileName) {
		String data = "";
		LOGGER.info("Starting uploadLogs method.");
		try {
			if (logFile != null && !logFile.isEmpty()) {
				// Read the uploaded file content
				List<String> fileContent = readUploadedFileContent(logFile);

				// Get the real path for logs
				String uploadLogPath = commonService.getBaseFilePathForUploadLogAPI();
				File logFilePath = new File(uploadLogPath + Constants.FILE_PATH_SEPERATOR + fileName);

				// Ensure the directories exist
				File logDir = new File(uploadLogPath);
				if (!logDir.exists()) {
					logDir.mkdirs();
					LOGGER.info("Created directories for log file path: {}", uploadLogPath);
				}

				// Write content to the file
				writeContentToFile(logFilePath, fileContent);

				// Combine the file content into a single string
				data = String.join("\n", fileContent);
			} else {
				LOGGER.warn("No file was uploaded or the file is empty.");
			}
		} catch (IOException e) {
			LOGGER.error("IOException: An I/O error occurred while processing the file.", e);
		} catch (MaxUploadSizeExceededException e) {
			LOGGER.error("MaxUploadSizeExceededException: Uploaded file size exceeds the maximum limit.", e);
		} catch (Exception e) {
			LOGGER.error("uploadLogs ERROR: {}", e.getMessage(), e);
		}
		LOGGER.info("uploadLogs method completed.");
		return data;
	}

	/**
	 * This method is used to upload file content
	 * 
	 * @param uploadedFile
	 * @return
	 * @throws IOException
	 */
	private List<String> readUploadedFileContent(MultipartFile uploadedFile) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(uploadedFile.getInputStream()))) {
			return reader.lines().toList();
		}
	}

	/**
	 * Write content to the file
	 * 
	 * @param logFile
	 * @param fileContent
	 * @throws IOException
	 */
	private void writeContentToFile(File logFile, List<String> fileContent) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
			for (String log : fileContent) {
				writer.write(log);
				writer.newLine();
			}
		}
	}

	/**
	 * This method is to build command list for file transfer for thunder disabled.
	 * After the python script is executed, the file will be transferred from the
	 * box to the server via upload API.
	 * 
	 * @param device         - Device entity
	 * @param sourceFilePath - source file path in the device
	 * @param targetFileName - name, where the source file needs to be saved when
	 *                       uplaoded
	 * @return List of command
	 */
	private List<String> buildCommandListForFileTransfer(Device device, String sourceFilePath, String targetFileName) {
		List<String> cmdList = new ArrayList<>();
		cmdList.add(commonService.getPythonCommandFromConfig());
		cmdList.add(
				AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + getFileTransferScriptNameForTDKEnabled());
		cmdList.add(device.getIp());
		cmdList.add(device.getAgentMonitorPort());
		cmdList.add(sourceFilePath);
		cmdList.add(targetFileName);
		String configFileUrl = commonService.getTMUrlFromConfigFile();
		cmdList.add(configFileUrl);

		// Log the command list
		LOGGER.info("Thunder disabled command: {}", String.join(" ", cmdList));

		return cmdList;
	}

	/**
	 * Get file transfer script name based on the device type
	 * 
	 * @param - the device entity
	 * @return the file transfer script name
	 */
	private String getFileTransferScriptNameForTDKEnabled() {
		String scriptName = Constants.FILE_UPLOAD_SCRIPT;
		return scriptName;
	}

	/**
	 * This method is to retrieve the version file for thunder disabled devices
	 * 
	 * @param logTransferFilePath
	 * @param executionId
	 */
	public void copyVersionLogsIntoDir(String logTransferFilePath, String executionId) {
		try {
			String baseLogUploadPathForUploadAPI = commonService.getBaseFilePathForUploadLogAPI();

			File logDir = new File(baseLogUploadPathForUploadAPI);
			if (!logDir.isDirectory()) {
				LOGGER.warn("Log directory does not exist: {}", baseLogUploadPathForUploadAPI);
				return;
			}

			Files.walk(Paths.get(baseLogUploadPathForUploadAPI)).filter(Files::isRegularFile)
					.forEach(filePath -> transferThunderDisabledVersionFile(filePath.toFile(), logTransferFilePath,
							executionId));
		} catch (IOException e) {
			LOGGER.error("Error while accessing log directory: {}", e.getMessage(), e);
		}
	}

	/**
	 * This method is to transfer the version.txt file for thunder disabled devices
	 * to the destination location
	 * 
	 * @param file
	 * @param logTransferFilePath
	 * @param executionId
	 */
	private void transferThunderDisabledVersionFile(File file, String logTransferFilePath, String executionId) {
		LOGGER.info("Processing file: {}", file.getName());
		if (!file.getName().contains("version.txt")) {
			return;
		}

		String[] logFileNameParts = file.getName().split("_");
		if (logFileNameParts.length > 1 && executionId != null && executionId.toString().equals(logFileNameParts[0])) {

			String versionFileName = executionId + "_version.txt";
			File logTransferDir = new File(logTransferFilePath);

			if (logTransferDir.mkdirs() || logTransferDir.exists()) {
				File movedFile = new File(logTransferFilePath, versionFileName);
				if (file.renameTo(movedFile)) {
					LOGGER.info("File moved successfully: {}", file.getName());
				} else {
					LOGGER.warn("Failed to move file: {}", file.getName());
				}
			} else {
				LOGGER.error("Failed to create log transfer directory: {}", logTransferFilePath);
			}
		}
	}

	/**
	 * This method is to transfer the file for thunder enabled devices
	 * 
	 * @param deviceIP
	 * @param destinationPath
	 * @param fileNameToBeSaved
	 * @param fileNameToBeTransferred
	 * @param waittime
	 * @return boolean
	 */
	private boolean transferFileForThunderEnabled(String deviceIP, String destinationPath, String fileNameToBeSaved,
			String fileNameToBeTransferred, int waittime) {
		boolean fileTransferredStatus = false;
		boolean fileRenamedStatus = false;

		try {
			String transferScriptFilePath = getScriptFilePathForThunderEnabledFiletransfer();
			String pythonCommand = commonService.getPythonCommandFromConfig();
			String[] cmd = commandForTransferThunderEnabledLogFiles(pythonCommand, transferScriptFilePath, deviceIP,
					fileNameToBeTransferred, destinationPath, fileNameToBeSaved);
			String exitCode = scriptExecutorService.executeScript(cmd, waittime);
			LOGGER.info("Thunder enabled file transfer Script exit code: {}", exitCode);
			fileTransferredStatus = verifyFileTransfer(destinationPath, fileNameToBeTransferred);
			LOGGER.info("Execution instance directory path: {}", fileTransferredStatus);
			if (fileTransferredStatus) {
				fileRenamedStatus = renameTransferredFile(destinationPath, fileNameToBeTransferred, fileNameToBeSaved);
			}

		} catch (Exception e) {
			LOGGER.error("Error: {}", e.getMessage());
			e.printStackTrace();
		}

		return fileRenamedStatus;
	}

	/**
	 * This method is to get the script file path for thunder enabled file transfer
	 * 
	 * @return String
	 */
	private String getScriptFilePathForThunderEnabledFiletransfer() {
		return AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + Constants.FILE_TRANSFER_SCRIPT_RDKSERVICE;
	}

	/**
	 * This method is to verify the file transfer
	 * 
	 * @param destinationPath
	 * @param fileNameToBeTransferred
	 * @return boolean
	 */
	private boolean verifyFileTransfer(String destinationPath, String fileNameToBeTransferred) {
		String transferredFilePath = Paths.get(destinationPath, fileNameToBeTransferred).toString();
		LOGGER.info("Transferred file path: {}", transferredFilePath);
		File transferredFile = new File(transferredFilePath);
		boolean fileTransferredStatus = transferredFile.isFile();
		LOGGER.info("File transferred status: {}", fileTransferredStatus);
		return fileTransferredStatus;
	}

	/**
	 * THis method is to rename the transferred file
	 * 
	 * @param destinationPath
	 * @param fileNameToBeTransferred
	 * @param fileNameToBeSaved
	 * @return boolean
	 */
	private boolean renameTransferredFile(String destinationPath, String fileNameToBeTransferred,
			String fileNameToBeSaved) {
		String fileNameToBeSavedFullPath = destinationPath + Constants.FILE_PATH_SEPERATOR + fileNameToBeSaved;
		LOGGER.info("File name to be saved full path: {}", fileNameToBeSavedFullPath);
		File transferredFile = new File(destinationPath + Constants.FILE_PATH_SEPERATOR + fileNameToBeTransferred);
		boolean fileRenamedStatus = transferredFile.renameTo(new File(fileNameToBeSavedFullPath));
		LOGGER.info("File renamed status: {}", fileRenamedStatus);
		return fileRenamedStatus;
	}

	/**
	 * This method is to create directories
	 * 
	 * @param path
	 * @return boolean
	 */
	private boolean createDirectories(String path) {
		File dir = new File(path);
		if (!dir.exists() && dir.mkdirs()) {
			LOGGER.info("Directory created successfully: {}", path);
			return true;
		}
		return false;
	}

	/**
	 * This method is to create the version file
	 * 
	 * @param versionFile
	 * @param content
	 * @return boolean
	 */
	private boolean createVersionFile(File versionFile, String content) {
		try {
			if (versionFile.createNewFile()) {
				try (FileWriter fr = new FileWriter(versionFile)) {
					fr.write(content);
					return true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * This method is to retrieve the firmware version for thunder enabled devices
	 * 
	 * @param deviceIP
	 * @return String
	 */
	public String retrieveFirmwareVersionForThunderEnabled(Device device) {
		String thunderVersionDetailsFormatted = "";
		String thunderPort = device.getThunderPort() != null ? device.getThunderPort() : Constants.THUNDER_DEFAULT_PORT;
		String deviceIP = device.getIp();

		// TODO: Fix this code with HttpClient
		try {
			String urlString = "http://" + deviceIP + ":" + thunderPort + "/jsonrpc";
			URL url = new URL(urlString);
			URLConnection con = url.openConnection();
			HttpURLConnection http = (HttpURLConnection) con;

			http.setRequestMethod("POST");
			http.setDoOutput(true);
			String jsonInputString = "{\"jsonrpc\": \"2.0\",\"id\": 1234567890,\"method\": \"org.rdk.System.1.getDownloadedFirmwareInfo\"}";
			byte[] out = jsonInputString.getBytes(StandardCharsets.UTF_8);
			int length = out.length;
			http.setFixedLengthStreamingMode(length);
			http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			http.setRequestProperty("Accept", "application/json");

			con.getOutputStream().write(out);
			http.connect();

			try (InputStream iStream = con.getInputStream();
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(iStream, StandardCharsets.UTF_8))) {
				StringBuilder result = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					result.append(line);
				}
				String response = result.toString();

				JSONObject jsonObject = new JSONObject(response);
				JSONObject resultJsonObject = jsonObject.getJSONObject("result");
				if (resultJsonObject.has("currentFWVersion")) {
					String currentFWVersion = resultJsonObject.getString("currentFWVersion");
					thunderVersionDetailsFormatted = "currentFWVersion: " + currentFWVersion;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return thunderVersionDetailsFormatted;
	}

	/**
	 * This method is to copy the device logs into the specified directory
	 * 
	 * @param deviceLogDestPath
	 * @param executionId
	 * @param executionResultId
	 */
	public void copyLogsIntoDir(String deviceLogDestPath, String executionResultId) {
		try {
			// Get the base log path
			String baseLogPath = commonService.getBaseFilePathForUploadLogAPI();
			File logDir = new File(baseLogPath);
			if (logDir.isDirectory()) {
				for (File file : Objects.requireNonNull(logDir.listFiles())) {
					// Skip unwanted files
					if (file.getName().matches(
							".*(version\\.txt|benchmark\\.log|memused\\.log|cpu\\.log|AgentConsoleLog\\.log).*")) {
						continue;
					}

					String[] logFileName = file.getName().split("_");
					if (logFileName.length >= 3 & executionResultId.toString().equals(logFileName[0])) {

						String sanitizedFileName = file.getName().replaceAll("\\s", "").replaceAll("\\$:", "Undefined");

						// Create log transfer directory if it doesn't exist
						new File(deviceLogDestPath).mkdirs();
						File logTransferPath = new File(deviceLogDestPath);

						// Move the file
						boolean fileMoved = file.renameTo(new File(logTransferPath, sanitizedFileName));
						if (fileMoved) {
							LOGGER.info("File moved successfully: {}", sanitizedFileName);
						} else {
							LOGGER.warn("Failed to move file: {}", sanitizedFileName);
						}
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error while copying logs: {}", e.getMessage(), e);
		}
	}

	/**
	 * This method get the agent log file content
	 * 
	 * @param executionResultId
	 * @return String
	 */
	@Override
	public String getAgentLogContent(String executionResultId) {
		LOGGER.info("Getting agent log content for executionResultId: {}", executionResultId);

		// Retrieve execution result details
		ExecutionResult executionResult = executionResultRepository.findById(UUID.fromString(executionResultId))
				.orElseThrow(() -> new ResourceNotFoundException("Execution Result ID", executionResultId));
		String executionId = executionResult.getExecution().getId().toString();

		// Construct the path to the log file
		String baseLogPath = commonService.getBaseLogPath();
		String agentLogsDirectory = commonService.getAgentLogPath(executionId, executionResultId, baseLogPath);

		// Construct the full path to AGENT_CONSOLE_LOG_FILE
		String logFilePath = Paths
				.get(agentLogsDirectory, executionResultId + Constants.UNDERSCORE + Constants.AGENT_CONSOLE_LOG_FILE)
				.toString();

		File file = new File(logFilePath);
		StringBuilder fileContent = new StringBuilder();

		// Check if the file exists and is a file
		if (file.exists() && file.isFile()) {
			try (BufferedReader br = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = br.readLine()) != null) {
					fileContent.append(line).append("\n");
				}
			} catch (IOException e) {
				// Handle IO exceptions and log error
				LOGGER.error("Error reading agent log file: {}", e.getMessage(), e);
				return "Error reading log file";
			}
		} else {
			return "Log file not found";
		}

		return fileContent.toString();
	}

	/**
	 * This method is to download the agent log file
	 * 
	 * @param executionResId
	 * @return Resource
	 */
	@Override
	public Resource downloadAgentLogFile(String executionResId) {
		LOGGER.info("Inside downloadAgentLogFile method ");
		ExecutionResult executionResult = executionResultRepository.findById(UUID.fromString(executionResId))
				.orElseThrow(() -> new ResourceNotFoundException("Execution Result ID ", executionResId));
		String executionId = executionResult.getExecution().getId().toString();

		// Determine the path for the agent log file
		String baseLogPath = commonService.getBaseLogPath();
		String agentLogsDirectory = commonService.getAgentLogPath(executionId, executionResId, baseLogPath);
		String logTransferFileNameForTheExecution = executionResId + Constants.UNDERSCORE
				+ Constants.AGENT_CONSOLE_LOG_FILE;
		// Use the helper method to get the resource
		return getFileAsResource(agentLogsDirectory, logTransferFileNameForTheExecution);
	}

	/**
	 * Helper method to retrieve a file as a Resource.
	 *
	 * @param directoryPath The directory where the file is located.
	 * @param fileName      The name of the file to retrieve.
	 * @return Resource representing the file.
	 */
	private Resource getFileAsResource(String directoryPath, String fileName) {
		Path filePath = Paths.get(directoryPath, fileName);

		// Check if the file exists
		if (!Files.exists(filePath)) {
			LOGGER.error("{} not found: {}", "Log file", fileName);
			throw new ResourceNotFoundException("Log file", fileName);
		}

		// Load the resource
		Resource resource;
		try {
			resource = new UrlResource(filePath.toUri());
		} catch (MalformedURLException e) {
			LOGGER.error("Error loading {}: {}", "Log file", e.getMessage(), e);
			throw new RuntimeException("Error loading " + "Log file", e);
		}

		// Check if the resource exists
		if (resource == null || !resource.exists()) {
			LOGGER.error("{} not found: {}", "Log file", fileName);
			throw new ResourceNotFoundException("Log file", fileName);
		}

		return resource;
	}

	/**
	 * Retrieves the content of a specified log file associated with a given
	 * execution result.
	 *
	 * @param logFileName       the name of the log file to retrieve.
	 * @param executionResultID the ID of the execution result associated with the
	 *                          log file.
	 * @return the content of the specified log file as a String.
	 * @throws ResourceNotFoundException if the execution result ID or log file is
	 *                                   not found.
	 */
	@Override
	public String getAdditionalLogs(String logFileName, String executionResultID) {
		LOGGER.info("Getting log file names with parameters:executionResId={}", executionResultID);
		ExecutionResult executionResult = executionResultRepository.findById(UUID.fromString(executionResultID))
				.orElseThrow(() -> new ResourceNotFoundException("Execution Result ID ", executionResultID));
		String executionId = executionResult.getExecution().getId().toString();
		String baselogpath = commonService.getBaseLogPath();
		String logsDirectory = commonService.getDeviceLogsPathForTheExecution(executionId, executionResultID,
				baselogpath);
		String filePath = logsDirectory + Constants.FILE_PATH_SEPERATOR + logFileName + Constants.LOG_FILE_EXTENSION;

		StringBuilder contentBuilder = new StringBuilder();
		// return the content from the log file
		File logFile = new File(filePath);
		if (logFile.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
				String line;
				while ((line = reader.readLine()) != null) {
					contentBuilder.append(line).append("\n");
				}
			} catch (IOException e) {
				LOGGER.error("Error reading log file: {}", e.getMessage(), e);
			}
		} else {
			LOGGER.error("Log file not found: {}", logFileName);
			throw new ResourceNotFoundException("Log file", logFileName);
		}
		return contentBuilder.toString();

	}

	/**
	 * Moves and renames script-generated files based on the execution ID,
	 * execution device ID, and execution result ID.
	 * The path hardocded in the python scripts are
	 * is baseLogPath + "executionId + "_" + executionDeviceId + "_" +
	 * executionResultId + "_"+
	 * <filename>.<fileExtension>, we need to copy this from this location to the
	 * log location
	 * for that particular script that is where the agent logs and device logs are
	 * stored.
	 * 
	 * @param ExecutionResult executionResult - The execution result object
	 *                        containing
	 *
	 */
	public void moveAndRenameScriptGeneratedFiles(Execution execution, ExecutionResult executionResult,
			ExecutionDevice executionDevice) {
		LOGGER.info("Going to move the script specific Files");
		String baseLogFilePath = commonService.getBaseLogPath();

		String executionId = execution.getId().toString();
		String executionDeviceId = executionDevice.getId().toString();
		String executionResultId = executionResult.getId().toString();
		LOGGER.info(
				"Moving and renaming script-generated files for executionId: {}, executionDeviceId: {}, executionResultId: {}",
				executionId, executionDeviceId, executionResultId);
		// Build the prefix to match files

		String prefix = executionId + "_" + executionDeviceId + "_" + executionResultId;
		// Build the target directory
		String deviceLogsPath = baseLogFilePath + Constants.FILE_PATH_SEPERATOR + Constants.EXECUTION_KEYWORD
				+ Constants.UNDERSCORE + executionId + Constants.FILE_PATH_SEPERATOR + Constants.RESULT
				+ Constants.UNDERSCORE + executionResultId + Constants.FILE_PATH_SEPERATOR + Constants.DEVICE_LOGS
				+ Constants.FILE_PATH_SEPERATOR;

		LOGGER.info("Going to move the files to devicelogs folder: {}", deviceLogsPath);

		File baseDir = new File(baseLogFilePath);
		File targetDir = new File(deviceLogsPath);

		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}

		File[] filesToMove = baseDir.listFiles((dir, name) -> name.startsWith(prefix));
		LOGGER.info("Files to move: {}", Arrays.toString(filesToMove));

		if (filesToMove == null || filesToMove.length == 0) {
			LOGGER.warn("No files found to move with prefix: {}", prefix);
			return;
		}
		if (filesToMove != null) {
			for (File file : filesToMove) {
				String[] parts = file.getName().split("_");
				if (parts.length > 1) {
					int indexOFfilename = parts.length - 1;
					String newFileName = parts[indexOFfilename];
					File targetFile = new File(targetDir, newFileName);
					file.renameTo(targetFile);
				}
			}
		}
	}

}
