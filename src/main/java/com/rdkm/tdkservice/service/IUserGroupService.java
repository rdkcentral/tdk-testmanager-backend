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

import java.util.List;
import java.util.UUID;

import com.rdkm.tdkservice.dto.UserGroupDTO;

public interface IUserGroupService {

	/**
	 * Creates a new user group based on the provided user group name.
	 *
	 * @param userGroupName The name of the user group to be created.
	 * @return A boolean value indicating whether the user group was created
	 *         successfully.
	 */
	boolean createUserGroup(String userGroupName);

	/**
	 * Retrieves all user groups from the database.
	 *
	 * @return A list of all user groups.
	 */
	List<UserGroupDTO> findAll();

	/**
	 * Retrieves a user group by id.
	 *
	 * @param id The id of the user group to retrieve.
	 * @return The user group with the provided id.
	 */
	void deleteById(UUID id);

	/**
	 * Updates the user group with the provided id based on the provided user group
	 * request.
	 *
	 * @param userGroupRequest The request object containing the updated details of
	 *                         the user group.
	 * @param id               The id of the user group to be updated.
	 * @return The updated user group.
	 */
	UserGroupDTO updateUserGroup(UserGroupDTO userGroupRequest);

}
