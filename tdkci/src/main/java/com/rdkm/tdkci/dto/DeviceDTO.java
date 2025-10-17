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
package com.rdkm.tdkci.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Data Transfer Object representing a device and its associated properties.
 */
@Data
public class DeviceDTO {

	/**
	 * Unique identifier for the device.
	 * Must not be null.
	 */
	@NotNull(message = "Device id is required")
	private UUID id;

	/**
	 * Name of the device.
	 */
	private String deviceName;

	/**
	 * IP address assigned to the device.
	 */
	private String deviceIp;

	/**
	 * MAC address of the device in format XX:XX:XX:XX:XX:XX.
	 */
	@Pattern(regexp = "^([0-9A-F]{2}:){5}[0-9A-F]{2}$", message = "Invalid MAC address format")
	private String deviceMac;

	/**
	 * Name of the last updated image for the device.
	 */
	private String lastUpdatedImageName;

	/**
	 * Indicates whether a device upgrade is required.
	 */
	private boolean isDeviceUpgradeRequired;

	/**
	 * Port number or identifier used by the device.
	 */
	private String devicePort;

	/**
	 * File extension associated with the device.
	 */
	private String deviceFileExtension;

	/**
	 * Category or type of the device.
	 */
	private String deviceCategory;

	/**
	 * Current status of the device.
	 */
	private String deviceStatus;

	/**
	 * List of image prefixes related to the device.
	 */
	private List<String> imagePrefixes;

	/**
	 * List of suites available for the device.
	 */
	private List<String> deviceSuites;

	/**
	 * List of test scripts associated with the device.
	 */
	private List<String> deviceTestScripts;

	/**
	 * XConf model name for the device.
	 */
	private String xconfModelName;

}
