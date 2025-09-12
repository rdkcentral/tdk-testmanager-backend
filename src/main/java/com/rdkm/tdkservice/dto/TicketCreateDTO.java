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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object for creating a ticket.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TicketCreateDTO {

	/**
	 * The ID of the execution result. This field is mandatory.
	 */
	@NotBlank(message = "Execution Result Id is mandatory")
	private String executionResultId;

	/**
	 * The name of the project. This field is mandatory.
	 */
	@NotBlank(message = "Project Name is mandatory")
	private String projectName;

	/**
	 * A summary of the issue. This field is mandatory.
	 */
	@NotBlank(message = "Issue Summary is mandatory")
	private String issueSummary;

	/**
	 * A detailed description of the issue. This field is mandatory.
	 */
	@NotBlank(message = "Issue Description is mandatory")
	private String issueDescription;

	/**
	 * The type of the issue. This field is mandatory.
	 */
	@NotBlank(message = "Issue Type is mandatory")
	private String issueType;

	/**
	 * The priority of the issue. This field is mandatory.
	 */
	@NotBlank(message = "Issue Priority is mandatory")
	private String priority;

	/**
	 * The labels associated with the issue. This field is mandatory.
	 */
	private List<String> label;

	/**
	 * The release version in which the issue was found.
	 */
	private String releaseVersion;

	/**
	 * The hardware configuration related to the issue.
	 */
	private String hardwareConfig;

	/**
	 * The platforms impacted by the issue.
	 */
	private List<String> impactedPlatforms;

	/**
	 * The environment setup for testing.
	 */
	private String environmentForTestSetup;

	/**
	 * The reproducibility of the issue.
	 */
	private int reproducability;

	/**
	 * The steps to reproduce the issue.
	 */
	private String stepsToReproduce;

	/**
	 * The components impacted by the issue.
	 */
	private List<String> componentsImpacted;

	/**
	 * The version in which the issue is fixed.
	 */
	private String fixedInVersion;

	/**
	 * Any third-party dependencies related to the issue.
	 */
	private String thirdPartyDependency;

	/**
	 * The severity of the issue.
	 */
	private String severity;

	/**
	 * The acceptance criteria for the issue.
	 */
	private String rdkVersion;

	/**
	 * The version of TDK related to the issue.
	 */
	private String tdkVersion;

	/**
	 * The user reporting the issue.
	 */
	private String user;

	/**
	 * The password of the user reporting the issue.
	 */
	private String password;

	/**
	 * Indicates if the device log is required.
	 */
	private boolean isDeviceLogRequired;

	/**
	 * Indicates if the execution log is required.
	 */
	private boolean isExecutionLogRequired;

	/**
	 * The type of defect.
	 */
	private String analysisDefectType;

	/**
	 * Any remarks from the analysis.
	 */
	private String analysisRemark;

	/**
	 * The User who analyse the isuue.
	 */
	private String analysisUser;
	
	/**
	 * The category of the issue.
	 */
	private String category;
}
