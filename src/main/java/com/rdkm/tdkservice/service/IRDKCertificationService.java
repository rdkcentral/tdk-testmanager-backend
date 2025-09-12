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

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for RDK Certification Service
 */
public interface IRDKCertificationService {

	/**
	 * Method to create or update or upload a config file
	 * 
	 * @param file
	 * @return boolean
	 */
	boolean createOrUploadConfigFile(MultipartFile file);

	/**
	 * Method to download a config file
	 * 
	 * @param fileName
	 * @return Resource
	 */
	Resource downloadConfigFile(String fileName);

	/**
	 * Method to get all config file names
	 * 
	 * @return List<String>
	 */
	List<String> getAllConfigFileNames();

	/**
	 * Method to get the content of the config file
	 * 
	 * @param fileName
	 * @return String
	 * 
	 */
	String getConfigFileContent(String fileName);

	/**
	 * Method to delete a config file
	 * 
	 * @param fileName
	 * @return boolean
	 */
	boolean deleteConfigFile(String fileName);

	/**
	 * Updates the configuration file with the provided multipart file.
	 *
	 * @param file the multipart file to be used for updating the configuration
	 * @return true if the configuration file was successfully updated, false
	 *         otherwise
	 */
	boolean updateConfigFile(MultipartFile file);

}
