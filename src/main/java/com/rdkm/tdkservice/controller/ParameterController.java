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

import com.rdkm.tdkservice.dto.ParameterCreateDTO;
import com.rdkm.tdkservice.dto.ParameterDTO;
import com.rdkm.tdkservice.enums.ParameterDataType;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.response.Response;
import com.rdkm.tdkservice.service.IParameterService;
import com.rdkm.tdkservice.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

/**
 * REST controller for managing parameter types.
 */
@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/parameter")
public class ParameterController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ParameterController.class);

	@Autowired
	private IParameterService parameterService;

	/**
	 * Creates a new parameter type.
	 *
	 * @param parameterCreateDTO the parameter type creation data transfer object
	 * @return ResponseEntity with Success or Failure message in Response object
	 */
	@Operation(summary = "Create a new parameter ", description = "Creates a new parameter in the system.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Parameter type created successfully"),
			@ApiResponse(responseCode = "500", description = "Failed to create parameter"),
			@ApiResponse(responseCode = "404", description = "Parameter  already exists"),
			@ApiResponse(responseCode = "400", description = "Invalid input"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") })
	@PostMapping("/create")
	public ResponseEntity<Response> createParameter(@RequestBody @Valid ParameterCreateDTO parameterCreateDTO) {
		LOGGER.info("Creating new parameter : {}", parameterCreateDTO);
		boolean isCreated = parameterService.createParameter(parameterCreateDTO);
		if (isCreated) {
			LOGGER.info("Parameter  created successfully: {}", parameterCreateDTO);
			return ResponseUtils.getCreatedResponse("Parameter created successfully");

		} else {
			LOGGER.error("Failed to create parameter : {}", parameterCreateDTO);
			throw new TDKServiceException("Error in saving Parameter data");
		}
	}

	/**
	 * Updates an existing parameter .
	 *
	 * @param parameterDTO the parameter data transfer object
	 * @return ResponseEntity with Success or Failure message in Response object
	 */
	@Operation(summary = "Update an existing parameter ", description = "Updates an existing parameter in the system.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Parameter updated successfully"),
			@ApiResponse(responseCode = "500", description = "Failed to update parameter "),
			@ApiResponse(responseCode = "400", description = "Invalid input"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Parameter not found") })
	@PutMapping("/update")
	public ResponseEntity<Response> updateParameter(@RequestBody ParameterDTO parameterDTO) {
		LOGGER.info("Updating parameter : {}", parameterDTO);
		boolean isUpdated = parameterService.updateParameter(parameterDTO);
		if (isUpdated) {
			LOGGER.info("Parameter  updated successfully: {}", parameterDTO);
			return ResponseUtils.getSuccessResponse("Parameter updated successfully");
		} else {
			LOGGER.error("Failed to update parameter type: {}", parameterDTO);
			throw new TDKServiceException("Error in updating Parameter data");
		}
	}

	/**
	 * Retrieves all parameter types.
	 *
	 * @return ResponseEntity with a list of all parameter types
	 */
	@Operation(summary = "Retrieve all parameter ", description = "Retrieves a list of all parameter  in the system.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successfully retrieved all parameter "),
			@ApiResponse(responseCode = "404", description = "No parameter  found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "Internal server error") })
	@GetMapping("/findAll")
	public ResponseEntity<DataResponse> findAllParameter() {
		LOGGER.info("Retrieving all parameter ");
		List<ParameterDTO> parameters = parameterService.findAllParameters();
		if (parameters != null && !parameters.isEmpty()) {
			LOGGER.info("Successfully retrieved all parameter ");
			return ResponseUtils.getSuccessDataResponse("Parameters fetched successfully", parameters);
		} else {
			LOGGER.error("No parameter found");
			return ResponseUtils.getSuccessDataResponse("No parameters available", null);
		}
	}

	/**
	 * Retrieves a parameter type by its ID.
	 *
	 * @param id the ID of the parameter type
	 * @return ResponseEntity with the parameter type data transfer object
	 */
	@Operation(summary = "Retrieve a parameter  by its ID", description = "Retrieves a parameter  by its ID.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successfully retrieved the parameter "),
			@ApiResponse(responseCode = "404", description = "Parameter type not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "Internal server error") })
	@GetMapping("/findById")
	public ResponseEntity<DataResponse> findParameterById(@RequestParam UUID id) {
		LOGGER.info("Retrieving parameter by ID: {}", id);
		ParameterDTO parameter = parameterService.findParameterById(id);
		if (parameter != null) {
			LOGGER.info("Successfully retrieved parameter : {}", parameter);
			return ResponseUtils.getSuccessDataResponse("Parameter fetched successfully", parameter);
		} else {
			LOGGER.error("Error in getting parameter with id: {}", id);
			throw new TDKServiceException("Error in getting parameter with id: " + id);
		}
	}

	/**
	 * Deletes a parameter type by its ID.
	 *
	 * @param id the ID of the parameter type
	 * @return ResponseEntity with a message indicating success or failure in
	 *         Response
	 */
	@Operation(summary = "Delete a parameter  by its ID", description = "Deletes a parameter  by its ID.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Parameter  deleted successfully"),
			@ApiResponse(responseCode = "500", description = "Failed to delete parameter "),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Parameter not found") })
	@DeleteMapping("/delete")
	public ResponseEntity<Response> deleteParameter(@RequestParam UUID id) {
		LOGGER.info("Deleting parameter type by ID: {}", id);
		boolean isDeleted = parameterService.deleteParameter(id);
		if (isDeleted) {
			LOGGER.info("Parameter deleted successfully: {}", id);
			return ResponseUtils.getSuccessResponse("Parameter deleted successfully");
		} else {
			LOGGER.error("Failed to delete parameter : {}", id);
			throw new TDKServiceException("Error in deleting parameter with id: " + id);
		}
	}

	/**
	 * Retrieves all parameters by function name.
	 *
	 * @param functionName the name of the function
	 * @return ResponseEntity with a list of all parameters
	 */
	@Operation(summary = "Retrieve all parameters by function name", description = "Retrieves a list of all parameters by function name.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successfully retrieved all parameters"),
			@ApiResponse(responseCode = "404", description = "No parameters found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "Internal server error") })
	@GetMapping("/findAllByFunction")
	public ResponseEntity<DataResponse> findAllParametersByFunction(@RequestParam String functionName) {
		LOGGER.info("Retrieving all parameters by function name: {}", functionName);
		List<ParameterDTO> parameters = parameterService.findAllParametersByFunction(functionName);
		if (parameters != null && !parameters.isEmpty()) {
			LOGGER.info("Successfully retrieved all parameters by function name: {}", functionName);
			return ResponseUtils.getSuccessDataResponse("Parameters fetched successfully", parameters);
		} else {
			LOGGER.error("No parameters found for function name: {}", functionName);
			return ResponseUtils.getSuccessDataResponse("No parameters available for function: " + functionName, null);
		}
	}

	/**
	 * Retrieves all parameter enums.
	 *
	 * @return ResponseEntity with a list of all parameter enums in DataResponse
	 */
	@Operation(summary = "Retrieve all parameter enums", description = "Retrieves a list of all parameter enums.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved all parameter enums"),
			@ApiResponse(responseCode = "500", description = "Internal server error") })
	@GetMapping("/getListOfParameterDatatypes")
	public ResponseEntity<DataResponse> getAllParameterEnums() {
		LOGGER.info("Retrieving all ParameterDatatypes");
		List<ParameterDataType> parameterEnums = parameterService.getAllParameterEnums();
		if (parameterEnums == null || parameterEnums.isEmpty()) {
			LOGGER.error("No parameter enums found");
			return ResponseUtils.getSuccessDataResponse("No Parameter Datatypes available", null);
		} else {
			LOGGER.info("Successfully retrieved all ParameterDatatypes");
			return ResponseUtils.getSuccessDataResponse("Parameter Datatypes fetched successfully", parameterEnums);
		}
	}

}