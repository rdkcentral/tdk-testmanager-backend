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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.rdkm.tdkservice.serviceimpl.UserService;
import com.rdkm.tdkservice.util.JWTUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This class is used to filter the request and validate the JWT token
 */
@Component
public class JWTAuthFilter extends OncePerRequestFilter {

	@Autowired
	private JWTUtils jwtUtils;

	@Autowired
	private UserService userDetailsService;

	private static final Logger LOGGER = LoggerFactory.getLogger(JWTAuthFilter.class);

	/**
	 * This method is used to filter the request and validate the JWT token
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			final String authHeader = request.getHeader("Authorization");
			final String jwtToken;
			final String userName;
			if (authHeader == null || authHeader.isBlank()) {
				LOGGER.debug("Auth header" + authHeader);
				filterChain.doFilter(request, response);
				return;
			}

			jwtToken = authHeader.substring(7);
			LOGGER.debug("JWT token" + authHeader);
			userName = jwtUtils.extractUsername(jwtToken);
			if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {

				UserDetails userDetails = userDetailsService.loadUserByUsername(userName);
				if (jwtUtils.isTokenValid(jwtToken, userDetails)) {
					SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

					UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userDetails,
							null, userDetails.getAuthorities());
					LOGGER.debug("Token is valid ");
					LOGGER.debug(" User authorities are" + userDetails.getAuthorities());
					token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					securityContext.setAuthentication(token);
					SecurityContextHolder.setContext(securityContext);
				}

			}
			filterChain.doFilter(request, response);

		} catch (Exception exception) {
			LOGGER.error("An error occurred while processing the JWT token", exception);
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");

		}
	}

}
