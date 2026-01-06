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
package com.rdkm.tdkservice.controller;

import java.io.ByteArrayInputStream;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.dto.TestSuiteCreateDTO;
import com.rdkm.tdkservice.dto.TestSuiteCustomDTO;
import com.rdkm.tdkservice.dto.TestSuiteDTO;
import com.rdkm.tdkservice.dto.TestSuiteDetailsResponse;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.response.Response;
import com.rdkm.tdkservice.service.ITestSuiteService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/**
 * The TestSuiteController class is a REST controller that handles all the
 * requests related to the test suite.
 */
@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/testsuite")
public class TestSuiteController {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestSuiteController.class);

	@Autowired
	ITestSuiteService testSuiteService;

	/**
	 * Create a test suite.
	 * 
	 * @param testSuiteCreateDTO
	 * @return testSUite list
	 */
	@Operation(summary = "Create test suite", description = "Create test suite")
	@ApiResponse(responseCode = "201", description = "test suite created successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "500", description = "Issue in creating test suite")
	@PostMapping("/create")
	public ResponseEntity<Response> createTestSuite(@RequestBody @Valid TestSuiteCreateDTO testSuiteCreateDTO) {
		LOGGER.info("Received create test suite request: " + testSuiteCreateDTO.toString());
		boolean isTestSuiteCreated = testSuiteService.createTestSuite(testSuiteCreateDTO);
		if (isTestSuiteCreated) {
			LOGGER.info("Test suite created successfully");
			return ResponseUtils.getCreatedResponse("Test suite created successfully");
		} else {
			LOGGER.error("Error in creating script");
			throw new TDKServiceException("Error in creating script");
		}

	}

	/**
	 * Update the test suite.
	 * 
	 * @param testSuiteDTO - the test suite dto
	 * @return ResponseEntity<Response> - the response entity
	 */
	@Operation(summary = "Update test suite", description = "Update test suite")
	@ApiResponse(responseCode = "200", description = "test suite updated successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "500", description = "Issue in updating test suite")
	@PutMapping("/update")
	public ResponseEntity<Response> updateTestSuite(@RequestBody @Valid TestSuiteDTO testSuiteDTO) {
		LOGGER.info("Received update test suite request: " + testSuiteDTO.toString());
		boolean isTestSuiteUpdated = testSuiteService.updateTestSuite(testSuiteDTO);
		if (isTestSuiteUpdated) {
			LOGGER.info("Test suite updated successfully");
			return ResponseUtils.getSuccessResponse("Test suite updated successfully");
		} else {
			LOGGER.error("Error in updating test suite");
			throw new TDKServiceException("Error in updating test suite");
		}

	}

	/**
	 * This method is used to get the test suite by id
	 * 
	 * @param testSuiteId - the test suite id
	 * @return ResponseEntity<DataResponse> - the response entity
	 */
	@Operation(summary = "Get TestSuite By Id", description = "Get test suite by id")
	@ApiResponse(responseCode = "200", description = "Scriptgroup fetched successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/findById")
	public ResponseEntity<DataResponse> getTestSuiteById(@RequestParam UUID id) {
		LOGGER.info("Received get testSuite by id request: " + id);
		TestSuiteDTO testSuiteDTO = testSuiteService.findTestSuiteById(id);
		if (testSuiteDTO != null) {
			LOGGER.info("Test suite fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Test suite fetched successfully", testSuiteDTO);
		} else {
			LOGGER.error("No test suite found");
			return ResponseUtils.getNotFoundDataResponse("No test suite found" + id, null);
		}
	}

	/**
	 * This method is used to get All the test suite by category
	 * 
	 * @param category - the test suite category
	 * @return ResponseEntity - the response entity
	 */
	@Operation(summary = "Get All TestSuite By Category", description = "Get All test suite by category")
	@ApiResponse(responseCode = "200", description = "Scriptgroup fetched successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/findAllByCategory")
	public ResponseEntity<DataResponse> getAllTestSuiteByCategory(@RequestParam String category) {
		LOGGER.info("Received get testSuite by category request: " + category);
		List<TestSuiteDTO> testSuiteDTOList = testSuiteService.findAllTestSuiteByCategory(category);
		if (testSuiteDTOList != null) {
			LOGGER.info("Test suite fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Test suite fetched successfully", testSuiteDTOList);
		} else {
			LOGGER.error("No test suite found by category");
			return ResponseUtils.getSuccessDataResponse("No Test suite details found for category" + category, null);
		}
	}

	/**
	 * This method is used to delete the test suite
	 * 
	 * @param scriptId - the test suite id
	 * @return ResponseEntity - the response entity
	 */
	@Operation(summary = "Delete TestSuite", description = "Delete test suite")
	@ApiResponse(responseCode = "200", description = "Scriptgroup deleted successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@DeleteMapping("/delete")
	public ResponseEntity<Response> deleteScript(@RequestParam UUID id) {
		LOGGER.info("Received delete testSuite request: " + id);
		boolean isScriptDeleted = testSuiteService.deleteTestSuite(id);
		if (isScriptDeleted) {
			LOGGER.info("Test suite deleted successfully");
			return ResponseUtils.getSuccessResponse("Test suite is deleted successfully");
		} else {
			LOGGER.error("Error in deleting test suite");
			throw new TDKServiceException("Error in deleting test suite");
		}
	}

	/**
	 * This method is used to download the test cases details for all the scripts in
	 * test suite as excel
	 * 
	 * @param testSuite - the test suite name
	 * @return ResponseEntity - the response entity
	 */
	@Operation(summary = "Download TestCases", description = "Download TestCases")
	@ApiResponse(responseCode = "200", description = "TestCases downloaded successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "500", description = "Issue in downloading test cases")
	@GetMapping("/downloadTestCases")
	public ResponseEntity<?> downloadTestCasesAsExcel(@RequestParam String testSuite) {
		LOGGER.info("Received download test cases details for all the scripts in test suite as excel request:");
		ByteArrayInputStream in = testSuiteService.getTestCasesInTestSuiteAsExcel(testSuite);
		if (in == null || in.available() == 0) {
			LOGGER.error("Error in downloading test cases details for all the scripts in test suite as excel");
			throw new TDKServiceException(
					"Error in downloading test cases details for all the scripts in test suite as excel");
		}
		// Prepare response with the Excel file
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Disposition",
				"attachment; filename=SuiteTestcases_" + testSuite + Constants.EXCEL_FILE_EXTENSION);
		LOGGER.info("Downloaded test case as excel by module");
		return ResponseEntity.status(HttpStatus.OK).headers(headers).contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(new InputStreamResource(in));
	}

	/**
	 * This method is used to create the custom test suite
	 * 
	 * @param testSuiteCustomDTO - the test suite custom dto
	 * @return ResponseEntity - the response entity
	 */
	@Operation(summary = "Create Custom Test Suite", description = "Create Custom Test Suite")
	@ApiResponse(responseCode = "201", description = "Custom Test Suite created successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "500", description = "Issue in creating Custom Test Suite")
	@PostMapping("/createCustomTestSuite")
	public ResponseEntity<Response> createCustomTestSuite(@RequestBody @Valid TestSuiteCustomDTO testSuiteCustomDTO) {
		LOGGER.info("Received create custom test suite request: " + testSuiteCustomDTO.toString());
		boolean isTestSuiteCreated = testSuiteService.createCustomTestSuite(testSuiteCustomDTO);
		if (isTestSuiteCreated) {
			LOGGER.info("Custom Test Suite created successfully");
			return ResponseUtils.getSuccessResponse("Custom Test Suite created successfully");
		} else {
			LOGGER.error("Error in creating Custom Test Suite");
			throw new TDKServiceException("Error in creating Custom Test Suite");
		}

	}

	/**
	 * This method is used to download the test suite as XML
	 * 
	 * @param testSuite - the test suite name
	 * @return ResponseEntity - the response entity
	 */
	@Operation(summary = "Download test suite xml", description = "Download test suite xml")
	@ApiResponse(responseCode = "200", description = "test suite xml downloaded successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "500", description = "Issue in downloading test suite xml")
	@GetMapping("/downloadTestSuiteXml")
	public ResponseEntity<?> downloadTestSuiteXML(@RequestParam String testSuite) {
		LOGGER.info("Received download test suite XML request:");
		ByteArrayInputStream inputStream = testSuiteService.downloadTestSuiteAsXML(testSuite);
		if (inputStream == null || inputStream.available() == 0) {
			LOGGER.error("Error in downloading test suite as XML");
			throw new TDKServiceException("Error in creating Custom Test Suite");
		}
		// Prepare response with the XML file
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Disposition", "attachment; filename=" + testSuite + Constants.XML_EXTENSION);
		LOGGER.info("Downloaded test suite as XML");
		return ResponseEntity.status(HttpStatus.OK).headers(headers).contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(new InputStreamResource(inputStream));
	}

	/**
	 * This method is used to upload the test suite as XML
	 * 
	 * @param scriptFile - the test suite xml file
	 * @return ResponseEntity - the response entity
	 */
	@Operation(summary = "Upload test suite xml", description = "Upload test suite xml")
	@ApiResponse(responseCode = "200", description = "test suite uploaded successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "500", description = "Issue in uploading test suite")
	@PostMapping("/uploadTestSuiteXml")
	public ResponseEntity<Response> uploadTestSuiteXML(@RequestPart("testSuite") MultipartFile testSuite) {
		boolean isTestSuiteUploaded = testSuiteService.uploadTestSuiteAsXML(testSuite);
		if (isTestSuiteUploaded) {
			LOGGER.info("Script names uploaded successfully");
			return ResponseUtils.getSuccessResponse("Test suite uploaded successfully");
		} else {
			LOGGER.error("Error in uploading test suites");
			throw new TDKServiceException("Error in creating Custom Test Suite");
		}
	}

	/**
	 * This method is used to update the test suite by module name
	 *
	 * @param moduleName - the module name
	 * @return ResponseEntity - the response entity
	 */
	@Operation(summary = "Update TestSuite By Module Name", description = "Update TestSuite By Module Name")
	@ApiResponse(responseCode = "200", description = "Scriptgroup updated successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@PutMapping("/updateTestSuite")
	public ResponseEntity<Response> updateTestSuiteByModuleNameAndCategory(@RequestParam String moduleName,
			@RequestParam String category) {
		LOGGER.info("Received request to update test suite for module: {} and category: {}", moduleName, category);
		String message = testSuiteService.updateTestSuiteByModuleNameAndCategory(moduleName, category);
		LOGGER.info("Successfully updated test suite for module: {} and category: {}", moduleName, category);
		return ResponseUtils.getSuccessResponse(message);
	}

	/**
	 * This method is used to download all the test suite as XML
	 * 
	 * @param category - the test suite category
	 * @return ResponseEntity - the response entity
	 * 
	 */
	@Operation(summary = "Download All TestSuite xml as zip by category", description = "Download All TestSuite As xml as zip by category")
	@ApiResponse(responseCode = "200", description = "All TestSuite downloaded successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "500", description = "Issue in downloading All TestSuite")
	@GetMapping("/downloadAllTestSuiteXml")
	public ResponseEntity<?> downloadAllTestSuiteXML(@RequestParam String category) {
		LOGGER.info("Received download all test suite XML request:");
		ByteArrayInputStream inputStream = testSuiteService.downloadAllTestSuiteAsXML(category);
		if (inputStream == null || inputStream.available() == 0) {
			LOGGER.error("Error in downloading all test suite as XML");
			throw new TDKServiceException("Error in creating Custom Test Suite");
		}
		// Prepare response with the XML file
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Disposition", "attachment; filename=Testcases_" + category + Constants.ZIP_EXTENSION);
		LOGGER.info("Downloaded all test suite xml as zip");
		return ResponseEntity.status(HttpStatus.OK).headers(headers).contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(new InputStreamResource(inputStream));
	}

	/**
	 * This method used to get List of Test Suite by Category
	 * 
	 * @param category
	 * @param isThunderEnabled
	 * @return
	 */
	@Operation(summary = "Get List of Test Suite by Category", description = "Get List of Test Suite by Category")
	@ApiResponse(responseCode = "200", description = "List of test suite fetched successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/getListofTestSuiteByCategory")
	public ResponseEntity<DataResponse> getListofTestSuiteByCategory(@RequestParam String category,
			@RequestParam boolean isThunderEnabled) {
		LOGGER.info("Received request to get list of test suites by category: {} and isThunderEnabled: {}", category,
				isThunderEnabled);

		List<TestSuiteDetailsResponse> testSuites = testSuiteService.getListofTestSuiteNamesByCategory(category,
				isThunderEnabled);

		if (testSuites != null) {
			LOGGER.info("Test suites fetched successfully for category: {} and isThunderEnabled: {}", category,
					isThunderEnabled);
			return ResponseUtils.getSuccessDataResponse("Test suites fetched successfully for category - " + category
					+ " and thunderenabled status - " + isThunderEnabled, testSuites);
		} else {
			LOGGER.warn("No test suites found for category: {} and isThunderEnabled: {}", category, isThunderEnabled);
			return ResponseUtils.getSuccessDataResponse(
					"No test suites found for category: " + category + " and isThunderEnabled: " + isThunderEnabled,
					testSuites);
		}
	}

	/**
	 * This method used to get List of all Test Suite
	 * 
	 * @return ResponseEntity<DataResponse>
	 */
	@Operation(summary = "Get List of all Test Suite", description = "Get List of all Test Suite")
	@ApiResponse(responseCode = "200", description = "List of all test suite fetched successfully")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "500", description = "Issue in fetching all test suites")
	@GetMapping("/getTestSuiteList")
	public ResponseEntity<DataResponse> getTestSuiteList() {
		LOGGER.info("Received request to get list of test suites");
		List<TestSuiteDetailsResponse> testSuites = testSuiteService.getTestSuiteList();
		if (testSuites != null) {
			LOGGER.info("Test suites fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Test suites fetched successfully", testSuites);
		} else {
			LOGGER.warn("No test suites found");
			return ResponseUtils.getSuccessDataResponse("No test suites found", testSuites);
		}
	}

	/**
	 * This method is used to download custom/non-module test suites as archive (ZIP
	 * or TAR.GZ) (test suites whose names don't match any module name)
	 * 
	 * @param category - the test suite category
	 * @param format   - the archive format (zip or tar.gz), default is zip
	 * @return ResponseEntity - the response entity with archive file
	 */
	@Operation(summary = "Download custom test suites as archive by category", description = "Download custom (non-module) test suites as xml in an archive file (ZIP or TAR.GZ) by category")
	@ApiResponse(responseCode = "200", description = "Custom test suites downloaded successfully")
	@ApiResponse(responseCode = "400", description = "Bad request - Invalid format or category")
	@ApiResponse(responseCode = "404", description = "No custom test suites found")
	@ApiResponse(responseCode = "500", description = "Issue in downloading custom test suites")
	@GetMapping("/downloadCustomTestSuiteXml")
	public ResponseEntity<?> downloadCustomTestSuiteXML(@RequestParam String category,
			@RequestParam(defaultValue = "zip") String format) {

		LOGGER.info("Received download custom test suite request for category: {} in format: {}", category, format);

		// Validate format parameter
		format = format.toLowerCase().trim();
		if (!format.equals("zip") && !format.equals("tar.gz") && !format.equals("tgz")) {
			LOGGER.error("Invalid format: {}. Supported formats are: zip, tar.gz, tgz", format);
			throw new TDKServiceException("Invalid format. Supported formats are: zip, tar.gz, tgz");
		}

		ByteArrayInputStream inputStream;
		String contentType;
		String fileExtension;

		// Process based on format
		if (format.equals("tar.gz") || format.equals("tgz")) {
			inputStream = testSuiteService.downloadCustomTestSuiteAsTarGz(category);
			contentType = "application/gzip";
			fileExtension = ".tar.gz";
		} else {
			// Default to ZIP
			inputStream = testSuiteService.downloadCustomTestSuiteAsXML(category);
			contentType = "application/zip";
			fileExtension = ".zip";
		}

		if (inputStream == null || inputStream.available() == 0) {
			LOGGER.error("Error in downloading custom test suites as {}", format);
			throw new TDKServiceException("Error in downloading custom test suites");
		}

		// Prepare response with the archive file
		HttpHeaders headers = new HttpHeaders();
		String filename = "Custom_TestSuites_" + category + fileExtension;
		headers.add("Content-Disposition", "attachment; filename=" + filename);

		LOGGER.info("Downloaded custom test suites as {} for category: {}", format, category);

		return ResponseEntity.status(HttpStatus.OK).headers(headers).contentType(MediaType.parseMediaType(contentType))
				.body(new InputStreamResource(inputStream));
	}

	/**
	 * This method is used to upload multiple test suites from an archive file (ZIP
	 * or TAR.GZ)
	 * 
	 * @param archiveFile - the archive file containing multiple test suite XML
	 *                    files
	 * @return ResponseEntity - the response entity
	 */
	@Operation(summary = "Upload multiple test suites from archive", description = "Upload multiple test suites from a ZIP or TAR.GZ archive file containing XML files")
	@ApiResponse(responseCode = "200", description = "Test suites uploaded successfully")
	@ApiResponse(responseCode = "400", description = "Bad request - Invalid file format")
	@ApiResponse(responseCode = "500", description = "Issue in uploading test suites")
	@PostMapping("/uploadAllTestSuitesArchive")
	public ResponseEntity<Response> uploadAllTestSuitesFromArchive(
			@RequestPart("archiveFile") MultipartFile archiveFile) {

		LOGGER.info("Received upload all test suites from archive request: {}",
				archiveFile.getOriginalFilename());

		String result = testSuiteService.uploadAllTestSuitesFromArchive(archiveFile);

		LOGGER.info("Test suites upload completed: {}", result);
		return ResponseUtils.getSuccessResponse(result);
	}

}
