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
package com.rdkm.tdkservice.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.rdkm.tdkservice.config.JwtConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

/**
 * JWTUtils class
 * 
 */
@Component
public class JWTUtils {

	private SecretKey key;

	private final JwtConfig jwtConfig;

	/**
	 * Constructor for JWTUtils class. Initializes the JWTUtils object with the
	 * provided JwtConfig object. Decodes the secret key from Base64 and initializes
	 * a SecretKey object.
	 *
	 * @param jwtConfig JwtConfig object containing the configuration for the JWT
	 *                  token.
	 */
	public JWTUtils(JwtConfig jwtConfig) {
		this.jwtConfig = jwtConfig;
		byte[] keyBytes = Base64.getDecoder().decode(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
		this.key = new SecretKeySpec(keyBytes, "HmacSHA256");
	}

	/**
	 * This method is used to generate a JWT token for a given user.
	 *
	 * @param userDetails This is a UserDetails object that contains information
	 *                    about the user. UserDetails is an interface provided by
	 *                    Spring Security to represent a user in the system.
	 *
	 * @return String This returns a JWT token as a string. The token is generated
	 *         using the Jwts library. The token includes the username of the user
	 *         as the subject, the current time as the issued at time, and an
	 *         expiration time which is the current time plus the configured
	 *         expiration time. The token is signed with a secret key.
	 *
	 *         The JWT token is a compact, URL-safe means of representing claims to
	 *         be transferred between two parties. The claims in a JWT are encoded
	 *         as a JSON object that is used as the payload of a JSON Web Signature
	 *         (JWS) structure or as the plaintext of a JSON Web Encryption (JWE)
	 *         structure, enabling the claims to be digitally signed or integrity
	 *         protected with a Message Authentication Code (MAC) and/or encrypted.
	 */
	public String generateToken(UserDetails userDetails) {
		return Jwts.builder().subject(userDetails.getUsername()).issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpirationTime())).signWith(key)
				.compact();

	}

	/**
	 * Extracts the username from a given JWT token.
	 *
	 * @param token JWT token as a string.
	 * @return The username as a string.
	 */
	public String extractUsername(String token) {
		return extractClaims(token, Claims::getSubject);
	}

	/**
	 * Extracts claims from a given JWT token.
	 *
	 * @param token           JWT token as a string.
	 * @param claimsTFunction Function object used to extract the claims.
	 * @return The extracted claims.
	 */
	private <T> T extractClaims(String token, Function<Claims, T> claimsTFunction) {
		return claimsTFunction.apply(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
	}

	/**
	 * Validates a given JWT token.
	 *
	 * @param token       JWT token as a string.
	 * @param userDetails UserDetails object containing information about the user.
	 * @return A boolean value indicating whether the token is valid or not.
	 */
	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
	}

	/**
	 * Checks if a given JWT token is expired.
	 *
	 * @param token JWT token as a string.
	 * @return A boolean value indicating whether the token is expired or not.
	 */
	public boolean isTokenExpired(String token) {
		return extractClaims(token, Claims::getExpiration).before(new Date());
	}

}
