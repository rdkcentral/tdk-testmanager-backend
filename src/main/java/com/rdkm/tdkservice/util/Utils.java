
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.exception.UserInputException;

/**
 * This class is used to store the utility methods used in the application
 */
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
	 * This method is to convert list of strings to comma separated string
	 * 
	 * @param list - list of strings
	 * @return - comma separated string
	 */
	public static String convertListToCommaSeparatedString(List<String> list) {
		LOGGER.info("Inside convertListToCommaSeparatedString method with list");
		if (list == null || list.isEmpty()) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (String s : list) {
			sb.append(s);
			if (list.indexOf(s) == list.size() - 1)
				break;
			sb.append(",");
		}
		LOGGER.info("Converted string is" + sb.toString());
		return sb.toString();
	}

	/**
	 * Helper method to validate a string is an integer.
	 * 
	 * @param value     The string value to validate.
	 * @param fieldName The name of the field being validated.
	 */
	public static void validateInteger(String value, String field) {
		try {
			Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new UserInputException(field + " value need to be integer ");
		}
	}

	/**
	 * Converts a given LocalDateTime to a UTC formatted string.
	 *
	 * @param executionDate the LocalDateTime to be converted
	 * @return the UTC formatted string representation of the given LocalDateTime,
	 *         or null if an error occurs during conversion
	 */
	public static String convertToUTCString(LocalDateTime executionDate) {
		// Define the date-time formatter for UTC string
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		try {
			// Convert LocalDateTime to UTC string
			String utcDate = executionDate.toInstant(ZoneOffset.UTC).atOffset(ZoneOffset.UTC).format(formatter);
			return utcDate;
		} catch (Exception e) {
			// Log the error if any exception occurs
			LOGGER.error("Error converting LocalDateTime to UTC string", e);
			return null;
		}
	}

	/**
	 * Converts the given Instant to an Instant without milliseconds.
	 * 
	 * @param time the Instant to be converted, may be null
	 * @return the Instant truncated to seconds, or null if the input is null
	 */
	public static Instant convertInstantToWithoutMilliseconds(Instant time) {
		if (time == null) {
			return null;
		}
		return time.truncatedTo(ChronoUnit.SECONDS);

	}

	/**
	 * Generates a formatted date and time stamp based on the current date and time.
	 * The format used is "MMddyyyyHHmmss".
	 *
	 * @return A string representing the current date and time in the format
	 *         "MMddyyyyHHmmss".
	 */
	public static String getFormatedDateStamp() {
		LocalDateTime now = LocalDateTime.now();

		// Define the desired format
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMddyyyyHHmmss");

		// Format the current date and time
		String formattedDateTime = now.format(formatter);

		// Print the formatted date and time
		return formattedDateTime;
	}

	/**
	 * Generates a timestamp in UTC formatted as MMDDYYHHMMSS.
	 *
	 * @return A string representing the current date and time in UTC in the format
	 *         MMDDYYHHMMSS.
	 */
	public static String getTimeStampInUTCForExecutionName() {
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

		// Format the date and time in the format MMDDYYHHMMSS
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMddyyHHmmss");
		String formattedDateTime = now.format(formatter);
		return formattedDateTime;

	}
}
