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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * The ChangePasswordRequestDTO class is used to map the request body of the
 * change password request.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangePasswordRequestDTO {

	/**
	 * Represents the username of the user. This field is mandatory, hence it cannot
	 * be blank.
	 */
	@NotBlank(message = "Username is required")
	private String userName;

	/**
	 * Represents the old password of the user. This field is mandatory, hence it
	 * cannot be blank.
	 */
	@NotBlank(message = "Old Password is required")
	private String oldPassword;

	/**
	 * Represents the new password of the user. This field is mandatory, hence it
	 * cannot be blank.
	 */
	@NotBlank(message = "New Password is required")
	@Size(min = 6, message = "Password must be at least 6 characters long")
	private String newPassword;

}
