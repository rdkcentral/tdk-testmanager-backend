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
package com.rdkm.tdkci.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdkm.tdkci.dto.DeviceCreateDTO;
import com.rdkm.tdkci.dto.DeviceDTO;
import com.rdkm.tdkci.exception.TDKCIServiceException;
import com.rdkm.tdkci.response.DataResponse;
import com.rdkm.tdkci.response.Response;
import com.rdkm.tdkci.service.IDeviceService;
import com.rdkm.tdkci.utils.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Controller class for managing devices.
 */
@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/device")
public class DeviceController {

	@Autowired
	private IDeviceService deviceService;

	private static final Logger LOGGER = LoggerFactory.getLogger(DeviceController.class);

	/**
	 * Handles the creation of a new device.
	 * <p>
	 * Receives a {@link DeviceCreateDTO} object in the request body, logs the
	 * device creation attempt, and delegates the creation process to the
	 * {@code deviceService}. If the device is created successfully, returns a 201
	 * Created response. Otherwise, throws a {@link TDKCIServiceException}.
	 * </p>
	 *
	 * @param deviceRequest the device creation request payload
	 * @return a {@link ResponseEntity} containing the creation result
	 * @throws TDKCIServiceException if device creation fails
	 */
	@Operation(summary = "Create a new device")
	@ApiResponse(responseCode = "201", description = "Device created successfully")
	@ApiResponse(responseCode = "500", description = "Internal server error")
	@PostMapping("/create")
	public ResponseEntity<Response> createDevice(@RequestBody DeviceCreateDTO deviceRequest) {
		LOGGER.info("Creating device with name: {}", deviceRequest.getDeviceName());
		boolean isDeviceCreated = deviceService.createDevice(deviceRequest);
		if (isDeviceCreated) {
			LOGGER.info("Device created successfully");
			return ResponseUtils.getCreatedResponse("Device created successfully");
		} else {
			LOGGER.error("Failed to create device");
			throw new TDKCIServiceException("Failed to create device");
		}
	}

	/**
	 * Retrieves all devices from the system.
	 * <p>
	 * This endpoint fetches a list of all available devices.
	 * </p>
	 *
	 * @return ResponseEntity containing a DataResponse with the list of DeviceDTO
	 *         objects if found, or a message indicating no devices were found.
	 *
	 * @apiNote Accessible via GET request at /findAll.
	 * @see DeviceService#getAllDevices()
	 */
	@Operation(summary = "Get all devices")
	@ApiResponse(responseCode = "200", description = "Devices fetched successfully")
	@ApiResponse(responseCode = "500", description = "Internal server error")
	@GetMapping("/findAll")
	public ResponseEntity<DataResponse> getAllDevices() {
		LOGGER.info("Fetching all devices");
		List<DeviceDTO> devices = deviceService.getAllDevices();
		if (devices != null) {
			LOGGER.info("Devices fetched successfully");
			return ResponseUtils.getSuccessDataResponse(devices);
		} else {
			LOGGER.error("No devices found");
			return ResponseUtils.getSuccessDataResponse("No devices found");
		}
	}

	/**
	 * Retrieves all device names.
	 * <p>
	 * This endpoint fetches a list of all device names available in the system.
	 * </p>
	 *
	 * @return ResponseEntity containing a DataResponse with the list of device
	 *         names if found, or a message indicating no device names were found.
	 *
	 * @apiNote
	 *          <ul>
	 *          <li>HTTP 200: Device names fetched successfully.</li>
	 *          <li>HTTP 500: Internal server error.</li>
	 *          </ul>
	 */
	@Operation(summary = "Get all device names")
	@ApiResponse(responseCode = "200", description = "Device names fetched successfully")
	@ApiResponse(responseCode = "500", description = "Internal server error")
	@GetMapping("/findAllDeviceNames")
	public ResponseEntity<DataResponse> getAllDeviceNames() {
		LOGGER.info("Fetching all device names");
		List<String> devices = deviceService.getAllDeviceNames();
		if (devices != null) {
			LOGGER.info("Device names fetched successfully");
			return ResponseUtils.getSuccessDataResponse(devices);
		} else {
			LOGGER.error("No device names found");
			return ResponseUtils.getSuccessDataResponse("No device names found");
		}
	}

	/**
	 * Deletes a device by its unique identifier.
	 *
	 * @param id the UUID of the device to be deleted
	 * @return ResponseEntity containing the operation result
	 * @throws TDKCIServiceException if the device could not be deleted
	 *
	 *                               <p>
	 *                               This endpoint deletes a device specified by its
	 *                               ID. If the deletion is successful, a success
	 *                               response is returned. Otherwise, an exception
	 *                               is thrown indicating failure.
	 *                               </p>
	 */
	@Operation(summary = "Delete device by ID")
	@ApiResponse(responseCode = "200", description = "Device deleted successfully")
	@ApiResponse(responseCode = "500", description = "Internal server error")
	@DeleteMapping("/delete/{id}")
	public ResponseEntity<Response> deleteDeviceById(@PathVariable UUID id) {
		LOGGER.info("Deleting device with id: {}", id);
		boolean isDeviceDeleted = deviceService.deleteDeviceById(id);
		if (isDeviceDeleted) {
			LOGGER.info("Device deleted successfully");
			return ResponseUtils.getSuccessResponse("Device deleted successfully");
		} else {
			LOGGER.error("Failed to delete device");
			throw new TDKCIServiceException("Failed to delete device");
		}
	}

	/**
	 * Updates the information of an existing device.
	 * <p>
	 * This endpoint receives a {@link DeviceDTO} object containing the updated
	 * device information. If the update is successful, a success response is
	 * returned. Otherwise, an exception is thrown.
	 * </p>
	 *
	 * @param deviceUpdateRequest the {@link DeviceDTO} containing updated device
	 *                            information
	 * @return {@link ResponseEntity} containing the operation result
	 * @throws TDKCIServiceException if the device update fails
	 */
	@Operation(summary = "Update device information")
	@ApiResponse(responseCode = "200", description = "Device updated successfully")
	@ApiResponse(responseCode = "500", description = "Internal server error")
	@PutMapping("/update")
	public ResponseEntity<Response> updateDevice(@RequestBody DeviceDTO deviceUpdateRequest) {
		LOGGER.info("Updating device with id: {}", deviceUpdateRequest.getId());
		boolean isDeviceUpdated = deviceService.updateDevice(deviceUpdateRequest);
		if (isDeviceUpdated) {
			LOGGER.info("Device updated successfully");
			return ResponseUtils.getSuccessResponse("Device updated successfully");
		} else {
			LOGGER.error("Failed to update device");
			throw new TDKCIServiceException("Failed to update device");
		}
	}

}
