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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.rdkm.tdkservice.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enumeration representing different types of analysis defects.
 */
public enum AnalysisDefectType {

	/** Defect type for script issues */
	SCRIPT_ISSUE("Script Issue"),

	/** Defect type for environment issues */
	ENV_ISSUE("Environment Issue"),

	/** Defect type for interface changes */
	INTERFACE_CHANGE("Interface Change"),

	/** Defect type for RDK issues */
	RDK_ISSUE("RDK Issue"),

	/** Defect type for other issues */
	OTHER_ISSUE("Other Issue");

	private final String value;

	AnalysisDefectType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	/***
	 * Get the AnalysisDefectType from the given value.
	 * 
	 * @param value the value to match
	 * @return the AnalysisDefectType that matches the given value
	 */
	public static AnalysisDefectType getAnalysisDefectTypefromValue(String value) {
		for (AnalysisDefectType type : values()) {
			if (type.value.equalsIgnoreCase(value)) {
				return type;
			}
		}
		return null; // Or throw an exception if no match is found
	}

	/**
	 * Get all the values of the AnalysisDefectType.
	 * 
	 * @return the list of all the values of the AnalysisDefectType
	 */
	public static List<String> getAllValues() {
		return Arrays.stream(values()).map(AnalysisDefectType::getValue).collect(Collectors.toList());
	}
}