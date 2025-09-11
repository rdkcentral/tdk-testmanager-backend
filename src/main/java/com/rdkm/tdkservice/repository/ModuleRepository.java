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
import com.rdkm.tdkservice.model.Module;

/**
 * Repository interface for accessing Module entities.
 */
@Repository
public interface ModuleRepository extends JpaRepository<Module, UUID> {

	/**
	 * Finds a module by its name.
	 *
	 * @param name the name of the module
	 * @return the module with the specified name, or null if not found
	 */
	Module findByName(String name);

	/**
	 * + * Finds all modules by category. + * @param categories + * @return +
	 */
	List<Module> findAllByCategoryIn(List<Category> categories);

	/**
	 * Finds all modules by category.
	 *
	 * @param category the category of the modules
	 * @return a list of modules with the specified category
	 */
	List<Module> findAllByCategory(Category category);

	/**
	 * Checks if a module with the specified name exists.
	 *
	 * @param name the name of the module
	 * @return true if a module with the specified name exists, false otherwise
	 */
	boolean existsByName(String name);

	/**
	 * Finds all modules.
	 *
	 * @return a list of all modules
	 */
	List<Module> findAll();
	
	/**
	 * This method is used to find modules that were created or updated after the specified timestamps.
	 * @param createdDate the timestamp after which modules were created
	 * @param updatedAt the timestamp after which modules were updated
	 * @return a list of modules that match the criteria
	 */
	List<Module> findByCreatedDateAfterOrUpdatedAtAfter(Instant createdDate, Instant updatedAt);


}