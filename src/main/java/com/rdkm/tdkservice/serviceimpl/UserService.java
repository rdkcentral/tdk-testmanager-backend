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

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.controller.LoginController;
import com.rdkm.tdkservice.dto.ChangePasswordRequestDTO;
import com.rdkm.tdkservice.dto.UserCreateDTO;
import com.rdkm.tdkservice.dto.UserDTO;
import com.rdkm.tdkservice.dto.UserUpdateDTO;
import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.enums.Theme;
import com.rdkm.tdkservice.exception.DeleteFailedException;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.exception.UserInputException;
import com.rdkm.tdkservice.model.User;
import com.rdkm.tdkservice.model.UserGroup;
import com.rdkm.tdkservice.model.UserRole;
import com.rdkm.tdkservice.repository.UserGroupRepository;
import com.rdkm.tdkservice.repository.UserRepository;
import com.rdkm.tdkservice.repository.UserRoleRepository;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.MapperUtils;
import com.rdkm.tdkservice.util.Utils;

/**
 * The UserService class is a service class that handles the business logic for
 * user-related operations. It interacts with the UserRepository to perform CRUD
 * operations on the User entity. In addition to that it is implementing
 * the UserDetailsService interface which allows it to load user-specific data
 * used for
 * Spring Security based authentication.
 */
@Service
public class UserService implements UserDetailsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	UserRoleRepository userRoleRepository;

	@Autowired
	@Lazy
	PasswordEncoder passwordEncoder;

	@Autowired
	private UserGroupRepository userGroupRepository;

	@Autowired
	CommonService commonService;

	/**
	 * This method is used to load a user by their username.
	 *
	 * @param username The username of the user to be loaded.
	 * @return UserDetails The UserDetails object representing the user.
	 * @throws UsernameNotFoundException If the user with the provided username does
	 *                                   not exist.
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(username);
		if (user == null) {
			throw new UsernameNotFoundException("User not found: " + username);
		}
		return user;
	}

	/**
	 * This method is used to create a new User object based on the UserRequest
	 * object. It sets the properties of the User object based on the properties of
	 * the UserRequest object. It also sets the user role, theme, and user group
	 * based on the values provided in the UserRequest object. It then saves the
	 * User object in the database and returns the saved User object.
	 *
	 * @param registerRequest - the request object containing the details of the
	 *                        user to be created
	 * @return the saved User object
	 */
	public boolean createUser(UserCreateDTO userRequest) {
		LOGGER.info("Going to create a new user");
		// Check if the user already exists with the same username
		if (userRepository.existsByUsername(userRequest.getUserName())) {
			LOGGER.info("User already exists with the same username: " + userRequest.getUserName());
			throw new ResourceAlreadyExistsException(Constants.USER_NAME, userRequest.getUserName());
		}

		// Check if the user already exists with the same email
		if (userRepository.existsByEmail(userRequest.getUserEmail())) {
			LOGGER.info("User already exists with the same email: " + userRequest.getUserEmail());
			throw new ResourceAlreadyExistsException(Constants.EMAIL, userRequest.getUserEmail());
		}

		User user = new User();
		user.setUsername(userRequest.getUserName());
		if (!Utils.isEmpty(userRequest.getUserEmail())) {
			user.setEmail(userRequest.getUserEmail());
		}

		user.setDisplayName(userRequest.getUserDisplayName());
		user.setPassword(passwordEncoder.encode(userRequest.getPassword()));

		Category category = Category.getCategory(userRequest.getUserCategory());
		if (category != null) {
			user.setCategory(category);
		} else {
			user.setCategory(Category.RDKV);
		}
		// Setting User role, if empty default user role TESTER will be set
		String userRoleName = userRequest.getUserRoleName();
		// Default value for userrole - by default it will be tester
		if (Utils.isEmpty(userRoleName))
			userRoleName = Constants.DEFAULT_USER_ROLE;
		UserRole userRole = userRoleRepository.findByName(userRoleName);
		if (userRole != null) {
			user.setUserRole(userRole);
		} else {
			LOGGER.error("User role not found: " + userRoleName);
			throw new ResourceNotFoundException(Constants.USER_ROLE, userRoleName);
		}

		// Setting User theme, if empty default theme LIGHT is set
		String userThemeName = userRequest.getUserThemeName();
		Theme theme;
		if (Utils.isEmpty(userThemeName)) {
			theme = Theme.getDefaultTheme();
		} else {
			theme = Theme.valueOf(userThemeName.toUpperCase());
		}
		user.setTheme(theme);

		UserGroup userGroup = userGroupRepository.findByName(userRequest.getUserGroupName());
		if (null != userGroup)
			user.setUserGroup(userGroup);

		// Setting user status to pending during the registration
		user.setStatus(Constants.USER_PENDING);

		User savedUser = userRepository.save(user);
		if (savedUser != null && savedUser.getId() != null) {
			LOGGER.info("Creating new user success");
			return true;
		} else {
			LOGGER.error("Creating new user failed");
			return false;
		}
	}

	/**
	 * This method is used to find a user by their ID.
	 *
	 * @param id The ID of the user to be found.
	 * @return UserDTO The DTO representation of the User object.
	 * @throws ResourceNotFoundException If the user with the provided ID does not
	 *                                   exist.
	 */
	public UserDTO findUserById(UUID id) {
		LOGGER.info("Executing findUserById method with id: " + id);

		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.USER_ID, id.toString()));
		return MapperUtils.populateUserDTO(user);
	}

	/**
	 * This method is used to retrieve all users.
	 *
	 * It fetches all the users from the repository, maps each User object to a
	 * UserDTO object, and returns a list of these UserDTO objects. If there are no
	 * users in the repository, it returns null.
	 *
	 * @return List<UserDTO> A list of UserDTO objects representing all the users.
	 *         Returns null if there are no users.
	 */
	public List<UserDTO> getAllUsers() {
		LOGGER.info("Executing getAllUsers method");
		List<User> userslist = userRepository.findAll();
		if (userslist.isEmpty()) {
			return null;
		}
		return userslist.stream().map(MapperUtils::populateUserDTO).collect(Collectors.toList());
	}

	/**
	 * This method is to get all the users who are pending for approval
	 * 
	 * @return List<UserDTO> A list of UserDTO objects representing all the users
	 *         who are pending for approval.
	 */
	public List<UserDTO> getAllPendingUsers() {
		LOGGER.info("Fetching all pending users");
		List<User> pendingUsers = userRepository.findByStatus(Constants.USER_PENDING);
		if (pendingUsers.isEmpty() || pendingUsers.size() == 0) {
			return null;
		}
		return pendingUsers.stream().map(MapperUtils::populateUserDTO).collect(Collectors.toList());
	}

	/**
	 * This method is used to approve the user
	 *
	 * @param username - String
	 * @return boolean - returns true if user is approved successfully
	 */
	public boolean activateUser(String username) {
		LOGGER.info("Activating user: " + username);
		User user = userRepository.findByUsername(username);
		if (user == null) {
			LOGGER.error("User not found: " + username);
			return false;
		}
		user.setStatus(Constants.USER_ACTIVE);
		userRepository.save(user);
		LOGGER.info("User activated successfully: " + username);
		return true;
	}

	/**
	 * This method is used to update a user's details.
	 *
	 * @param updateUserRequest The DTO containing the updated details of the user.
	 * @param id                The ID of the user to be updated.
	 * @return UserDTO The DTO representation of the updated User object.
	 * @throws ResourceNotFoundException If the user with the provided ID does not
	 *                                   exist.
	 */
	public UserDTO updateUser(UserUpdateDTO updateUserRequest) {
		LOGGER.info("Executing updateUser method for the user: " + updateUserRequest.toString());

		// Retrieve the user from the database
		User user = userRepository.findById(updateUserRequest.getUserId()).orElseThrow(
				() -> new ResourceNotFoundException(Constants.USER_ID, updateUserRequest.getUserId().toString()));

		// Validation for mandatory fields are not applied for update request
		// if the update , for user update should happen for specific fields as well.
		// So we are doing the empty check and if it is empty then no need to update
		if (!Utils.isEmpty(updateUserRequest.getUserName())) {
			User newUser = userRepository.findByUsername(updateUserRequest.getUserName());
			if (newUser != null && updateUserRequest.getUserName().equalsIgnoreCase(user.getUsername())) {
				user.setUsername(updateUserRequest.getUserName());
			} else {

				if (userRepository.existsByUsername(updateUserRequest.getUserName())) {
					LOGGER.info("User name already exists with the same name: " + updateUserRequest.getUserName());
					throw new ResourceAlreadyExistsException(Constants.USER_NAME, updateUserRequest.getUserName());
				} else {
					user.setUsername(updateUserRequest.getUserName());
				}
			}
		}

		if (!Utils.isEmpty(updateUserRequest.getPassword())) {
			user.setPassword(passwordEncoder.encode(updateUserRequest.getPassword()));
		}

		if (!Utils.isEmpty(updateUserRequest.getUserEmail())) {
			User newUser = userRepository.findByEmail(updateUserRequest.getUserEmail());
			if (newUser != null && updateUserRequest.getUserEmail().equalsIgnoreCase(user.getEmail())) {
				user.setEmail(updateUserRequest.getUserEmail());
			} else {

				if (userRepository.existsByEmail(updateUserRequest.getUserEmail())) {
					LOGGER.info("Email already exists with the same name: " + updateUserRequest.getUserEmail());
					throw new ResourceAlreadyExistsException(Constants.EMAIL, updateUserRequest.getUserEmail());
				} else {
					user.setEmail(updateUserRequest.getUserEmail());
				}
			}
		}

		if (!Utils.isEmpty(updateUserRequest.getUserDisplayName()))
			user.setDisplayName(updateUserRequest.getUserDisplayName());

		// Find the UserRole, Theme, and UserGroup from their repositories and set them
		// to the user
		if (!Utils.isEmpty(updateUserRequest.getUserRoleName())) {
			UserRole userRole = userRoleRepository.findByName(updateUserRequest.getUserRoleName());
			if (null != userRole)
				user.setUserRole(userRole);
		}

		if (!Utils.isEmpty(updateUserRequest.getUserThemeName())) {
			Theme theme = Theme.valueOf(updateUserRequest.getUserThemeName().toUpperCase());
			user.setTheme(theme);
		}

		if (!Utils.isEmpty(updateUserRequest.getUserCategory())) {
			Category category = Category.getCategory(updateUserRequest.getUserCategory());
			if (category != null) {
				user.setCategory(category);
			}
		}

		if (!Utils.isEmpty(updateUserRequest.getUserGroupName())) {
			UserGroup userGroup = userGroupRepository.findByName(updateUserRequest.getUserGroupName());
			if (null != userGroup)
				user.setUserGroup(userGroup);
		}

		User savedUser = userRepository.save(user);
		if (savedUser == null) {
			LOGGER.error("Updating user failed");
			throw new TDKServiceException("Updating user failed");
		}
		return MapperUtils.populateUserDTO(savedUser);

	}

	/**
	 * This method is used to delete a user by their ID.
	 *
	 * @param id The ID of the user to be deleted.
	 * @throws ResourceNotFoundException If the user with the provided ID does not
	 *                                   exist.
	 */
	public void deleteUser(UUID id) {
		LOGGER.info("Executing deleteUser method with id: " + id);
		// Check if the user exists
		if (!userRepository.existsById(id)) {
			LOGGER.info("No User found with id: " + id);
			throw new ResourceNotFoundException(Constants.USER_ID, id.toString());
		}
		try {
			userRepository.deleteById(id);
		} catch (DataIntegrityViolationException e) {
			LOGGER.error("Error occurred while deleting User with id: " + id, e);
			throw new DeleteFailedException();
		}
		LOGGER.info("User deleted successfully with id: " + id);

	}

	/**
	 * This method is used to change the password of the user
	 * 
	 * @param changePasswordRequest - ChangePasswordRequest
	 * @return boolean - returns true if password is changed successfully
	 */
	public boolean changePassword(ChangePasswordRequestDTO changePasswordRequest) {
		LOGGER.info("The change password request is " + changePasswordRequest.toString());

		if (changePasswordRequest.getNewPassword().trim().equals(changePasswordRequest.getOldPassword().trim())) {
			throw new UserInputException("Old password and New password are same");
		}

		User user = userRepository.findByUsername(changePasswordRequest.getUserName());
		if (null == user) {
			LOGGER.error("User doesnt exists with the username: " + changePasswordRequest.getUserName());
			throw new ResourceNotFoundException(Constants.USER_NAME, changePasswordRequest.getUserName());
		}

		boolean isPasswordMatching = passwordEncoder.matches(changePasswordRequest.getOldPassword(),
				user.getPassword());
		if (isPasswordMatching) {
			userRepository.changeUserPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()),
					user.getUsername());
			return true;
		}
		return false;
	}

	/**
	 * This method is used to set the theme for the user
	 * 
	 * @param userId - UUID
	 * @param theme  - String
	 * @return boolean - returns true if theme is set successfully
	 */
	public boolean setTheme(UUID userId, String theme) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.USER_ID, userId.toString()));
		Theme userTheme = Theme.getTheme(theme);
		if (userTheme != null) {
			user.setTheme(userTheme);
			userRepository.save(user);
			return true;
		} else {
			LOGGER.error("User theme not found for the user id: " + userId);
			throw new ResourceNotFoundException(Constants.USER_THEME, theme);
		}
	}

	/**
	 * This method is used to get the theme for the user
	 * 
	 * @param userId - UUID
	 * @return String - returns theme for the user
	 */
	public String getTheme(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.USER_ID, userId.toString()));
		return user.getTheme().name();
	}

	/**
	 * This method is used to get the app version
	 * 
	 * @return String - returns the app version
	 */
	public String getAppVersion() {
		LOGGER.info("Getting the current app version");

		String tmConfigFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
				+ Constants.TM_CONFIG_FILE;
		String appVersion;
		try {
			appVersion = commonService.getConfigProperty(new File(tmConfigFilePath), Constants.APP_VERSION);
		} catch (Exception e) {
			LOGGER.error("Error occurred while getting the app version from the configuration file", e);
			throw new TDKServiceException("Failed to get app version" + e.getMessage());
		}

		if (appVersion == null) {
			LOGGER.error("App version is null");
			throw new UserInputException("App version not configured");
		}

		return appVersion;
	}

}