/*
* If not stated otherwise in this file or this component's LICENSE file the
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
package com.rdkm.tdkci.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rdkm.tdkci.enums.Category;
import com.rdkm.tdkci.exception.ResourceNotFoundException;

public class Utils {

	private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

	/**
	 * This method is used to check if the string is empty or not
	 * 
	 * @param string
	 * @return boolean
	 */
	public static boolean isEmpty(final String string) {
		// Null-safe, short-circuit evaluation.
		return string == null || string.trim().isEmpty();
	}

	/**
	 * This method is used to check if the string is not empty
	 * 
	 * @param string
	 * @return boolean
	 */
	public static void checkCategoryValid(String category) {
		if (Category.getCategory(category) == null) {
			LOGGER.error("Invalid category: " + category);
			throw new ResourceNotFoundException(Constants.CATEGORY, category);
		}
	}

	/**
	 * Method to load properties from a file
	 * 
	 * @param filePath The path to the configuration file
	 * @return The loaded properties, or null if the file does not exist or an error
	 *         occurs
	 */
	private static  Properties loadPropertiesFromFile(String filePath) {
		LOGGER.debug("Loading properties from file: {}", filePath);
		File configFile = new File(filePath);
		if (!configFile.exists() || !Files.exists(configFile.toPath())) {
			LOGGER.error("No Config File !!! ");
			return null;
		}
		try (InputStream is = new FileInputStream(configFile)) {
			Properties prop = new Properties();
			prop.load(is);
			return prop;
		} catch (IOException e) {
			LOGGER.error("Error reading config file: {}", configFile, e);
		}
		return null;
	}

	/**
	 * Method to get the configuration property from the specified file
	 *
	 * @param configFile The configuration file
	 * @param key        The key to search for in the configuration file
	 * @return The value of the configuration property
	 */
	public static String getConfigProperty(File configFile, String key) {
		Properties prop = loadPropertiesFromFile(configFile.getPath());
		if (prop != null) {
			LOGGER.debug(" properties key for getting the property from config file" + prop.getProperty(key));
			return prop.getProperty(key);
		}
		return null;
	}
}
