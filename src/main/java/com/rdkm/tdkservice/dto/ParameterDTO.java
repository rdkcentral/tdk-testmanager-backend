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

import java.util.UUID;

import com.rdkm.tdkservice.enums.ParameterDataType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Data Transfer Object for a parameter type.
 */
@Data
public class ParameterDTO {

	/**
	 * The unique identifier of the parameter type.
	 */
	@NotNull(message = "ID is required")
	private UUID id;

	/**
	 * The name of the parameter type.
	 */
	private String parameterName;

	/**
	 * The enumeration of the parameter type.
	 */
	private ParameterDataType parameterDataType;

	/**
	 * The range value of the parameter type.
	 */
	private String parameterRangeVal;

	/**
	 * The function associated with the parameter type.
	 */
	private String function;
}