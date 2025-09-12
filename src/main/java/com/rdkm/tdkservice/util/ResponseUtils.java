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

package com.rdkm.tdkservice.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.response.Response;

/**
 * Utility class for creating standardized HTTP responses.
 */
public class ResponseUtils {

	/**
	 * Creates a ResponseEntity for successful resource creation (HTTP 201 Created).
	 * Includes a message and optionally the ID of the created resource.
	 *
	 * @param message    The success message.
	 * @param resourceId The ID of the created resource (optional, can be null).
	 * @return ResponseEntity with the success message and ID in JSON format, 201
	 *         Created status.
	 */
	public static ResponseEntity<Response> getCreatedResponse(String message) {
		Response response = new Response(message, HttpStatus.CREATED.value());
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

	/**
	 * Creates a ResponseEntity for a standard successful operation (HTTP 200 OK).
	 * Includes a message.
	 *
	 * @param message The success message.
	 * @return ResponseEntity with the success message in JSON format, 200 OK
	 *         status.
	 */
	public static ResponseEntity<Response> getSuccessResponse(String message) {
		Response response = new Response(message, HttpStatus.OK.value());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	/**
	 * Creates a ResponseEntity for a standard successful operation (HTTP 200 OK).
	 * Includes a message and data.
	 *
	 * @param message The success message.
	 * @param data    The data to be returned.
	 * @return ResponseEntity with the success message and data in JSON format, 200
	 *         OK status.
	 */
	public static ResponseEntity<DataResponse> getSuccessDataResponse(Object data) {
		DataResponse successDataResponse = new DataResponse("Data fetched successfully", HttpStatus.OK.value(), data);
		return new ResponseEntity<>(successDataResponse, HttpStatus.OK);
	}

	/**
	 * Creates a ResponseEntity for a standard successful operation (HTTP 200 OK).
	 * Includes a message and data.
	 *
	 * @param message The success message.
	 * @param data    The data to be returned.
	 * @return ResponseEntity with the success message and data in JSON format, 200
	 *         OK status.
	 */
	public static ResponseEntity<DataResponse> getSuccessDataResponse(String message, Object data) {
		DataResponse successDataResponse = new DataResponse(message, HttpStatus.OK.value(), data);
		return new ResponseEntity<>(successDataResponse, HttpStatus.OK);
	}

	/**
	 * Creates a ResponseEntity for a standard successful operation (HTTP 404 OK).
	 * 
	 * @param message The success message.
	 * @param data    The data to be returned.
	 * @return ResponseEntity<DataResponse>
	 */
	public static ResponseEntity<DataResponse> getNotFoundDataResponse(String message, Object data) {
		DataResponse successDataResponse = new DataResponse(message, HttpStatus.NOT_FOUND.value(), data);
		return new ResponseEntity<>(successDataResponse, HttpStatus.NOT_FOUND);
	}

	/**
	 * Creates a ResponseEntity for a standard dataespecially configuration data not
	 * available in the backend.
	 * 
	 * @param message The success message.
	 * @param data    The data to be returned.
	 * @return ResponseEntity<DataResponse with the success message and data in JSON
	 *         format, 503
	 */
	public static ResponseEntity<DataResponse> getNotFoundDataConfigResponse(String message, Object data) {
		DataResponse successDataResponse = new DataResponse(message, HttpStatus.SERVICE_UNAVAILABLE.value(), data);
		return new ResponseEntity<>(successDataResponse, HttpStatus.SERVICE_UNAVAILABLE);

	}

	/**
	 * Creates a ResponseEntity for a standard successful operation (HTTP 200 OK).
	 * Includes a message and data.
	 *
	 * @param message The success message.
	 * @param data    The data to be returned.
	 * @return ResponseEntity with the success message and data in JSON format, 200
	 *         OK status.
	 */
	public static ResponseEntity<DataResponse> getSignInResponse(Object data) {
		DataResponse successDataResponse = new DataResponse("Sign is successful", HttpStatus.OK.value(), data);
		return new ResponseEntity<>(successDataResponse, HttpStatus.OK);
	}

	/**
	 * Creates a ResponseEntity for a no content operation (HTTP 404 Not FOund).
	 *
	 * @return ResponseEntity with Response and 404 Not Found status.
	 */
	public static ResponseEntity<Response> getNotFoundResponse(String message) {
		Response response = new Response(message, HttpStatus.NOT_FOUND.value());
		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}

	/**
	 * Creates a ResponseEntity for a no content operation (HTTP 204 No Content).
	 *
	 * @return ResponseEntity with Response and 204 No Content status.
	 */
	public static ResponseEntity<Response> getNoContent() {
		Response response = new Response("No content", HttpStatus.NO_CONTENT.value());
		return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
	}

	/**
	 * Creates a ResponseEntity for a no content operation (HTTP 204 No Content).
	 *
	 * @return ResponseEntity with Response and 204 No Content status.
	 */
	public static ResponseEntity<Response> getNoContent(String message) {
		Response response = new Response(message, HttpStatus.NO_CONTENT.value());
		return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
	}

}