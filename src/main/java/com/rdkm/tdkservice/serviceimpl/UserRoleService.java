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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rdkm.tdkservice.dto.UserRoleDTO;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.model.UserRole;
import com.rdkm.tdkservice.repository.UserRoleRepository;
import com.rdkm.tdkservice.service.IUserRoleService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.MapperUtils;

/**
 * This class provides the implementation for the UserRoleService interface. It
 * provides methods to perform CRUD operations on UserRole entities.
 */
@Service
public class UserRoleService implements IUserRoleService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserGroupService.class);

	@Autowired
	private UserRoleRepository userRoleRepository;

	/**
	 * This method is used to create a new UserRole.
	 * 
	 * @param userRole This is the name of the UserRole to be created.
	 * @return boolean This returns true if the UserRole was created successfully,
	 *         false otherwise.
	 */
	@Override
	public boolean createUserRole(String userRole) {
		LOGGER.info("Going to create user role");

		if (userRoleRepository.existsByName(userRole)) {
			LOGGER.info("User already exists with the same username: " + userRole);
			throw new ResourceAlreadyExistsException(Constants.USER_ROLE, userRole);
		}
		UserRole userRoleObject = new UserRole();
		userRoleObject.setName(userRole);
		try {
			userRoleObject = userRoleRepository.save(userRoleObject);
		} catch (Exception e) {
			LOGGER.error("Error occurred while creating UserRole", e);
			return false;
		}

		return userRoleObject != null && userRoleObject.getId() != null;

	}

	/**
	 * This method is used to retrieve all user roles.
	 * 
	 * @return List<UserRoleDTO> This returns a list of all user roles.
	 */
	@Override
	public List<UserRoleDTO> findAll() {
		LOGGER.info("Going to find all user roles");
		List<UserRole> userRoles = userRoleRepository.findAll();
		if (userRoles.isEmpty()) {
			LOGGER.info("No user roles found");
			return null;
		}
		return userRoles.stream().map(MapperUtils::convertToUserRoleDTO).toList();
	}
}
