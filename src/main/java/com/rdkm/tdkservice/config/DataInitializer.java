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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.rdkm.tdkservice.model.User;
import com.rdkm.tdkservice.repository.UserRepository;
import com.rdkm.tdkservice.util.Constants;

/**
 * This class is used to initialize the data in the database. It will read the
 * data.sql file and execute the SQL queries in it. It will also extract the
 * password from the file and encode it using the password encoder.
 */
@Component
public class DataInitializer {
	@Autowired
	private ApplicationContext appContext;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	UserRepository userRepository;

	private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);

	/**
	 * This method will be executed after the application context is loaded. It will
	 * read the data.sql file and execute the SQL queries in it. It will also
	 * extract the password from the file and encode it using the password encoder.
	 * 
	 * @throws SQLException
	 */
	@Bean
	void initData() throws SQLException {
		Resource resource = appContext.getResource(Constants.DB_FILE_NAME);
		Connection connection = dataSource.getConnection();
		ScriptUtils.executeSqlScript(connection, resource);

		String password = extractPasswordFromSQLFile();
		if (password != null) {
			String hashedPassword = passwordEncoder.encode(password);
			User user = userRepository.findByUsername("admin");
			if (user != null) {
				user.setPassword(hashedPassword);
				userRepository.save(user);
			}
		} else {
			LOGGER.info("Password not found in data.sql file");
		}
	}

	/**
	 * This method will extract the password from the data.sql file.
	 * 
	 * @return the password extracted from the file
	 */
	private String extractPasswordFromSQLFile() {
		Resource resource = appContext.getResource(Constants.DB_FILE_NAME);
		try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
			String line;
			while ((line = br.readLine()) != null) {
				Pattern pattern = Pattern.compile("VALUES \\('.+?', '.+?', '(.+?)',");
				Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					return matcher.group(1);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error extracting password from data.sql file", e);
		}
		return null;
	}
}