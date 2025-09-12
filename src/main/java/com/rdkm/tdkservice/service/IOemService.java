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

import com.rdkm.tdkservice.dto.OemCreateDTO;
import com.rdkm.tdkservice.dto.OemDTO;

/**
 * This is the IOemService interface and contains the methods related to oem.
 * 
 */

public interface IOemService {

	/**
	 * This method is used to create a new oem.
	 * 
	 * @param oemDTO This is the request object containing the details of the oem to
	 *               be created.
	 * @return boolean This returns true if the oem was created successfully, false
	 *         otherwise.
	 */

	boolean createOem(OemCreateDTO oemDTO);

	/**
	 * This method is used to retrieve all oem's.
	 * 
	 * @return List<oem> This returns a list of all oems.
	 */

	List<OemDTO> getAllOem();

	/**
	 * This method is used to delete a oem by its id.
	 * 
	 * @param id This is the id of the oem to be deleted.
	 */

	void deleteOem(UUID id);

	/**
	 * This method is used to find a oem by its id.
	 * 
	 * @param id This is the id of the oem to be found.
	 * @return oem This returns the found oem.
	 */

	OemDTO findById(UUID id);

	/**
	 * This method is used to update a oem.
	 * 
	 * @param oemUpdateDTO This is the request object containing the updated details
	 *                     of the oemUpdateDTO.
	 * @param id           This is the id of the oemUpdateDTO to be updated.
	 * @return oemUpdateDTO This returns the updated oemUpdateDTO.
	 */

	OemDTO updateOem(OemDTO oemUpdateDTO);

	/**
	 * This method is used to retrieve all oems by category.
	 * 
	 * @param category This is the category of the oems to be retrieved.
	 * @return List<oemUpdateDTO> This returns a list of oemUpdateDTO.
	 */
	List<OemDTO> getOemsByCategory(String category);

	/**
	 * This method is used to retrieve all oems by category.
	 * 
	 * @param category This is the category of the oems to be retrieved.
	 * @return List<String> This returns a list of oems.
	 */
	List<String> getOemListByCategory(String category);

}
