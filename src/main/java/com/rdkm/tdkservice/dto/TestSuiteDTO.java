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
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * The TestSuiteDTO class is used to store test suite data. This class is used
 * to transfer the data from the service layer to the controller layer.
 */
@Data
public class TestSuiteDTO {

	/**
	 * The id of the test suite.
	 */
	@NotNull(message = "Test suite id cannot be null")
	private UUID id;

	/**
	 * The name of the test suite.
	 */
	private String name;

	/**
	 * The description of the test suite.
	 */
	private String description;

	/**
	 * The category of the test suite.
	 */
	private String category;

	/**
	 * The scripts of the test suite.
	 */
	private List<ScriptListDTO> scripts;

}
