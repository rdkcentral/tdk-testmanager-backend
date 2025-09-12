package com.rdkm.tdkservice.serviceimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.rdkm.tdkservice.config.JwtConfig;
import com.rdkm.tdkservice.dto.SigninRequestDTO;
import com.rdkm.tdkservice.dto.SigninResponseDTO;
import com.rdkm.tdkservice.dto.UserCreateDTO;
import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.exception.UserInputException;
import com.rdkm.tdkservice.model.User;
import com.rdkm.tdkservice.repository.UserRepository;
import com.rdkm.tdkservice.service.ILoginService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.JWTUtils;

/**
 * The LoginService class is responsible for handling user authentication and
 * registration. It provides methods for user registration and sign in. This
 * class uses the UserRepository to interact with the database and the JWTUtils
 * to generate JWT tokens.
 */
@Service
public class LoginService implements ILoginService {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoginService.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JWTUtils jwtUtils;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	UserDetailsService userDetailsService;

	@Autowired
	UserService userService;

	@Autowired
	JwtConfig jwtConfig;

	/**
	 * This method is used to register a user.
	 * 
	 * @param register request - RegisterRequest
	 * @return User - returns user object if successfully saved
	 */
	@Override
	public boolean register(UserCreateDTO registerRequest) {
		LOGGER.info("Registed user the user");
		boolean registeredUser = userService.createUser(registerRequest);
		return registeredUser;
	}

	/**
	 * This method is used to authenticate a user. It takes a SigninRequest object
	 * as input which contains the user's username and password. It first loads the
	 * user details based on the username. If the user is not found, it throws a
	 * UsernameNotFoundException. If the user is found, it authenticates the user
	 * using the AuthenticationManager. After successful authentication, it
	 * generates a JWT token for the user and sets it in the SigninResponse. It also
	 * sets the expiration time of the token in the SigninResponse. The method
	 * returns the SigninResponse object which contains the JWT token and its
	 * expiration time.
	 *
	 * @param signinRequest - the request object containing the username and
	 *                      password of the user
	 * @return the SigninResponse object containing the JWT token and its expiration
	 *         time
	 * @throws UsernameNotFoundException if the user is not found
	 */
	@Override
	public SigninResponseDTO signIn(SigninRequestDTO signinRequest) {
		LOGGER.info("Recieved signin request" + signinRequest.toString());
		SigninResponseDTO signinResponse = new SigninResponseDTO();

		// This acts as the validation for the user name , which is
		// done by Spring security itself before proceeding with authentiation
		userDetailsService.loadUserByUsername(signinRequest.getUsername());

		// If the password is wrong, a BadCredentialsException is thrown.
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(signinRequest.getUsername(), signinRequest.getPassword()));

		User user = userRepository.findByUsername(signinRequest.getUsername());

		// TODO: Once the user status is implemented from the front end , uncomment this
		// if (user.getStatus() == Constants.USER_PENDING) {
		// LOGGER.error("User account is pending: " + user.getUsername());
		// throw new UserInputException("User account is pending: " + user.getUsername()
		// + " .Please contact your administrator to approve the request");
		// }

		String jwt = jwtUtils.generateToken(user);
		signinResponse.setToken(jwt);
		signinResponse.setExpirationTime(jwtConfig.getExpirationTime());
		signinResponse.setUserID(user.getId());
		signinResponse.setUserEmail(user.getEmail());
		signinResponse.setUserName(user.getUsername());
		signinResponse.setUserRoleName(user.getUserRole().getName());
		signinResponse.setThemeName(user.getTheme().getName());
		signinResponse.setDisplayName(user.getDisplayName());
		if (user.getUserGroup() != null) {
			signinResponse.setUserGroupName(user.getUserGroup().getName());
		}
		signinResponse.setUserCategory(user.getCategory().name());
		LOGGER.info("Finished signin request" + signinRequest.toString());
		return signinResponse;

	}

	/**
	 * This method is used to change the category preference of the user
	 *
	 * @param userName - String
	 * @param category - String
	 * @return boolean - returns true if category preference is changed successfully
	 */
	public boolean changeCategoryPreference(String userName, String category) {
		LOGGER.info("The change category preference request is " + userName + " " + category);
		User user = userRepository.findByUsername(userName);
		if (null == user) {
			LOGGER.error("User doesnt exists with the username: " + userName);
			throw new ResourceNotFoundException(Constants.USER_NAME, userName);
		}

		Category categoryEnum = null;
		try {
			// Check if the category is valid
			categoryEnum = Category.valueOf(category);
		} catch (IllegalArgumentException e) {
			LOGGER.error("Invalid category: " + category);
			throw new UserInputException("Invalid category: " + category); // Changed to InvalidArgumentException
		}
		user.setCategory(categoryEnum);
		User savedUser = userRepository.save(user);
		if (savedUser != null && savedUser.getId() != null) {
			System.out.println(savedUser.getCategory().toString());
			LOGGER.info("Changing category preference success");
			return true;
		} else {
			LOGGER.error("Changing category preference failed");
			return false;
		}
	}

}
