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

import com.rdkm.tdkservice.model.Device;
import com.rdkm.tdkservice.model.Script;
import com.rdkm.tdkservice.model.TestSuite;
import com.rdkm.tdkservice.model.User;

import lombok.Data;

/**
 * Represents the execution details DTO.
 */
@Data
public class ExecutionDetailsDTO {

	/*
	 * Represents the list of devices.
	 */
	private List<Device> deviceList;
	/*
	 * Represents the list of scripts.
	 */
	private List<Script> scriptList;
	/*
	 * Represents the test suite.
	 */
	private List<TestSuite> testSuite;

	/*
	 * Represents the test type.
	 */
	private String testType;
	/*
	 * Represents the user.
	 */
	private String user;
	/*
	 * Represents the category.
	 */
	private String category;
	/*
	 * Represents the execution name.
	 */
	private String executionName;
	/*
	 * Represents the execution repeat count.
	 */
	private int repeatCount;

	/*
	 * Represents the is rerun on failure.
	 */
	private boolean isRerunOnFailure;

	/*
	 * Represents the is device Logs Needed.
	 */
	private boolean isDeviceLogsNeeded;

	/*
	 * Represents the is performance logs needed.
	 */
	private boolean isPerformanceLogsNeeded;

	/*
	 * Represents the is diagnostic logs needed.
	 */
	private boolean isDiagnosticLogsNeeded;

	/*
	 * Represents the is individual repeat execution.
	 */
	private boolean isIndividualRepeatExecution;

	/*
	 * Represents the callback url.This call back url is coming from CI portal and
	 * this ci execution trigger will get added to ci_portal.The url of this
	 * ci_portal is mentioned in this callbackurl.
	 */
	private String callBackUrl;

	/*
	 * Represents the image version that is using for the ci trigger.
	 */
	private String imageVersion;

}
