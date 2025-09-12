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

import lombok.Data;

@Data
public class ExecutionSchedulesResponseDTO {

	/**
	 * The unique identifier for the execution schedule.
	 */
	private String id;

	/**
	 * The time at which the execution is scheduled to occur.
	 */
	private Instant executionTime;
	
	/**
	 * The start time for the cron job.
	 */
	private Instant cronStartTime;
	
	/**
	 * The end time for the cron job.
	 */
	private Instant cronEndTime;

	/**
	 * The name of the job associated with the execution schedule.
	 */
	private String jobName;

	/**
	 * The name of the script or test suite that is scheduled to be executed.
	 */
	private String scriptTestSuite;

	/**
	 * The device on which the execution is scheduled to be performed.
	 */
	private String device;

	/**
	 * Additional details about the execution schedule.
	 */
	private String details;

	/**
	 * The status of the execution schedule (e.g., "SCHEDULED", "COMPLETED").
	 */
	private String status;

	/**
	 * The type of schedule (e.g., "ONCE", "REPEAT").
	 */
	private String scheduleType;

}
