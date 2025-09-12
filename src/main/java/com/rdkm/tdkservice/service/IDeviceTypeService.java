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
package com.rdkm.tdkservice.service;

import java.util.List;
import java.util.UUID;

import com.rdkm.tdkservice.dto.DeviceTypeCreateDTO;
import com.rdkm.tdkservice.dto.DeviceTypeDTO;

/**
 * This interface defines the contract for the oemService. It provides methods
 * to perform CRUD operations on deviceType entities.
 */
public interface IDeviceTypeService {

	/**
	 * This method is used to create a new deviceType.
	 * 
	 * @param deviceTypeDTO This is the request object containing the details of the
	 *                      deviceType to be created.
	 * @return boolean This returns true if the deviceType was created successfully,
	 *         false otherwise.
	 */
	boolean createDeviceType(DeviceTypeCreateDTO deviceTypeDTO);

	/**
	 * This method is used to retrieve all deviceTypes.
	 * 
	 * @return List<DeviceTypeDTO> This returns a list of all deviceTypes.
	 */
	List<DeviceTypeDTO> getAllDeviceTypes();

	/**
	 * This method is used to delete a deviceType by its id.
	 * 
	 * @param id This is the id of the deviceType to be deleted.
	 */
	void deleteById(UUID id);

	/**
	 * This method is used to find a deviceType by its id.
	 * 
	 * @param id This is the id of the deviceType to be found.
	 * @return id This returns the found deviceType.
	 */
	DeviceTypeDTO findById(UUID id);

	/**
	 * This method is used to update a deviceType.
	 * 
	 * @param deviceTypeUpdateDTO This is the request object containing the updated
	 *                            details of the DeviceType.
	 * @param id                  This is the id of the DeviceType to be updated.
	 * @return deviceTypeUpdateDTO This returns the updated DeviceType.
	 */
	DeviceTypeDTO updateDeviceType(DeviceTypeDTO deviceTypeUpdateDTO);

	/**
	 * This method is used to retrieve all DeviceTypes by category.
	 * 
	 * @param category This is the category of the DeviceTypes to be retrieved.
	 * @return List<DeviceTypeRequest> This returns a list of DeviceTypes.
	 */
	List<DeviceTypeDTO> getDeviceTypesByCategory(String category);

	/**
	 * This method is used to retrieve all DeviceTypes by category.
	 * 
	 * @param category This is the category of the DeviceTypes to be retrieved.
	 * @return List<String> This returns a list of DeviceTypes.
	 */
	List<String> getDeviceTypeNameByCategory(String category);

}
