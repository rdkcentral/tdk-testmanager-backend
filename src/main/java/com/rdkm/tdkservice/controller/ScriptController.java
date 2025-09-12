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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.dto.ScriptCreateDTO;
import com.rdkm.tdkservice.dto.ScriptDTO;
import com.rdkm.tdkservice.dto.ScriptDetailsResponse;
import com.rdkm.tdkservice.dto.ScriptListDTO;
import com.rdkm.tdkservice.dto.ScriptModuleDTO;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.response.Response;
import com.rdkm.tdkservice.service.IScriptService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/**
 * The ScriptController class is used to handle script related operations. It
 * contains the APIs to create, update, delete and get the script.
 */
@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/script")
public class ScriptController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptController.class);

	@Autowired
	IScriptService scriptService;

	/**
	 * This method is used to create the script. The script is created by uploading
	 * the script file and providing the script details.
	 * 
	 * @param scriptCreateDTO - the script create dto
	 * @param file            - the script file
	 * @return ResponseEntity - the ResponseEntity<Response>
	 */
	@Operation(summary = "Create Script", description = "Create Script")
	@ApiResponse(responseCode = "201", description = "Script created successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "500", description = "Issue in creating script")
	@PostMapping("/create")
	public ResponseEntity<Response> createScript(
			@Valid @RequestPart("scriptCreateData") ScriptCreateDTO scriptCreateDTO,
			@RequestPart("scriptFile") MultipartFile scriptFile) {
		LOGGER.info("Received create script request: " + scriptCreateDTO.toString());
		boolean isScriptCreated = scriptService.saveScript(scriptFile, scriptCreateDTO);
		if (isScriptCreated) {
			LOGGER.info("Script created successfully");
			return ResponseUtils.getCreatedResponse("Script created successfully");
		} else {
			LOGGER.error("Error in creating script");
			throw new TDKServiceException("Failed to create device");
		}
	}

	/**
	 * This method is used to update the script. The script is updated by uploading
	 * 
	 * @param scriptUpdateDTO - the script update dto
	 * @param scriptFile      - the script
	 * @return ResponseEntity - the response entity
	 */
	@Operation(summary = "Update Script", description = "Update Script")
	@ApiResponse(responseCode = "200", description = "Script updated successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "500", description = "Issue in updating script")
	@PutMapping("/update")
	public ResponseEntity<Response> updateScript(@Valid @RequestPart("scriptUpdateData") ScriptDTO scriptUpdateDTO,
			@RequestPart("scriptFile") MultipartFile scriptFile) {
		LOGGER.info("Received update script request: " + scriptUpdateDTO.toString());
		boolean isScriptUpdated = scriptService.updateScript(scriptFile, scriptUpdateDTO);
		if (isScriptUpdated) {
			LOGGER.info("Script updated successfully");
			return ResponseUtils.getSuccessResponse("Script updated successfully");
		} else {
			LOGGER.error("Error in updating script");
			throw new TDKServiceException("Error in updating script");
		}
	}

	/**
	 * This method is used to delete the script
	 * 
	 * @param scriptId - the script id
	 * @return ResponseEntity<Response> - the response entity
	 */
	@Operation(summary = "Delete Script", description = "Delete Script")
	@ApiResponse(responseCode = "200", description = "Script deleted successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@DeleteMapping("/delete")
	public ResponseEntity<Response> deleteScript(@RequestParam UUID id) {
		LOGGER.info("Received delete script request: " + id);
		boolean isScriptDeleted = scriptService.deleteScript(id);
		if (isScriptDeleted) {
			LOGGER.info("Script deleted successfully");
			return ResponseUtils.getSuccessResponse("Script deleted successfully");
		} else {
			LOGGER.error("Error in deleting script");
			throw new TDKServiceException("Error in deleting script");
		}
	}

	/**
	 * This method is used to get all the scripts list by module. This will return
	 * the list of scripts for the given module as script id and script name.
	 * 
	 * @param module - the module
	 * @return ResponseEntity - the response entity
	 */
	@Operation(summary = "Get all scripts by module", description = "Get all scripts by module")
	@ApiResponse(responseCode = "200", description = "Scripts fetched successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/findListByModule")
	public ResponseEntity<DataResponse> findAllScriptsByModule(@RequestParam String module) {
		LOGGER.info("Received get all scripts request for module: " + module);
		List<ScriptListDTO> scripts = scriptService.findAllScriptsByModule(module);
		if (scripts != null && !scripts.isEmpty()) {
			LOGGER.info("Scripts fetched successfully  for module: " + module);
			return ResponseUtils.getSuccessDataResponse("Scripts fetched successfully", scripts);
		} else {
			LOGGER.error("Scripts not found for module: " + module);
			return ResponseUtils.getSuccessDataResponse("Scripts not found for module", null);
		}
	}

	/**
	 * This method is used to get the script details by script id.
	 * 
	 * @param scriptId - the script id
	 * @return ResponseEntity<DataResponse> - the response entity
	 */
	@Operation(summary = "Get Script by Id", description = "Get Script by Id")
	@ApiResponse(responseCode = "200", description = "Script fetched successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/findById")
	public ResponseEntity<DataResponse> findScriptById(@RequestParam UUID id) {
		LOGGER.info("Received get script by id request for script id: " + id);
		ScriptDTO script = scriptService.findScriptById(id);
		if (script != null) {
			LOGGER.info("Script fetched successfully for script id: " + id);
			return ResponseUtils.getSuccessDataResponse("Script fetched successfully", script);
		} else {
			LOGGER.error("Error is fetching script for script id: " + id);
			throw new TDKServiceException("Error is fetching script for script id: " + id);
		}
	}

	/**
	 * This method is used to get all the scripts by module with category. This will
	 * return the list of scripts for all the modules by the given category.
	 * 
	 * @param category - the category
	 * @return ResponseEntity - the response entity
	 */
	@Operation(summary = "Get all scripts by module with category", description = "Get all scripts by module with category")
	@ApiResponse(responseCode = "200", description = "Scripts fetched successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/findAllByModuleWithCategory")
	public ResponseEntity<DataResponse> findAllScriptByModuleWithCategory(@RequestParam String category) {
		LOGGER.info("Received get all scripts request for category: " + category);
		List<ScriptModuleDTO> scripts = scriptService.findAllScriptByModuleWithCategory(category);
		if (scripts != null && !scripts.isEmpty()) {
			LOGGER.info("Scripts fetched successfully for category: " + category);
			return ResponseUtils.getSuccessDataResponse("Scripts fetched successfully for category: " + category,
					scripts);
		} else {
			LOGGER.error("No script found for category", category);
			return ResponseUtils.getSuccessDataResponse("No script found for category: " + category, null);
		}
	}

	/**
	 * This method is used to download the test case as excel
	 * 
	 * @param testScriptName - the test script name
	 * @return ResponseEntity - the response entity
	 */
	@Operation(summary = "Download Test Case as Excel", description = "Download Test Case as Excel")
	@ApiResponse(responseCode = "200", description = "Test case downloaded successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/downloadTestCaseAsExcel")
	public ResponseEntity<?> downloadTestCaseAsExcel(@RequestParam String testScriptName) {
		ByteArrayInputStream in = scriptService.testCaseToExcel(testScriptName);
		// Prepare response with the Excel file
		if (in == null || in.available() == 0) {
			throw new TDKServiceException("Error in downloading test case as excel");
		}
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Disposition",
				"attachment; filename=TestCase_" + testScriptName + Constants.EXCEL_FILE_EXTENSION);
		LOGGER.info("Test case downloaded successfully");
		return ResponseEntity.status(HttpStatus.OK).headers(headers).contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(new InputStreamResource(in));
	}

	/**
	 * This method is used to download the test case as excel by module
	 * 
	 * @param moduleName - the module name
	 * @return ResponseEntity - the response entity
	 */
	@Operation(summary = "Download Test Case as Excel by Module", description = "Download Test Case as Excel by Module")
	@ApiResponse(responseCode = "200", description = "Test case downloaded successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/downloadTestCaseAsExcelByModule")
	public ResponseEntity<?> downloadTestCaseAsExcelByModule(@RequestParam String moduleName) {
		ByteArrayInputStream in = scriptService.testCaseToExcelByModule(moduleName);
		if (in == null || in.available() == 0) {
			LOGGER.error("Error in downloading test case as excel by module");
			throw new TDKServiceException("Error in downloading test case as excel by module");

		}
		// Prepare response with the Excel file
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Disposition",
				"attachment; filename=TestCase_" + moduleName + Constants.EXCEL_FILE_EXTENSION);
		LOGGER.info("Downloaded test case as excel by module");
		return ResponseEntity.status(HttpStatus.OK).headers(headers).contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(new InputStreamResource(in));
	}

	/**
	 * This method is used to get all the scripts by category. This will return the
	 * list of scripts for the given category.
	 * 
	 * @param category - the category
	 * @return ResponseEntity<DataResponse> - the response entity
	 */
	@Operation(summary = "Get all scripts by  category", description = "Get all scripts by category")
	@ApiResponse(responseCode = "200", description = "Scripts fetched successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/findListByCategory")
	public ResponseEntity<DataResponse> findAllScriptByCategory(@RequestParam String category) {
		LOGGER.info("Received get all scripts request for category: " + category);
		List<ScriptListDTO> scripts = scriptService.findAllScriptsByCategory(category);
		if (scripts != null) {
			LOGGER.info("Scripts fetched successfully for category: " + category);
			return ResponseUtils.getSuccessDataResponse("Scripts fetched successfully for category: " + category,
					scripts);
		} else {
			LOGGER.error("No script found for category");
			return ResponseUtils.getSuccessDataResponse("No script found for category", null);
		}
	}

	/**
	 * This method is used to download the script
	 * 
	 * @param scriptName - the script name
	 * @return ResponseEntity - the response entity
	 * @throws IOException
	 */
	@Operation(summary = "Download Script", description = "Download Script")
	@ApiResponse(responseCode = "200", description = "Script downloaded successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/downloadScriptDataZip")
	public ResponseEntity<?> downloadScript(@RequestParam String scriptName) {
		byte[] zipBytes = scriptService.generateScriptZip(scriptName);
		if (zipBytes == null) {
			LOGGER.error("Error in downloading script ");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error in downloading script");
		}
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Script-" + scriptName + ".zip");
		LOGGER.info("Script downloaded successfully");
		return ResponseEntity.status(HttpStatus.OK).headers(headers).contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(zipBytes);
	}

	/**
	 * This method is used to upload the ZIP file
	 * 
	 * @param file - the ZIP file
	 * @return ResponseEntity - the response entity
	 */

	@Operation(summary = "Upload ZIP file", description = "Upload ZIP file")
	@ApiResponse(responseCode = "200", description = "ZIP file has been successfully processed")
	@ApiResponse(responseCode = "500", description = "Error in processing ZIP file")
	@PostMapping("/uploadScriptDataZip")
	public ResponseEntity<Response> uploadScript(@RequestParam("file") MultipartFile file) {
		LOGGER.info("Received upload ZIP file request: " + file.getOriginalFilename());
		boolean scriptUpload;
		try {
			scriptUpload = scriptService.uploadZipFile(file);
		} catch (IOException e) {
			LOGGER.error("Error in processing ZIP file");
			throw new TDKServiceException("Error in processing ZIP file");
		}
		if (scriptUpload) {
			LOGGER.info("ZIP file has been successfully processed");
			return ResponseUtils.getSuccessResponse("ZIP file has been successfully processed");
		} else {
			LOGGER.error("Error in processing ZIP file");
			throw new TDKServiceException("Error in processing ZIP file");
		}

	}

	/**
	 * This method is used to get the script template details by primitive test
	 * name.
	 *
	 * @param primitiveTestName - the primitive test name
	 * @return ResponseEntity - the response entity
	 */
	@Operation(summary = "Get Script Template", description = "Get Script Template")
	@ApiResponse(responseCode = "200", description = "Script template fetched successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "500", description = "Issue in fetching script template")
	@GetMapping("/getScriptTemplate")
	public ResponseEntity<String> getScriptTemplate(@RequestParam String primitiveTestName) {
		String scriptTemplate = scriptService.scriptTemplate(primitiveTestName);
		return ResponseEntity.status(HttpStatus.OK).body(scriptTemplate);
	}

	/**
	 * This method is used to download all the test cases as Excel by Module ZIP by
	 * Category
	 * 
	 * @param category - the category
	 * @return ResponseEntity - the response entity
	 */
	@Operation(summary = "Download all Test Case as Module Excel  zip by Category", description = "Download all Test Case as Excel by Module ZIP by Category")
	@ApiResponse(responseCode = "200", description = "Test case downloaded successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/downloadAllTestcaseZipByCategory")
	public ResponseEntity<?> downloadAllTestCaseAsZipByCategory(@RequestParam String category) {
		LOGGER.info("Received download all test case as excel as zip  by category : " + category);
		ByteArrayInputStream in = scriptService.testCaseToExcelByCategory(category);
		if (in == null || in.available() == 0) {
			LOGGER.error("Error in downloading all test case as excel by module ZIP by category");
			throw new TDKServiceException("Error in downloading all test case as excel by module ZIP by category");
		}
		// Prepare response with the Excel file
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=TestCase_" + category + Constants.ZIP_EXTENSION);
		LOGGER.info("Downloaded all test case as excel by module ZIP by category");
		return ResponseEntity.status(HttpStatus.OK).headers(headers).contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(new InputStreamResource(in));
	}

	/**
	 * This method is used to get the list of script by category.
	 *
	 * @param category         - the category
	 * @param isThunderEnabled - the isThunderEnabled
	 * @return ResponseEntity<DataResponse> - the list of script in
	 */
	@Operation(summary = "Get List of Script by Category", description = "Get List of Script by Category")
	@ApiResponse(responseCode = "200", description = "List of script fetched successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/getListofScriptByCategory")
	public ResponseEntity<DataResponse> getListofScriptByCategory(@RequestParam String category,
			@RequestParam boolean isThunderEnabled) {
		LOGGER.info("Received request to get list of scripts by category: {} and isThunderEnabled: {}", category,
				isThunderEnabled);
		List<ScriptDetailsResponse> scripts = scriptService.getListofScriptNamesByCategory(category, isThunderEnabled);

		if (scripts != null) {
			LOGGER.info("Scripts fetched successfully for category: {} and isThunderEnabled: {}", category,
					isThunderEnabled);
			return ResponseUtils.getSuccessDataResponse("Scripts fetched successfully for category: " + category
					+ "and isThunderEnabled: " + isThunderEnabled, scripts);
		} else {
			LOGGER.warn("No scripts found for category: {} and isThunderEnabled: {}", category, isThunderEnabled);
			return ResponseUtils.getSuccessDataResponse(
					"No scripts found for category: " + category + "and isThunderEnabled: " + isThunderEnabled, null);
		}
	}

	/**
	 * This method is used to download the markdown file for a given script name.
	 *
	 * @param scriptName - the name of the script
	 * @return ResponseEntity - the response entity containing the markdown file
	 * @throws IOException - if an error occurs while reading the file
	 */
	@Operation(summary = "Download Markdown File", description = "Download the markdown file for a given script name")
	@ApiResponse(responseCode = "200", description = "Markdown file downloaded successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "500", description = "Error in downloading markdown file")
	@GetMapping("/downloadmdfilebyname")
	public ResponseEntity<?> downloadMarkdown(@RequestParam String scriptName) throws IOException {
		LOGGER.info("Received request to download markdown file for scriptName: {}", scriptName);
		ByteArrayInputStream mdFile = scriptService.createMarkdownFile(scriptName);
		if (mdFile == null || mdFile.available() == 0) {
			LOGGER.error("Markdown file not found for scriptName: {}", scriptName);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error in downloading markdown file");
		}

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + scriptName + ".md")
				.contentType(MediaType.parseMediaType("text/markdown")).body((new InputStreamResource(mdFile)));
	}

	/**
	 * This method is used to download the markdown file for a given script ID.
	 *
	 * @param scriptId - the UUID of the script
	 * @return ResponseEntity - the response entity containing the markdown file
	 * @throws IOException - if an error occurs while reading the file
	 */
	@Operation(summary = "Download Markdown File by Script ID", description = "Download the markdown file for a given script ID")
	@ApiResponse(responseCode = "200", description = "Markdown file downloaded successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "500", description = "Error in downloading markdown file")
	@GetMapping("/downloadmdfilebyid")
	public ResponseEntity<?> downloadMarkdownById(@RequestParam UUID scriptId) throws IOException {
		LOGGER.info("Received request to download markdown file for scriptId: {}", scriptId);

		ByteArrayInputStream mdFile = scriptService.createMarkdownFilebyScriptId(scriptId);
		if (mdFile == null || mdFile.available() == 0) {
			LOGGER.error("Markdown file not found for scriptId: {}", scriptId);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error in downloading markdown file");
		}

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + scriptId + ".md")
				.contentType(MediaType.parseMediaType("text/markdown")).body((new InputStreamResource(mdFile)));
	}

	/**
	 * API to trigger default test suite creation for all existing modules. Useful
	 * for data recovery or system initialization scenarios.
	 */
	@Operation(summary = "Create/Update Default Test Suites for All Modules", description = "Creates or updates default test suites for all modules in all categories.")
	@ApiResponse(responseCode = "200", description = "Default test suites created/updated successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@PostMapping("/createOrUpdateDefaultTestSuites")
	public ResponseEntity<Response> createOrUpdateDefaultTestSuites() {
		LOGGER.info("Received request to create/update default test suites");
		scriptService.defaultTestSuiteCreationForExistingModule();
		return ResponseUtils.getSuccessResponse("Default test suites created/updated successfully");
	}

	/*
	 * This method is used to get all the scripts list by script group. This will
	 * return the list of scripts for the given script group as script id and script
	 * name.
	 *
	 * @param scriptGroup - the script group
	 * 
	 * @return ResponseEntity - the response entity
	 */
	@Operation(summary = "Get all scripts by script group", description = "Get all scripts by script group")
	@ApiResponse(responseCode = "200", description = "Scripts fetched successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/findScriptByTestSuite")
	public ResponseEntity<DataResponse> findAllScriptsByTestSuite(@RequestParam String scriptGroup) {
		LOGGER.info("Received get all scripts request for test suite: " + scriptGroup);
		List<ScriptListDTO> scripts = scriptService.findAllScriptsByTestSuite(scriptGroup);
		if (scripts != null && !scripts.isEmpty()) {
			LOGGER.info("Scripts fetched successfully for script group: " + scriptGroup);
			return ResponseUtils.getSuccessDataResponse("Scripts fetched successfully", scripts);
		} else {
			LOGGER.error("Scripts not found for script group: " + scriptGroup);
			return ResponseUtils.getSuccessDataResponse("Scripts not found for script group", null);
		}

	}

	/**
	 * This method is used to get the module execution time.
	 *
	 * @param moduleName - the module name
	 * @return ResponseEntity<DataResponse> - the response entity with execution
	 *         time value
	 */
	@Operation(summary = "Get module execution time", description = "Get the execution time for a specific module")
	@ApiResponse(responseCode = "200", description = "Module execution time fetched successfully")
	@ApiResponse(responseCode = "404", description = "Module not found")
	@ApiResponse(responseCode = "500", description = "Failed to fetch module execution time")
	@GetMapping("/getModuleScriptTimeOut")
	public ResponseEntity<DataResponse> getModuleScriptTimeout(@RequestParam String moduleName) {
		LOGGER.info("Received request to get module execution time for module: " + moduleName);
		Integer timeout = scriptService.getModuleScriptTimeout(moduleName);
		LOGGER.info("Module execution time fetched successfully for module: " + moduleName);
		return ResponseUtils.getSuccessDataResponse("Module execution time fetched successfully", timeout);
	}
}
