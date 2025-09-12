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
package com.rdkm.tdkservice.exception;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * This is a custom exception class that is thrown when a not exists in the
 * database. This is used in spring boot exception handling for returning
 * appropriate response to the client.
 */
@NoArgsConstructor
@AllArgsConstructor
public class ResourceNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private String resourceType;
	private String resourceName;

	/**
	 * This method returns the exception message
	 */
	public String getMessage() {
		if (resourceName == null)
			return null;
		return String.format(" %s - '%s' doesnt exist", resourceType, resourceName);
	}

}