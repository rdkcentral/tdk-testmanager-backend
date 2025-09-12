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

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Year;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.exception.UserInputException;
import com.rdkm.tdkservice.service.IDeviceConfigService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.Utils;

/**
 * This class is used to provide the service to get the device configuration
 * file for a given deviceType name or device type or default device
 * configuration file and to upload the device configuration file
 * 
 */
@Service
public class DeviceConfigService implements IDeviceConfigService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeviceConfigService.class);

	/**
	 * This method is used to get the device configuration file for a given device
	 * type name or device type or default device configuration file.
	 * 
	 * @param deviceTypeName - the device name
	 * @param deviceType     - the device type
	 * @return Resource - the device configuration file null - if the device config
	 *         file is not found
	 */
	@Override
	public Resource getDeviceConfigFile(String deviceTypeName, String deviceType, boolean isThunderEnabled) {
		LOGGER.info("Inside getDeviceConfigFile method with deviceTypeName: {}, deviceType: {}", deviceTypeName,
				deviceType);
		Resource resource = null;

		// Try to get the device config file for the given deviceType name
		if (!Utils.isEmpty(deviceTypeName)) {
			resource = getDeviceConfigFileGivenName(deviceTypeName + Constants.CONFIG_FILE_EXTENSION, isThunderEnabled);
		}

		// If not found, try to get the device config file for the given deviceType type
		if (resource == null && !Utils.isEmpty(deviceType)) {
			resource = getDeviceConfigFileGivenName(deviceType + Constants.CONFIG_FILE_EXTENSION, isThunderEnabled);
		}

		// If still not found, get the default device config file
		if (resource == null && isThunderEnabled) {
			resource = getDeviceConfigFileGivenName(Constants.THUNDER_DEVICE_CONFIG_FILE, isThunderEnabled);
		} else if (resource == null && !isThunderEnabled) {
			resource = getDeviceConfigFileGivenName(Constants.DEFAULT_DEVICE_CONFIG_FILE, isThunderEnabled);
		}
		// Add header to the resource
		try {
			return addHeader(resource);
		} catch (IOException e) {
			LOGGER.error("Failed to add header to the resource: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * This method is used to add the header to the resource
	 * 
	 * @param resource - the device configuration file
	 * @return Resource - the device configuration file with header
	 * @throws IOException - if an I/O error occurs
	 */
	private Resource addHeader(Resource resource) throws IOException {

		// Read the file content as a String
		String fileContent = new String(Files.readAllBytes(resource.getFile().toPath()));
		if (fileContent.contains(Constants.HEADER_FINDER)) {
			LOGGER.info("Header already exists in the file");
			return resource;
		}
		String headerFileLocation = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
				+ Constants.TDK_UTIL_FILE_LOCATION;
		Path path = Paths.get(headerFileLocation);
		if (!Files.exists(path)) {
			LOGGER.error("Header file not found at location: " + headerFileLocation);
			throw new IOException("Header file not found at location: " + headerFileLocation);
		}

		String headerContent = Files.readString(path, StandardCharsets.UTF_8);
		String currentYear = Year.now().toString();
		String header = headerContent.replace("CURRENT_YEAR", currentYear);

		// Prepend the header to the file content
		String updatedContent = header + fileContent;

		return new ByteArrayResource(updatedContent.getBytes()) {
			@Override
			public String getFilename() {
				return resource.getFilename(); // Return the original filename
			}

			@Override
			public long contentLength() {
				return updatedContent.getBytes().length; // Override content length
			}
		};
	}

	/**
	 * This method is used to upload the device configuration file
	 * 
	 * @param file - the device configuration file
	 * @return boolean - true if the device config file is uploaded successfully
	 *         false - if the device config file is not uploaded successfully
	 */
	public boolean uploadDeviceConfigFile(MultipartFile file, boolean isThunderEnabled) {
		LOGGER.info("Inside uploadDeviceConfigFile method with file: {}", file.getOriginalFilename());
		validateFile(file);
		String path = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR;
		if (isThunderEnabled) {
			path = path + Constants.THUNDER_DEVICE_CONFIG_DIR + Constants.FILE_PATH_SEPERATOR;
		} else {
			path = path + Constants.TDKV_DEVICE_CONFIG_DIR + Constants.FILE_PATH_SEPERATOR;
		}
		try {
			Path uploadPath = Paths.get(path);
			if (!Files.exists(uploadPath)) {
				Files.createDirectories(uploadPath);
			}
			// Save the file to the path
			Path filePath = uploadPath.resolve(file.getOriginalFilename());

			Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.info("File uploaded successfully: {}", file.getOriginalFilename());
			return true;
		} catch (IOException ex) {
			LOGGER.error("Upload device Config file failed due to this exception - {}", ex.getMessage());
			return false;
		} catch (Exception ex) {
			LOGGER.error("Upload device Config file failed due to this exception - {}", ex.getMessage());
			return false;
		}

	}

	/**
	 * This method is used to delete the device configuration file
	 * 
	 * @param deviceConfigFileName - the device configuration file name
	 * @return boolean - true if the device config file is deleted successfully
	 *         false - if the device config file is not deleted
	 */
	@Override
	public boolean deleteDeviceConfigFile(String deviceConfigFileName, boolean isThunderEnabled) {
		LOGGER.info("Inside deleteDeviceConfigFile method with deviceConfigFileName: {}", deviceConfigFileName);

		String path = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR;
		if (isThunderEnabled) {
			path = path + Constants.THUNDER_DEVICE_CONFIG_DIR + Constants.FILE_PATH_SEPERATOR;
		} else {
			path = path + Constants.TDKV_DEVICE_CONFIG_DIR + Constants.FILE_PATH_SEPERATOR;
		}
		Path filePath = Paths.get(path).resolve(deviceConfigFileName);
		try {
			Files.delete(filePath);
			LOGGER.info("File deleted successfully: {}", deviceConfigFileName);
			return true;
		} catch (NoSuchFileException ex) {
			LOGGER.error("Delete device Config file failed due to this NoSuchFileException - {}", ex.getMessage());
			throw new UserInputException("No such file exists");
		} catch (IOException ex) {
			LOGGER.error("Delete device Config file failed due to this exception - {}", ex.getMessage());
			return false;
		} catch (Exception ex) {
			LOGGER.error("Delete device Config file failed due to this exception - {}", ex.getMessage());
			return false;
		}
	}

	/**
	 * This method is used to get the device configuration file for a given config
	 * file name
	 * 
	 * @param configFileName - the config file name
	 * @return Resource - the device configuration file null - if the device config
	 *         file is not found
	 */
	private Resource getDeviceConfigFileGivenName(String configFileName, boolean isThunderEnabled) {
		LOGGER.info("Inside getDeviceConfigFileGivenName method with configFileName: {}", configFileName);
		String path = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR;
		if (isThunderEnabled) {
			path = path + Constants.THUNDER_DEVICE_CONFIG_DIR + Constants.FILE_PATH_SEPERATOR;
		} else {
			path = path + Constants.TDKV_DEVICE_CONFIG_DIR + Constants.FILE_PATH_SEPERATOR;
		}
		Path pathFile = Paths.get(path).resolve(configFileName);
		Resource resource = null;
		try {
			resource = new UrlResource(pathFile.toUri());
		} catch (MalformedURLException e) {
			LOGGER.error("Device config file not found: {}", configFileName);
		}
		// Loads the resource and checks if it exists
		if (null != resource && !resource.exists()) {
			LOGGER.error("Device config file not found: {}", configFileName);
			return null;
		}
		return resource;
	}

	/**
	 * Validates the uploaded file.
	 *
	 * @param file the uploaded file
	 */
	private void validateFile(MultipartFile file) {
		String fileName = file.getOriginalFilename();
		if (fileName == null || !fileName.endsWith(Constants.CONFIG_FILE)) {
			LOGGER.error("The uploaded file must have a .config extension {}", fileName);
			throw new UserInputException("Please upload a .config file.");
		}
		if (file.isEmpty()) {
			LOGGER.error("The uploaded file is empty");
			throw new UserInputException("The uploaded file is empty.");
		}
	}

	/**
	 * This method retrieves the configuration file name for a given device based on
	 * its name, type, or Thunder-enabled status. If neither the device name nor
	 * type is provided, it returns a default configuration file name.
	 * 
	 * @param deviceType
	 * @param isThunderEnabled
	 * @return String - the name of the device configuration file
	 */
	public String getDeviceConfigFileName(String deviceName, String deviceType, boolean isThunderEnabled) {
		LOGGER.debug("Fetching device config file name for deviceName: {}, deviceType: {}", deviceName, deviceType);

		// Check if deviceName is provided
		if (!Utils.isEmpty(deviceName)) {
			return deviceName + Constants.CONFIG_FILE_EXTENSION;
		}

		// Check if deviceType is provided
		if (!Utils.isEmpty(deviceType)) {
			return deviceType + Constants.CONFIG_FILE_EXTENSION;
		}

		// Return default config file name based on Thunder status
		if (isThunderEnabled) {
			return Constants.THUNDER_DEVICE_CONFIG_FILE;
		} else {
			return Constants.DEFAULT_DEVICE_CONFIG_FILE;
		}
	}

}
