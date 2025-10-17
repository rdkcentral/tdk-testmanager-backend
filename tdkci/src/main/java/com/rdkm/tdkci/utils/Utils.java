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
package com.rdkm.tdkci.utils;

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
}
