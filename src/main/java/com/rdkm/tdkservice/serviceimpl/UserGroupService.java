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
package com.rdkm.tdkservice.serviceimpl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.rdkm.tdkservice.dto.UserGroupDTO;
import com.rdkm.tdkservice.exception.DeleteFailedException;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.model.UserGroup;
import com.rdkm.tdkservice.repository.UserGroupRepository;
import com.rdkm.tdkservice.service.IUserGroupService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.MapperUtils;

/**
 * This class provides the implementation for the UserGroupService interface. It
 * provides methods to perform CRUD operations on UserGroup entities.
 */
@Service
public class UserGroupService implements IUserGroupService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserGroupService.class);

	@Autowired
	private UserGroupRepository userGroupRepository;

	/**
	 * This method is used to create a new UserGroup.
	 * 
	 * @param userGroupName This is the name of the UserGroup to be created.
	 * @return boolean This returns true if the UserGroup was created successfully,
	 *         false otherwise.
	 */
	@Override
	public boolean createUserGroup(String userGroupName) {
		LOGGER.info("Going to create user group");

		if (userGroupRepository.existsByName(userGroupName)) {
			LOGGER.info("User Group already exists with the same username: " + userGroupName);
			throw new ResourceAlreadyExistsException(Constants.USER_GROUP, userGroupName);
		}

		UserGroup userGroup = new UserGroup();
		userGroup.setName(userGroupName);
		try {
			userGroup = userGroupRepository.save(userGroup);
		} catch (Exception e) {
			LOGGER.error("Error occurred while creating UserGroup", e);
			return false;
		}

		return userGroup != null && userGroup.getId() != null;
	}

	/**
	 * This method is used to retrieve all UserGroups.
	 * 
	 * @return List<UserGroupDTO> This returns a list of all UserGroups.
	 */
	@Override
	public List<UserGroupDTO> findAll() {
		LOGGER.info("Going to fetch all user groups");
		List<UserGroup> userslist = userGroupRepository.findAll();
		if (userslist.isEmpty()) {
			return null;
		}
		return userslist.stream().map(MapperUtils::convertToUserGroupDTO).collect(Collectors.toList());
	}

	/**
	 * This method is used to delete a UserGroup by its id.
	 */
	@Override
	public void deleteById(UUID id) {
		LOGGER.info("Going to delete user group with id: " + id);
		if (!userGroupRepository.existsById(id)) {
			LOGGER.info("No UserGroup found with id: " + id);
			throw new ResourceNotFoundException(Constants.USER_GROUP_ID, id.toString());
		}
		try {
			userGroupRepository.deleteById(id);
		} catch (DataIntegrityViolationException e) {
			LOGGER.error("Error occurred while deleting UserGroup with id: " + id, e);
			throw new DeleteFailedException();

		}
	}

	/**
	 * This method is used to update a UserGroup's details.
	 * 
	 * @param userGroupRequest The DTO containing the updated details of the
	 *                         UserGroup.
	 */

	@Override
	public UserGroupDTO updateUserGroup(UserGroupDTO userGroupRequest) {
		LOGGER.info("Going to update user group with id: " + userGroupRequest.getUserGroupId().toString());
		UserGroup userGroup = userGroupRepository.findById(userGroupRequest.getUserGroupId())
				.orElseThrow(() -> new ResourceNotFoundException(Constants.USER_GROUP_ID,
						userGroupRequest.getUserGroupId().toString()));
		if (userGroupRepository.existsByName(userGroupRequest.getUserGroupName())) {
			LOGGER.info("UserGroup already exists with the same name: " + userGroupRequest.getUserGroupName());
			throw new ResourceAlreadyExistsException(Constants.USER_GROUP, userGroupRequest.getUserGroupName());
		} else {
			userGroup.setName(userGroupRequest.getUserGroupName());
		}
		try {
			userGroup = userGroupRepository.save(userGroup);
		} catch (Exception e) {
			LOGGER.error("Error occurred while updating UserGroup with id: " + userGroupRequest.getUserGroupId(), e);
		}
		return MapperUtils.convertToUserGroupDTO(userGroup);

	}

}
