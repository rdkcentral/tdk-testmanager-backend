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
package com.rdkm.tdkservice.service;

import com.rdkm.tdkservice.dto.SigninRequestDTO;
import com.rdkm.tdkservice.dto.SigninResponseDTO;
import com.rdkm.tdkservice.dto.UserCreateDTO;

/**
 * The ILoginService interface provides methods for user authentication and
 * registration.
 */
public interface ILoginService {
	/**
	 * This method is used to sign in a user. It receives a SigninRequestDTO object
	 * that contains the username and password of the user. The method validates the
	 * user credentials and returns a SigninResponseDTO object that contains the
	 * user's information and a JWT token.
	 *
	 * @param signinRequest A SigninRequestDTO object that contains the username and
	 *                      password of the user.
	 * @return A SigninResponseDTO object that contains the user's information and a
	 *         JWT token.
	 */
	SigninResponseDTO signIn(SigninRequestDTO signinRequest);

	/**
	 * This method is used to register a new user. It receives a UserDTO object that
	 * contains the information of the user to be registered. The method saves the
	 * user data in the database and returns a boolean value indicating the success
	 * of the operation.
	 *
	 * @param registerRequest A UserDTO object that contains the information of the
	 *                        user to be registered.
	 * @return A boolean value indicating the success of the operation.
	 */
	boolean register(UserCreateDTO registerRequest);

	/**
	 * This method is used to change the category preference of a user. It receives
	 * the username of the user and the category to be changed. The method changes
	 * the category preference of the user and returns a boolean value indicating
	 * the success of the operation.
	 *
	 * @param userName the username of the user
	 * @param category the category to be changed
	 * @return A boolean value indicating the success of the operation.
	 */
	boolean changeCategoryPreference(String userName, String category);

}
