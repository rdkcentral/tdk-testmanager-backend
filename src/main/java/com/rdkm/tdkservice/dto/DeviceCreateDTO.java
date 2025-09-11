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
package com.rdkm.tdkservice.dto;

import com.rdkm.tdkservice.enums.DeviceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DeviceCreateDTO {

	/**
	 * Represents the deviceIp of the device. This field is mandatory, hence it
	 * cannot be blank.
	 */
	@NotBlank
	private String deviceIp;
	/**
	 * Represents the deviceName of the device. This field is mandatory, hence it
	 * cannot
	 */
	@NotBlank
	private String deviceName;

	/**
	 * Represents the devicePort of the device.
	 */
	private String devicePort;
	/**
	 * Represents the statusPort of the device.
	 */
	private String statusPort;
	/**
	 * Represents the agentMonitorPort of the device.
	 */
	private String agentMonitorPort;
	/**
	 * Represents the logTransferPort of the device.
	 */
	private String logTransferPort;

	/**
	 * Represents the macId of the device. This field is mandatory, hence it cannot
	 * be blank.
	 */
	@NotBlank
	@Pattern(regexp = "^([0-9A-F]{2}:){5}[0-9A-F]{2}$", message = "Invalid MAC address format")
	private String macId;

	/**
	 * Represents the deviceTypeName of the device.
	 */
	private String deviceTypeName;
	/**
	 * Represents the oemName of the device.
	 */
	private String oemName;
	/**
	 * Represents the socName of the device.
	 */
	private String socName;
	/**
	 * Represents the deviceStatus of the device.
	 */
	private DeviceStatus devicestatus;

	/**
	 * Represents the isThunderEnabled of the device.
	 */
	private boolean isThunderEnabled;
	/**
	 * Represents the thunderPort of the device.
	 */
	private String thunderPort;
	/**
	 * Represents the userGroupName of the device.
	 */
	private String userGroupName;
	/**
	 * Represents the category of the device. This field is mandatory, hence it
	 * cannot be blank.
	 */
	@NotBlank
	private String category;

	/**
	 * Represents the isDevicePortsConfigured of the device.
	 */
	private boolean isDevicePortsConfigured;
}