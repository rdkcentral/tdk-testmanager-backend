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

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Data Transfer Object for a function.
 */
@Data
public class FunctionDTO {

	/**
	 * The unique identifier of the function.
	 */
	@NotNull(message = "ID is required")
	private UUID id;

	/**
	 * The name of the function.
	 */

	private String functionName;

	/**
	 * The name of the module to which the function belongs.
	 */
	private String moduleName;

	/**
	 * The category of the function.
	 */
	private String functionCategory;
}