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

import lombok.Data;

/**
 * Data Transfer Object (DTO) for summarizing analysis issues by type. This
 * class uses Lombok's @Data annotation to generate boilerplate code such as
 * getters, setters, toString, equals, and hashCode methods.
 */
@Data
public class AnalysisIssueTypewiseSummaryDTO {

	/** Number of script issues */
	int scriptIssue;

	/** Number of environment issues */
	int envIssue;

	/** Number of interface change issues */
	int interfaceChange;

	/** Number of RDK issues */
	int rdkIssue;

	/** Number of other issues */
	int otherIssue;

	/** Number of failures */
	int failure;

	/** Number of analysed issues */
	int analysed;

	/** Number of not analysed issues */
	int notAnalysed;

	/** Percentage of analysed issues */
	int percentageAnalysed;

}
