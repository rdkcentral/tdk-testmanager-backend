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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.dto.ModuleCreateDTO;
import com.rdkm.tdkservice.dto.ModuleDTO;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.response.Response;
import com.rdkm.tdkservice.service.IModuleService;
import com.rdkm.tdkservice.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

/**
 * REST controller for managing modules.
 */
@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/module")
public class ModuleController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModuleController.class);

	@Autowired
	IModuleService moduleService;

	/**
	 * Creates a new module.
	 *
	 * @param moduleDTO the module creation data transfer object
	 * @return ResponseEntity with a success or failure message
	 */
	@Operation(summary = "Create a new module", description = "Creates a new module in the system.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Module created successfully"),
			@ApiResponse(responseCode = "500", description = "Failed to create module"),
			@ApiResponse(responseCode = "400", description = "Invalid input"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") })
	@PostMapping("/create")
	public ResponseEntity<Response> createModule(@RequestBody @Valid ModuleCreateDTO moduleDTO) {
		LOGGER.info("Creating new module: {}", moduleDTO);
		boolean isSaved = moduleService.saveModule(moduleDTO);
		if (isSaved) {
			LOGGER.info("Module created successfully: {}", moduleDTO);
			return ResponseUtils.getCreatedResponse("Module created successfully");
		} else {
			LOGGER.error("Failed to create module: {}", moduleDTO);
			throw new TDKServiceException("Failed to create module");

		}
	}

	/**
	 * Updates an existing module.
	 *
	 * @param moduleDTO the module data transfer object
	 * @return ResponseEntity with a success or failure message
	 */
	@Operation(summary = "Update an existing module", description = "Updates an existing module in the system.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Module updated successfully"),
			@ApiResponse(responseCode = "409", description = "Module already exists"),
			@ApiResponse(responseCode = "500", description = "Failed to update module"),
			@ApiResponse(responseCode = "400", description = "Invalid input"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Module not found") })
	@PutMapping("/update")
	public ResponseEntity<Response> updateModule(@Valid @RequestBody ModuleDTO moduleDTO) {
		LOGGER.info("Updating module: {}", moduleDTO);
		boolean isUpdated = moduleService.updateModule(moduleDTO);
		if (isUpdated) {
			LOGGER.info("Module updated successfully: {}", moduleDTO);
			return ResponseUtils.getSuccessResponse("Module updated successfully");
		} else {
			LOGGER.error("Failed to update module: {}", moduleDTO);
			throw new TDKServiceException("Failed to update module");
		}
	}

	/**
	 * Retrieves all modules.
	 *
	 * @return ResponseEntity with a list of all modules in DataResponse
	 */
	@Operation(summary = "Retrieve all modules", description = "Retrieves a list of all modules in the system.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successfully retrieved all modules"),
			@ApiResponse(responseCode = "404", description = "No modules found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "Internal server error") })
	@GetMapping("/findAll")
	public ResponseEntity<DataResponse> findAllModules() {
		LOGGER.info("Retrieving all modules");
		List<ModuleDTO> modules = moduleService.findAllModules();
		if (modules.isEmpty()) {
			LOGGER.warn("No modules found");
			return ResponseUtils.getSuccessDataResponse("No modules found", modules);
		} else {
			return ResponseUtils.getSuccessDataResponse("All modules retrieved successfully", modules);
		}

	}

	/**
	 * Retrieves a module by its ID.
	 *
	 * @param id the ID of the module
	 * @return ResponseEntity<DataResponse> with the module in DataResponse
	 */
	@Operation(summary = "Retrieve a module by its ID", description = "Retrieves a module by its ID.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successfully retrieved the module"),
			@ApiResponse(responseCode = "404", description = "Module not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "Internal server error") })
	@GetMapping("/findById")
	public ResponseEntity<DataResponse> findModuleById(@RequestParam UUID id) {
		LOGGER.info("Retrieving module by ID: {}", id);
		ModuleDTO module = moduleService.findModuleById(id);
		if (module != null) {
			LOGGER.info("Successfully retrieved module: {}", module);
			return ResponseUtils.getSuccessDataResponse(module);
		} else {
			LOGGER.warn("Module not found with ID: {}", id);
			return ResponseUtils.getNotFoundDataResponse("Module not found with ID: " + id, module);
		}
	}

	/**
	 * Retrieves module Details by its category.
	 *
	 * @param category the category of the module - RDKV,RDKB,RDKC
	 * @return ResponseEntity<DataResponse> with a list of modules
	 */
	@Operation(summary = "Retrieve a module by its category", description = "Retrieves a module by its category.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successfully retrieved the module"),
			@ApiResponse(responseCode = "404", description = "Module not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "Internal server error") })
	@GetMapping("/findAllByCategory")
	public ResponseEntity<DataResponse> findAllByCategory(@RequestParam String category) {
		LOGGER.info("Retrieving modules by category: {}", category);
		List<ModuleDTO> modules = moduleService.findAllByCategory(category);
		if (modules != null && !modules.isEmpty()) {
			LOGGER.info("Successfully retrieved modules by category: {}", category);
			return ResponseUtils.getSuccessDataResponse("Modules retrieved successfully", modules);
		} else {
			LOGGER.warn("No modules found for category: {}", category);
			return ResponseUtils.getSuccessDataResponse("No modules found for category", null);
		}
	}

	/**
	 * Retrieves module list by its category.(Seperately for rdkv and rdkvservice)
	 *
	 * @param category the category of the module - RDKV, RDKV_RDKService, RDKB,RDKC
	 * @return ResponseEntity with a list of modules in DataResponse
	 */
	@Operation(summary = "Retrieve a module by its category", description = "Retrieves a module by its category.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successfully retrieved the module"),
			@ApiResponse(responseCode = "404", description = "Module not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "Internal server error") })
	@GetMapping("/findAllModuleNamesBySubCategory")
	public ResponseEntity<DataResponse> findAllModuleNameByCategoryWithRDKService(@RequestParam String category) {
		LOGGER.info("Retrieving modules by category: {}", category);
		List<String> modules = moduleService.findAllModuleNamesBySubCategory(category);
		if (modules != null && !modules.isEmpty()) {
			LOGGER.info("Successfully retrieved modules by category: {}", category);
			return ResponseUtils.getSuccessDataResponse("Modules retrieved successfully", modules);
		} else {
			LOGGER.error("No modules found for category: {}", category);
			return ResponseUtils.getSuccessDataResponse("No modules found for category", null);
		}
	}

	/**
	 * Retrieves all module names by category, the data returned for RDKV will be
	 * combined one for RDKV and RDKVRDKService.
	 *
	 * @param category the category of the module - RDKV, RDKB, RDKC
	 * @return ResponseEntity with a list of module names in DataResponse
	 */
	@Operation(summary = "Retrieve a module by its category", description = "Retrieves a module by its category.")
	@ApiResponse(responseCode = "200", description = "Successfully retrieved the module")
	@ApiResponse(responseCode = "404", description = "Module not found")
	@GetMapping("/findAllModuleNamesByCategory")
	public ResponseEntity<?> findAllModuleNameByCategoryWithoutRDKService(@RequestParam String category) {
		LOGGER.info("Retrieving modules by category: {}", category);
		List<String> modules = moduleService.findAllModuleNameByCategory(category);
		if (modules != null && !modules.isEmpty()) {
			LOGGER.info("Successfully retrieved modules by category: {}", category);
			return ResponseUtils.getSuccessDataResponse("Modules list retrieved successfully", modules);
		} else {
			LOGGER.error("No modules found for category: {}", category);
			return ResponseUtils.getSuccessDataResponse("No modules found for category", null);
		}
	}

	/**
	 * Deletes a module by its ID.
	 *
	 * @param id the ID of the module
	 * @return ResponseEntity with a success or failure message in Response
	 */
	@Operation(summary = "Delete a module by its ID", description = "Deletes a module by its ID.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Module deleted successfully"),
			@ApiResponse(responseCode = "404", description = "Module not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "Internal server error") })
	@DeleteMapping("/delete")
	public ResponseEntity<Response> deleteModule(@RequestParam UUID id) {
		LOGGER.info("Deleting module by ID: {}", id);
		boolean isDeleted = moduleService.deleteModule(id);
		if (isDeleted) {
			LOGGER.info("Module deleted successfully: {}", id);
			return ResponseUtils.getSuccessResponse("Module is deleted successfully");
		} else {
			LOGGER.error("Module not found with ID: {}", id);
			return ResponseUtils.getNotFoundResponse("Module not found with ID: " + id);
		}
	}

	/**
	 * Retrieves all test groups.
	 *
	 * @return ResponseEntity with a list of all test groups
	 */
	@Operation(summary = "Retrieve all test groups", description = "Retrieves a list of all test groups from the enum.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successfully retrieved test groups"),
			@ApiResponse(responseCode = "404", description = "No test groups found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "Internal server error") })
	@GetMapping("/getAllTestGroups")
	public ResponseEntity<DataResponse> getAllTestGroups() {
		LOGGER.info("Retrieving all test groups from enum");
		List<String> testGroups = moduleService.findAllTestGroupsFromEnum();
		if (testGroups != null && !testGroups.isEmpty()) {
			LOGGER.info("Successfully retrieved all test groups from enum");
			return ResponseUtils.getSuccessDataResponse("Test groups retrieved successfully", testGroups);
		} else {
			LOGGER.error("No test groups found in enum");
			return ResponseUtils.getSuccessDataResponse("Test groups not available", null);
		}
	}

	/**
	 * Parses and saves the XML file.
	 *
	 * @param file the XML file
	 * @return ResponseEntity with a success or failure message
	 */
	@Operation(summary = "Parse and save XML", description = "Parses and saves the XML file.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "XML parsed and data saved successfully"),
			@ApiResponse(responseCode = "500", description = "Failed to parse XML"),
			@ApiResponse(responseCode = "400", description = "Invalid input"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "No data found") })
	@PostMapping("/uploadxml")
	public ResponseEntity<Response> parseXml(@RequestParam("file") MultipartFile file) {
		LOGGER.info("Received upload xml file request: " + file.getOriginalFilename());
		boolean isXmlUploaded = moduleService.parseAndSaveXml(file);
		if (isXmlUploaded) {
			LOGGER.info("XML parsed and data saved successfully");
			return ResponseUtils.getSuccessResponse("XML parsed and data saved successfully");
		} else {
			LOGGER.error("Could not upload the xml file");
			throw new TDKServiceException("Could not upload the xml file");
		}

	}

	/**
	 * Generates XML for a module.
	 *
	 * @param moduleName the name of the module
	 * @return ResponseEntity with the generated XML content
	 */

	@Operation(summary = "Generate XML for a module", description = "Generates XML for a module.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "XML generated successfully"),
			@ApiResponse(responseCode = "500", description = "Failed to generate XML"),
			@ApiResponse(responseCode = "400", description = "Invalid input"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "No data found") })
	@GetMapping(value = "/downloadxml", produces = "application/xml")
	public ResponseEntity<?> downloadModuleXML(@RequestParam String moduleName) {
		LOGGER.info("Downloading XML for module: {}", moduleName);
		String xmlContent = moduleService.generateXML(moduleName);
		LOGGER.info("XML content: {}", xmlContent);
		if (xmlContent == null) {
			LOGGER.error("Failed to download XML for module: {}", moduleName);
			throw new TDKServiceException("Failed to download XML for module: " + moduleName);
		}
		ByteArrayResource resource = new ByteArrayResource(xmlContent.getBytes());
		LOGGER.info("XML downloaded successfully for module: {}", moduleName);
		return ResponseEntity.status(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + moduleName + ".xml")
				.contentType(MediaType.APPLICATION_XML).body(resource);
	}

	/**
	 * Downloads all modules as a ZIP file.
	 *
	 * @param category the category of the modules
	 * @return ResponseEntity with the ZIP file
	 * 
	 */
	@Operation(summary = "Download modules as ZIP", description = "Downloads all modules as a ZIP file.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Modules downloaded successfully"),
			@ApiResponse(responseCode = "500", description = "Failed to download modules"),
			@ApiResponse(responseCode = "400", description = "Invalid input"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "No data found") })
	@GetMapping(value = "/downloadzip", produces = "application/zip")
	public ResponseEntity<?> downloadModulesAsZip(@RequestParam String category) {
		ByteArrayResource resource = moduleService.downloadModulesAsZip(category);
		LOGGER.info("Modules downloaded successfully as ZIP file for category: {}", category);
		return ResponseEntity.status(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=modules.zip")
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);

	}
}