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

import lombok.Data;

/**
 * The SigninResponseDTO class is used to map the response body of the sign-in
 * request.
 */
@Data
public class SigninResponseDTO {

	/**
	 * Represents the token generated for the user.
	 */
	private String token;
	/**
	 * Represents the expiration time of the token.
	 */
	private long expirationTime;
	/**
	 * Represents the user id.
	 */
	private UUID userID;
	/**
	 * Represents the user name.
	 */
	private String userName;
	/**
	 * Represents the user role name.
	 */
	private String userRoleName;
	/**
	 * Represents the user email.
	 */
	private String userEmail;
	/**
	 * Represents the theme name.
	 */
	private String themeName;
	/**
	 * Represents the display name.
	 */
	private String displayName;

	/**
	 * Represents the user category.
	 */
	private String userCategory;
	/**
	 * Represents the user group name.
	 */
	private String userGroupName;

}
