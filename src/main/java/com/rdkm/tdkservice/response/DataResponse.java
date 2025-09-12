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

package com.rdkm.tdkservice.response;

import lombok.Getter;

/**
 * This class is used to return success response with data
 */
@Getter
public class DataResponse extends Response {

	/**
	 * This holds the data
	 */
	Object data;

	/**
	 * Constructor to initialize the SuccessDataResponse with message, statusCode,
	 * and data.
	 *
	 * @param message    The success message.
	 * @param statusCode The HTTP status code.
	 * @param data       The data to be returned.
	 */
	public DataResponse(String message, int statusCode, Object data) {
		super(message, statusCode);
		this.data = data;
	}

}
