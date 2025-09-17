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

package com.rdkm.tdkservice.service;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.dto.ModuleCreateDTO;
import com.rdkm.tdkservice.dto.ModuleDTO;

/**
 * Service interface for managing module details.
 */
public interface IModuleService {

	/**
	 * Saves a new module.
	 *
	 * @param moduleDTO the data transfer object containing the module details
	 * @return true if the module was saved successfully, false otherwise
	 */
	public boolean saveModule(ModuleCreateDTO moduleDTO);

	/**
	 * Updates an existing module.
	 *
	 * @param moduleDTO the data transfer object containing the updated module
	 *                  details
	 * @return true if the module was updated successfully, false otherwise
	 */
	public boolean updateModule(ModuleDTO moduleDTO);

	/**
	 * Finds all modules.
	 *
	 * @return a list of data transfer objects containing the details of all modules
	 */
	public List<ModuleDTO> findAllModules();

	/**
	 * Finds a module by its ID.
	 *
	 * @param id the ID of the module
	 * @return the data transfer object containing the details of the module, or
	 *         null if not found
	 */
	public ModuleDTO findModuleById(UUID id);

	/**
	 * Finds a module by its category.
	 *
	 * @param category the category of the module
	 * @return the data transfer object containing the details of the module, or
	 *         null if not found
	 */
	public List<ModuleDTO> findAllByCategory(String category);

	/**
	 * Deletes a module by its ID.
	 *
	 * @param id the ID of the module
	 * @return true if the module was deleted successfully, false otherwise
	 */
	public boolean deleteModule(UUID id);

	/**
	 * Finds all test groups.
	 *
	 * @return a list of all test groups
	 */
	public List<String> findAllTestGroupsFromEnum();

	/**
	 * Finds all module names by category.
	 *
	 * @return a list of all module names
	 */
	public List<String> findAllModuleNameByCategory(String category);

	/**
	 * Parses and saves the XML file.
	 *
	 * @param file the XML file
	 * @return
	 */
	boolean parseAndSaveXml(MultipartFile file);

	/**
	 * Generates the XML file.
	 *
	 * @param module the module name
	 * @return the XML file as a string
	 */
	String generateXML(String module);

	/**
	 * Generates the XML file.
	 *
	 * @param module the module name
	 * @return the XML file as a string
	 */
	ByteArrayResource downloadModulesAsZip(String category);

	/**
	 * Finds all module names by category.
	 *
	 * @return a list of all module names
	 */
	public List<String> findAllModuleNamesBySubCategory(String category);
}