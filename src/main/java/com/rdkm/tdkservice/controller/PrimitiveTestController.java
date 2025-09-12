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

import com.rdkm.tdkservice.dto.PrimitiveTestCreateDTO;
import com.rdkm.tdkservice.dto.PrimitiveTestDTO;
import com.rdkm.tdkservice.dto.PrimitiveTestNameAndIdDTO;
import com.rdkm.tdkservice.dto.PrimitiveTestUpdateDTO;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.response.Response;
import com.rdkm.tdkservice.service.IPrimitiveTestService;
import com.rdkm.tdkservice.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/*
 * The PrimitiveTestController class is used to handle the primitive test related operations
 */
@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/primitivetest")
public class PrimitiveTestController {

	private static final Logger LOGGER = LoggerFactory.getLogger(PrimitiveTestController.class);

	@Autowired
	IPrimitiveTestService primitiveTestService;

	/**
	 * This method is used to create the primitive test
	 *
	 * @param primitiveTestDTO - PrimitiveTestCreateDTO object
	 * @return ResponseEntity<Response> - response entity - message in Response
	 */

	@Operation(summary = "Create Primitive Test", description = "Create Primitive Test in the system")
	@ApiResponse(responseCode = "201", description = "Primitive Test created successfully")
	@ApiResponse(responseCode = "500", description = "Error in saving primitive test data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@PostMapping("/create")
	public ResponseEntity<Response> createPrimitiveTest(@RequestBody @Valid PrimitiveTestCreateDTO primitiveTestDTO) {
		boolean isPrimitiveTestCreated = primitiveTestService.createPrimitiveTest(primitiveTestDTO);
		if (isPrimitiveTestCreated) {
			LOGGER.info("Primitive Test created successfully");
			return ResponseUtils.getCreatedResponse("Primitive Test created successfully");
		} else {
			LOGGER.error("Error in saving primitive test data");
			throw new TDKServiceException("Error in saving primitive test data");
		}

	}

	/**
	 * This method is used to update the primitive test
	 *
	 * @param primitiveTestDTO - PrimitiveTestUpdateDTO object
	 * @return ResponseEntity<Response> - response entity - message in Response
	 */

	@Operation(summary = "Update Primitive Test", description = "Update Primitive Test in the system")
	@ApiResponse(responseCode = "200", description = "Primitive Test updated successfully")
	@ApiResponse(responseCode = "500", description = "Error in updating primitive test data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@PutMapping("/update")
	public ResponseEntity<Response> updatePrimitiveTest(@RequestBody @Valid PrimitiveTestUpdateDTO primitiveTestDTO) {
		boolean isPrimitiveTestUpdated = primitiveTestService.updatePrimitiveTest(primitiveTestDTO);
		if (isPrimitiveTestUpdated) {
			LOGGER.info("Primitive Test Updated Successfully");
			return ResponseUtils.getSuccessResponse("Primitive Test Updated Successfully");
		} else {
			LOGGER.error("Error in updating primitive test data");
			throw new TDKServiceException("Error in updating primitive test data");
		}
	}

	/**
	 * This method is used to delete the primitive test
	 *
	 * @param id - UUID
	 * @return ResponseEntity<String> - response entity - message
	 */

	@Operation(summary = "Delete Primitive Test", description = "Delete Primitive Test by id")
	@ApiResponse(responseCode = "200", description = "Primitive Test deleted successfully")
	@ApiResponse(responseCode = "500", description = "Error in deleting primitive test data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@DeleteMapping("/delete")
	public ResponseEntity<Response> deleteById(@RequestParam UUID id) {
		LOGGER.info("Deleting primitive test with id: " + id);
		primitiveTestService.deleteById(id);
		LOGGER.info("Primitive test deleted successfully with id: " + id);
		return ResponseUtils.getSuccessResponse("Primitive test deleted successfully");
	}

	/**
	 * This method is used to get the primitive test details by id
	 *
	 * @param id - UUID
	 * @return ResponseEntity<DataResponse> - response entity - message in
	 *         DataResponse
	 */
	@Operation(summary = "Find Primitive Test", description = "Find Primitive Test by id")
	@ApiResponse(responseCode = "200", description = "Primitive Test found successfully")
	@ApiResponse(responseCode = "404", description = "Primitive Test not found")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/findbyid")
	public ResponseEntity<DataResponse> getPrimitiveTestDetails(@RequestParam UUID id) {
		LOGGER.info("Finding primitive test with id: " + id);
		PrimitiveTestDTO primitiveTestDTO = primitiveTestService.getPrimitiveTestDetailsById(id);
		if (primitiveTestDTO != null) {
			LOGGER.info("Primitive test found  " + primitiveTestDTO.toString());
			return ResponseUtils.getSuccessDataResponse("Primitive test fetched successfully", primitiveTestDTO);

		} else {
			LOGGER.error("Primitive test not found with id: " + id);
			throw new TDKServiceException("Error in getting Primitive test with id: " + id);
		}

	}

	/**
	 * This method is used to get the primitive test details by module name
	 *
	 * @param moduleName - String
	 * @return ResponseEntity<DataResponse>- response entity - message with
	 *         DataResponse
	 */
	@Operation(summary = "Find Primitive Test", description = "Find Primitive Test name and Id list  by module name")
	@ApiResponse(responseCode = "200", description = "Primitive Test found successfully")
	@ApiResponse(responseCode = "404", description = "Primitive Test not found")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/getlistbymodulename")
	public ResponseEntity<DataResponse> getPrimitiveTestDetailsByModuleName(@RequestParam String moduleName) {
		LOGGER.info("Finding primitive test with module name: " + moduleName);
		List<PrimitiveTestNameAndIdDTO> primitiveTestDTO = primitiveTestService
				.getPrimitiveTestDetailsByModuleName(moduleName);
		if (primitiveTestDTO != null) {
			LOGGER.info("Primitive test found  " + primitiveTestDTO.toString());
			return ResponseUtils.getSuccessDataResponse("PrimitiveTests fetched with module name: " + moduleName,
					primitiveTestDTO);
		} else {
			LOGGER.error("Primitive test not found with module name: " + moduleName);
			return ResponseUtils.getSuccessDataResponse("No primitive test not found for the module: " + moduleName,
					null);
		}

	}

	/**
	 * This method is used to get the primitive test details by module name
	 *
	 * @param moduleName - String
	 * @return ResponseEntity<?> - response entity - message
	 */

	@Operation(summary = "Find Primitive Test", description = "Find Primitive Test DTO  by module name")
	@ApiResponse(responseCode = "200", description = "Primitive Test found successfully")
	@ApiResponse(responseCode = "404", description = "Primitive Test not found")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/findallbymodulename")
	public ResponseEntity<?> findAllByModuleName(@RequestParam String moduleName) {
		LOGGER.info("Finding primitive test with module name: " + moduleName);
		List<PrimitiveTestDTO> primitiveTestDTO = primitiveTestService.findAllByModuleName(moduleName);
		if (primitiveTestDTO != null) {
			LOGGER.info("Primitive test found  " + primitiveTestDTO.toString());
			return ResponseUtils.getSuccessDataResponse("PrimitiveTests fetched with module name: " + moduleName,
					primitiveTestDTO);
		} else {
			LOGGER.error("Primitive test not found with module name: " + moduleName);
			return ResponseUtils.getSuccessDataResponse("No primitive test not found for the module: " + moduleName,
					null);
		}

	}
}
