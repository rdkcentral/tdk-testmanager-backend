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

import com.rdkm.tdkservice.dto.SocCreateDTO;
import com.rdkm.tdkservice.dto.SocDTO;

/**
 * 
 * This interface provides methods to create, read, update and delete Soc
 * details.
 * 
 * @version 1.0
 * @since 1.0
 * 
 */
public interface ISocService {

	/**
	 * This method is used to create a new SocVendor.
	 * 
	 * @param socDTO This is the request object containing the details of the
	 *               SocVendor to be created.
	 * @return boolean This returns true if the SocVendor was created successfully,
	 *         false otherwise.
	 */
	boolean createSoc(SocCreateDTO socDTO);

	/**
	 * This method is used to retrieve all SocVendors.
	 * 
	 * @return List<SocVendorDTO> This returns a list of all SocVendors.
	 */

	List<SocDTO> findAll();

	/**
	 * This method is used to delete a SocVendor by its id.
	 * 
	 * @param id This is the id of the SocVendor to be deleted.
	 */

	void deleteSoc(UUID id);

	/**
	 * This method is used to find a SocVendor by its id.
	 * 
	 * @param id This is the id of the SocVendor to be found.
	 * @return SocVendorDTO This returns the found SocVendor.
	 */

	SocDTO findById(UUID id);

	/**
	 * This method is used to update a Soc.
	 * 
	 * @param socUpdateDTO This is the request object containing the updated details
	 *                     of the Soc.
	 * @param id           This is the id of the Soc to be updated.
	 * @return SocVendorDTO This returns the updated SocVendor.
	 */

	public SocDTO updateSoc(SocDTO socUpdateDTO);

	/**
	 * This method is used to retrieve all SocVendors DTO by category.
	 * 
	 * @param category This is the category of the SocVendors to be retrieved.
	 * @return List<SocVendorDTO> This returns a list of SocVendors.
	 */
	List<SocDTO> getSOCsByCategory(String category);

	/**
	 * This method is used to retrieve all SocVendors name by category.
	 * 
	 * @param category This is the category of the SocVendors to be retrieved.
	 * @return List<String> This returns a list of SocVendors.
	 */
	List<String> getSOCsListByCategory(String category);

}
