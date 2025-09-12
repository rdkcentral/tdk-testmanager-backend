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
package com.rdkm.tdkservice.serviceimpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.dto.AnalysisIssueTypewiseSummaryDTO;
import com.rdkm.tdkservice.dto.AnalysisResultDTO;
import com.rdkm.tdkservice.dto.IssueSearchRequestDTO;
import com.rdkm.tdkservice.dto.JiraDescriptionDTO;
import com.rdkm.tdkservice.dto.TicketCreateDTO;
import com.rdkm.tdkservice.dto.TicketCreateResponseDTO;
import com.rdkm.tdkservice.dto.TicketDetailsDTO;
import com.rdkm.tdkservice.dto.TicketUpdateDTO;
import com.rdkm.tdkservice.enums.AnalysisDefectType;
import com.rdkm.tdkservice.enums.ExecutionResultStatus;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.exception.UserInputException;
import com.rdkm.tdkservice.model.Execution;
import com.rdkm.tdkservice.model.ExecutionDevice;
import com.rdkm.tdkservice.model.ExecutionResult;
import com.rdkm.tdkservice.model.ExecutionResultAnalysis;
import com.rdkm.tdkservice.model.Module;
import com.rdkm.tdkservice.model.PreCondition;
import com.rdkm.tdkservice.model.Script;
import com.rdkm.tdkservice.model.TestStep;
import com.rdkm.tdkservice.repository.ExecutionDeviceRepository;
import com.rdkm.tdkservice.repository.ExecutionRepository;
import com.rdkm.tdkservice.repository.ExecutionResultAnalysisRepository;
import com.rdkm.tdkservice.repository.ExecutionResultRepository;
import com.rdkm.tdkservice.repository.ScriptRepository;
import com.rdkm.tdkservice.service.IExecutionAnalysisService;
import com.rdkm.tdkservice.service.IScriptService;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.util.Constants;

/**
 * ExecutionAnalysisService is a service implementation class that provides
 * methods to fetch ticket details from Jira, save analysis results, and get
 * module-wise analysis status summary for execution results.
 * 
 */
@Service
public class ExecutionAnalysisService implements IExecutionAnalysisService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionAnalysisService.class);

	@Autowired
	private ExecutionResultRepository executionResultRepository;

	@Autowired
	private ExecutionResultAnalysisRepository executionResultAnalysisRepository;

	@Autowired
	private ExecutionRepository executionRepository;

	@Autowired
	private ExecutionDeviceRepository executionDeviceRepository;

	@Autowired
	private HttpService httpService;

	@Autowired
	private IScriptService scriptService;

	@Autowired
	private FileTransferService fileTransferService;

	@Autowired
	private CommonService commonService;

	@Autowired
	ScriptRepository scriptRepository;

	/**
	 * Fetches ticket details from Jira based on the provided execution result ID
	 * and project name.
	 *
	 * @param executionResultID the unique identifier of the execution result
	 * @param projectName       the name of the project in Jira
	 * @return a list of TicketDetailsDTO containing the ticket details fetched from
	 *         Jira
	 * @throws ResourceNotFoundException if the execution result ID is not found in
	 *                                   the database
	 * @throws TDKServiceException       if there is an error while sending the POST
	 *                                   request to Jira or processing the response
	 */
	@Override
	public List<TicketDetailsDTO> getTicketDetailsFromJira(UUID executionResultID, String projectName) {
		LOGGER.info("Fetching ticket details from Jira for Execution Result Id: {}", executionResultID);
		ExecutionResult executionResult = executionResultRepository.findById(executionResultID).orElseThrow(() -> {
			LOGGER.error("Execution Result Id: {} not found in the database", executionResultID);
			return new ResourceNotFoundException("Execution Result Id", executionResultID.toString());
		});

		String scriptName = executionResult.getScript();
		String configFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + Constants.TM_CONFIG_FILE;
		String baseUrl = commonService.getConfigProperty(new File(configFilePath), Constants.TICKET_HANDLER_URL);
		String apiUrl = baseUrl + Constants.SEARCH_SUMMARY_API_ENDPOINT;
		IssueSearchRequestDTO searchRequest = new IssueSearchRequestDTO();
		searchRequest.setSearchTitleOrDescription(scriptName);
		searchRequest.setProjectID(projectName);
		searchRequest.setIssueType(Constants.BUG_ISSUE_TYPE);

		ResponseEntity<String> response;
		try {
			response = httpService.sendPostRequest(apiUrl, searchRequest, null);
		} catch (Exception e) {
			LOGGER.error("Error occurred while sending POST request to {}: {}", baseUrl, e.getMessage());
			throw new TDKServiceException("Failed to fetch ticket details from Jira" + e.getMessage());
		}

		if (!response.getStatusCode().is2xxSuccessful()) {
			LOGGER.error("Failed to fetch ticket details from Jira, status code: {}", response.getStatusCode());
			throw new TDKServiceException(
					"Failed to fetch ticket details from Jira, status code: " + response.getStatusCode());
		}

		ObjectMapper objectMapper = new ObjectMapper();
		List<TicketDetailsDTO> ticketResponseList;
		try {
			ticketResponseList = objectMapper.readValue(response.getBody(),
					new TypeReference<List<TicketDetailsDTO>>() {
					});
		} catch (JsonMappingException e) {
			LOGGER.error("Error mapping JSON response to TicketDetailsDTO: {}", e.getMessage());
			throw new TDKServiceException("Error mapping JSON response to TicketDetailsDTO:" + e.getMessage());
		} catch (JsonProcessingException e) {
			LOGGER.error("Error processing JSON response: {}", e.getMessage());
			throw new TDKServiceException("Error processing JSON response:" + e.getMessage());
		}

		return ticketResponseList;
	}

	/**
	 * This method is used to save the analysis result
	 * 
	 * @param analysisResultRequest - the analysis result request DTO
	 * @return boolean - true if the analysis result is saved successfully
	 */
	@Override
	public boolean saveAnalysisResult(UUID executionResultID, AnalysisResultDTO analysisResultRequest) {
		LOGGER.info("Saving analysis result for execution result id: {}", executionResultID.toString());

		// Get the execution result based on the ID provided and save it after marking
		// the execution as analyzed
		ExecutionResult executionResult = executionResultRepository.findById(executionResultID).orElseThrow(
				() -> new ResourceNotFoundException("ExecutionResult with ID", executionResultID.toString()));

		// Check if there is already an analysis result for the execution result
		ExecutionResultAnalysis executionResultAnalysis = executionResultAnalysisRepository
				.findByExecutionResult(executionResult);
		if (null == executionResultAnalysis) {
			LOGGER.info("Analysis result already exists for execution result id: {}", executionResultID.toString());
			executionResultAnalysis = new ExecutionResultAnalysis();
			// Save the ExecutionResultAnalysis based on the data given in DTO and save it
			executionResultAnalysis.setExecutionResult(executionResult);
		}

		AnalysisDefectType defectType = AnalysisDefectType
				.getAnalysisDefectTypefromValue(analysisResultRequest.getAnalysisDefectType());

		if (null != defectType) {
			executionResultAnalysis.setAnalysisDefectType(defectType);
		}
		executionResultAnalysis.setAnalysisUser(analysisResultRequest.getAnalysisUser());
		executionResultAnalysis.setAnalysisRemark(analysisResultRequest.getAnalysisRemark());
		executionResultAnalysis.setAnalysisTicketID(analysisResultRequest.getAnalysisTicketID());
		executionResultAnalysisRepository.save(executionResultAnalysis);

		LOGGER.info("Successfully saved analysis result for execution result id: {}", executionResultID.toString());
		return true;
	}

	/**
	 * 
	 * This method is to get the module wise anlysis status summary
	 * 
	 * @param executionId - the execution id
	 * @return the module wise anlysis status summary
	 */
	@Override
	public Map<String, AnalysisIssueTypewiseSummaryDTO> getModulewiseAnalysisSummary(UUID executionId) {
		LOGGER.info("Fetching analysis summary for id: {}", executionId);

		Execution execution = executionRepository.findById(executionId)
				.orElseThrow(() -> new ResourceNotFoundException("Execution with id", executionId.toString()));

		List<ExecutionResult> executionResults = executionResultRepository.findByExecution(execution);
		if (executionResults.isEmpty()) {
			LOGGER.error("No execution results found for execution with id: {}", executionId);
			return null;
		}

		Map<String, AnalysisIssueTypewiseSummaryDTO> modulewiseAnalysisSummaryMap = new HashMap<>();

		for (ExecutionResult executionResult : executionResults) {
			if (executionResult.getResult() == ExecutionResultStatus.FAILURE) {

				// Get the module name of the script
				String scriptName = executionResult.getScript();
				Module module = scriptService.getModuleByScriptName(scriptName);
				String moduleName = module.getName();

				// If the module name is already there in the map , then the existing DTO is
				// updated
				// other wise a new DTO is created and added to the map
				AnalysisIssueTypewiseSummaryDTO analysisIssueTypewiseSummaryDTO = null;
				if (modulewiseAnalysisSummaryMap.containsKey(moduleName)) {
					analysisIssueTypewiseSummaryDTO = modulewiseAnalysisSummaryMap.get(moduleName);
				} else {
					analysisIssueTypewiseSummaryDTO = new AnalysisIssueTypewiseSummaryDTO();
				}

				// Get ExecutionResultAnalysis from the ExecutionResult
				ExecutionResultAnalysis executionResultAnalysis = executionResultAnalysisRepository
						.findByExecutionResult(executionResult);
				// Failure count is updated
				analysisIssueTypewiseSummaryDTO.setFailure(analysisIssueTypewiseSummaryDTO.getFailure() + 1);

				AnalysisDefectType analysisDefectType = null;
				if (null != executionResultAnalysis) {
					analysisIssueTypewiseSummaryDTO.setAnalysed(analysisIssueTypewiseSummaryDTO.getAnalysed() + 1);
					analysisDefectType = executionResultAnalysis.getAnalysisDefectType();
					if (null != analysisDefectType) {
						this.setAnalysisSummaryCount(analysisIssueTypewiseSummaryDTO, analysisDefectType);
					}
				}

				if (analysisIssueTypewiseSummaryDTO.getFailure() > 0) {
					analysisIssueTypewiseSummaryDTO.setNotAnalysed(analysisIssueTypewiseSummaryDTO.getFailure()
							- analysisIssueTypewiseSummaryDTO.getAnalysed());
				}

				modulewiseAnalysisSummaryMap.put(moduleName, analysisIssueTypewiseSummaryDTO);
			}
		}

		if (modulewiseAnalysisSummaryMap.isEmpty()) {
			LOGGER.error(
					"No module-wise analysis needed as there is no execution with failure status for the execution {}",
					executionId);
			return null;
		}

		try {
			// Update the analysis summary map with percentage
			this.updateAnalysisSummaryMapWithPercentageCount(modulewiseAnalysisSummaryMap);

			// Find the total analysis summary of all the failures in the execution
			this.updateTotalAnalysisSummary(modulewiseAnalysisSummaryMap);
		} catch (Exception e) {
			LOGGER.error("Error updating analysis summary map: {}", e.getMessage());
		}

		LOGGER.info("Successfully fetched analysis summary for id: {}", executionId);
		return modulewiseAnalysisSummaryMap;

	}

	/**
	 * This method is used to update the total analysis summary of all the failures
	 * in the execution
	 * 
	 * @param modulewiseAnalysisSummaryMap - map updated with total data
	 */
	private void updateTotalAnalysisSummary(Map<String, AnalysisIssueTypewiseSummaryDTO> modulewiseAnalysisSummaryMap) {
		AnalysisIssueTypewiseSummaryDTO totalAnalysisSummary = new AnalysisIssueTypewiseSummaryDTO();

		for (Map.Entry<String, AnalysisIssueTypewiseSummaryDTO> entry : modulewiseAnalysisSummaryMap.entrySet()) {
			AnalysisIssueTypewiseSummaryDTO analysisIssueTypewiseSummaryDTO = entry.getValue();
			totalAnalysisSummary
					.setFailure(totalAnalysisSummary.getFailure() + analysisIssueTypewiseSummaryDTO.getFailure());
			totalAnalysisSummary
					.setRdkIssue(totalAnalysisSummary.getRdkIssue() + analysisIssueTypewiseSummaryDTO.getRdkIssue());
			totalAnalysisSummary.setScriptIssue(
					totalAnalysisSummary.getScriptIssue() + analysisIssueTypewiseSummaryDTO.getScriptIssue());
			totalAnalysisSummary.setInterfaceChange(
					totalAnalysisSummary.getInterfaceChange() + analysisIssueTypewiseSummaryDTO.getInterfaceChange());
			totalAnalysisSummary
					.setEnvIssue(totalAnalysisSummary.getEnvIssue() + analysisIssueTypewiseSummaryDTO.getEnvIssue());
			totalAnalysisSummary.setOtherIssue(
					totalAnalysisSummary.getOtherIssue() + analysisIssueTypewiseSummaryDTO.getOtherIssue());
			totalAnalysisSummary
					.setAnalysed(totalAnalysisSummary.getAnalysed() + analysisIssueTypewiseSummaryDTO.getAnalysed());
			totalAnalysisSummary.setNotAnalysed(
					totalAnalysisSummary.getNotAnalysed() + analysisIssueTypewiseSummaryDTO.getNotAnalysed());
		}

		// Calculate the percentage based on Total analysed and Total Failure
		int totalAnalysed = totalAnalysisSummary.getEnvIssue() + totalAnalysisSummary.getInterfaceChange()
				+ totalAnalysisSummary.getOtherIssue() + totalAnalysisSummary.getRdkIssue()
				+ totalAnalysisSummary.getScriptIssue();
		int totalFailure = totalAnalysisSummary.getFailure();
		if (totalAnalysed > 0 && totalFailure > 0) {
			int percentage = (int) Math.round(((double) totalAnalysed / totalFailure) * 100);
			totalAnalysisSummary.setPercentageAnalysed(percentage);
		}

		modulewiseAnalysisSummaryMap.put(Constants.TOTAL_KEYWORD, totalAnalysisSummary);
	}

	/**
	 * This method is used to update the analysis summary map with the analysis
	 * percentage for each module
	 * 
	 * @param modulewiseAnalysisSummaryMap
	 */
	private void updateAnalysisSummaryMapWithPercentageCount(
			Map<String, AnalysisIssueTypewiseSummaryDTO> modulewiseAnalysisSummaryMap) {

		for (Map.Entry<String, AnalysisIssueTypewiseSummaryDTO> entry : modulewiseAnalysisSummaryMap.entrySet()) {
			String moduleName = entry.getKey();
			AnalysisIssueTypewiseSummaryDTO analysisIssueTypewiseSummaryDTO = entry.getValue();

			// Calculate total executions
			int totalAnalysed = analysisIssueTypewiseSummaryDTO.getAnalysed();
			int totalFailure = analysisIssueTypewiseSummaryDTO.getFailure();

			// Calculate the percentage based on Total analysed and Total Failure
			if (totalFailure > 0) {
				int percentage = (int) Math.round(((double) totalAnalysed / totalFailure) * 100);
				analysisIssueTypewiseSummaryDTO.setPercentageAnalysed(percentage);
			}

			// Update the map with the modified DTO
			modulewiseAnalysisSummaryMap.put(moduleName, analysisIssueTypewiseSummaryDTO);
		}
	}

	/**
	 * This method is used to get the analysis summary count based on different
	 * issue types
	 * 
	 * @param analysisIssueTypewiseSummaryDTO - the analysis issue type wise summary
	 * @param analysisDefectType              - Analysis Defect Type
	 */
	private void setAnalysisSummaryCount(AnalysisIssueTypewiseSummaryDTO analysisIssueTypewiseSummaryDTO,
			AnalysisDefectType analysisDefectType) {
		switch (analysisDefectType) {
		case RDK_ISSUE:
			analysisIssueTypewiseSummaryDTO.setRdkIssue(analysisIssueTypewiseSummaryDTO.getRdkIssue() + 1);
			break;
		case SCRIPT_ISSUE:
			analysisIssueTypewiseSummaryDTO.setScriptIssue(analysisIssueTypewiseSummaryDTO.getScriptIssue() + 1);
			break;
		case INTERFACE_CHANGE:
			analysisIssueTypewiseSummaryDTO
					.setInterfaceChange(analysisIssueTypewiseSummaryDTO.getInterfaceChange() + 1);
			break;
		case ENV_ISSUE:
			analysisIssueTypewiseSummaryDTO.setEnvIssue(analysisIssueTypewiseSummaryDTO.getEnvIssue() + 1);
			break;
		case OTHER_ISSUE:
			analysisIssueTypewiseSummaryDTO.setOtherIssue(analysisIssueTypewiseSummaryDTO.getOtherIssue() + 1);
			break;
		default:
			break;

		}

	}

	/**
	 * This method is used to get the analysis summary for the given execution ID.
	 * 
	 * @param executionResultID - the unique identifier of the execution result
	 * @return the analysis summary
	 */
	@Override
	public AnalysisResultDTO getAnalysisResult(UUID executionResultID) {
		LOGGER.info("Fetching analysis result for execution result id: {}", executionResultID.toString());
		ExecutionResult executionResult = executionResultRepository.findById(executionResultID).orElseThrow(
				() -> new ResourceNotFoundException("ExecutionResult with ID", executionResultID.toString()));
		ExecutionResultAnalysis executionResultAnalysis = executionResultAnalysisRepository
				.findByExecutionResult(executionResult);
		if (null == executionResultAnalysis) {
			LOGGER.error("Analysis result not found for execution result id: {}", executionResultID);
			return null;
		}
		AnalysisResultDTO analysisResultDTO = new AnalysisResultDTO();
		if (executionResultAnalysis.getAnalysisDefectType() != null
				&& executionResultAnalysis.getAnalysisDefectType().getValue() != null)
			analysisResultDTO.setAnalysisDefectType(executionResultAnalysis.getAnalysisDefectType().getValue());
		if (executionResultAnalysis.getAnalysisRemark() != null)
			analysisResultDTO.setAnalysisRemark(executionResultAnalysis.getAnalysisRemark());
		analysisResultDTO.setAnalysisTicketID(executionResultAnalysis.getAnalysisTicketID());
		if (executionResultAnalysis.getAnalysisUser() != null)
			analysisResultDTO.setAnalysisUser(executionResultAnalysis.getAnalysisUser());
		LOGGER.info("Successfully fetched analysis result for execution result id: {}", executionResultID.toString());

		return analysisResultDTO;
	}

	/**
	 * Fetches details for populating ticket details for a given execution result
	 * ID.
	 *
	 * @param execResultID the UUID of the execution result
	 * @return JiraDescriptionDTO containing the ticket details description and
	 *         image version
	 * @throws ResourceNotFoundException if the execution result with the given ID
	 *                                   is not found
	 */
	@Override
	public JiraDescriptionDTO getDetailsForPopulatingTicketDetails(UUID execResultID) {
		LOGGER.info("Fetching details for populating ticket details for execution result id: {}", execResultID);
		ExecutionResult executionResult = executionResultRepository.findById(execResultID)
				.orElseThrow(() -> new ResourceNotFoundException("ExecutionResult with ID", execResultID.toString()));

		Execution execution = executionResult.getExecution();

		Script script = scriptRepository.findByName(executionResult.getScript());
		JiraDescriptionDTO jiraDescriptionDTO = new JiraDescriptionDTO();

		String imageName = fileTransferService.getDeviceDetailsFromVersionFile(execution.getId().toString());

		ExecutionDevice executionDevice = executionDeviceRepository.findByExecution(execution);

		if (null == imageName || imageName.isEmpty()) {
			imageName = executionDevice.getBuildName();
		}
		String ticketDetailDescription = getTicketDetailDescription(script, execution, imageName);
		jiraDescriptionDTO.setDescription(ticketDetailDescription);
		jiraDescriptionDTO.setImageVersion(imageName);
		LOGGER.info("Successfully fetched details for populating ticket details for execution result id: {}",
				execResultID);
		return jiraDescriptionDTO;

	}

	/**
	 * Generates a detailed description for a ticket based on the provided script,
	 * execution, and image name.
	 *
	 * @param script    the script containing the test steps and expected output
	 * @param execution the execution instance containing the execution name
	 * @param imageName the name of the image associated with the execution
	 * @return a string containing the detailed description of the ticket
	 */
	private String getTicketDetailDescription(Script script, Execution execution, String imageName) {
		StringBuilder description = new StringBuilder();
		List<String> executionNames = executionRepository.findAll().stream().map(Execution::getName)
				.collect(Collectors.toList());
		int count = countRerunExecutions(execution.getName(), executionNames);
		description.append(script.getName()).append(" failed in execution: ").append(execution.getName())
				.append("\n\n");
		List<PreCondition> preConditionList = script.getPreConditions();
		if (null != preConditionList && !preConditionList.isEmpty()) {
			description.append("*Preconditions* : \n\n");
			for (PreCondition preCondition : preConditionList) {
				description.append(preCondition.getPreConditionDescription()).append("\n");
			}
			description.append("\n");
		}

		List<TestStep> testStep = script.getTestSteps();
		if (null != testStep && !testStep.isEmpty()) {
			description.append("*Test Steps* : \n\n");
			description.append("|Step Name|Step Description|Expected Result|\n");
			for (TestStep step : testStep) {
				description.append("|" + step.getStepName()).append("|").append(step.getStepDescription()).append("|")
						.append(step.getExpectedResult() + "|\n");
			}
			description.append("\n");
		}
		description.append("*Actual Result* :  < ENTER ACTUAL RESULTS >\n\n");

		description.append("*Image Details*:\n\n").append(imageName).append("\n\n").append("*Rerun status* :\n");

		if (count > 0) {
			description.append(count).append(" reruns of the execution was performed for this result");
		} else {
			description.append("No reruns of the execution was performed for this result");
		}

		return description.toString();
	}

	/*
	 * The method to get the configuration file based on the category
	 * 
	 * @param category - the category of the configuration file
	 * 
	 * @return the path of the configuration file based on the category
	 */
	private String getConfigFileBasedOnCategory(String category) {
		String configFile = null;
		if ("RDKV".equalsIgnoreCase(category)) {
			configFile = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
					+ Constants.ISSUE_ANALYSER_CONFIG_RDKV;
		} else if ("RDKB".equalsIgnoreCase(category)) {
			configFile = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
					+ Constants.ISSUE_ANALYSER_CONFIG_RDKB;
		}
		return configFile;
	}

	/**
	 * Counts the number of executions that match the given execution name,
	 * including reruns. A rerun is identified by the suffix "_RERUN_" followed by a
	 * number.
	 *
	 * @param executionName  the base name of the execution to count
	 * @param executionNames the list of execution names to search through
	 * @return the count of executions that match the given execution name,
	 *         including reruns
	 */
	private static int countRerunExecutions(String executionName, List<String> executionNames) {
		int count = 0;
		Pattern pattern = Pattern.compile(executionName + "(?:_RERUN_\\d+)?");

		for (String name : executionNames) {
			Matcher matcher = pattern.matcher(name);
			if (matcher.matches()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Retrieves a list of project IDs from the configuration file.
	 *
	 * This method reads the configuration file specified by the base location and
	 * the issue analyzer configuration file name. It then fetches the project IDs
	 * from the configuration file and returns them as a list of strings.
	 *
	 * @return a list of project IDs if found in the configuration file, otherwise
	 *         null.
	 */
	@Override
	public List<String> getListOfProjectIDs(String category) {
		LOGGER.info("Fetching list of project IDs");
		String configFilePath = this.getConfigFileBasedOnCategory(category);
		String projectIDs = commonService.getConfigProperty(new File(configFilePath), Constants.PROJECT_IDS);
		if (null == projectIDs || projectIDs.isEmpty()) {
			LOGGER.error("No project IDs found in the config file");
			return null;
		}
		List<String> projectIDList = List.of(projectIDs.split(","));
		LOGGER.info("Successfully fetched list of project IDs");
		return projectIDList;
	}

	/**
	 * Checks if the given project ID is a platform project ID.
	 *
	 * This method retrieves the platform project IDs from a configuration file and
	 * checks if the given project ID is present in the list of platform project
	 * IDs.
	 *
	 * @param projectID the project ID to check
	 * @return true if the project ID is a platform project ID, false otherwise
	 */
	@Override
	public String getProjectCategory(String projectID, String category) {
		LOGGER.info("Checking if the project ID is a platform project ID: {}", projectID);
		String configFilePath = this.getConfigFileBasedOnCategory(category);
		String platformProjectIDs = commonService.getConfigProperty(new File(configFilePath),
				Constants.PLATFORM_PROJECT_IDS);
		if (null == platformProjectIDs || platformProjectIDs.isEmpty()) {
			LOGGER.error("No platform project IDs found in the config file");
			return projectID;
		}
		List<String> platformProjectIDList = List.of(platformProjectIDs.split(","));
		boolean isPlatformProjectID = platformProjectIDList.contains(projectID);
		LOGGER.info("Project ID: {} is a platform project ID: {}", projectID, isPlatformProjectID);
		return isPlatformProjectID ? "PLATFORM" : projectID;
	}

	/**
	 * Retrieves a list of labels from the configuration file.
	 * 
	 * This method fetches the labels from a configuration file specified by the
	 * application configuration. If the labels are not found or the configuration
	 * file is empty, an error is logged and the method returns null.
	 * 
	 * @return a list of labels if found, otherwise null
	 */
	@Override
	public List<String> getListOfLabels(String category) {
		LOGGER.info("Fetching list of labels");
		String configFilePath = this.getConfigFileBasedOnCategory(category);
		String labels = commonService.getConfigProperty(new File(configFilePath), Constants.LABELS);
		if (null == labels || labels.isEmpty()) {
			LOGGER.error("No labels found in the config file");
			return null;
		}
		List<String> labelList = List.of(labels.split(","));
		LOGGER.info("Successfully fetched list of labels");
		return labelList;
	}

	/**
	 * Retrieves the list of release versions from the configuration file.
	 *
	 * @return a list of release version strings, or null if no release versions are
	 *         found in the config file.
	 */
	@Override
	public List<String> getReleaseVersions(String category) {
		LOGGER.info("Fetching list of release versions");
		String configFilePath = this.getConfigFileBasedOnCategory(category);
		String releaseVersions = commonService.getConfigProperty(new File(configFilePath), Constants.RELEASE_VERSIONS);
		if (null == releaseVersions || releaseVersions.isEmpty()) {
			LOGGER.error("No release versions found in the config file");
			return null;
		}
		List<String> releaseVersionList = List.of(releaseVersions.split(","));
		LOGGER.info("Successfully fetched list of release versions");
		return releaseVersionList;
	}

	/**
	 * Retrieves the list of hardware configurations from the configuration file.
	 *
	 * @return a list of hardware configuration strings, or null if no
	 *         configurations are found
	 */
	@Override
	public List<String> getHardwareConfiguration(String category) {
		LOGGER.info("Fetching list of hardware configurations");
		String configFilePath = this.getConfigFileBasedOnCategory(category);
		String hardwareConfigurations = commonService.getConfigProperty(new File(configFilePath),
				Constants.HARDWARE_CONFIGURATIONS);
		if (null == hardwareConfigurations || hardwareConfigurations.isEmpty()) {
			LOGGER.error("No hardware configurations found in the config file");
			return null;
		}
		List<String> hardwareConfigurationList = List.of(hardwareConfigurations.split(","));
		LOGGER.info("Successfully fetched list of hardware configurations");
		return hardwareConfigurationList;
	}

	/**
	 * Retrieves a list of impacted platforms from the configuration file.
	 *
	 * @return a list of impacted platforms if found, otherwise null
	 */
	@Override
	public List<String> getImpactedPlatforms(String category) {
		LOGGER.info("Fetching list of impacted platforms");
		String configFilePath = this.getConfigFileBasedOnCategory(category);
		String impactedPlatforms = commonService.getConfigProperty(new File(configFilePath),
				Constants.IMPACTED_PLATFORMS);
		if (null == impactedPlatforms || impactedPlatforms.isEmpty()) {
			LOGGER.error("No impacted platforms found in the config file");
			return null;
		}
		List<String> impactedPlatformList = List.of(impactedPlatforms.split(","));
		LOGGER.info("Successfully fetched list of impacted platforms");
		return impactedPlatformList;
	}

	/**
	 * Retrieves a list of severities from the configuration file.
	 *
	 * This method fetches the severities from a configuration file specified by the
	 * application configuration. If the severities are not found or the
	 * configuration file is empty, an error is logged and the method returns null.
	 *
	 * @return a list of severities as strings, or null if no severities are found.
	 */
	@Override
	public List<String> getSeverities(String category) {
		LOGGER.info("Fetching list of severities");
		String configFilePath = this.getConfigFileBasedOnCategory(category);
		String severities = commonService.getConfigProperty(new File(configFilePath), Constants.SEVERITIES);
		if (null == severities || severities.isEmpty()) {
			LOGGER.error("No severities found in the config file");
			return null;
		}
		List<String> severityList = List.of(severities.split(","));
		LOGGER.info("Successfully fetched list of severities");
		return severityList;
	}

	/**
	 * Retrieves a list of fixed-in versions from the configuration file.
	 *
	 * This method fetches the configuration file path using the base location and
	 * the issue analyzer configuration constants. It then reads the fixed-in
	 * versions property from the configuration file. If the property is not found
	 * or is empty, an error is logged and the method returns null. Otherwise, the
	 * fixed-in versions are split by commas and returned as a list.
	 * 
	 *
	 * @return a list of fixed-in versions, or null if the property is not found or
	 *         is empty.
	 */
	@Override
	public List<String> getFixedInVersions(String category) {
		LOGGER.info("Fetching list of fixed in versions");
		String configFilePath = this.getConfigFileBasedOnCategory(category);
		String fixedInVersions = commonService.getConfigProperty(new File(configFilePath), Constants.FIXED_IN_VERSIONS);
		if (null == fixedInVersions || fixedInVersions.isEmpty()) {
			LOGGER.error("No fixed in versions found in the config file");
			return null;
		}
		List<String> fixedInVersionList = List.of(fixedInVersions.split(","));
		LOGGER.info("Successfully fetched list of fixed in versions");
		return fixedInVersionList;
	}

	/**
	 * Retrieves a list of components impacted from the configuration file.
	 *
	 * @return a list of components impacted, or null if no components are found in
	 *         the config file.
	 */
	@Override
	public List<String> getComponentsImpacted(String category) {
		LOGGER.info("Fetching list of components impacted");
		String configFilePath = this.getConfigFileBasedOnCategory(category);
		String componentsImpacted = commonService.getConfigProperty(new File(configFilePath),
				Constants.COMPONENTS_IMPACTED);
		if (null == componentsImpacted || componentsImpacted.isEmpty()) {
			LOGGER.error("No components impacted found in the config file");
			return null;
		}
		List<String> componentsImpactedList = List.of(componentsImpacted.split(","));
		LOGGER.info("Successfully fetched list of components impacted");
		return componentsImpactedList;
	}

	/**
	 * Retrieves a list of defect types.
	 *
	 * This method fetches the list of defect types by mapping the names of the enum
	 * values of AnalysisDefectType to a list of strings.
	 *
	 * @return a list of defect type names as strings
	 */
	@Override
	public List<String> getDefectTypes() {
		LOGGER.info("Fetching list of defect types");
		List<String> defectTypes = Arrays.stream(AnalysisDefectType.values()).map(Enum::name)
				.collect(Collectors.toList());
		LOGGER.info("Successfully fetched list of defect types");
		return defectTypes;
	}

	/**
	 * Retrieves the steps to reproduce for a given script name.
	 *
	 * @param scriptName the name of the script for which steps to reproduce are to
	 *                   be fetched
	 * @return a formatted string containing the steps to reproduce, or null if the
	 *         script is not found
	 */
	@Override
	public String getStepsToReproduce(String scriptName) {
		LOGGER.info("Fetching steps to reproduce");
		Script script = scriptRepository.findByName(scriptName);
		if (null == script) {
			LOGGER.error("Script with name: {} not found", scriptName);
			return null;
		}
		StringBuilder stepsToReproduce = new StringBuilder();
		List<PreCondition> preConditionList = script.getPreConditions();
		if (null != preConditionList && !preConditionList.isEmpty()) {
			stepsToReproduce.append("*Preconditions* : \n\n");
			for (PreCondition preCondition : preConditionList) {
				stepsToReproduce.append(preCondition.getPreConditionDescription()).append("\n");
			}
			stepsToReproduce.append("\n");
		}

		stepsToReproduce.append("*Test Steps* : \n\n");
		List<TestStep> testStep = script.getTestSteps();
		if (null != testStep && !testStep.isEmpty()) {

			for (TestStep step : testStep) {
				stepsToReproduce.append(step.getStepName()).append(" - ").append(step.getStepDescription())
						.append("\n");
			}
			stepsToReproduce.append("\n");
		}
		return stepsToReproduce.toString();

	}

	/**
	 * Retrieves a list of priorities from the configuration file.
	 *
	 * This method fetches the priorities from a configuration file specified by the
	 * application configuration. If the priorities are not found or the
	 * configuration file is empty, an error is logged and the method returns null.
	 *
	 * @return a list of priorities as strings, or null if no priorities are found.
	 */
	@Override
	public List<String> getPriorities(String category) {
		LOGGER.info("Fetching list of priorities");
		String configFilePath = this.getConfigFileBasedOnCategory(category);
		String priorities = commonService.getConfigProperty(new File(configFilePath), Constants.PRIORITIES);
		if (null == priorities || priorities.isEmpty()) {
			LOGGER.error("No priorities found in the config file");
			return null;
		}
		List<String> priorityList = List.of(priorities.split(","));
		LOGGER.info("Successfully fetched list of priorities");
		return priorityList;
	}

	/**
	 * Validates the fields of a TicketCreateDTO object for a platform project.
	 * Throws a UserInputException if any required field is missing or invalid.
	 *
	 * @param ticketCreateDTO the TicketCreateDTO object to validate
	 * @throws UserInputException if any required field is missing or invalid
	 */
	private void validatePlatformProjectFields(TicketCreateDTO ticketCreateDTO) {
		if (ticketCreateDTO.getHardwareConfig() == null || ticketCreateDTO.getHardwareConfig().isEmpty()) {
			throw new UserInputException("Hardware Configuration field is required for platform project");
		}
		if (ticketCreateDTO.getImpactedPlatforms() == null || ticketCreateDTO.getImpactedPlatforms().isEmpty()) {
			throw new UserInputException("Impacted Platform field is required for platform project");
		}
		if (ticketCreateDTO.getReleaseVersion() == null || ticketCreateDTO.getReleaseVersion().isEmpty()) {
			throw new UserInputException("Release Version field is required for platform project");
		}
		if (ticketCreateDTO.getComponentsImpacted() == null || ticketCreateDTO.getComponentsImpacted().isEmpty()) {
			throw new UserInputException("Components Impacted field is required for platform project");
		}
		if (ticketCreateDTO.getSeverity() == null || ticketCreateDTO.getSeverity().isEmpty()) {
			throw new UserInputException("Severity field is required for platform project");
		}
		if (ticketCreateDTO.getEnvironmentForTestSetup() == null
				|| ticketCreateDTO.getEnvironmentForTestSetup().isEmpty()) {
			throw new UserInputException("Environment for Test Setup field is required for platform project");
		}
		if (ticketCreateDTO.getReproducability() <= 0) {
			throw new UserInputException("Reproducability field is required for platform project");
		}
		if (ticketCreateDTO.getStepsToReproduce() == null || ticketCreateDTO.getStepsToReproduce().isEmpty()) {
			throw new UserInputException("Steps to reproduce field is required for platform project");
		}
		if (ticketCreateDTO.getThirdPartyDependency() == null || ticketCreateDTO.getThirdPartyDependency().isEmpty()) {
			throw new UserInputException("Third Party Dependency field is required for platform project");
		}
		if (ticketCreateDTO.getTdkVersion() == null || ticketCreateDTO.getTdkVersion().isEmpty()) {
			throw new UserInputException("TDK Version field is required for platform project");
		}
	}

	/**
	 * Validates the fields of a RDKPREINTG project in the provided TicketCreateDTO.
	 * Throws a UserInputException if the Acceptance Criteria field is null or
	 * empty.
	 *
	 * @param ticketCreateDTO the TicketCreateDTO object containing the project
	 *                        details to be validated
	 * @throws UserInputException if the Acceptance Criteria field is null or empty
	 */
	private void validateTDKProjectFields(TicketCreateDTO ticketCreateDTO) {
		if (ticketCreateDTO.getRdkVersion() == null || ticketCreateDTO.getRdkVersion().isEmpty()) {
			throw new UserInputException("Rdk version field is required for TDK project");
		}
	}

	/**
	 * Creates a Jira ticket based on the provided TicketCreateDTO.
	 *
	 * @param ticketCreateDTO the DTO containing the details for the ticket creation
	 * @return a string message indicating the result of the ticket creation process
	 * @throws UserInputException  if there is an error with the user input
	 * @throws TDKServiceException if there is a general error during the ticket
	 *                             creation process
	 */
	@Override
	public String createJiraTicket(TicketCreateDTO ticketCreateDTO) {
		LOGGER.info("Creating Jira ticket for ticket create DTO: {}", ticketCreateDTO);
		StringBuilder responseString = new StringBuilder();

		try {
			if (this.getProjectCategory(ticketCreateDTO.getProjectName(), ticketCreateDTO.getCategory())
					.equals("PLATFORM")) {
				validatePlatformProjectFields(ticketCreateDTO);
			}

			if (Constants.TDK_PROJECT_NAME.equals(ticketCreateDTO.getProjectName())) {
				validateTDKProjectFields(ticketCreateDTO);
			}

			String configFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
					+ Constants.TM_CONFIG_FILE;
			String baseUrl = commonService.getConfigProperty(new File(configFilePath), Constants.TICKET_HANDLER_URL);
			String apiUrl = baseUrl + Constants.CREATE_API_ENDPOINT;

			ResponseEntity<String> response = httpService.sendPostRequest(apiUrl, ticketCreateDTO, null);
			ObjectMapper objectMapper = new ObjectMapper();
			TicketCreateResponseDTO ticketResponse = null;
			if (response.getStatusCode().equals(HttpStatus.OK)) {
				ticketResponse = objectMapper.readValue(response.getBody(),
						new TypeReference<TicketCreateResponseDTO>() {
						});
			}

			if (response.getStatusCode().equals(HttpStatus.OK)) {
				LOGGER.info("Ticket ID: {} created successfully", ticketResponse.getTicketNumber());
				responseString.append("Ticket ID: ").append(ticketResponse.getTicketNumber())
						.append(" created successfully.");
			} else {
				LOGGER.error("Failed to create Jira ticket: {}", response.getBody());
				responseString.append("Ticket creation Failed.");
			}
			ExecutionResultAnalysis executionResultAnalysis = executionResultAnalysisRepository.findByExecutionResult(
					executionResultRepository.findById(UUID.fromString(ticketCreateDTO.getExecutionResultId())).get());

			if (executionResultAnalysis == null) {
				executionResultAnalysis = new ExecutionResultAnalysis();
				executionResultAnalysis.setExecutionResult(executionResultRepository
						.findById(UUID.fromString(ticketCreateDTO.getExecutionResultId())).get());

			}

			executionResultAnalysis.setAnalysisDefectType(
					AnalysisDefectType.getAnalysisDefectTypefromValue(ticketCreateDTO.getAnalysisDefectType()));

			executionResultAnalysis.setAnalysisRemark(ticketCreateDTO.getAnalysisRemark());
			if (ticketResponse.getTicketNumber() != null) {
				executionResultAnalysis.setAnalysisTicketID(ticketResponse.getTicketNumber());
			}
			executionResultAnalysis.setAnalysisUser(ticketCreateDTO.getAnalysisUser());
			executionResultAnalysisRepository.save(executionResultAnalysis);
			responseString.append("Defect analysis saved to db.");

			if (!response.getStatusCode().equals(HttpStatus.OK)) {
				return responseString.toString();
			}

			String attachUrl = baseUrl + Constants.ATTACHMENT_API_ENDPOINT;
			if (ticketCreateDTO.isExecutionLogRequired()) {
				String execLogMessage = attachExecutionLogs(attachUrl, ticketCreateDTO.getExecutionResultId(),
						ticketResponse.getTicketNumber(), ticketCreateDTO.getUser(), ticketCreateDTO.getPassword());
				responseString.append(execLogMessage);
			}

			if (ticketCreateDTO.isDeviceLogRequired()) {
				String deviceLogMessage = attachDeviceLogs(attachUrl, ticketCreateDTO.getExecutionResultId(),
						ticketResponse.getTicketNumber(), ticketCreateDTO.getUser(), ticketCreateDTO.getPassword());
				responseString.append(deviceLogMessage);
			}

		} catch (UserInputException e) {
			LOGGER.error("User input error: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			LOGGER.error("Error occurred while creating Jira ticket: {}", e.getMessage());
			throw new TDKServiceException("Failed to create Jira ticket: " + e.getMessage());
		}

		return responseString.toString();
	}

	/**
	 * Attaches device logs to a specified ticket.
	 *
	 * @param apiUrl            the API URL to which the logs will be attached
	 * @param executionResultId the ID of the execution result to retrieve logs for
	 * @param ticketNumber      the ticket number to which the logs will be attached
	 * @param user              the username for authentication
	 * @param password          the password for authentication
	 * @return a message indicating the result of the operation
	 */
	private String attachDeviceLogs(String apiUrl, String executionResultId, String ticketNumber, String user,
			String password) {
		File deviceLogFile = new File("deviceLog.zip");
		try {
			byte[] fileBytes = fileTransferService.downloadAllDeviceLogFiles(executionResultId);

			try (FileOutputStream fos = new FileOutputStream(deviceLogFile)) {
				fos.write(fileBytes);
			}

			ResponseEntity<String> response = httpService.addAttachmentToTicket(apiUrl, ticketNumber, deviceLogFile,
					user, password);
			if (response.getStatusCode().equals(HttpStatus.OK)) {
				LOGGER.info("Device logs attached successfully");
				return "Device logs attached successfully.";
			} else {
				LOGGER.error("Failed to attach device logs: {}", response.getBody());
				return "Failed to attach device logs ";
			}
		} catch (ResourceNotFoundException | FileNotFoundException e) {
			LOGGER.error("Error attaching device logs: {}", e.getMessage());
			return "Failed to attach device logs.Device logs not found.";
		} catch (Exception e) {
			LOGGER.error("Error attaching device logs: {}", e.getMessage());
			return "Failed to attach device logs ";
		} finally {
			if (deviceLogFile.exists()) {
				if (!deviceLogFile.delete()) {
					LOGGER.warn("Failed to delete temporary log file: {}", deviceLogFile.getAbsolutePath());
				}
			}
		}
	}

	/**
	 * Attaches execution logs to a ticket.
	 *
	 * @param apiUrl      the API URL to attach the logs
	 * @param execResutId the ID of the execution result
	 * @param ticketId    the ID of the ticket to attach the logs to
	 * @param userName    the username for authentication
	 * @param password    the password for authentication
	 * @return a message indicating the result of the operation
	 */
	private String attachExecutionLogs(String apiUrl, String execResutId, String ticketId, String userName,
			String password) {
		try {
			ExecutionResult executionResult = executionResultRepository.findById(UUID.fromString(execResutId))
					.orElseThrow(
							() -> new ResourceNotFoundException("ExecutionResult with ID", execResutId.toString()));
			String executionID = executionResult.getExecution().getId().toString();

			String executionLogfile = commonService.getExecutionLogFilePath(executionID,
					executionResult.getId().toString());
			File logFile = new File(executionLogfile);
			File zipFile = convertToZip(logFile);

			ResponseEntity<String> response = httpService.addAttachmentToTicket(apiUrl, ticketId, zipFile, userName,
					password);
			if (zipFile.exists()) {
				if (!zipFile.delete()) {
					LOGGER.warn("Failed to delete temporary log file: {}", zipFile.getAbsolutePath());
				}
			}
			if (response.getStatusCode().equals(HttpStatus.OK)) {
				LOGGER.info("Execution logs attached successfully");
				return "Execution logs attached successfully.";
			} else {
				LOGGER.error("Failed to attach execution logs: {}", response.getBody());
				return "Failed to attach execution logs.";
			}

		} catch (ResourceNotFoundException e) {
			LOGGER.error("Error attaching execution logs: {}", e.getMessage());
			return "Failed to attach execution logs.Execution logs not found.";
		} catch (Exception e) {
			LOGGER.error("Error attaching execution logs: {}", e.getMessage());
			return "Failed to attach execution logs.";
		}
	}

	/**
	 * Converts the given log file to a zip file.
	 *
	 * @param logFile the log file to be converted to zip
	 * @return the zip file containing the compressed log file
	 * @throws TDKServiceException if an I/O error occurs during the conversion
	 *                             process
	 */
	private File convertToZip(File logFile) {
		File zipFile = new File("execution_log.zip");

		try (FileOutputStream fos = new FileOutputStream(zipFile);
				ZipOutputStream zipOut = new ZipOutputStream(fos);
				FileInputStream fis = new FileInputStream(logFile)) {

			ZipEntry zipEntry = new ZipEntry(logFile.getName());
			zipOut.putNextEntry(zipEntry);

			byte[] bytes = new byte[1024];
			int length;
			while ((length = fis.read(bytes)) >= 0) {
				zipOut.write(bytes, 0, length);
			}

		} catch (IOException e) {
			LOGGER.error("Error converting log file to zip: {}", e.getMessage());
			throw new TDKServiceException("Failed to convert log file to zip: " + e.getMessage());
		}
		return zipFile;
	}

	/**
	 * Updates a Jira ticket based on the provided TicketUpdateDTO.
	 *
	 * @param ticketUpdateDTO the DTO containing ticket update information
	 * @return a string message indicating the result of the update operation
	 * @throws TDKServiceException if an error occurs while updating the Jira ticket
	 */
	@Override
	public String updateJiraTicket(TicketUpdateDTO ticketUpdateDTO) {
		LOGGER.info("Updating Jira ticket for ticket update DTO: {}", ticketUpdateDTO);
		StringBuilder responseString = new StringBuilder();
		try {
			String configFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
					+ Constants.TM_CONFIG_FILE;
			String baseUrl = commonService.getConfigProperty(new File(configFilePath), Constants.TICKET_HANDLER_URL);
			String apiUrl = baseUrl + Constants.UPDATE_API_ENDPOINT;

			ResponseEntity<String> response = httpService.sendPostRequest(apiUrl, ticketUpdateDTO, null);
			if (response.getStatusCode().equals(HttpStatus.OK)) {
				LOGGER.info("Ticket updated successfully");
				responseString.append("Ticket updated successfully.");
			} else {
				LOGGER.error("Failed to update ticket: {}", response.getBody());
				return "Failed to update ticket";
			}

			String attachUrl = baseUrl + Constants.ATTACHMENT_API_ENDPOINT;
			if (ticketUpdateDTO.isExecutionLogNeeded()) {
				String execLogMessage = attachExecutionLogs(attachUrl, ticketUpdateDTO.getExecutionResultId(),
						ticketUpdateDTO.getTicketNumber(), ticketUpdateDTO.getUser(), ticketUpdateDTO.getPassword());
				responseString.append(execLogMessage);
			}

			if (ticketUpdateDTO.isDeviceLogNeeded()) {
				String deviceLogMessage = attachDeviceLogs(attachUrl, ticketUpdateDTO.getExecutionResultId(),
						ticketUpdateDTO.getTicketNumber(), ticketUpdateDTO.getUser(), ticketUpdateDTO.getPassword());
				responseString.append(deviceLogMessage);
			}

		} catch (Exception e) {
			LOGGER.error("Error occurred while updating Jira ticket: {}", e.getMessage());
			throw new TDKServiceException("Failed to update Jira ticket: " + e.getMessage());
		}
		return responseString.toString();
	}

	/**
	 * Checks if Jira automation is implemented by reading the configuration
	 * property.
	 *
	 * @return true if Jira automation is implemented, false otherwise.
	 */
	@Override
	public boolean isJiraAutomationImplemented() {
		LOGGER.info("Checking if Jira automation is implemented");
		String configFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + Constants.TM_CONFIG_FILE;
		String jiraAutomation = commonService.getConfigProperty(new File(configFilePath), Constants.JIRA_AUTOMATION);
		if (null == jiraAutomation || jiraAutomation.isEmpty()) {
			LOGGER.error("Jira automation not implemented");
			return false;
		}
		LOGGER.info("Jira automation is implemented");
		return Boolean.parseBoolean(jiraAutomation);

	}

	/**
	 * This method is used to get the analysis defect types.
	 *
	 * @return the list of analysis defect types
	 */
	@Override
	public List<String> getAnalysisDefectTypes() {
		// Get list of the Analysis Defect Type
		List<String> analysisDefectTypeList = AnalysisDefectType.getAllValues();
		return analysisDefectTypeList;
	}

	/**
	 * Retrieves a list of RDK versions from the configuration file.
	 *
	 * @param category the category of the project (e.g., RDKV, RDKB)
	 * @return a list of RDK versions if found, otherwise null
	 */
	@Override
	public List<String> getRDKVersion(String category) {
		LOGGER.info("Fetching list of RDK versions");
		String configFilePath = this.getConfigFileBasedOnCategory(category);
		String rdkVersions = commonService.getConfigProperty(new File(configFilePath), Constants.RDK_VERSIONS);
		if (null == rdkVersions || rdkVersions.isEmpty()) {
			LOGGER.error("No RDK versions found in the config file");
			return null;
		}
		List<String> rdkVersionList = List.of(rdkVersions.split(","));
		LOGGER.info("Successfully fetched list of RDK versions");
		return rdkVersionList;
	}
}
