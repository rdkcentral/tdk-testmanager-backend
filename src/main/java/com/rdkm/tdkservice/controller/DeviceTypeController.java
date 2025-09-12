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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rdkm.tdkservice.dto.DeviceTypeCreateDTO;
import com.rdkm.tdkservice.dto.DeviceTypeDTO;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.response.Response;
import com.rdkm.tdkservice.service.IDeviceTypeService;
import com.rdkm.tdkservice.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/**
 * The controller class that handles the endpoints related to
 * DeviceTypeController types.
 */
@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/devicetype")
public class DeviceTypeController {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeviceTypeController.class);

	/*
	 * The service that handles the business logic for device types.
	 */

	@Autowired
	IDeviceTypeService deviceTypeService;

	/**
	 * Creates a new device type based on the provided device type request.
	 *
	 * @param deviceTypeDTO The request object containing the details of the device
	 *                      type.
	 * @return A ResponseEntity containing the created device type if successful, or
	 *         an error message if unsuccessful.
	 * @throws ResourceAlreadyExistsException If a device type with the same name
	 *                                        already exists.
	 */
	@Operation(summary = "Create a new device type", description = "Creates a new device type in the system.")
	@ApiResponse(responseCode = "201", description = "device type created successfully")
	@ApiResponse(responseCode = "500", description = "Error in saving device type data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@PostMapping("/create")
	public ResponseEntity<Response> createDeviceType(@RequestBody @Valid DeviceTypeCreateDTO deviceTypeDTO) {
		LOGGER.info("Received create device type request: " + deviceTypeDTO.toString());
		boolean isDeviceTypeCreated = deviceTypeService.createDeviceType(deviceTypeDTO);
		if (isDeviceTypeCreated) {
			LOGGER.info("device type created successfully");
			return ResponseUtils.getCreatedResponse("Device Type created successfully");
		} else {
			LOGGER.error("Error in saving devicetype data");
			throw new TDKServiceException("Error in saving device type data");
		}

	}

	/**
	 * Retrieves all device types.
	 *
	 * @return ResponseEntity containing the list of device types if found, or a
	 *         ResponseEntity with status NOT_FOUND and a message if no device types
	 *         are found.
	 */
	@Operation(summary = "Get all device types", description = "Retrieves all device types in the system.")
	@ApiResponse(responseCode = "200", description = "device types retrieved successfully")
	@ApiResponse(responseCode = "404", description = "No device types found")
	@GetMapping("/findall")
	public ResponseEntity<DataResponse> getAllDeviceTypes() {
		LOGGER.info("Going to fetch all Device types");
		List<DeviceTypeDTO> deviceTypeDTOS = deviceTypeService.getAllDeviceTypes();
		if (null != deviceTypeDTOS) {
			LOGGER.info("Device types found");
			return ResponseUtils.getSuccessDataResponse("Device types fetched successfully", deviceTypeDTOS);
		} else {
			LOGGER.info("No device types found");
			return ResponseUtils.getSuccessDataResponse("No devicetypes available", deviceTypeDTOS);
		}

	}

	/**
	 * Deletes a device type with the specified ID.
	 *
	 * @param id the ID of the device type to delete
	 * @return a ResponseEntity with the status and message indicating the result of
	 *         the deletion
	 */
	@Operation(summary = "Delete a device type", description = "Deletes a device type from the system.")
	@ApiResponse(responseCode = "200", description = "Device type deleted successfully")
	@ApiResponse(responseCode = "404", description = "Device type not found")
	@DeleteMapping("/delete")
	public ResponseEntity<Response> deleteDeviceType(@RequestParam UUID id) {
		LOGGER.info("Received delete device type request: " + id);
		deviceTypeService.deleteById(id);
		return ResponseUtils.getSuccessResponse("Device Type deleted successfully");
	}

	/**
	 * Retrieves a device type by its ID.
	 *
	 * @param id the ID of the device type to retrieve
	 * @return a ResponseEntity containing the device type if found, or a NOT_FOUND
	 *         status with an error message if not found
	 */
	@Operation(summary = "Find device type by ID", description = "Retrieves a device type by its ID.")
	@ApiResponse(responseCode = "200", description = "Device type found")
	@ApiResponse(responseCode = "404", description = "Device type not found")
	@GetMapping("/findbyid")
	public ResponseEntity<DataResponse> findById(@RequestParam UUID id) {
		LOGGER.info("Received find device type by id request: " + id);
		DeviceTypeDTO deviceTypeDTO = deviceTypeService.findById(id);
		return ResponseUtils.getSuccessDataResponse("Device type found", deviceTypeDTO);

	}

	/**
	 * Updates a device type based on the provided .
	 * 
	 * @param deviceTypeDTO The DeviceTypeUpdateDTO containing the updated device
	 *                      type information.
	 * @return ResponseEntity containing the updated DeviceTypeUpdateDTO if it
	 *         exists, or a NOT_FOUND status with an error message if the device
	 *         type is not found.
	 */
	@Operation(summary = "Update a device type", description = "Updates a device type in the system.")
	@ApiResponse(responseCode = "200", description = "device type updated successfully")
	@ApiResponse(responseCode = "404", description = "device type not found")
	@ApiResponse(responseCode = "500", description = "Error in updating device type data")
	@PutMapping("/update")
	public ResponseEntity<DataResponse> updateDeviceType(@RequestBody DeviceTypeDTO deviceTypeDTO) {
		LOGGER.info("Received update device type request: " + deviceTypeDTO.toString());
		DeviceTypeDTO deviceTypeObjDTO = deviceTypeService.updateDeviceType(deviceTypeDTO);
		if (null != deviceTypeObjDTO) {
			LOGGER.info("DeviceType updated successfully");
			return ResponseUtils.getSuccessDataResponse("Device Type updated successfully", deviceTypeObjDTO);
		} else {
			LOGGER.error("Error in updating Device type data");
			throw new TDKServiceException("Error in updating user data");
		}

	}

	/**
	 * Retrieves all device types in the system by category.
	 *
	 * @param category the category of the device types to retrieve
	 * @return a ResponseEntity containing the list of device types if found, or a
	 *         ResponseEntity with status NOT_FOUND and a message if no device types
	 *         are found.
	 */
	@Operation(summary = "Get device types by category", description = "Retrieves all device types in the system by category.")
	@ApiResponse(responseCode = "200", description = "device types retrieved successfully")
	@ApiResponse(responseCode = "404", description = "No device types found")
	@GetMapping("/findallbycategory")
	public ResponseEntity<DataResponse> getDeviceTypesByCategory(@RequestParam String category) {
		LOGGER.info("Received find device type by category request: " + category);
		List<DeviceTypeDTO> deviceTypeDTOS = deviceTypeService.getDeviceTypesByCategory(category);
		if (null != deviceTypeDTOS) {
			LOGGER.info("device types found");
			return ResponseUtils.getSuccessDataResponse("Device types fetched successfully", deviceTypeDTOS);
		} else {
			LOGGER.error("No device types found");
			return ResponseUtils.getSuccessDataResponse("No device types found", deviceTypeDTOS);
		}

	}

	/**
	 * Retrieves all device type names in the system by category.
	 *
	 * @param category the category of the device types to retrieve
	 * @return a ResponseEntity containing the list of deviceDTO if found, or a
	 *         ResponseEntity with status NOT_FOUND and a message if no device types
	 *         are found.
	 */
	@Operation(summary = "Get device types by category", description = "Retrieves all device types in the system by category.")
	@ApiResponse(responseCode = "200", description = "device types retrieved successfully")
	@ApiResponse(responseCode = "404", description = "No device types found")
	@GetMapping("/getlistbycategory")
	public ResponseEntity<DataResponse> getDeviceTypesListByCategory(@RequestParam String category) {
		LOGGER.info("Received find device type by category request: " + category);
		List<String> deviceDTO = deviceTypeService.getDeviceTypeNameByCategory(category);
		if (null != deviceDTO) {
			LOGGER.info("device types found");
			return ResponseUtils.getSuccessDataResponse("Device types fetched successfully", deviceDTO);
		} else {
			LOGGER.error("No device types found");
			return ResponseUtils.getSuccessDataResponse("No device types found", deviceDTO);

		}
	}

	/**
	 * Retrieves all device type names in the system by category other than the
	 * device type name given
	 *
	 * @param category   the category of the device types to retrieve
	 * @param devicetype the name of the device type to exclude
	 * @return a ResponseEntity containing the list of device types if found, or a
	 *         ResponseEntity with status NOT_FOUND and a message if no device types
	 *         are found.
	 */
	@Operation(summary = "Get  device types list by category other than the device type name passed ", description = "Retrieves all device types in the system by category, excluded the deviceType name passed.")
	@ApiResponse(responseCode = "200", description = "device types retrieved successfully")
	@ApiResponse(responseCode = "404", description = "No device types found")
	@GetMapping("/getlistofotherdevicetypesbycategory")
	public ResponseEntity<?> getOtherDeviceTypesListByCategory(@RequestParam String category,
			@RequestParam String devicetype) {
		LOGGER.info("Received find device type by category request: " + category);
		List<String> deviceTypeDTO = deviceTypeService.getDeviceTypeNameByCategory(category);
		deviceTypeDTO.remove(devicetype);
		if (null != deviceTypeDTO && !deviceTypeDTO.isEmpty()) {
			LOGGER.info("Device types found");
			return ResponseUtils.getSuccessDataResponse("Device types fetched successfully", deviceTypeDTO);
		} else {
			LOGGER.error("No device types found");
			return ResponseUtils.getSuccessDataResponse("No device types found", deviceTypeDTO);
		}
	}

}
