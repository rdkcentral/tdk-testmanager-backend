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

import com.rdkm.tdkservice.service.IPrimitiveTestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * The PrimitiveTestPythonController class is used to handle the primitive test
 * related operations
 * 
 * TODO : Keeping the Rest API response and path as such for keeping backward
 * compatibility with python framework, change to a proper standard form of Rest
 * API after change in Python lib
 * 
 */

@Validated
@RestController
@CrossOrigin
@RequestMapping("/primitiveTest")
public class PrimitiveTestPythonController {

	private static final Logger LOGGER = LoggerFactory.getLogger(PrimitiveTestPythonController.class);

	@Autowired
	IPrimitiveTestService primitiveTestService;

	/**
	 * This method is used to get the primitive test details by module name
	 *
	 * @param testName - String
	 * @return ResponseEntity<?> - response entity - message
	 */
	@Operation(summary = "Find Primitive Test", description = "Find Primitive Test DTO  by module name")
	@ApiResponse(responseCode = "200", description = "Primitive Test found successfully")
	@ApiResponse(responseCode = "404", description = "Primitive Test not found")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/getJson")
	public ResponseEntity<?> getJson(@RequestParam String testName, @RequestParam String idVal) {
		LOGGER.info("Finding primitive test with testName name: " + testName);
		String json = String.valueOf(primitiveTestService.getJson(testName));
		if (json != null) {
			LOGGER.info("Primitive test found  " + json);
			return ResponseEntity.status(HttpStatus.OK).body(json);
		} else {
			LOGGER.error("Primitive test not found with module name: " + testName);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Primitive test not found");
		}

	}
}
