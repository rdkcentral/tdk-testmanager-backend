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

import com.rdkm.tdkservice.dto.UserRoleDTO;

/**
 * IUserRoleService is an interface that contains the methods to be implemented
 * by the UserRoleServiceImpl class The methods in this interface are: 1.
 * createUserRole(String userRole) 2. findAll()
 * 
 * @see IUserRoleService
 * @see UserRoleServiceImpl
 */
public interface IUserRoleService {

	/**
	 * This method is used to create a new UserRole.
	 * 
	 * @param userRole This is the request object containing the details of the
	 *                 UserRole to be created.
	 * @return boolean This returns true if the UserRole was created successfully,
	 *         false otherwise.
	 */
	boolean createUserRole(String userRole);

	/**
	 * This method is used to retrieve all UserRoles.
	 * 
	 * @return List<UserRoleDTO> This returns a list of all UserRoles.
	 */
	List<UserRoleDTO> findAll();

}
