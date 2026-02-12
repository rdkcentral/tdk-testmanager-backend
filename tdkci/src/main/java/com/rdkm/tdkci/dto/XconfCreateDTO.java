/*
* If not stated otherwise in this file or this component's LICENSE file the
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
package com.rdkm.tdkci.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object for creating Xconf configuration.
 * Contains details required for Xconf device type and configuration.
 */
@Data
public class XconfCreateDTO {

	/**
	 * Name of the Xconf  name.
	 * This field is required and must not be blank.
	 */
	@NotBlank(message = "Xconf Device Type Name is required")
	private String name;

	/**
	 * Name of the Xconf configuration.
	 * This field is required and must not be blank.
	 */
	@NotBlank(message = "Xconf Config Name is required")
	private String xconfName;

	/**
	 * Identifier for the Xconf configuration.
	 * This field is required and must not be blank.
	 */
	@NotBlank(message = "Xconf Config Id is required")
	private String xconfConfigId;

	/**
	 * Description of the Xconf configuration.
	 * This field is required and must not be blank.
	 */
	@NotBlank(message = "Xconf Config Description is required")
	private String xconfConfigDescription;
}
