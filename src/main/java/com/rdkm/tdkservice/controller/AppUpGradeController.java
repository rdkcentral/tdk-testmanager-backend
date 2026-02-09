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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.rdkm.tdkservice.dto.AppUpgradeResponseDTO;
import com.rdkm.tdkservice.dto.DeploymentLogsDTO;
import com.rdkm.tdkservice.dto.EntityListResponseDTO;
import com.rdkm.tdkservice.dto.WarUploadResponseDTO;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.service.IAppUpgradeService;

import com.rdkm.tdkservice.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.rdkm.tdkservice.response.Response;
import com.rdkm.tdkservice.service.IDataRecoveryService;
import com.rdkm.tdkservice.service.ILiquibaseService;

/**
 * AppUpgradeController handles requests related to app upgrades
 */
@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/app-upgrade")
public class AppUpGradeController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AppUpGradeController.class);

	private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();
	/**
	 * AppUpgradeService bean for exporting device type changes to SQL.
	 */
	@Autowired
	private IAppUpgradeService appUpgradeService;

	/**
	 * LiquibaseService bean for manual database migrations.
	 */
	@Autowired
	private ILiquibaseService liquibaseService;

	/**
	 * DataRecoveryService bean for executing data recovery.
	 */
	@Autowired
	private IDataRecoveryService dataRecoveryService;

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
	@Operation(summary = "Export change SQL based on timestamp")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Export Successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid Time format"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error") })
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
	 * Exports entity list in JSON format based on the provided timestamp. This
	 * endpoint
	 * generates a JSON file containing all entities (DeviceType, OEM, SOC, Module,
	 * Function,
	 * Parameter, PrimitiveTest, Script, TestSuite) that were created after the
	 * given timestamp,
	 * organized by entity type and category. This is useful for tracking new
	 * entities
	 * created for distribution across multiple instances via Liquibase.
	 *
	 * @param since - The timestamp from which the entities need to be populated
	 */
	@GetMapping("/exportEntityListJson")
	@Operation(summary = "Export entity list JSON based on timestamp")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Export Successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid Time format"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error") })
	public ResponseEntity<byte[]> exportEntityListJson(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
		try {
			String filePath = "entity_list_" + since.toString().replace(":", "-") + ".json";
			appUpgradeService.writeEntityListJsonToFile(since, filePath);
			Path path = Paths.get(filePath);
			byte[] fileBytes = Files.readAllBytes(path);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setContentDispositionFormData("attachment", filePath);
			return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(("Error exporting entity list JSON: " + e.getMessage()).getBytes());
		}
	}

	/**
	 * Returns entity list in JSON format based on the provided timestamp. This
	 * endpoint
	 * generates a JSON response containing all entities (DeviceType, OEM, SOC,
	 * Module, Function,
	 * Parameter, PrimitiveTest, Script, TestSuite) that were created after the
	 * given timestamp,
	 * organized by entity type and category.
	 *
	 * @param since - The timestamp from which the entities need to be populated
	 */
	@GetMapping("/getListOfAllChangesSince")
	@Operation(summary = "Get entity list JSON based on timestamp")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Retrieved Successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid Time format"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error") })
	public ResponseEntity<DataResponse> getEntityListJson(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
		try {
			EntityListResponseDTO result = appUpgradeService.generateEntityListJsonByCreatedDate(since);
			return ResponseUtils.getSuccessDataResponse("Successfully retrieved the new changes", result);
		} catch (Exception e) {
			// Create an error response DTO
			throw new TDKServiceException("Error fetching data list changes since the UTC time: " + since.toString());
			// Return appropriate error response
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

	/**
	 * API to execute Liquibase migrations manually
	 * This endpoint allows you to run database migrations after deployment
	 * without requiring the application to restart.
	 * 
	 * @return Response with migration status
	 */
	@Operation(summary = "Execute Liquibase database migrations")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Migration Executed Successfully"),
			@ApiResponse(responseCode = "500", description = "Migration Failed") })
	@GetMapping("/runLiquibase")
	public ResponseEntity<Response> runLiquibase() {
		LOGGER.info("Manual Liquibase migration requested");
		try {
			String result = liquibaseService.executeMigrations();
			LOGGER.info("Liquibase migration completed successfully");
			return ResponseUtils.getSuccessResponse(result);
		} catch (Exception e) {
			LOGGER.error("Error during Liquibase migration: {}", e.getMessage(), e);
			/**
			 * The Liquibase migration can be failed due to various reasons like conflicts
			 * in change sets, database connectivity issues, etc.
			 * In such cases, it's crucial to inform the admin user to analyze the logs and
			 * take necessary actions.
			 * If it is due to conflicts in change sets, it is recommended to take a backup
			 * of the current database state before attempting any recovery actions.
			 * The admin user should also be made aware of the potential impact on the
			 * application and data integrity. For more details, please refer to the release
			 * migration
			 * documentation
			 * 
			 */
			throw new TDKServiceException(
					"Migratuion failed. Admin user, Please analyse logs, take your data backup and use data recovery feature");
		}
	}

	/**
	 * API to execute data recovery
	 * This endpoint allows you to recover data from a backup after a failed
	 * migration.
	 */
	@GetMapping("/data-recovery/execute")
	public ResponseEntity<?> executeDataRecovery() {
		LOGGER.info("Data recovery execution requested");
		try {
			dataRecoveryService.executeDataRecovery();
			return ResponseUtils.getSuccessResponse("Data recovery executed successfully");
		} catch (Exception e) {
			LOGGER.error("Error during data recovery: {}", e.getMessage(), e);
			throw new TDKServiceException("Data recovery failed, Please check the logs");
		}
	}

	/**
	 * Executes the WAR generation process with the specified release tag.
	 * 
	 * This endpoint initiates the WAR generation script execution asynchronously
	 * and returns
	 * an execution ID that can be used to track the progress of the generation
	 * process.
	 * 
	 * @param releaseTag the release tag to be used for WAR generation (required)
	 * @return ResponseEntity containing DataResponse with execution details
	 *         including:
	 *         - executionId: unique identifier for tracking the generation process
	 *         - message: confirmation message about script execution
	 *         - status: current status of the execution (initially "RUNNING")
	 */
	@PostMapping("/war-generation")
	@Operation(summary = "Execute WAR generation script with release tag")
	@ApiResponse(responseCode = "200", description = "WAR generation started successfully")
	@ApiResponse(responseCode = "400", description = "Invalid input")
	@ApiResponse(responseCode = "500", description = "Internal Server Error")
	public ResponseEntity<DataResponse> executeWarGeneration(@RequestParam(required = true) String releaseTag) {
		LOGGER.info("Starting WAR generation with release tag: {}", releaseTag);
		String executionId = appUpgradeService.executeWarGeneration(releaseTag);
		Map<String, String> response = Map.of(
				"executionId", executionId,
				"message", "WAR generation script execution started",
				"status", "RUNNING");
		return ResponseUtils.getSuccessDataResponse("WAR generation started successfully", response);

	}

	/**
	 * Streams WAR generation logs in real-time using Server-Sent Events (SSE).
	 * 
	 * This endpoint establishes a persistent connection to stream log messages
	 * for a specific WAR generation execution. The connection automatically
	 * handles cleanup on completion, timeout, or error scenarios.
	 * 
	 * @param executionId The unique identifier for the WAR generation execution
	 * @return SseEmitter configured for streaming log messages with a 5-minute
	 *         timeout
	 */
	@GetMapping(value = "/war-generation/logs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(summary = "Stream WAR generation logs")
	@ApiResponse(responseCode = "200", description = "Log streaming started")
	@ApiResponse(responseCode = "404", description = "Execution not found")
	public SseEmitter streamWarGenerationLogs(@RequestParam String executionId) {
		LOGGER.info("Starting log stream for WAR generation execution ID: {}", executionId);
		SseEmitter emitter = new SseEmitter(300000L);
		activeEmitters.put(executionId, emitter);
		emitter.onCompletion(() -> {
			LOGGER.info("SSE connection completed for execution: {}", executionId);
			activeEmitters.remove(executionId);
		});
		emitter.onTimeout(() -> {
			LOGGER.warn("SSE connection timeout for execution: {}", executionId);
			activeEmitters.remove(executionId);
			emitter.complete();
		});
		emitter.onError((ex) -> {
			LOGGER.error("SSE connection error for execution: {}", executionId, ex);
			activeEmitters.remove(executionId);
		});
		appUpgradeService.streamWarGenerationLogs(executionId, emitter);
		return emitter;
	}
}
