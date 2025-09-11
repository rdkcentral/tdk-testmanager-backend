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
package com.rdkm.tdkservice.dto;

import java.time.Instant;

import com.rdkm.tdkservice.enums.ScheduleType;

import lombok.Data;

/**
 * Data Transfer Object for Execution Schedule. This class is used to transfer
 * execution schedule data between processes.
 */
@Data
public class ExecutionScheduleDTO {

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
	 * This is the Cron Expression for scheduling the execution
	 */
	private String cronExpression;

	/**
	 * The type of cron expression.
	 */
	private String cronType;

	/**
	 * The cron query for scheduling the execution in a unique human readable format
	 */
	private String cronQuery;

	/**
	 * The type of schedule.
	 * 
	 * @see com.rdkm.tdkservice.enums.ScheduleType
	 */
	private ScheduleType scheduleType;

	/**
	 * The trigger details for the execution.
	 * 
	 * @see ExecutionTriggerDTO
	 */
	private ExecutionTriggerDTO executionTriggerDTO;

}
