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
package com.rdkm.tdkservice.model;

import java.time.Instant;

import com.rdkm.tdkservice.enums.ScheduleStatus;
import com.rdkm.tdkservice.enums.ScheduleType;
import com.rdkm.tdkservice.enums.Category;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents an execution schedule entity. This class is used to store the
 * details of an execution schedule.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class ExecutionSchedule extends BaseEntity {

	/**
	 * The time at which the execution is scheduled.
	 */
	private Instant executionTime;

	/**
	 * The start time for the cron job
	 */
	private Instant cronStartTime;
	
	/**
	 * The end time for the cron job
	 */
	private Instant cronEndTime;
	
	
	/**
	 * The cron expression for scheduling.
	 */
	private String cronExpression;
	
	/**
	 * Cron query in human-readable format to be displayed on UI.
	 */
	private String cronQuery;

	/**
	 * The type of schedule.
	 * 
	 * @see com.rdkm.tdkservice.enums.ScheduleType
	 */
	@Enumerated(EnumType.STRING)
	private ScheduleType scheduleType;



	/**
	 * The status of the schedule.
	 * 
	 * @see com.rdkm.tdkservice.enums.ScheduleStatus
	 */
	@Enumerated(EnumType.STRING)
	private ScheduleStatus scheduleStatus;

	/**
	 * The list of devices for the execution.
	 */
	private String device;

	/**
	 * Represents the list of scripts.
	 */
	private String scriptList;

	/**
	 * Represents the test suite.
	 */
	private String testSuite;

	/**
	 * Represents the test type.
	 */
	private String testType;

	/**
	 * Represents the user.
	 */
	private String user;

	/**
	 * Represents the category.
	 */
	@Enumerated(EnumType.STRING)
	private Category category;

	/**
	 * Represents the execution name.
	 */
	private String executionName;

	/**
	 * Represents the execution repeat count.
	 */
	private int repeatCount;

	/**
	 * Represents whether to rerun on failure.
	 */
	private boolean isRerunOnFailure;

	/**
	 * Represents whether device logs are needed.
	 */
	private boolean isDeviceLogsNeeded;

	/**
	 * Represents whether performance logs are needed.
	 */
	private boolean isPerformanceLogsNeeded;

	/**
	 * Represents whether diagnostic logs are needed.
	 */
	private boolean isDiagnosticLogsNeeded;

	/**
	 * Represents the is individual repeat execution.
	 */
	private boolean isIndividualRepeatExecution;

	/**
	 * Represents the callback url.This call back url is coming from CI portal and
	 * this ci execution trigger will get added to ci_portal.The url of this
	 * ci_portal is mentioned in this callbackurl.
	 */
	private String ciCallBackUrl;

	/**
	 * Represents the image version that is using for the ci trigger.
	 */
	private String ciImageVersion;
}
