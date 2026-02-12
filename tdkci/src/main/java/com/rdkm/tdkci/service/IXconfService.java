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

import com.rdkm.tdkci.dto.XconfCreateDTO;
import com.rdkm.tdkci.dto.XconfDTO;

/**
 * Service interface for managing Xconf configurations.
 */
public interface IXconfService {

	/**
	 * Creates a new Xconf configuration based on the provided request data.
	 *
	 * @param xconfRequest the data transfer object containing the details for the
	 *                     new Xconf configuration
	 * @return true if the Xconf configuration was created successfully, false
	 *         otherwise
	 */
	boolean createXconf(XconfCreateDTO xconfRequest);

	/**
	 * Retrieves a list of all Xconf configuration data transfer objects.
	 *
	 * @return a list of {@link XconfDTO} representing all Xconf configurations.
	 */
	List<XconfDTO> findAllXconfConfigurations();

	/**
	 * Retrieves a list of Xconf names.
	 *
	 * @return a list of Xconf names as {@link String} objects.
	 */
	List<String> findXconfNames();

	/**
	 * Deletes the Xconf entity identified by the specified UUID.
	 *
	 * @param id the UUID of the Xconf entity to be deleted
	 * @return true if the entity was successfully deleted, false otherwise
	 */
	boolean deleteXconfById(UUID id);

	/**
	 * Updates the Xconf configuration based on the provided update request.
	 *
	 * @param xconfUpdateRequest the DTO containing the updated Xconf configuration
	 *                           details
	 * @return true if the update was successful, false otherwise
	 */
	boolean updateXconf(XconfDTO xconfUpdateRequest);

}
