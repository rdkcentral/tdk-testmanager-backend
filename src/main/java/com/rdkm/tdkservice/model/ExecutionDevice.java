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
package com.rdkm.tdkservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents the execution device entity. This class is used to store the
 * execution device information.
 * 
 * Fields: - device: The device entity. - buildName: The build name. -
 * execution: The execution entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class ExecutionDevice extends BaseEntity {

	/**
	 * The device name.
	 */
	String device;

	/**
	 * The build name.
	 */
	String buildName;
	
	/**
	 * The device ip address.
	 */
	String deviceIp;

	/**
	 * The device mac address.
	 */
	String deviceMac;

	/**
	 * The device type.
	 */
	String deviceType;

	/**
	 * The execution entity
	 * 
	 */
	@OneToOne
	Execution execution;

}
