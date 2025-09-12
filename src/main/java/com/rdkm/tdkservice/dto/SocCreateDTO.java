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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object for Soc. This class is used to transfer data between
 * different parts of the application. It includes the necessary Jackson
 * annotations to ignore unknown properties and include non-null fields when
 * serializing to JSON.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SocCreateDTO {

	/**
	 * Represents the name of the SocVendor. This field is mandatory, hence it
	 * cannot be blank.
	 */
	@NotBlank(message = "Soc name is required")
	private String socName;

	/**
	 * Represents the category of the Soc. This field is mandatory, hence it cannot
	 * be blank.
	 */
	@NotBlank(message = "Category is required")
	private String socCategory;

	/**
	 * Represents the user group of the Soc. This field is optional and can be null.
	 */
	private String socUserGroup;

}
