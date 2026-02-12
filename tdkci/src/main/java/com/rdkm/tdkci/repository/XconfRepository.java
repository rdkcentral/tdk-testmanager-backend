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
package com.rdkm.tdkci.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rdkm.tdkci.model.XconfConfig;

/**
 * Repository interface for managing XconfConfig entities.
 */
@Repository
public interface XconfRepository extends JpaRepository<XconfConfig, UUID> {

	/**
	 * Retrieves an {@link XconfConfig} entity by its model name.
	 *
	 * @param xconfModelName the name of the Xconf model to search for
	 * @return the {@link XconfConfig} associated with the specified model name, or
	 *         {@code null} if not found
	 */
	XconfConfig findByName(String xconfModelName);

	/**
	 * Checks if an Xconf configuration exists with the specified ID.
	 *
	 * @param xconfConfigId the ID of the Xconf configuration to check for existence
	 * @return true if a configuration with the given ID exists, false otherwise
	 */
	boolean existsByXconfigId(String xconfConfigId);

	/**
	 * Checks if an Xconf configuration exists with the specified name.
	 *
	 * @param xconfConfigName the name of the Xconf configuration to check for
	 *                        existence
	 * @return true if a configuration with the given name exists, false otherwise
	 */
	boolean existsByXconfigName(String xconfConfigName);

}
