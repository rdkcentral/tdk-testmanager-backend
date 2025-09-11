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
package com.rdkm.tdkservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.rdkm.tdkservice.model.User;

/**
 * The UserRepository interface provides methods for user operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

	/**
	 * This method is used to find a user by username.
	 *
	 * @param username the username of the user to find
	 * @return a User object containing the user's information
	 */
	User findByUsername(String username);

	/**
	 * This method is used to check if a user exists by username.
	 *
	 * @param username the username of the user to check
	 * @return a boolean value indicating whether the user exists
	 */
	boolean existsByUsername(String username);

	/**
	 * This method is used to change the password of a user.
	 *
	 * @param password the new password
	 * @param username the username of the user
	 */
	@Transactional
	@Modifying
	@Query(value = "UPDATE user set password = :password where username = :username", nativeQuery = true)
	void changeUserPassword(@Param("password") String password, @Param("username") String username);

	/**
	 * This method is used to check if a user exists by email.
	 *
	 * @param email the email of the user to check
	 * @return a boolean value indicating whether the user exists
	 */
	boolean existsByEmail(String email);

	/*
	 * This method is used to get the user by usermail
	 * 
	 * @param user email
	 * 
	 * @return User
	 */
	User findByEmail(String userEmail);

	/**
	 * This method is used to find all users by their status.
	 *
	 * @param status the status of the users to find *
	 * @return a list of User objects containing the users' information
	 */
	List<User> findByStatus(String status);

}
