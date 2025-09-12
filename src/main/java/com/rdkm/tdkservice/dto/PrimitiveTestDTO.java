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

import lombok.Data;

/*
 * The PrimitiveTestDTO class is used to get the primitive test details response for get operations
 */
@Data
public class PrimitiveTestDTO {

	/*
	 * The primitive test id .
	 */
	private UUID primitiveTestId;

	/*
	 * The primitive test name .
	 */
	private String primitiveTestName;

	/*
	 * The primitive test module name .
	 */
	private String primitivetestModule;

	/*
	 * The primitive test function name .
	 * 
	 */
	private String primitiveTestfunction;

	/*
	 * The primitive test parameter DTO .
	 */
	private List<PrimitiveTestParameterDTO> primitiveTestParameters;

}
