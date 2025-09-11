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

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * The UserUpdateDTO class is used to map the request body of the user update
 * request.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserUpdateDTO {

	/**
	 * Represents the unique identifier for the User. This field is mandatory, hence
	 * it cannot be null.
	 */
	@NotNull(message = "User id is required")
	private UUID userId;

	/**
	 * Represents the username of the User.
	 */
	private String userName;

	/**
	 * Represents the password of the User.
	 */
	private String password;

	/**
	 * Represents the email of the User.
	 */
	private String userEmail;

	/**
	 * Represents the display name of the User.
	 */

	private String userDisplayName;

	/**
	 * Represents the theme name of the User.
	 */

	private String userThemeName;

	/**
	 * Represents the user group of the User.
	 */

	private String userGroupName;

	/**
	 * Represents the user category.
	 */
	private String userCategory;

	/**
	 * Represents the user role of the User.
	 */
	private String userRoleName;

	/**
	 * Represents the user status of the User.
	 */
	private String userStatus;

}
