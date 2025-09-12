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
package com.rdkm.tdkservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.model.Device;
import com.rdkm.tdkservice.model.DeviceType;

@Repository
public interface DeviceRepositroy extends JpaRepository<Device, UUID> {
	/**
	 * This method is used to check if a Device exists by its Ip.
	 *
	 * @param ip This is the stbIp of the Device to be checked.
	 * @return boolean This returns true if the Device exists, false otherwise.
	 */
	boolean existsByIp(String ip);

	/**
	 * This method is used to check if a Device exists by its stbName.
	 *
	 * @param name This is the stbName of the Device to be checked.
	 * @return boolean This returns true if the Device exists, false otherwise.
	 */
	boolean existsByName(String name);

	/**
	 * This method is used to check if a Device exists by its macId.
	 *
	 * @param macid This is the macId of the Device to be checked.
	 * @return boolean This returns true if the Device exists, false otherwise.
	 */
	boolean existsByMacId(String macid);

	/**
	 * This method is used to find a Device by its stbIp.
	 *
	 * @param ip This is the stbIp of the Device to be found.
	 * @return Device This returns the found Device.
	 */
	Device findByIp(String ip);

	/**
	 * This method is used to find a Device by its macId.
	 *
	 * @param macid This is the macId of the Device to be found.
	 * @return Device This returns the found Device.
	 */
	Device findByMacId(String macid);

	/**
	 * This method is used to find a Device by its stbName.
	 *
	 * @param category This is the stbName of the Device to be found.
	 * @return Device This returns the found Device.
	 */
	List<Device> findByCategory(Category category);

	/**
	 * This method is used to find a Device by its deviceType.
	 *
	 * @param deviceType This is the deviceType of the Device to be found.
	 * @return Device This returns the found Device.
	 */
	List<Device> findByDeviceType(DeviceType deviceType);

	/**
	 * This method is used to find a Device by its stbName.
	 *
	 * @param name This is the stbName of the Device to be found.
	 * @return Device This returns the found Device.
	 */
	Device findByName(String name);

	/**
	 * This method is used to find all Devices by their category.
	 *
	 * @param category This is the category of the Devices to be found.
	 * @return List<Device> This returns the found Devices.
	 */
	List<Device> findAllByCategory(Category category);

	/**
	 * This method is used to find all Devices by their deviceType.\ and category
	 *
	 * @param deviceType This is the deviceType of the Devices to be found.
	 * @return List<Device> This returns the found Devices.
	 */
	List<Device> findByDeviceTypeAndCategory(DeviceType deviceType, Category category);

	/**
	 * This method is used to find a Device by its deviceIP and port.
	 *
	 * @param deviceIP This is the deviceIP of the Device to be found.
	 * @param port     This is the port of the Device to be found.
	 */
	Device findByIpAndPort(String deviceIP, String port);

	/**
	 * Finds a list of devices by their category and thunder enabled status.
	 *
	 * @param category         the category of the devices to find
	 * @param isThunderEnabled the thunder enabled status of the devices to find
	 * @return a list of devices that match the given category and thunder enabled
	 *         status
	 */
	List<Device> findByCategoryAndIsThunderEnabled(Category category, Boolean isThunderEnabled);

	/**
	 * Retrieves all Device entities from the database, sorted by their deviceStatus
	 * in the following order: INPROGRESS, BUSY, HANG. Any devices with a
	 * deviceStatus not in this list will be placed at the end.
	 *
	 * @return A List of Device objects sorted by deviceStatus.
	 */
	@Query("SELECT d FROM Device d ORDER BY FIELD(d.deviceStatus, 'BUSY', 'HANG','FREE','NOT_FOUND','ALLOCATED','TDK_DISABLED')")
	List<Device> findAllSortedByDeviceStatus();

}
