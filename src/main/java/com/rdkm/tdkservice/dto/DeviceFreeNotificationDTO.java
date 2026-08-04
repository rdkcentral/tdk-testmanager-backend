/*
* If not stated otherwise in this file or this component's LICENSE file the
* following copyright and licenses apply:
*
* Copyright 2026 RDK Management
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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal payload sent to the internal CI application when a device becomes
 * free after an execution completes, so the CI app can correlate the result
 * back to its job and process the next queued execution.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceFreeNotificationDTO {

	/**
	 * The CI job ID originally passed by the CI app when triggering the execution.
	 * The CI app uses this to correlate the notification to the exact job it
	 * submitted.
	 * Null if the execution was not triggered by the CI app.
	 */
	private String ciJobId;

	/**
	 * Name of the device that is now free and available for the next queued job.
	 */
	private String deviceName;

	/**
	 * Overall result of the execution.
	 * Possible values: SUCCESS, FAILURE, ABORTED, TIMEOUT.
	 */
	private String overallResult;

	/**
	 * Final progress status of the execution (reserved for future use).
	 * Possible values: COMPLETED, ABORTED, PAUSED.
	 */
	private String executionStatus;

}
