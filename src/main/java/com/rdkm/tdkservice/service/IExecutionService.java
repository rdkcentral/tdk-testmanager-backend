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
package com.rdkm.tdkservice.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;
import org.springframework.core.io.Resource;

import com.rdkm.tdkservice.dto.ResultDTO;
import com.rdkm.tdkservice.dto.ExecutionByDateDTO;
import com.rdkm.tdkservice.dto.ExecutionDetailsForHtmlReportDTO;
import com.rdkm.tdkservice.dto.ExecutionDetailsResponseDTO;
import com.rdkm.tdkservice.dto.ExecutionListDTO;
import com.rdkm.tdkservice.dto.ExecutionListResponseDTO;
import com.rdkm.tdkservice.dto.ExecutionNameRequestDTO;
import com.rdkm.tdkservice.dto.ExecutionResponseDTO;
import com.rdkm.tdkservice.dto.ExecutionResultResponseDTO;
import com.rdkm.tdkservice.dto.ExecutionSearchFilterDTO;
import com.rdkm.tdkservice.dto.ExecutionSummaryResponseDTO;
import com.rdkm.tdkservice.dto.ExecutionTriggerDTO;

public interface IExecutionService {

	/**
	 * Triggers the execution of a test case on a device.
	 *
	 * @param executionTriggerDTO the DTO containing the details of the execution to
	 *                            be triggered
	 * @return an ExecutionResponseDTO containing the details of the triggered
	 *         execution
	 */
	ExecutionResponseDTO triggerExecution(ExecutionTriggerDTO executionTriggerDTO);

	/**
	 * Saves the result of an execution.
	 *
	 * @param execId         the unique identifier of the execution
	 * @param resultData     the result data of the execution
	 * @param execResult     the result of the execution
	 * @param expectedResult the expected result of the execution
	 * @param resultStatus   the status of the execution
	 * @param testCaseName   the name of the test case
	 * @param execDevice     the device on which the execution is performed
	 * @return true if the result is saved successfully, false otherwise
	 */
	boolean saveExecutionResult(String execId, String resultData, String execResult, String expectedResult,
			String resultStatus, String testCaseName, String execDevice);

	/**
	 * Saves the status of a load module execution.
	 *
	 * @param execId     the unique identifier of the execution
	 * @param statusData the status data of the load module
	 * @param execDevice the device on which the execution is performed
	 * @param execResult the result of the execution
	 * @return true if the status is saved successfully, false otherwise
	 */
	boolean saveLoadModuleStatus(String execId, String statusData, String execDevice, String execResult);

	/**
	 * Retrieves the client port information for a given device IP and port.
	 *
	 * @param deviceIP the IP address of the device
	 * @param port     the port number
	 * @return a JSONObject containing the client port information
	 */
	JSONObject getClientPort(String deviceIP, String port);

	/**
	 * Get executions by category with the pagination applied
	 * 
	 * @param categoryName
	 * @param page
	 * @param size
	 * @param sortBy
	 * @param sortDir
	 * @return List of executions in required data format in DTO
	 */
	ExecutionListResponseDTO getExecutionsByCategory(String categoryName, int page, int size, String sortBy,
			String sortDir);

	/**
	 * This method is used to get the execution logs
	 * 
	 * @param executionResultID
	 * @return returns the execution logs
	 */
	String getExecutionLogs(String executionResultID);

	/**
	 * This method is used to get the execution name
	 * 
	 * @param devices - the list
	 * @return the execution name
	 */
	String getExecutionName(ExecutionNameRequestDTO executionNameRequestDTO);

	/**
	 * Retrieves the execution details for the given execution ID.
	 *
	 * @param id the unique identifier of the execution
	 * @return an ExecutionDetailsResponseDTO containing the details of the
	 *         execution
	 */
	ExecutionDetailsResponseDTO getExecutionDetails(UUID id ,String execName);

	/**
	 * Retrieves the execution result for the given execution result ID.
	 *
	 * @param execResultId the unique identifier of the execution result
	 * @return an ExecutionResultResponseDTO containing the details of the execution
	 *         result
	 */
	ExecutionResultResponseDTO getExecutionResult(UUID execResultId);

	/**
	 * Retrieves the trend analysis for the given execution result ID.
	 *
	 * @param execResultId the unique identifier of the execution result
	 * @return a list of trend analysis data
	 */
	List<String> getTrendAnalysis(UUID execResultId);

	/**
	 * Aborts the execution identified by the given execution ID.
	 *
	 * @param execId the unique identifier of the execution to be aborted
	 * @return true if the execution was successfully aborted, false otherwise
	 */
	boolean abortExecution(UUID execId ,String execName);

	/**
	 * Determines whether the execution with the specified ID should be repeated.
	 *
	 * @param execId the unique identifier of the execution
	 * @return true if the execution should be repeated, false otherwise
	 */
	boolean repeatExecution(UUID execId, String user);

	/**
	 * Re-runs the failed script associated with the given execution ID.
	 *
	 * @param execId the unique identifier of the execution whose failed script
	 *               needs to be re-run
	 * @return true if the script was successfully re-run, false otherwise
	 */
	boolean reRunFailedScript(UUID execId, String user);

	/**
	 * Deletes an execution identified by the given UUID.
	 *
	 * @param id the UUID of the execution to be deleted
	 * @return true if the execution was successfully deleted, false otherwise
	 */
	boolean deleteExecution(UUID id);

	/**
	 * Deletes the executions with the specified IDs.
	 *
	 * @param ids the list of UUIDs representing the executions to be deleted
	 * @return true if the executions were successfully deleted, false otherwise
	 */
	boolean deleteExecutions(List<UUID> ids);

	/**
	 * Retrieves a paginated list of executions filtered by device name and category
	 * name.
	 *
	 * @param deviceName   the name of the device to filter executions by
	 * @param categoryName the name of the category to filter executions by
	 * @param page         the page number to retrieve
	 * @param size         the number of executions per page
	 * @param sortBy       the field to sort the executions by
	 * @param sortDir      the direction to sort the executions (e.g., "asc" for
	 *                     ascending, "desc" for descending)
	 * @return a response containing the list of executions matching the specified
	 *         filters
	 */
	ExecutionListResponseDTO getExecutionsByDeviceName(String deviceName, String categoryName, int page, int size,
			String sortBy, String sortDir);

	/**
	 * This method is used to get the executions by device name with pagination
	 * 
	 * @param deviceName   - the device name
	 * @param categoryName - RDKV, RDKB, RDKC
	 * @param page         - the page number
	 * @param size         - size in page
	 * @param sortBy       - by default it is date
	 * @param sortDir      - by default it is desc
	 * @return response DTO
	 */
	ExecutionListResponseDTO getExecutionsByScriptTestsuite(String testSuiteName, String categoryName, int page,
			int size, String sortBy, String sortDir);

	/**
	 * This method is used to get the executions by executionName with pagination
	 * 
	 * @param executionName - executionName
	 * @param categoryName  - RDKV, RDKB, RDKC
	 * @param page          - the page number
	 * @param size          - size in page
	 * @param sortBy        - by default it is date
	 * @param sortDir       - by default it is desc
	 * @return response DTO
	 */
	ExecutionListResponseDTO getExecutionsByExecutionName(String executionName, String categoryName, int page, int size,
			String sortBy, String sortDir);

	/**
	 * This method is used to get the executions by user with pagination
	 * 
	 * @param username     - the username
	 * @param categoryName - RDKV, RDKB, RDKC
	 * @param page         - the page number
	 * @param size         - size in page
	 * @param sortBy       - by default it is date
	 * @param sortDir      - by default it is desc
	 * @return response DTO
	 */
	ExecutionListResponseDTO getExecutionsByUser(String username, String category, int page, int size, String sortBy,
			String sortDir);

	/**
	 * This method is used to get the unique users.
	 * 
	 * @return List of String - the list of unique users
	 */
	List<String> getUniqueUsers();

	/**
	 * 
	 * This method is to get the module wise summary
	 * 
	 * @param executionId - the execution id
	 * @return the module wise summary
	 */
	Map<String, ExecutionSummaryResponseDTO> getModulewiseExecutionSummary(UUID executionId ,String execName);

	/**
	 * Deletes executions within the specified date range.
	 *
	 * @param fromDate the start date of the range (inclusive)
	 * @param toDate   the end date of the range (inclusive)
	 * @return the number of executions deleted
	 */
	int deleteExecutionsByDateRange(Instant fromDate, Instant toDate);

	/**
	 * Downloads the script associated with the given execution resource ID.
	 *
	 * @param executionResId the unique identifier of the execution resource
	 * @return the script as a Resource object
	 */
	Resource downloadScript(UUID executionResId);

	/**
	 * Gets the list of the executions based on the filter criteria in the DTO
	 * 
	 * @param filterRequest - the filter DTO with multple filter criteria like start
	 *                      date, end date, execution type, script test suite,
	 *                      device type etc.
	 * @return the list of executions based on the filter criteria
	 */
	List<ExecutionListDTO> getExecutionDetailsByFilter(ExecutionSearchFilterDTO filterRequest);

	/**
	 * This method is used to return true if any execution result is failed
	 * 
	 * @param executionId - the execution ID
	 * @return true if the execution result is failed
	 */
	boolean isExecutionResultFailed(UUID executionId);

	/**
	 * This method is used to get the device status
	 * 
	 * @param deviceName - the device name
	 * @param deviceType - the device type
	 * @return the JSON object of the device status
	 */
	JSONObject getDeviceStatus(String deviceName, String deviceType);

	/**
	 * This method is used to get the execution details for the HTML report
	 * 
	 * @param executionId - the execution ID
	 * @return the list of execution details for the HTML report
	 */
	List<ExecutionDetailsForHtmlReportDTO> getExecutionDetailsForHtmlReport(UUID executionId);

	/**
	 * Creates a file and writes the provided test data into it. This is
	 * predominaltly used for Media validation scripts
	 * 
	 * @param execId    Execution ID
	 * @param execDevId Execution Device ID
	 * @param resultId  Result ID
	 * @param test      Test data to write
	 * @return JSONObject with status and remarks
	 */
	JSONObject createFileAndWrite(String execId, String execDevId, String resultId, String test);

	/**
	 * Retrieves the execution ID for a specific execution name.
	 *
	 * @param executionName the name of the execution for which to retrieve the ID
	 * @return the execution ID as UUID, or null if the execution is not found
	 */
	UUID getExecutionId(String executionName);

	/**
	 * Retrieves the execution result in JSON format for a specific execution name.
	 *
	 * @param executionName the name of the execution for which to retrieve the
	 *                      result
	 * @return a CIRequestDTO containing the execution result in JSON format
	 */
	ResultDTO getExecutionResultInJson(String executionName);

	/**
	 * Retrieves the device details associated with a specific execution name.
	 *
	 * @param executionName the name of the execution for which to retrieve device
	 *                      details
	 * @return a String containing the device details, or null if the execution is
	 *         not found
	 */
	String getDeviceDetailsByExecutionName(String executionName);

	/**
	 * Retrieves a list of executions that were performed on a specific date.
	 *
	 * @param date the date for which to retrieve executions, in the format
	 *             "yyyy-MM-dd"
	 * @return a list of ExecutionByDateDTO objects representing the executions
	 *         performed on the specified date
	 */
	List<ExecutionByDateDTO> getExecutionByDate(String date);
	
	/**
	 * Retrieves the execution timeout for a specific script.
	 *
	 * @param scriptName the name of the script for which to retrieve the timeout
	 * @return the execution timeout in seconds, or null if the script is not found
	 */
	Integer getScriptExecutionTimeout(String scriptName);
}
