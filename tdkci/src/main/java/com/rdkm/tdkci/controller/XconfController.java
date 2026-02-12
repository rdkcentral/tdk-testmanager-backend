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

import com.rdkm.tdkci.dto.XconfCreateDTO;
import com.rdkm.tdkci.dto.XconfDTO;
import com.rdkm.tdkci.exception.TDKCIServiceException;
import com.rdkm.tdkci.response.DataResponse;
import com.rdkm.tdkci.response.Response;
import com.rdkm.tdkci.service.IXconfService;
import com.rdkm.tdkci.utils.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Controller class for managing xconf configurations.
 */
@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/xconf")
public class XconfController {

	@Autowired
	private IXconfService xconfService;

	private static final Logger LOGGER = LoggerFactory.getLogger(XconfController.class);

	/**
	 * Handles the creation of a new Xconf configuration.
	 *
	 * @param xconfRequest the DTO containing Xconf configuration details to be
	 *                     created
	 * @return ResponseEntity containing the response with creation status
	 * @throws TDKCIServiceException if the Xconf creation fails
	 *
	 *                               <p>
	 *                               HTTP POST /create
	 *                               <ul>
	 *                               <li>201: Xconf created successfully</li>
	 *                               <li>500: Internal server error</li>
	 *                               </ul>
	 *                               </p>
	 */
	@Operation(summary = "Create a new xconf configuration")
	@ApiResponse(responseCode = "201", description = "Xconf created successfully")
	@ApiResponse(responseCode = "500", description = "Internal server error")
	@PostMapping("/create")
	public ResponseEntity<Response> createXconf(@RequestBody XconfCreateDTO xconfRequest) {
		boolean isXconfCreated = xconfService.createXconf(xconfRequest);
		if (isXconfCreated) {
			LOGGER.info("Xconf created successfully");
			return ResponseUtils.getCreatedResponse("Xconf created successfully");
		} else {
			LOGGER.error("Failed to create Xconf");
			throw new TDKCIServiceException("Failed to create Xconf");
		}
	}

	/**
	 * Retrieves all Xconf configurations.
	 * <p>
	 * This endpoint fetches a list of all available Xconf configurations.
	 * </p>
	 *
	 * @return ResponseEntity containing a DataResponse with the list of XconfDTO
	 *         objects if found, or a message indicating no configurations were
	 *         found.
	 *
	 * @operation.summary Get all xconf configurations @apiresponse.200 Xconf
	 *                    configurations fetched successfully @apiresponse.500
	 *                    Internal server error
	 */
	@Operation(summary = "Get all xconf configurations")
	@ApiResponse(responseCode = "200", description = "Xconf configurations fetched successfully")
	@ApiResponse(responseCode = "500", description = "Internal server error")
	@GetMapping("/findAll")
	public ResponseEntity<DataResponse> findAllXconfConfigurations() {
		LOGGER.info("Fetching all xconf configurations");
		List<XconfDTO> xconfConfigurations = xconfService.findAllXconfConfigurations();
		if (xconfConfigurations != null) {
			LOGGER.info("Xconf names fetched successfully");
			return ResponseUtils.getSuccessDataResponse(xconfConfigurations);
		} else {
			LOGGER.error("No Xconf names found");
			return ResponseUtils.getSuccessDataResponse("No Xconf names found");
		}
	}

	/**
	 * Handles HTTP GET requests to retrieve all Xconf names.
	 * <p>
	 * This endpoint returns a list of Xconf names if available. If no names are
	 * found, it returns a message indicating that no Xconf names were found.
	 * </p>
	 *
	 * @return ResponseEntity containing a DataResponse with either the list of
	 *         Xconf names or a message indicating no names were found.
	 */
	@Operation(summary = "Get all xconf names")
	@ApiResponse(responseCode = "200", description = "Xconf names fetched successfully")
	@ApiResponse(responseCode = "500", description = "Internal server error")
	@GetMapping("/findxconfNames")
	public ResponseEntity<DataResponse> findXconfNames() {
		LOGGER.info("Fetching all xconf names");
		List<String> xconfNames = xconfService.findXconfNames();
		if (xconfNames != null && !xconfNames.isEmpty()) {
			LOGGER.info("Xconf names fetched successfully");
			return ResponseUtils.getSuccessDataResponse(xconfNames);
		} else {
			LOGGER.error("No Xconf names found");
			return ResponseUtils.getSuccessDataResponse("No Xconf names found");
		}
	}

	/**
	 * Deletes an Xconf entity by its unique identifier.
	 *
	 * <p>
	 * This endpoint deletes the Xconf resource associated with the provided UUID.
	 * If the deletion is successful, a success response is returned. Otherwise, an
	 * exception is thrown.
	 * </p>
	 *
	 * @param id the UUID of the Xconf entity to be deleted
	 * @return ResponseEntity containing the result of the deletion operation
	 * @throws TDKCIServiceException if the Xconf entity could not be deleted
	 *
	 * @operation.summary Delete xconf by ID
	 * @apiResponse 200 Xconf deleted successfully
	 * @apiResponse 500 Internal server error
	 */
	@Operation(summary = "Delete xconf by ID")
	@ApiResponse(responseCode = "200", description = "Xconf deleted successfully")
	@ApiResponse(responseCode = "500", description = "Internal server error")
	@DeleteMapping("/delete/{id}")
	public ResponseEntity<Response> deleteXconfById(@PathVariable UUID id) {
		LOGGER.info("Deleting xconf with id: " + id);
		boolean isXconfDeleted = xconfService.deleteXconfById(id);
		if (isXconfDeleted) {
			LOGGER.info("Xconf deleted successfully");
			return ResponseUtils.getSuccessResponse("Xconf deleted successfully");
		} else {
			LOGGER.error("Failed to delete Xconf");
			throw new TDKCIServiceException("Failed to delete Xconf");
		}
	}

	/**
	 * Updates the Xconf configuration with the provided data.
	 *
	 * <p>
	 * This endpoint receives an {@link XconfDTO} object in the request body and
	 * attempts to update the Xconf configuration. If the update is successful, a
	 * success response is returned. Otherwise, an exception is thrown indicating
	 * the failure.
	 * </p>
	 *
	 * @param xconfUpdateRequest the {@link XconfDTO} containing the updated
	 *                           configuration data
	 * @return a {@link ResponseEntity} containing the operation result
	 * @throws TDKCIServiceException if the update operation fails
	 */
	@Operation(summary = "Update xconf configuration")
	@ApiResponse(responseCode = "200", description = "Xconf updated successfully")
	@ApiResponse(responseCode = "500", description = "Internal server error")
	@PutMapping("/update")
	public ResponseEntity<Response> updateXconf(@RequestBody XconfDTO xconfUpdateRequest) {
		boolean isXconfUpdated = xconfService.updateXconf(xconfUpdateRequest);
		if (isXconfUpdated) {
			LOGGER.info("Xconf updated successfully");
			return ResponseUtils.getSuccessResponse("Xconf updated successfully");
		} else {
			LOGGER.error("Failed to update Xconf");
			throw new TDKCIServiceException("Failed to update Xconf");
		}
	}
}
