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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.dto.ScriptCreateDTO;
import com.rdkm.tdkservice.dto.ScriptDTO;
import com.rdkm.tdkservice.dto.ScriptDetailsResponse;
import com.rdkm.tdkservice.dto.ScriptListDTO;
import com.rdkm.tdkservice.dto.ScriptModuleDTO;
import com.rdkm.tdkservice.model.Module;

/**
 * Service for scripts.
 */
public interface IScriptService {

	/**
	 * This method is used to save the script.
	 * 
	 * @param scriptFile      - the script file
	 * @param scriptCreateDTO - the script create dto
	 * @return boolean - true if the script is saved successfully, false otherwise
	 */
	boolean saveScript(MultipartFile scriptFile, ScriptCreateDTO scriptCreateDTO);

	/**
	 * This method is used to update the script.
	 * 
	 * @param scriptFile      - the script file
	 * @param scriptUpdateDTO - the script update dto
	 * @return - true if the script is updated successfully, false otherwise
	 */
	boolean updateScript(MultipartFile scriptFile, ScriptDTO scriptUpdateDTO);

	/**
	 * This method is used to delete the script.
	 * 
	 * @param scriptId - the script
	 * @return - true if the script is deleted successfully, false otherwise
	 */
	boolean deleteScript(UUID scriptId);

	/**
	 * This method is used to get the list of scripts based on the module.
	 * 
	 * @param moduleName - the module name
	 * @return - the list of scripts based on the module
	 */
	List<ScriptListDTO> findAllScriptsByModule(String moduleName);

	/**
	 * This method is used to get all the scripts based on the module by the
	 * category
	 * 
	 * @param category - the category
	 * @return - the list of scripts for all the modules by the given category
	 */
	List<ScriptModuleDTO> findAllScriptByModuleWithCategory(String category);

	/**
	 * This method is used to get the script details excel by module.
	 * 
	 * @param moduleName - the module name
	 * @return - the script
	 */
	ByteArrayInputStream testCaseToExcelByModule(String moduleName);

	/**
	 * This method is used to get the script details excel by testScriptName.
	 * 
	 * @param moduleName - the module name
	 * @return - the script
	 */
	ByteArrayInputStream testCaseToExcel(String testScriptName);

	/**
	 * This method is used to get the script details by testscriptId.
	 * 
	 * @param scriptId - the script id
	 * @return - the script
	 */
	ScriptDTO findScriptById(UUID scriptId);

	/**
	 * Find all scripts by category
	 * 
	 * @param categoryName
	 * @return
	 */
	List<ScriptListDTO> findAllScriptsByCategory(String categoryName);

	/**
	 * This method is used to upload the zip file.
	 * 
	 * @param file - the file
	 * @return - true if the file is uploaded successfully, false otherwise
	 * @throws IOException
	 */

	boolean uploadZipFile(MultipartFile file) throws IOException;

	/**
	 * This method is used to generate the script zip.
	 * 
	 * @param scriptName - the script name
	 * @return - the script zip
	 */
	byte[] generateScriptZip(String scriptName);

	/**
	 * This method is used to get the script template details by primitiveTestName.
	 *
	 * @param primitiveTestName - the primitive test name
	 * @return - the script
	 */
	public String scriptTemplate(String primitiveTestName);

	/**
	 * This method is used to get the script details in excel form by category.
	 * 
	 * @param category - the category
	 * @return - the script
	 */
	ByteArrayInputStream testCaseToExcelByCategory(String category);

	/**
	 * This method is used to get the list of script names by category.
	 *
	 * @param category - the category
	 * @return - the list of script names by category
	 */
	public List<ScriptDetailsResponse> getListofScriptNamesByCategory(String category, boolean isThunderEnabled);

	/**
	 * This method is used to get the module for the given script name
	 * 
	 * @param scriptName - the script
	 * @return - the module - the module entity
	 */
	Module getModuleByScriptName(String scriptName);

	/**
	 * This method is used to get the script details by script name.
	 * 
	 * @param scriptName - the script name
	 * @return - the md file
	 */
	ByteArrayInputStream createMarkdownFile(String scriptNme);

	/**
	 * This method is used to get the script details by script id.
	 * 
	 * @param scriptId - the script id
	 * @return - the md file
	 */
	ByteArrayInputStream createMarkdownFilebyScriptId(UUID scriptId);

	/**
	 * This method is used to create the default test suite(test suite that has all
	 * the scripts of the module with module name) for existing modules it will
	 * include all the scripts added to the module
	 */
	void defaultTestSuiteCreationForExistingModule();

	/**
	 * This method is used to get the list of scripts based on the script group.
	 *
	 * @param scriptGroup - the script group name
	 * @return - the list of scripts based on the script group
	 */
	List<ScriptListDTO> findAllScriptsByTestSuite(String scriptGroup);

	/**
	 * This method is used to get the module execution time.
	 *
	 * @param moduleName - the module name
	 * @return - the module execution time in seconds
	 */
	Integer getModuleScriptTimeout(String moduleName);
}
