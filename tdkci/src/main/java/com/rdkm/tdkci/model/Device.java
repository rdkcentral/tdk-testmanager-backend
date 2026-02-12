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
package com.rdkm.tdkci.model;

import java.util.ArrayList;
import java.util.List;

import com.rdkm.tdkci.enums.Category;
import com.rdkm.tdkci.enums.DeviceStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a device entity in the system.
 * Stores device-specific information such as name, IP, MAC address, status,
 * configuration, and related collections.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "device")
public class Device extends BaseEntity {

	/**
	 * Unique name of the device.
	 */
	@Column(unique = true)
	private String name;

	/**
	 * Unique IP address assigned to the device.
	 */
	@Column(unique = true)
	private String ip;

	/**
	 * Unique MAC address of the device.
	 */
	@Column(unique = true)
	private String macAddress;

	/**
	 * Name of the last updated image for the device.
	 */
	private String lastUpdatedImageName;

	/**
	 * Indicates whether a device upgrade is required.
	 */
	private boolean isUpgradeRequired;

	/**
	 * Port number or identifier associated with the device.
	 */
	private String port;

	/**
	 * File extension used for device-related files.
	 */
	private String fileExtension;

	/**
	 * Category of the device.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Category category;

	/**
	 * Current status of the device.
	 * Defaults to DeviceStatus.FREE.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DeviceStatus deviceStatus = DeviceStatus.FREE;

	/**
	 * List of image prefixes associated with the device.
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "image_prefix", joinColumns = @JoinColumn(name = "device_id"))
	@Column(name = "image_prefix_name")
	private List<String> imagePrefixes = new ArrayList<>();

	/**
	 * List of suites associated with the device.
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "device_suites", joinColumns = @JoinColumn(name = "device_id"))
	@Column(name = "suite_name")
	private List<String> deviceSuites = new ArrayList<>();

	/**
	 * List of test scripts associated with the device.
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "device_test_scripts", joinColumns = @JoinColumn(name = "device_id"))
	@Column(name = "script_name")
	private List<String> deviceTestScripts = new ArrayList<>();

	/**
	 * Reference to the XconfConfig entity associated with the device.
	 */
	@ManyToOne(cascade = CascadeType.PERSIST )
	@JoinColumn(name = "xconf_id", nullable = false)
	private XconfConfig xconfConfig;

}
