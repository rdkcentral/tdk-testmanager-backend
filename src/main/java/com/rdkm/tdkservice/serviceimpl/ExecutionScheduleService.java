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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.dto.ExecutionResponseDTO;
import com.rdkm.tdkservice.dto.ExecutionScheduleDTO;
import com.rdkm.tdkservice.dto.ExecutionSchedulesResponseDTO;
import com.rdkm.tdkservice.dto.ExecutionTriggerDTO;
import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.enums.ExecutionTriggerStatus;
import com.rdkm.tdkservice.enums.ScheduleStatus;
import com.rdkm.tdkservice.enums.ScheduleType;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.exception.UserInputException;
import com.rdkm.tdkservice.model.ExecutionSchedule;
import com.rdkm.tdkservice.repository.ExecutionScheduleRepository;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.MapperUtils;
import com.rdkm.tdkservice.util.Utils;

import jakarta.annotation.PostConstruct;

/**
 * The service class for the execution schedule feature
 */
@Service
public class ExecutionScheduleService {

	@Autowired
	ExecutionScheduleRepository executionScheduleRepository;

	@Autowired
	private ExecutionService executionService;

	@Autowired
	private CommonService commonService;

	private TaskScheduler taskScheduler;

	// Map to store the scheduled tasks
	private Map<UUID, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

	private static final int SCHEDULE_LIMIT = 15;

	private static final String SCHEDULE_TASK_LIMIT = "schedule.tasks.number";

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionScheduleService.class);

	/**
	 * Initializes the task scheduler and loads scheduled tasks.
	 */
	@PostConstruct
	public void init() {
		taskScheduler = new ThreadPoolTaskScheduler();
		((ThreadPoolTaskScheduler) taskScheduler).initialize();
		loadScheduledTasks();
	}

	/**
	 * Saves the execution schedule.
	 * 
	 * @param executionScheduleDTO the execution schedule DTO
	 * @return boolean status of the execution
	 */
	public boolean saveScheduleExecution(ExecutionScheduleDTO executionScheduleDTO) {
		LOGGER.info("Creating the execution schedule");
		boolean isScheduledExecution = false;
		String cronExpression = null;
		ExecutionTriggerDTO executionTriggerDTO = executionScheduleDTO.getExecutionTriggerDTO();

		// check if the schedule task limit is reached
		int scheduleTaskLimit = this.getScheduleTaskLimit();
		int activeTaskCount = this.getActiveTaskCount();
		if (activeTaskCount >= scheduleTaskLimit) {
			LOGGER.error("Schedule task limit reached, cannot schedule more tasks");
			throw new UserInputException(
					"Schedule task limit of " + activeTaskCount + " reached, cannot schedule more tasks");
		}

		// if the device list is more than 1, throw exception
		if (executionTriggerDTO.getDeviceList() != null && executionTriggerDTO.getDeviceList().size() > 1) {
			throw new UserInputException("Multiple devices are not supported for scheduling");
		}

		// if the execution time and device is already exists
		if (executionScheduleRepository.existsByExecutionTimeAndDevice(executionScheduleDTO.getExecutionTime(),
				executionScheduleDTO.getExecutionTriggerDTO().getDeviceList().get(0))) {
			throw new UserInputException("There is already a schedule for the same execution time and device");
		}

		if (ScheduleType.ONCE == executionScheduleDTO.getScheduleType()) {
			// Perform the validations for one time execution
			this.performOneTimeExecutionValidations(executionScheduleDTO);
		}

		if (ScheduleType.REPEAT == executionScheduleDTO.getScheduleType()) {
			// Perform the validations for repeat execution
			this.performRepeatExecutionValidations(executionScheduleDTO);
			// get cron expression from the cron type and query
			cronExpression = this.getCronExpression(executionScheduleDTO.getCronType(),
					executionScheduleDTO.getCronQuery(), executionScheduleDTO.getCronExpression());

		}

		// Once the execution DTO is created, check if the trigger request is valid
		// If not valid, throw an exception which will be caught by the controller
		executionService.checkValidTriggerRequest(executionTriggerDTO);

		// Saving the execution schedule object with schedule and execution details
		// This is to store the execution schedule in the DB, where lists are converted
		// to comma separated string.When the scheduled execution happens, then it is
		// converted back to list or Execution Trigger DTO
		ExecutionSchedule executionSchedule = MapperUtils.convertToExecutionSchedule(executionScheduleDTO,
				cronExpression);
		ExecutionSchedule savedExecutionSchedule = null;
		try {
			savedExecutionSchedule = executionScheduleRepository.save(executionSchedule);
		} catch (Exception e) {
			LOGGER.error("Database error while saving the execution schedule: " + e.getMessage());
		}
		// Create the schedule based on the saved execution schedule , after the
		// schedule is created
		if (savedExecutionSchedule != null) {
			isScheduledExecution = this.createSchedule(savedExecutionSchedule);
		}
		return isScheduledExecution;
	}

	/**
	 * Schedules a task based on the provided execution schedule.
	 * 
	 * @param schedule the execution schedule containing the details for scheduling
	 *                 the task
	 */
	private boolean scheduleTask(ExecutionSchedule schedule) {
		boolean isScheduled = false;
		try {
			if ((schedule.getScheduleType() == ScheduleType.REPEAT) && (schedule.getCronExpression() != null)) {
				ScheduledFuture<?> future = taskScheduler.schedule(() -> triggerScheduledExecution(schedule),
						new CronTrigger(schedule.getCronExpression()));
				scheduledTasks.put(schedule.getId(), future);
				isScheduled = true;
			} else if ((schedule.getScheduleType() == ScheduleType.ONCE) && (schedule.getExecutionTime() != null)) {
				ScheduledFuture<?> future = taskScheduler.schedule(() -> triggerScheduledExecution(schedule),
						schedule.getExecutionTime());
				scheduledTasks.put(schedule.getId(), future);
				isScheduled = true;

			}
		} catch (TaskRejectedException e) {
			LOGGER.error("Error while scheduling the task: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			LOGGER.error("Error while scheduling the task: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (isScheduled) {
				LOGGER.info("Task scheduled successfully with ID: " + schedule.getId());
			} else {
				LOGGER.error("Failed to schedule task with ID: " + schedule.getId());
			}
		}
		return isScheduled;

	}

	/**
	 * Loads scheduled tasks from the repository and schedules them if their status
	 * is SCHEDULED.
	 */
	private void loadScheduledTasks() {
		List<ExecutionSchedule> schedules = executionScheduleRepository.findAll();
		// Add the schedules only when the scheudle status is SCHDEULED
		// do not add if the schedule is COmpleted or cancelled
		for (ExecutionSchedule schedule : schedules) {
			if (ScheduleStatus.SCHEDULED.equals(schedule.getScheduleStatus())) {
				scheduleTask(schedule);
			}
		}

	}

	/**
	 * Cancels a scheduled task.
	 * 
	 * @param taskId the ID of the task to be cancelled
	 */
	public void cancelTask(UUID execId) {
		LOGGER.info("Cancelling the task with ID: " + execId);
		ScheduledFuture<?> scheduledTask = scheduledTasks.get(execId);
		if (scheduledTask != null) {
			scheduledTask.cancel(false);
			scheduledTasks.remove(execId);
		}
		ExecutionSchedule executionSchedule = executionScheduleRepository.findById(execId).get();
		if (executionSchedule == null) {
			LOGGER.error("Execution schedule not found");
			throw new UserInputException("Execution schedule not found");
		}
		executionSchedule.setScheduleStatus(ScheduleStatus.CANCELLED);
		executionScheduleRepository.save(executionSchedule);
		LOGGER.info("Cancelled the task with ID: " + execId);

	}

	/**
	 * Reverts the cancellation of a scheduled task.
	 * 
	 * @param taskId the ID of the task to be reverted
	 */
	public boolean revertCancelTask(UUID execId) {
		LOGGER.info("Reverting the cancellation of the task with ID: " + execId);
		ExecutionSchedule executionSchedule = executionScheduleRepository.findById(execId).get();
		if (executionSchedule == null) {
			LOGGER.error("Execution schedule not found");
			throw new UserInputException("Execution schedule not found");
		}
		executionSchedule.setScheduleStatus(ScheduleStatus.SCHEDULED);
		ExecutionSchedule savedExecutionSchedule = executionScheduleRepository.save(executionSchedule);
		this.createSchedule(savedExecutionSchedule);
		LOGGER.info("Reverted the cancellation of the task with ID: " + execId + " to SCHEDULED");
		return true;
	}

	/**
	 * Creates a schedule based on the saved execution schedule.
	 * 
	 * @param savedExecutionSchedule the saved execution schedule
	 * @return the created execution schedule
	 */
	public boolean createSchedule(ExecutionSchedule savedExecutionSchedule) {
		LOGGER.info("Creating schedule for the execution");
		return scheduleTask(savedExecutionSchedule);
	}

	/**
	 * This method is used to get the schedule task limit from the config file
	 * 
	 * @return schedule task limit
	 */
	private int getScheduleTaskLimit() {
		// get tm.config file using base location
		int scheduleTaskLimit = 0;
		String configFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + Constants.TM_CONFIG_FILE;
		LOGGER.debug("configFilePath: {}", configFilePath);
		// get the TM URL from the config file
		String scheduleTaskLimitFromConfig = commonService.getConfigProperty(new File(configFilePath),
				SCHEDULE_TASK_LIMIT);

		if (!Utils.isEmpty(scheduleTaskLimitFromConfig)) {
			try {
				LOGGER.debug("scheduleTaskLimitFromConfig: {}", scheduleTaskLimitFromConfig);
				scheduleTaskLimit = Integer.parseInt(scheduleTaskLimitFromConfig);
				if (scheduleTaskLimit == 0) {
					LOGGER.error("Schedule task limit is 0, defaulting to 10");
					scheduleTaskLimit = SCHEDULE_LIMIT;
				}
			} catch (NumberFormatException e) {
				LOGGER.error("Error while parsing the schedule task limit from config file: " + e.getMessage());
			} catch (Exception e) {
				LOGGER.error("Error while getting the schedule task limit from config file: " + e.getMessage());
			}
		}

		if (scheduleTaskLimit == 0) {
			LOGGER.error("Schedule task limit is 0, defaulting to 10");
			scheduleTaskLimit = SCHEDULE_LIMIT;
		}
		return scheduleTaskLimit;
	}

	/**
	 * This method is used to get the active task count
	 * 
	 * @return active task count
	 */
	public int getActiveTaskCount() {
		return scheduledTasks.size();
	}

	/**
	 * This method is used to perform the repeat execution validations
	 * 
	 * @param executionScheduleDTO
	 */
	private void performRepeatExecutionValidations(ExecutionScheduleDTO executionScheduleDTO) {
		LOGGER.info("Performing repeat execution validations");
		if (executionScheduleDTO.getCronStartTime() == null) {
			throw new UserInputException("Cron start time should be provided when we select repeat execution");
		}

		if (executionScheduleDTO.getCronEndTime() == null) {
			throw new UserInputException("Cron end time should be provided when we select repeat execution");
		}

		if (executionScheduleDTO.getCronStartTime().isAfter(executionScheduleDTO.getCronEndTime())) {
			throw new UserInputException("Cron start time should be less than the cron end time");
		}

		if (executionScheduleDTO.getCronStartTime().isBefore(Instant.now())) {
			throw new UserInputException("Cron start time should be after the current time");
		}

		if (executionScheduleDTO.getCronEndTime().isBefore(Instant.now())) {
			throw new UserInputException("Cron end time should be greater than the current time");
		}
	}

	/**
	 * This method is used to perform the one time execution validations
	 * 
	 * @param executionScheduleDTO - execution schedule DTO
	 */
	private void performOneTimeExecutionValidations(ExecutionScheduleDTO executionScheduleDTO) {
		LOGGER.info("Performing one time execution validations");
		// if execution time is null, when the type is ONCE
		if (executionScheduleDTO.getExecutionTime() == null) {
			throw new UserInputException("Execution time should be provided when you select one time execution");
		}

		// if execution time is less than the current time, throw exception
		if (executionScheduleDTO.getExecutionTime().isBefore(Instant.now())) {
			throw new UserInputException("Execution time should be greater than the current time");
		}

	}

	/**
	 * Triggers the scheduled execution.
	 * 
	 * @param executionSchedule the execution schedule
	 * @return the result of the execution
	 */
	public void triggerScheduledExecution(ExecutionSchedule executionSchedule) {
		LOGGER.info("Triggering the scheduled execution");

		if (executionSchedule.getScheduleType() == ScheduleType.REPEAT) {
			LOGGER.info("Checking if start time has reached for the cron job");
			if (executionSchedule.getCronStartTime() != null) {
				// if the start time is after the current time, do not trigger the execution
				if (executionSchedule.getCronStartTime().isAfter(Instant.now())) {
					LOGGER.info("Start time has not reached yet");
					return;
				}
			}
			LOGGER.info("Checking if end time has reached for the cron job");
			if (executionSchedule.getCronEndTime() != null) {
				if (executionSchedule.getCronEndTime().isBefore(Instant.now())) {
					LOGGER.info("End time has reached");
					this.removeScheduleExecution(executionSchedule.getId());
					// Since the end date is reached, we need to mark the schedule as completed
					ExecutionSchedule schedule = executionScheduleRepository.findById(executionSchedule.getId()).get();
					schedule.setScheduleStatus(ScheduleStatus.COMPLETED);

					executionScheduleRepository.save(schedule);// Cancel the schedule
					return;
				}
			}
		}

		ExecutionTriggerDTO executionTriggerDTO = convertToExecutionTriggerDTO(executionSchedule);
		ExecutionResponseDTO executionResponseDTO = null;
		try {
			executionResponseDTO = executionService.startExecution(executionTriggerDTO);
			// Add unique suffix for each cron execution
			if (executionSchedule.getScheduleType() == ScheduleType.REPEAT) {
				String baseName = executionTriggerDTO.getExecutionName();
				String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
						.withZone(java.time.ZoneId.systemDefault())
						.format(java.time.Instant.now());
				executionTriggerDTO.setExecutionName(baseName + "_" + timestamp);
			}
			if (executionResponseDTO.getExecutionTriggerStatus() == ExecutionTriggerStatus.NOTTRIGGERED) {
				LOGGER.error("Execution trigger status was not triggered , the response is: "
						+ executionResponseDTO.getMessage());
			}
		} catch (Exception e) {
			LOGGER.error("Error while triggering the execution: " + e.getMessage());
			e.printStackTrace();
		}

		if (ScheduleType.ONCE.equals(executionSchedule.getScheduleType())) {

			ExecutionSchedule schedule = executionScheduleRepository.findById(executionSchedule.getId()).get();
			schedule.setScheduleStatus(ScheduleStatus.COMPLETED);
			executionScheduleRepository.save(schedule);
		}
	}

	/**
	 * This method is used to convert the execution schedule to execution
	 * 
	 * @param executionSchedule the execution schedule to be converted
	 * @return
	 */
	private ExecutionTriggerDTO convertToExecutionTriggerDTO(ExecutionSchedule executionSchedule) {
		ExecutionTriggerDTO executionTriggerDTO = new ExecutionTriggerDTO();
		// convert comma separated string to list
		if (null != executionSchedule.getScriptList() && !executionSchedule.getScriptList().isEmpty()) {
			List<String> scriptList = Arrays.asList(executionSchedule.getScriptList().split(","));
			executionTriggerDTO.setScriptList(scriptList);
		}

		if (null != executionSchedule.getDevice() && !executionSchedule.getDevice().isEmpty()) {
			List<String> deviceList = Arrays.asList(executionSchedule.getDevice().split(","));
			executionTriggerDTO.setDeviceList(deviceList);
		}

		if (null != executionSchedule.getTestSuite() && !executionSchedule.getTestSuite().isEmpty()) {
			List<String> testSuite = Arrays.asList(executionSchedule.getTestSuite().split(","));
			executionTriggerDTO.setTestSuite(testSuite);
		}

		executionTriggerDTO.setTestType(executionSchedule.getTestType());
		executionTriggerDTO.setUser(executionSchedule.getUser());
		executionTriggerDTO.setCategory(executionSchedule.getCategory().getName());
		executionTriggerDTO.setExecutionName(executionSchedule.getExecutionName());
		executionTriggerDTO.setRepeatCount(executionSchedule.getRepeatCount());
		executionTriggerDTO.setRerunOnFailure(executionSchedule.isRerunOnFailure());
		executionTriggerDTO.setDeviceLogsNeeded(executionSchedule.isDeviceLogsNeeded());
		executionTriggerDTO.setPerformanceLogsNeeded(executionSchedule.isPerformanceLogsNeeded());
		executionTriggerDTO.setDiagnosticLogsNeeded(executionSchedule.isDiagnosticLogsNeeded());

		return executionTriggerDTO;

	}

	/**
	 * This method is used to cancel the execution schedule
	 * 
	 * @param executionID the execution ID of the schedule to be cancelled
	 * @return boolean status of the execution cancellation
	 */
	public boolean cancelScheduleExecution(UUID executionID) {
		LOGGER.info("Cancelling the execution schedule");
		this.removeScheduleExecution(executionID);
		ExecutionSchedule executionSchedule = executionScheduleRepository.findById(executionID).get();
		if (executionSchedule == null) {
			LOGGER.error("Execution schedule not found");
			throw new UserInputException("Execution schedule not found");
		}
		executionSchedule.setScheduleStatus(ScheduleStatus.CANCELLED);
		executionScheduleRepository.save(executionSchedule);
		LOGGER.info("Execution schedule cancelled successfully");
		return true;
	}

	/**
	 * This method is used to remove the schedule execution
	 * 
	 * @param executionID - ID of the execution
	 * @return - true or false - checks if execution is removed or not
	 */
	private boolean removeScheduleExecution(UUID executionID) {
		LOGGER.info("Removing the execution schedule");
		ScheduledFuture<?> scheduledTask = scheduledTasks.get(executionID);
		if (scheduledTask != null) {
			scheduledTask.cancel(false);
			scheduledTasks.remove(executionID);
		}
		LOGGER.info("Execution schedule removed successfully");
		return true;
	}

	/**
	 * This method is for deleting the scheduled execution
	 * 
	 * @param executionID - ID of the execution
	 * @return - true or false - checks if execution is deleted or not
	 */
	public boolean deleteScheduleExecution(UUID executionID) {
		LOGGER.info("Deleting the execution schedule");
		try {
			ScheduledFuture<?> scheduledTask = scheduledTasks.get(executionID);
			if (scheduledTask != null) {
				scheduledTask.cancel(false);
				scheduledTasks.remove(executionID);
			}
			ExecutionSchedule executionSchedule = executionScheduleRepository.findById(executionID).get();
			executionScheduleRepository.delete(executionSchedule);
			LOGGER.info("Execution schedule deleted successfully");
			return true;
		} catch (Exception e) {
			LOGGER.error("Error while deleting the execution schedule: " + e.getMessage());
			return false;
		}

	}

	/**
	 * This method is to get all the execution schedules added
	 * 
	 * @return list of execution schedules
	 */
	public List<ExecutionSchedulesResponseDTO> getAllExecutionSchedulesByCategory(String categoryValue) {
		LOGGER.info("Fetching all the execution schedules");

		Category category = Category.valueOf(categoryValue.toUpperCase());
		if (category == null) {
			throw new ResourceNotFoundException("Category ", categoryValue.toUpperCase());
		}
		List<ExecutionSchedule> listOfExecutionSchedules = executionScheduleRepository.findAllByCategory(category);
		if (listOfExecutionSchedules == null) {
			return null;
		} else {
			// Sort by creation date in descending order
			listOfExecutionSchedules = listOfExecutionSchedules.stream()
					.sorted(Comparator.comparing(ExecutionSchedule::getCreatedDate).reversed())
					.collect(Collectors.toList());

			return getResponseDTOList(listOfExecutionSchedules);
		}

	}

	/**
	 * This method returns the execution schedule response list
	 * 
	 * @param listOfExecutionSchedules - list of schedules
	 * @return listOfExecutionSchedules
	 */
	private List<ExecutionSchedulesResponseDTO> getResponseDTOList(List<ExecutionSchedule> listOfExecutionSchedules) {
		List<ExecutionSchedulesResponseDTO> responseDTOList = new ArrayList<>();
		for (ExecutionSchedule executionSchedule : listOfExecutionSchedules) {
			ExecutionSchedulesResponseDTO responseDTO = new ExecutionSchedulesResponseDTO();
			responseDTO.setExecutionTime(executionSchedule.getExecutionTime());
			responseDTO.setStatus(executionSchedule.getScheduleStatus().toString());
			responseDTO.setJobName(executionSchedule.getExecutionName());
			responseDTO.setId(executionSchedule.getId().toString());
			responseDTO.setCronEndTime(executionSchedule.getCronEndTime());
			responseDTO.setCronStartTime(executionSchedule.getCronStartTime());
			responseDTO.setScheduleType(executionSchedule.getScheduleType().toString());
			if (executionSchedule.getDevice() != null) {
				responseDTO.setDevice(executionSchedule.getDevice());
			}

			// To display the suite and script name same as the execution page
			if (executionSchedule.getScriptList() != null) {
				if (convertStringToListAndGetSize(executionSchedule.getScriptList()) == 1) {
					responseDTO.setScriptTestSuite(executionSchedule.getScriptList());
				} else {
					responseDTO.setScriptTestSuite("Multiple Scripts");
				}

			}

			if (executionSchedule.getTestSuite() != null) {
				if (convertStringToListAndGetSize(executionSchedule.getTestSuite()) == 1) {
					responseDTO.setScriptTestSuite(executionSchedule.getTestSuite());
				} else {
					responseDTO.setScriptTestSuite("Multiple TestSuite");
				}
			}

			if (executionSchedule.getScheduleType().equals(ScheduleType.ONCE)) {
				responseDTO.setDetails("One time execution");
			} else {
				if (!Utils.isEmpty(executionSchedule.getCronQuery())) {
					responseDTO.setDetails(executionSchedule.getCronQuery());
				} else {
					responseDTO.setDetails("Repeat execution");
				}
			}
			responseDTOList.add(responseDTO);
		}
		return responseDTOList;
	}

	/**
	 * This method is to convert String to List and get size
	 * 
	 * @return size of the converted string to list
	 */
	private int convertStringToListAndGetSize(String commaSeperatedString) {
		if (Utils.isEmpty(commaSeperatedString)) {
			return 0;
		}
		String[] splitArray = commaSeperatedString.split(",");
		List<String> listOfValues = Arrays.asList(splitArray);
		return listOfValues.size();
	}

	/**
	 * This method is used to get the cron expression
	 * 
	 * @param executionScheduleDTO - execution schedule DTO
	 * @return cron expression
	 */
	public String getCronExpression(String cronType, String cronQuery, String cronExpression) {
		if (cronExpression == null || cronExpression.isEmpty()) {
			LOGGER.error("Cron expression is empty");
			cronExpression = getCronExpressionFromTypeAndQuery(cronType.trim(), cronQuery.trim());
		}
		return cronExpression;
	}

	/**
	 * Generates a cron expression based on the provided cron type and query.
	 * 
	 * @param cronType  - the type of cron expression (e.g., daily, weekly, monthly)
	 * @param cronQuery - the cron query string containing specific details for the
	 *                  cron eg : "every 2 days", "every week day",
	 * @return the generated cron expression
	 */
	private String getCronExpressionFromTypeAndQuery(String cronType, String cronQuery) {
		String cronExpression = null;
		switch (cronType.toLowerCase().trim()) {

			case "daily":
				if (cronQuery.toLowerCase().contains("every") && cronQuery.toLowerCase().contains("days")) {
					// Extract the number of days
					String[] parts = cronQuery.split(" ");
					int days = Integer.parseInt(parts[1]);
					// Generate cron expression for every <no> days
					if (days == 1) {
						cronExpression = "0 0 0 * * *"; // Every day at midnight
					} else {
						// Generate cron expression for every <no> days midnight
						cronExpression = "0 0 0 */" + days + " * *";

					}
				} else if (cronQuery.toLowerCase().contains("every week day")) {
					// Generate cron expression for every week day (Monday to Friday)
					cronExpression = "0 0 0 * * 1-5"; // Every weekday at midnight";
				} else {
					throw new UserInputException(
							"Please check the given query format," + "Invalid cron query for daily schedule: "
									+ cronQuery);
				}
				break;
			case "weekly":
				cronExpression = generateWeeklyCronExpression(cronQuery);
				break;
			case "monthly":
				cronExpression = generateMonthlyCronExpression(cronQuery);
				break;
			default:
				LOGGER.error("Invalid cron type: {}", cronType);
				throw new UserInputException("Invalid cron type: " + cronType);
		}
		return cronExpression;
	}

	/**
	 * Generates a cron expression for a weekly schedule based on the provided cron
	 * 
	 * @param cronQuery the cron query string containing days of the week
	 * @return the cron expression for the weekly schedule
	 */
	private static String generateWeeklyCronExpression(String cronQuery) {
		List<String> daysOfWeek = Arrays.asList(cronQuery.split(",\\s*"));
		if (daysOfWeek.isEmpty()) {
			LOGGER.error("Invalid cron query for weekly schedule: {}", cronQuery);
			throw new UserInputException("Invalid cron query for weekly schedule: " + cronQuery);
		} else if (daysOfWeek.size() > 7) {
			LOGGER.error("Invalid cron query for weekly schedule: {}", cronQuery);
			throw new UserInputException(
					"Invalid cron query for weekly schedule, there are more than 7 days selected: " + cronQuery);
		} else if (daysOfWeek.size() == 7) {
			LOGGER.error("Invalid cron query for weekly schedule: {}", cronQuery);
			throw new UserInputException(
					"Invalid cron query for weekly schedule, you can't select all days " + cronQuery);
		} else if (daysOfWeek.size() == 0) {
			LOGGER.error("Invalid cron query for weekly schedule: {}", cronQuery);
			throw new UserInputException(
					"Invalid cron query for weekly schedule, please select at least one day " + cronQuery);
		} else if (daysOfWeek.size() == 1) {
			LOGGER.info("Cron query for weekly schedule for one day is  correct{}", cronQuery);
			return "0 0 0 * * " + mapDayToCronValue(daysOfWeek.get(0));
		} else if (daysOfWeek.size() > 1) {
			LOGGER.info("Cron query for weekly schedule for is correct{}", cronQuery);
			List<String> cronDays = daysOfWeek.stream().map(ExecutionScheduleService::mapDayToCronValue)
					.collect(Collectors.toList());
			return "0 0 0 * * " + String.join(",", cronDays);
		}
		return null;

	}

	/**
	 * Generates a cron expression for a monthly schedule based on the provided cron
	 * 
	 * @param cronQuery the cron query string containing the day of the month and
	 *                  interval eg: "Day 2 of every 3 month"
	 * @return the cron expression for the monthly schedule
	 */
	private static String generateMonthlyCronExpression(String cronQuery) {
		Pattern pattern = Pattern.compile("Day\\s+(\\d+)\\s+of\\s+every\\s+(\\d+)\\s+month", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(cronQuery);

		if (matcher.matches()) {
			int dayOfMonth = Integer.parseInt(matcher.group(1));
			int interval = Integer.parseInt(matcher.group(2));

			if (dayOfMonth < 1 || dayOfMonth > 31) {
				throw new UserInputException("Invalid day of month: " + dayOfMonth);
			}

			if (interval < 1) {
				throw new UserInputException("Invalid interval number: " + interval);
			}

			return "0 0 0 " + dayOfMonth + " */" + interval + " *";
		} else {
			throw new UserInputException("Invalid monthly cron query format: " + cronQuery);
		}
	}

	/**
	 * Maps the day of the week to its corresponding cron value.
	 * 
	 * @param day the day of the week (e.g., "SUN", "MON", etc.)
	 * @return the cron value for the specified day
	 */
	private static String mapDayToCronValue(String day) {
		switch (day.toUpperCase()) {
			case "SUN":
				return "0";
			case "MON":
				return "1";
			case "TUE":
				return "2";
			case "WED":
				return "3";
			case "THU":
				return "4";
			case "FRI":
				return "5";
			case "SAT":
				return "6";
			default:
				throw new UserInputException(
						" The days expected are SUN, MON, TUE, WEB, THU, FRI, SAT, Invalid day selected :" + day);
		}
	}
}
