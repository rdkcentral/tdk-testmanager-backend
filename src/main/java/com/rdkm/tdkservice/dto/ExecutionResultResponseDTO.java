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

import java.util.List;

import lombok.Data;

@Data
public class ExecutionResultResponseDTO {

	/**
	 * The name of the script that was executed.
	 */
	private String script;

	/**
	 * The time taken to execute the script, in seconds.
	 */
	private double timeTaken;

	/**
	 * The number of test cases included in the script.
	 */
	private int testCaseCount;

	/**
	 * A list of results for each method executed within the script, each
	 * encapsulated in an ExecutionMethodResultResponseDTO object.
	 */
	private List<ExecutionMethodResultResponseDTO> executionMethodResult;

	/**
	 * A list of trends observed during the execution.
	 */
	private List<String> executionTrend;

	/**
	 * The logs generated during the execution.
	 */
	private String logs;

	/**
	 * The device on which the script was executed.
	 */
	private String executionDevice;

	/**
	 * The status of the execution result (e.g., "Passed", "Failed", "In Progress").
	 */
	private String executionResultStatus;

	/**
	 * The URL to access the execution logs.
	 */
	private String agentLogUrl;

	/**
	 * Indicates whether agent logs are available.
	 */
	private boolean isAgentLogsAbvailable;

	/**
	 * Indicates whether crash logs are available.
	 */
	private boolean isCrashLogsAvailable;

	/**
	 * Indicates whether device logs are available.
	 */
	private boolean isDeviceLogsAvailable;
}
