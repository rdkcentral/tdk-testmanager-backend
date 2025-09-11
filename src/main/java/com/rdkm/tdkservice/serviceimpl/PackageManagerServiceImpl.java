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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.enums.DeviceStatus;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.exception.UserInputException;
import com.rdkm.tdkservice.model.Device;
import com.rdkm.tdkservice.repository.DeviceRepositroy;
import com.rdkm.tdkservice.response.PackageResponse;
import com.rdkm.tdkservice.service.IPackageManagerService;
import com.rdkm.tdkservice.service.utilservices.ScriptExecutorService;
import com.rdkm.tdkservice.util.Constants;

@Service
public class PackageManagerServiceImpl implements IPackageManagerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PackageManagerServiceImpl.class);

	@Autowired
	DeviceRepositroy deviceRepository;

	@Autowired
	DeviceStatusService deviceStatusService;

	@Autowired
	private ScriptExecutorService scriptExecutorService;

	/**
	 * Creates a package for the specified device.
	 *
	 * @param type   the type of the package to be created
	 * @param device the name of the device for which the package is to be created
	 * @return a CreatePackageResponse object containing the status and logs of the
	 *         package creation process
	 * @throws UserInputException if the specified device is not found
	 */

	@Override
	public PackageResponse createPackage(String type, String device) {

		LOGGER.info("Creating package for device " + device);
		PackageResponse createPackageResponse = new PackageResponse();
		boolean isPackageCreation = true;
		Device deviceObj = validateDeviceAndSoc(device);

		// Determine script and package folder based on type
		String scriptName = getScriptFile(type, false, isPackageCreation);
		String packageFolder = getPackageFolder(type);
		String packagePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + packageFolder;
		File packageFolderFile = new File(packagePath);
		// Check and create the folder if it doesn't exist
		if (!packageFolderFile.exists()) {
			if (!packageFolderFile.mkdirs()) {
				LOGGER.error("Failed to create directory {}", packageFolderFile);
				throw new TDKServiceException("Failed to create directory: " + packageFolderFile);
			}
			LOGGER.info("Created directory {}", packageFolderFile);
		}

		String shellScriptPath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + scriptName;
		File createTdkPackageFile = new File(shellScriptPath);
		String createTdkPackageFilePath = createTdkPackageFile.getParent();
		String createTdkPackageFileName = createTdkPackageFile.getName();

		StringBuilder commandBuilder = new StringBuilder();
		commandBuilder.append("cd ").append(createTdkPackageFilePath).append(" && ./").append(createTdkPackageFileName)
				.append(" ").append(deviceObj.getSoc().getName().toLowerCase());
		String[] command = { "sh", "-c", commandBuilder.toString() };

		String outputData;
		try {
			outputData = scriptExecutorService.executeScript(command, 60);
		} catch (Exception e) {
			LOGGER.error("Error executing script", e);
			return null;
		}

		Pattern pattern = Pattern.compile("Created (VTS_Package|TDK_Package)_.*\\.(tgz|tar\\.gz) successfully");
		if (pattern.matcher(outputData).find()) {
			createPackageResponse.setLogs(outputData);
			createPackageResponse.setStatusCode(HttpStatus.OK.value());
			LOGGER.info("Package created successfully");
		} else {
			createPackageResponse.setLogs(outputData);
			createPackageResponse.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
		}
		return createPackageResponse;
	}

	/**
	 * Retrieves a list of available packages for a given device.
	 *
	 * @param device the name of the device for which to retrieve available packages
	 * @return a list of available package names, or null if no packages are found
	 * @throws UserInputException if the specified device is not found
	 */
	@Override
	public List<String> getAvailablePackages(String type, String device) {

		LOGGER.info("Getting available packages for device " + device);
		Device deviceObj = validateDeviceAndSoc(device);
		// Determine package folder based on type
		String packageFolder = getPackageFolder(type);

		String tdkPackagesLocation = AppConfig.getBaselocation() + "/" + packageFolder + "/"
				+ deviceObj.getSoc().getName().toLowerCase();
		File file = new File(tdkPackagesLocation);
		if (!file.exists()) {
			LOGGER.error("No packages found for the device");
			return null;
		}
		return List.of(file.list());

	}

	/**
	 * Uploads a package file for a specified device.
	 *
	 * @param uploadFile the MultipartFile to be uploaded, must be a .tar.gz file
	 * @param device     the name of the device for which the package is being
	 *                   uploaded
	 * @return true if the package is uploaded successfully, false otherwise
	 * @throws UserInputException if the file format is invalid or the device is not
	 *                            found
	 */
	@Override
	public boolean uploadPackage(String type, MultipartFile uploadFile, String device) {
		LOGGER.info("Uploading package for device {}", device);

		// Add validation that upoaded file must be .tar.gz
		String fileName = uploadFile.getOriginalFilename();
		isValidFileType(type, fileName);

		//Validation added to upload only packages that are applicable to particular device soc
		Device deviceObj = validateDeviceAndSoc(device);
		String regex = "(?i)" + type + "_Package_" + deviceObj.getSoc().getName().toLowerCase() + "_.*$";
		if (!fileName.matches(regex)) {
			LOGGER.error("Invalid file name pattern. Expected: {}", regex);
			throw new UserInputException(
					"Please upload a valid package file.Package file uploaded not suited for this device");
		}
		// Determine package folder based on type
		String packageFolder = getPackageFolder(type);

		String tdkPackagesLocation = AppConfig.getBaselocation() + "/" + packageFolder + "/"
				+ deviceObj.getSoc().getName().toLowerCase();
		File directory = new File(tdkPackagesLocation);
		if (!directory.exists()) {
			if (!directory.mkdirs()) {
				LOGGER.error("Failed to create directory {}", tdkPackagesLocation);
				return false;
			}
		}
		if (fileName == null || fileName.isEmpty()) {
			LOGGER.error("Invalid file name");
			return false;
		}
		File destination = new File(directory, fileName);
		try {
			uploadFile.transferTo(destination);
			LOGGER.info("Package uploaded successfully to {}", destination.getAbsolutePath());
			return true;
		} catch (Exception e) {
			LOGGER.error("Error uploading the package", e);
			return false;
		}

	}

	/**
	 * Installs a specified package on a given device.
	 *
	 * @param device      the name of the device on which the package is to be
	 *                    installed
	 * @param packageName the name of the package to be installed
	 * @return the output of the script execution
	 * @throws UserInputException        if the device is not found or is offline
	 * @throws ResourceNotFoundException if the package or the installation script
	 *                                   is not found
	 * @throws RuntimeException          if there is an error executing the script
	 */
	@Override
	public PackageResponse installPackage(String type, String device, String packageName) {
		LOGGER.info("Installing package {} of type {} on device {}", packageName, type, device);
		boolean isPackageInstallation = true;
		Device deviceObj = validateDeviceAndSoc(device);

		DeviceStatus deviceStatus = deviceStatusService.fetchDeviceStatus(deviceObj);
		if (deviceStatus == DeviceStatus.FREE) {
			deviceStatusService.setDeviceStatus(DeviceStatus.IN_USE, deviceObj.getName());
		} else if (deviceStatus == DeviceStatus.NOT_FOUND) {
			LOGGER.error("Device is offline");
			throw new UserInputException("Device " + device + " is down");
		} else {
			LOGGER.error("Device is not available");
			throw new UserInputException("Device " + device + " is not available for update");
		}

		// Determine script and package folder based on type
		String scriptName = getScriptFile(type, isPackageInstallation, false);
		String packageFolder = getPackageFolder(type);

		String scriptPath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + scriptName;
		if (!new File(scriptPath).exists()) {
			LOGGER.error("Script not found");
			throw new ResourceNotFoundException("Script ", scriptPath);
		}

		String tdkPackagesLocation = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + packageFolder
				+ Constants.FILE_PATH_SEPERATOR + deviceObj.getSoc().getName().toLowerCase()
				+ Constants.FILE_PATH_SEPERATOR + packageName;
		File packageFile = new File(tdkPackagesLocation);
		if (!packageFile.exists()) {
			LOGGER.error("Package not found");
			throw new ResourceNotFoundException("Package ", packageName);
		}

		String remoteFilePath = "/opt/TDK/logs/tdk_agent.log";
		String deviceIp = deviceObj.getIp();
		String scpOption = "-O";

		String sshOptions = "-o StrictHostKeyChecking=no";
		// sshpass command to bypass password that entered manually
		String sshPass = "sshpass";
		String password = ""; // Your password here
		String user = "root";
		String userPassword = "root";
		String vtsPackageCommand = "\"find / -maxdepth 1 -name 'VTS_Package' -type d -cmin -5\"";
		String tdkPackageCommand = "systemctl status tdk | grep 'Active: active (running)'";
		String fncsCommand = "command -v tdk_mediapipelinetests";
		String[] copyPackageCommand = { sshPass, "-p", password, "scp", scpOption, tdkPackagesLocation,
				user + "@" + deviceIp + ":/" };
		String[] copyScriptCommand = { sshPass, "-p", password, "scp", scpOption, scriptPath,
				user + "@" + deviceIp + ":/" };
		String[] executeScriptCommand = { sshPass, "-p", userPassword, "ssh", sshOptions, user + "@" + deviceIp,
				"mkdir -p $(dirname " + remoteFilePath + ") && sh /" + scriptName + " \"" + packageName + "\" > "
						+ remoteFilePath
				// No single quotes around the remote command
		};
		String[] logsCommand = { sshPass, "-p", userPassword, "ssh", "-o", "StrictHostKeyChecking=no",
				user + "@" + deviceIp, "/bin/cat " + remoteFilePath };

		String[] vtsPackageVerificationCommand = { sshPass, "-p", password, "ssh", user + "@" + deviceIp,
				vtsPackageCommand };
		String[] tdkPackageVerificationCommand = { sshPass, "-p", password, "ssh", user + "@" + deviceIp,
				tdkPackageCommand };
		String[] fncsVerificationCommand = { sshPass, "-p", password, "ssh", user + "@" + deviceIp, fncsCommand };

		LOGGER.info("copyPackageCommand: " + Arrays.toString(copyPackageCommand));
		LOGGER.info("copyScriptCommand: " + Arrays.toString(copyScriptCommand));
		LOGGER.info("executeScriptCommand: " + Arrays.toString(executeScriptCommand));
		LOGGER.info("logsCommand: " + Arrays.toString(logsCommand));
		LOGGER.info("vtsPackageVerificationCommand: " + Arrays.toString(vtsPackageVerificationCommand));
		LOGGER.info("tdkPackageVerificationCommand: " + Arrays.toString(tdkPackageVerificationCommand));
		try {
			
			// Execute the commands to copy the package file to the device root folder
			scriptExecutorService.executeScript(copyPackageCommand, 300);
			// Execute the commands to copy the shell script file to the device root folder
			scriptExecutorService.executeScript(copyScriptCommand, 300);
			//Execute the shellscript in device and writes the logs to the directory /opt/TDK/logs/tdk_agent.log
			scriptExecutorService.executeScript(executeScriptCommand, 120);			
			//cat output of the script execution logs
			String output = scriptExecutorService.executeScript(logsCommand, 60);
			LOGGER.info("Script output: {}", output);
			PackageResponse installPackageResponse = new PackageResponse();
			if ("TDK".equalsIgnoreCase(type)) {
				if (packageName.contains("fncs") || packageName.contains("FNCS")) {
					// If package is fncs then we need to verify fncs installation
					String fncsVerification = scriptExecutorService.executeScript(fncsVerificationCommand, 60);
					if (fncsVerification != null && !fncsVerification.isEmpty()) {
						String message = "\nTDK Package installed successfully.";
						output = output + message;
						installPackageResponse.setStatusCode(HttpStatus.OK.value());
						installPackageResponse.setLogs(output);
					} else {
						String errorMessage = "\n Error Occured While Installation";
						output = output + errorMessage;
						installPackageResponse.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
						installPackageResponse.setLogs(output);
					}

				} else if (packageName.contains("tdk") || packageName.contains("TDK")) {
					//Checks whether tdk agent status is Active
					String tdkVerification = scriptExecutorService.executeScript(tdkPackageVerificationCommand, 0);
					if (tdkVerification != null && !tdkVerification.isEmpty()) {
						LOGGER.info("TDK Package installed successfully" + tdkVerification);
						String message = "\nTDK Package installed successfully.";
						LOGGER.info("Message: " + output + message);
						output = output + message;
						installPackageResponse = new PackageResponse();
						installPackageResponse.setStatusCode(HttpStatus.OK.value());
						installPackageResponse.setLogs(output);

					} else {
						String errorMessage = "\n Error Occured While Installation";
						output = output + errorMessage;
						installPackageResponse = new PackageResponse();
						installPackageResponse.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
						installPackageResponse.setLogs(output);
					}

				}

			} else if ("VTS".equalsIgnoreCase(type)) {
				// After VTS package installation we can see VTS_Package folder in the device
				String vtsVerification = scriptExecutorService.executeScript(vtsPackageVerificationCommand, 30);
				if (vtsVerification != null && vtsVerification.isEmpty()) {
					String message = "\nVTS Package installed successfully.";
					output = output + message;
					installPackageResponse = new PackageResponse();
					installPackageResponse.setStatusCode(HttpStatus.OK.value());
					installPackageResponse.setLogs(output);
				} else {
					String errorMessage = "\n Error Occured While Installation";
					output = output + errorMessage;
					installPackageResponse = new PackageResponse();
					installPackageResponse.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
					installPackageResponse.setLogs(output);
				}

			}
			return installPackageResponse;

		} catch (Exception e) {
			LOGGER.error("Error executing script", e);
			return null;

		} finally {
			deviceStatusService.fetchAndUpdateDeviceStatus(deviceObj);
		}

	}

	/**
	 * Uploads a generic package file for a specified device.
	 *
	 * @param uploadFile the MultipartFile to be uploaded
	 * @param device     the name of the device for which the package is being
	 *                   uploaded
	 * @return true if the package is uploaded successfully, false otherwise
	 * @throws UserInputException if the file format is invalid or the device is not
	 *                            found
	 */
	@Override
	public boolean uploadGenericPackage(String type, MultipartFile uploadFile, String device) {
		LOGGER.info("Uploading generic package for device {}", device);

		// Validate file name and type
		String fileName = uploadFile.getOriginalFilename();
		isValidFileType(type, fileName);
		String regex = "^VTS_Package_\\d+.*$";
		if (type.equalsIgnoreCase(Constants.TDK) && !fileName.contains("Generic")) {
			LOGGER.error("Invalid file format for TDK package");
			throw new UserInputException("Please upload a generic package file");
		} else if (type.equalsIgnoreCase(Constants.VTS) && !fileName.matches(regex)) {
			LOGGER.error("Invalid file format for VTS package");
			throw new UserInputException("Please upload a generic package file");
		}
		// Determine package folder based on type
		String packageFolder = getPackageFolder(type);

		// Construct package directory path
		String packageLocation = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + packageFolder;
		File directory = new File(packageLocation);

		// Check and create directory if it doesn't exist
		if (!directory.exists() && !directory.mkdirs()) {
			LOGGER.error("Failed to create directory {}", packageLocation);
			throw new TDKServiceException("Failed to create directory: " + packageLocation);
		}

		// Transfer file to the destination
		File destination = new File(directory, fileName);
		try {
			uploadFile.transferTo(destination);
			LOGGER.info("Package uploaded successfully to {}", destination.getAbsolutePath());
			return true;
		} catch (Exception e) {
			LOGGER.error("Error uploading the package", e);
			return false;
		}
	}

	/**
	 * Retrieves the package folder based on the package type.
	 *
	 * @param type the type of the package
	 * @return the package folder name
	 * @throws UserInputException if the package type is invalid
	 */
	private String getPackageFolder(String type) {

		if (Constants.TDK.equalsIgnoreCase(type)) {
			return "tdk_packages";
		} else if (Constants.VTS.equalsIgnoreCase(type)) {
			return "vts_packages";
		} else {
			LOGGER.error("Invalid package type");
			throw new UserInputException("Invalid package type: " + type);
		}
	}

	/**
	 * Retrieves the script file name based on the package type and installation
	 * status.
	 *
	 * @param type                  the type of the package
	 * @param isPackageInstallation true if it's a package installation, false if
	 *                              it's a package creation
	 * @return the script file name
	 */
	private String getScriptFile(String type, boolean isPackageInstallation, boolean isPackageCreation) {

		switch (type.toUpperCase()) {
		case Constants.TDK:
			return isPackageInstallation ? "InstallTDKPackage.sh" : "createTDKPackage.sh";
		case Constants.VTS:
			return isPackageInstallation ? "InstallVTSPackage.sh" : "createVTSPackage.sh";
		default:
			LOGGER.error("Invalid package type: {}", type);
			throw new UserInputException("Invalid package type: " + type);
		}
	}

	/**
	 * Validates the device and its SoC.
	 *
	 * @param device the name of the device to validate
	 * @return the Device object if valid
	 * @throws UserInputException if the device is not found or its SoC is invalid
	 */
	private Device validateDeviceAndSoc(String device) {
		Device deviceObj = deviceRepository.findByName(device);
		if (deviceObj == null) {
			LOGGER.error("Device not found");
			throw new UserInputException("Device " + device + " not found");
		}
		String socName = deviceObj.getSoc() != null ? deviceObj.getSoc().getName() : null;
		if (socName == null || socName.isEmpty()) {
			LOGGER.error("Soc name not found for the device");
			throw new UserInputException("Soc name not found for this device");
		}
		return deviceObj;
	}

	/**
	 * Validates the file type based on the package type and file name.
	 *
	 * @param type     the type of the package
	 * @param fileName the name of the file to validate
	 * @return true if the file type is valid, false otherwise
	 * @throws UserInputException if the file name is invalid or the file format is
	 *                            incorrect
	 */
	private boolean isValidFileType(String type, String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			LOGGER.error("Invalid file name");
			throw new UserInputException("File name cannot be empty");
		}
		if (Constants.TDK.equalsIgnoreCase(type) && !fileName.endsWith(".tar.gz")) {
			LOGGER.error("Invalid file format for TDK");
			throw new UserInputException("Invalid package format. TDK package must be in .tar.gz format");
		}
		if (Constants.VTS.equalsIgnoreCase(type) && !fileName.endsWith(".tgz")) {
			LOGGER.error("Invalid file format for VTS");
			throw new UserInputException("Invalid package format. VTS package must be in .tgz format");
		}
		return true;
	}

}
