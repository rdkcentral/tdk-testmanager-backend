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

import lombok.Data;

/**
 * Data Transfer Object for Ticket Create Response.
 * This class is used to encapsulate the response data when a ticket is created.
 * 
 * @author 
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TicketCreateResponseDTO {

	/**
	 * The status of the ticket creation process.
	 */
	private String status;

	/**
	 * The unique number assigned to the created ticket.
	 */
	private String ticketNumber;

	/**
	 * The link to access the created ticket.
	 */
	private String ticketLink;

}
