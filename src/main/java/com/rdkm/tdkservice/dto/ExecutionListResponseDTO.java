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

import lombok.Data;

@Data
public class ExecutionListResponseDTO {

	/**
	 * A list of execution details, each encapsulated in an ExecutionListDTO object.
	 */
	private List<ExecutionListDTO> executions;

	/**
	 * The current page number in the paginated list of executions.
	 */
	private int currentPage;

	/**
	 * The total number of items (executions) available.
	 */
	private long totalItems;

	/**
	 * The total number of pages available in the paginated list of executions.
	 */
	private int totalPages;

}
