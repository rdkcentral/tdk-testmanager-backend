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

import com.rdkm.tdkservice.enums.ExecutionMethodResultStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents the execution method result. This class is used to store the
 * execution method result information.
 * 
 * Fields: - functionName: The name of the function. - expectedResult: The
 * expected result from the script. - actualResult: The actual result sent from
 * the script. - methodResult: The calculated result.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class ExecutionMethodResult extends BaseEntity {

	/**
	 * The name of the function.
	 */
	private String functionName;

	/*
	 * Expected result from script
	 */
	@Enumerated(EnumType.STRING)
	private ExecutionMethodResultStatus expectedResult;

	/*
	 * Actual result sent from script
	 */
	@Enumerated(EnumType.STRING)
	private ExecutionMethodResultStatus actualResult;

	/*
	 * Calculated result
	 */
	@Enumerated(EnumType.STRING)
	private ExecutionMethodResultStatus methodResult;

	/*
	 * Represents the execution result.
	 */
	@ManyToOne
	@JoinColumn(name = "execution_result_id", nullable = false)
	@NotNull
	private ExecutionResult executionResult;
}