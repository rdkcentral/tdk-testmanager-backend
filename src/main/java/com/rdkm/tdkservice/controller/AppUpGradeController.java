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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.dto.AppUpgradeResponseDTO;
import com.rdkm.tdkservice.dto.DeploymentLogsDTO;
import com.rdkm.tdkservice.dto.WarUploadResponseDTO;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.service.IAppUpgradeService;
import com.rdkm.tdkservice.serviceimpl.AppUpgradeService;
import com.rdkm.tdkservice.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * AppUpgradeController handles requests related to app upgrades
 */
@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/app-upgrade")
public class AppUpGradeController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AppUpGradeController.class);
	/**
	 * AppUpgradeService bean for exporting device type changes to SQL.
	 */
	@Autowired
	private IAppUpgradeService appUpgradeService;

	/**
	 * Exports change SQL based on the provided timestamp. This endpoint triggers
	 * the SQL export process for app upgrades. This change only SQL is used to get
	 * the changes added to the database after a particular time stamp, it takes the
	 * changes of device config and script related tables. Then these SQL can be
	 * either integrated in the liquibase like tools or directly applied to the
	 * existing database
	 *
	 * @param since - The time stamp from which the changes needs to be populated
	 */
	@GetMapping("/exportChangeBasedOnTime")
	public ResponseEntity<byte[]> exportChangeSql(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
		try {
			String filePath = "app_upgrade_changes_" + since.toString().replace(":", "-") + ".sql";
			appUpgradeService.writeAppUpgradeSqlToFile(since, filePath);
			Path path = Paths.get(filePath);
			byte[] fileBytes = Files.readAllBytes(path);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.valueOf("application/sql"));
			headers.setContentDispositionFormData("attachment", filePath);
			return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(("Error exporting SQL: " + e.getMessage()).getBytes());
		}
	}

	/**
	 * Handles the uploading of a WAR file for application upgrade.
	 * 
	 * @param uploadFile the WAR file to be uploaded, provided as a multipart file
	 * @return a {@link ResponseEntity} containing a {@link DataResponse} with the
	 *         upload result
	 * @throws TDKServiceException if there is an error during the file upload
	 *                             process
	 */
	@Operation(summary = "Upload WAR file for application upgrade")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "File Uploaded Successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid File Format or Upload Error"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error") })
	@PostMapping("/uploadWarFile")
	public ResponseEntity<DataResponse> uploadWarFile(@RequestParam MultipartFile uploadFile) {
		LOGGER.info("UploadWarFile method is called");
		WarUploadResponseDTO response = appUpgradeService.uploadWar(uploadFile);
		if (response != null) {
			LOGGER.info("War file uploaded successfully");
			return ResponseUtils.getSuccessDataResponse("File Uploaded Succesfully, Proceed with the App Upgradation",
					response);
		} else {
			LOGGER.error("War file upload failed");
			throw new TDKServiceException("Error while uploading War file");
		}
	}

	/**
	 * API to upgrade the application
	 * 
	 * @param backupLocation Optional backup location
	 * @param uploadLocation Location of the uploaded WAR file
	 * @return Response with status and backup location
	 */
	@Operation(summary = "Upgrade the application with the uploaded WAR file")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Application Upgrade Initiated Successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid Request Parameters"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error") })
	@PostMapping("/upgradeApplication")
	public ResponseEntity<DataResponse> upgradeApplication(@RequestParam(required = false) String backupLocation,
			@RequestParam String uploadLocation) {
		LOGGER.info("UpgradeApplication method is called");
		LOGGER.info("Upload location: {}, Backup location: {}", uploadLocation, backupLocation);
		AppUpgradeResponseDTO response = appUpgradeService.upgradeApplication(uploadLocation, backupLocation);
		if (response != null) {
			LOGGER.info("Application upgrade initiated successfully");
			return ResponseUtils.getSuccessDataResponse("Application upgrade initiated successfully", response);
		} else {
			LOGGER.error("Application upgrade failed");
			throw new TDKServiceException("Error while upgrading application");
		}
	}

	/**
	 * API to fetch the latest deployment logs
	 * 
	 * @param backupLocation Optional backup location, if null default location will
	 *                       be used
	 * @return Response with deployment logs
	 */
	@Operation(summary = "Fetch the latest deployment logs")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Logs Retrieved Successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid Request Parameters"),
			@ApiResponse(responseCode = "404", description = "No Deployment Logs Found"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error") })
	@GetMapping("/deploymentLogs")
	public ResponseEntity<DataResponse> getDeploymentLogs() {
		LOGGER.info("GetDeploymentLogs method is called");
		DeploymentLogsDTO response = appUpgradeService.getLatestDeploymentLogs();
		if (response != null) {
			LOGGER.info("Deployment logs retrieved successfully");
			return ResponseUtils.getSuccessDataResponse("Deployment logs retrieved successfully", response);
		} else {
			LOGGER.error("Failed to retrieve deployment logs");
			throw new TDKServiceException("Error while retrieving deployment logs");
		}
	}
}
