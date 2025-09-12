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

import java.util.List;

import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.enums.ExecutionOverallResultStatus;
import com.rdkm.tdkservice.enums.ExecutionProgressStatus;
import com.rdkm.tdkservice.enums.ExecutionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents the execution entity. This class is used to store execution
 * information.
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Execution extends BaseEntity {

	/**
	 * Name of the execution
	 */
	@Column(nullable = false, unique = true)
	private String name;

	/*
	 * The script test suite name
	 */
	private String scripttestSuiteName;

	/**
	 * The execution time to run script
	 */
	private double realExecutionTime;

	/*
	 * Execution time for the full execution
	 */
	private double executionTime;

	/*
	 * The execution type
	 */
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private ExecutionType executionType;

	/*
	 * The execution progress status
	 */
	@Column(nullable = true)
	@Enumerated(EnumType.STRING)
	private ExecutionProgressStatus executionStatus;

	/*
	 * Overall result status - Success or Failure
	 */
	@Enumerated(EnumType.STRING)
	private ExecutionOverallResultStatus result;

	/*
	 * The user
	 *
	 */
	private String user;

	/*
	 * The test type
	 */
	private String testType;

	/*
	 * is rerun on failure
	 */
	private boolean isRerunOnFailure = false;

	/*
	 * is abort requested
	 */
	private boolean isAbortRequested = false;

	/*
	 * Represents the execution repeat count.
	 */
	private int repeatCount;

	/*
	 * Represents the is device Logs Needed.
	 */
	private boolean isDeviceLogsNeeded = false;

	/*
	 * Represents the is performance logs needed.
	 */
	private boolean isPerformanceLogsNeeded = false;

	/*
	 * Represents the is diagnostic logs needed.
	 */
	private boolean isDiagnosticLogsNeeded = false;

	/*
	 * Represents the execution category.
	 */
	@Enumerated(EnumType.STRING)
	private Category category;

	/*
	 * Represents the execution results.
	 */
	@OneToMany(mappedBy = "execution", fetch = FetchType.EAGER)
	@OrderBy("createdDate ASC")
	private List<ExecutionResult> executionResults;

	/*
	 * Represents the execution devices.
	 */

	@OneToOne
	ExecutionDevice executionDevice;

}
