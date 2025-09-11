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
package com.rdkm.tdkservice.controller;

import static com.rdkm.tdkservice.util.Constants.DEVICE_FILE_EXTENSION_ZIP;
import static com.rdkm.tdkservice.util.Constants.DEVICE_XML_FILE_EXTENSION;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.dto.DeviceCreateDTO;
import com.rdkm.tdkservice.dto.DeviceResponseDTO;
import com.rdkm.tdkservice.dto.DeviceStatusResponseDTO;
import com.rdkm.tdkservice.dto.DeviceUpdateDTO;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.response.Response;
import com.rdkm.tdkservice.service.IDeviceConfigService;
import com.rdkm.tdkservice.service.IDeviceService;
import com.rdkm.tdkservice.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/**
 * The DeviceController class is a REST controller that handles device-related
 * requests. It provides endpoints for creating, updating, and deleting devices,
 * as well as for fetching device details and streams. This class uses the
 * IDeviceService and IDeviceConfigService to perform the actual business logic.
 *
 */

@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/device")
public class DeviceController {

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private IDeviceConfigService deviceConfigService;

	private static final Logger LOGGER = LoggerFactory.getLogger(DeviceController.class);

	/**
	 * This method is used to create a new device.
	 *
	 * @param deviceDTO This is the request object containing the details of the
	 *                  device to be created.
	 * @return ResponseEntity<Response> This returns the response message in
	 *         Response object.
	 */
	@Operation(summary = "Create a new device details", description = "Creates a new device details in the system.")
	@ApiResponse(responseCode = "201", description = "device details created successfully")
	@ApiResponse(responseCode = "500", description = "Error in saving device details data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@PostMapping("/create")
	public ResponseEntity<Response> createDevice(@RequestBody @Valid DeviceCreateDTO deviceDTO) {
		LOGGER.info("Received create device request: " + deviceDTO.toString());
		boolean isDeviceCreated = deviceService.createDevice(deviceDTO);
		if (isDeviceCreated) {
			LOGGER.info("Device created successfully");
			return ResponseUtils.getCreatedResponse("Device created successfully");
		} else {
			LOGGER.error("Failed to create device data");
			throw new TDKServiceException("Failed to create device");
		}
	}

	/**
	 * This method is used to update a device.
	 *
	 * @param deviceUpdateDTO This is the request object containing the updated
	 *                        details of the device.
	 * @return ResponseEntity<Response> This returns the response message in
	 *         Response object..
	 */
	@Operation(summary = "Update device details", description = "Updates the device details in the system.")
	@ApiResponse(responseCode = "200", description = "device details updated successfully")
	@ApiResponse(responseCode = "500", description = "Error in updating device details data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@PutMapping("/update")
	public ResponseEntity<Response> updateDevice(@RequestBody @Valid DeviceUpdateDTO deviceUpdateDTO) {
		LOGGER.info("Received update device request: " + deviceUpdateDTO.toString());
		boolean isDeviceUpdated = deviceService.updateDevice(deviceUpdateDTO);
		if (isDeviceUpdated) {
			LOGGER.info("Device updated successfully");
			return ResponseUtils.getSuccessResponse("Device updated successfully");
		} else {
			LOGGER.error("Failed to update device data");
			throw new TDKServiceException("Failed to update module");
		}
	}

	/**
	 * This method is used to get all devices.
	 *
	 * @return ResponseEntity<?> This returns the response entity.
	 */
	@Operation(summary = "Get All device details", description = "Get the device details in the system.")
	@ApiResponse(responseCode = "200", description = "device details fetched successfully")
	@ApiResponse(responseCode = "500", description = "Error in fetching device details data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/findAll")
	public ResponseEntity<DataResponse> getAllDevices() {
		LOGGER.info("Received find all devices request");
		List<DeviceResponseDTO> deviceDetails = deviceService.getAllDeviceDetails();
		if (deviceDetails != null && !deviceDetails.isEmpty()) {
			LOGGER.info("Device details fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Device details fetched successfully", deviceDetails);
		} else {
			LOGGER.error("No devices found");
			return ResponseUtils.getSuccessDataResponse("Device details retrieved successfully", deviceDetails);
		}
	}

	/**
	 * This method is used to get all devices by category.
	 *
	 * @param category This is the category of the devices to be fetched.
	 * @return ResponseEntity<DataResponse> with a list of Devices
	 */
	@Operation(summary = "Get All device details", description = "Get the device details in the system.")
	@ApiResponse(responseCode = "200", description = "device details fetched successfully")
	@ApiResponse(responseCode = "500", description = "Error in fetching device details data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/findAllByCategory")
	public ResponseEntity<DataResponse> getAllDevicesByCategory(@RequestParam String category) {
		LOGGER.info("Received find all devices by category request: " + category);
		List<DeviceResponseDTO> deviceDetails = deviceService.getAllDeviceDetailsByCategory(category);
		if (deviceDetails != null && !deviceDetails.isEmpty()) {
			LOGGER.info("Device details fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Device details fetched successfully", deviceDetails);
		} else {
			LOGGER.error("No devices found for the category");
			return ResponseUtils.getSuccessDataResponse("No Device details found for category", null);
		}
	}

	/**
	 * This method is used to get device details by id.
	 *
	 * @param id This is the id of the device to be fetched.
	 * @return ResponseEntity<?> This returns the response entity.
	 */
	@Operation(summary = "Get device details by id", description = "Get the device details by id in the system.")
	@ApiResponse(responseCode = "200", description = "device details fetched successfully")
	@ApiResponse(responseCode = "500", description = "Error in fetching device details data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/findbyid")
	public ResponseEntity<DataResponse> getDeviceById(@RequestParam UUID id) {
		LOGGER.info("Received find device by id request: " + id);
		DeviceResponseDTO deviceDetails = deviceService.findDeviceById(id);
		if (deviceDetails != null) {
			LOGGER.info("Device details fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Device details fetched successfully", deviceDetails);
		} else {
			LOGGER.error("No device found");
			return ResponseUtils.getNotFoundDataResponse("No device found: " + id, null);
		}
	}

	/**
	 * This method is used to delete a device by its id.
	 *
	 * @param id This is the id of the device to be deleted.
	 * @return ResponseEntity<DataResponse> with the device in DataResponse
	 */
	@Operation(summary = "Delete device details by id", description = "Delete the device details by id in the system.")
	@ApiResponse(responseCode = "200", description = "device details deleted successfully")
	@ApiResponse(responseCode = "500", description = "Error in deleting device details data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@DeleteMapping("/delete")
	public ResponseEntity<Response> deleteDeviceById(@RequestParam UUID id) {
		LOGGER.info("Received delete device request for id: " + id);
		deviceService.deleteDeviceById(id);
		LOGGER.info("Device deleted successfully");
		return ResponseUtils.getSuccessResponse("Device is deleted successfully");
	}

	/**
	 * This method is used to upload the XML file for a device.
	 *
	 * @param file This is the XML file to be uploaded.
	 * @return ResponseEntity<String> This returns the response message.
	 */
	@Operation(summary = "Upload device XML File", description = "Upload device XML File.")
	@ApiResponse(responseCode = "200", description = "Device created successfully from XML data")
	@ApiResponse(responseCode = "500", description = "Failed to create device from XML data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@PostMapping("/uploadxml")
	public ResponseEntity<Response> createDeviceFromXML(@Valid @RequestParam("file") MultipartFile file) {
		LOGGER.info("Received upload device XML request: " + file.getOriginalFilename());
		boolean isUploadedDeviceXml = deviceService.parseXMLForDevice(file);
		if (isUploadedDeviceXml) {
			LOGGER.info("Device created successfully from XML data");
			return ResponseUtils.getCreatedResponse("Device created successfully from XML data");
		} else {
			LOGGER.error("Failed to create device from XML data");
			throw new TDKServiceException("Could not upload the xml file");

		}
	}

	/**
	 * This method is used to download the XML file for a device.
	 *
	 * @param deviceName This is the name of the device for which the XML file is to
	 *                   be downloaded.
	 * @return ResponseEntity<String> This returns the XML content as a response.
	 */

	@Operation(summary = "Download device XML File", description = "Generate device XML File.")
	@ApiResponse(responseCode = "200", description = "Downloaded  Device XMl successfully")
	@ApiResponse(responseCode = "500", description = "Failed to create device from XML data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/downloadXML")
	public ResponseEntity<String> downloadXML(@RequestParam String deviceName) {
		LOGGER.info("Received download device XML request: " + deviceName);
		String xmlContent = deviceService.downloadDeviceXML(deviceName);
		if (xmlContent == null) {
			LOGGER.error("Failed to download device XML");
			throw new TDKServiceException("Failed to download device XML");
		}
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + deviceName + DEVICE_XML_FILE_EXTENSION);
		LOGGER.info("Downloaded  Device XMl successfully");
		return ResponseEntity.status(HttpStatus.OK).headers(headers).body(xmlContent);
	}

	/**
	 * This method is used to download the device configuration file exclusively for
	 * RDKV devices and the related usecases
	 *
	 * @param deviceTypeName - the deviceType name
	 * @param deviceType     - the device type
	 * @return ResponseEntity<Resource> - the response entity - HttpStatus.OK - if
	 *         the file download is successful - HttpStatus.NOT_FOUND - if the file
	 *         is not found
	 *
	 */
	@Operation(summary = "Download device configuration file", description = "Download the device configuration file for a specific device in the system.")
	@ApiResponse(responseCode = "200", description = "Device configuration file downloaded successfully")
	@ApiResponse(responseCode = "500", description = "Internal server error in downloading device configuration file")
	@ApiResponse(responseCode = "400", description = "There is no file associated with the  deviceType and no default file found.")
	@GetMapping("/downloadDeviceConfigFile")
	public ResponseEntity<Resource> downloadDeviceConfigFile(@RequestParam String deviceTypeName,
			@RequestParam String deviceType, @RequestParam boolean isThunderEnabled) {
		LOGGER.info("Going to get the device config file " + deviceTypeName + " " + deviceTypeName);
		Resource resource = deviceConfigService.getDeviceConfigFile(deviceTypeName, deviceType, isThunderEnabled);
		if (resource == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		} else {
			return ResponseEntity.status(HttpStatus.OK)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
					.header("Access-Control-Expose-Headers", "content-disposition").body(resource);
		}

	}

	/**
	 * This method is used to upload the device configuration file exclusively for
	 * RDKV devices and the related usecases
	 * 
	 * @param file - the device configuration file
	 * @return ResponseEntity<String> - the response entity - HttpStatus.OK - if the
	 *         file upload is successful - HttpStatus.BAD_REQUEST - if the file is
	 *         empty - HttpStatus.INTERNAL_SERVER_ERROR - if the file upload is not
	 *         successful
	 * 
	 */
	@Operation(summary = "Upload device configuration file", description = "Upload the device configuration file for a specific device in the system.")
	@ApiResponse(responseCode = "200", description = "Device configuration file uploaded successfully")
	@ApiResponse(responseCode = "500", description = "Internal server error in uploading device configuration file")
	@ApiResponse(responseCode = "400", description = "When the file is empty")
	@PostMapping("/uploadDeviceConfigFile")
	public ResponseEntity<Response> uploadFile(@RequestParam("uploadFile") MultipartFile file,
			@RequestParam boolean isThunderEnabled) {
		LOGGER.info("Received upload device config file request: " + file.getOriginalFilename());
		boolean isfileUploaded = deviceConfigService.uploadDeviceConfigFile(file, isThunderEnabled);
		if (isfileUploaded) {
			LOGGER.info("File upload is succesful");
			return ResponseUtils.getCreatedResponse("File uploaded successfully");
		} else {
			LOGGER.error("Failed to upload file");
			throw new TDKServiceException("Failed to upload file");
		}
	}

	/**
	 * This method is used to delete the device configuration file exclusively for
	 * RDKV devices and the related usecases
	 *
	 * @param deviceConfigFileName - the device configuration file name
	 * @return ResponseEntity<String> - the response entity - HttpStatus.OK - if the
	 *         file deletion is successful - HttpStatus.INTERNAL_SERVER_ERROR - if
	 *         the file deletion is not successful
	 *
	 */
	@Operation(summary = "Delete device configuration file", description = "Delete the device configuration file for a specific device in the system.")
	@ApiResponse(responseCode = "200", description = "Device configuration file deleted successfully")
	@ApiResponse(responseCode = "400", description = "No such file exists")
	@ApiResponse(responseCode = "500", description = "Internal server error in deleting device configuration file")
	@DeleteMapping("/deleteDeviceConfigFile")
	public ResponseEntity<String> deleteDeviceConfigFile(@RequestParam String deviceConfigFileName,
			@RequestParam boolean isThunderEnabled) {
		LOGGER.info("Received delete device config file request: " + deviceConfigFileName);
		boolean isFileDeleted = deviceConfigService.deleteDeviceConfigFile(deviceConfigFileName, isThunderEnabled);
		if (isFileDeleted) {
			return ResponseEntity.status(HttpStatus.OK).body("File deleted successfully");
		} else {
			LOGGER.error("Could not delete the device config file");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Could not delete the device config file");
		}
	}

	/**
	 * This method is used to download all Devices by Category.
	 *
	 * @param category This is the category of the Devices to be downloaded.
	 * @return ResponseEntity<Resource> This returns the Device XML.
	 */
	@Operation(summary = "Download all devices by category", description = "Download all devices by category to the system")
	@ApiResponse(responseCode = "200", description = "Downloaded successfully.")
	@ApiResponse(responseCode = "500", description = "Internal server error while downloading.")
	@GetMapping("/downloadDevicesByCategory")
	public ResponseEntity<?> downloadAllDevicesByCategory(@RequestParam String category) {
		LOGGER.info("Received download all devices by category request: " + category);
		Path file = deviceService.downloadAllDevicesByCategory(category);
		byte[] fileContent;
		try {
			fileContent = Files.readAllBytes(file);
		} catch (IOException e) {
			LOGGER.error("Error in downloading device by category:" + category);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("Failed to download device by category:" + category);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=devices_" + category + DEVICE_FILE_EXTENSION_ZIP);
		LOGGER.info("Downloaded all devices by category successfully");
		return ResponseEntity.status(HttpStatus.OK).headers(headers).body(fileContent);

	}

	/**
	 * This method is used to get the status of all devices in a category.
	 *
	 * @param category This is the category of the devices.
	 * @return ResponseEntity<?> This returns the response entity.
	 */
	@Operation(summary = "Get All device status", description = "Get the status of all devices in the system.")
	@ApiResponse(responseCode = "200", description = "device status fetched successfully")
	@ApiResponse(responseCode = "500", description = "Error in fetching device status data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "404", description = "No devices found for the category")
	@GetMapping("/getalldevicestatus")
	public ResponseEntity<?> getAllDeviceStatus(@RequestParam String category) {
		LOGGER.debug("Received request to fetch status for all devices in category: " + category);
		List<DeviceStatusResponseDTO> deviceStatusList = deviceService.getAllDeviceStatus(category);
		if (deviceStatusList != null && !deviceStatusList.isEmpty()) {
			LOGGER.debug("Fetched status for all devices in category: " + category);
			return ResponseEntity.status(HttpStatus.OK).body(deviceStatusList);
		} else {
			LOGGER.error("No devices found in the category  " + category);
			return ResponseEntity.status(HttpStatus.OK).body("No devices found in the category: " + category);
		}

	}

	/**
	 * Retrieves a list of devices based on the specified category and thunder
	 * status.
	 *
	 * @param category         the category of the devices to retrieve
	 * @param isThunderEnabled optional parameter to filter devices by their thunder
	 *                         status
	 * @return a ResponseEntity containing a list of DeviceResponseDTO objects if
	 *         devices are found, or an appropriate HTTP status code if no devices
	 *         are found or an error occurs
	 */
	@Operation(summary = "Get devices by category and thunder status", description = "Get devices by category and thunder status in the system.")
	@ApiResponse(responseCode = "200", description = "Devices fetched successfully")
	@ApiResponse(responseCode = "500", description = "Error in fetching device data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "404", description = "No devices found for the category")
	@GetMapping("/getDeviceByCategoryAndThunderstatus")
	public ResponseEntity<DataResponse> getDevicesByCategoryAndThunderStatus(@RequestParam String category,
			@RequestParam(required = false) Boolean isThunderEnabled) {
		LOGGER.info("Received request to fetch devices by category: " + category + " and thunder status: "
				+ (isThunderEnabled != null ? isThunderEnabled : "not specified"));
		// Fetch devices
		List<DeviceResponseDTO> devices = deviceService.getDevicesByCategoryAndThunderStatus(category,
				isThunderEnabled);

		if (devices == null || devices.isEmpty() || devices.size() == 0) {
			return ResponseUtils.getSuccessDataResponse("No devices found for the category: " + category, null);
		} else {
			return ResponseUtils.getSuccessDataResponse("Devices fetched successfully", devices);

		}

	}

	/**
	 * This method is used to toggle the thunder enabled status of a device.
	 *
	 * @param deviceIp This is the IP of the device.
	 * @return ResponseEntity<Response> This returns the response message.
	 */
	@Operation(summary = "Toggle Thunder Enabled Status", description = "Toggle the thunder enabled status of a device.")
	@ApiResponse(responseCode = "200", description = "Thunder enabled status toggled successfully")
	@ApiResponse(responseCode = "500", description = "Error in toggling thunder enabled status")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/toggleThunderEnabledStatus")
	public ResponseEntity<Response> toggleThunderEnabledStatus(@RequestParam String deviceIp) {
		LOGGER.info("Received toggle thunder enabled status request for device: " + deviceIp);
		boolean status = deviceService.toggleThunderEnabledstatus(deviceIp);
		if (status) {
			LOGGER.info("Thunder enabled status toggled successfully");
			return ResponseUtils.getSuccessResponse("Thunder enabled successfully");
		} else {
			LOGGER.info("Thunder enabled status toggled successfully");
			return ResponseUtils.getSuccessResponse("Thunder disabled successfully");
		}
	}

	/**
	 * This method is used to get the status of a device by device name.
	 *
	 * @param deviceName This is the name of the device.
	 * @return ResponseEntity<?> This returns the response entity.
	 */
	@Operation(summary = "Get device status by device name", description = "Get the status of a device by device name in the system.")
	@ApiResponse(responseCode = "200", description = "device status fetched successfully")
	@ApiResponse(responseCode = "500", description = "Error in fetching device status data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/getDeviceStatusByIP")
	public ResponseEntity<?> getDeviceStatusByIP(@RequestParam String deviceIP) {
		LOGGER.info("Received request to fetch status for device: " + deviceIP);
		DeviceStatusResponseDTO deviceStatus = deviceService.getDeviceStatus(deviceIP);
		LOGGER.info("Fetched status for device: " + deviceIP);
		return ResponseUtils.getSuccessDataResponse("Device status fetched successfully for :" + deviceIP,
				deviceStatus);
	}

	/**
	 * Updates the status of all devices in a specified category and retrieves the
	 * updated list.
	 *
	 * @param category The category of devices to update and retrieve.
	 * @return ResponseEntity<DataResponse> containing the updated list of devices.
	 */
	@Operation(summary = "Update and Get All Device Status by Category", description = "Updates the status of all devices in the specified category and retrieves the updated list.")
	@ApiResponse(responseCode = "200", description = "Device statuses updated and retrieved successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "500", description = "Internal server error")
	@GetMapping("/updateAndGetAllDeviceStatus")
	public ResponseEntity<?> updateAndGetAllDeviceStatus(@RequestParam String category) {
		LOGGER.info("Received request to update and get all device statuses for category: " + category);
		List<DeviceStatusResponseDTO> updatedDevices = deviceService.updateAndGetAllDeviceStatus(category);
		if (updatedDevices != null && !updatedDevices.isEmpty()) {
			LOGGER.info("Device statuses updated and retrieved successfully for category: " + category);
			return ResponseUtils.getSuccessDataResponse("Device statuses updated and retrieved successfully",
					updatedDevices);
		} else {
			LOGGER.warn("No devices found for the category: " + category);
			return ResponseUtils.getSuccessDataResponse("No devices found for the category", null);
		}

	}

	/**
	 * This method is used to get device details by device name.
	 *
	 * @param deviceName This is the name of the device to be fetched.
	 * @return ResponseEntity<?> This returns the response entity.
	 */
	@Operation(summary = "Get device details by device name", description = "Get the device details by device name in the system.")
	@ApiResponse(responseCode = "200", description = "device details fetched successfully")
	@ApiResponse(responseCode = "500", description = "Error in fetching device details data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/getDeviceDetailsByName")
	public ResponseEntity<DataResponse> getDeviceDetailsByName(@RequestParam String deviceName) {
		LOGGER.info("Received find device details by name request: " + deviceName);
		String deviceDetails = deviceService.findDeviceDetailsByName(deviceName);
		if (deviceDetails != null) {
			LOGGER.info("Device details fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Device details fetched successfully", deviceDetails);
		} else {
			LOGGER.error("No device found");
			return ResponseUtils.getNotFoundDataResponse("No device found with name: " + deviceName, null);
		}
	}

}
