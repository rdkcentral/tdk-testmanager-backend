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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.internal.Utils;
import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.dto.ComponentLevelDTO;
import com.rdkm.tdkservice.dto.DeviceDetailsDTO;
import com.rdkm.tdkservice.dto.ResultDTO;
import com.rdkm.tdkservice.dto.DetailedResultDTO;
import com.rdkm.tdkservice.dto.ScriptDetailsDTO;
import com.rdkm.tdkservice.dto.TestInfoDTO;
import com.rdkm.tdkservice.enums.DeviceStatus;
import com.rdkm.tdkservice.enums.ExecutionOverallResultStatus;
import com.rdkm.tdkservice.enums.ExecutionProgressStatus;
import com.rdkm.tdkservice.enums.ExecutionResultStatus;
import com.rdkm.tdkservice.enums.ExecutionStatus;
import com.rdkm.tdkservice.enums.ExecutionType;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.model.Device;
import com.rdkm.tdkservice.model.Execution;
import com.rdkm.tdkservice.model.ExecutionDevice;
import com.rdkm.tdkservice.model.ExecutionEntities;
import com.rdkm.tdkservice.model.ExecutionMethodResult;
import com.rdkm.tdkservice.model.ExecutionResult;
import com.rdkm.tdkservice.model.ExecutionResultAnalysis;
import com.rdkm.tdkservice.model.Script;
import com.rdkm.tdkservice.repository.DeviceRepositroy;
import com.rdkm.tdkservice.repository.ExecutionDeviceRepository;
import com.rdkm.tdkservice.repository.ExecutionMethodResultRepository;
import com.rdkm.tdkservice.repository.ExecutionRepository;
import com.rdkm.tdkservice.repository.ExecutionResultAnalysisRepository;
import com.rdkm.tdkservice.repository.ExecutionResultRepository;
import com.rdkm.tdkservice.repository.ScriptRepository;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.service.utilservices.PythonLibraryScriptExecutorService;
import com.rdkm.tdkservice.service.utilservices.ScriptExecutorService;
import com.rdkm.tdkservice.util.Constants;

/**
 * This class is used to execute the scripts asynchronously.
 */
@Service
public class ExecutionAsyncService {

	@Autowired
	private ScriptExecutorService scriptExecutorService;

	@Autowired
	private ExecutionRepository executionRepository;

	@Autowired
	private ExecutionResultRepository executionResultRepository;

	@Autowired
	private ExecutionMethodResultRepository executionMethodResultRepository;

	@Autowired
	private ExecutionResultAnalysisRepository executionResultAnalysisRepository;

	@Autowired
	private ExecutionDeviceRepository executionDeviceRepository;

	@Autowired
	private ScriptRepository scriptRepository;

	@Autowired
	private DeviceRepositroy deviceRepository;

	@Autowired
	private CommonService commonService;

	@Autowired
	private HttpService httpService;

	@Autowired
	private AppConfig appConfig;

	@Autowired
	private DeviceStatusService deviceStatusService;

	@Autowired
	private FileTransferService fileTransferService;

	@Autowired
	private PythonLibraryScriptExecutorService pythonLibraryScriptExecutorService;

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionService.class);

	/**
	 * This method is used to execute a single script on a device asynchronously.
	 * 
	 * @param device           This is the device on which the script should be
	 *                         executed.
	 * @param script           This is the script that should be executed.
	 * @param user             This is the user who triggered the execution.
	 * @param executionName    This is the name of the execution.
	 * @param category         This is the category of the device.
	 * @param repeatCount      This is the number of times the script should be
	 *                         repeated.
	 * @param isRerunOnFailure This is a boolean flag to indicate if the script
	 *                         should be rerun on failure.
	 */
	@Async
	public void prepareAndExecuteSingleScript(Device device, Script script, String user, String executionName,
			int repeatCount, boolean isRerunOnFailure, boolean isDeviceLogsNeeded, boolean isPerformanceLogsNeeded,
			boolean isDiagnosticLogsNeeded, String testType, String callBackUrl, String imageVersion) {
		LOGGER.info("Going to execute script: {} on device: {}", script.getName(), device.getName());

		try {
			String realExecutionName = "";

			// repeatCount means how many times the execution needs to be done
			// If 0 is added by the user , then the execution won't happen
			// So if the repeatcount is 0, then it is defaulted to 1
			if (repeatCount == 0) {
				repeatCount = 1;
			}
			for (int i = 0; i < repeatCount; i++) {

				// Busy lock in the database device entity before execution
				deviceStatusService.setDeviceStatus(DeviceStatus.IN_USE, device.getName());
				if (i == 0) {
					realExecutionName = executionName;
				} else {
					// For repeat of the base execution, add a suffix R1, R2, R3 etc
					realExecutionName = executionName + "_R" + i;
				}
				// Start time is stored
				double executionStartTime = System.currentTimeMillis();

				// The set of objects required for the executing the script are added here.
				// for concise code
				ExecutionEntities executionEntities = this.getExecutionEntitiesForExecution(device, script, user,
						realExecutionName, isRerunOnFailure, isDeviceLogsNeeded, isPerformanceLogsNeeded,
						isDiagnosticLogsNeeded, testType);
				// Transfer the version File
				this.transferVersionFileOfTheDevice(executionEntities.getExecution().getId().toString(), device,
						executionEntities.getExecutionDevice().getId().toString());

				boolean executionStatus = executeScriptinDevice(script, executionEntities.getExecution(),
						executionEntities.getExecutionResult().get(0), executionEntities.getExecutionDevice());
				double executionEndTime = System.currentTimeMillis();
				double executionTime = this
						.roundOfToThreeDecimals(this.computeTimeDifference(executionStartTime, executionEndTime));

				double realExecutionTime = this.getRealExcecutionTime(executionEntities.getExecutionResult());
				this.setExecutionTime(executionEntities.getExecution(), executionTime, realExecutionTime);
				Execution finalExecutionStatus = this.setFinalStatusOfExecution(executionEntities.getExecution());
				this.callCiRequest(finalExecutionStatus, callBackUrl, imageVersion);

				// TODO : Execution Status logic cross check once again
				if (isRerunOnFailure && !executionStatus) {
					// For the rerun on Failure, add _RERUN in the execution name
					realExecutionName = realExecutionName + Constants.RERUN_APPENDER;
					executionStartTime = System.currentTimeMillis();
					ExecutionEntities executionEntitiesForFailureRerun = this.getExecutionEntitiesForExecution(device,
							script, user, realExecutionName, isRerunOnFailure, isDeviceLogsNeeded,
							isPerformanceLogsNeeded, isDiagnosticLogsNeeded, testType);

					executeScriptinDevice(script, executionEntitiesForFailureRerun.getExecution(),
							executionEntitiesForFailureRerun.getExecutionResult().get(0),
							executionEntitiesForFailureRerun.getExecutionDevice());

					this.transferVersionFileOfTheDevice(
							executionEntitiesForFailureRerun.getExecution().getId().toString(), device,
							executionEntities.getExecutionDevice().getId().toString());

					executionEndTime = System.currentTimeMillis();
					executionTime = this
							.roundOfToThreeDecimals(this.computeTimeDifference(executionStartTime, executionEndTime));
					realExecutionTime = this
							.getRealExcecutionTime(executionEntitiesForFailureRerun.getExecutionResult());
					this.setExecutionTime(executionEntities.getExecution(), executionTime, realExecutionTime);
					Execution execData = this
							.setFinalStatusOfExecution(executionEntitiesForFailureRerun.getExecution());
					this.callCiRequest(execData, callBackUrl, imageVersion);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Unlock the device with the current status
			deviceStatusService.fetchAndUpdateDeviceStatus(device);
			LOGGER.error("Error in executing script: {} on device: {} , due to the exception", script.getName(),
					device.getName(), e);
		}
		// Unlock the device with the current status
		deviceStatusService.fetchAndUpdateDeviceStatus(device);

	}

	/**
	 * This method is used to call ci request
	 * 
	 * @param finalExecutionStatus - the final execution status
	 * @param callBackUrl          - the call back url
	 * @param imageVersion         - the image
	 * 
	 */
	private void callCiRequest(Execution finalExecutionStatus, String callBackUrl, String imageVersion) {
		if (!finalExecutionStatus.getTestType().contains("CI")) {
			return;
		}

		ResultDTO request = getResultJson(finalExecutionStatus.getId().toString(), imageVersion, "CI");
		LOGGER.info("CI Jsonobject: {}", request);

		if (callBackUrl == null || callBackUrl.isEmpty()) {
			callBackUrl = getCallBackUrlFromConfig();
		}

		if (callBackUrl == null) {
			LOGGER.error("CallBack url not found in tm.config file");
			throw new TDKServiceException("CallBack url not found in tm.config file");
		}

		try {
			httpService.sendPostRequest(callBackUrl, request, null);
		} catch (Exception e) {
			LOGGER.error("Error occurred while sending the request to the CI server", e.getMessage());
		}
	}

	/*
	 * This method is used to get the call back url from the config file
	 * 
	 * @return String - the call back url
	 */
	private String getCallBackUrlFromConfig() {
		String configFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + Constants.TM_CONFIG_FILE;
		return commonService.getConfigProperty(new File(configFilePath), Constants.CI_CALLBACK_URL);
	}

	/**
	 * This method is used to set the execution time
	 * 
	 * @param executionObject   - the execution object
	 * @param executionTime     - the execution time
	 * @param executionRealTime - the real time for script execution
	 */
	private void setExecutionTime(Execution executionObject, double executionTime, double executionRealTime) {
		Execution execution = executionRepository.findById(executionObject.getId()).orElse(null);
		execution.setExecutionTime(executionTime);
		execution.setRealExecutionTime(executionRealTime);
		executionRepository.save(execution);

	}

	/**
	 * This method is used to execute multiple scripts on a device asynchronously.
	 * 
	 * @param device        This is the device on which the scripts should be
	 *                      executed.
	 * @param scriptList    This is the list of scripts that should be executed
	 * @param user          This is the user who triggered the execution.
	 * @param executionName This is the name
	 * @param category      This is the category of the device.
	 * @param testSuiteName This is the name of the test suite.
	 * 
	 */
	@Async
	public void prepareAndExecuteMultiScript(Device device, List<Script> scriptList, String user, String executionName,
			String category, String testSuiteName, int repeatCount, boolean isRerunOnFailure,
			boolean isDeviceLogsNeeded, boolean isDiagnosticsLogsNeeded, boolean isPerformanceNeeded,
			boolean isIndividualRepeatExecution, String testType, String callBackUrl, String imageVersion) {
		LOGGER.info("Executing multiple scripts execution in device:" + device.getName());
		try {
			// repeatCount means how many times the execution needs to be done
			// If 0 is added by the user , then the execution won't happen
			// So if the repeatcount is 0, then it is defaulted to 1
			if (repeatCount == 0) {
				repeatCount = 1;
			}

			boolean pauseExecution = false;
			for (int repeatIndex = 0; repeatIndex < repeatCount; repeatIndex++) {
				deviceStatusService.setDeviceStatus(DeviceStatus.IN_USE, device.getName());

				String currentExecutionName = executionName;

				// Use the provided execution name for the first execution
				if (repeatIndex == 0) {
					currentExecutionName = executionName;
				} else {
					// Append _R<i> for subsequent executions, starting from _R1
					currentExecutionName = executionName + "_R" + repeatIndex;
				}
				LOGGER.info("Starting  multiscript execution: {}", currentExecutionName);

				List<Script> applicableScripts = new ArrayList<>();
				List<Script> invalidScripts = new ArrayList<>();

				for (Script script : scriptList) {
					if ((commonService.validateScriptDeviceDeviceType(device, script))
							&& (commonService.vaidateScriptDeviceCategory(device, script))
							&& !commonService.isScriptMarkedToBeSkipped(script)) {
						applicableScripts.add(script);
					} else {
						invalidScripts.add(script);
					}
				}

				double executionStartTime = System.currentTimeMillis();

				// Create and save Execution for the device
				Execution execution = new Execution();
				execution.setCategory(device.getCategory());
				if (testSuiteName == null) {
					execution.setExecutionType(ExecutionType.MULTISCRIPT);
					execution.setScripttestSuiteName("Multiple Scripts");
				} else if (Constants.MULTI_TEST_SUITE.equals(testSuiteName)) {
					execution.setExecutionType(ExecutionType.MULTITESTSUITE);
					execution.setScripttestSuiteName(testSuiteName);
				} else {
					execution.setExecutionType(ExecutionType.TESTSUITE);
					execution.setScripttestSuiteName(testSuiteName);
				}
				execution.setName(currentExecutionName);
				execution.setResult(ExecutionOverallResultStatus.INPROGRESS);
				execution.setExecutionStatus(ExecutionProgressStatus.INPROGRESS);
				execution.setTestType(testType);
				execution.setDeviceLogsNeeded(isDeviceLogsNeeded);
				execution.setDiagnosticLogsNeeded(isDiagnosticsLogsNeeded);
				execution.setPerformanceLogsNeeded(isPerformanceNeeded);
				execution.setUser(user);
				execution.setRepeatCount(repeatCount);
				execution.setRerunOnFailure(isRerunOnFailure);

				Execution savedExecution = executionRepository.save(execution);

				// Create and save ExecutionDevice
				ExecutionDevice executionDevice = new ExecutionDevice();
				executionDevice.setDevice(device.getName());
				executionDevice.setDeviceIp(device.getIp());
				executionDevice.setDeviceMac(device.getMacId());
				executionDevice.setDeviceType(device.getDeviceType().getName());
				executionDevice.setExecution(savedExecution);
				String executionID = savedExecution.getId().toString();

				ExecutionDevice savedExecutionDevice = executionDeviceRepository.save(executionDevice);

				List<ExecutionResult> execResultList = new ArrayList<>();
				List<ExecutionResult> inValidExecutionResults = new ArrayList<>();
				if (!invalidScripts.isEmpty()) {
					inValidExecutionResults = handleInvalidScripts(invalidScripts, execution, device);
				}

				List<ExecutionResult> executableResultList = new ArrayList<>();
				if (!applicableScripts.isEmpty()) {
					executableResultList = handleApplicableScripts(applicableScripts, device, execution,
							executionDevice, isIndividualRepeatExecution, repeatCount);
				}

				execResultList.addAll(inValidExecutionResults);
				execResultList.addAll(executableResultList);
				execution.setExecutionResults(execResultList);
				executionRepository.save(execution);

				UUID executionId = execution.getId();
				this.transferVersionFileOfTheDevice(executionID, device, savedExecutionDevice.getId().toString());
				int executedScript = 0;

				for (ExecutionResult execRes : executableResultList) {
					if (this.isExecutionAborted(executionId)) {
						LOGGER.info("Execution aborted for device: {}", device.getName());
						for (ExecutionResult execResults : executableResultList) {
							if (execResults.getResult() == ExecutionResultStatus.INPROGRESS
									|| execResults.getResult() == ExecutionResultStatus.PENDING) {
								execResults.setResult(ExecutionResultStatus.ABORTED);
								execResults.setExecutionRemarks("Execution aborted by user");
								executionResultRepository.save(execResults);
							}
						}
						Execution executionAborted = executionRepository.findById(executionId).orElse(null);
						executionAborted.setExecutionStatus(ExecutionProgressStatus.ABORTED);
						executionAborted.setResult(ExecutionOverallResultStatus.ABORTED);
						executionRepository.save(executionAborted);
						LOGGER.info("Execution aborted for device: {}", device.getName());
						break;
					}
					execRes.setExecutionRemarks(
							"Executing script: " + execRes.getScript() + " on device: " + device.getName());
					executionResultRepository.save(execRes);
					Script script = scriptRepository.findByName(execRes.getScript());
					boolean executionResult = executeScriptinDevice(script, execution, execRes, executionDevice);

					if (executionResult) {
						LOGGER.info("Execution result success for {} on device: {}", script.getName(),
								device.getName());
					} else {
						LOGGER.info("Execution result failed for {} on device: {}", script.getName(), device.getName());
					}

					executedScript++;
					Execution executionCompleted = executionRepository.findById(executionId).orElse(null);
					double currentExecTime = System.currentTimeMillis();
					double executionTime = this.computeTimeDifference(executionStartTime, currentExecTime);
					executionCompleted.setExecutionTime(executionTime);
					executionRepository.save(executionCompleted);

					// After the execution is completed, check the device status
					DeviceStatus currentStatus = deviceStatusService.fetchDeviceStatus(device);
					LOGGER.info("Current device status after script execution: {}", currentStatus);
					if (currentStatus == DeviceStatus.NOT_FOUND || currentStatus == DeviceStatus.HANG) {
						LOGGER.warn("Device status is not found or hang for device: {}", device.getName());
						// If the device is not found or hang, pause the execution
						// and update the execution status to paused
						pauseExecution = true;
						break;
					}

				}

				Execution finalExecution = executionRepository.findById(executionId).orElse(null);

				if (executableResultList != null) {
					double realExecTime = this.getRealExcecutionTime(executableResultList);
					double roundOfValue = this.roundOfToThreeDecimals(realExecTime);
					finalExecution.setRealExecutionTime(roundOfValue);
				}

				double executionEndTime = System.currentTimeMillis();
				double executionTime = this.computeTimeDifference(executionStartTime, executionEndTime);
				finalExecution.setExecutionTime(executionTime);
				executionRepository.save(finalExecution);

				if (pauseExecution) {
					LOGGER.info("Device status is not found or hang, pausing execution for device: {}",
							device.getName());
					this.setExecutiontoPausedState(execution);
					break;
				}
				Execution finalExecutionStatus = this.setFinalStatusOfExecution(finalExecution);
				this.callCiRequest(finalExecutionStatus, callBackUrl, imageVersion);

				if (isRerunOnFailure && (finalExecutionStatus.getResult() == ExecutionOverallResultStatus.FAILURE)) {
					List<String> failedScriptName = executableResultList.stream()
							.filter(result -> result.getResult() == ExecutionResultStatus.FAILURE)
							.map(ExecutionResult::getScript).toList();
					List<Script> failedScripts = failedScriptName.stream().map(scriptRepository::findByName).toList();
					if (!failedScripts.isEmpty()) {
						String rerunExecutionName = currentExecutionName + "_RERUN";
						LOGGER.info("Starting Rerun due to failure: execution name: {}", rerunExecutionName);
						prepareAndExecuteMultiScript(device, failedScripts, user, rerunExecutionName, category,
								testSuiteName, 1, false, isDeviceLogsNeeded, isDiagnosticsLogsNeeded,
								isPerformanceNeeded, isIndividualRepeatExecution, testType, callBackUrl, imageVersion);
					}
				}

				if (isIndividualRepeatExecution) {
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Unlock the device with the current status
			deviceStatusService.fetchAndUpdateDeviceStatus(device);
			LOGGER.error("Error in executing scripts: {} on device: {}", device.getName());
			throw new TDKServiceException("Error in executing scripts: " + " on device: " + device.getName());
		} finally {
			LOGGER.info("Execution completed in device: {}  for the execution: {}, updating the status",
					device.getName(), executionName);

			// Unlock the device with the current status
			deviceStatusService.fetchAndUpdateDeviceStatus(device);
		}

	}

	/**
	 * Method to transfer version file from the device, get the image name and store
	 * in the Execution Device object
	 * 
	 * @param executionID       - ID of the Execution ID object
	 * @param device            - Device object
	 * @param executionDeviceID - ID of the Execution ID object
	 */
	private void transferVersionFileOfTheDevice(String executionID, Device device, String executionDeviceID) {
		boolean isVersionFileTransfered = fileTransferService.getVersionFileForTheDevice(executionID, device);
		if (!isVersionFileTransfered) {
			LOGGER.warn("Version file is not transferred for the device: {}", device.getName());
		}
		String buildName = fileTransferService.getImageName(executionID);
		if (Utils.isEmpty(buildName)) {
			buildName = Constants.BUILD_NAME_FAILED;
		}
		ExecutionDevice executionDevice = executionDeviceRepository.findById(UUID.fromString(executionDeviceID)).get();

		executionDevice.setBuildName(buildName);
		executionDeviceRepository.save(executionDevice);

	}

	/**
	 * Generates a resultDTO for the RDK Portal based on the provided execution ID.
	 *
	 * @param executionId the ID of the execution to retrieve the CI request for
	 * @return a resultDTO containing details about the execution, including device
	 *         and component level details
	 *
	 */
	public ResultDTO getResultJson(String executionId, String imageVersion, String testType) {
		LOGGER.info("Getting CI request for RDK Portal");
		Execution execution = executionRepository.findById(UUID.fromString(executionId)).orElse(null);
		String baseUrl = appConfig.getBaseURL() + "/execution/getExecutionLogs?executionResultID=";
		ResultDTO resultDTO = new ResultDTO();
		resultDTO.setService(Constants.TDK_PORTAL_SERVICE);
		resultDTO.setStarted_at(getEpochTime(execution.getCreatedDate()));
		if (testType.equals("CI")) {
			resultDTO.setStarted_by("RDKPortal/Jenkins");
		}
		resultDTO.setStatus(execution.getResult().name());
		resultDTO.setDuration(getExecutionDurationInEpoch(execution.getExecutionTime()));

		ArrayList<DetailedResultDTO> resultDTOList = new ArrayList<>();
		DetailedResultDTO detailedResultDTO = new DetailedResultDTO();
		detailedResultDTO.setExecutionName(execution.getName());
		detailedResultDTO.setExecutionStatus(execution.getExecutionStatus().name());
		detailedResultDTO.setScriptOrTestSuite(execution.getScripttestSuiteName());

		ArrayList<DeviceDetailsDTO> deviceDTOList = new ArrayList<>();
		ArrayList<Object> systemInfoList = new ArrayList<>();
		ExecutionDevice executionDevice = executionDeviceRepository.findByExecution(execution);
		if (executionDevice != null) {
			DeviceDetailsDTO deviceDetailsDTO = new DeviceDetailsDTO();
			if (testType.equals("CI")) {
				deviceDetailsDTO.setDevice(getDeviceNameFromConfigFile(imageVersion));
				deviceDetailsDTO.setImageName(imageVersion);
			} else {
				deviceDetailsDTO.setDevice(executionDevice.getDevice());
				deviceDetailsDTO.setImageName(executionDevice.getBuildName());
			}
			Device device = deviceRepository.findByName(executionDevice.getDevice());
			deviceDetailsDTO.setDeviceType(device.getDeviceType().getName());

			ArrayList<ComponentLevelDTO> componentLevelDTOList = new ArrayList<>();
			List<ExecutionResult> executionResults = execution.getExecutionResults();

			Map<String, List<ExecutionResult>> moduleScriptMap = new HashMap<>();
			for (ExecutionResult result : executionResults) {
				String moduleName = scriptRepository.findByName(result.getScript()).getModule().getName();
				moduleScriptMap.computeIfAbsent(moduleName, k -> new ArrayList<>()).add(result);
			}

			for (Map.Entry<String, List<ExecutionResult>> entry : moduleScriptMap.entrySet()) {
				String moduleName = entry.getKey();
				List<ExecutionResult> scriptResults = entry.getValue();

				ComponentLevelDTO componentLevelDTO = new ComponentLevelDTO();
				componentLevelDTO.setModuleName(moduleName);
				boolean moduleStatus = true;
				ArrayList<ScriptDetailsDTO> scriptDetailsDTOList = new ArrayList<>();
				for (ExecutionResult result : scriptResults) {
					ScriptDetailsDTO scriptDetailsDTO = new ScriptDetailsDTO();
					scriptDetailsDTO.setScriptName(result.getScript());
					scriptDetailsDTO.setScriptStatus(result.getResult().name());
					scriptDetailsDTO.setLogUrl(baseUrl + result.getId().toString());
					if (result.getResult().name().equals(ExecutionResultStatus.FAILURE.name())) {
						moduleStatus = false;
					}

					ArrayList<TestInfoDTO> ciTestInfoDTOList = new ArrayList<>();
					if (moduleName.equals("rdkservices")) {
						List<ExecutionMethodResult> executionMethodResults = executionMethodResultRepository
								.findByExecutionResult(result);
						for (ExecutionMethodResult methodResult : executionMethodResults) {
							TestInfoDTO ciTestInfoDTO = new TestInfoDTO();
							ciTestInfoDTO.setTestCaseName(methodResult.getFunctionName());
							ciTestInfoDTO.setTestCaseStatus(methodResult.getActualResult().name());
							ciTestInfoDTOList.add(ciTestInfoDTO);
						}
					}
					scriptDetailsDTO.setTestInfo(ciTestInfoDTOList.isEmpty() ? new ArrayList<>() : ciTestInfoDTOList);

					scriptDetailsDTOList.add(scriptDetailsDTO);
				}

				if (moduleStatus) {
					componentLevelDTO.setModuleStatus(ExecutionResultStatus.SUCCESS.name());
				} else {
					componentLevelDTO.setModuleStatus(ExecutionResultStatus.FAILURE.name());
				}
				componentLevelDTO.setScriptDetails(scriptDetailsDTOList);
				componentLevelDTOList.add(componentLevelDTO);
			}
			deviceDetailsDTO.setComponentLevelDetails(componentLevelDTOList);
			deviceDetailsDTO.setSystemLevelDetails(systemInfoList);
			deviceDTOList.add(deviceDetailsDTO);
		}

		detailedResultDTO.setDeviceDetails(deviceDTOList.isEmpty() ? new ArrayList<>() : deviceDTOList);
		resultDTOList.add(detailedResultDTO);
		resultDTO.setResult(resultDTOList.isEmpty() ? new ArrayList<>() : resultDTOList);

		return resultDTO;
	}

	/*
	 * The method is used to get the Device corresponding to the image version from
	 * config file
	 * 
	 * @param imageVersion - the image version
	 * 
	 * @return - the device
	 */
	private String getDeviceNameFromConfigFile(String imageVersion) {
		String configFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
				+ Constants.CI_IMAGE_BOXTYPE_CONFIG_FILE;
		List<Map<String, String>> boxTypeMap = parseConfigFile(configFilePath);

		for (Map<String, String> boxType : boxTypeMap) {
			for (Map.Entry<String, String> entry : boxType.entrySet()) {
				if (entry.getValue() != null && entry.getValue().contains(imageVersion)) {
					return entry.getKey();
				}
			}
		}
		throw new TDKServiceException("Device name not found for image version: " + imageVersion);
	}

	/**
	 * This method is used to create a map of Device and Corresponding Image Version
	 * keyWord From Config file
	 * 
	 * @param filePath - the file path
	 * @return - the list of map of device and image version keyword
	 */
	public static List<Map<String, String>> parseConfigFile(String filePath) {
		List<Map<String, String>> configMaps = new ArrayList<>();
		Map<String, String> configMap = new HashMap<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				// Trim and skip empty or comment lines
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}

				// Split the line by ':'
				String[] parts = line.split("=", 2);
				if (parts.length == 2) {
					String key = parts[0].trim();
					String value = parts[1].trim();
					configMap.put(key, value);
				}
			}

			// Add the map to the list if it's not empty
			if (!configMap.isEmpty()) {
				configMaps.add(configMap);
			}

		} catch (Exception e) {
			LOGGER.error("Error reading config file: {}", filePath, e);
			throw new TDKServiceException("Error reading config file: " + filePath);
		}

		return configMaps;
	}

	/**
	 * Converts the given execution time in minutes to epoch time in seconds.
	 *
	 * @param executionTime the execution time in minutes
	 * @return the execution time in epoch seconds as a String
	 */
	private String getExecutionDurationInEpoch(double executionTime) {
		// convert double to epoch time
		long epochTime = (long) (executionTime * 60);
		return String.valueOf(epochTime);

	}

	/**
	 * Converts the given Instant to its epoch time in milliseconds as a String.
	 *
	 * @param createdDate the Instant to be converted
	 * @return the epoch time in milliseconds as a String
	 */
	private String getEpochTime(Instant createdDate) {
		return String.valueOf(createdDate.toEpochMilli());

	}

	/**
	 * Checks if the execution with the given ID has been aborted.
	 *
	 * @param executionId the unique identifier of the execution
	 * @return {@code true} if the execution has an abort request, {@code false}
	 *         otherwise
	 */
	private boolean isExecutionAborted(UUID executionId) {
		Execution execution = executionRepository.findById(executionId).orElse(null);
		if (execution == null) {
			return false;
		}
		return execution.isAbortRequested();
	}

	/**
	 * The method is used to execute the script in the device
	 * 
	 * @param script          - the script to be executed
	 * @param execution       - the execution object
	 * @param executionResult - the execution result object
	 * @param executionDevice - the execution device object
	 * @return boolean - true if the script is executed successfully, false
	 *         otherwise
	 */
	private boolean executeScriptinDevice(Script script, Execution execution, ExecutionResult executionResult,
			ExecutionDevice executionDevice) {
		String output = "";
		LOGGER.info("Executing script: {} on device: {}", script.getName(), executionDevice.getDevice());
		Device device = deviceRepository.findByName(executionDevice.getDevice());
		StringBuilder remarksString = new StringBuilder();
		remarksString.append("Execution started in device: ").append(device.getName()).append(" for script: ")
				.append(script.getName()).append("\n");
		executionResult.setExecutionRemarks(remarksString.toString());
		executionResult.setResult(ExecutionResultStatus.INPROGRESS);
		executionResult.setStatus(ExecutionStatus.INPROGRESS);
		executionResult.setDateOfExecution(Instant.now());
		executionResultRepository.save(executionResult);
		ExecutionResultStatus finalExecutionResultStatus = ExecutionResultStatus.FAILURE;
		try {
			String basePathForFileStore = AppConfig.getBaselocation();
			String scriptPath = script.getScriptLocation();
			String absoluteScriptFilePath = basePathForFileStore + Constants.FILE_PATH_SEPERATOR + scriptPath
					+ Constants.FILE_PATH_SEPERATOR + script.getName() + Constants.PYTHON_FILE_EXTENSION;
			String temporaryScriptPath = basePathForFileStore + Constants.FILE_PATH_SEPERATOR
					+ Constants.TEMP_SCRIPT_FILE_KEYWORD + Constants.UNDERSCORE + script.getName()
					+ Constants.UNDERSCORE + System.currentTimeMillis() + Constants.PYTHON_FILE_EXTENSION;

			// Read the content of the original python script
			String content = null;
			content = readFile(absoluteScriptFilePath);
			if (content == null || content.isEmpty()) {
				remarksString
						.append("There is something wrong with the script, Please check the script is there or not\n");
				executionResult.setExecutionRemarks(remarksString.toString());
				executionResult.setResult(ExecutionResultStatus.FAILURE);
				executionResult.setStatus(ExecutionStatus.COMPLETED);
				executionResultRepository.save(executionResult);
				return false;
			}

			// Prepare replacements and modify content
			Map<String, String> replacements = new HashMap<>();
			replacements.put(Constants.PORT_REPLACE_TOKEN, device.getPort());
			replacements.put(Constants.IP_REPLACE_TOKEN, "\"" + device.getIp() + "\"");
			replacements.put(Constants.CONFIGURE_TESTCASE_REPLACE_TOKEN,
					prepareReplacementString(device, execution, executionDevice, executionResult, script));
			String modifiedContent = modifyContent(content, replacements);

			// Append the script end token to the modified content, to identify the
			// end of the script execution
			modifiedContent += Constants.SCRIPT_END_PY_CODE;
			// Write the modified content to a temporary file
			writeFile(temporaryScriptPath, modifiedContent);

			// If the script is marked as long duration, then there is no timeout
			// for the scripts execution
			int waittime = 0;
			if (!script.isLongDuration()) {
				waittime = script.getExecutionTimeOut();
			}
			// A standardized path for storing the logs
			String executionLogfile = commonService.getExecutionLogFilePath(execution.getId().toString(),
					executionResult.getId().toString());

			LOGGER.info("Execution log file path: " + executionLogfile);

			String[] commands = { commonService.getPythonCommandFromConfig(), temporaryScriptPath };

			double currentTimeMillisBeforeExecution = System.currentTimeMillis();

			output = scriptExecutorService.executeTestScript(commands, waittime, executionLogfile);

			fileTransferService.moveAndRenameScriptGeneratedFiles(execution, executionResult, executionDevice);

			double currentTimeMillisAfterExecution = System.currentTimeMillis();

			// Finding the execution time and converts to minutes
			double executiontime = this.computeTimeDifference(currentTimeMillisBeforeExecution,
					currentTimeMillisAfterExecution);
			executionResult.setExecutionTime(executiontime);

			LOGGER.debug("Execution output: " + output);

			// Update the execution result based on the output

			// If there is a TDK_error string , add it as failure
			if (output.contains(Constants.ERROR_TAG_PY_COMMENT)) {
				LOGGER.info(
						"There is TDK error string in the log, So it can be an exception or error from the python framework");
				executionResult.setResult(ExecutionResultStatus.FAILURE);
				executionResultRepository.save(executionResult);
				if (!device.isThunderEnabled()) {
					LOGGER.info("The device is TDK enabled, So after the error, the TDK agent needs to be reset");
					pythonLibraryScriptExecutorService.resetAgentForTDKDevices(device.getIp(), device.getPort(),
							Constants.TRUE);
				}
			} else if (output.contains("Pre-Condition not met")) {
				LOGGER.info("Precondition failure happened");
				executionResult.setResult(ExecutionResultStatus.FAILURE);
				executionResultRepository.save(executionResult);
			} else {

				if (output.contains(Constants.END_TAG_PY_COMMENT)) {
					// The execution result is parallely set from the python framework via APIS
					// that is added here
					ExecutionResult finalExecutionResult = executionResultRepository.findById(executionResult.getId())
							.get();
					finalExecutionResultStatus = finalExecutionResult.getResult();

					// When the result status was not parallely set from the
					// python framework but the execution was completed, due to someissue
					// in the python framework. In that case the result status is not set to SUCCESS
					// FAILURE ,In that case, the result status is set to FAILURE
					if (finalExecutionResultStatus == ExecutionResultStatus.INPROGRESS) {
						finalExecutionResultStatus = ExecutionResultStatus.FAILURE;
						executionResult.setExecutionRemarks(remarksString
								+ "Script execution completed with status FAILURE, because the script execution got finished.But no status was set from the python framework or script"
								+ finalExecutionResultStatus);
					}
					executionResult.setResult(finalExecutionResultStatus);
					executionResultRepository.save(executionResult);

				} else {
					// If the script output does not contain the script end token,
					// then the script execution is considered as interrupted in between due
					// to timeout or either any abrupt failure
					if ((executiontime >= waittime) && (waittime != 0)) {
						LOGGER.info("The script execution is interrupted due to timeout");
						executionResult.setResult(ExecutionResultStatus.TIMEOUT);
						executionResultRepository.save(executionResult);
						if (!device.isThunderEnabled()) {
							LOGGER.info(
									"The device is TDK enabled, the TDK agent needs to be reset, other wise the device status will be in busy state");
							pythonLibraryScriptExecutorService.resetAgentForTDKDevices(device.getIp(), device.getPort(),
									Constants.TRUE);
						}
					} else {
						executionResult.setResult(ExecutionResultStatus.FAILURE);
						if (!device.isThunderEnabled()) {
							LOGGER.info(
									"The device is TDK enabled, the TDK agent needs to be reset, other wise the device status will be in busy state");
							pythonLibraryScriptExecutorService.resetAgentForTDKDevices(device.getIp(), device.getPort(),
									Constants.FALSE);
						}
					}
					executionResultRepository.save(executionResult);

				}

			}

			// Delete the temporary script file after execution
			deleteTemporaryFile(temporaryScriptPath);
			// remove the unwanted comments in the console log like TDK_ERROR which were
			// added for the result status reading
			this.removeFWRequiredTextsFromLogs(executionLogfile);

			if (!device.isThunderEnabled()) {
				fileTransferService.transferAgentLogs(device, execution.getId().toString(),
						executionResult.getId().toString());
			}

			if (execution.isDeviceLogsNeeded()) {
				fileTransferService.transferDeviceLogs(execution, executionResult, device);
			}

			if (execution.isDiagnosticLogsNeeded() && device.isThunderEnabled()) {
				fileTransferService.transferDiagnosisLogs(execution, executionResult, device);
			}

			// transfer crash logs
			fileTransferService.transferCrashLogs(execution, executionResult, device);

			ExecutionResult finalExecutionResult = executionResultRepository.findById(executionResult.getId()).get();
			finalExecutionResult.setStatus(ExecutionStatus.COMPLETED);
			executionResultRepository.save(finalExecutionResult);

		} catch (Exception e) {
			finalExecutionResultStatus = ExecutionResultStatus.FAILURE;
			executionResult.setResult(ExecutionResultStatus.FAILURE);
			executionResult.setStatus(ExecutionStatus.COMPLETED);
			executionResult.setExecutionRemarks("Execution failed due to some issue in TM");
			executionResultRepository.save(executionResult);
			LOGGER.error("Error in executing script: {} on device: {} due to ", script.getName(), device.getName(), e);
		}

		// boolean returned with execution result status
		return finalExecutionResultStatus == ExecutionResultStatus.SUCCESS;
	}

	/**
	 * This method is to remove unwanted texts that was added for the python script
	 * execution status checks. SCRIPTEND#!@~ for checking python script was
	 * executed till end. "#TDK_@error" for adding the status as Failure when
	 * exception is thrown from the python script
	 * 
	 * @param logFilePath
	 */
	private void removeFWRequiredTextsFromLogs(String logFilePath) {
		File logFile = new File(logFilePath);
		if (!logFile.exists()) {
			LOGGER.error("Log file does not exist at path: " + logFilePath);
			return;
		}
		StringBuilder contentBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new FileReader(logFilePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.replace(Constants.END_TAG_PY_COMMENT, "").replace(Constants.ERROR_TAG_PY_COMMENT, "");
				contentBuilder.append(line).append("\n");
			}
		} catch (IOException e) {
			LOGGER.error("Error reading log file: " + logFilePath, e);
			return;
		}

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(logFilePath))) {
			bw.write(contentBuilder.toString());
		} catch (IOException e) {
			LOGGER.error("Error writing to log file: " + logFilePath, e);
		}

	}

	/**
	 * Sets the final status of the given execution object based on the results of
	 * its execution.
	 *
	 * This method retrieves the execution object from the repository using its ID.
	 * If the execution status is INPROGRESS, it updates the status to COMPLETED. It
	 * then retrieves the list of execution results associated with the execution
	 * and calculates the counts of different result statuses (FAILURE, NA, TIMEOUT,
	 * SUCCESS, SKIPPED).
	 *
	 * Based on the counts of these result statuses, the method sets the overall
	 * result status of the execution: - If there are any FAILURE results, the
	 * overall result is set to FAILURE. - If there are any TIMEOUT results, the
	 * overall result is set to FAILURE. - If all results are either NA or SKIPPED,
	 * the overall result is set to FAILURE. - If there are any SUCCESS results, the
	 * overall result is set to SUCCESS.
	 *
	 * Finally, the method saves the updated execution object back to the
	 * repository.
	 *
	 * @param executionObject the execution object whose final status needs to be
	 *                        set
	 * @return the updated execution object with the final status set
	 */
	private Execution setFinalStatusOfExecution(Execution executionObject) {

		Execution execution = executionRepository.findById(executionObject.getId()).orElse(null);

		if (execution.getExecutionStatus() == ExecutionProgressStatus.INPROGRESS) {
			execution.setExecutionStatus(ExecutionProgressStatus.COMPLETED);
		}

		if (execution.getExecutionStatus() == ExecutionProgressStatus.ABORTED) {
			return execution;
		}

		List<ExecutionResult> executionResults = executionResultRepository.findByExecution(execution);
		int totalExecuted = 0;
		int failureCount = 0;
		int naCount = 0;
		int skipped = 0;
		int success = 0;
		int scriptTimeoutCount = 0;

		if (executionResults == null || executionResults.isEmpty()) {
			execution.setResult(ExecutionOverallResultStatus.FAILURE);
			execution.setExecutionStatus(ExecutionProgressStatus.COMPLETED);
			executionRepository.save(execution);
			return execution;
		}

		for (ExecutionResult result : executionResults) {
			ExecutionResultStatus status = result.getResult();
			totalExecuted++;
			if (status == ExecutionResultStatus.FAILURE) {
				failureCount++;
			} else if (status == ExecutionResultStatus.NA) {
				naCount++;
			} else if (status == ExecutionResultStatus.TIMEOUT) {
				scriptTimeoutCount++;
			} else if (status == ExecutionResultStatus.SUCCESS) {
				success++;
			} else if (status == ExecutionResultStatus.SKIPPED) {
				skipped++;
			}
		}

		if (failureCount > 0) {
			execution.setResult(ExecutionOverallResultStatus.FAILURE);
		} else if (scriptTimeoutCount > 0) {
			execution.setResult(ExecutionOverallResultStatus.FAILURE);
		} else if (naCount + skipped == totalExecuted) {
			execution.setResult(ExecutionOverallResultStatus.FAILURE);
		} else if (success > 0) {
			execution.setResult(ExecutionOverallResultStatus.SUCCESS);
		}

		executionRepository.save(execution);
		return execution;

	}

	/**
	 * 
	 * This method is used to get execution entities for the execution as a code
	 * optimisaation for Single script execution
	 * 
	 * @param device
	 * @param script
	 * @param user
	 * @param executionName
	 * @param category
	 * @param isRerunOnFailure
	 * @return ExecutionEntities
	 * 
	 */
	private ExecutionEntities getExecutionEntitiesForExecution(Device device, Script script, String user,
			String executionName, boolean isRerunOnFailure, boolean isDeviceLogsNeeded, boolean isPerformanceLogsNeeded,
			boolean isDiagnosticLogsNeeded, String testType) {
		Execution execution = new Execution();
		execution.setName(executionName);
		execution.setCategory(device.getCategory());
		execution.setExecutionType(ExecutionType.SINGLESCRIPT);
		execution.setScripttestSuiteName(script.getName());
		execution.setRerunOnFailure(isRerunOnFailure);
		execution.setResult(ExecutionOverallResultStatus.INPROGRESS);
		execution.setExecutionStatus(ExecutionProgressStatus.INPROGRESS);
		execution.setUser(user);
		execution.setTestType(testType);
		execution.setDeviceLogsNeeded(isDeviceLogsNeeded);
		execution.setPerformanceLogsNeeded(isPerformanceLogsNeeded);
		execution.setDiagnosticLogsNeeded(isDiagnosticLogsNeeded);
		Execution savedExecution = executionRepository.save(execution);

		// Create and save ExecutionDevice
		ExecutionDevice executionDevice = new ExecutionDevice();
		executionDevice.setDevice(device.getName());
		executionDevice.setDeviceIp(device.getIp());
		executionDevice.setDeviceMac(device.getMacId());
		executionDevice.setDeviceType(device.getDeviceType().getName());
		// Before saving the execution device, get the latest build name from the device
		executionDevice.setExecution(savedExecution);
		executionDeviceRepository.save(executionDevice);

		ExecutionResult executionResult = new ExecutionResult();
		executionResult.setDateOfExecution(Instant.now());
		executionResult.setExecution(execution);
		executionResult.setScript(script.getName());
		executionResult.setResult(ExecutionResultStatus.INPROGRESS);
		executionResultRepository.save(executionResult);

		ExecutionEntities executionEntites = new ExecutionEntities();
		executionEntites.setExecution(execution);
		executionEntites.setExecutionDevice(executionDevice);
		List<ExecutionResult> executionResultList = new ArrayList<ExecutionResult>();
		executionResultList.add(executionResult);
		executionEntites.setExecutionResult(executionResultList);

		return executionEntites;

	}

	/**
	 * This method is used to get real execution time
	 * 
	 * @param executableResultList
	 * @return double
	 */
	private double getRealExcecutionTime(List<ExecutionResult> executableResultList) {
		double realExecutionTime = 0L;
		for (ExecutionResult executionResult : executableResultList) {
			realExecutionTime += executionResult.getExecutionTime();
		}
		return realExecutionTime;
	}

	/**
	 * Handles invalid scripts by generating execution results for each script.
	 * 
	 * @param invalidScripts the list of invalid scripts to be processed
	 * @param execution      the execution context associated with the scripts
	 * @param device         the device associated with the execution
	 * @return a list of execution results for the invalid scripts
	 */
	private List<ExecutionResult> handleInvalidScripts(List<Script> invalidScripts, Execution execution,
			Device device) {
		List<ExecutionResult> execResultList = new ArrayList<>();
		for (Script script : invalidScripts) {
			StringBuilder remarks = new StringBuilder();
			ExecutionResult executionResult = new ExecutionResult();
			executionResult.setDateOfExecution(Instant.now());
			executionResult.setExecution(execution);
			executionResult.setScript(script.getName());

			if (script.isSkipExecution()) {
				remarks.append("Script: ").append(script.getName())
						.append(" is marked as skipTest, so not triggering execution.\n");
				executionResult.setResult(ExecutionResultStatus.SKIPPED);
			} else if (!commonService.validateScriptDeviceDeviceType(device, script)) {
				remarks.append("Device: ").append(device.getName()).append(" and Script: ").append(script.getName())
						.append(" combination is invalid due to different device types, so not triggering execution.\n");
				executionResult.setResult(ExecutionResultStatus.NA);
			} else if (!commonService.vaidateScriptDeviceCategory(device, script)) {
				remarks.append("Device: ").append(device.getName()).append(" and Script: ").append(script.getName())
						.append(" combination is invalid and belongs to different category, so not triggering execution.\n");
				executionResult.setResult(ExecutionResultStatus.NA);
			}

			executionResult.setExecutionRemarks(remarks.toString());
			executionResultRepository.save(executionResult);
			execResultList.add(executionResult);
		}
		return execResultList;
	}

	/*
	 * This method is to handle the applicable scripts and create the execution
	 * result for the same and save it
	 * 
	 * @param applicableScripts
	 * 
	 * @param device
	 * 
	 * @param execution
	 * 
	 * @param executionDevice
	 * 
	 * @return List<ExecutionResult>
	 */

	private List<ExecutionResult> handleApplicableScripts(List<Script> applicableScripts, Device device,
			Execution execution, ExecutionDevice executionDevice, boolean isIndividualRepeatExecution,
			int repeatCount) {
		if (!isIndividualRepeatExecution) {
			repeatCount = 1;
		}
		List<ExecutionResult> executableResultList = new ArrayList<>();
		for (Script script : applicableScripts) {
			for (int i = 0; i < repeatCount; i++) {
				LOGGER.info("Executing script: {} on device: {}", script.getName(), device.getName());
				ExecutionResult executionResult = new ExecutionResult();
				executionResult.setDateOfExecution(Instant.now());
				executionResult.setExecution(execution);
				executionResult.setScript(script.getName());
				executionResult.setResult(ExecutionResultStatus.PENDING);
				executionResultRepository.save(executionResult);
				executableResultList.add(executionResult);
			}
		}
		return executableResultList;
	}

	/**
	 * This method is used to create the replacement string for the script the
	 * params that should be passed to the ConfigureTestCase method in the python
	 * script
	 * 
	 * @param device
	 * @param execution
	 * @param executionDevice
	 * @param executionResult
	 * @param script
	 * @return the modified ConfigureTestCase method string with required parameters
	 */
	private String prepareReplacementString(Device device, Execution execution, ExecutionDevice executionDevice,
			ExecutionResult executionResult, Script script) {
		return Constants.METHOD_TOKEN + Constants.LEFT_PARANTHESIS + Constants.SINGLE_QUOTES
				+ commonService.getTMUrlFromConfigFileForTestExecution() + Constants.SINGLE_QUOTES
				+ Constants.COMMA_SEPERATOR + Constants.SINGLE_QUOTES + AppConfig.getRealPath()
				+ Constants.SINGLE_QUOTES + Constants.COMMA_SEPERATOR + Constants.SINGLE_QUOTES
				+ commonService.getBaseLogPath() + Constants.FILE_PATH_SEPERATOR + Constants.SINGLE_QUOTES
				+ Constants.COMMA_SEPERATOR + Constants.SINGLE_QUOTES + execution.getId() + Constants.SINGLE_QUOTES
				+ Constants.COMMA_SEPERATOR + Constants.SINGLE_QUOTES + executionDevice.getId()
				+ Constants.SINGLE_QUOTES + Constants.COMMA_SEPERATOR + Constants.SINGLE_QUOTES
				+ executionResult.getId() + Constants.SINGLE_QUOTES + Constants.REPLACE_BY_TOKEN
				+ device.getAgentMonitorPort() + Constants.COMMA_SEPERATOR + device.getStatusPort()
				+ Constants.COMMA_SEPERATOR + Constants.SINGLE_QUOTES + script.getId() + Constants.SINGLE_QUOTES
				+ Constants.COMMA_SEPERATOR + Constants.SINGLE_QUOTES + device.getId() + Constants.SINGLE_QUOTES
				+ Constants.COMMA_SEPERATOR + Constants.SINGLE_QUOTES + false + Constants.SINGLE_QUOTES
				+ Constants.COMMA_SEPERATOR + Constants.SINGLE_QUOTES + false + Constants.SINGLE_QUOTES
				+ Constants.COMMA_SEPERATOR + Constants.SINGLE_QUOTES + false + Constants.SINGLE_QUOTES
				+ Constants.COMMA_SEPERATOR;
	}

	/**
	 * This method is used to delete the temporary file
	 * 
	 * @param deleteFilePath
	 */
	private void deleteTemporaryFile(String deleteFilePath) {
		LOGGER.info("Deleting temporary file: " + deleteFilePath);
		boolean isDeleted = commonService.deleteFile(deleteFilePath);
		if (!isDeleted) {
			LOGGER.error("Failed to delete  temporaryfile: " + deleteFilePath);
		} else {
			LOGGER.info("Temporary file deleted successfully: " + deleteFilePath);

		}
	}

	/**
	 * This method is used to modify the content of the file by replacing the
	 * placeholders with the actual values
	 * 
	 * @param content
	 * @param replacements
	 * @return String
	 */
	private String modifyContent(String content, Map<String, String> replacements) {
		for (Map.Entry<String, String> entry : replacements.entrySet()) {
			content = content.replace(entry.getKey(), entry.getValue());
		}
		return content;
	}

	/**
	 * This method is used to read the content of a file
	 * 
	 * @param filePath
	 * @return String
	 * @throws IOException
	 */
	private String readFile(String filePath) throws IOException {
		// Check if the file exists
		File file = new File(filePath);
		if (!file.exists()) {
			LOGGER.error("File does not exist at path: " + filePath);
		}
		StringBuilder contentBuilder = new StringBuilder();

		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String line;
		while ((line = br.readLine()) != null) {
			contentBuilder.append(line).append("\n");
		}

		return contentBuilder.toString();
	}

	/**
	 * This method is used to write content to a file
	 * 
	 * @param filePath
	 * @param content
	 * @throws IOException
	 */
	private void writeFile(String filePath, String content) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
			bw.write(content);
		}

	}

	/**
	 * This method is used to compute the time difference between two time stamps
	 * 
	 * @param currentTimeMillisBeforeExecution
	 * @param currentTimeMillisAfterExecution
	 * @return double
	 */
	private double computeTimeDifference(double currentTimeMillisBeforeExecution,
			double currentTimeMillisAfterExecution) {
		double differenceInMinutes = (currentTimeMillisAfterExecution - currentTimeMillisBeforeExecution) / (1000 * 60);
		return Math.round(differenceInMinutes * 1000.0) / 1000.0;
	}

	/**
	 * This method is used to round off the time to three decimal places
	 * 
	 * @param time
	 * @return double
	 */
	private double roundOfToThreeDecimals(double time) {
		return Math.round(time * 1000.0) / 1000.0;
	}

	/**
	 * This method is used to restart the multi script execution that was
	 * interrupted after the application restart.
	 * 
	 * @param execution the execution object that was interrupted
	 */
	@Async
	public void restartMultiScriptExecution(Execution execution) {
		LOGGER.info("Restarting multi script execution: {}", execution.getName().toUpperCase());
		try {

			// Retrieve the associated device and check if it is available ,set
			// the status of the execution results as aborted if the device is not
			// available
			ExecutionDevice executionDevice = executionDeviceRepository.findByExecution(execution);
			Device device = deviceRepository.findByName(executionDevice.getDevice());
			if (!isDeviceAvailableForExecution(device, execution)) {
				return;
			}

			// List of ExecutionResults with INPROGRESS or PENDING status remaining in the
			// ongoing execution before the app restart
			List<ExecutionResult> executionResults = processExecutionResults(execution);
			if (executionResults.isEmpty()) {
				LOGGER.warn("No valid execution results found for execution: {}", execution.getName());
				this.pausePendingAndInProgressResultsWhileRestart(execution);
				return;
			}

			// Set the device status to IN_USE state if not already set
			deviceStatusService.setDeviceStatus(DeviceStatus.IN_USE, device.getName());

			// Execute the scripts that were in progress or pending before the restart
			this.executeTheListOfExecutionResults(execution, executionResults);

		} catch (Exception e) {
			LOGGER.error("Error occurred while restarting multi script execution: {}", execution.getName(), e);
			return;
		}

	}

	/**
	 * This method is used to restart the single script execution that was
	 * interrupted after the application restart.
	 * 
	 * @param execution the execution object that was interrupted
	 */
	@Async
	public void restartSingleScriptExecution(Execution execution) {
		LOGGER.info("Restarting single script execution: {}", execution.getName().toUpperCase());
		// Retrieve the associated device and check if it is available ,set
		// the status of the execution results as aborted if the device is not
		// available
		ExecutionDevice executionDevice = executionDeviceRepository.findByExecution(execution);
		Device device = deviceRepository.findByName(executionDevice.getDevice());
		if (!isDeviceAvailableForExecution(device, execution)) {
			return;
		}

		try {

			LOGGER.info("Device {} is available for execution: {}", device.getName(), execution.getName());

			// Get the execution results that was in progress or pending before the restart
			ExecutionResult executionResult = this.processExecutionResultForSingleScriptRestart(execution);

			Script script = scriptRepository.findByName(executionResult.getScript());

			DeviceStatus deviceStatus = deviceStatusService.fetchDeviceStatus(device);
			if (deviceStatus != DeviceStatus.FREE) {
				LOGGER.warn("Device {} is not available for re-run of execution: {}", device.getName(),
						execution.getName());
				abortPendingAndInProgressResultsWhileRestart(execution);
				return;

			}

			// Set the device status to IN_USE state if not already set
			deviceStatusService.setDeviceStatus(DeviceStatus.IN_USE, device.getName());

			executeScriptinDevice(script, execution, executionResult, executionDevice);

			double executionStartTime = execution.getCreatedDate().toEpochMilli();
			double executionEndTime = System.currentTimeMillis();
			double executionTime = this
					.roundOfToThreeDecimals(this.computeTimeDifference(executionStartTime, executionEndTime));

			double realExecutionTime = this.getRealExcecutionTime(new ArrayList<>(execution.getExecutionResults()));
			this.setExecutionTime(execution, executionTime, realExecutionTime);

			this.setFinalStatusOfExecution(execution);

		} catch (Exception e) {
			LOGGER.error("Error occurred while restarting single script execution: {}", execution.getName(), e);
			return;
		}
		if (device != null) {
			deviceStatusService.fetchAndUpdateDeviceStatus(device);
		}

	}

	/**
	 * This method checks if the device is available for execution.
	 * 
	 * @param device    the device to check availability for execution
	 * @param execution the execution object containing the device information
	 * @return true if the device is available for execution, false otherwise
	 */
	private boolean isDeviceAvailableForExecution(Device device, Execution execution) {
		if (device == null) {
			LOGGER.warn("Device {} not found in the repository", execution.getExecutionDevice().getDevice());
			abortPendingAndInProgressResultsWhileRestart(execution);
			return false;
		}

		DeviceStatus deviceStatus = deviceStatusService.fetchDeviceStatus(device);

		// For the TDK Agent enabled executions,the status from the devices
		// will be busy. This will happen when the execution get abruptly stopped
		// during the restart.
		if (deviceStatus == DeviceStatus.BUSY) {
			pythonLibraryScriptExecutorService.resetAgentForTDKDevices(device.getIp(), device.getPort(),
					Constants.TRUE);
			// Add wait for 2 seconds
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				LOGGER.error("Error occurred while waiting for device to become available: {}", e.getMessage());
			}
			// Fetch the status of the device after 2 sec waiting, if it
			// comes up the restart will happen immediately, other wise
			// it will get aborted if the device is still not in FREE state
			// as per the logic given below
			deviceStatus = deviceStatusService.fetchDeviceStatus(device);
		}

		// Check if the device is still busy after waiting
		if (deviceStatus != DeviceStatus.FREE) {
			LOGGER.warn("Device {} is not available for re-run of execution: {}", device.getName(),
					execution.getName());
			pausePendingAndInProgressResultsWhileRestart(execution);
			return false;
		}

		return true;
	}

	/**
	 * This method processes the execution result for a single script restart.
	 * 
	 * @param execution the execution object that was interrupted
	 * @return ExecutionResult or null if no valid execution result is found
	 */
	private ExecutionResult processExecutionResultForSingleScriptRestart(Execution execution) {
		List<ExecutionResult> executionResultsList = executionResultRepository.findByExecution(execution); // Process
																											// any
																											// INPROGRESS
		LOGGER.info("Processing execution results for execution: {}", execution.getName());
		LOGGER.info("Number of execution results found: {}", executionResultsList.size());
		if (executionResultsList == null || executionResultsList.isEmpty() || executionResultsList.size() > 1) {
			LOGGER.warn("No valid execution result found for execution: {}", execution.getName());
			// Abort all pending and in progress results
			this.abortPendingAndInProgressResultsWhileRestart(execution);
			return null;
		} else {
			ExecutionResult executionResult = executionResultsList.get(0);
			LOGGER.info("Going to restart the Execution Result: {} ", executionResult.getId());

			return executionResult;

		}
	}

	/**
	 * This method retrieves all execution results associated with the execution
	 * that are either INPROGRESS or PENDING. Processes the INPROGRESS results by
	 * deleting their associated ExecutionMethodResults and ExecutionResultAnalysis,
	 * then updates their status to PENDING. Finally, it saves the updated
	 * ExecutionResults back to the repository for it to be executed again.
	 * 
	 * @param execution the execution object containing the execution results to be
	 *                  processed
	 * @return a list of ExecutionResults that were processed
	 */
	private List<ExecutionResult> processExecutionResults(Execution execution) {
		// Retrieve all ExecutionResults with INPROGRESS status
		List<ExecutionResult> executionResultsInprogress = executionResultRepository.findByExecution(execution).stream()
				.filter(result -> result.getStatus() == ExecutionStatus.INPROGRESS)
				.sorted(Comparator.comparing(ExecutionResult::getCreatedDate)).toList();

		List<ExecutionResult> executionResultsPending = executionResultRepository.findByExecution(execution).stream()
				.filter(result -> result.getResult() == ExecutionResultStatus.PENDING)
				.sorted(Comparator.comparing(ExecutionResult::getCreatedDate)).toList();

		for (ExecutionResult executionResult : executionResultsInprogress) {

			// Delete all ExecutionMethodResults under the ExecutionResult
			List<ExecutionMethodResult> executionMethodResults = executionMethodResultRepository
					.findByExecutionResult(executionResult);
			if (executionMethodResults != null && !executionMethodResults.isEmpty()) {
				executionMethodResultRepository.deleteAll(executionMethodResults);
			}

			// Delete the ExecutionResultAnalysis associated with the ExecutionResult
			ExecutionResultAnalysis executionResultAnalyses = executionResultAnalysisRepository
					.findByExecutionResult(executionResult);
			if (executionResultAnalyses != null) {
				executionResultAnalysisRepository.delete(executionResultAnalyses);
			}

			// Set the status to PENDING
			executionResult.setResult(ExecutionResultStatus.PENDING);
			executionResult.setStatus(null);

			// Save the updated ExecutionResult
			executionResultRepository.save(executionResult);
		}

		List<ExecutionResult> orderedExecutionResults = new ArrayList<>();
		orderedExecutionResults.addAll(executionResultsInprogress);
		orderedExecutionResults.addAll(executionResultsPending);
		return orderedExecutionResults;

	}

	/**
	 * This method is used to execute the execution results for the given execution
	 * and execution results. Primarily used for restarting paused executions.
	 * 
	 * @param execution        the execution object that was paused
	 * @param executionResults the list of execution results to be executed
	 */
	@Async
	public void executeThePausedExecutions(Execution execution, List<ExecutionResult> executionResults) {

		execution.setExecutionStatus(ExecutionProgressStatus.INPROGRESS);
		execution.setResult(ExecutionOverallResultStatus.INPROGRESS);
		Execution executionSaved = executionRepository.save(execution);
		this.executeTheListOfExecutionResults(executionSaved, executionResults);
		LOGGER.info("Execution restarted for execution: {}", execution.getName());

	}

	/**
	 * This method executes the pending execution results for the given execution.
	 * 
	 * @param execution
	 * @param executionResults
	 */
	public void executeTheListOfExecutionResults(Execution execution, List<ExecutionResult> executionResults) {
		UUID executionId = execution.getId();
		ExecutionDevice executionDevice = executionDeviceRepository.findByExecution(execution);
		Device device = deviceRepository.findByName(executionDevice.getDevice());
		boolean pauseExecution = false;
		int executedScript = 0;
		try {
			for (ExecutionResult execRes : executionResults) {
				if (this.isExecutionAborted(executionId)) {
					for (ExecutionResult execResults : executionResults) {
						if (execResults.getResult() == ExecutionResultStatus.INPROGRESS
								|| execResults.getResult() == ExecutionResultStatus.PENDING) {
							execResults.setResult(ExecutionResultStatus.ABORTED);
							execResults.setExecutionRemarks("Execution aborted by user");
							executionResultRepository.save(execResults);
						}
					}
					Execution executionAborted = executionRepository.findById(executionId).orElse(null);
					executionAborted.setExecutionStatus(ExecutionProgressStatus.ABORTED);
					executionAborted.setResult(ExecutionOverallResultStatus.ABORTED);
					executionRepository.save(executionAborted);
					LOGGER.info("Execution aborted for device: {}", executionDevice.getDevice());
					break;
				}
				execRes.setExecutionRemarks(
						"Executing script: " + execRes.getScript() + " on device: " + executionDevice.getDevice());
				executionResultRepository.save(execRes);
				Script script = scriptRepository.findByName(execRes.getScript());
				boolean executionResult = executeScriptinDevice(script, execution, execRes, executionDevice);

				if (executionResult) {
					LOGGER.info("Execution result success for {} on device: {}", script.getName(),
							executionDevice.getDevice());
				} else {
					LOGGER.info("Execution result failed for {} on device: {}", script.getName(),
							executionDevice.getDevice());
				}

				executedScript++;
				Execution executionCompleted = executionRepository.findById(executionId).orElse(null);

				// The total execution time is valculated based on the time taken for execution
				// of all
				// the scripts executed so far
				double executionStartTime = execution.getCreatedDate().toEpochMilli();
				double currentExecTime = System.currentTimeMillis();
				double executionTime = this.computeTimeDifference(executionStartTime, currentExecTime);
				executionCompleted.setExecutionTime(executionTime);
				executionRepository.save(executionCompleted);

				DeviceStatus currentStatus = deviceStatusService.fetchDeviceStatus(device);
				if (currentStatus == DeviceStatus.NOT_FOUND || currentStatus == DeviceStatus.HANG) {
					pauseExecution = true;
					LOGGER.warn("Device {} is not available for execution, pausing execution: {}", device.getName(),
							execution.getName());
					this.setExecutiontoPausedState(execution);
					break;
				}

			}

			Execution finalExecution = executionRepository.findById(executionId).orElse(null);

			// Total execution time based on the time taken by adding the time taken for
			// execution
			// of all scripts
			List<ExecutionResult> allResultList = executionResultRepository.findByExecution(finalExecution);
			if (allResultList != null) {
				double realExecTime = this.getRealExcecutionTime(allResultList);
				double roundOfValue = this.roundOfToThreeDecimals(realExecTime);
				finalExecution.setRealExecutionTime(roundOfValue);
			}

			// Final execution time calculation after considering the execution of all
			// scripts
			// This is the time when the execution is completed from the start of the
			// execution
			double executionEndTime = System.currentTimeMillis();
			double executionStartTime = finalExecution.getCreatedDate().toEpochMilli();
			double executionTime = this.computeTimeDifference(executionStartTime, executionEndTime);
			finalExecution.setExecutionTime(executionTime);

			executionRepository.save(finalExecution);
			if (!pauseExecution) {
				LOGGER.info("All Script executions completed for device : {}", executionDevice.getDevice());
				this.setFinalStatusOfExecution(finalExecution);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Unlock the device with the current status
			deviceStatusService.fetchAndUpdateDeviceStatus(device);
			LOGGER.error("Error in executing scripts: {} on device: {}", device.getName());
			throw new TDKServiceException("Error in executing scripts: " + " on device: " + device.getName());
		}
		// Unlock the device with the current status
		deviceStatusService.fetchAndUpdateDeviceStatus(device);
	}

	/**
	 * Marks all PENDING and INPROGRESS ExecutionResults of the given Execution as
	 * ABORTED while trying to restart because the device is not available for
	 * execution.
	 *
	 * @param execution the Execution object whose results need to be updated
	 */
	private void abortPendingAndInProgressResultsWhileRestart(Execution execution) {
		// Retrieve all ExecutionResults associated with the given Execution
		List<ExecutionResult> executionResults = executionResultRepository.findByExecution(execution);

		for (ExecutionResult result : executionResults) {
			// Taking the result status for Pending status and
			// status checking for InProgress, because the status will be set to FAILURE
			// or SUCCESS with the saveResultDetails
			if (result.getResult() == ExecutionResultStatus.PENDING
					|| result.getStatus() == ExecutionStatus.INPROGRESS) {
				// Update the status to ABORTED
				result.setStatus(ExecutionStatus.COMPLETED);
				result.setResult(ExecutionResultStatus.ABORTED);
				result.setExecutionRemarks(
						"Execution aborted due to app restart and during the start up, the device is not available for execution.");

				// Save the updated ExecutionResult
				executionResultRepository.save(result);
			}
		}
		// Update the Execution status to ABORTED
		execution.setExecutionStatus(ExecutionProgressStatus.ABORTED);
		execution.setResult(ExecutionOverallResultStatus.ABORTED);

		ExecutionDevice executionDevice = executionDeviceRepository.findByExecution(execution);
		Device device = deviceRepository.findByName(executionDevice.getDevice());
		// Unlock the device with the current status
		deviceStatusService.fetchAndUpdateDeviceStatus(device);

		// Save the updated Execution
		executionRepository.save(execution);
	}

	/**
	 * This method sets the execution status to PAUSED for the given Execution
	 * object. It updates the execution status to PAUSED and sets the result to
	 * PAUSED. It also iterates through the list of ExecutionResults associated with
	 * the Execution object and updates their status to PAUSED if they are currently
	 * INPROGRESS or PENDING.
	 */
	private void setExecutiontoPausedState(Execution execution) {
		LOGGER.info("Setting execution to PAUSED state for execution: {}", execution.getName());
		Execution finalExecution = executionRepository.findById(execution.getId()).orElse(null);

		finalExecution.setExecutionStatus(ExecutionProgressStatus.PAUSED);
		finalExecution.setResult(ExecutionOverallResultStatus.PAUSED);

		List<ExecutionResult> executionResults = finalExecution.getExecutionResults();
		for (ExecutionResult executionResult : executionResults) {
			if (executionResult.getResult() == ExecutionResultStatus.INPROGRESS
					|| executionResult.getResult() == ExecutionResultStatus.PENDING) {
				executionResult.setResult(ExecutionResultStatus.PAUSED);
				executionResult.setStatus(ExecutionStatus.PAUSED);
				executionResult.setExecutionRemarks("Execution paused due to device unavailability");
				executionResultRepository.save(executionResult);
			}
		}
		executionRepository.save(finalExecution);
	}

	/**
	 * Marks all PENDING and INPROGRESS ExecutionResults of the given Execution as
	 * ABORTED while trying to restart because the device is not available for
	 * execution.
	 *
	 * @param execution the Execution object whose results need to be updated
	 */
	private void pausePendingAndInProgressResultsWhileRestart(Execution execution) {
		// Retrieve all ExecutionResults associated with the given Execution
		List<ExecutionResult> executionResults = executionResultRepository.findByExecution(execution);

		for (ExecutionResult result : executionResults) {
			// Taking the result status for Pending status and
			// status checking for InProgress, because the status will be set to FAILURE
			// or SUCCESS with the saveResultDetails
			if (result.getResult() == ExecutionResultStatus.PENDING
					|| result.getStatus() == ExecutionStatus.INPROGRESS) {
				// Update the status to PAUSED
				result.setStatus(ExecutionStatus.PAUSED);
				result.setResult(ExecutionResultStatus.PAUSED);
				result.setExecutionRemarks(
						"Execution got paused due to app restart and during the start up, the device was not available for execution.");

				// Save the updated ExecutionResult
				executionResultRepository.save(result);
			}
		}
		// Update the Execution status to PAUSED
		execution.setExecutionStatus(ExecutionProgressStatus.PAUSED);
		execution.setResult(ExecutionOverallResultStatus.PAUSED);

		ExecutionDevice executionDevice = executionDeviceRepository.findByExecution(execution);
		Device device = deviceRepository.findByName(executionDevice.getDevice());
		// Unlock the device with the current status
		deviceStatusService.fetchAndUpdateDeviceStatus(device);

		// Save the updated Execution
		executionRepository.save(execution);
	}

}
