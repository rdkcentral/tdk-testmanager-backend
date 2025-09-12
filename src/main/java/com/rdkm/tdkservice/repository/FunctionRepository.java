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
import com.rdkm.tdkservice.model.Function;
import com.rdkm.tdkservice.model.Module;

/**
 * Repository interface for accessing Function entities.
 */
@Repository
public interface FunctionRepository extends JpaRepository<Function, UUID> {

	/**
	 * Finds a function by its name.
	 *
	 * @param name the name of the function
	 * @return the function with the specified name, or null if not found
	 */
	Function findByName(String name);

	/**
	 * Finds all functions by their category.
	 *
	 * @param category the category of the functions
	 * @return a list of functions in the specified category
	 */
	List<Function> findAllByCategory(Category category);

	/**
	 * Checks if a function with the specified name exists.
	 *
	 * @param name the name of the function
	 * @return true if a function with the specified name exists, false otherwise
	 */
	boolean existsByName(String name);

	/**
	 * Finds all functions by their module ID.
	 *
	 * @param moduleId the ID of the module
	 * @return a list of functions in the specified module
	 */
	List<Function> findAllByModuleId(UUID moduleId);

	/**
	 * Finds all functions by their module.
	 *
	 * @param module the module
	 * @return a list of functions in the specified module
	 */
	List<Function> findAllByModule(Module module);
	
	/**
	 * This method is used to find functions that were created or updated after the specified timestamps.
	 * @param createdDate the timestamp after which functions were created
	 * @param updatedAt the timestamp after which functions were updated
	 * @return a list of functions that match the criteria
	 */
	List<Function> findByCreatedDateAfterOrUpdatedAtAfter(Instant createdDate, Instant updatedAt);

}