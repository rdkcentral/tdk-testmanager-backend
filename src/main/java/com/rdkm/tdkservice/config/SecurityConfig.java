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
package com.rdkm.tdkservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.rdkm.tdkservice.serviceimpl.UserService;

/**
 * This class is used to configure the security for the application using Spring
 * Security and JWT token.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	UserService userService;

	@Autowired
	JWTAuthFilter jwtAuthFilter;

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);

	/**
	 * This method is used to configure the security for the application using
	 * Spring Security and JWT token. This method will filter the apis.
	 * 
	 * @param httpSecurity - HttpSecurity
	 * @return SecurityFilterChain
	 * @throws Exception
	 */
	@Bean
	SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
		try {

			httpSecurity.csrf(AbstractHttpConfigurer::disable).cors(Customizer.withDefaults())
					.authorizeHttpRequests(request -> request
							// Allowing the access to the below paths without authentication this include
							// the login and signup apis, the actuator apis and those apis that are being
							// called from the python framework or scripts.
							// TODO: Add changes to the python framework and then change these paths to
							// proper REST API paths
							.requestMatchers("/api/v1/auth/**", "/actuator/**", "/fileStore/**", "/execution/**",
									"/deviceGroup/**", "/primitiveTest/**")
							.permitAll()
							// Authorization based access control framework is added.
							// Currently, the authorization based on roles is handled in the frontend.
							// In the future, backend role-based access can be enabled by uncommenting the
							// line below
							// and adding the APIs to be accessed by admin or other roles as needed.
							// .requestMatchers("/api/v1/users/**").hasAuthority("admin")
							.requestMatchers(SWAGGER_UI).permitAll().anyRequest().authenticated())
					.sessionManagement(manager -> manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
					.authenticationProvider(authenticationProvider())
					.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
		} catch (Exception e) {
			LOGGER.error("Error while configuring the security filter chain", e);

		}

		return httpSecurity.build();

	}

	/**
	 * This method is used to configure the swagger ui
	 */
	private static final String[] SWAGGER_UI = { "/swagger-resources/**", "/swagger-ui.html", "/swagger-ui/**",
			"/v3/api-docs/**" };

	/**
	 * This method is used to configure the authentication provider
	 * by injecting the authentication provider bean
	 * 
	 * @return AuthenticationProvider
	 */
	@Bean
	AuthenticationProvider authenticationProvider() {
		// Configure the authentication provider
		DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
		daoAuthenticationProvider.setUserDetailsService(userService);
		daoAuthenticationProvider.setPasswordEncoder(getPasswordEncoder());

		return daoAuthenticationProvider;
	}

	/**
	 * This method is used to configure the password encoder by
	 * injecting the password encoder bean
	 * 
	 * @return PasswordEncoder
	 */
	@Bean
	PasswordEncoder getPasswordEncoder() {
		return new BCryptPasswordEncoder();

	}

	/**
	 * This method is used to configure the authentication manager by
	 * injecting the authentication configuration bean
	 * 
	 * @param authenticationConfiguration
	 * @return AuthenticationManager
	 * @throws Exception
	 */
	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

}
