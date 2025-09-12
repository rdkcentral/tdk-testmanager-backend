
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

import lombok.Data;

/**
 * Represents the execution entities. This class is used to transfer execution
 * information.
 */
@Data
public class ExecutionEntities {

	/**
	 * Represents the execution.
	 */
	Execution execution;

	/**
	 * Represents the execution device.
	 */
	ExecutionDevice executionDevice;

	/**
	 * Represents the execution results.
	 */
	List<ExecutionResult> executionResult;

}
