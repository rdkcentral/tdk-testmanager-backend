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

import com.rdkm.tdkservice.dto.FunctionCreateDTO;
import com.rdkm.tdkservice.dto.FunctionDTO;

/**
 * Service interface for managing function details.
 */
public interface IFunctionService {

	/**
	 * Creates a new function.
	 *
	 * @param functionCreateDTO the data transfer object containing the function
	 *                          details
	 * @return true if the function was created successfully, false otherwise
	 */
	boolean createFunction(FunctionCreateDTO functionCreateDTO);

	/**
	 * Updates an existing function.
	 *
	 * @param functionDTO the data transfer object containing the updated function
	 *                    details
	 * @return true if the function was updated successfully, false otherwise
	 */
	boolean updateFunction(FunctionDTO functionDTO);

	/**
	 * Finds all functions.
	 *
	 * @return a list of data transfer objects containing the details of all
	 *         functions
	 */
	List<FunctionDTO> findAllFunctions();

	/**
	 * Finds a function by its ID.
	 *
	 * @param id the ID of the function
	 * @return the data transfer object containing the details of the function, or
	 *         null if not found
	 */
	FunctionDTO findFunctionById(UUID id);

	/**
	 * Deletes a function by its ID.
	 *
	 * @param id the ID of the function
	 * @return true if the function was deleted successfully, false otherwise
	 */
	void deleteFunction(UUID id);

	/**
	 * Finds all functions by their category.
	 *
	 * @param category the category of the functions
	 * @return a list of data transfer objects containing the details of all
	 *         functions in the specified category
	 */
	List<FunctionDTO> findAllByCategory(String category);

	/**
	 * Finds all functions by their module.
	 *
	 * @param moduleName the module of the functions
	 * @return a list of data transfer objects containing the details of all
	 *         functions in the specified module
	 */
	public List<FunctionDTO> findAllFunctionsByModule(String moduleName);

	/**
	 * Finds all functions by their module.
	 *
	 * @param moduleName the module of the functions
	 * @return a list of function names in the specified module
	 */
	List<String> findAllFunctionNameByModule(String category);
}