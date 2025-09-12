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

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * The UserDTO class is used to map the request body of the user request.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserCreateDTO {

	/**
	 * Represents the username of the User. This field is mandatory, hence it cannot
	 * be blank.
	 */
	@NotBlank(message = "Username is required")
	private String userName;
	/**
	 * Represents the password of the User. This field is mandatory, hence it cannot
	 * be blank.
	 */
	@NotBlank(message = "Password is required")
	@Size(min = 6, message = "Password must be at least 6 characters long")
	private String password;

	/**
	 * Represents the email of the User. The email should be valid.
	 */
	@Email(message = "Email should be valid")
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
	 * Represents the user category.
	 */
	@NotBlank(message = "Category is required")
	private String userCategory;

	/**
	 * Represents the user group of the User.
	 */
	private String userGroupName;

	/**
	 * Represents the role of the User.
	 */
	private String userRoleName;

	/**
	 * Represents the status of the User.
	 */
	private String userStatus;

}
