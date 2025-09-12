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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * Data Transfer Object for creating a new script.
 */
@Data
public class ScriptCreateDTO {

	/**
	 * The name of the Script.
	 */
	@NotBlank(message = "Name is required")
	private String name;

	/**
	 * The description of the script.
	 */
	@NotBlank(message = "Synopsis is required")
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
	@NotBlank(message = "Primitive test is required")
	private String primitiveTestName;

	/**
	 * List of deviceTypes that the script is associated with.
	 */
	private List<String> deviceTypes;

	/**
	 * The user group associated with the module.
	 */
	private String userGroup;

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
	@NotBlank(message = "Test ID is required")
	private String testId;

	/**
	 * Objective of the test case
	 */
	@NotBlank(message = "Objective is required")
	private String objective;


	/**
	 * Prerequisites for the testcase
	 */
	@NotEmpty(message = "Preconditions are required")
	private List<String> preConditions;

	/**
	 * Automation Approach or steps
	 */
	@Valid
	private List<TestStepCreateDTO> testSteps;

	/**
	 * Priority of the test case
	 */
	@NotBlank(message = "Priority is required")
	private String priority;

	/**
	 * Release version of the test
	 */
	private String releaseVersion;

}
