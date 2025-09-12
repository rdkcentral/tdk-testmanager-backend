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
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rdkm.tdkservice.dto.AnalysisIssueTypewiseSummaryDTO;
import com.rdkm.tdkservice.dto.AnalysisResultDTO;
import com.rdkm.tdkservice.dto.JiraDescriptionDTO;
import com.rdkm.tdkservice.dto.TicketCreateDTO;
import com.rdkm.tdkservice.dto.TicketDetailsDTO;
import com.rdkm.tdkservice.dto.TicketUpdateDTO;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.response.Response;
import com.rdkm.tdkservice.service.IExecutionAnalysisService;
import com.rdkm.tdkservice.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * This controller class is used to handle the execution analysis related
 * requests.
 *
 */
@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/analysis")
public class ExecutionAnalysisController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionAnalysisController.class);

	@Autowired
	private IExecutionAnalysisService executionAnalysisService;

	/**
	 * Endpoint to save the analysis result for an execution result.
	 *
	 * @param analysisResultRequest the request body containing analysis result
	 *                              details
	 * @return ResponseEntity<Response> indicating the result of the save operation
	 */
	@Operation(summary = "Save analysis result for an execution result")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Analysis result saved successfully"),
			@ApiResponse(responseCode = "500", description = "Failed to save analysis result"),
			@ApiResponse(responseCode = "400", description = "Bad request, invalid parameters") })
	@PostMapping("/saveAnalysisResult")
	public ResponseEntity<Response> saveAnalysisResult(
			@RequestParam(value = "executionResultID", required = true) UUID executionResultID,
			@RequestBody AnalysisResultDTO analysisResultRequest) {
		LOGGER.info("Going to save analysis result");
		boolean saved = executionAnalysisService.saveAnalysisResult(executionResultID, analysisResultRequest);
		if (saved) {
			return ResponseUtils.getCreatedResponse("Result analysis saved succesfullly");
		} else {
			throw new TDKServiceException("Failed to save analysis result");
		}
	}

	/**
	 * Endpoint to get the analysis result for an execution result.
	 *
	 * @param executionResultID the UUID of the execution result
	 * @return ResponseEntity<DataResponse> containing the analysis result details
	 */
	@Operation(summary = "Get analysis result for an execution result")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Analysis result fetched successfully"),
			@ApiResponse(responseCode = "500", description = "Failed to get analysis result") })
	@GetMapping("/getAnalysisResult")
	public ResponseEntity<DataResponse> getAnalysisResult(@RequestParam UUID executionResultID) {
		LOGGER.info("Going to get analysis result");
		AnalysisResultDTO analysisResult = executionAnalysisService.getAnalysisResult(executionResultID);
		if (analysisResult != null) {
			LOGGER.info("Analysis result fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Analysis result fetched successfully", analysisResult);
		} else {
			return ResponseUtils.getSuccessDataResponse("Analysis result for this execution result not available",
					null);
		}
	}

	/**
	 * Endpoint to get the module-wise analysis summary for an execution .
	 *
	 * @param executionID the UUID of the execution
	 * @return ResponseEntity<DataResponse> containing the module-wise analysis
	 *         summary details
	 */
	@Operation(summary = "Get module-wise analysis summary for an execution")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Module-wise analysis summary fetched successfully"),
			@ApiResponse(responseCode = "500", description = "Failed to get module-wise analysis summary"),
			@ApiResponse(responseCode = "201", description = "Module-wise analysis summary for this execution not available") })
	@GetMapping("/getModulewiseAnalysisSummary")
	public ResponseEntity<DataResponse> getModulewiseAnalysisSummary(@RequestParam UUID executionID) {
		LOGGER.info("Going to get module-wise analysis summary");
		Map<String, AnalysisIssueTypewiseSummaryDTO> analysisSummary = executionAnalysisService
				.getModulewiseAnalysisSummary(executionID);
		LOGGER.info("Module-wise analysis summary fetched succesfully");
		if (analysisSummary != null) {
			return ResponseUtils.getSuccessDataResponse("Module-wise analysis summary fetched succesfully",
					analysisSummary);
		} else {
			return ResponseUtils.getSuccessDataResponse(
					"Module-wise analysis summary is not fetched, as there is no failure case", analysisSummary);
		}
	}

	/**
	 * Retrieves the ticket details from Jira for the specified execution script.
	 *
	 * @param executionResultID the unique identifier of the execution result
	 * @param projectName       the name of the project
	 * @return ResponseEntity<DataResponse> containing the ticket details if
	 *         available, or a message indicating no data is available
	 *
	 */
	@Operation(summary = "Get the ticket details from jira for the particular execution script")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ticket details fetched successfully"),
			@ApiResponse(responseCode = "200", description = "Ticket details not available") })
	@GetMapping("/getTicketDetaisFromJira")
	public ResponseEntity<DataResponse> getTicketDetailsFromJira(@RequestParam UUID executionResultID,
			@RequestParam String projectName) {
		LOGGER.info("Going to get ticket details from Jira");
		List<TicketDetailsDTO> ticketDetails = executionAnalysisService.getTicketDetailsFromJira(executionResultID,
				projectName);
		if (ticketDetails != null && !ticketDetails.isEmpty()) {
			LOGGER.info("Ticket details fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Ticket details fetched successfully", ticketDetails);
		} else {
			LOGGER.error("Ticket details for this issue not available");
			return ResponseUtils.getSuccessDataResponse("Ticket details for this issue not available", null);
		}
	}

	/**
	 * Endpoint to get the ticket details for populating the ticket details.
	 *
	 * @param execResultID the UUID of the execution result
	 * @return ResponseEntity<DataResponse> containing the ticket description if
	 *         available, or a message indicating no data is available
	 * 
	 */
	@Operation(summary = "Get the ticket details for populating the ticket details")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ticket details fetched successfully"),
			@ApiResponse(responseCode = "503", description = "Ticket details not available") })
	@GetMapping("/getDetailsForPopulatingTicketDetails")
	public ResponseEntity<DataResponse> getDetailsForPopulatingTicketDetails(@RequestParam UUID execResultID) {
		LOGGER.info("Going to get ticket details for populating the ticket details");
		JiraDescriptionDTO ticketDescription = executionAnalysisService
				.getDetailsForPopulatingTicketDetails(execResultID);
		if (ticketDescription != null) {
			LOGGER.info("Ticket details fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Ticket description fetched successfully", ticketDescription);
		} else {
			LOGGER.error("Ticket details for this ticket not available");
			throw new TDKServiceException("Error in getting Ticket details for this issue");
		}
	}

	/**
	 * Endpoint to get the list of project IDs.
	 * 
	 * @return ResponseEntity<DataResponse> containing the list of project IDs if
	 *         available, otherwise a message indicating no project IDs are
	 *         available.
	 * 
	 */
	@Operation(summary = "Get the list of project IDs")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Project IDs fetched successfully"),
			@ApiResponse(responseCode = "503", description = "Project IDs not available") })
	@GetMapping("/getListOfProjectIDs")
	public ResponseEntity<DataResponse> getListOfProjectIDs(@RequestParam String category) {
		LOGGER.info("Going to get the list of project IDs");
		List<String> projectIDs = executionAnalysisService.getListOfProjectIDs(category);
		if (projectIDs != null && !projectIDs.isEmpty()) {
			LOGGER.info("Project IDs fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Project IDs fetched successfully", projectIDs);
		} else {
			LOGGER.error("Project IDs not available");
			return ResponseUtils.getNotFoundDataConfigResponse("Project IDs not available", projectIDs);
		}
	}

	/**
	 * Endpoint to check if the project ID is a platform project ID.
	 * 
	 * @param projectID the project ID to be checked
	 * @return ResponseEntity<DataResponse> containing true if the project ID is a
	 *         platform project ID, otherwise false.
	 * 
	 */
	@Operation(summary = "Check if the project ID is a platform project ID")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Project ID is a platform project ID"),
			@ApiResponse(responseCode = "200", description = "Project ID is not a platform project ID") })
	@GetMapping("/isPlatformProjectID")
	public ResponseEntity<DataResponse> isPlatformProjectID(@RequestParam String projectID,@RequestParam String category) {
		LOGGER.info("Going to check if the project ID is a platform project ID");
		String isPlatformProjectID = executionAnalysisService.getProjectCategory(projectID, category);
		if (null != isPlatformProjectID ) {
			LOGGER.info("Project ID is a platform project ID");
			return ResponseUtils.getSuccessDataResponse("Project ID is a platform project ID", isPlatformProjectID);
		} else {
			LOGGER.error("Project ID is not a platform project ID");
			return ResponseUtils.getSuccessDataResponse("Project ID is not a platform project ID", isPlatformProjectID);
		}
	}

	/**
	 * Endpoint to get the list of labels.
	 *
	 * @return ResponseEntity containing the list of labels if available, or a
	 *         message indicating no labels are available.
	 */
	@Operation(summary = "Get the list of labels")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Labels fetched successfully"),
			@ApiResponse(responseCode = "503", description = "Labels not available") })
	@GetMapping("/getListOfLabels")
	public ResponseEntity<DataResponse> getListOfLabels(@RequestParam String category) {
		LOGGER.info("Going to get the list of labels");
		List<String> labels = executionAnalysisService.getListOfLabels(category);
		if (labels != null && !labels.isEmpty()) {
			LOGGER.info("Labels fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Labels fetched successfully", labels);
		} else {
			LOGGER.error("Labels not available");
			return ResponseUtils.getNotFoundDataConfigResponse("Labels not available", labels);
		}
	}

	/**
	 * Endpoint to get the list of release versions.
	 * 
	 * @return ResponseEntity containing the list of release versions if available,
	 *         otherwise a message indicating that no release versions are
	 *         available.
	 */
	@Operation(summary = "Get the list of release versions")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Release versions fetched successfully"),
			@ApiResponse(responseCode = "503", description = "Release versions not available") })
	@GetMapping("/getReleaseVersions")
	public ResponseEntity<DataResponse> getReleaseVersions(@RequestParam String category) {
		LOGGER.info("Going to get the list of release versions");
		List<String> releaseVersions = executionAnalysisService.getReleaseVersions(category);
		if (releaseVersions != null && !releaseVersions.isEmpty()) {
			LOGGER.info("Release versions fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Release versions fetched successfully", releaseVersions);
		} else {
			LOGGER.error("Release versions not available");
			return ResponseUtils.getNotFoundDataConfigResponse("Release versions not available", releaseVersions);
		}
	}

	/**
	 * Endpoint to get the hardware configuration.
	 * 
	 * @return ResponseEntity containing the hardware configuration if available,
	 *         otherwise a message indicating that no hardware configuration is
	 *         available.
	 */
	@Operation(summary = "Get the hardware configuration")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Hardware configuration fetched successfully"),
			@ApiResponse(responseCode = "503", description = "Hardware configuration not available") })
	@GetMapping("/getHardwareConfiguration")
	public ResponseEntity<DataResponse> getHardwareConfiguration(@RequestParam String category) {
		LOGGER.info("Going to get the hardware configuration");
		List<String> hardwareConfiguration = executionAnalysisService.getHardwareConfiguration(category);
		if (hardwareConfiguration != null && !hardwareConfiguration.isEmpty()) {
			LOGGER.info("Hardware configuration fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Hardware configuration fetched successfully",
					hardwareConfiguration);
		} else {
			LOGGER.error("Hardware configuration not available");
			return ResponseUtils.getNotFoundDataConfigResponse("Hardware configuration not available",
					hardwareConfiguration);
		}
	}

	/**
	 * Endpoint to get the list of impacted platforms.
	 * 
	 * @return ResponseEntity containing the list of impacted platforms with HTTP
	 *         status 200 if found, or a message indicating no impacted platforms
	 *         are available with HTTP status 204.
	 *
	 */
	@Operation(summary = "Get the list of impacted platforms")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Impacted platforms fetched successfully"),
			@ApiResponse(responseCode = "503", description = "Impacted platforms not available") })
	@GetMapping("/getImpactedPlatforms")
	public ResponseEntity<DataResponse> getImpactedPlatforms(@RequestParam String category) {
		LOGGER.info("Going to get the list of impacted platforms");
		List<String> impactedPlatforms = executionAnalysisService.getImpactedPlatforms(category);
		if (impactedPlatforms != null && !impactedPlatforms.isEmpty()) {
			LOGGER.info("Impacted platforms fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Impacted platforms fetched successfully", impactedPlatforms);
		} else {
			LOGGER.error("Impacted platforms not available");
			return ResponseUtils.getNotFoundDataConfigResponse("Impacted platforms not available", impactedPlatforms);
		}
	}

	/**
	 * Endpoint to get the list of severities.
	 *
	 * @return ResponseEntity containing the list of severities if available,
	 *         otherwise a message indicating no severities are available.
	 * 
	 */
	@Operation(summary = "Get the list of severities")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Severities fetched successfully"),
			@ApiResponse(responseCode = "503", description = "Severities not available") })
	@GetMapping("/getSeverities")
	public ResponseEntity<DataResponse> getSeverities(@RequestParam String category) {
		LOGGER.info("Going to get the list of severities");
		List<String> severities = executionAnalysisService.getSeverities(category);
		if (severities != null && !severities.isEmpty()) {
			LOGGER.info("Severities fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Severities fetched successfully", severities);
		} else {
			LOGGER.error("Severities not available");
			return ResponseUtils.getNotFoundDataConfigResponse("Severities not available", severities);
		}
	}

	/**
	 * Handles the HTTP GET request to retrieve the list of fixed in versions.
	 * 
	 * @return ResponseEntity containing the list of fixed in versions if available,
	 *         otherwise a response indicating that no fixed in versions are
	 *         available.
	 */
	@Operation(summary = "Get the list of fixed in versions")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Fixed in versions fetched successfully"),
			@ApiResponse(responseCode = "503", description = "Fixed in versions not available") })
	@GetMapping("/getFixedInVersions")
	public ResponseEntity<DataResponse> getFixedInVersions(@RequestParam String category) {
		LOGGER.info("Going to get the list of fixed in versions");
		List<String> fixedInVersions = executionAnalysisService.getFixedInVersions(category);
		if (fixedInVersions != null && !fixedInVersions.isEmpty()) {
			LOGGER.info("Fixed in versions fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Fixed in versions fetched successfully", fixedInVersions);
		} else {
			LOGGER.error("Fixed in versions not available");
			return ResponseUtils.getNotFoundDataConfigResponse("Fixed in versions not available", fixedInVersions);
		}
	}

	/**
	 * Endpoint to get the list of components impacted.
	 *
	 * @return ResponseEntity<DataResponse> containing the list of components
	 *         impacted if available, otherwise a message indicating no components
	 *         impacted are available.
	 */
	@Operation(summary = "Get the list of components impacted")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Components impacted fetched successfully"),
			@ApiResponse(responseCode = "204", description = "Components impacted not available") })
	@GetMapping("/getComponentsImpacted")
	public ResponseEntity<DataResponse> getComponentsImpacted(@RequestParam String category) {
		LOGGER.info("Going to get the list of components impacted");
		List<String> componentsImpacted = executionAnalysisService.getComponentsImpacted(category);
		if (componentsImpacted != null && !componentsImpacted.isEmpty()) {
			LOGGER.info("Components impacted fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Components impacted fetched successfully", componentsImpacted);
		} else {
			LOGGER.error("Components impacted not available");
			return ResponseUtils.getNotFoundDataConfigResponse("Fixed in versions not available", componentsImpacted);
		}
	}

	/**
	 * Endpoint to get the list of defect types.
	 * 
	 * @return ResponseEntity<DataResponse> containing the list of defect types if
	 *         available, otherwise a message indicating that no defect types are
	 *         available.
	 */
	@Operation(summary = "Get the list of defect types")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Defect types fetched successfully"),
			@ApiResponse(responseCode = "503", description = "Defect types not available") })
	@GetMapping("/getDefectTypes")
	public ResponseEntity<DataResponse> getDefectTypes() {
		LOGGER.info("Going to get the list of defect types");
		List<String> defectTypes = executionAnalysisService.getDefectTypes();
		if (defectTypes != null && !defectTypes.isEmpty()) {
			LOGGER.info("Defect types fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Defect types fetched successfully", defectTypes);
		} else {
			LOGGER.error("Defect types not available");
			return ResponseUtils.getNotFoundDataConfigResponse("Defect types not available", defectTypes);
		}

	}

	/**
	 * Endpoint to get the steps to reproduce for a given script.
	 *
	 * @param scriptName the name of the script for which steps to reproduce are to
	 *                   be fetched
	 * @return ResponseEntity containing the steps to reproduce if available,
	 *         otherwise a not found status
	 */
	@Operation(summary = "Get the steps to reproduce")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Steps to reproduce fetched successfully"),
			@ApiResponse(responseCode = "503", description = "Steps to reproduce not available") })
	@GetMapping("/getStepsToReproduce")
	public ResponseEntity<?> getStepsToReproduce(String scriptName) {
		LOGGER.info("Going to get the steps to reproduce");
		String stepsToReproduce = executionAnalysisService.getStepsToReproduce(scriptName);
		if (stepsToReproduce != null && !stepsToReproduce.isEmpty()) {
			LOGGER.info("Steps to reproduce fetched successfully");
			return ResponseEntity.status(HttpStatus.OK).body(stepsToReproduce);
		} else {
			LOGGER.error("Steps to reproduce not available");
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No steps to reproduce available");
		}

	}

	/**
	 * Handles the HTTP GET request to retrieve the list of priorities.
	 * 
	 * @return ResponseEntity<DataResponse> containing the list of priorities if
	 *         available, otherwise a response indicating that no priorities are
	 *         available.
	 * 
	 */
	@Operation(summary = "Get the list of priorities")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Priorities fetched successfully"),
			@ApiResponse(responseCode = "204", description = "Priorities not available") })
	@GetMapping("/getPriorities")
	public ResponseEntity<DataResponse> getPriorities(@RequestParam String category) {
		LOGGER.info("Going to get the list of priorities");
		List<String> priorities = executionAnalysisService.getPriorities(category);
		if (priorities != null && !priorities.isEmpty()) {
			LOGGER.info("Priorities fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Priorities fetched successfully", priorities);
		} else {
			LOGGER.error("Priorities not available");
			return ResponseUtils.getNotFoundDataConfigResponse("Priorities not available", priorities);
		}
	}

	/**
	 * Endpoint to create a Jira ticket.
	 *
	 * @param ticketCreateDTO the data transfer object containing the details
	 *                        required to create a Jira ticket
	 * @return ResponseEntity containing the response message and HTTP status code
	 *
	 */
	@Operation(summary = "Create Jira ticket")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Jira ticket created successfully"),
			@ApiResponse(responseCode = "500", description = "Failed to create Jira ticket") })
	@PostMapping("/createJiraTicket")
	public ResponseEntity<DataResponse> createJiraTicket(@RequestBody TicketCreateDTO ticketCreateDTO) {
		LOGGER.info("Going to create Jira ticket");
		String response = executionAnalysisService.createJiraTicket(ticketCreateDTO);
		if (response != null) {
			LOGGER.info("Jira ticket created successfully");
			return ResponseUtils.getSuccessDataResponse("Jira ticket created successfully", response); 
		} else {
			LOGGER.error("Failed to create Jira ticket");
			throw new TDKServiceException("Failed to create Jira ticket");
		}

	}

	/**
	 * Updates a Jira ticket with the provided details.
	 *
	 * @param ticketUpdateDTO the data transfer object containing the details for
	 *                        updating the Jira ticket
	 * @return a ResponseEntity containing the response message and HTTP status code
	 * 
	 */
	@Operation(summary = "Update Jira ticket")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Jira ticket updated successfully"),
			@ApiResponse(responseCode = "500", description = "Failed to update Jira ticket") })
	@PostMapping("/updateJiraTicket")
	public ResponseEntity<DataResponse> updateJiraTicket(@RequestBody TicketUpdateDTO ticketUpdateDTO) {
		LOGGER.info("Going to update Jira ticket");
		String response = executionAnalysisService.updateJiraTicket(ticketUpdateDTO);
		if (response != null) {
			LOGGER.info("Jira ticket updated successfully");
			return ResponseUtils.getSuccessDataResponse("Jira ticket updated successfully", response); // Return the
																										// response with
																										// the ticket
																										// ID)
		} else {
			LOGGER.error("Failed to update Jira ticket");
			throw new TDKServiceException("Failed to update Jira ticket");
		}
	}

	/**
	 * Endpoint to check if Jira automation is implemented.
	 *
	 * @return ResponseEntity<DataResponse>true if Jira automation is implemented,
	 *         false otherwise.
	 *
	 */
	@Operation(summary = "Check if Jira automation is implemented")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Jira automation is implemented"),
			@ApiResponse(responseCode = "204", description = "Jira automation is not implemented") })
	@GetMapping("/isJiraAutomationImplemented")
	public ResponseEntity<DataResponse> isJiraAutomationImplemented() {
		LOGGER.info("Going to check if Jira automation is implemented");
		boolean isJiraAutomationImplemented = executionAnalysisService.isJiraAutomationImplemented();
		return ResponseUtils.getSuccessDataResponse("Jira automation is implemented", isJiraAutomationImplemented);
	}

	/**
	 * Endpoint to get the analysis defect types.
	 *
	 * @return ResponseEntity<?> containing the analysis defect types
	 */
	@Operation(summary = "Get analysis defect types")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Analysis defect types fetched successfully"),
			@ApiResponse(responseCode = "500", description = "Failed to get analysis defect types"),
			@ApiResponse(responseCode = "204", description = "Analysis defect types not available") })
	@GetMapping("/getAnalysisDefectTypes")
	public ResponseEntity<DataResponse> getAnalysisDefectType() {
		LOGGER.info("Going to get AnalysisDefectType");
		List<String> analysisDefectType = executionAnalysisService.getAnalysisDefectTypes();
		if (analysisDefectType != null) {
			return ResponseUtils.getSuccessDataResponse("AnalysisDefectType fetched successfully", analysisDefectType);
		} else {
			return ResponseUtils.getNotFoundDataConfigResponse("AnalysisDefectType not available", analysisDefectType);
		}
	}
	
	/**
	 * Endpoint to get the RDK version.
	 *
	 * @return ResponseEntity containing the RDK version if available, otherwise a
	 *         message indicating that no RDK version is available.
	 */
	@Operation(summary = "Get the RDK version")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "RDK version fetched successfully"),
			@ApiResponse(responseCode = "503", description = "RDK version not available") })
	@GetMapping("/getRDKVersions")
	public ResponseEntity<DataResponse> getRDKVersion(@RequestParam String category) {
		LOGGER.info("Going to get the RDK version");
		List<String> rdkVersion = executionAnalysisService.getRDKVersion(category);
		if (rdkVersion != null && !rdkVersion.isEmpty()) {
			LOGGER.info("RDK version fetched successfully");
			return ResponseUtils.getSuccessDataResponse("RDK version fetched successfully", rdkVersion);
		} else {
			LOGGER.error("RDK version not available");
			return ResponseUtils.getNotFoundDataConfigResponse("RDK version not available", rdkVersion);
		}
	}
	

}
