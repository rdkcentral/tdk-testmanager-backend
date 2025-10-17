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
package com.rdkm.tdkci.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rdkm.tdkci.model.Device;

/**
 * Repository interface for managing Device entities.
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

	/**
	 * Checks if a device with the specified name exists in the repository.
	 *
	 * @param deviceName the name of the device to check for existence
	 * @return {@code true} if a device with the given name exists, {@code false}
	 *         otherwise
	 */
	boolean existsByName(String deviceName);

	/**
	 * Checks if a device exists with the specified IP address.
	 *
	 * @param deviceIp the IP address of the device to check
	 * @return true if a device with the given IP exists, false otherwise
	 */
	boolean existsByIp(String deviceIp);

	/**
	 * Checks if a device with the specified MAC address exists in the repository.
	 *
	 * @param deviceMac the MAC address of the device to check
	 * @return true if a device with the given MAC address exists, false otherwise
	 */
	boolean existsByMacAddress(String deviceMac);

}
