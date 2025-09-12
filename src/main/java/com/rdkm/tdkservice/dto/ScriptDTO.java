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
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Data Transfer Object for script.
 */
@Data
public class ScriptDTO {

	/**
	 * The id of the Script.
	 */
	@NotNull(message = "Script  id cannot be null")
	private UUID id;

	/**
	 * The name of the Script.
	 */
	private String name;

	/**
	 * The description of the script.
	 */
	private String synopsis;

	/**
	 * The execution time of the script.
	 */
	private int executionTimeOut;

	/**
	 * Is the script long duration
	 */
	private boolean isLongDuration = false;

	/**
	 * Primitive test name
	 */
	private String primitiveTestName;

	/**
	 * The moduleName of the script.
	 */
	private String moduleName;

	/**
	 * List of device types that the script is associated with.
	 */
	private List<String> deviceTypes;

	/**
	 * true if script needs to be skipped while executing test suite
	 */
	boolean skipExecution = false;

	/**
	 * Remarks for skipping the script
	 */
	String skipRemarks;

	/**
	 * The testID of the script,say CT_Aamp_39
	 */
	private String testId;

	/**
	 * Objective of the test case
	 */
	private String objective;

	/**
	 * Prerequisites for the testcase
	 */
	private List<PreConditionDTO> preConditions;

	/**
	 * Automation Approach or steps
	 */
	private List<TestStepDTO> testSteps;

	/**
	 * Priority of the test case
	 */
	private String priority;


	/**
	 * Release version of the test
	 */
	private String releaseVersion;


	/**
	 * The script content
	 */
	private String scriptContent;

}
