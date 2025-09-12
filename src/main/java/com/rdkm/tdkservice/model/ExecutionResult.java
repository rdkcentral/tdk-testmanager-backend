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

import java.time.Instant;

import com.rdkm.tdkservice.enums.ExecutionResultStatus;
import com.rdkm.tdkservice.enums.ExecutionStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents the execution result. This class is used to store the execution
 * result information.
 * 
 * Fields: - script: The script that was executed. - result: The result of the
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class ExecutionResult extends BaseEntity {

	/**
	 * The script that was executed.
	 */
	String script;

	/**
	 * The result of the execution.
	 */
	@Enumerated(EnumType.STRING)
	ExecutionResultStatus result;

	/**
	 * The status of the execution script.
	 */
	@Enumerated(EnumType.STRING)
	ExecutionStatus status;

	/*
	 * The date of execution of the script
	 */
	private Instant dateOfExecution;

	/*
	 * Execution time taken for the execution of the script
	 */
	double executionTime;

	/**
	 * The output location of the execution
	 */
	String executionOutputLocation;

	/**
	 * For storing the remarks of the execution,or reason other than the logs
	 */
	String executionRemarks;

	/**
	 * The execution entity reference
	 */
	@ManyToOne
	@JoinColumn(name = "execution_id")
	private Execution execution;
}