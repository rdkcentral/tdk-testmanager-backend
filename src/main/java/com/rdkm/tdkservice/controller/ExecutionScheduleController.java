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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rdkm.tdkservice.dto.ExecutionScheduleDTO;
import com.rdkm.tdkservice.dto.ExecutionSchedulesResponseDTO;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.response.Response;
import com.rdkm.tdkservice.serviceimpl.ExecutionScheduleService;
import com.rdkm.tdkservice.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * The ExecutionScheduleController class is used to handle the execution
 * schedule related operations
 * 
 */
@RestController
@CrossOrigin
@RequestMapping("/api/v1/executionScheduler")
public class ExecutionScheduleController {

	@Autowired
	ExecutionScheduleService executionScheduleService;

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionScheduleController.class);

	/**
	 * This method is used to create the execution schedule
	 * 
	 * @param executionScheduleDTO
	 * @return ResponseEntity<Response>
	 */
	@Operation(summary = "Create the execution schedule")
	@ApiResponse(responseCode = "201", description = "Execution Schedule created successfully")
	@ApiResponse(responseCode = "500", description = "Failed to schedule execution")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@PostMapping("/create")
	public ResponseEntity<Response> createExecutionSchedule(@RequestBody ExecutionScheduleDTO executionScheduleDTO) {
		LOGGER.info("Creating the execution schedule");
		boolean executionSchedule = executionScheduleService.saveScheduleExecution(executionScheduleDTO);
		if (executionSchedule) {
			LOGGER.info("Execution schedule created successfully");
			return ResponseUtils.getCreatedResponse("Execution schedule created successfully");
		} else {
			throw new TDKServiceException("Failed to schedule execution");
		}
	}

	/**
	 * This method is used to cancel the execution schedule
	 * 
	 * @param executionScheduleDTO
	 * @return ResponseEntity<?>
	 */
	@Operation(summary = "Cancel the execution schedule")
	@ApiResponse(responseCode = "200", description = "Execution Schedule cancelled successfully")
	@ApiResponse(responseCode = "500", description = "Error in cancelling the execution schedule")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/cancel")
	public ResponseEntity<Response> cancelExecutionSchedule(@RequestParam UUID executionID) {
		LOGGER.info("Cancelling the execution schedule");
		boolean executionSchedule = executionScheduleService.cancelScheduleExecution(executionID);
		if (executionSchedule) {
			LOGGER.info("Execution schedule cancelled successfully");
			return ResponseUtils.getSuccessResponse("Execution schedule cancelled successfully");
		} else {
			throw new TDKServiceException("Failed to cancel execution");
		}
	}

	/**
	 * This method is used to revert the cancel of the execution schedule
	 * 
	 * @param executionID Id of the execution schedule
	 * @return Response for the request
	 */
	@Operation(summary = "Revert the cancel of the execution schedule")
	@ApiResponse(responseCode = "200", description = "Execution Schedule reverted successfully")
	@ApiResponse(responseCode = "500", description = "Error in reverting the cancel of the execution schedule")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/scheduleAgain")
	public ResponseEntity<Response> revertCancelTask(@RequestParam UUID executionID) {
		LOGGER.info("Reverting the cancel of the execution schedule");
		boolean executionSchedule = executionScheduleService.revertCancelTask(executionID);
		if (executionSchedule) {
			LOGGER.info("Execution schedule reverted successfully");
			return ResponseUtils.getSuccessResponse("Execution schedule reverted successfully");
		} else {
			throw new TDKServiceException("Failed to revert cancel execution");
		}

	}

	/**
	 * This method is used to delete the execution schedule
	 * 
	 * @param executionScueduleID Id of the execution schedule
	 * @return Response for the request
	 */
	@Operation(summary = "Delete the execution schedule")
	@ApiResponse(responseCode = "200", description = "Execution Schedule deleted successfully")
	@ApiResponse(responseCode = "500", description = "Error in deleting the execution schedule")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/delete")
	public ResponseEntity<Response> deleteExecutionSchedule(@RequestParam UUID executionScueduleID) {
		LOGGER.info("Deleting the execution schedule");
		boolean executionSchedule = executionScheduleService.deleteScheduleExecution(executionScueduleID);
		if (executionSchedule) {
			LOGGER.info("Execution schedule deleted successfully");
			return ResponseUtils.getSuccessResponse("Execution schedule deleted successfully");
		} else {
			LOGGER.error("Failed to delete execution schedule");
			throw new TDKServiceException("Failed to delete execution schedule");
		}
	}

	/**
	 * This method is used to get all the execution schedules
	 * 
	 * @return Response for the request
	 */
	@Operation(summary = "Get all execution schedules")
	@ApiResponse(responseCode = "200", description = "Execution Schedules fetched successfully")
	@ApiResponse(responseCode = "500", description = "Error in fetching the execution schedules")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@GetMapping("/getAll")
	public ResponseEntity<DataResponse> getAllExecutionSchedules(@RequestParam String category) {
		LOGGER.info("Fetching all the execution schedules");
		List<ExecutionSchedulesResponseDTO> executionSchedules = executionScheduleService
				.getAllExecutionSchedulesByCategory(category);
		if (executionSchedules != null) {
			LOGGER.info("Execution schedules fetched successfully");
			return ResponseUtils.getSuccessDataResponse("Execution schedules fetched successfully", executionSchedules);
		} else {
			LOGGER.error("Failed to fetch execution schedules");
			return ResponseUtils.getSuccessDataResponse("No execution schedules available", null);
		}
	}

}
