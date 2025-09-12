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

import lombok.Data;

@Data
public class ExecutionSummaryResponseDTO {

	/**
	 * The total number of scripts in the execution summary.
	 */
	private int totalScripts;

	/**
	 * The number of scripts that have been executed.
	 */
	private int executed;

	/**
	 * The number of scripts that executed successfully.
	 */
	private int success;

	/**
	 * The number of scripts that failed during execution.
	 */
	private int failure;

	/**
	 * The number of scripts that were not applicable (N/A).
	 */
	private int na;

	/**
	 * The number of scripts that timed out during execution.
	 */
	private int timeout;

	/**
	 * The number of scripts that are pending execution.
	 */
	private int pending;

	/**
	 * The number of scripts that will be Inprogress.
	 */
	private int inProgressCount;

	/**
	 * The number of scripts that are skipped.
	 */
	private int skipped;

	/**
	 * The number of scripts that are aborted.
	 */
	private int aborted;

	/**
	 * The success percentage of the execution
	 */
	private double successPercentage;

}
