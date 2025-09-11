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
 * Data Transfer Object for CI Script Details.
 * This class holds information about a CI script, including its name, status, log URL, and associated test information.
 */
@Data
public class ScriptDetailsDTO {

	/**
	 * The name of the CI script.
	 */
	public String scriptName;

	/**
	 * The status of the CI script.
	 */
	public String scriptStatus;

	/**
	 * The URL to the log file for the CI script.
	 */
	public String logUrl;

	/**
	 * A list of test information associated with the CI script.
	 */
	public ArrayList<TestInfoDTO> testInfo;

}
