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

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object for Execution Search Filter. This DTO is used to
 * encapsulate the search filter criteria for fetching executions.
 */
@Data
public class ExecutionSearchFilterDTO {

	/**
	 * The start date for the execution search filter.
	 */
	@NotBlank
	Instant startDate;

	/**
	 * The end date for the execution search filter.
	 */
	@NotBlank
	Instant endDate;

	/**
	 * The type of execution to filter by - say script, testsuite etc[.
	 */
	String executionType;

	/**
	 * The script test suite to filter by.
	 */
	String scriptTestSuite;

	/**
	 * The type of device to filter by.
	 */
	String deviceType;

	/**
	 * The category of the execution
	 */
	@NotBlank
	String category;

	/**
	 * The maximum number of results to return.
	 */
	int sizeLimit;

}