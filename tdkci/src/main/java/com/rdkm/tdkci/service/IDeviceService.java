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
package com.rdkm.tdkci.service;

import java.util.List;
import java.util.UUID;

import com.rdkm.tdkci.dto.DeviceCreateDTO;
import com.rdkm.tdkci.dto.DeviceDTO;

/**
 * Service interface for managing devices.
 */
public interface IDeviceService {

	/**
	 * Creates a new device based on the provided device request data.
	 *
	 * @param deviceRequest the data transfer object containing device creation
	 *                      details
	 * @return true if the device was successfully created, false otherwise
	 */
	boolean createDevice(DeviceCreateDTO deviceRequest);

	/**
	 * Retrieves a list of all devices.
	 *
	 * @return a list of {@link DeviceDTO} objects representing all devices.
	 */
	List<DeviceDTO> getAllDevices();

	/**
	 * Retrieves a list of all device names.
	 *
	 * @return a list containing the names of all devices
	 */
	List<String> getAllDeviceNames();

	/**
	 * Deletes a device identified by the specified UUID.
	 *
	 * @param id the unique identifier of the device to be deleted
	 * @return true if the device was successfully deleted, false otherwise
	 */
	boolean deleteDeviceById(UUID id);

	/**
	 * Updates the details of an existing device.
	 *
	 * @param deviceUpdateRequest the DTO containing updated device information
	 * @return true if the device was successfully updated, false otherwise
	 */
	boolean updateDevice(DeviceDTO deviceUpdateRequest);

}
