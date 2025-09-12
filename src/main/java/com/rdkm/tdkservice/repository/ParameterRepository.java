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

import com.rdkm.tdkservice.model.Function;
import com.rdkm.tdkservice.model.Parameter;

/**
 * Repository interface for accessing ParameterType entities.
 */
@Repository
public interface ParameterRepository extends JpaRepository<Parameter, UUID> {

	/**
	 * Finds a parameter type by its name.
	 *
	 * @param name the name of the parameter type
	 * @return the parameter type with the specified name, or null if not found
	 */
	boolean existsByName(String name);

	/**
	 * Finds all parameter types by function ID.
	 *
	 * @param functionId the ID of the function
	 * @return a list of parameter types with the specified function ID
	 */
	List<Parameter> findAllByFunctionId(UUID functionId);

	/**
	 * Finds all parameters by function.
	 *
	 * @param function the function
	 * @return a list of parameters with the specified function
	 */

	List<Parameter> findByFunction(Function function);

	/**
	 * Finds a parameter by its name.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter with the specified name, or null if not found
	 */
	Parameter findByName(String parameterName);

	/**
	 * Exist a parameter by its name and function
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter with the specified name, or null if not found
	 */
	boolean existsByNameAndFunction(String name, Function function);

	/**
	 * Find by name and function
	 * 
	 * @param parameterName
	 * @return
	 */
	Parameter findByNameAndFunction(String name, Function function);
	
	/**
	 * This method is used to find parameters that were created or updated after the specified timestamps.
	 * @param createdDate the timestamp after which parameters were created
	 * @param updatedAt the timestamp after which parameters were updated
	 * @return a list of parameters that match the criteria
	 */
	List<Parameter> findByCreatedDateAfterOrUpdatedAtAfter(Instant createdDate, Instant updatedAt);


}