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
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rdkm.tdkservice.dto.UserGroupDTO;
import com.rdkm.tdkservice.service.IUserGroupService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * The UserGroupController class is responsible for handling HTTP requests
 * related to user groups. It provides endpoints for creating, updating,
 * finding, and deleting user groups.
 */
@RestController
@CrossOrigin
@RequestMapping("/api/v1/usergroup/")
public class UserGroupController {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class.getName());

	@Autowired
	IUserGroupService userGroupService;

	/**
	 * Creates a new user group with the given user group name.
	 * 
	 * @param userGroupName the name of the user group to create
	 * @return a ResponseEntity containing the created user group if successful, or
	 *         an error message if unsuccessful
	 */
	@Operation(summary = "Create a new user group", description = "Creates a new user group in the system.")
	@ApiResponse(responseCode = "201", description = "User group created successfully")
	@ApiResponse(responseCode = "500", description = "Error in saving user group data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@PostMapping("/create")
	public ResponseEntity<String> createUserGroup(@RequestParam String userGroupName) {
		LOGGER.info("Received create user group request: " + userGroupName);
		boolean isUserGroupCreated = userGroupService.createUserGroup(userGroupName);
		if (isUserGroupCreated) {
			LOGGER.info("User Group created succesfully");
			return ResponseEntity.status(HttpStatus.CREATED).body("User Group created succesfully");
		} else {
			LOGGER.error("Error in saving user group");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error in saving user group");
		}

	}

	/**
	 * Retrieves all user groups. *
	 * 
	 * @return ResponseEntity containing a list of UserGroup objects
	 */
	@Operation(summary = "Find all user groups", description = "Retrieves all user groups in the system.")
	@ApiResponse(responseCode = "200", description = "User groups found")
	@ApiResponse(responseCode = "404", description = "No user groups found")
	@GetMapping("/findall")
	public ResponseEntity<?> findAllUserGroups() {
		LOGGER.info("Received request to find all user groups");
		List<UserGroupDTO> userGroups = userGroupService.findAll();
		if (null != userGroups) {
			LOGGER.info("User groups found");
			return ResponseEntity.status(HttpStatus.OK).body(userGroups);
		} else {
			LOGGER.error("No user groups found");
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No user groups found");
		}
	}

	/**
	 * This method is mapped to the "/getList" endpoint and is responsible for
	 * retrieving all user groups. It uses the findAll method of the
	 * userGroupService to get a list of all UserGroupDTO objects. Then, it uses a
	 * stream to map each UserGroupDTO to its name and collects these names into a
	 * list. Finally, it returns a ResponseEntity containing this list of user group
	 * names.
	 *
	 * @return ResponseEntity containing a list of user group names
	 */
	@Operation(summary = "Get list of user groups", description = "Retrieves a list of all user groups in the system.")
	@ApiResponse(responseCode = "200", description = "User groups retrieved successfully")
	@ApiResponse(responseCode = "404", description = "No user groups found")
	@GetMapping("/getList")
	public ResponseEntity<List<String>> getListOfUserGroups() {
		LOGGER.info("Received request to get list of user groups");
		List<UserGroupDTO> userGroups = userGroupService.findAll();
		List<String> userGroupNames = userGroups.stream().map(UserGroupDTO::getUserGroupName)
				.collect(Collectors.toList());
		LOGGER.info("User groups found: " + userGroupNames);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(userGroupNames);
	}

	/**
	 * Deletes a user group with the specified ID.
	 *
	 * @param id the ID of the user group to delete
	 * @return a ResponseEntity with the result of the deletion operation
	 */
	@Operation(summary = "Delete a user group", description = "Deletes a user group from the system.")
	@ApiResponse(responseCode = "200", description = "User group deleted successfully")
	@ApiResponse(responseCode = "404", description = "User group not found")
	@DeleteMapping("/delete/{id}")
	public ResponseEntity<String> deleteUserGroup(@PathVariable UUID id) {
		LOGGER.info("Received delete user group request for ID " + id);
		userGroupService.deleteById(id);
		return ResponseEntity.status(HttpStatus.OK).body("Succesfully deleted the userGroup");

	}

	/**
	 * This method is used to update a user group. It receives a PUT request at the
	 * "/update/{id}" endpoint with a UserGroupDTO object in the request body. The
	 * UserGroupDTO object should contain the updated information for the user group
	 * to be updated.
	 *
	 * @param id               The ID of the user group to update.
	 * @param userGroupRequest A UserGroupDTO object that contains the updated
	 *                         information for the user group.
	 * @return A ResponseEntity containing the updated user group if successful, or
	 *         an error message if unsuccessful.
	 */
	@Operation(summary = "Update a user group", description = "Updates a user group in the system.")
	@ApiResponse(responseCode = "200", description = "User group updated successfully")
	@ApiResponse(responseCode = "404", description = "User group not found")
	@ApiResponse(responseCode = "500", description = "Internal Server Error")
	@ApiResponse(responseCode = "400", description = "Bad Request")
	@PutMapping("/update")
	public ResponseEntity<?> updateUserGroup(@RequestBody UserGroupDTO userGroupRequest) {
		LOGGER.info("Executing updateUserGroup method with request: " + userGroupRequest.toString());
		UserGroupDTO updatedUserGroup = userGroupService.updateUserGroup(userGroupRequest);
		if (null != updatedUserGroup) {
			LOGGER.info("User group updated successfully");
			return ResponseEntity.status(HttpStatus.OK).body(updatedUserGroup);
		} else {
			LOGGER.error("Error in updating user group");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error in updating user group data");
		}
	}

}
