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

import com.rdkm.tdkservice.dto.SocCreateDTO;
import com.rdkm.tdkservice.dto.SocDTO;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.response.Response;
import com.rdkm.tdkservice.service.ISocService;
import com.rdkm.tdkservice.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/**
 * This class is the controller for SocVendor related operations. It handles the
 * creation, retrieval, update, and deletion of SocVendor entities.
 */
@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/soc")
public class SocController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SocController.class);

	@Autowired
	ISocService socService;

	/**
	 * Creates a new Soc entity.
	 * 
	 * @param socDTO
	 * @return
	 */
	@Operation(summary = "Create a new Soc", description = "Creates a new Soc in the system.")
	@ApiResponse(responseCode = "201", description = "Soc created successfully")
	@ApiResponse(responseCode = "500", description = "Error in saving Soc data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@PostMapping("/create")
	public ResponseEntity<Response> createSoc(@RequestBody @Valid SocCreateDTO socDTO) {
		LOGGER.info("Received create soc request: " + socDTO.toString());
		boolean isSocVendorCreated = socService.createSoc(socDTO);
		if (isSocVendorCreated) {
			LOGGER.info("Soc created succesfully");
			return ResponseUtils.getCreatedResponse("SoC created succesfully");
		} else {
			LOGGER.error("Error in creating soc data");
			throw new TDKServiceException("Error in creating SoC");
		}

	}

	/**
	 * Retrieves all SOC vendors.
	 *
	 * @return ResponseEntity containing the list of SOCs if found, or a NOT_FOUND
	 *         status with an error message if no SOC vendors are found.
	 */
	@Operation(summary = "Retrieve all SOC", description = "Retrieves all SOC in the system.")
	@ApiResponse(responseCode = "200", description = "SOC retrieved successfully")
	@ApiResponse(responseCode = "404", description = "No SOC found")
	@GetMapping("/findall")
	public ResponseEntity<DataResponse> getAllSocs() {
		LOGGER.info("Received find all Soc request");
		List<SocDTO> socs = socService.findAll();
		if (socs != null && !socs.isEmpty()) {
			LOGGER.info("Soc found");
			return ResponseUtils.getSuccessDataResponse("SOC fetched successfully", socs);
		} else {
			LOGGER.error("No Soc found");
			return ResponseUtils.getSuccessDataResponse("No SoC available", socs);
		}
	}

	/**
	 * Deletes a Soc with the specified ID.
	 *
	 * @param id The ID of the Soc to delete.
	 * @return A ResponseEntity with the status and message indicating the result of
	 *         the deletion.
	 */
	@Operation(summary = "Delete a Soc ", description = "Deletes a Soc  in the system.")
	@ApiResponse(responseCode = "200", description = "Soc deleted successfully")
	@ApiResponse(responseCode = "404", description = "Soc not found")
	@DeleteMapping("/delete")
	public ResponseEntity<?> deleteSoc(@RequestParam UUID id) {
		LOGGER.info("Received delete SocVendor: " + id);
		socService.deleteSoc(id);
		return ResponseUtils.getSuccessResponse("Succesfully deleted the SoC");

	}

	/**
	 * Retrieves a Soc object by its ID.
	 *
	 * @param id The ID of the Soc to retrieve.
	 * @return ResponseEntity containing the Soc object if found, or a NOT_FOUND
	 *         status with an error message if not found.
	 */
	@Operation(summary = "Find Soc by ID", description = "Retrieves a Soc by its ID.")
	@ApiResponse(responseCode = "200", description = "Soc found")
	@ApiResponse(responseCode = "404", description = "Soc not found")
	@GetMapping("/findbyid")
	public ResponseEntity<DataResponse> findById(@RequestParam UUID id) {
		LOGGER.info("Received find Soc by id request: " + id);
		SocDTO socDTO = socService.findById(id);
		return ResponseUtils.getSuccessDataResponse("SOC fetched successfully", socDTO);

	}

	/**
	 * Updates a SocVendor entity based on the provided SocVendorUpdateRequest.
	 * 
	 * @param socUpdateDTO The SocVendorUpdateRequest object containing the updated
	 *                     data.
	 * @return ResponseEntity containing the updated SocVendor entity if it exists,
	 *         or a NOT_FOUND status if it doesn't.
	 */
	@Operation(summary = "Update a Soc", description = "Updates a Soc in the system.")
	@ApiResponse(responseCode = "200", description = "Soc updated successfully")
	@ApiResponse(responseCode = "404", description = "Soc not found")
	@ApiResponse(responseCode = "500", description = "Error in updating Soc data")
	@PutMapping("/update")
	public ResponseEntity<DataResponse> updateSoc(@RequestBody SocDTO socUpdateDTO) {
		LOGGER.info("Received update SocVendor request: " + socUpdateDTO.toString());
		SocDTO socUpdateDto = socService.updateSoc(socUpdateDTO);
		if (socUpdateDto != null) {
			LOGGER.info("Soc updated succesfully");
			return ResponseUtils.getSuccessDataResponse("SOC updated successfully", socUpdateDto);
		} else {
			LOGGER.error("Error in updating soc data");
			throw new TDKServiceException("Error in updating SOC");
		}

	}

	/**
	 * Retrieves all SOC vendors by category.
	 *
	 * @param category The category of the SOCs to retrieve.
	 * @return ResponseEntity containing the list of SOC vendors if found, or a
	 *         NOT_FOUND status with an error message if no SOCs are found.
	 */

	@Operation(summary = "Find SOCs DTO by category", description = "Retrieves all SOCs DTO by category.")
	@ApiResponse(responseCode = "200", description = "SOCs found")
	@ApiResponse(responseCode = "404", description = "No SOCs found")
	@GetMapping("/findallbycategory")
	public ResponseEntity<DataResponse> getSOCsByCategory(@RequestParam String category) {
		LOGGER.info("Received find soc by category request: " + category);
		List<SocDTO> socs = socService.getSOCsByCategory(category);
		if (socs != null && !socs.isEmpty()) {
			LOGGER.info("Socs found");
			return ResponseUtils.getSuccessDataResponse("SOC fetched successfully", socs);
		} else {
			LOGGER.error("No Soc vs found");
			return ResponseUtils.getSuccessDataResponse("No SOC found for the category", socs);
		}
	}

	/**
	 * Retrieves all SOC by category.
	 *
	 * @param category The category of the SOC to retrieve.
	 * @return ResponseEntity containing the list of SOC if found, or a NOT_FOUND
	 *         status with an error message if no SOCs are found.
	 */

	@Operation(summary = "Find SOC  name by category", description = "Retrieves all SOC  names list by category.")
	@ApiResponse(responseCode = "200", description = "SOC  found")
	@ApiResponse(responseCode = "404", description = "No SOC  found")
	@GetMapping("/getlistbycategory")
	public ResponseEntity<DataResponse> getSOCsListByCategory(String category) {
		LOGGER.info("Received find Soc by category request: " + category);
		List<String> socsList = socService.getSOCsListByCategory(category);
		if (socsList != null && !socsList.isEmpty()) {
			LOGGER.info("Socs found");
			return ResponseUtils.getSuccessDataResponse("SOC fetched successfully", socsList);
		} else {
			LOGGER.error("No Socs found");
			return ResponseUtils.getSuccessDataResponse("No SOC found for the category", socsList);
		}

	}

}
