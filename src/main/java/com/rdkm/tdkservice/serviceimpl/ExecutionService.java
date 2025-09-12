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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.rdkm.tdkservice.serviceimpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.dto.ResultDTO;
import com.rdkm.tdkservice.dto.ExecutionByDateDTO;
import com.rdkm.tdkservice.dto.ExecutionDetailsDTO;
import com.rdkm.tdkservice.dto.ExecutionDetailsForHtmlReportDTO;
import com.rdkm.tdkservice.dto.ExecutionDetailsResponseDTO;
import com.rdkm.tdkservice.dto.ExecutionListDTO;
import com.rdkm.tdkservice.dto.ExecutionListResponseDTO;
import com.rdkm.tdkservice.dto.ExecutionMethodResultResponseDTO;
import com.rdkm.tdkservice.dto.ExecutionNameRequestDTO;
import com.rdkm.tdkservice.dto.ExecutionResponseDTO;
import com.rdkm.tdkservice.dto.ExecutionResultDTO;
import com.rdkm.tdkservice.dto.ExecutionResultResponseDTO;
import com.rdkm.tdkservice.dto.ExecutionSearchFilterDTO;
import com.rdkm.tdkservice.dto.ExecutionSummaryResponseDTO;
import com.rdkm.tdkservice.dto.ExecutionTriggerDTO;
import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.enums.DeviceStatus;
import com.rdkm.tdkservice.enums.ExecutionMethodResultStatus;
import com.rdkm.tdkservice.enums.ExecutionOverallResultStatus;
import com.rdkm.tdkservice.enums.ExecutionProgressStatus;
import com.rdkm.tdkservice.enums.ExecutionResultStatus;
import com.rdkm.tdkservice.enums.ExecutionStatus;
import com.rdkm.tdkservice.enums.ExecutionTriggerStatus;
import com.rdkm.tdkservice.enums.ExecutionType;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.exception.UserInputException;
import com.rdkm.tdkservice.model.Device;
import com.rdkm.tdkservice.model.DeviceType;
import com.rdkm.tdkservice.model.Execution;
import com.rdkm.tdkservice.model.ExecutionDevice;
import com.rdkm.tdkservice.model.ExecutionMethodResult;
import com.rdkm.tdkservice.model.ExecutionResult;
import com.rdkm.tdkservice.model.ExecutionResultAnalysis;
import com.rdkm.tdkservice.model.Module;
import com.rdkm.tdkservice.model.Oem;
import com.rdkm.tdkservice.model.Script;
import com.rdkm.tdkservice.model.ScriptTestSuite;
import com.rdkm.tdkservice.model.Soc;
import com.rdkm.tdkservice.model.TestSuite;
import com.rdkm.tdkservice.model.User;
import com.rdkm.tdkservice.repository.DeviceRepositroy;
import com.rdkm.tdkservice.repository.ExecutionDeviceRepository;
import com.rdkm.tdkservice.repository.ExecutionMethodResultRepository;
import com.rdkm.tdkservice.repository.ExecutionRepository;
import com.rdkm.tdkservice.repository.ExecutionResultAnalysisRepository;
import com.rdkm.tdkservice.repository.ExecutionResultRepository;
import com.rdkm.tdkservice.repository.ScriptRepository;
import com.rdkm.tdkservice.repository.TestSuiteRepository;
import com.rdkm.tdkservice.repository.UserRepository;
import com.rdkm.tdkservice.service.IExecutionService;
import com.rdkm.tdkservice.service.IScriptService;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.Utils;

/*
 * The ExecutionService class is used to handle the execution related operations
 */
@Service
public class ExecutionService implements IExecutionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionService.class);

	@Autowired
	private DeviceRepositroy deviceRepository;

	@Autowired
	private ExecutionRepository executionRepository;

	@Autowired
	private ScriptRepository scriptRepository;

	@Autowired
	private ExecutionResultRepository executionResultRepository;

	@Autowired
	private ExecutionMethodResultRepository executionMethodResultRepository;

	@Autowired
	private ExecutionResultAnalysisRepository executionResultAnalysisRepository;

	@Autowired
	private ExecutionAsyncService executionAsyncService;

	@Autowired
	private TestSuiteRepository testSuiteRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CommonService commonService;

	@Autowired
	private DeviceStatusService deviceStatusService;

	@Autowired
	private ExecutionDeviceRepository executionDeviceRepository;

	@Autowired
	private FileTransferService fileTransferService;

	@Autowired
	private IScriptService scriptService;

	@Autowired
	private AppConfig appConfig;

	/**
	 * This method is used to trigger the execution of the scripts or test suite
	 * based on the input provided.
	 *
	 * @param executionTriggerDTO - the execution trigger DTO
	 * @return ExecutionResponseDTO - the execution response
	 */
	@Override
	public ExecutionResponseDTO triggerExecution(ExecutionTriggerDTO executionTriggerDTO) {
		LOGGER.info("Triggering execution with details: {}", executionTriggerDTO);
		// Checks if the trigger request is valid
		this.checkValidTriggerRequest(executionTriggerDTO);
		ExecutionResponseDTO response = this.startExecution(executionTriggerDTO);
		return response;
	}

	/**
	 * The execution part with out the validation to be used in scheduler as well as
	 * in triggered execution
	 * 
	 * @param executionTriggerDTO
	 * @return ExecutionResponseDTO
	 */
	public ExecutionResponseDTO startExecution(ExecutionTriggerDTO executionTriggerDTO) {
		// Prepare the response string
		StringBuilder responseLogs = new StringBuilder();

		// Get the devices from the request
		List<String> devicesFromRequest = executionTriggerDTO.getDeviceList();
		List<Device> deviceList = this.getValidDeviceList(devicesFromRequest, responseLogs);

		ExecutionResponseDTO response = null;

		ExecutionDetailsDTO executionDetailsDTO = null;
		List<String> scriptsListFromRequest = executionTriggerDTO.getScriptList();
		List<String> testSuiteListFromRequest = executionTriggerDTO.getTestSuite();

		// Check if the devices are available for execution
		List<Device> freeDevices = this.filterFreeDevices(deviceList, responseLogs);
		if (freeDevices.isEmpty()) {
			if (deviceList.size() > 1) {
				LOGGER.error("No free devices found in the request");
			} else {
				LOGGER.error("No free device found in the request");
			}
			LOGGER.error("No valid devices found for execution");
			ExecutionResponseDTO executionResponseDTO = createExecutionResponseDTO(responseLogs.toString(),
					ExecutionTriggerStatus.NOTTRIGGERED, null);
			return executionResponseDTO;
		}

		// Script Execution - Single and Multiple
		if (null != scriptsListFromRequest && !scriptsListFromRequest.isEmpty()) {
			LOGGER.info("The request came for  script execution");
			List<Script> scriptsList = this.getValidScriptList(scriptsListFromRequest, responseLogs);
			executionDetailsDTO = this.convertTriggerDTOToExecutionDetailsDTO(executionTriggerDTO, freeDevices,
					scriptsList, null);
			if (scriptsList.size() > 1) {
				LOGGER.info("The request came for multiple script execution");
				response = multiScriptExcecution(executionDetailsDTO);
			} else if (scriptsList.size() == 1) {
				LOGGER.info("The request came for single script execution");
				response = singleScriptExecution(executionDetailsDTO);
			}
		}

		// Test suite Execution - Single and Multiple
		if (null != testSuiteListFromRequest && !testSuiteListFromRequest.isEmpty()) {
			LOGGER.info("The request came for  Test suite execution");
			List<TestSuite> testSuiteList = this.getValidTestSuiteList(testSuiteListFromRequest, responseLogs);
			executionDetailsDTO = this.convertTriggerDTOToExecutionDetailsDTO(executionTriggerDTO, deviceList, null,
					testSuiteList);
			if (testSuiteList.size() > 1) {
				LOGGER.info("The request came for multi testsuite execution");
				response = multiTestSuiteExecution(executionDetailsDTO);
			} else if (testSuiteList.size() == 1) {
				LOGGER.info("The request came for single testsuite execution");
				response = testSuiteExecution(executionDetailsDTO);

			}

		}
		response = this.appendResponseLogs(response, responseLogs);

		return response;

	}

	/**
	 * This method is used to trigger the execution of a single script
	 * 
	 * @param executionDetailsDTO - the execution details DTO
	 * @return ExecutionResponseDTO - the execution response
	 */
	private ExecutionResponseDTO singleScriptExecution(ExecutionDetailsDTO executionDetailsDTO) {
		LOGGER.info("Executing single script: {}", executionDetailsDTO.getScriptList().get(0).getName());
		StringBuilder responseLogs = new StringBuilder();

		Script script = executionDetailsDTO.getScriptList().get(0);

		List<Device> deviceList = executionDetailsDTO.getDeviceList();
		List<Device> validDevices = getValidDevicesForScriptbasedOnCategory(deviceList, script, responseLogs);

		if (validDevices.isEmpty()) {
			responseLogs.append("No valid devices found for the script as categories are different: ")
					.append(script.getName()).append(". So not triggering execution").append("\n");
			ExecutionResponseDTO executionResponseDTO = createExecutionResponseDTO(responseLogs.toString(),
					ExecutionTriggerStatus.NOTTRIGGERED, null);
			return executionResponseDTO;
		}

		if (isScriptMarkedToBeSkipped(script)) {
			responseLogs.append("Script: ").append(script.getName())
					.append(" is marked to be skipped as it is obsolete. So execution is not triggered\n");
			ExecutionResponseDTO executionResponseDTO = createExecutionResponseDTO(responseLogs.toString(),
					ExecutionTriggerStatus.NOTTRIGGERED, null);
			return executionResponseDTO;
		}
		List<String> executionUrls = new ArrayList<>();
		boolean isScriptExecutionTriggered = false;
		for (Device device : deviceList) {
			if (!validateScriptDeviceDeviceType(device, script)) {
				LOGGER.error("Device: {} and Script: {} combination is invalid\n", device.getName(), script.getName());
				responseLogs.append("Device: " + device.getName() + " and Script: " + script.getName()
						+ " combination is invalid due to different devicetypes, So not triggering execution in the device\n");
				continue;
			}

			if (!checkDeviceAvailabilityForExecution(device)) {
				LOGGER.error("Device: {} is not available for execution in it\n", device.getName());
				responseLogs.append("Device: " + device.getName() + " is not available for execution\n");
				continue;
			}

			isScriptExecutionTriggered = true;
			responseLogs.append("Executing script: ").append(script.getName()).append(" on device: ")
					.append(device.getName()).append(".");
			String executionName = getExecutionName(executionDetailsDTO.getExecutionName(), device,
					executionDetailsDTO.getTestType());
			LOGGER.info("Execution script on " + script.getName() + "the device" + device.getName());
			executionAsyncService.prepareAndExecuteSingleScript(device, script, executionDetailsDTO.getUser(),
					executionName, executionDetailsDTO.getRepeatCount(), executionDetailsDTO.isRerunOnFailure(),
					executionDetailsDTO.isDeviceLogsNeeded(), executionDetailsDTO.isPerformanceLogsNeeded(),
					executionDetailsDTO.isDiagnosticLogsNeeded(), executionDetailsDTO.getTestType(),
					executionDetailsDTO.getCallBackUrl(), executionDetailsDTO.getImageVersion());
			LOGGER.info(" Asynchronous Execution of script on " + script.getName() + "the device" + device.getName()
					+ " triggered");
			executionUrls
					.add(appConfig.getBaseURL() + "/execution/getExecutionResultJson?executionName=" + executionName);

		}
		// If atleast one execution is triggered in one box, then return the
		// response as triggered
		if (isScriptExecutionTriggered) {
			LOGGER.info("Script execution is triggered");
			ExecutionResponseDTO executionResponseDTO = this.createExecutionResponseDTO(responseLogs.toString(),
					ExecutionTriggerStatus.TRIGGERED, executionUrls);
			return executionResponseDTO;
		} else {
			LOGGER.info("Script execution is not triggered");
			ExecutionResponseDTO executionResponseDTO = this.createExecutionResponseDTO(responseLogs.toString(),
					ExecutionTriggerStatus.NOTTRIGGERED, null);
			return executionResponseDTO;
		}

	}

	/**
	 * This method is used to get the execution name based on the device and the
	 * execution name provided in the request
	 * 
	 * @param executionName - the execution name
	 * @param device        - the device
	 * @param testType      - the test type
	 * @return String - the execution name
	 */
	private String getExecutionName(String executionName, Device device, String testType) {
		String baseExecutionName = executionName;
		if (!Utils.isEmpty(executionName) && !executionName.contains(Constants.MULTIPLE_KEY_WORD)) {
			baseExecutionName = executionName;
		} else {
			baseExecutionName = this.getExecutionNameFromDeviceAndTestType(device, testType);
		}
		return baseExecutionName;
	}

	/**
	 * This method is used to trigger the execution of multiple scripts
	 * 
	 * @param executionDetailsDTO - the execution details DTO
	 * @return ExecutionResponseDTO - the execution response
	 */
	private ExecutionResponseDTO multiScriptExcecution(ExecutionDetailsDTO executionDetailsDTO) {
		LOGGER.info("Starting multiScript excecution");

		StringBuilder responseLogs = new StringBuilder();
		boolean isExecutionTriggered = false;
		String executionName = null;
		List<String> executionUrls = new ArrayList<>();
		for (Device device : executionDetailsDTO.getDeviceList()) {
			if (!checkDeviceAvailabilityForExecution(device)) {
				LOGGER.error("Device: {} is not available for execution\n",
						device.getName());
				responseLogs.append("Device: " + device.getName()
						+ " is not available for execution, So not triggering excution in it\n");
				break;
			}
			executionName = getExecutionName(executionDetailsDTO.getExecutionName(), device,
					executionDetailsDTO.getTestType());
			isExecutionTriggered = true;
			responseLogs.append("MultiScript execution triggered on device :").append(device.getName()).append("\n");
			executionAsyncService.prepareAndExecuteMultiScript(device, executionDetailsDTO.getScriptList(),
					executionDetailsDTO.getUser(), executionName, executionDetailsDTO.getCategory(), null,
					executionDetailsDTO.getRepeatCount(), executionDetailsDTO.isRerunOnFailure(),
					executionDetailsDTO.isDeviceLogsNeeded(), executionDetailsDTO.isDiagnosticLogsNeeded(),
					executionDetailsDTO.isPerformanceLogsNeeded(), executionDetailsDTO.isIndividualRepeatExecution(),
					executionDetailsDTO.getTestType(), executionDetailsDTO.getCallBackUrl(),
					executionDetailsDTO.getImageVersion());
			executionUrls
					.add(appConfig.getBaseURL() + "/execution/getExecutionResultJson?executionName=" + executionName);
		}

		// If atleast one script execution is triggered in one box, then return the
		// response as triggered
		if (isExecutionTriggered) {
			LOGGER.info("Execution is triggered");
			ExecutionResponseDTO executionResponseDTO = this.createExecutionResponseDTO(responseLogs.toString(),
					ExecutionTriggerStatus.TRIGGERED, executionUrls);
			return executionResponseDTO;
		} else {
			LOGGER.info("Execution is  not triggered");
			ExecutionResponseDTO executionResponseDTO = this.createExecutionResponseDTO(responseLogs.toString(),
					ExecutionTriggerStatus.NOTTRIGGERED, null);
			return executionResponseDTO;
		}

	}

	/**
	 * Executes the test suite based on the provided execution details.
	 *
	 * @param executionDetailsDTO the details of the execution including test suite,
	 *                            devices, user, etc.
	 * @return ExecutionResponseDTO containing the response logs and execution
	 *         status.
	 *
	 *         This method performs the following steps: 1. Logs the start of the
	 *         test suite execution. 2. Retrieves the list of scripts from the test
	 *         suite. 3. Iterates over each device in the execution details: a.
	 *         Checks if the device is available for execution. b. If the device is
	 *         not available, logs an error and appends a message to the response
	 *         logs. c. If the device is available, prepares and executes the
	 *         scripts asynchronously on the device. 4. Creates and returns an
	 *         ExecutionResponseDTO with the accumulated response logs and a
	 *         triggered status.
	 */
	private ExecutionResponseDTO testSuiteExecution(ExecutionDetailsDTO executionDetailsDTO) {
		LOGGER.info("Executing test suite: {}", executionDetailsDTO.getTestSuite().get(0).getName());
		// Get script list from Test suite
		TestSuite testSuite = executionDetailsDTO.getTestSuite().get(0);
		List<ScriptTestSuite> scriptList = testSuite.getScriptTestSuite();
		List<Script> scripts = new ArrayList<>();
		for (ScriptTestSuite scriptTestSuite : scriptList) {
			scripts.add(scriptTestSuite.getScript());
		}

		boolean isExecutionTriggered = false;

		StringBuilder responseLogs = new StringBuilder();
		List<String> executionUrls = new ArrayList<>();
		for (Device device : executionDetailsDTO.getDeviceList()) {
			if (!checkDeviceAvailabilityForExecution(device)) {
				LOGGER.error("Device: {} is not available for execution\n",
						device.getName());
				responseLogs.append("Device: " + device.getName()
						+ " is not available for execution, So not triggering excution in it\n");
				continue;
			}
			isExecutionTriggered = true;
String executionName = getExecutionName(executionDetailsDTO.getExecutionName(), device,
					executionDetailsDTO.getTestType());
			responseLogs.append("TestSuite execution on device: ").append(device.getName()).append(".");
			executionAsyncService.prepareAndExecuteMultiScript(device, scripts, executionDetailsDTO.getUser(),
					executionName, executionDetailsDTO.getCategory(), testSuite.getName(),
					executionDetailsDTO.getRepeatCount(), executionDetailsDTO.isRerunOnFailure(),
					executionDetailsDTO.isDeviceLogsNeeded(), executionDetailsDTO.isDiagnosticLogsNeeded(),
					executionDetailsDTO.isDiagnosticLogsNeeded(), executionDetailsDTO.isIndividualRepeatExecution(),
					executionDetailsDTO.getTestType(), executionDetailsDTO.getCallBackUrl(),
					executionDetailsDTO.getImageVersion());
			executionUrls
					.add(appConfig.getBaseURL() + "/execution/getExecutionResultJson?executionName=" + executionName);

		}

		// If atleast one execution is triggered in one box, then return the
		// response as triggered
		if (isExecutionTriggered) {
			LOGGER.info("Execution is triggered");
			ExecutionResponseDTO executionResponseDTO = this.createExecutionResponseDTO(responseLogs.toString(),
					ExecutionTriggerStatus.TRIGGERED, executionUrls);
			return executionResponseDTO;
		} else {
			LOGGER.info("Execution is  not triggered");
			ExecutionResponseDTO executionResponseDTO = this.createExecutionResponseDTO(responseLogs.toString(),
					ExecutionTriggerStatus.NOTTRIGGERED, null);
			return executionResponseDTO;
		}

	}

	/**
	 * Executes multiple test suites on the provided devices.
	 * 
	 * @param executionDetailsDTO the details of the execution, including test
	 *                            suites, devices, user, and other parameters.
	 * @return an ExecutionResponseDTO containing the response logs and the status
	 *         of the execution trigger.
	 */
	private ExecutionResponseDTO multiTestSuiteExecution(ExecutionDetailsDTO executionDetailsDTO) {
		LOGGER.info("Starting multiTestSuite excecution");
		List<TestSuite> testSuiteList = executionDetailsDTO.getTestSuite();
		StringBuilder responseLogs = new StringBuilder();
		List<Script> scriptSet = new ArrayList<>();
		List<ScriptTestSuite> scriptList = new ArrayList<>();
		for (TestSuite testSuite : testSuiteList) {
			scriptList.addAll(testSuite.getScriptTestSuite());
		}
		for (ScriptTestSuite scriptTestSuite : scriptList) {
			if (!scriptSet.contains(scriptTestSuite.getScript()))
				scriptSet.add(scriptTestSuite.getScript());
		}
		boolean isExecutionTriggered = false;
		List<String> executionUrls = new ArrayList<>();
		for (Device device : executionDetailsDTO.getDeviceList()) {
			if (!checkDeviceAvailabilityForExecution(device)) {
				LOGGER.error("Device: {} is not available for execution\n",
						device.getName());
				responseLogs.append("Device: " + device.getName()
						+ " is not available for execution, So not triggering excution in it\n");
				break;
			}
			isExecutionTriggered = true;
			String executionName = getExecutionName(executionDetailsDTO.getExecutionName(), device,
					executionDetailsDTO.getTestType());
			responseLogs.append("Multitestsuite execution on device: ").append(device.getName()).append(".");
			executionAsyncService.prepareAndExecuteMultiScript(device, scriptSet, executionDetailsDTO.getUser(),
					executionName, executionDetailsDTO.getCategory(), Constants.MULTI_TEST_SUITE,
					executionDetailsDTO.getRepeatCount(), executionDetailsDTO.isRerunOnFailure(),
					executionDetailsDTO.isDeviceLogsNeeded(), executionDetailsDTO.isDiagnosticLogsNeeded(),
					executionDetailsDTO.isPerformanceLogsNeeded(), executionDetailsDTO.isIndividualRepeatExecution(),
					executionDetailsDTO.getTestType(), executionDetailsDTO.getCallBackUrl(),
					executionDetailsDTO.getImageVersion());
			executionUrls
					.add(appConfig.getBaseURL() + "/execution/getExecutionResultJson?executionName=" + executionName);
		}

		// If atleast one execution is triggered in one box, then return the
		// response as triggered

		if (isExecutionTriggered) {
			LOGGER.info("Execution is triggered");
			ExecutionResponseDTO executionResponseDTO = this.createExecutionResponseDTO(responseLogs.toString(),
					ExecutionTriggerStatus.TRIGGERED, executionUrls);
			return executionResponseDTO;
		} else {
			LOGGER.info("Execution is  not triggered");
			ExecutionResponseDTO executionResponseDTO = this.createExecutionResponseDTO(responseLogs.toString(),
					ExecutionTriggerStatus.NOTTRIGGERED, null);
			return executionResponseDTO;
		}

	}

	/**
	 * This method is used to check if the device is available for execution
	 * 
	 * @param device - the device
	 * @return boolean - true if the device is available for execution, false
	 *         otherwise
	 */
	private boolean checkDeviceAvailabilityForExecution(Device device) {
		// Check if the device is available for execution
		// If the device is not available, then return false
		// If the device is available, then return true
		DeviceStatus deviceStatus = deviceStatusService.fetchDeviceStatus(device);
		if (DeviceStatus.FREE.equals(deviceStatus)) {
			return true;
		}
		return false;
	}

	/**
	 * This method is used to append the response logs to the execution response DTO
	 * 
	 * @param response       - the execution response DTO
	 * @param responseString - the response string
	 * @return ExecutionResponseDTO - the execution response DTO
	 */

	private ExecutionResponseDTO appendResponseLogs(ExecutionResponseDTO response, StringBuilder responseLogs) {

		ExecutionResponseDTO executionResponseDTO = response;
		if (!Utils.isEmpty(responseLogs.toString())) {
			String executionResponseLog = executionResponseDTO.getMessage();
			String executionResponseAndOtherResponseLog = responseLogs + executionResponseLog;
			executionResponseDTO.setMessage(executionResponseAndOtherResponseLog);
		}
		return executionResponseDTO;

	}

	/**
	 * This method is used to create the execution response DTO
	 * 
	 * @param message                - the message
	 * @param executionTriggerStatus - the execution trigger status
	 * @return ExecutionResponseDTO - the execution response DTO
	 */

	private ExecutionResponseDTO createExecutionResponseDTO(String message,
			ExecutionTriggerStatus executionTriggerStatus, List<String> executionUrls) {
		ExecutionResponseDTO executionResponseDTO = new ExecutionResponseDTO();
		executionResponseDTO.setMessage(message);
		executionResponseDTO.setExecutionTriggerStatus(executionTriggerStatus);
		executionResponseDTO.setExecResultDetailsUrl(executionUrls);
		return executionResponseDTO;
	}

	/**
	 * This method is used to get the valid devices for the script based on the
	 * category
	 * 
	 * @param deviceList     - the device list
	 * @param script         - the script
	 * @param responseString - the response string
	 * @return List<Device> - the valid devices
	 */
	private List<Device> getValidDevicesForScriptbasedOnCategory(List<Device> deviceList, Script script,
			StringBuilder responseString) {
		LOGGER.info("Getting valid devices for script based on category");
		List<Device> validDevices = new ArrayList<>();
		for (Device device : deviceList) {
			if (commonService.vaidateScriptDeviceCategory(device, script)) {
				validDevices.add(device);
			} else {
				LOGGER.error("Device: {} and Script: {} combination is invalid and belongs to different category\n",
						device.getName(), script.getName());
			}
		}
		return validDevices;
	}

	/**
	 * This method is used to validate the script and device type
	 * 
	 * @param device - the device
	 * @param script - the script
	 * @return boolean - true if the script and device type is valid, false
	 *         otherwise
	 */

	private boolean validateScriptDeviceDeviceType(Device device, Script script) {
		LOGGER.info("Validating script and device type");
		List<DeviceType> deviceTypes = script.getDeviceTypes();
		if (deviceTypes.isEmpty()) {
			LOGGER.info("Script has no device types");
			return true;
		} else {
			for (DeviceType deviceType : deviceTypes) {
				if (deviceType.equals(device.getDeviceType())) {
					LOGGER.info("Device: {} and Script: {} combination is valid\n", device.getName(), script.getName());
					return true;
				}
			}
		}
		LOGGER.error("Device: {} and Script: {} combination is invalid and belongs to different devicetypes\n",
				device.getName(), script.getName());
		return false;

	}

	/**
	 * This method is used to check if the script is marked to be skipped
	 * 
	 * @param script - the script
	 * @return boolean - true if the script is marked to be skipped, false otherwise
	 */

	private boolean isScriptMarkedToBeSkipped(Script script) {
		if (script.isSkipExecution()) {
			LOGGER.info("Script: {} is marked to be skipped\n", script.getName());
			return true;
		}
		LOGGER.info("Script: {} is not marked to be skipped\n", script.getName());
		return false;
	}

	/**
	 * Retrieves a list of valid scripts based on the provided script names.
	 * 
	 * @param scripts        A list of script names to be validated and retrieved.
	 * @param responseString A StringBuilder to append error messages if scripts are
	 *                       not found.
	 * @return A list of valid Script objects.
	 * @throws ResourceNotFoundException if no valid scripts are found in the
	 *                                   request.
	 */
	private List<Script> getValidScriptList(List<String> scripts, StringBuilder responseString) {
		List<Script> scriptList = new ArrayList<>();
		for (String scriptName : scripts) {
			Script script = scriptRepository.findByName(scriptName);
			// TODO : Check if file exists using a scriptService method
			if (script == null) {
				LOGGER.error("Script not found with name: {}", scriptName);
				responseString.append("Script: " + scriptName + " not found.)");
			} else {
				scriptList.add(script);
			}
		}
		if (scriptList.isEmpty()) {
			LOGGER.error("No valid/available scripts found in the request");
			throw new ResourceNotFoundException("Script/s in the request", scripts.toString());
		}
		return scriptList;

	}

	/**
	 * This method is used to convert the trigger DTO to execution details DTO
	 * 
	 * @param executionTriggerDTO - the execution trigger DTO
	 * @param deviceList          - the device list
	 * @param scriptList          - the script list
	 * @param testSuite           - the test suite
	 * @return ExecutionDetailsDTO - the execution details DTO
	 */
	private ExecutionDetailsDTO convertTriggerDTOToExecutionDetailsDTO(ExecutionTriggerDTO executionTriggerDTO,
			List<Device> deviceList, List<Script> scriptList, List<TestSuite> testSuite) {
		LOGGER.debug("Converting ExecutionTriggerDTO to ExecutionDetailsDTO");
		ExecutionDetailsDTO executionDetailsDTO = new ExecutionDetailsDTO();
		executionDetailsDTO.setCategory(executionTriggerDTO.getCategory());
		executionDetailsDTO.setDeviceList(deviceList);
		executionDetailsDTO.setExecutionName(executionTriggerDTO.getExecutionName());
		executionDetailsDTO.setTestType(executionTriggerDTO.getTestType());
		executionDetailsDTO.setScriptList(scriptList);

		executionDetailsDTO.setTestSuite(testSuite);
		executionDetailsDTO.setRepeatCount(executionTriggerDTO.getRepeatCount());
		executionDetailsDTO.setRerunOnFailure(executionTriggerDTO.isRerunOnFailure());
		if (executionTriggerDTO.getUser() != null) {
			User user = userRepository.findByUsername(executionTriggerDTO.getUser());
			if (null != user) {
				executionDetailsDTO.setUser(user.getUsername());
			}

		}
		executionDetailsDTO.setCallBackUrl(executionTriggerDTO.getCiCallBackUrl());
		executionDetailsDTO.setImageVersion(executionTriggerDTO.getCiImageVersion());
		executionDetailsDTO.setIndividualRepeatExecution(executionTriggerDTO.isIndividualRepeatExecution());
		executionDetailsDTO.setDeviceLogsNeeded(executionTriggerDTO.isDeviceLogsNeeded());
		executionDetailsDTO.setPerformanceLogsNeeded(executionTriggerDTO.isPerformanceLogsNeeded());
		executionDetailsDTO.setDiagnosticLogsNeeded(executionTriggerDTO.isDiagnosticLogsNeeded());
		LOGGER.debug("Converted ExecutionTriggerDTO to ExecutionDetailsDTO");
		return executionDetailsDTO;

	}

	/**
	 * Retrieves a list of valid TestSuite objects based on the provided list of
	 * test suite names.
	 * 
	 * @param testSuiteListFromRequest A list of test suite names to be validated
	 *                                 and retrieved.
	 * @param responseLogs             A StringBuilder object to append logs for any
	 *                                 test suites that are not found.
	 * @return A list of valid TestSuite objects.
	 * @throws ResourceNotFoundException If no valid test suites are found in the
	 *                                   provided list.
	 */
	private List<TestSuite> getValidTestSuiteList(List<String> testSuiteListFromRequest, StringBuilder responseLogs) {
		LOGGER.info("Getting valid test suites from the request");
		List<TestSuite> testSuiteList = new ArrayList<>();
		for (String testSuiteName : testSuiteListFromRequest) {
			TestSuite testSuite = testSuiteRepository.findByName(testSuiteName);
			if (testSuite == null) {
				LOGGER.error("TestSuite not found with name: {}", testSuiteName);
				responseLogs.append("TestSuite: " + testSuiteName + " not found.");
			} else {
				testSuiteList.add(testSuite);
			}
		}
		if (testSuiteList.isEmpty()) {
			LOGGER.error("No valid/available Test Suites found in the request");
			throw new ResourceNotFoundException("Test Suites in the request", testSuiteListFromRequest.toString());
		}
		return testSuiteList;
	}

	/**
	 * This method is used to get the valid device list, if the device is not found
	 * then it will append the response string with the device name not found and if
	 * no devices found then it will throw ResourceNotFoundException
	 * 
	 * @param devices        List of devices from the request
	 * @param responseString StringBuilder to append the response
	 * @return List of valid devices
	 */
	private List<Device> getValidDeviceList(List<String> devices, StringBuilder responseString) {
		LOGGER.info("Getting valid devices from the request");
		List<Device> deviceList = new ArrayList<>();
		for (String deviceName : devices) {
			Device device = deviceRepository.findByName(deviceName);
			if (device == null) {
				LOGGER.error("Device not found with name: {}", deviceName);
				responseString.append("Device: " + deviceName + " not found.\n");
			} else {
				deviceList.add(device);
			}
		}
		if (deviceList.isEmpty()) {
			throw new ResourceNotFoundException("Device/s in the request", devices.toString());
		}
		return deviceList;
	}

	/**
	 * This method is used to get the execution name based on the device
	 * 
	 * @param deviceList - the device list
	 * @return String - the execution name
	 */
	private List<Device> filterFreeDevices(List<Device> deviceList, StringBuilder responseString) {
		LOGGER.info("Filtering devices based on the availability");
		List<Device> availableDevices = new ArrayList<>();
		for (Device device : deviceList) {
			if (device.getDeviceStatus().equals(DeviceStatus.FREE)) {
				availableDevices.add(device);
			} else {
				responseString.append("Device: " + device.getName() + " is not available for execution\n");
			}
		}
		if (availableDevices.isEmpty()) {
			LOGGER.error("Device not available for execution");
			responseString.append("No devices available for execution\n");

		}
		return availableDevices;
	}

	/**
	 * Validates the execution trigger request.
	 *
	 * This method performs the following validations: 1. Ensures that either
	 * scripts or test suite is provided in the request. 2. Checks if the device
	 * list is not empty. 3. Ensures that both script list and test suite are not
	 * provided simultaneously. 4. Verifies that the execution name, if provided, is
	 * unique.
	 *
	 * @param executionTriggerDTO the execution trigger request data transfer object
	 * @throws UserInputException if any validation fails with appropriate error
	 *                            message
	 */
	public void checkValidTriggerRequest(ExecutionTriggerDTO executionTriggerDTO) {
		// Checking for scripts or test suite is null or empty
		if ((executionTriggerDTO.getScriptList() == null || executionTriggerDTO.getScriptList().isEmpty())
				&& (executionTriggerDTO.getTestSuite() == null || executionTriggerDTO.getTestSuite().isEmpty())) {
			LOGGER.error("Either scripts or test suite must be provided in the request");
			throw new UserInputException("Either scripts or test suite must be provided in the request");
		}

		// Checking for devices
		if (!executionTriggerDTO.getDeviceList().isEmpty() && executionTriggerDTO.getDeviceList().size() > 0) {
		} else {
			LOGGER.error("No devices found in the request");
			throw new UserInputException("Devices not found in the request");
		}

		// Both script list and test suite cannot be non empty
		if (!executionTriggerDTO.getScriptList().isEmpty() && !executionTriggerDTO.getTestSuite().isEmpty()) {
			LOGGER.error("Both scripts and test suite cannot be provided in the  same request");
			throw new UserInputException("Both scripts and test suite cannot be provided in the  same request");
		}

		// Execution name provided should not be already available
		if (!Utils.isEmpty(executionTriggerDTO.getExecutionName())) {
			boolean isExecutionNameExists = executionRepository.existsByName(executionTriggerDTO.getExecutionName());
			if (isExecutionNameExists) {
				LOGGER.error("The Execution name already exists, execution name should be unique");
				throw new UserInputException("The Execution name already exists, execution name should be unique");
			}
		}
	}

	/**
	 * This method is used to save the execution result
	 * 
	 * @param execId         - the execution ID
	 * @param resultData     - the result data
	 * @param execResult     - the execution result
	 * @param expectedResult - the expected result
	 * @param resultStatus   - the result status
	 * @param testCaseName   - the test case name
	 * @param execDevice     - the execution device
	 * @return boolean - true if the execution result is saved, false otherwise
	 */
	@Override
	@Transactional
	public boolean saveExecutionResult(String execId, String resultData, String execResult, String expectedResult,
			String resultStatus, String testCaseName, String execDevice) {
		LOGGER.info(
				"Saving execution result for execId: {}, resultData: {}, execResultID: {}, expectedResult: {}, resultStatus: {}, testCaseName: {}, execDevice: {}",
				execId, resultData, execResult, expectedResult, resultStatus, testCaseName, execDevice);

		// Check if the execution ID is empty and throw an exception
		if (Utils.isEmpty(execId)) {
			LOGGER.error("Execution ID is empty");
			throw new UserInputException("Execution ID is empty");
		}

		// Check if the execution data exists and get the data from DB
		Execution execution = executionRepository.findById(UUID.fromString(execId)).orElseThrow(() -> {
			LOGGER.error("Execution not found with id: {}", execId);
			return new ResourceNotFoundException("Execution ID ", execId);
		});
		String executionResultID = execResult;

		if (!Utils.isEmpty(executionResultID)) {

			// Check if the executionResult ID is empty and throw an exception
			ExecutionResult executionResult = executionResultRepository.findById(UUID.fromString(executionResultID))
					.orElseThrow(() -> {
						LOGGER.error("Execution not found with id: {}", execResult);
						return new ResourceNotFoundException("Execution ID ", execResult);
					});

			// Save the execution method result or test case result
			ExecutionMethodResult executionMethodResult = new ExecutionMethodResult();
			String actualResult = resultData;
			if (resultStatus.equals(Constants.STATUS_NONE) || Utils.isEmpty(resultStatus)) {
				executionMethodResult.setMethodResult(ExecutionMethodResultStatus.valueOf(actualResult));
			} else {
				executionMethodResult.setExecutionResult(executionResult);
				executionMethodResult.setExpectedResult(ExecutionMethodResultStatus.valueOf(expectedResult));
				executionMethodResult.setActualResult(ExecutionMethodResultStatus.valueOf(actualResult));
				executionMethodResult.setMethodResult(ExecutionMethodResultStatus.valueOf(resultStatus));
			}
			executionMethodResult.setFunctionName(testCaseName);
			executionMethodResultRepository.save(executionMethodResult);

			// If the result in Execution and ExecutionResult is already failure from
			// anyone of the test case, then don't update the status. Because the status is
			// already failed
			if ((null == executionResult.getResult())
					|| (!executionResult.getResult().equals(ExecutionResultStatus.FAILURE))) {
				executionResult.setResult(ExecutionResultStatus.valueOf(resultStatus));
				executionResultRepository.save(executionResult);
				if ((null == execution.getResult())
						|| !(execution.getResult().equals(ExecutionOverallResultStatus.FAILURE))) {
					execution.setResult(ExecutionOverallResultStatus.valueOf(resultStatus));
					executionRepository.save(execution);
				}

			}
			executionResultRepository.save(executionResult);

		} else {
			// If the executionResult ID is not passed and executionID is passed, mark the
			// status as failed

			execution.setResult(ExecutionOverallResultStatus.FAILURE);
			executionRepository.save(execution);

		}
		return true;
	}

	/**
	 * This method is used to trigger the execution of the scripts or test suite
	 * based on the input provided.
	 *
	 * @param execId     - the execution trigger DTO
	 * @param statusData - the execution trigger DTO
	 * @param execDevice - the execution trigger DTO
	 * @param execResult - the execution trigger DTO
	 */
	@Override
	@Transactional
	public boolean saveLoadModuleStatus(String execId, String statusData, String execDevice, String execResult) {
		Logger LOGGER = LoggerFactory.getLogger(ExecutionService.class);
		LOGGER.info("Finding execution with id: {}", execId);
		Execution execution = executionRepository.findById(UUID.fromString(execId))
				.orElseThrow(() -> new ResourceNotFoundException("Execution ID", execId));

		if (execution != null && !ExecutionOverallResultStatus.FAILURE.equals(execution.getResult())) {
			LOGGER.info("Updating execution result to: {}", statusData);
			execution.setResult(ExecutionOverallResultStatus.valueOf(statusData.toUpperCase().trim()));
			executionRepository.save(execution);
		}

		LOGGER.info("Finding execution result with id: {}", execResult);
		ExecutionResult executionResult = executionResultRepository.findById(UUID.fromString(execResult))
				.orElseThrow(() -> new ResourceNotFoundException("ExecutionResult  ID", execResult));

		if (executionResult != null && !ExecutionResultStatus.FAILURE.equals(executionResult.getResult())) {
			LOGGER.info("Updating execution result to: {}", statusData);
			executionResult.setResult(ExecutionResultStatus.valueOf(statusData.toUpperCase().trim()));
			executionResultRepository.save(executionResult);
		}
		return true;

	}

	/**
	 * This method is used to get the client port
	 * 
	 * @param deviceIP - the device IP
	 * @param port     - the port
	 * @return JSONObject - the client port
	 */
	@Override
	public JSONObject getClientPort(String deviceIP, String port) {
		LOGGER.info("Getting client port for deviceIP: {} and port: {}", deviceIP, port);
		JSONObject resultNode = new JSONObject();
		if (deviceIP != null && port != null) {
			Device device = deviceRepository.findByIpAndPort(deviceIP, port);
			if (device != null) {
				// Adding properties to the JsonObject
				try {
					resultNode.put("logTransferPort", device.getAgentMonitorPort());
					resultNode.put("statusPort", device.getStatusPort());
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}
			}
		}
		// Return the JSON response
		return resultNode;
	}

	/**
	 * This method is used to get the executions by category
	 * 
	 * @param category - the category RDKV, B, C
	 * @param page     - the page number
	 * @param size     - size in page
	 * @param sortBy   - by default it is createdDate
	 * @param sortDir  - by default it is desc
	 * @return ExecutionListResponseDTO
	 */
	@Override
	public ExecutionListResponseDTO getExecutionsByCategory(String categoryName, int page, int size, String sortBy,
			String sortDir) {
		LOGGER.debug("Fetching executions by category: {} for size{} and page number{} ", categoryName, size, page);
		Category category = commonService.validateCategory(categoryName);
		Pageable pageable = getPageable(page, size, sortBy, sortDir);
		Page<Execution> pageExecutions = executionRepository.findByCategory(category, pageable);
		ExecutionListResponseDTO executionListResponseDTO = getExecutionListResponseFromSearchResult(pageExecutions);

		return executionListResponseDTO;
	}

	/**
	 * This method is to search executions based on test suite name and script name
	 * 
	 * @param scriptTestSuiteName - full script name or testsuite name or partial
	 *                            name for search query
	 * @param categoryName        - RDKV, RDKB, RDKC
	 * @param page                - the page number
	 * @param size                - size in page
	 * @param sortBy              - by default it is createdDate
	 * @param sortDir             - by default it is desc
	 * @return ExecutionListResponseDTO
	 */
	@Override
	public ExecutionListResponseDTO getExecutionsByScriptTestsuite(String testSuiteName, String categoryName, int page,
			int size, String sortBy, String sortDir) {
		LOGGER.info("Searching executions by test suite or script  name: {} for size{} and page number{} ",
				testSuiteName, size, page);

		Category category = Category.valueOf(categoryName);
		if (category == null) {
			throw new UserInputException("Invalid category name provided");
		}

		Pageable pageable = getPageable(page, size, sortBy, sortDir);
		// Search the executions based on the test suite name or script
		Page<Execution> pageExecutions = executionRepository.findByscripttestSuiteNameContainingAndCategory(
				testSuiteName, Category.valueOf(categoryName), pageable);

		ExecutionListResponseDTO executionListResponseDTO = getExecutionListResponseFromSearchResult(pageExecutions);

		return executionListResponseDTO;
	}

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
	@Override
	public ExecutionListResponseDTO getExecutionsByDeviceName(String deviceName, String categoryName, int page,
			int size, String sortBy, String sortDir) {
		LOGGER.info("Fetching executions by device name: {} for size{} and page number{} ", deviceName, size, page);
		Pageable pageable = getPageable(page, size, sortBy, sortDir);
		Page<Execution> pageExecutions = executionRepository.findByDeviceName(deviceName, pageable);
		ExecutionListResponseDTO executionListResponseDTO = getExecutionListResponseFromSearchResult(pageExecutions);
		return executionListResponseDTO;
	}

	/**
	 * This method is used to get the executions by execution name with pagination
	 * 
	 * @param executionName - the execution name
	 * @param categoryName  - RDKV, RDKB, RDKC
	 * @param page          - the page number
	 * @param size          - size in page
	 * @param sortBy        - by default it is date
	 * @param sortDir       - by default it is desc
	 * @return executionListResponseDTO
	 */
	@Override
	public ExecutionListResponseDTO getExecutionsByExecutionName(String executionName, String categoryName, int page,
			int size, String sortBy, String sortDir) {
		LOGGER.info("Fetching executions by execution name: {} for size{} and page number{} ", executionName, size,
				page);
		Category category = Category.valueOf(categoryName);
		if (category == null) {
			throw new UserInputException("Invalid category name provided");
		}
		Pageable pageable = getPageable(page, size, sortBy, sortDir);
		Page<Execution> pageExecutions = executionRepository.findByNameContainingAndCategory(executionName, category,
				pageable);
		ExecutionListResponseDTO executionListResponseDTO = getExecutionListResponseFromSearchResult(pageExecutions);
		return executionListResponseDTO;

	}

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
	@Override
	public ExecutionListResponseDTO getExecutionsByUser(String username, String categoryName, int page, int size,
			String sortBy, String sortDir) {
		LOGGER.info("Fetching executions by user: {} for size{} and page number{} ", username, size, page);
		User user = userRepository.findByUsername(username);
		if (user == null) {
			throw new ResourceNotFoundException("User", username);
		}

		Category category = Category.valueOf(categoryName);
		if (category == null) {
			throw new UserInputException("Invalid category name provided");
		}

		Pageable pageable = getPageable(page, size, sortBy, sortDir);
		Page<Execution> pageExecutions = executionRepository.findByUserAndCategory(user.getUsername(), category,
				pageable);
		ExecutionListResponseDTO executionListResponseDTO = getExecutionListResponseFromSearchResult(pageExecutions);
		return executionListResponseDTO;

	}

	/**
	 * This method is used to get the pageable object
	 * 
	 * @param page    - the page number
	 * @param size    - size in page
	 * @param sortBy  - by default it is createdDate
	 * @param sortDir - by default it is desc
	 * @return Pageable - the pageable object
	 */
	private Pageable getPageable(int page, int size, String sortBy, String sortDir) {
		Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
				: Sort.by(sortBy).descending();
		return PageRequest.of(page, size, sort);
	}

	/**
	 * This method is to convert the pagination based execution search data to
	 * response DTO
	 * 
	 * @param pageExecutions - Execution pagination data
	 * @return Final Execution list response
	 */
	private ExecutionListResponseDTO getExecutionListResponseFromSearchResult(Page<Execution> pageExecutions) {
		List<Execution> listOfExecutions = pageExecutions.getContent();
		List<ExecutionListDTO> executionListDTO = getExecutionDTOListFromExecutionList(listOfExecutions);
		if (executionListDTO.isEmpty()) {
			return null;
		}
		ExecutionListResponseDTO executionListResponseDTO = new ExecutionListResponseDTO();
		executionListResponseDTO.setExecutions(executionListDTO);
		executionListResponseDTO.setCurrentPage(pageExecutions.getNumber());
		executionListResponseDTO.setTotalItems(pageExecutions.getTotalElements());
		executionListResponseDTO.setTotalPages(pageExecutions.getTotalPages());
		return executionListResponseDTO;

	}

	/*
	 * This method is used to get the execution dto list from the execution list
	 * 
	 * @param executionList - the execution list
	 * 
	 * @return List<ExecutionListDTO> - the execution list DTO
	 */
	private List<ExecutionListDTO> getExecutionDTOListFromExecutionList(List<Execution> executionList) {
		List<ExecutionListDTO> executionListDTO = new ArrayList<>();
		for (Execution execution : executionList) {
			ExecutionListDTO executionDTO = new ExecutionListDTO();
			executionDTO.setExecutionName(execution.getName());
			executionDTO.setExecutionDate(Utils.convertInstantToWithoutMilliseconds(execution.getCreatedDate()));
			executionDTO.setStatus(this.getStatusFromExecution(execution));
			// Find ExecutionDevice for execution
			ExecutionDevice executionDevice = executionDeviceRepository.findByExecution(execution);
			if (executionDevice != null) {
				executionDTO.setDevice(executionDevice.getDevice());
			}

			String user = execution.getUser();
			if (user != null) {
				executionDTO.setUser(user);
			}
			executionDTO.setExecutionId(execution.getId().toString());
			ExecutionType executionType = execution.getExecutionType();
			String scriptTestSuite = null;
			if (executionType != null) {
				if ((executionType.equals(ExecutionType.SINGLESCRIPT))
						|| (executionType.equals(ExecutionType.TESTSUITE))) {
					scriptTestSuite = execution.getScripttestSuiteName();
				} else if (executionType.equals(ExecutionType.MULTISCRIPT)) {
					scriptTestSuite = "Multiple Scripts";
				} else if (executionType.equals(ExecutionType.MULTITESTSUITE)) {
					scriptTestSuite = "Multi TestSuite";
				}

			}

			if (executionType.equals(ExecutionType.TESTSUITE) || executionType.equals(ExecutionType.MULTITESTSUITE)
					|| executionType.equals(ExecutionType.MULTISCRIPT)) {
				if (execution.getExecutionStatus() == ExecutionProgressStatus.INPROGRESS) {
					executionDTO.setAbortNeeded(true);
				}
			}
			executionDTO.setScriptTestSuite(scriptTestSuite);

			executionListDTO.add(executionDTO);
		}

		return executionListDTO;

	}

	/**
	 * Retrieves the status from the given Execution object.
	 *
	 * This method checks the execution status and result of the provided Execution
	 * object and returns the corresponding status as a string. The possible
	 * statuses are: - INPROGRESS: If the execution status is INPROGRESS. - ABORTED:
	 * If the execution status is ABORTED. - SUCCESS: If the execution status is
	 * COMPLETED and the result is SUCCESS. - FAILURE: If the execution status is
	 * COMPLETED and the result is FAILURE, or if none of the above conditions are
	 * met.
	 *
	 * @param execution the Execution object from which to retrieve the status
	 * @return a string representing the status of the execution
	 */
	private String getStatusFromExecution(Execution execution) {
		if (execution.getExecutionStatus() == ExecutionProgressStatus.INPROGRESS) {
			return ExecutionProgressStatus.INPROGRESS.toString();
		} else if (execution.getExecutionStatus() == ExecutionProgressStatus.ABORTED) {
			return ExecutionProgressStatus.ABORTED.toString();
		} else if (execution.getExecutionStatus() == ExecutionProgressStatus.PAUSED) {
			return ExecutionProgressStatus.PAUSED.toString();
		} else if (execution.getExecutionStatus() == ExecutionProgressStatus.COMPLETED) {
			if (execution.getResult() == ExecutionOverallResultStatus.SUCCESS) {
				return ExecutionOverallResultStatus.SUCCESS.toString();
			} else if (execution.getResult() == ExecutionOverallResultStatus.FAILURE) {
				return ExecutionOverallResultStatus.FAILURE.toString();
			}
		}
		return ExecutionOverallResultStatus.FAILURE.toString();

	}

	/**
	 * Retrieves the execution logs for a given execution result ID.
	 *
	 * @param executionResultID the ID of the execution result for which logs are to
	 *                          be fetched
	 * @return a string containing the execution remarks followed by the content of
	 *         the execution log file
	 * @throws ResourceNotFoundException if the execution result ID is not found or
	 *                                   if the log file does not exist
	 */
	@Override
	public String getExecutionLogs(String executionResultID) {
		LOGGER.info("Fetching execution logs for exec with Id: {}", executionResultID);
		ExecutionResult executionResult = executionResultRepository.findById(UUID.fromString(executionResultID))
				.orElseThrow(() -> new ResourceNotFoundException("Execution Result ID ", executionResultID));
		String executionID = executionResult.getExecution().getId().toString();

		String executionLogfile = commonService.getExecutionLogFilePath(executionID,
				executionResult.getId().toString());
		String executionRemarks = executionResult.getExecutionRemarks() + "\n";
		File logFile = new File(executionLogfile);
		if (logFile.exists()) {
			StringBuilder logFileData = new StringBuilder(executionRemarks); // Start with execution remarks

			try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
				String line;
				while ((line = reader.readLine()) != null) {
					logFileData.append("\n").append(line); // Add line with newline character
				}
				return logFileData.toString();
			} catch (IOException e) {
				LOGGER.error("Error reading log file: {}", executionLogfile, e);
				throw new TDKServiceException("Error Occcured while reading the log file");
			}
		} else {
			LOGGER.warn("Displaying the execution remarks: {}", executionRemarks);
			return (executionRemarks);

		}
	}

	/**
	 * Retrieves the execution name based on the provided ExecutionNameRequestDTO.
	 * 
	 * @param nameRequestDTO the DTO containing the list of device names and test
	 *                       type
	 * @return the generated execution name
	 * 
	 *         The method performs the following steps: 1. Logs the initiation of
	 *         the execution name retrieval process. 2. Extracts the list of device
	 *         names from the request DTO. 3. Initializes an empty string for the
	 *         execution name. 4. Checks if the list of devices is not empty: - If
	 *         there are multiple devices, logs the process and generates an
	 *         execution name for multiple devices. - If there is a single device,
	 *         logs the process, retrieves the device from the repository, and
	 *         generates an execution name based on the device and test type.
	 */
	@Override
	public String getExecutionName(ExecutionNameRequestDTO nameRequestDTO) {
		// Implementation logic to handle the list of devices
		LOGGER.info("Going to get the Execution Name");
		List<String> devices = nameRequestDTO.getDeviceNames();
		String executionName = "";

		if (!devices.isEmpty()) {
			if (devices.size() > 1) {
				LOGGER.info("Going to generate device name for multiple devices execution");
				executionName = Constants.MULTIPLE_KEY_WORD + Constants.UNDERSCORE
						+ Utils.getTimeStampInUTCForExecutionName();
			} else if (devices.size() == 1) {
				LOGGER.info("Going to generate device name for single devices execution");
				Device device = deviceRepository.findByName(devices.getFirst());
				executionName = this.getExecutionNameFromDeviceAndTestType(device, nameRequestDTO.getTestType());
			}
		}

		return executionName;

	}

	/**
	 * Generates an execution name based on the provided device and test type.
	 *
	 * @param device   the device object containing information about the OEM and
	 *                 SOC
	 * @param testType the type of test being executed
	 * @return a string representing the execution name, which includes the SOC
	 *         name, OEM name, test type, and a timestamp in UTC format. If the OEM
	 *         or SOC is null, the device name is used instead. If the test type is
	 *         empty, only the timestamp is appended.
	 */
	private String getExecutionNameFromDeviceAndTestType(Device device, String testType) {
		LOGGER.info("Generating execution name based on device and test type");
		String executionName;
		Oem oem = device.getOem();
		Soc soc = device.getSoc();
		if (oem != null && soc != null) {
			if (oem.getName().equalsIgnoreCase(soc.getName())) {
				executionName = soc.getName() + Constants.UNDERSCORE;
			} else {
				executionName = soc.getName() + Constants.UNDERSCORE + oem.getName() + Constants.UNDERSCORE;
			}
		} else if (oem != null && soc == null) {
			executionName = oem.getName() + Constants.UNDERSCORE;
		} else if (soc != null && oem == null) {
			executionName = soc.getName() + Constants.UNDERSCORE;
		} else {
			executionName = device.getName() + Constants.UNDERSCORE;
		}
		if (!Utils.isEmpty(testType)) {
			executionName += testType + Constants.UNDERSCORE + Utils.getTimeStampInUTCForExecutionName();
		} else {
			executionName += Utils.getTimeStampInUTCForExecutionName();
		}
		LOGGER.info("Generated Execution Name: {}", executionName);
		return executionName;
	}

	/**
	 * This method is used to get the trend analysis
	 * 
	 * @param executionResultId - the execution result ID
	 * @return List of String - the trend analysis
	 */
	@Override
	public List<String> getTrendAnalysis(UUID executionResultId) {
		LOGGER.info("Fetching script trend for executionResultId: {}", executionResultId);
		ExecutionResult executionResult;
		try {
			executionResult = executionResultRepository.findById(executionResultId)
					.orElseThrow(() -> new ResourceNotFoundException("ExecutionResult", executionResultId.toString()));
		} catch (ResourceNotFoundException e) {
			LOGGER.error("ExecutionResult not found with id: {}", executionResultId);
			throw e;
		} catch (Exception e) {
			LOGGER.error("Error fetching ExecutionResult with id: {}", executionResultId, e);
			throw new TDKServiceException(e.getMessage());
		}
		String scriptName = executionResult.getScript();
		List<ExecutionResult> results;
		try {
			Pageable pageable = PageRequest.of(0, 5);
			results = executionResultRepository.findTop5ByScriptOrderByDateOfExecutionDesc(scriptName, pageable);
		} catch (Exception e) {
			LOGGER.error("Error fetching script trend for script: {}", scriptName, e);
			throw new TDKServiceException(e.getMessage());
		}
		// Remove the ExecutionResult whose status is inprogress
		results.removeIf(result -> result.getStatus().equals(ExecutionStatus.INPROGRESS));

		List<String> trend = results.stream().map(ExecutionResult::getResult).map(Enum::name).toList();
		LOGGER.info("Successfully fetched script trend for executionResultId: {}", executionResultId);
		return trend;
	}

	/**
	 * This method is used to get the execution result
	 * 
	 * @param execResultId - the execution result ID
	 * @return ExecutionResultResponseDTO - the execution result response
	 * @throws ResourceNotFoundException - if the execution result is not found
	 */
	@Override
	public ExecutionResultResponseDTO getExecutionResult(UUID execResultId) {
		LOGGER.info("Fetching execution result for  execResultId: {}" + execResultId);
		ExecutionResult executionResult;
		try {
			executionResult = executionResultRepository.findById(execResultId)
					.orElseThrow(() -> new ResourceNotFoundException("ExecutionResult", execResultId.toString()));
		} catch (ResourceNotFoundException e) {
			LOGGER.error("ExecutionResult not found with id: {}", execResultId);
			throw e;

		} catch (Exception e) {
			LOGGER.error("Error fetching ExecutionResult with id: {}", execResultId, e);
			throw new TDKServiceException(e.getMessage());
		}
		Execution execution = executionResult.getExecution();
		ExecutionDevice executionDevice = executionDeviceRepository.findByExecution(execution);
		ExecutionResultResponseDTO response = new ExecutionResultResponseDTO();
		response.setScript(executionResult.getScript());
		response.setTimeTaken(executionResult.getExecutionTime());
		response.setExecutionTrend(getTrendAnalysis(execResultId));
		response.setExecutionDevice(executionDevice.getDevice());
		response.setExecutionResultStatus(executionResult.getResult().name());
		String executionLogs = null;
		executionLogs = this.getExecutionLogs(execResultId.toString());

		if (!Utils.isEmpty(executionLogs)) {
			response.setLogs(executionLogs);
		} else if (executionResult.getExecutionRemarks() != null) {
			response.setLogs(executionResult.getExecutionRemarks().toString());
		} else {
			response.setLogs("No logs found for this execution");
		}

		List<ExecutionMethodResult> methodResults;
		try {
			methodResults = executionMethodResultRepository.findByExecutionResult(executionResult);
			int methodCount = methodResults.size();
			response.setTestCaseCount(methodCount);

		} catch (Exception e) {
			LOGGER.error("Error fetching ExecutionMethodResults for ExecutionResult with id: {}", execResultId, e);
			throw new TDKServiceException(e.getMessage());
		}

		if (methodResults != null) {
			List<ExecutionMethodResultResponseDTO> methodResponse = new ArrayList<>();
			for (ExecutionMethodResult result : methodResults) {
				ExecutionMethodResultResponseDTO method = new ExecutionMethodResultResponseDTO();
				method.setFunctionName(result.getFunctionName());
				method.setActualResult(result.getActualResult().name());
				method.setExpectedResult(result.getExpectedResult().name());
				method.setResultStatus(result.getMethodResult().name());
				methodResponse.add(method);
			}
			response.setExecutionMethodResult(methodResponse);
		}
		response.setAgentLogUrl(appConfig.getBaseURL() + "/execution/getAgentLogContent?executionResId="
				+ executionResult.getId().toString());
		// Check if logs are available and set the status in the response
		response.setAgentLogsAbvailable(this.checkLogExists(Constants.AGENTLOGTYPE, executionResult));
		response.setDeviceLogsAvailable(this.checkLogExists(Constants.DEVICELOGTYPE, executionResult));
		response.setCrashLogsAvailable(this.checkLogExists(Constants.CRASHLOGTYPE, executionResult));

		LOGGER.info("Successfully fetched execution result for execId: {}, execResultId: {}", execResultId);
		return response;
	}

	/**
	 * This method is used to check the log exists or not
	 * 
	 * @param logtype         - Device, Crash, Agent
	 * @param executionResult - the execution result
	 * @return boolean - true if the log exists, false otherwise
	 */
	private boolean checkLogExists(String logtype, ExecutionResult executionResult) {
		LOGGER.info("Checking if the log exists for log type: {}", logtype);
		String executionID = executionResult.getExecution().getId().toString();
		String executionResultID = executionResult.getId().toString();
		if (logtype.equalsIgnoreCase(Constants.DEVICELOGTYPE)) {
			return checkDeviceLogsExists(executionID, executionResultID);
		} else if (logtype.equalsIgnoreCase(Constants.AGENTLOGTYPE)) {
			return checkAgentLogsExists(executionID, executionResultID);
		} else if (logtype.equalsIgnoreCase(Constants.CRASHLOGTYPE)) {
			return checkCrashLogsExists(executionID, executionResultID);
		} else {
			return false;
		}

	}

	/**
	 * This method is used to check the device logs exists or not
	 * 
	 * @param executionID       - the execution ID
	 * @param executionResultID - the execution result ID
	 * @return boolean - true if the device logs exists, false otherwise
	 */
	private boolean checkDeviceLogsExists(String executionID, String executionResultID) {
		String baseLogPath = commonService.getBaseLogPath();
		String devicelogsDirectoryPath = commonService.getDeviceLogsPathForTheExecution(executionID, executionResultID,
				baseLogPath);
		return commonService.checkAFolderExists(devicelogsDirectoryPath);
	}

	/**
	 * This method is used to check the agent logs exists or not
	 * 
	 * @param executionID       - the execution ID
	 * @param executionResultID - the execution result ID
	 * @return boolean - true if the agent logs exists, false otherwise
	 */
	private boolean checkAgentLogsExists(String executionID, String executionResultID) {
		String baseLogPath = commonService.getBaseLogPath();
		String agentLogsDirectoryPath = commonService.getAgentLogPath(executionID, executionResultID, baseLogPath);
		return commonService.checkAFolderExists(agentLogsDirectoryPath);
	}

	/**
	 * This method is used to abort the execution
	 * 
	 * @param execId - the execution ID
	 */

	@Override
	public boolean abortExecution(UUID execId, String execName) {
		LOGGER.info("Aborting execution with id: {} or name: {}", execId, execName);
		Execution execution = null;
		if (null != execId) {
			execution = executionRepository.findById(execId).orElse(null);
		}
		// If not found by ID, try by name
		if (null == execution && !Utils.isEmpty(execName)) {
			execution = executionRepository.findByName(execName);
		}
		if (execution == null) {
			LOGGER.error("Execution  not found for id: {} or name: {}", execId, execName);
			throw new ResourceNotFoundException("Execution with ",
					(null != execId ? "ID: " + execId : "Name: " + execName));
		}

		if (execution.getExecutionStatus() == ExecutionProgressStatus.COMPLETED) {
			LOGGER.error("Execution with name: {} is already completed", execution.getName());
			throw new UserInputException("Execution is already completed");
		}
		if (execution.isAbortRequested()) {
			throw new UserInputException(
					"Abort is already requested, Please wait for the current script execution to finish");
		}
		try {
			execution.setAbortRequested(true);
			executionRepository.save(execution);
		} catch (Exception e) {
			LOGGER.error("Error aborting execution with id: {}", execId, e);
			throw new TDKServiceException(e.getMessage());
		}
		return true;
	}

	/**
	 * Repeats the execution of a given execution ID.
	 *
	 * @param execId the UUID of the execution to be repeated
	 * @return true if the execution is successfully repeated
	 * @throws ResourceNotFoundException if the execution, execution device, or
	 *                                   scripts are not found
	 * @throws TDKServiceException       if there is an error during the execution
	 *                                   repetition process
	 */
	@Override
	public boolean repeatExecution(UUID execId, String user) {
		LOGGER.info("Repeating execution with id: {}", execId);
		Execution execution = executionRepository.findById(execId)
				.orElseThrow(() -> new ResourceNotFoundException("Execution", execId.toString()));
		ExecutionDevice executionDevice = executionDeviceRepository.findByExecution(execution);
		if (executionDevice == null) {
			LOGGER.error("Execution Device not found for execution with id: {}", execId);
			throw new ResourceNotFoundException("Execution Device", "Execution ID: " + execId.toString());
		}
		Device device = deviceRepository.findByName(executionDevice.getDevice());
		if (device == null || device.getDeviceStatus() != DeviceStatus.FREE) {
			LOGGER.error(" Device not available for execution");
			throw new UserInputException("The device not available for execution");
		}
		List<ExecutionResult> executionResult = execution.getExecutionResults();
		List<String> scriptNames = executionResult.stream().map(ExecutionResult::getScript).toList();
		List<Script> scriptList = this.getValidScriptList(scriptNames, null);
		boolean isDeviceScriptCategoryInValid = false;
		for (Script script : scriptList) {
			if (!commonService.vaidateScriptDeviceCategory(device, script)) {
				isDeviceScriptCategoryInValid = true;
				break;
			} else {
				continue;
			}
		}
		if (isDeviceScriptCategoryInValid) {
			LOGGER.error("Device and Script combination is invalid and belongs to different category");
			throw new UserInputException("Device and Script combination is invalid and belongs to different category");
		}
		try {
			List<String> executionNames = executionRepository.findAll().stream().map(Execution::getName)
					.collect(Collectors.toList());
			String execName = getNextRepeatExecutionName(execution.getName(), executionNames);
			List<String> deviceList = new ArrayList<>();
			deviceList.add(device.getName());
			List<String> testSuiteList = new ArrayList<>();
			if (execution.getExecutionType() == ExecutionType.TESTSUITE) {
				testSuiteList.add(execution.getScripttestSuiteName());

			}
			ExecutionTriggerDTO executionTriggerDTO = new ExecutionTriggerDTO();
			executionTriggerDTO.setDeviceList(deviceList);
			if (testSuiteList == null || testSuiteList.isEmpty()) {
				executionTriggerDTO.setScriptList(scriptNames);
			}
			if (null != testSuiteList && !testSuiteList.isEmpty()) {
				executionTriggerDTO.setTestSuite(testSuiteList);
			}
			if (null != execution.getTestType()) {
				executionTriggerDTO.setTestType(execution.getTestType());
			}
			if (Utils.isEmpty(user) && null != execution.getUser()) {
				executionTriggerDTO.setUser(execution.getUser());
			} else {
				executionTriggerDTO.setUser(user);
			}
			executionTriggerDTO.setCategory(execution.getCategory().name());
			executionTriggerDTO.setExecutionName(execName);
			executionTriggerDTO.setRepeatCount(1);
			executionTriggerDTO.setRerunOnFailure(false);
			executionTriggerDTO.setDeviceLogsNeeded(true);
			executionTriggerDTO.setDiagnosticLogsNeeded(true);
			executionTriggerDTO.setPerformanceLogsNeeded(true);
			this.startExecution(executionTriggerDTO);
			LOGGER.info("Successfully repeated execution with id: {}", execId);
		} catch (Exception e) {
			LOGGER.error("Error repeating execution with id: {}", execId, e);
			throw new TDKServiceException("Error repeating execution with id: " + execId + e.getMessage());
		}
		return true;
	}

	/**
	 * Re-runs the failed scripts for a given execution ID.
	 *
	 * @param execId the UUID of the execution whose failed scripts need to be
	 *               re-run
	 * @return true if the failed scripts were successfully re-run
	 * @throws ResourceNotFoundException if the execution, execution device, or
	 *                                   failed scripts are not found
	 * @throws TDKServiceException       if there is an error during the re-run
	 *                                   process
	 */
	@Override
	public boolean reRunFailedScript(UUID execId, String user) {
		LOGGER.info("Re-running failed scripts for execution with id: {}", execId);
		Execution execution = executionRepository.findById(execId).orElseThrow(
				() -> new ResourceNotFoundException("Execution", "id: " + execId.toString() + " not found"));
		ExecutionDevice executionDevice = executionDeviceRepository.findByExecution(execution);
		if (executionDevice == null) {
			LOGGER.error("Execution Device not found for execution with id: {}", execId);
			throw new ResourceNotFoundException("Execution Device", "Execution ID: " + execId.toString());
		}

		Device device = deviceRepository.findByName(executionDevice.getDevice());

		if (device == null || device.getDeviceStatus() != DeviceStatus.FREE) {
			LOGGER.error(" Device not available for execution");
			throw new UserInputException("The device not available for execution");
		}

		List<ExecutionResult> execResults = execution.getExecutionResults();
		List<ExecutionResult> failedResults = execResults.stream()
				.filter(result -> result.getResult().equals(ExecutionResultStatus.FAILURE)).toList();
		if (failedResults.isEmpty() || failedResults == null) {
			LOGGER.error("No failed scripts found for execution with id: {}", execId);
			throw new ResourceNotFoundException("Failed Scripts", "for Execution ID: " + execId.toString());
		}
		String triggerUser = null;
		if (Utils.isEmpty(user)) {
			LOGGER.error("User not found for execution with id: {}", execId);
			triggerUser = execution.getUser();
		} else {
			User userName = userRepository.findByUsername(user);
			triggerUser = userName.getUsername();
		}
		List<String> failedScripts = failedResults.stream().map(ExecutionResult::getScript).toList();
		List<Script> scripts = getValidScriptList(failedScripts, new StringBuilder());
		boolean isDeviceScriptCategoryInValid = false;
		for (Script script : scripts) {
			if (!commonService.vaidateScriptDeviceCategory(device, script)) {
				isDeviceScriptCategoryInValid = true;
				break;
			} else {
				continue;
			}
		}
		if (isDeviceScriptCategoryInValid) {
			LOGGER.error("Device and Script combination is invalid and belongs to different category");
			throw new UserInputException("Device and Script combination is invalid and belongs to different category");
		}
		try {
			List<String> executionNames = executionRepository.findAll().stream().map(Execution::getName)
					.collect(Collectors.toList());
			String execName = getNextRerunExecutionName(execution.getName(), executionNames);
			executionAsyncService.prepareAndExecuteMultiScript(device, scripts, triggerUser, execName,
					execution.getCategory().name(), execution.getScripttestSuiteName(), 1, false,
					execution.isDeviceLogsNeeded(), execution.isDiagnosticLogsNeeded(),
					execution.isDiagnosticLogsNeeded(), false, execution.getTestType(), null, null);
			LOGGER.info("Successfully re-run failed scripts for execution with id: {}", execId);
		} catch (Exception e) {
			LOGGER.error("Error re-running failed scripts for execution with id: {}", execId, e);
			throw new TDKServiceException(
					"Error re-running failed scripts for execution with id: " + execId + e.getMessage());
		}
		return true;
	}

	/**
	 * This method is used to delete single the execution detail
	 * 
	 * @param id - the execution ID
	 * @return ExecutionDetailsResponse
	 * @throws ResourceNotFoundException - if the execution is not found
	 */

	@Override
	@Transactional
	public boolean deleteExecution(UUID id) {
		LOGGER.info("Deleting execution with id: {}", id);
		this.deleteAllFilesForTheExecution(id.toString());
		try {
			Execution execution = executionRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Execution", id.toString()));
			List<ExecutionResult> executionResults = executionResultRepository.findByExecution(execution);
			for (ExecutionResult executionResult : executionResults) {
				List<ExecutionMethodResult> executionMethodResults = executionMethodResultRepository
						.findByExecutionResult(executionResult);
				if (executionMethodResults != null && !executionMethodResults.isEmpty()) {
					executionMethodResultRepository.deleteAll(executionMethodResults);
				}

				ExecutionResultAnalysis executionResultAnalysis = executionResultAnalysisRepository
						.findByExecutionResult(executionResult);

				if (executionResultAnalysis != null) {
					executionResultAnalysisRepository.delete(executionResultAnalysis);
				}
			}
			// Delete execution results
			if (executionResults != null && !executionResults.isEmpty()) {
				executionResultRepository.deleteAll(executionResults);
			}

			ExecutionDevice executionDevice = executionDeviceRepository.findByExecution(execution);
			if (executionDevice != null) {
				executionDeviceRepository.delete(executionDevice);
			}

			executionRepository.delete(execution);

			LOGGER.info("Successfully deleted execution with id: {}", id);
			return true;
		} catch (Exception e) {
			LOGGER.error("Error deleting execution with id: {}", id, e);
			throw new TDKServiceException("Error deleting execution with id: " + id);
		}
	}

	/**
	 * This method is used to delete the execution related files for the given
	 * execution
	 * 
	 * @param executionId - the execution ID
	 */
	private void deleteAllFilesForTheExecution(String executionId) {
		LOGGER.info("Deleting all files for the execution with id: {}", executionId);
		String logBasePath = commonService.getBaseLogPath();
		String executionBasePath = logBasePath + Constants.FILE_PATH_SEPERATOR + Constants.EXECUTION_KEYWORD
				+ Constants.UNDERSCORE + executionId;
		// Delete the execution folder
		File executionFolder = new File(executionBasePath);
		if (executionFolder.exists()) {
			try {
				FileUtils.deleteDirectory(executionFolder);
			} catch (IOException e) {
				LOGGER.error("Error deleting execution folder: {}", executionBasePath, e);
			}
		}

	}

	/**
	 * This method is used to delete the execution details list
	 * 
	 * @param id - the execution ID
	 * @return ExecutionDetailsResponse
	 * @throws ResourceNotFoundException - if the execution is not found
	 */

	@Override
	@Transactional
	public boolean deleteExecutions(List<UUID> ids) {
		LOGGER.info("Deleting executions with ids: {}", ids);
		try {
			for (UUID id : ids) {
				deleteExecution(id);
			}
			LOGGER.info("Successfully deleted executions with ids: {}", ids);
			return true;
		} catch (Exception e) {
			LOGGER.error("Error deleting executions with ids: {}", ids, e);
			throw new TDKServiceException("Error deleting executions with ids: " + ids);
		}
	}

	/**
	 * Generates the next repeat execution name based on the current execution name
	 * and a list of existing execution names.
	 *
	 * @param currentExecutionName   the current execution name to base the new name
	 *                               on
	 * @param existingExecutionNames a list of existing execution names to check for
	 *                               repeat counts
	 * @return the next repeat execution name in the format
	 *         "baseName_R{nextRepeatNumber}"
	 */
	public String getNextRepeatExecutionName(String currentExecutionName, List<String> existingExecutionNames) {
		// Extract the base name before "_R"
		String baseName = currentExecutionName.split("_R\\d+")[0];

		// Initialize the maximum repeat count
		int maxRepeatCount = 0;

		// Iterate through existing execution names
		for (String existingName : existingExecutionNames) {
			if (existingName.startsWith(baseName + "_R")) {
				try {
					// Extract the number after "_R"
					String suffix = existingName.substring((baseName + "_R").length());
					int repeatNumber = Integer.parseInt(suffix);

					// Update the maximum repeat count
					if (repeatNumber > maxRepeatCount) {
						maxRepeatCount = repeatNumber;
					}
				} catch (NumberFormatException e) {
					// Ignore names that do not conform to the pattern
					continue;
				}
			}
		}

		// Generate the next execution name
		return baseName + "_R" + (maxRepeatCount + 1);
	}

	/**
	 * Generates the next rerun execution name based on the current execution name
	 * and a list of existing execution names.
	 * 
	 * @param currentExecutionName   The name of the current execution.
	 * @param existingExecutionNames A list of existing execution names.
	 * @return The next rerun execution name.
	 * 
	 *         This method checks if the current execution name already contains
	 *         "RERUN". If it does, it extracts the base name and the current rerun
	 *         count. If it does not, it initializes the base name with "_RERUN"
	 *         appended to the current execution name.
	 * 
	 *         It then iterates through the list of existing execution names to find
	 *         the maximum rerun count for the given base name. Finally, it
	 *         generates the next rerun execution name by incrementing the maximum
	 *         rerun count by 1.
	 */
	public String getNextRerunExecutionName(String currentExecutionName, List<String> existingExecutionNames) {
		// Initialize the base name for rerun
		String baseName;
		int currentRerunCount = 0;

		// Check if the current execution name already contains "RERUN"
		if (currentExecutionName.contains("RERUN")) {
			int rerunIndex = currentExecutionName.lastIndexOf("RERUN");
			baseName = currentExecutionName.substring(0, rerunIndex + 5); // Include "RERUN"
			String suffix = currentExecutionName.substring(rerunIndex + 5);
			if (suffix.matches("\\d+")) {
				currentRerunCount = Integer.parseInt(suffix);
			}
		} else {
			baseName = currentExecutionName + "_RERUN";
		}

		// Initialize the maximum rerun count
		int maxRerunCount = currentRerunCount;

		// Iterate through the existing execution names
		for (String existingName : existingExecutionNames) {
			if (existingName.startsWith(baseName)) {
				try {
					// Check if the name ends with a number (e.g., _RERUN2, _R1_RERUN3)
					String suffix = existingName.substring(baseName.length());
					if (suffix.matches("\\d+")) {
						int rerunNumber = Integer.parseInt(suffix);
						maxRerunCount = Math.max(maxRerunCount, rerunNumber);
					}
				} catch (NumberFormatException e) {
					// Ignore names that do not conform to the expected pattern
					continue;
				}
			}
		}

		// Generate the next rerun execution name
		String name = baseName + (maxRerunCount + 1);
		return name;
	}

	/**
	 * Retrieves the execution details for a given execution ID.
	 *
	 * @param id the UUID of the execution to retrieve details for
	 * @return an ExecutionDetailsResponseDTO containing the details of the
	 *         execution
	 * @throws ResourceNotFoundException if the execution or execution device is not
	 *                                   found
	 */
	@Override
	public ExecutionDetailsResponseDTO getExecutionDetails(UUID id, String execName) {
		LOGGER.info("Fetching execution details with id: {} or name: {}", id, execName);
		Execution execution = null;
		if (null != id) {
			execution = executionRepository.findById(id).orElse(null);
		}
		// If not found by ID, try by name
		if (null == execution && !Utils.isEmpty(execName)) {
			execution = executionRepository.findByName(execName);
		}
		if (execution == null) {
			LOGGER.error("Execution  not found for id: {} or name: {}", id, execName);
			throw new ResourceNotFoundException("Execution with ", (null != id ? "ID: " + id : "Name: " + execName));
		}
		ExecutionDevice executionDevice = executionDeviceRepository.findByExecution(execution);
		if (executionDevice == null) {
			LOGGER.error("Execution Device not found for execution with id: {}", id);
			throw new ResourceNotFoundException("Execution Device", "Execution ID: " + id + " not found");
		}
		Device device = deviceRepository.findByName(executionDevice.getDevice());
		ExecutionDetailsResponseDTO response = new ExecutionDetailsResponseDTO();
		response.setDeviceName(executionDevice.getDevice());
		response.setDeviceIP(executionDevice.getDeviceIp());
		response.setDeviceMac(executionDevice.getDeviceMac());
		if (null != device) {
			response.setDeviceThunderEnabled(device.isThunderEnabled());
		}
		String deviceDetails = fileTransferService.getDeviceDetailsFromVersionFile(execution.getId().toString());
		if (null != deviceDetails) {
			deviceDetails = deviceDetails.replace("imagename:", "");
		}
		response.setDeviceDetails(deviceDetails);
		response.setDeviceImageName(executionDevice.getBuildName());
		response.setRealExecutionTime(execution.getRealExecutionTime());
		response.setTotalExecutionTime(execution.getExecutionTime());
		response.setDateOfExecution(execution.getCreatedDate());
		response.setExecutionType(execution.getExecutionType().name());
		response.setScriptTestSuite(execution.getScripttestSuiteName());
		response.setExecutionStatus(execution.getExecutionStatus().name());
		if (execution.getExecutionStatus() == ExecutionProgressStatus.COMPLETED) {
			response.setResult(execution.getResult().name());
		} else {
			response.setResult(execution.getExecutionStatus().name());
		}
		response.setSummary(getExecutionSummary(execution));
		response.setExecutionResults(getExecutionResults(execution));
		Map<String, ExecutionSummaryResponseDTO> detailMap = this.getModulewiseExecutionSummary(id, execName);
		response.setDetailMap(detailMap);
		return response;
	}

	/**
	 * Converts a list of ExecutionResult objects to a list of ExecutionResultDTO
	 * objects.
	 *
	 * @param execution the Execution object containing the list of ExecutionResult
	 *                  objects
	 * @return a list of ExecutionResultDTO objects
	 */
	public List<ExecutionResultDTO> getExecutionResults(Execution execution) {
		List<ExecutionResult> results = execution.getExecutionResults();
		List<ExecutionResultDTO> resultDTO = new ArrayList<>();
		for (ExecutionResult result : results) {
			ExecutionResultDTO resultDTOObj = new ExecutionResultDTO();
			resultDTOObj.setExecutionResultID(result.getId());
			resultDTOObj.setName(result.getScript());
			if (null != result.getStatus() && result.getStatus() == ExecutionStatus.INPROGRESS) {
				resultDTOObj.setStatus(ExecutionResultStatus.INPROGRESS.name());
			} else {
				resultDTOObj.setStatus(result.getResult().name());
			}

			ExecutionResultAnalysis executionResultAnalysis = executionResultAnalysisRepository
					.findByExecutionResult(result);
			if (null != executionResultAnalysis && !Utils.isEmpty(executionResultAnalysis.getAnalysisTicketID())) {

				resultDTOObj.setAnalysisTicket(executionResultAnalysis.getAnalysisTicketID());
			}

			resultDTO.add(resultDTOObj);
		}
		// Separate the "NA", "SKIPPED", and other statuses
		List<ExecutionResultDTO> naList = new ArrayList<>();
		List<ExecutionResultDTO> skippedList = new ArrayList<>();
		List<ExecutionResultDTO> othersList = new ArrayList<>();

		for (ExecutionResultDTO dto : resultDTO) {
			if ("NA".equals(dto.getStatus())) {
				naList.add(dto);
			} else if ("SKIPPED".equals(dto.getStatus())) {
				skippedList.add(dto);
			} else {
				othersList.add(dto);
			}
		}

		// Combine the lists, with "NA" and "SKIPPED" first
		List<ExecutionResultDTO> orderedResultDTO = new ArrayList<>();
		orderedResultDTO.addAll(naList);
		orderedResultDTO.addAll(skippedList);
		orderedResultDTO.addAll(othersList);

		return orderedResultDTO;
	}

	/**
	 * Generates an execution summary for the given execution.
	 *
	 * @param execution the execution object containing details of the execution
	 * @return an ExecutionSummaryResponseDTO object containing the summary of the
	 *         execution
	 */
	public ExecutionSummaryResponseDTO getExecutionSummary(Execution execution) {

		ExecutionSummaryResponseDTO summary = new ExecutionSummaryResponseDTO();
		summary.setTotalScripts(execution.getExecutionResults().size());
		summary.setInProgressCount((int) execution.getExecutionResults().stream()
				.filter(r -> r.getStatus() == ExecutionStatus.INPROGRESS).count());

		// Calculate counts for SUCCESS AND FAILURE status, Since the
		// status can be set while the script is in progress, we need to filter out
		// the results that are still in progress.
		int successCount = (int) execution.getExecutionResults().stream().filter(
				r -> r.getResult() == ExecutionResultStatus.SUCCESS && r.getStatus() != ExecutionStatus.INPROGRESS)
				.count();
		int failureCount = (int) execution.getExecutionResults().stream().filter(
				r -> r.getResult() == ExecutionResultStatus.FAILURE && r.getStatus() != ExecutionStatus.INPROGRESS)
				.count();

		int timeoutCount = (int) execution.getExecutionResults().stream()
				.filter(r -> r.getResult() == ExecutionResultStatus.TIMEOUT).count();

		summary.setSuccess(successCount);
		summary.setFailure(failureCount);
		summary.setExecuted(failureCount + successCount);
		summary.setTimeout(timeoutCount);
		summary.setPending((int) execution.getExecutionResults().stream()
				.filter(r -> r.getResult() == ExecutionResultStatus.PENDING).count());
		summary.setNa((int) execution.getExecutionResults().stream()
				.filter(r -> r.getResult() == ExecutionResultStatus.NA).count());
		summary.setTimeout((int) execution.getExecutionResults().stream()
				.filter(r -> r.getResult() == ExecutionResultStatus.TIMEOUT).count());
		summary.setSkipped((int) execution.getExecutionResults().stream()
				.filter(r -> r.getResult() == ExecutionResultStatus.SKIPPED).count());
		summary.setAborted((int) execution.getExecutionResults().stream()
				.filter(r -> r.getResult() == ExecutionResultStatus.ABORTED).count());
		summary.setSuccessPercentage((int) Math.round((double) summary.getSuccess() / summary.getTotalScripts() * 100));
		return summary;

	}

	/**
	 * This method is used to get the unique users.
	 * 
	 * @return List of String - the list of unique users
	 */

	@Override
	public List<String> getUniqueUsers() {
		LOGGER.info("Fetching unique users");
		List<Execution> executions = executionRepository.findAll();
		Set<String> users = new HashSet<>();
		for (Execution execution : executions) {
			if (execution.getUser() != null) {
				users.add(execution.getUser());
			}
		}
		List<String> uniqueUsers = users.stream().sorted().collect(Collectors.toList());
		LOGGER.info("Successfully fetched unique users");
		return uniqueUsers;
	}

	/**
	 * 
	 * This method is to get the module wise summary
	 * 
	 * @param executionId - the execution id
	 * @return the module wise summary
	 */
	@Override
	public Map<String, ExecutionSummaryResponseDTO> getModulewiseExecutionSummary(UUID executionId, String execName) {
		LOGGER.info("Fetching execution summary for id: {} or name: {}", executionId, execName);
		Execution execution = null;
		if (null != executionId) {
			execution = executionRepository.findById(executionId).orElse(null);
		}
		// If not found by ID, try by name
		if (null == execution && !Utils.isEmpty(execName)) {
			execution = executionRepository.findByName(execName);
		}
		if (executionId == null) {
			LOGGER.error("Execution  not found for id: {} or name: {}", executionId, execName);
			throw new ResourceNotFoundException("Execution with ",
					(null != executionId ? "ID: " + executionId : "Name: " + execName));
		}
		List<ExecutionResult> executionResults = executionResultRepository.findByExecution(execution);
		if (executionResults.isEmpty()) {
			LOGGER.error("No execution results found for execution with id: {}", execution.getId());
			return null;
		}

		// Map to store module-wise summary and the module name
		Map<String, ExecutionSummaryResponseDTO> moduleSummaryMap = new HashMap<>();

		for (ExecutionResult executionResult : executionResults) {
			String scriptName = executionResult.getScript();
			Module module = scriptService.getModuleByScriptName(scriptName);
			String moduleName = module.getName();

			ExecutionSummaryResponseDTO executionSummaryResponseDTO = null;
			if (moduleSummaryMap.containsKey(moduleName)) {
				executionSummaryResponseDTO = moduleSummaryMap.get(moduleName);

			} else {
				executionSummaryResponseDTO = new ExecutionSummaryResponseDTO();
			}
			executionSummaryResponseDTO.setTotalScripts(executionSummaryResponseDTO.getTotalScripts() + 1);

			ExecutionResultStatus result = executionResult.getResult();
			ExecutionStatus status = executionResult.getStatus();
			if (status == ExecutionStatus.INPROGRESS) {
				result = ExecutionResultStatus.INPROGRESS;
			}

			this.setExecutionSummaryCount(executionSummaryResponseDTO, result);

			moduleSummaryMap.put(moduleName, executionSummaryResponseDTO);
		}

		moduleSummaryMap.put(Constants.TOTAL_KEYWORD, this.getExecutionSummary(execution));
		if (moduleSummaryMap.isEmpty()) {
			LOGGER.error("No module-wise summary found for execution with id: {}", execution.getId());
			return null;
		} else {
			LOGGER.info("Successfully fetched module-wise summary for execution with id: {}", execution.getId());
			// loop through the map and calculate percentage and assign it to the object
			// executionSummaryResponseDTO
			for (Map.Entry<String, ExecutionSummaryResponseDTO> entry : moduleSummaryMap.entrySet()) {
				String moduleName = entry.getKey();
				ExecutionSummaryResponseDTO summaryDTO = entry.getValue();

				// Calculate total scripts (excluding "Total Execution" entry)
				int totalScripts = summaryDTO.getTotalScripts();

				// Calculate percentages (assuming all counters are non-zero)
				if (totalScripts > 0) {
					summaryDTO.setSuccessPercentage(Math.round((double) summaryDTO.getSuccess() / totalScripts * 100));
				}

				// Update the map with the modified DTO
				moduleSummaryMap.put(moduleName, summaryDTO);
			}

		}
		LOGGER.info("Successfully fetched execution summary for id: {}", execution.getId());

		return moduleSummaryMap;

	}

	/**
	 * This method is used to get the execution summary count assigned to the
	 * ExecutionSummaryResponseDTO object based on the ExecutionResultStatus
	 * 
	 * @param executionSummaryResponseDTO - the execution summary response DTO
	 * @param status                      - the execution
	 */
	private void setExecutionSummaryCount(ExecutionSummaryResponseDTO executionSummaryResponseDTO,
			ExecutionResultStatus status) {
		switch (status) {
			case SUCCESS:
				executionSummaryResponseDTO.setSuccess(executionSummaryResponseDTO.getSuccess() + 1);
				executionSummaryResponseDTO.setExecuted(executionSummaryResponseDTO.getExecuted() + 1);
				break;
			case FAILURE:
				executionSummaryResponseDTO.setFailure(executionSummaryResponseDTO.getFailure() + 1);
				executionSummaryResponseDTO.setExecuted(executionSummaryResponseDTO.getExecuted() + 1);
				break;
			case INPROGRESS:
				executionSummaryResponseDTO.setInProgressCount(executionSummaryResponseDTO.getInProgressCount() + 1);
				break;
			case PENDING:
				executionSummaryResponseDTO.setPending(executionSummaryResponseDTO.getPending() + 1);
				break;
			case NA:
				executionSummaryResponseDTO.setNa(executionSummaryResponseDTO.getNa() + 1);
				break;
			case TIMEOUT:
				executionSummaryResponseDTO.setTimeout(executionSummaryResponseDTO.getTimeout() + 1);
				break;
			case SKIPPED:
				executionSummaryResponseDTO.setSkipped(executionSummaryResponseDTO.getSkipped() + 1);
				break;
			case ABORTED:
				executionSummaryResponseDTO.setAborted(executionSummaryResponseDTO.getAborted() + 1);
				break;

			default:
				// Do nothing
				break;
		}

	}

	/**
	 * This method is used to delete the executions by date range
	 * 
	 * @param fromDate the start date
	 * @param toDate   end date
	 * @return total executions
	 */
	@Override
	public int deleteExecutionsByDateRange(Instant fromDate, Instant toDate) {
		LOGGER.info("Deleting executions between dates: {} and {}", fromDate, toDate);
		List<Execution> executions = executionRepository.executionListInDateRange(fromDate, toDate);
		if (executions.isEmpty()) {
			LOGGER.info("No executions found between dates: {} and {}", fromDate, toDate);
			return 0;
		}
		try {
			for (Execution execution : executions) {
				deleteExecution(execution.getId());
			}
			LOGGER.info("Successfully deleted executions between dates: {} and {}", fromDate, toDate);
			return executions.size();
		} catch (Exception e) {
			LOGGER.error("Error deleting executions between dates: {} and {}", fromDate, toDate, e);
			throw new TDKServiceException("Error deleting executions between dates: " + fromDate + " and " + toDate);
		}
	}

	/**
	 * Checks if crash logs exist for a given execution.
	 *
	 * @param executionID       the ID of the execution
	 * @param executionResultID the ID of the execution result
	 * @return true if the crash logs directory exists, false otherwise
	 */
	private boolean checkCrashLogsExists(String executionID, String executionResultID) {
		String baseLogPath = commonService.getBaseLogPath();
		String crashLogsDirectoryPath = commonService.getCrashLogsPathForTheExecution(executionID, executionResultID,
				baseLogPath);
		return commonService.checkAFolderExists(crashLogsDirectoryPath);
	}

	/**
	 * Downloads the script associated with the given execution result ID.
	 *
	 * @param executionResId the UUID of the execution result
	 * @return the script as a Resource
	 * @throws ResourceNotFoundException if the execution result or script is not
	 *                                   found
	 * @throws TDKServiceException       if an error occurs during the download
	 *                                   process
	 */
	@Override
	public Resource downloadScript(UUID executionResId) {
		LOGGER.info("Downloading script for execution result id: {}", executionResId);
		ExecutionResult executionResult = executionResultRepository.findById(executionResId)
				.orElseThrow(() -> new ResourceNotFoundException("ExecutionResult", executionResId.toString()));
		String scriptName = executionResult.getScript();
		Script script = scriptRepository.findByName(scriptName);
		if (script == null) {
			LOGGER.error("Script not found with name: {}", scriptName);
			throw new ResourceNotFoundException("Script", scriptName);
		}
		Path scriptFilePath = Paths
				.get(AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + script.getScriptLocation()
						+ Constants.FILE_PATH_SEPERATOR + scriptName + Constants.PYTHON_FILE_EXTENSION);
		Resource resource = null;
		try {
			resource = new UrlResource(scriptFilePath.toUri());
		} catch (MalformedURLException e) {
			e.printStackTrace();
			LOGGER.error("Error creating URL resource for script file: {}", scriptFilePath, e);
			throw new TDKServiceException("Error downloading script for execution result ");
		}
		if (!resource.exists()) {
			LOGGER.error("Script file not found at path: {}", scriptFilePath);
			throw new ResourceNotFoundException("Script ", script.getName());
		}
		LOGGER.info("Successfully downloaded script for execution result id: {}", executionResId);
		return resource;

	}

	/**
	 * Gets the list of the executions based on the filter criteria in the DTO
	 * 
	 * @param filterRequest - the filter DTO with multple filter criteria like start
	 *                      date, end date, execution type, script test suite,
	 *                      device type etc.
	 * @return the list of executions based on the filter criteria
	 */
	@Override
	public List<ExecutionListDTO> getExecutionDetailsByFilter(ExecutionSearchFilterDTO searchFilterRequest) {
		LOGGER.info("Fetching execution details based on custom filter criteria");

		List<Execution> filteredExecutionList = this.getFilteredExecutions(searchFilterRequest);
		if (filteredExecutionList == null || filteredExecutionList.isEmpty()) {
			LOGGER.info("No executions found based on the filter criteria");
			return null;
		} else {
			if (!Utils.isEmpty(searchFilterRequest.getDeviceType())) {
				LOGGER.info("Filtering the execution list based on device type: {}",
						searchFilterRequest.getDeviceType());
				filteredExecutionList = this.filterTheExecutionByDeviceType(filteredExecutionList,
						searchFilterRequest.getDeviceType());
			}
			if (filteredExecutionList.isEmpty() || filteredExecutionList == null) {
				LOGGER.info("No executions found based on the filter criteria");
				return null;
			}

		}
		List<ExecutionListDTO> finalExecutionListResponse = this
				.getExecutionDTOListFromExecutionList(filteredExecutionList);
		LOGGER.info("Fetching execution details based on custom filter criteria completed successfully");

		return finalExecutionListResponse;

	}

	/**
	 * This method is used to again filter the filtered execution list by device
	 * type
	 * 
	 * @param filteredExecutionList - the filtered execution list with other filters
	 * @param deviceType            - the device type
	 * @return the refiltered execution list by device
	 */
	private List<Execution> filterTheExecutionByDeviceType(List<Execution> filteredExecutionList, String deviceType) {
		List<Execution> filteredExecutionListByDeviceType = new ArrayList<>();
		for (Execution execution : filteredExecutionList) {
			ExecutionDevice executionDevice = executionDeviceRepository.findByExecution(execution);
			if (executionDevice != null) {
				if (executionDevice != null && executionDevice.getDeviceType().equalsIgnoreCase(deviceType)) {
					filteredExecutionListByDeviceType.add(execution);
				}
			}
		}
		return filteredExecutionListByDeviceType;
	}

	/**
	 * This method is used to filter an return execution list based on from date, to
	 * date, execution type, script test suite, category and status
	 * 
	 * @param filterRequest - the filter DTO with multiple filter criteria
	 * @return the list of executions based on the filter criteria
	 */
	private List<Execution> getFilteredExecutions(ExecutionSearchFilterDTO filterRequest) {
		LOGGER.info("Filtering the execution list based on the filter criteria ");
		try {
			// Validate the Category
			Category category = Category.valueOf(filterRequest.getCategory().toUpperCase());
			if (category == null) {
				throw new UserInputException("Invalid category name provided");
			}

			// Add page limit to the pageable object
			Pageable pageable = Pageable.unpaged();
			if (filterRequest.getSizeLimit() > 0) {
				pageable = PageRequest.of(0, filterRequest.getSizeLimit());
			} else {
				// If no size limit added, the add without any limit
				pageable = Pageable.unpaged();
			}

			// Only SUCCESS and FAILURE status are allowed
			List<ExecutionOverallResultStatus> executionStatuses = Arrays.asList(ExecutionOverallResultStatus.SUCCESS,
					ExecutionOverallResultStatus.FAILURE);

			List<Execution> executions = null;

			// The case where the execution type is not provided and script test suite is
			// not provided
			if ((Utils.isEmpty(filterRequest.getExecutionType()))
					&& (Utils.isEmpty(filterRequest.getScriptTestSuite()))) {
				LOGGER.info("Fetching execution list based on category, start date, end date and status");
				executions = executionRepository.getExecutionListByFilter(category, filterRequest.getStartDate(),
						filterRequest.getEndDate(), executionStatuses, pageable);
			} else if (!Utils.isEmpty(filterRequest.getExecutionType())) {
				LOGGER.info(
						"Fetching execution list based on category, start date, end date, status and execution type");
				ExecutionType executionType = ExecutionType.valueOf(filterRequest.getExecutionType().toUpperCase());
				if (executionType == null) {
					throw new UserInputException("Invalid execution type provided");
				}
				// The case where the execution type is provided and script test suite is not
				// provided
				if (Utils.isEmpty(filterRequest.getScriptTestSuite())) {
					executions = executionRepository.getExecutionListByFilterWithExecutionType(category,
							filterRequest.getStartDate(), filterRequest.getEndDate(), executionStatuses, executionType,
							pageable);
				} else {
					// The case where the execution type and script test suite are provided
					if (executionType == ExecutionType.TESTSUITE || executionType == ExecutionType.SINGLESCRIPT) {
						// The case where the execution type is testsuite or singlescript
						executions = executionRepository.getExecutionListByFilterWithExecutionTypeAndSuitescript(
								category, filterRequest.getStartDate(), filterRequest.getEndDate(), executionStatuses,
								executionType, filterRequest.getScriptTestSuite(), pageable);
					} else {
						// The case where the execution type is Multiscript
						executions = executionRepository.getExecutionListByFilterWithExecutionType(category,
								filterRequest.getStartDate(), filterRequest.getEndDate(), executionStatuses,
								executionType, pageable);
					}
				}

			}
			return executions;
		} catch (Exception e) {
			LOGGER.error("Error fetching execution details based on custom filter criteria", e);
			throw new TDKServiceException(e.getMessage());
		}

	}

	/**
	 * Method that checks if any execution result is failed.
	 *
	 * @param executionId the UUID of the execution to check
	 * @return true if any execution result is failed
	 */
	@Override
	public boolean isExecutionResultFailed(UUID executionId) {
		LOGGER.info("Checking if execution result failed for execution id: {}", executionId);
		Execution execution = executionRepository.findById(executionId)
				.orElseThrow(() -> new ResourceNotFoundException("Execution", "id" + executionId));
		List<ExecutionResult> executionResults = executionResultRepository.findByExecution(execution);
		if (executionResults.isEmpty()) {
			LOGGER.error("No execution results found for execution with id: {}", executionId);
			throw new ResourceNotFoundException("Execution Results", "for Execution ID: " + executionId.toString());
		}
		boolean isFailed = false;
		for (ExecutionResult executionResult : executionResults) {
			if (executionResult.getResult() == ExecutionResultStatus.FAILURE) {
				isFailed = true;
				break;
			}
		}
		LOGGER.info("Execution result failed status for execution id: {} is: {}", executionId, isFailed);
		return isFailed;
	}

	/**
	 * Method to get the device status based on the device name and device type
	 * 
	 * @param deviceName
	 * @param deviceType
	 * @return JSONObject - the device status in JSON format
	 */
	@Override
	public JSONObject getDeviceStatus(String deviceName, String deviceType) {
		Device device = deviceRepository.findByName(deviceName);
		JSONObject deviceStatus = new JSONObject();
		if (device == null) {
			return deviceStatus;
		}
		try {
			deviceStatus.put(
					device.getName() + Constants.LEFT_PARANTHESIS + device.getIp() + Constants.RIGHT_PARANTHESIS,
					device.getDeviceStatus().name());
		} catch (JSONException e) {
			LOGGER.error("Error creating JSON object for device status", e);
		}

		return deviceStatus;
	}

	/**
	 * Retrieves the execution details for generating an HTML report based on the
	 * given execution ID.
	 *
	 * @param executionId the unique identifier of the execution
	 * @return a list of ExecutionDetailsForHtmlReportDTO containing the details of
	 *         the execution
	 * @throws ResourceNotFoundException if the execution or execution results are
	 *                                   not found
	 */
	@Override
	public List<ExecutionDetailsForHtmlReportDTO> getExecutionDetailsForHtmlReport(UUID executionId) {
		LOGGER.info("Fetching execution details for id: {}", executionId);
		Execution execution = executionRepository.findById(executionId)
				.orElseThrow(() -> new ResourceNotFoundException("Execution", "id" + executionId));
		List<ExecutionResult> executionResults = executionResultRepository.findByExecution(execution);
		if (executionResults.isEmpty()) {
			LOGGER.error("No execution results found for execution with id: {}", executionId);
			throw new ResourceNotFoundException("Execution Results", "for Execution ID: " + executionId.toString());
		}
		List<ExecutionDetailsForHtmlReportDTO> executionDetails = new ArrayList<>();
		for (ExecutionResult executionResult : executionResults) {
			ExecutionDetailsForHtmlReportDTO executionDetail = new ExecutionDetailsForHtmlReportDTO();
			executionDetail.setExecutionScriptName(executionResult.getScript());
			executionDetail.setExecutionStatus(executionResult.getResult().name());
			executionDetail.setExecutionLogs(getExecutionLogs(executionResult.getId().toString()));
			executionDetail.setExecutionResultID(executionResult.getId().toString());
			executionDetail.setLogLinkUrl(getLogLinkUrl(executionResult.getId().toString()));
			executionDetails.add(executionDetail);
		}
		return executionDetails;
	}

	/**
	 * Generates a URL for retrieving execution logs based on the provided execution
	 * result ID.
	 *
	 * @param execResultId the ID of the execution result for which the log link is
	 *                     to be generated
	 * @return a string representing the URL to access the execution logs
	 */
	private String getLogLinkUrl(String execResultId) {
		String logLink = appConfig.getBaseURL() + "/execution/getExecutionLogs?executionResultID=" + execResultId;
		return logLink;

	}

	/**
	 * Re-runs the stopped executions after the service restart. This method checks
	 * for executions that were in progress at the time of the service restart, and
	 * attempts to re-run them based on their execution type.
	 */
	public void reRunTheStoppedExecutionsAfterRestart() {
		LOGGER.info("Re-running the stopped executions on restart");
		// Removes all temporary script files created during the past executions
		commonService.removeTemporaryScriptFiles();

		// Fetch all executions that are in progress and created within the last 10 days
		// This is considering the long running executions that might have been
		// interrupted .Calculate the date range
		Instant tenDaysAgo = Instant.now().minusSeconds(10 * 24 * 60 * 60); // 10 days in seconds
		LOGGER.info("Fetching executions in progress from {} to now", tenDaysAgo);
		Instant now = Instant.now();
		LOGGER.info("Current time: {}", now);
		List<Execution> stoppedExecutions = executionRepository
				.findExecutionListInDateRange(ExecutionProgressStatus.INPROGRESS, tenDaysAgo, now);
		if (stoppedExecutions.isEmpty()) {
			LOGGER.info("No stopped executions found to re-run after restart");
			return;
		}

		for (Execution execution : stoppedExecutions) {
			LOGGER.info("Re-running execution: {} with status: {}", execution.getName(),
					execution.getExecutionStatus());
			if (execution.getExecutionType() == ExecutionType.MULTISCRIPT
					|| execution.getExecutionType() == ExecutionType.MULTITESTSUITE
					|| execution.getExecutionType() == ExecutionType.TESTSUITE) {
				LOGGER.info("Re-running multi script execution: {}", execution.getName());
				executionAsyncService.restartMultiScriptExecution(execution);
			} else if (execution.getExecutionType() == ExecutionType.SINGLESCRIPT) {
				LOGGER.info("Re-running single script execution: {}", execution.getName());
				executionAsyncService.restartSingleScriptExecution(execution);
			} else {
				LOGGER.warn("Unsupported execution type: {} for re-run", execution.getExecutionType());
			}
		}
		LOGGER.info("Re-running of stopped executions completed");
	}

	/**
	 * Scheduled task to check and process paused executions. This method runs every
	 * 5 minutes (after an initial delay of 5 seconds) and performs the following:
	 * This helps ensure that paused executions are either resumed or properly
	 * aborted based on their age and device status.
	 */
	@Scheduled(initialDelay = 5000, fixedDelay = 300000)
	public void checkForPausedExecutionsAndResume() {
		LOGGER.info("Scheduled task to check for paused executions started");

		// Checks for paused executions in the last 5 days and resumes them
		// if the associated device is up.
		LOGGER.info("Checking for paused executions in last 5 days");
		Instant fiveDaysAgo = Instant.now().minusSeconds(5 * 24 * 60 * 60);
		List<Execution> pausedExecutionsRecent = executionRepository
				.findByExecutionStatusAndCreatedDateAfter(ExecutionProgressStatus.PAUSED, fiveDaysAgo);
		if (!pausedExecutionsRecent.isEmpty()) {
			handlePausedExecutions(pausedExecutionsRecent);
		}

		// Checks for paused executions older than 5 days and aborts them
		// if the associated execution results are still in progress or pending.
		LOGGER.info("Checking for paused executions before 5 days");
		List<Execution> pausedExecutionsOld = executionRepository
				.findByExecutionStatusAndCreatedDateBefore(ExecutionProgressStatus.PAUSED, fiveDaysAgo);
		if (!pausedExecutionsOld.isEmpty()) {
			handleOldPausedExecutions(pausedExecutionsOld);
		}
	}

	/*
	 * This method handles paused executions that are older than 5 days. It checks
	 * the associated execution results and aborts them if they are still in
	 * progress or pending.
	 */
	private void handleOldPausedExecutions(List<Execution> pausedExecutionsOld) {
		LOGGER.info("Going to handle old paused executions which are aged more than 5 days");
		for (Execution execution : pausedExecutionsOld) {
			LOGGER.warn("Paused execution older than 5 days: {} created at {}", execution.getName(),
					execution.getCreatedDate());

			List<ExecutionResult> executionResults = executionResultRepository.findByExecution(execution);
			if (executionResults.isEmpty()) {
				LOGGER.warn("No execution results found for paused execution: {}", execution.getName());
				continue;
			}
			for (ExecutionResult execResults : executionResults) {
				if (execResults.getResult() == ExecutionResultStatus.INPROGRESS
						|| execResults.getResult() == ExecutionResultStatus.PENDING) {
					execResults.setResult(ExecutionResultStatus.ABORTED);
					execResults.setExecutionRemarks(
							"Execution is auto aborted, as it was paused for more than 5 days and device didn't come up during this period");
					executionResultRepository.save(execResults);
				}
			}
			execution.setExecutionStatus(ExecutionProgressStatus.ABORTED);
			execution.setResult(ExecutionOverallResultStatus.ABORTED);
			executionRepository.save(execution);
			LOGGER.info("Execution aborted for execution name: {}", execution.getName());
		}
	}

	/*
	 * This method handles paused executions that are less than 5 days. It checks
	 * the associated execution results and restarts them if they are still in
	 * progress or pending and the device is up.
	 */
	private void handlePausedExecutions(List<Execution> pausedExecutions) {

		for (Execution execution : pausedExecutions) {
			LOGGER.info("Found paused execution: {}", execution.getName());
			// Check if the device associated with this execution is up
			ExecutionDevice executionDevice = executionDeviceRepository.findByExecution(execution);
			if (executionDevice != null) {
				String deviceName = executionDevice.getDevice();
				Device device = deviceRepository.findByName(deviceName);
				boolean isDeviceUp = false;
				// Get all execution results with status PAUSED

				try {
					DeviceStatus deviceStatus = deviceStatusService.fetchDeviceStatus(device);
					isDeviceUp = (deviceStatus == DeviceStatus.FREE);
				} catch (Exception e) {
					LOGGER.error("Error fetching device status for device: {}", deviceName, e);
				}
				if (isDeviceUp) {
					List<ExecutionResult> pausedResults = executionResultRepository.findByExecution(execution).stream()
							.filter(r -> r.getStatus() == ExecutionStatus.PAUSED)
							.sorted(Comparator.comparing(ExecutionResult::getCreatedDate)).collect(Collectors.toList());
					// Set status of all pausedResults to PENDING and save to DB
					for (ExecutionResult result : pausedResults) {
						result.setResult(ExecutionResultStatus.PENDING);
						executionResultRepository.save(result);
					}

					LOGGER.info("Device {} is UP for paused execution {}", deviceName, execution.getName());
					deviceStatusService.setDeviceStatus(DeviceStatus.IN_USE, device.getName());
					executionAsyncService.executeThePausedExecutions(execution, pausedResults);
				} else {
					LOGGER.warn("Device {} is DOWN for paused execution {}", deviceName, execution.getName());
					// Logic to handle device down scenario
				}
			} else {
				LOGGER.warn("No device associated with paused execution: {}", execution.getName());
			}
		}

	}

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
	@Override
	public JSONObject createFileAndWrite(String execId, String execDevId, String resultId, String test) {
		JSONObject result = new JSONObject();
		try {
			if (execId != null && execDevId != null && resultId != null) {
				try {
					String realPathForLogs = commonService.getBaseLogPath();
					String fileName = realPathForLogs + "/" + execId + "/" + execId + "_" + execDevId + "_" + resultId
							+ "_mvs_applog.txt";
					File file = new File(fileName);
					if (!file.getParentFile().exists()) {
						file.getParentFile().mkdirs();
					}
					if (!file.exists()) {
						file.createNewFile();
						Files.write(file.toPath(), (System.lineSeparator() + test).getBytes(),
								StandardOpenOption.APPEND);
						result.put("Status", "SUCCESS");
						result.put("Remarks", "Created file Name: " + fileName);
					} else {
						Files.write(file.toPath(), (System.lineSeparator() + test).getBytes(),
								StandardOpenOption.APPEND);
						result.put("Status", "SUCCESS");
						result.put("Remarks", "Updated Existing file: " + fileName);
					}
				} catch (Exception e) {
					result.put("Status", "FAILURE");
					result.put("Remarks", "Unable to create file: " + e.getMessage());
				}
			} else {
				result.put("Status", "FAILURE");
				result.put("Remarks", "Unable to create a file execId and resultId empty");
			}
		} catch (JSONException je) {
			LOGGER.error("Error creating JSON object for file creation result", je);
		}
		return result;
	}

	/**
	 * Retrieves the execution ID for a specific execution name.
	 *
	 * @param executionName the name of the execution for which to retrieve the ID
	 * @return the execution ID as UUID, or null if the execution is not found
	 */
	@Override
	public UUID getExecutionId(String executionName) {
		LOGGER.info("Retrieving execution ID for execution name: {}", executionName);
		try {
			Execution execution = executionRepository.findByName(executionName);
			if (execution != null) {
				UUID executionId = execution.getId();
				LOGGER.info("Found execution ID: {} for execution: {}", executionId, execution.getName());
				return executionId;
			} else {
				LOGGER.warn("Execution not found with name: {}", executionName);
				return null;
			}
		} catch (Exception e) {
			LOGGER.error("Error retrieving execution ID for execution name {}: {}", executionName, e.getMessage(), e);
			throw new TDKServiceException("Error retrieving execution ID for execution");
		}
	}

	/**
	 * Retrieves the execution result in JSON format for a given execution name.
	 *
	 * @param executionName the name of the execution for which to retrieve the
	 *                      result
	 * @return a CIRequestDTO object containing the execution result in JSON format
	 * @throws ResourceNotFoundException if the execution with the given name is not
	 *                                   found
	 */
	@Override
	public ResultDTO getExecutionResultInJson(String executionName) {
		LOGGER.info("Fetching execution result in JSON for execution name: {}", executionName);
		Execution execution = executionRepository.findByName(executionName);
		if (execution == null) {
			LOGGER.error("Execution not found with name: {}", executionName);
			throw new ResourceNotFoundException("Execution", executionName);
		}
		String executionId = execution.getId().toString();
		ResultDTO thirdPartyJson = executionAsyncService.getResultJson(executionId, null, null);
		return thirdPartyJson;
	}

	/**
	 * Retrieves device details associated with a specific execution name.
	 *
	 * @param executionName the name of the execution for which to retrieve device
	 *                      details
	 * @return a string containing the device details
	 * @throws ResourceNotFoundException if the execution with the given name is not
	 *                                   found
	 */
	@Override
	public String getDeviceDetailsByExecutionName(String executionName) {
		LOGGER.info("Fetching device details for execution name: {}", executionName);
		Execution execution = executionRepository.findByName(executionName);
		if (execution == null) {
			LOGGER.error("Execution not found with name: {}", executionName);
			throw new ResourceNotFoundException("Execution", executionName);
		}
		String deviceDetails = fileTransferService.getDeviceDetailsFromVersionFile(execution.getId().toString());
		return deviceDetails;
	}

	/**
	 * Retrieves execution details based on a specific date.
	 *
	 * @param date the date for which to retrieve execution details (format:
	 *             YYYY-MM-DD)
	 * @return a list of ExecutionByDateDTO containing execution details for the
	 *         specified date
	 * @throws TDKServiceException if an error occurs while fetching execution
	 *                             details
	 */
	@Override
	public List<ExecutionByDateDTO> getExecutionByDate(String date) {
		LOGGER.info("Fetching execution details based on date: {}", date);
		List<ExecutionByDateDTO> executionByDateDTOList = new ArrayList<>();
		try {
			LocalDate localDate = LocalDate.parse(date);
			Instant startOfDay = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
			Instant endOfDay = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusSeconds(1);

			List<Execution> executions = executionRepository.executionListInDateRange(startOfDay, endOfDay);
			if (executions.isEmpty()) {
				LOGGER.info("No executions found for date: {}", date);
				return executionByDateDTOList;
			}
			for (Execution execution : executions) {
				ExecutionByDateDTO dto = new ExecutionByDateDTO();
				dto.setExecutionName(execution.getName());

				dto.setExecutionStatus(execution.getExecutionStatus().name());
				dto.setExecutionDate(execution.getCreatedDate());
				executionByDateDTOList.add(dto);
			}
			LOGGER.info("Successfully fetched execution details for date: {}", date);
			return executionByDateDTOList;
		} catch (Exception e) {
			LOGGER.error("Error fetching execution details for date: {}", date, e);
			throw new TDKServiceException("Error fetching execution details for date: " + date);
		}
	}

	/**
	 * Retrieves the execution timeout for a specific script.
	 *
	 * @param scriptName the name of the script for which to retrieve the timeout
	 * @return the execution timeout in seconds, or null if the script is not found
	 */
	@Override
	public Integer getScriptExecutionTimeout(String scriptName) {
		LOGGER.info("Retrieving execution timeout for script name: {}", scriptName);
		try {
			Script script = scriptRepository.findByName(scriptName);
			if (script != null) {
				Integer timeout = script.getExecutionTimeOut();
				LOGGER.info("Found execution timeout: {} seconds for script: {}", timeout, script.getName());
				return timeout;
			} else {
				LOGGER.warn("Script not found with name: {}", scriptName);
				return null;
			}
		} catch (Exception e) {
			LOGGER.error("Error retrieving execution timeout for script name {}: {}", scriptName, e.getMessage(), e);
			throw new TDKServiceException("Error retrieving execution timeout for script");
		}
	}

}
