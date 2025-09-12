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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.model.Module;
import com.rdkm.tdkservice.model.Script;

/**
 * The ScriptRepository interface is used to interact with the database. It
 * extends the JpaRepository interface which contains methods for performing
 * CRUD operations. The JpaRepository interface takes two parameters, the entity
 * class and the type of the primary key. The entity class is the class that is
 * being managed by the repository.
 */
@Repository
public interface ScriptRepository extends JpaRepository<Script, UUID> {

	/**
	 * This method is used to check whether the script exists by name.
	 * 
	 * @param name - the name of the script
	 * @return boolean - true if the script exists, false otherwise
	 */
	boolean existsByName(String name);

	/**
	 * This method is used to find the script by name.
	 * 
	 * @param name - the name of the script
	 * @return Script - the script
	 */
	List<Script> findAllByModule(Module module);

	/**
	 * This method is used to find the script by name.
	 * 
	 * @param testScriptName - the name of the script
	 * @return Script - the script
	 */
	Script findByName(String testScriptName);

	/**
	 * This method is used to find the script by name and category.
	 * 
	 * @param name     - the name of the script
	 * @param category - the category
	 * @return Script - the script
	 */
	Script findByNameAndCategory(String name, Category category);

	/**
	 * This method is used to find all the scripts by category.
	 * 
	 * @param category - the category
	 * @return List - the list of scripts
	 */
	List<Script> findAllByCategory(Category category);

	/**
	 * This method is used to find all the scripts by module and isLongDuration.
	 * 
	 * @param module         - module to which the script belongs to
	 * @param isLongDuration - Are the scripts long duration - boolean
	 * @return List - the list of scripts
	 */
	@Query("SELECT s FROM Script s WHERE s.module = ?1 AND s.isLongDuration = ?2")
	List<Script> findAllByModuleAndIsLongDuration(Module module, boolean isLongDuration);

	/**
	 * This method is used to find all the scripts by category.
	 * 
	 * @param name - the name of the category
	 * @return List - the list of scripts
	 */
	List<Script> findAllByCategory(String name);

	/**
	 * This method is used to find scripts that were created or updated after the
	 * specified timestamps.
	 * 
	 * @param createdDate the timestamp after which scripts were created
	 * @param updatedAt   the timestamp after which scripts were updated
	 * @return a list of scripts that match the criteria
	 */
	List<Script> findByCreatedDateAfterOrUpdatedAtAfter(Instant createdDate, Instant updatedAt);

	/**
	 * This method is used to find scripts whose pre-conditions were created or
	 * updated after the specified timestamp.
	 * 
	 * @param since the timestamp after which pre-conditions were created or updated
	 * @return a list of scripts that match the criteria
	 */
	@Query("SELECT DISTINCT p.script FROM PreCondition p WHERE p.createdDate > :since OR p.updatedAt > :since")
	List<Script> findScriptsWithPreConditionChangedSince(@Param("since") Instant since);

	/**
	 * This method is used to find scripts whose test steps were created or updated
	 * after the specified timestamp.
	 * 
	 * @param since the timestamp after which test steps were created or updated
	 * @return a list of scripts that match the criteria
	 */
	@Query("SELECT s FROM Script s JOIN s.testSteps ts WHERE ts.createdDate > :since OR ts.updatedAt > :since")
	List<Script> findScriptsWithTestStepChangedSince(@Param("since") Instant since);

}
