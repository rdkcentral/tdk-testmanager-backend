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
 * CIResultDTO is a Data Transfer Object that holds the result of a CI execution.
 * It contains the name of the execution and details of the devices involved.
 */
@Data
public class DetailedResultDTO {

	/**
	 * The name of the CI execution.
	 */
	public String executionName;
	
	/**
	 * The status of the CI execution.
	 */
	private String executionStatus;
	
	/**
	 * The script or test suite used in the CI execution.
	 */
	private String scriptOrTestSuite;

	/**
	 * A list of device details involved in the CI execution.
	 */
	public ArrayList<DeviceDetailsDTO> deviceDetails;
}
