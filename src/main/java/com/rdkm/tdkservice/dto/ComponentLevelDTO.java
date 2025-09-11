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

import java.util.ArrayList;

import lombok.Data;

/**
 * Data Transfer Object for CI Component Level.
 * This class holds information about a CI component, including its name, status, and associated script details.
 */
@Data
public class ComponentLevelDTO {

	/**
	 * Name of the module.
	 */
	public String moduleName;

	/**
	 * Status of the module.
	 */
	public String moduleStatus;

	/**
	 * List of script details associated with the module.
	 */
	public ArrayList<ScriptDetailsDTO> scriptDetails;
}
