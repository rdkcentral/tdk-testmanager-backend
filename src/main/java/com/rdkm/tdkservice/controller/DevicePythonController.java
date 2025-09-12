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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jayway.jsonpath.internal.Utils;
import com.rdkm.tdkservice.service.IDeviceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * The DevicePythonController class is used to handle the device related
 * operations that are called from the python scripts
 * 
 * TODO : Keeping the Rest API response and path as such for keeping backward
 * compatibility with python framework, change to a proper standard form of Rest
 * API after change in Python lib
 */
@Validated
@RestController
@CrossOrigin
@RequestMapping("/deviceGroup")
public class DevicePythonController {

	private static final Logger LOGGER = LoggerFactory.getLogger(DevicePythonController.class);

	@Autowired
	private IDeviceService deviceService;

	/**
	 * This method is used to get the details of a device.
	 *
	 * @param deviceIp This is the IP of the device.
	 * @return ResponseEntity<String> This returns the response entity.
	 */
	@Operation(summary = "Get device details", description = "Get the details of a device in the system.")
	@ApiResponse(responseCode = "200", description = "Device details fetched successfully")
	@ApiResponse(responseCode = "500", description = "Error in fetching device details")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "404", description = "No device found")
	@GetMapping("/getDeviceDetails")
	public ResponseEntity<String> getDeviceDetails(@RequestParam String deviceIp) {
		LOGGER.info("Received get device details request for device IP: " + deviceIp);
		String deviceDetails = deviceService.getDeviceDetails(deviceIp);
		if (deviceDetails != null) {
			LOGGER.info("Device details fetched successfully");
			return ResponseEntity.status(HttpStatus.OK).body(deviceDetails);
		} else {
			LOGGER.error("No device found");
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No device found");
		}

	}

	/**
	 * This method is used to get the Thunder ports of a device.
	 *
	 * @param stbIp This is the IP of the device.
	 * @return ResponseEntity<String> This returns the response entity.
	 */
	@Operation(summary = "Get Thunder ports", description = "Get the Thunder ports of a device in the system.")
	@ApiResponse(responseCode = "200", description = "Thunder ports fetched successfully")
	@ApiResponse(responseCode = "500", description = "Error in fetching Thunder ports")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "404", description = "No Thunder port found")
	@GetMapping("/getThunderDevicePorts")
	public ResponseEntity<String> getThunderDevicePorts(@RequestParam String stbIp) {
		LOGGER.info("Received request for getting Thunder ports");
		String thunderPortDetails = deviceService.getThunderDevicePorts(stbIp);
		if (!Utils.isEmpty(thunderPortDetails)) {
			return ResponseEntity.status(HttpStatus.OK).body(thunderPortDetails);
		} else {
			LOGGER.error("No thunder port found with this IP");
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No thunder port found with this ip" + stbIp);
		}
	}

	/**
	 * This method is used to get the box type of a device.
	 *
	 * @param deviceIp This is the IP of the device.
	 * @return ResponseEntity<String> This returns the response entity.
	 */
	@Operation(summary = "Get Box Type", description = "Get the box type of a device in the system.")
	@ApiResponse(responseCode = "200", description = "Box type fetched successfully")
	@ApiResponse(responseCode = "500", description = "Error in fetching Box type")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "404", description = "No Box type found")
	@GetMapping("/getDeviceBoxType")
	public ResponseEntity<String> getDeviceBoxType(@RequestParam String deviceIp) {
		LOGGER.info("Received request for getting Box Type");
		String boxType = deviceService.getDeviceType(deviceIp);
		if (!Utils.isEmpty(boxType)) {
			return ResponseEntity.status(HttpStatus.OK).body(boxType);
		} else {
			LOGGER.error("No box type found with this IP");
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No box type found with this ip" + deviceIp);
		}
	}
}
