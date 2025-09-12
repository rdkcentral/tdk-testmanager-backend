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

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Data Transfer Object for creating a new module.
 */
@Data
public class ModuleCreateDTO {

	/**
	 * The name of the module.
	 */
	@NotBlank(message = "Name is required")
	private String moduleName;

	/**
	 * The test group associated with the module.
	 */
	@NotBlank(message = "Test group is required")
	private String testGroup;

	/**
	 * The execution time of the module.
	 */
	@NotNull(message = "Execution time is required")
	private Integer executionTime;

	/**
	 * The user group associated with the module.
	 */
	private String userGroup;

	/**
	 * The set of log file names associated with the module.
	 */
	private Set<String> moduleLogFileNames;

	/**
	 * The set of crash log files associated with the module.
	 */
	private Set<String> moduleCrashLogFiles;

	/**
	 * The category of the module.
	 */
	@NotBlank(message = "Category is required")
	private String moduleCategory;

	/**
	 * The flag indicating whether the module is enabled.
	 */
	private boolean isModuleThunderEnabled;
}