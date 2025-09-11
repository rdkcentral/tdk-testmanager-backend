package com.rdkm.tdkservice.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rdkm.tdkservice.dto.UserRoleDTO;
import com.rdkm.tdkservice.service.IUserRoleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * This class represents the controller for managing user roles. It handles HTTP
 * requests related to user roles such as creating a new user role and
 * retrieving all user groups.
 */
@RestController
@CrossOrigin
@RequestMapping("/api/v1/userrole")
public class UserRoleController {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserRoleController.class.getName());

	@Autowired
	IUserRoleService userRoleService;

	/**
	 * Creates a new user role.
	 * 
	 * @param userRole the user role to be created
	 * @return a ResponseEntity containing the created user role if successful, or
	 *         an error message if unsuccessful
	 */
	@Operation(summary = "Create a new user role", description = "Creates a new user role in the system.")
	@ApiResponse(responseCode = "201", description = "User role created successfully")
	@ApiResponse(responseCode = "500", description = "Error in saving user role data")
	@ApiResponse(responseCode = "400", description = "Bad request")
	@PostMapping("/create")
	public ResponseEntity<?> createUserRole(@RequestParam String userRole) {

		LOGGER.info("Received request: " + userRole);
		boolean isUserRoleCreated = userRoleService.createUserRole(userRole);
		if (isUserRoleCreated) {
			LOGGER.info("User Role created succesfully");
			return ResponseEntity.status(HttpStatus.CREATED).body("User Role created succesfully");
		} else {
			LOGGER.error("Error in saving user role");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error in saving user role");
		}

	}

	/**
	 * Retrieves all user groups.
	 * 
	 * @return ResponseEntity containing a list of UserRole objects representing the
	 *         user groups.
	 */
	@Operation(summary = "Find all user groups", description = "Retrieves all user groups in the system.")
	@ApiResponse(responseCode = "200", description = "User groups found")
	@ApiResponse(responseCode = "404", description = "No user groups found")

	@GetMapping("/findall")
	public ResponseEntity<?> findAllUserRoles() {
		LOGGER.info("Received request to find all user groups");
		List<UserRoleDTO> userRoles = userRoleService.findAll();
		if (null != userRoles) {
			LOGGER.info("User roles found");
			return ResponseEntity.status(HttpStatus.OK).body(userRoles);
		} else {
			LOGGER.error("No user roles found");
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No user roles found");
		}

	}
}
