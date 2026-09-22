/*
* If not stated otherwise in this file or this component's LICENSE file the
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.enums.DeviceStatus;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.exception.UserInputException;
import com.rdkm.tdkservice.model.Device;
import com.rdkm.tdkservice.repository.DeviceRepositroy;
import com.rdkm.tdkservice.response.InstallJobStatusResponse;
import com.rdkm.tdkservice.response.PackageResponse;
import com.rdkm.tdkservice.service.IPackageManagerService;
import com.rdkm.tdkservice.service.utilservices.ScriptExecutorService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.Utils;

@Service
public class PackageManagerServiceImpl implements IPackageManagerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PackageManagerServiceImpl.class);

	@Autowired
	DeviceRepositroy deviceRepository;

	@Autowired
	DeviceStatusService deviceStatusService;

	@Autowired
	private ScriptExecutorService scriptExecutorService;

	@Autowired
	private DeviceConfigService deviceConfigService;

	private final ExecutorService installJobExecutor = Executors.newCachedThreadPool();
	private final Map<String, InstallJob> installJobs = new ConcurrentHashMap<>();
	private static final long INSTALL_JOB_RETENTION_MINUTES = 30;
	private static final String JOB_STATUS_RUNNING = "RUNNING";
	private static final String JOB_STATUS_SUCCESS = "SUCCESS";
	private static final String JOB_STATUS_FAILED = "FAILED";
	private static final String JOB_ERROR_MESSAGE = "Installation failed. Please retry or contact support.";

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

		// VTS (video specific) package creation is not applicable for Broadband devices
		if (Category.RDKB.equals(deviceObj.getCategory()) && Constants.VTS.equalsIgnoreCase(type)) {
			LOGGER.error("VTS package creation is not supported for Broadband devices");
			createPackageResponse.setStatusCode(HttpStatus.NOT_IMPLEMENTED.value());
			createPackageResponse.setLogs("Create Package is not supported for Broadband devices");
			return createPackageResponse;
		}
		// Determine script and package folder based on type
		String scriptName = getScriptFile(type, false, isPackageCreation, deviceObj.getCategory());
		String packageFolder = getPackageFolder(type, deviceObj.getCategory());
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
		if (isScriptMissing(shellScriptPath)) {
			LOGGER.error("Create package script {} not found", shellScriptPath);
			createPackageResponse.setStatusCode(HttpStatus.NOT_IMPLEMENTED.value());
			createPackageResponse.setLogs("Create Package is not supported for this device");
			return createPackageResponse;
		}
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
		validatePackageTypeSupported(type, deviceObj);
		// Determine package folder based on type
		String packageFolder = getPackageFolder(type, deviceObj.getCategory());

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

		// Validation added to upload only packages that are applicable to particular
		// device soc
		Device deviceObj = validateDeviceAndSoc(device);
		validatePackageTypeSupported(type, deviceObj);
		String socName = deviceObj.getSoc().getName().toLowerCase();
		String regex = "(?i)" + type + "_Package_(NPVS_)?" + socName + "_.*$";
		if (!fileName.matches(regex)) {
			LOGGER.error("Invalid file name pattern. Expected: {}", regex);
			throw new UserInputException(
					"Please upload a valid package file.Package file uploaded not suited for this device");
		}
		// Determine package folder based on type
		String packageFolder = getPackageFolder(type, deviceObj.getCategory());

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
	 * @throws UserInputException if the device is not found or is offline
	 * @throws RuntimeException   if there is an error executing the script
	 */
	public PackageResponse installPackage(String type, String device, String packageName,
			Consumer<String> phaseListener) {
		LOGGER.info("Installing package {} of type {} on device {}", packageName, type, device);
		boolean isPackageInstallation = true;
		Device deviceObj = validateDeviceAndSoc(device);
		validatePackageTypeSupported(type, deviceObj);
		// so an install already in progress must be detected from the persisted status
		// instead
		if (DeviceStatus.IN_USE.equals(deviceObj.getDeviceStatus())) {
			LOGGER.error("Device {} is already in use by another operation", device);
			throw new UserInputException("Device " + device + " is currently in use by another operation");
		}

		DeviceStatus deviceStatus = deviceStatusService.fetchDeviceStatus(deviceObj);
		if (deviceStatus == DeviceStatus.FREE) {
			deviceStatusService.setDeviceStatus(DeviceStatus.IN_USE, deviceObj.getName());
		} else if (deviceStatus == DeviceStatus.NOT_FOUND) {
			LOGGER.warn("Device {} reported as offline; proceeding with install attempt anyway", device);
			deviceStatusService.setDeviceStatus(DeviceStatus.IN_USE, deviceObj.getName());
		} else {
			LOGGER.error("Device is not available");
			throw new UserInputException("Device " + device + " is not available for update");
		}

		// Determine script and package folder based on type
		String scriptName = getScriptFile(type, isPackageInstallation, false, deviceObj.getCategory());
		String packageFolder = getPackageFolder(type, deviceObj.getCategory());

		String scriptPath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + scriptName;
		if (isScriptMissing(scriptPath)) {
			LOGGER.error("Install package script {} not found", scriptPath);
			deviceStatusService.fetchAndUpdateDeviceStatus(deviceObj);
			PackageResponse installPackageResponse = new PackageResponse();
			installPackageResponse.setStatusCode(HttpStatus.NOT_IMPLEMENTED.value());
			installPackageResponse.setLogs("Install Package is not supported for this device yet");
			return installPackageResponse;
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
		// sshpass command to bypass password that entered manually
		String sshPass = "sshpass";
		String password = ""; // Your password here
		String user = "root";
		String userPassword = "root";
		// Device-specific install directory (falls back to "/" if not configured)
		String installBasePath = resolveInstallBasePath(deviceObj, type);
		LOGGER.info("Resolved install base path {} for device {} and type {}", installBasePath, device, type);
		String vtsPackageCommand = "test -f " + installBasePath + "vts_installed && echo VTS_INSTALLED";

		String tdkPackageCommand = "(systemctl status tdk | grep 'Active: active (running)') || (test -f /opt/TDK/.no_tdk_agent && echo '.no_tdk_agent file found')";
		String npvsCommand = "command -v tdk_mediapipelinetests";
		String[] copyPackageCommand = { sshPass, "-p", password, "scp", scpOption,
				"-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null",
				tdkPackagesLocation, user + "@" + deviceIp + ":" + installBasePath };
		String[] copyScriptCommand = { sshPass, "-p", password, "scp", scpOption,
				"-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null",
				scriptPath, user + "@" + deviceIp + ":" + installBasePath };
		String[] executeScriptCommand = { sshPass, "-p", userPassword, "ssh",
				"-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null",
				user + "@" + deviceIp,
				"mkdir -p " + installBasePath + " $(dirname " + remoteFilePath + ") && bash " + installBasePath
						+ scriptName + " \"" + packageName + "\" > " + remoteFilePath + " 2>&1"
		};
		String[] logsCommand = { sshPass, "-p", userPassword, "ssh",
				"-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null",
				user + "@" + deviceIp, "/bin/cat " + remoteFilePath };

		String[] vtsPackageVerificationCommand = { sshPass, "-p", password, "ssh",
				"-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null",
				user + "@" + deviceIp, vtsPackageCommand };
		String[] tdkPackageVerificationCommand = { sshPass, "-p", password, "ssh",
				"-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null",
				user + "@" + deviceIp, tdkPackageCommand };
		String[] npvsVerificationCommand = { sshPass, "-p", password, "ssh",
				"-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null",
				user + "@" + deviceIp, npvsCommand };
		LOGGER.info("copyPackageCommand: " + Arrays.toString(copyPackageCommand));
		LOGGER.info("copyScriptCommand: " + Arrays.toString(copyScriptCommand));
		LOGGER.info("executeScriptCommand: " + Arrays.toString(executeScriptCommand));
		LOGGER.info("logsCommand: " + Arrays.toString(logsCommand));
		LOGGER.info("vtsPackageVerificationCommand: " + Arrays.toString(vtsPackageVerificationCommand));
		LOGGER.info("tdkPackageVerificationCommand: " + Arrays.toString(tdkPackageVerificationCommand));
		try {

			// Execute the commands to copy the package file to the device root folder
			reportPhase(phaseListener, "COPYING_PACKAGE");
			scriptExecutorService.executeScript(copyPackageCommand, 300);
			// Execute the commands to copy the shell script file to the device root folder
			reportPhase(phaseListener, "COPYING_SCRIPT");
			scriptExecutorService.executeScript(copyScriptCommand, 300);
			// Execute the shellscript in device and writes the logs to the directory
			// /opt/TDK/logs/tdk_agent.log
			reportPhase(phaseListener, "INSTALLING");
			scriptExecutorService.executeScript(executeScriptCommand, 120);
			reportPhase(phaseListener, "VERIFYING");
			PackageResponse installPackageResponse = new PackageResponse();
			if ("TDK".equalsIgnoreCase(type)) {
				if (packageName.contains("npvs") || packageName.contains("NPVS")) {
					// If package is npvs then we need to verify npvs installation
					String output = scriptExecutorService.executeScript(logsCommand, 90);
					String npvsVerification = scriptExecutorService.executeScript(npvsVerificationCommand, 60);
					if (npvsVerification != null && !npvsVerification.isEmpty()) {
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
					// Checks whether tdk agent status is Active
					String output = scriptExecutorService.executeScript(logsCommand, 90);
					String tdkVerification = scriptExecutorService.executeScript(tdkPackageVerificationCommand, 60);
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
				// After a successful VTS install, InstallVTSPackage.sh creates a vts_installed
				// marker file
				String output = scriptExecutorService.executeScript(logsCommand, 90);
				String vtsVerification = scriptExecutorService.executeScript(vtsPackageVerificationCommand, 30);
				if (vtsVerification != null && !vtsVerification.isEmpty()) {
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
	 * Reports an installation phase transition for progress polling; no-op without
	 * a listener.
	 * 
	 * @param phaseListener the listener to report phase transitions to
	 * @param phase         the current installation phase
	 * 
	 * @return void
	 */
	private void reportPhase(Consumer<String> phaseListener, String phase) {
		if (phaseListener != null) {
			phaseListener.accept(phase);
		}
	}

	/**
	 * Starts an asynchronous install package job.
	 *
	 * @param type        the type of package to install
	 * @param device      the target device for the installation
	 * @param packageName the name of the package to install
	 * @return the job ID for tracking the installation progress
	 */
	@Override
	public String startInstallPackageJob(String type, String device, String packageName) {
		String jobId = UUID.randomUUID().toString();
		installJobs.put(jobId, new InstallJob());
		installJobExecutor.submit(() -> runInstallJob(jobId, type, device, packageName));
		return jobId;
	}

	/**
	 * Retrieves the status of an asynchronous install package job.
	 *
	 * @param jobId the ID of the job to query
	 * @return the current status of the install job, or null if the job does not
	 *         exist
	 */
	@Override
	public InstallJobStatusResponse getInstallPackageJobStatus(String jobId) {
		InstallJob job = installJobs.get(jobId);
		if (job == null) {
			return null;
		}

		String currentStatus = job.status;
		InstallJobStatusResponse response = new InstallJobStatusResponse();
		response.setJobId(jobId);
		response.setPhase(job.phase);
		response.setStatus(currentStatus);
		if (!JOB_STATUS_RUNNING.equals(currentStatus)) {
			PackageResponse result = new PackageResponse();
			result.setStatusCode(job.statusCode != null ? job.statusCode : HttpStatus.SERVICE_UNAVAILABLE.value());
			result.setLogs(job.logs != null ? job.logs : "");
			response.setResult(result);
		}
		return response;
	}

	/**
	 * Executes the asynchronous install package job.
	 *
	 * @param jobId       the ID of the job to run
	 * @param type        the type of package to install
	 * @param device      the target device for the installation
	 * @param packageName the name of the package to install
	 */
	private void runInstallJob(String jobId, String type, String device, String packageName) {
		InstallJob job = installJobs.get(jobId);
		if (job == null) {
			LOGGER.warn("Attempted to process missing install job {}", jobId);
			return;
		}
		try {
			PackageResponse response = installPackage(type, device, packageName, phase -> job.phase = phase);
			Integer resultCode = response != null ? response.getStatusCode() : null;
			String resultLogs = response != null ? response.getLogs() : null;
			job.statusCode = resultCode;
			job.logs = resultLogs;
			job.status = (resultCode != null && resultCode == HttpStatus.OK.value()) ? JOB_STATUS_SUCCESS
					: JOB_STATUS_FAILED;
		} catch (Exception e) {
			LOGGER.error("Error during asynchronous package installation for job {}", jobId, e);
			job.statusCode = HttpStatus.SERVICE_UNAVAILABLE.value();
			job.logs = JOB_ERROR_MESSAGE;
			job.status = JOB_STATUS_FAILED;
		} finally {
			job.finishedAtMillis = System.currentTimeMillis();
		}
	}

	/**
	 * Prunes finished install jobs older than the retention window to avoid an
	 * unbounded in-memory job map.
	 * 
	 * @param none
	 * @return void
	 */
	@Scheduled(fixedRate = 30, timeUnit = TimeUnit.MINUTES)
	public void cleanupFinishedInstallJobs() {
		long cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(INSTALL_JOB_RETENTION_MINUTES);
		Iterator<Map.Entry<String, InstallJob>> iterator = installJobs.entrySet().iterator();
		int removed = 0;
		while (iterator.hasNext()) {
			InstallJob job = iterator.next().getValue();
			if (job.finishedAtMillis > 0 && job.finishedAtMillis < cutoff) {
				iterator.remove();
				removed++;
			}
		}
		if (removed > 0) {
			LOGGER.debug("Cleaned up {} finished install job(s)", removed);
		}
	}

	/**
	 * Represents an asynchronous install package job.
	 */
	private static final class InstallJob {
		/**
		 * The current phase of the install job.
		 */
		private volatile String phase = "QUEUED";
		/**
		 * The current status of the install job.
		 */
		private volatile String status = "RUNNING";
		/**
		 * The HTTP status code resulting from the install job.
		 */
		private volatile Integer statusCode;
		/**
		 * The logs generated during the install job.
		 */
		private volatile String logs;
		/**
		 * The timestamp when the install job finished.
		 */
		private volatile long finishedAtMillis;
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
		Device deviceObj = validateDevice(device);
		validatePackageTypeSupported(type, deviceObj);
		if (!fileName.matches(getGenericFileRegex(type))) {
			LOGGER.error("Invalid file format for {} generic package", type);
			throw new UserInputException("Please upload a generic package file");
		}
		// Determine package folder based on type; generic package sits directly in it
		String packageFolder = getPackageFolder(type, deviceObj.getCategory());

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
	 * Checks whether a generic package of the given type already exists for the
	 * device's category.
	 *
	 * @param type   the type of the package (TDK/VTS)
	 * @param device the device used to resolve the category-specific folder
	 * @return true if a generic package is present, false otherwise
	 */
	@Override
	public boolean isGenericPackagePresent(String type, String device) {
		LOGGER.info("Checking generic package presence for device {}", device);
		Device deviceObj = validateDevice(device);
		validatePackageTypeSupported(type, deviceObj);
		String packageFolder = getPackageFolder(type, deviceObj.getCategory());

		String packageLocation = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + packageFolder;
		File directory = new File(packageLocation);
		String[] files = directory.exists() ? directory.list() : null;
		if (files == null) {
			return false;
		}
		String genericFileRegex = getGenericFileRegex(type);
		for (String fileName : files) {
			if (fileName.matches(genericFileRegex)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Resolves the generic package file name pattern for the given type.
	 *
	 * @param type the type of the package (TDK/VTS)
	 * @return the regex matching a valid generic package file name for that type
	 */
	private String getGenericFileRegex(String type) {
		if (Constants.VTS.equalsIgnoreCase(type)) {
			return "^Generic_VTS_Package.*\\.tgz$";
		}
		return "^Generic_TDK_Package.*\\.tar\\.gz$";
	}

	/**
	 * Resolves the package folder based on the package type and device category.
	 * RDKV TDK packages live under "tdk_packages", RDKB TDK packages live under a
	 * dedicated "tdkb_packages" folder, and VTS packages live under "vts_packages".
	 *
	 * @param type     the type of the package
	 * @param category the category (RDKV/RDKB) of the device
	 * @return the package folder name
	 * @throws UserInputException if the package type is invalid
	 */
	private String getPackageFolder(String type, Category category) {

		if (Constants.TDK.equalsIgnoreCase(type)) {
			return Category.RDKB.equals(category) ? "tdkb_packages" : "tdk_packages";
		} else if (Constants.VTS.equalsIgnoreCase(type)) {
			return "vts_packages";
		} else {
			LOGGER.error("Invalid package type");
			throw new UserInputException("Invalid package type: " + type);
		}
	}

	/**
	 * Retrieves the script file name based on the package type, installation
	 * status and device category. Broadband (RDKB) devices use a dedicated
	 * create package script.
	 *
	 * @param type                  the type of the package
	 * @param isPackageInstallation true if it's a package installation, false if
	 *                              it's a package creation
	 * @param category              the category (RDKV/RDKB) of the device
	 * @return the script file name
	 */
	private String getScriptFile(String type, boolean isPackageInstallation, boolean isPackageCreation,
			Category category) {

		if (Constants.TDK.equalsIgnoreCase(type) && Category.RDKB.equals(category)) {
			if (isPackageCreation) {
				return "createTDKBPackage.sh";
			}
			if (isPackageInstallation) {
				return "InstallTDKBPackage.sh";
			}
		}
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
	 * Resolves the on-device directory the package and install script should be
	 * copied to and executed from. Reads the VTS_BASE_PATH/TDK_BASE_PATH override
	 * from the device's config file (falling back to the device type's config
	 * file), and uses it directly as the install directory. Falls back to "/"
	 * when no override is configured.
	 *
	 * @param device the device the package is being installed on
	 * @param type   the type of the package (TDK/VTS)
	 * @return the base directory (with trailing "/") to install into
	 */
	private String resolveInstallBasePath(Device device, String type) {
		String key = Constants.VTS.equalsIgnoreCase(type) ? Constants.VTS_BASE_PATH_CONFIG_KEY
				: Constants.TDK_BASE_PATH_CONFIG_KEY;
		String override = deviceConfigService.getDeviceConfigPropertyOverride(device, key);
		if (Utils.isEmpty(override)) {
			return Constants.FILE_PATH_SEPERATOR;
		}
		// Use the configured directory as-is, just ensure a single trailing slash
		String normalized = override.trim().replaceAll("/+$", "");
		return Utils.isEmpty(normalized) ? Constants.FILE_PATH_SEPERATOR : normalized + Constants.FILE_PATH_SEPERATOR;
	}

	/**
	 * Validates that VTS (video specific) packages are only requested for RDKV
	 * devices, since VTS packages are not applicable to Broadband (RDKB) devices.
	 *
	 * @param type      the package type
	 * @param deviceObj the device on which the operation is being performed
	 * @throws UserInputException if VTS type is requested for a Broadband device
	 */
	private void validatePackageTypeSupported(String type, Device deviceObj) {
		if (Constants.VTS.equalsIgnoreCase(type) && Category.RDKB.equals(deviceObj.getCategory())) {
			LOGGER.error("VTS package type is not supported for Broadband devices");
			throw new UserInputException("VTS packages are video specific and not supported for Broadband devices");
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
		Device deviceObj = validateDevice(device);
		String socName = deviceObj.getSoc() != null ? deviceObj.getSoc().getName() : null;
		if (socName == null || socName.isEmpty()) {
			LOGGER.error("Soc name not found for the device");
			throw new UserInputException("Soc name not found for this device");
		}
		return deviceObj;
	}

	/**
	 * Validates that the device exists.
	 *
	 * @param device the name of the device to validate
	 * @return the Device object if found
	 * @throws UserInputException if the device is not found
	 */
	private Device validateDevice(String device) {
		Device deviceObj = deviceRepository.findByName(device);
		if (deviceObj == null) {
			LOGGER.error("Device not found");
			throw new UserInputException("Device " + device + " not found");
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

	/**
	 * A missing script means this create/install flow isn't available for the
	 * device yet.
	 */
	private boolean isScriptMissing(String scriptPath) {
		return !new File(scriptPath).exists();
	}

}
