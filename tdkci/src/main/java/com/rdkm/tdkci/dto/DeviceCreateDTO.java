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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Data Transfer Object for creating a new Device.
 */
/**
 * Data Transfer Object for creating a Device.
 * Contains device details and validation constraints.
 */
@Data
public class DeviceCreateDTO {

	/**
	 * The name of the device.
	 * Must not be blank.
	 */
	@NotBlank(message = "Device Name is required")
	private String deviceName;

	/**
	 * The IP address of the device.
	 * Must not be blank.
	 */
	@NotBlank(message = "Device IP is required")
	private String deviceIp;

	/**
	 * The MAC address of the device.
	 * Must not be blank and must follow the format XX:XX:XX:XX:XX:XX.
	 */
	@NotBlank
	@Pattern(regexp = "^([0-9A-F]{2}:){5}[0-9A-F]{2}$", message = "Invalid MAC address format")
	private String deviceMac;

	/**
	 * The name of the last updated image for the device.
	 */
	private String lastUpdatedImageName;

	/**
	 * Indicates if the device requires an upgrade.
	 */
	private boolean isDeviceUpgradeRequired;

	/**
	 * The port associated with the device.
	 */
	private String devicePort;

	/**
	 * The file extension used by the device.
	 */
	private String deviceFileExtension;

	/**
	 * The category of the device.
	 */
	private String deviceCategory;

	/**
	 * The status of the device.
	 */
	private String deviceStatus;

	/**
	 * List of image prefixes associated with the device.
	 */
	private List<String> imagePrefixes;

	/**
	 * List of suites applicable to the device.
	 */
	private List<String> deviceSuites;

	/**
	 * List of test scripts for the device.
	 */
	private List<String> deviceTestScripts;

	/**
	 * The Xconf model name for the device.
	 * Must not be blank.
	 */
	@NotBlank(message = "Xconf model Name is required")
	private String xconfModelName;

}
