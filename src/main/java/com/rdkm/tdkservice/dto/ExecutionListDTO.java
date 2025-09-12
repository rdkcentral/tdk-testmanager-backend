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
public class ExecutionListDTO {

	/**
	 * The unique identifier for the execution.
	 */
	private String executionId;

	/**
	 * The name of the execution.
	 */
	private String executionName;

	/**
	 * The date and time when the execution was performed.
	 */
	private Instant executionDate;

	/**
	 * The name of the script or test suite that was executed.
	 */
	private String scriptTestSuite;

	/**
	 * The device on which the execution was performed.
	 */
	private String device;

	/**
	 * The category of the execution.
	 */
	boolean isAbortNeeded;

	/**
	 * The status of the execution (
	 */
	private String status;

	/**
	 * The user who initiated the execution.
	 */
	private String user;

}
