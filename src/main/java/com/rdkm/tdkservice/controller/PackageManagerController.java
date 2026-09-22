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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.response.InstallJobStatusResponse;
import com.rdkm.tdkservice.response.PackageResponse;
import com.rdkm.tdkservice.response.Response;
import com.rdkm.tdkservice.service.IPackageManagerService;
import com.rdkm.tdkservice.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/*
 * The controller class that handles the API endpoints related to TDK package installation.
 */

@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/packagemanager")
public class PackageManagerController {

	private static final Logger LOGGER = LoggerFactory.getLogger(PackageManagerController.class);

	@Autowired
	private IPackageManagerService packageManagerService;

	/**
	 * This method is used to create the package.
	 * 
	 * @param type   -TDK,VTS
	 * @param device -Device name
	 * @return ResponseEntity<String>
	 */
	@Operation(summary = "Create Package API")
	@ApiResponse(responseCode = "200", description = "Created Package Successfully")
	@ApiResponse(responseCode = "400", description = "Bad Request")
	@ApiResponse(responseCode = "500", description = "Internal Server Error")
	@PostMapping("/createPackageAPI")
	public ResponseEntity<DataResponse> createPackageAPI(@RequestParam String type, @RequestParam String device) {
		LOGGER.info("createPackageAPI method is called");
		PackageResponse response = packageManagerService.createPackage(type, device);
		if (response != null) {
			if (response.getStatusCode() == 200) {
				LOGGER.info("Package created successfully");
				return ResponseUtils.getSuccessDataResponse("Package created successfully", response);
			} else {
				LOGGER.error("Package creation failed: {}", response.getLogs());
				// Return 503 Service Unavailable with the error message from response
				return ResponseUtils.getNotFoundDataConfigResponse("Package creation failed", response);
			}
		} else {
			LOGGER.error("Package creation failed");
			throw new TDKServiceException("Error while package creation");

		}

	}

	/**
	 * This method is used to get the available packages.
	 * 
	 * @param device name
	 * @return ResponseEntity<?> - List of available packages
	 */
	@Operation(summary = "Get Available Packages API")
	@ApiResponse(responseCode = "200", description = "Get Available Packages Successfully")
	@ApiResponse(responseCode = "400", description = "Bad Request")
	@ApiResponse(responseCode = "500", description = "Internal Server Error")
	@GetMapping("/getAvailablePackages")
	public ResponseEntity<DataResponse> getAvailablePackages(@RequestParam String type, @RequestParam String device) {
		LOGGER.info("getAvailablePackages method is called");
		List<String> availablePackages = packageManagerService.getAvailablePackages(type, device);
		if (availablePackages != null) {
			LOGGER.info("Available packages are: {}", availablePackages);
			return ResponseUtils.getSuccessDataResponse(availablePackages);
		} else {
			LOGGER.error("No available packages");
			return ResponseUtils.getSuccessDataResponse("No Packages available", availablePackages);
		}

	}

	/**
	 * This method is used to upload the package.
	 * 
	 * @param uploadFile -file to upload
	 * @return ResponseEntity<String> -success or failure message
	 */
	@Operation(summary = "Upload Package API")
	@ApiResponse(responseCode = "201", description = "Package Uploaded Successfully")
	@ApiResponse(responseCode = "400", description = "Bad Request")
	@ApiResponse(responseCode = "500", description = "Internal Server Error")
	@PostMapping("/uploadPackage")
	public ResponseEntity<Response> uploadPackage(@RequestParam String type, @RequestParam MultipartFile uploadFile,
			@RequestParam String device) {
		LOGGER.info("uploadPackage method is called");
		boolean status = packageManagerService.uploadPackage(type, uploadFile, device);
		if (status) {
			LOGGER.info("Package uploaded successfully");
			return ResponseUtils.getSuccessResponse("Package uploaded successfully");
		} else {
			LOGGER.error("Package upload failed");
			throw new TDKServiceException("Error while uploading package");
		}
	}

	/**
	 * Starts package installation asynchronously and returns the initial job
	 * status. Poll GET /installPackage/status with the returned jobId for
	 * progress (COPYING_PACKAGE, INSTALLING, ...) and the final result.
	 * 
	 * @param type        the type of the package to install
	 * @param device      the device on which to install the package
	 * @param packageName the name of the package to install
	 * @return the initial job status containing the jobId to poll for progress
	 */
	@Operation(summary = "Install Package API (async)")
	@ApiResponse(responseCode = "200", description = "Installation started")
	@ApiResponse(responseCode = "400", description = "Bad Request")
	@ApiResponse(responseCode = "500", description = "Internal Server Error")
	@PostMapping("/installPackage")
	public ResponseEntity<DataResponse> installPackage(@RequestParam String type,
			@RequestParam String device, @RequestParam String packageName) {
		LOGGER.info("installPackage method is called");
		String jobId = packageManagerService.startInstallPackageJob(type, device, packageName);
		InstallJobStatusResponse status = packageManagerService.getInstallPackageJobStatus(jobId);
		return ResponseUtils.getSuccessDataResponse("Installation started", status);
	}

	/**
	 * Returns the current phase and (once finished) result for a job started via
	 * /installPackage. Intended to be polled every 1-2 seconds while status is
	 * RUNNING.
	 * 
	 * @param jobId the id of the installation job to query
	 * @return the current status of the installation job
	 */
	@Operation(summary = "Get Package Installation Status")
	@ApiResponse(responseCode = "200", description = "Status fetched successfully")
	@ApiResponse(responseCode = "404", description = "Job not found")
	@GetMapping("/installPackage/status")
	public ResponseEntity<DataResponse> getPackageInstallationStatus(@RequestParam String jobId) {
		LOGGER.info("getPackageInstallationStatus method is called with jobId: {}", jobId);
		InstallJobStatusResponse status = packageManagerService.getInstallPackageJobStatus(jobId);
		if (status == null) {
			// jobId unknown/expired - handled by GlobalExceptionHandler like other 404s
			throw new ResourceNotFoundException("Install job", jobId);
		}
		return ResponseUtils.getSuccessDataResponse(status);
	}

	/**
	 * This method is used to upload the generic package.
	 * 
	 * @param type       -TDK,VTS
	 * @param uploadFile -file to upload
	 * @return ResponseEntity<String> -success or failure message
	 */
	@Operation(summary = "Upload Generic Package API")
	@ApiResponse(responseCode = "201", description = "Generic Package Uploaded Successfully")
	@ApiResponse(responseCode = "400", description = "Bad Request")
	@ApiResponse(responseCode = "500", description = "Internal Server Error")
	@PostMapping("/uploadGenericPackage")
	public ResponseEntity<Response> uploadGenericPackage(@RequestParam String type,
			@RequestParam MultipartFile uploadFile, @RequestParam String device) {
		LOGGER.info("uploadGenericPackage method is called");
		boolean status = packageManagerService.uploadGenericPackage(type, uploadFile, device);
		if (status) {
			LOGGER.info("Generic Package uploaded successfully");
			return ResponseUtils.getSuccessResponse("Generic Package uploaded successfully");
		} else {
			LOGGER.error("Generic Package upload failed");
			throw new TDKServiceException("Error while uploading Generic package");
		}
	}

	/**
	 * This method is used to check whether a generic package is already present.
	 *
	 * @param type   -TDK,VTS
	 * @param device -Device name
	 * @return ResponseEntity<DataResponse> - true if a generic package is present
	 */
	@Operation(summary = "Check Generic Package Present API")
	@ApiResponse(responseCode = "200", description = "Fetched Generic Package presence Successfully")
	@ApiResponse(responseCode = "400", description = "Bad Request")
	@ApiResponse(responseCode = "500", description = "Internal Server Error")
	@GetMapping("/isGenericPackagePresent")
	public ResponseEntity<DataResponse> isGenericPackagePresent(@RequestParam String type,
			@RequestParam String device) {
		LOGGER.info("isGenericPackagePresent method is called");
		boolean isPresent = packageManagerService.isGenericPackagePresent(type, device);
		return ResponseUtils.getSuccessDataResponse(isPresent);
	}
}
