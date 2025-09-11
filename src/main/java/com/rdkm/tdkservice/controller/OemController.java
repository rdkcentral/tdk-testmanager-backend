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

import com.rdkm.tdkservice.dto.OemCreateDTO;
import com.rdkm.tdkservice.dto.OemDTO;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.response.Response;
import com.rdkm.tdkservice.service.IOemService;
import com.rdkm.tdkservice.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/**
 * The controller class that handles the API endpoints related to oems.
 */
@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/oem")
public class OemController {

	private static final Logger LOGGER = LoggerFactory.getLogger(OemController.class);

	@Autowired
	IOemService iOemService;

	/**
	 * Creates a new oem type.
	 *
	 * @param oemDTO The request object containing the details of the oem.
	 * @return ResponseEntity containing the created oem if successful, or an error
	 *         message if unsuccessful.
	 * @throws ResourceAlreadyExistsException if a device type with the same name
	 *                                        already exists.
	 */
	@Operation(summary = "Create a new oemDTO type", description = "Creates a new Oem type in the system.")
	@ApiResponse(responseCode = "201", description = "Oem created successfully")
	@ApiResponse(responseCode = "500", description = "Error in saving oem type data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@PostMapping("/create")
	public ResponseEntity<Response> createOem(@RequestBody @Valid OemCreateDTO oemDTO) {
		LOGGER.info("Received create oemDTO type request: " + oemDTO.toString());
		boolean isOemCreated = iOemService.createOem(oemDTO);
		if (isOemCreated) {
			LOGGER.info("Oem created successfully");
			return ResponseUtils.getCreatedResponse("Device Type created successfully");
		} else {
			LOGGER.error("Error in saving Oem type data");
			throw new TDKServiceException("Error in creating OEM");
		}

	}

	/**
	 * Retrieves all oem types.
	 *
	 * @return ResponseEntity containing the list of oem types if found, or a
	 *         NOT_FOUND status with an error message if not found.
	 */
	@Operation(summary = "Find all oem types", description = "Retrieves all oem types from the system.")
	@ApiResponse(responseCode = "200", description = "oem types found")
	@ApiResponse(responseCode = "404", description = "No oem types found")
	@GetMapping("/findall")
	public ResponseEntity<DataResponse> findAllOemTypes() {
		LOGGER.info("Received find all oem types request");
		List<OemDTO> oemDTOList = iOemService.getAllOem();
		if (oemDTOList != null && !oemDTOList.isEmpty()) {
			LOGGER.info("Oem types found successfully");
			return ResponseUtils.getSuccessDataResponse("OEM fetched successfully", oemDTOList);
		} else {
			LOGGER.error("No oem types found");
			return ResponseUtils.getSuccessDataResponse("No OEM available", oemDTOList);
		}

	}

	/**
	 * Deletes a oem by ID.
	 *
	 * @param id The ID of the oem to delete.
	 * @return A ResponseEntity with the status and message indicating the result of
	 *         the deletion.
	 */
	@Operation(summary = "Delete a oem type", description = "Deletes a oem type from the system.")
	@ApiResponse(responseCode = "200", description = "oem deleted successfully")
	@ApiResponse(responseCode = "404", description = "oem not found")
	@DeleteMapping("/delete")
	public ResponseEntity<Response> deleteOemType(@RequestParam UUID id) {
		LOGGER.info("Received delete oem type request for ID: " + id);
		iOemService.deleteOem(id);
		return ResponseUtils.getSuccessResponse("OEM deleted successfully");
	}

	/**
	 * Retrieves a oem by its ID.
	 *
	 * @param id the ID of the oem to retrieve
	 * @return a ResponseEntity containing the oem if found, or a NOT_FOUND status
	 *         with an error message if not found
	 */
	@Operation(summary = "Find oem by ID", description = "Retrieves a oem by its ID.")
	@ApiResponse(responseCode = "200", description = "oem found")
	@ApiResponse(responseCode = "500", description = "oem not found")
	@GetMapping("/findbyid")
	public ResponseEntity<DataResponse> findById(@RequestParam UUID id) {
		LOGGER.info("Received find oem by id request: " + id);
		OemDTO oemDTO = iOemService.findById(id);
		return ResponseUtils.getSuccessDataResponse("OEM found", oemDTO);

	}

	/**
	 * Updates the oem with the specified ID.
	 *
	 * @param id           the ID of the oem to update
	 * @param oemUpdateDTO the updated oem details
	 * @return a ResponseEntity containing the updated oem if successful, or an
	 *         error message if unsuccessful
	 */
	@Operation(summary = "Update a oem", description = "Updates oem in the system.")
	@ApiResponse(responseCode = "200", description = "oem updated successfully")
	@ApiResponse(responseCode = "500", description = "Error in updating oem")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@PutMapping("/update")
	public ResponseEntity<DataResponse> updateOemType(@RequestBody OemDTO oemUpdateDTO) {
		LOGGER.info("Received update oemUpdateDTO request: " + oemUpdateDTO.toString());
		OemDTO oemUpdateDto = iOemService.updateOem(oemUpdateDTO);
		if (oemUpdateDto != null) {
			LOGGER.info("oem updated successfully");
			return ResponseUtils.getSuccessDataResponse("oem updated successfully", oemUpdateDto);
		} else {
			LOGGER.error("Error in updating oem");
			throw new TDKServiceException("Error in updating OEM");
		}

	}

	/**
	 * Retrieves all oem by category.
	 *
	 * @param category the category of the oem to retrieve
	 * @return a ResponseEntity containing the list of oem if found, or a NOT_FOUND
	 *         status with an error message if not found
	 */
	@Operation(summary = "Find oem DTO by category", description = "Retrieves all oem by category.")
	@ApiResponse(responseCode = "200", description = "oem found")
	@ApiResponse(responseCode = "404", description = "No oem found")
	@GetMapping("/findallbycategory")
	public ResponseEntity<DataResponse> getOemsByCategory(String category) {
		LOGGER.info("Received find oem by category request: " + category);
		List<OemDTO> oemDTOList = iOemService.getOemsByCategory(category);
		if (oemDTOList != null && !oemDTOList.isEmpty()) {
			LOGGER.info("oem found");
			return ResponseUtils.getSuccessDataResponse("Oem fetched successfully", oemDTOList);
		} else {
			LOGGER.error("No oem found for the category");
			return ResponseUtils.getSuccessDataResponse("No oem found for the category", oemDTOList);
		}
	}

	/**
	 * Retrieves all oems by category.
	 *
	 * @param category the category of the oems to retrieve
	 * @return a ResponseEntity containing the list of oems if found, or a NOT_FOUND
	 *         status with an error message if not found
	 */

	@Operation(summary = "Get oem name list by category", description = "Retrieves all oems by category.")
	@ApiResponse(responseCode = "200", description = "oems retrieved successfully")
	@ApiResponse(responseCode = "404", description = "No oems found")
	@GetMapping("/getlistbycategory")
	public ResponseEntity<DataResponse> getOemListByCategory(@RequestParam String category) {
		LOGGER.info("Received find oem by category request: " + category);
		List<String> oemListByCategory = iOemService.getOemListByCategory(category);
		if (oemListByCategory != null && !oemListByCategory.isEmpty()) {
			LOGGER.info("oem found successfully");
			return ResponseUtils.getSuccessDataResponse("Oem fetched successfully", oemListByCategory);
		} else {
			LOGGER.error("No oem found for the category");
			return ResponseUtils.getSuccessDataResponse("No oem found for the category", oemListByCategory);
		}
	}

}
