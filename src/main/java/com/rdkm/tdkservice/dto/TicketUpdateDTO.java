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
 * Data Transfer Object for updating a ticket.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TicketUpdateDTO {

	/**
	 * The ID of the execution result. Cannot be empty.
	 */
	@NotBlank(message = "ExecutionResult Id cannot be empty")
	private String executionResultId;

	/**
	 * The number of the ticket. Cannot be empty.
	 */
	@NotBlank(message = "Ticket number cannot be empty")
	private String ticketNumber;

	/**
	 * Additional comments for the ticket.
	 */
	private String comments;

	/**
	 * The label associated with the ticket.
	 */
	private List<String> label;

	/**
	 * Indicates if device log is needed.
	 */
	boolean isDeviceLogNeeded;

	/**
	 * Indicates if execution log is needed.
	 */
	boolean isExecutionLogNeeded;

	/**
	 * The user associated with the ticket.
	 */
	private String user;

	/**
	 * The password for the user.
	 */
	private String password;
	
	/**
	 * The category of the ticket.
	 */
	private String category;
}