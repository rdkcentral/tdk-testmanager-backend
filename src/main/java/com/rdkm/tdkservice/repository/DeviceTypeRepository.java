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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.enums.DeviceTypeCategory;
import com.rdkm.tdkservice.model.DeviceType;

/**
 * The DeviceTypeRepository interface provides methods for device type
 * operations.
 */
@Repository
public interface DeviceTypeRepository extends JpaRepository<DeviceType, UUID> {
	/**
	 * This method is used to find a device type by name.
	 *
	 * @param deviceType the name of the device type to find
	 * @return a deviceType object containing the device type's information
	 */
	DeviceType findByNameAndCategory(String name, Category category);

	/**
	 * This method is used to delete a device type by name.
	 *
	 * @param name the name of the device type to delete
	 * @return a deviceType object containing the deviceType type's information
	 */
	DeviceType deleteByName(String name);

	/**
	 * This method is used to check if a device type exists by name.
	 *
	 * @param name the name of the device type to check
	 * @return a boolean value indicating whether the device type exists
	 */
	boolean existsByNameAndCategory(String name, Category category);

	/**
	 * This method is used to find a list of device types by category.
	 *
	 * @param category the category of the device type to find
	 * @return a list of deviceType objects containing the device type's information
	 */
	List<DeviceType> findByCategory(Category category);

	/**
	 * This method is used to find a list of device types by type.
	 *
	 * @param type the type of the device type to find
	 * @return a list of deviceType objects containing the device type's information
	 */
	List<DeviceType> findByType(DeviceTypeCategory type);

	/**
	 * This method is used to get the device type by created date or updated at.
	 *
	 * @param createdDate
	 * @param updatedAt
	 * @return List<DeviceType>
	 */
	List<DeviceType> findByCreatedDateAfterOrUpdatedAtAfter(Instant createdDate, Instant updatedAt);

}
