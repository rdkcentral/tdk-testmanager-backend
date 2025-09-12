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
package com.rdkm.tdkservice.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rdkm.tdkservice.dto.SigninRequestDTO;
import com.rdkm.tdkservice.dto.SigninResponseDTO;
import com.rdkm.tdkservice.dto.UserCreateDTO;
import com.rdkm.tdkservice.dto.UserGroupDTO;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.response.Response;
import com.rdkm.tdkservice.service.ILoginService;
import com.rdkm.tdkservice.service.IUserGroupService;
import com.rdkm.tdkservice.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/**
 * The LoginController class is a REST controller that handles login and
 * registration requests. It provides endpoints for user registration, sign in,
 * and password change. This class uses the ILoginService to perform the actual
 * business logic.
 *
 * Endpoints: POST /tdk/signup: Register a new user. POST /tdk/signin: Sign in a
 * user. POST /tdk/changepassword: Change the password of a user.
 * 
 */
@Validated
@RestController
@CrossOrigin
@RequestMapping("/api/v1/auth/")
public class LoginController {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

	@Autowired
	private ILoginService loginService;

	@Autowired
	private IUserGroupService userGroupService;

	/**
	 * This method is used to register a new user. It receives a POST request at the
	 * "/signup" endpoint with a UserDTO object in the request body. The UserDTO
	 * object should contain the necessary information for creating a new user.
	 * 
	 * @param registerRequest A UserDTO object that contains the information of the
	 *                        user to be registered.
	 * @return ResponseEntity<String> If the user is successfully created, it
	 *         returns a 201 status code with a success message. If there is an
	 *         error in saving the user data, it returns a 500 status code with an
	 *         error message.
	 * @throws Exception
	 */
	@Operation(summary = "Register a new user", description = "Registers a new user in the system.")
	@ApiResponse(responseCode = "201", description = "User created successfully")
	@ApiResponse(responseCode = "500", description = "Error in registering user data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "409", description = "Conflict , unique fields already existing")
	@PostMapping("/signup")
	public ResponseEntity<Response> signUp(@RequestBody @Valid UserCreateDTO registerRequest) {
		LOGGER.info("Received signup request: " + registerRequest.toString());
		boolean isUserCreated = loginService.register(registerRequest);
		if (isUserCreated) {
			LOGGER.info("User creation is successfull");
			return ResponseUtils.getCreatedResponse("User created successfully");
		} else {
			LOGGER.error("Error in registering user data");
			throw new TDKServiceException("Error in registering user data");
		}

	}

	/**
	 * This method is used to sign in a user. It receives a POST request at the
	 * "/signin" endpoint with a SigninRequestDTO object in the request body. The
	 * SigninRequestDTO object should contain the necessary information for signing
	 * in a user.
	 *
	 * @param signinRequest A SigninRequestDTO object that contains the information
	 *                      of the user to be signed in.
	 * @return ResponseEntity<SigninResponseDTO> If the user is successfully signed
	 *         in, it returns a 200 status code with a SigninResponseDTO object. The
	 *         SigninResponseDTO object contains the details of the signed in user.
	 *         If there is an error in signing in the user, it returns an
	 *         appropriate error status code with an error message.
	 * @throws Exception If any exception occurs during the execution of the method,
	 *                   it is thrown to the caller to handle.
	 */
	@Operation(summary = "Sign in a user", description = "Signs in a user in the system.")
	@ApiResponse(responseCode = "200", description = "User signed in successfully")
	@ApiResponse(responseCode = "500", description = "Error in signing in user")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@ApiResponse(responseCode = "401", description = "Unauthorized")
	@ApiResponse(responseCode = "403", description = "Forbidden")
	@ApiResponse(responseCode = "404", description = "User not found")
	@PostMapping("/signin")
	public ResponseEntity<DataResponse> signIn(@RequestBody @Valid SigninRequestDTO signinRequest) {
		LOGGER.info("Received sign request: " + signinRequest.toString());
		SigninResponseDTO signinResponseDTO = loginService.signIn(signinRequest);
		LOGGER.info("Finished signin request, response id: " + signinResponseDTO.toString());
		ResponseEntity<DataResponse> dataResponse = ResponseUtils.getSuccessDataResponse("Signin is successful",
				signinResponseDTO);
		return dataResponse;
	}

	/**
	 * This method is mapped to the "/getList" endpoint and is responsible for
	 * retrieving all user groups. It uses the findAll method of the
	 * userGroupService to get a list of all UserGroupDTO objects. Then, it uses a
	 * stream to map each UserGroupDTO to its name and collects these names into a
	 * list. Finally, it returns a ResponseEntity containing this list of user group
	 * names. TODO : Not valid now, UserGroup is not planned for initial release
	 * 
	 * 
	 * @return ResponseEntity containing a list of user group names
	 */
	@Operation(summary = "Get list of user groups", description = "Retrieves a list of all user groups in the system.")
	@ApiResponse(responseCode = "200", description = "User groups retrieved successfully")
	@ApiResponse(responseCode = "404", description = "No user groups found")
	@GetMapping("/getList")
	public ResponseEntity<DataResponse> getListOfUserGroups() {
		List<UserGroupDTO> userGroups = userGroupService.findAll();
		LOGGER.info("Received user groups: " + userGroups.toString());
		List<String> userGroupNames = userGroups.stream().map(UserGroupDTO::getUserGroupName)
				.collect(Collectors.toList());
		LOGGER.info("User groups found: " + userGroupNames);
		return ResponseUtils.getSuccessDataResponse(userGroupNames);
	}

	/**
	 * This method is used to change the category preference of a user. It receives
	 * a POST request at the "/changecategorypreference" endpoint with the username
	 * and category in the request parameters. The username is the username of the
	 * user whose category preference is to be changed. The category is the new
	 * category preference of the user.
	 *
	 * @param userName The username of the user whose category preference is to be
	 *                 changed.
	 * @param category The new category preference of the user.
	 * @return ResponseEntity<String> If the category preference is successfully
	 *         changed, it returns a 200 status code with a success message. If
	 *         there is an error in changing the category preference, it returns a
	 *         400 status code with an error message.
	 * @throws Exception If any exception occurs during the execution of the method,
	 *                   it is thrown to the caller to handle.
	 */
	@Operation(summary = "API to change the category preference", description = "This API is used to change the category preference of a user")
	@ApiResponse(responseCode = "200", description = "Successfully updated the category preference")
	@ApiResponse(responseCode = "500", description = "Internal Server Error")
	@ApiResponse(responseCode = "400", description = "Bad Request,Category preference change failed")
	@ApiResponse(responseCode = "404", description = "User name not found")
	@PostMapping("/changecategorypreference")
	public ResponseEntity<Response> changeCategoryPreference(@RequestParam String userName,
			@RequestParam String category) {
		LOGGER.info("The change category preference request is " + userName + " " + category);
		boolean isCategoryChanged = loginService.changeCategoryPreference(userName, category);
		if (isCategoryChanged) {
			LOGGER.info("Category preference change is successful");
			return ResponseUtils.getSuccessResponse("Successfully updated the category preference");
		} else {
			LOGGER.error("Category preference change failed");
			throw new TDKServiceException("Category preference change failed");
		}
	}
}
