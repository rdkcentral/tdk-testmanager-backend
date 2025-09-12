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
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class ExecutionDetailsResponseDTO {
	/**
	 * The name of the device on which the execution was performed.
	 */
	private String deviceName;

	/**
	 * The IP address of the device on which the execution was performed.
	 */
	private String deviceIP;

	/**
	 * The MAC address of the device on which the execution was performed.
	 */
	private String deviceMac;

	/**
	 * Additional details about the device on which the execution was performed.
	 */
	private String deviceDetails;

	/**
	 * The image name of the image flashed in the device under test
	 */
	private String deviceImageName;

	/**
	 * The date and time when the execution was performed.
	 */
	private Instant dateOfExecution;

	/**
	 * The total time taken for the execution in seconds.
	 */
	private double totalExecutionTime;

	/**
	 * The type of execution performed .
	 */
	private String executionType;

	/**
	 * The name of the script or test suite that was executed.
	 */
	private String scriptTestSuite;

	/**
	 * The status of the execution .
	 */
	private String executionStatus;

	/**
	 * The result of the execution.
	 */
	private String result;

	/**
	 * The real execution time for executing the scripts
	 */
	private double realExecutionTime;
	
	/*
	 * * Indicates whether the device is a Thunder-enabled device.
	 */
	private boolean isDeviceThunderEnabled;

	/**
	 * A summary of the execution, encapsulated in an ExecutionSummaryResponseDTO
	 * object.
	 */
	private ExecutionSummaryResponseDTO summary;
	
	/**
	 * A map of detailed execution summaries, where the key is a string identifier
	 * and the value is an ExecutionSummaryResponseDTO object.
	 */
	private Map<String ,ExecutionSummaryResponseDTO> detailMap;

	/**
	 * A list of detailed execution results, each encapsulated in an
	 * ExecutionResultDTO object.
	 */
	private List<ExecutionResultDTO> executionResults;

}
