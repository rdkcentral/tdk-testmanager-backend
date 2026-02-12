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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdkm.tdkci.dto.ExecutionDTO;
import com.rdkm.tdkci.response.Response;
import com.rdkm.tdkci.service.ExecutionServiceRegistery;

/*
 * Controller class for Execution Work Flow
 */
@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/execution")
public class ExecutionController {

	@Autowired
	private ExecutionServiceRegistery serviceRegistry;

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionController.class);

	/**
	 * Upgrades a device and triggers Continuous Integration execution.
	 * 
	 * This endpoint handles POST requests to upgrade a specified device and
	 * subsequently
	 * trigger a CI execution process based on the provided execution parameters.
	 * 
	 * @param execTriggerDTO the execution data transfer object containing device
	 *                       upgrade
	 *                       and CI execution parameters including device
	 *                       information,
	 *                       upgrade configuration, and execution settings
	 * @return ResponseEntity containing a Response object with the operation
	 *         status,
	 *         result details, and appropriate HTTP status code indicating success
	 *         or failure of the device upgrade and CI trigger operation
	 * @throws RuntimeException if device upgrade fails or CI execution cannot be
	 *                          triggered
	 */
	@PostMapping("/upgradeDeviceAndTriggerCI")
	public ResponseEntity<Response> upgradeDeviceAndTriggerCI(@RequestBody ExecutionDTO execTriggerDTO) {
		LOGGER.info("Received request to upgrade device and trigger CI execution with DTO: {}", execTriggerDTO);
		Response response = serviceRegistry.upgradeDeviceAndTriggerCIExecution(execTriggerDTO);
		return ResponseEntity.status(response.getStatusCode()).body(response);

	}

}
