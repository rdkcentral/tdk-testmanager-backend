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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * The JwtConfig class is used to configure the JWT.
 */
@Configuration
public class JwtConfig {

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.expirationTime}")
	private long expirationTime;

	/**
	 * This method is used to get the secret key for the JWT.
	 * 
	 * @return String
	 */
	public String getSecret() {
		return secret;
	}

	/**
	 * This method is used to get the expiration time for the JWT.
	 * 
	 * @return long
	 */
	public long getExpirationTime() {
		return expirationTime;
	}
}
