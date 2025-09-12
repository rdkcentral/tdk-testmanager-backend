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
import com.rdkm.tdkservice.model.Oem;

/**
 * The OemRepository interface provides methods for oem operations.
 */

@Repository
public interface OemRepository extends JpaRepository<Oem, UUID> {

	/**
	 * This method is used to find a oem by name.
	 *
	 * @param oem the name of the oem to find
	 * @return a oem object containing the oem's information
	 */
	Oem findByNameAndCategory(String oem, Category category);

	/**
	 * This method is used to delete a oem by name.
	 *
	 * @param name the name of the oem to delete
	 * @return a oem object containing the oem's information
	 */
	Oem deleteByName(String name);

	/**
	 * This method is used to check if a oem exists by name.
	 *
	 * @param name the name of the oem to check
	 * @return a boolean value indicating whether the oem exists
	 */
	boolean existsByNameAndCategory(String name, Category category);

	/**
	 * This method is used to find a list of Oems by category.
	 *
	 * @param category the category of the oems to find
	 * @return a list of oems objects containing the oem's information
	 */
	List<Oem> findByCategory(Category category);

	/**
	 * This method is used to find oem that were created or updated after the
	 * specified timestamps.
	 * 
	 * @param createdDate the timestamp after which oem were created
	 * @param updatedAt   the timestamp after which oem were updated
	 * @return a list of oem that match the criteria
	 */
	List<Oem> findByCreatedDateAfterOrUpdatedAtAfter(Instant createdDate, Instant updatedAt);

}
