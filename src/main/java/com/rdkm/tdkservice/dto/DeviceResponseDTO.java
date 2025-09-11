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

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Represents the details of a device entity.
 */

@Data
public class DeviceResponseDTO {

	/**
	 * Represents the unique identifier for the device.
	 */
	private UUID id;
	/**
	 * Represents the stbip of the device.
	 */
	private String deviceIp;
	/**
	 * Represents the stbName of the device.
	 */
	private String deviceName;
	/**
	 * Represents the stbPort of the device.
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
	 * Represents the macId of the device.
	 */
	private String macId;
	/**
	 * Represents the deviceTypeName of the device.
	 */
	private String deviceTypeName;
	/**
	 * Represents the OemName of the device.
	 */
	private String oemName;
	/**
	 * Represents the socName of the device.
	 */
	private String socName;

	/**
	 * Represents the thunderEnabled status of the device.
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
	 * Represents the category of the device.
	 */
	@NotBlank
	private String category;
	
	/**
	 * Represents the configure port status.
	 */
	private boolean isDevicePortsConfigured;
}
