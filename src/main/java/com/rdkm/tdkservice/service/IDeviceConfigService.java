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

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface IDeviceConfigService {

	/**
	 * This method is used to get therdkv device configuration file for a given
	 * device name or device type or default device configuration file.
	 * 
	 * @param deviceTypeName - the device type name
	 * @param deviceType     - the device type
	 * @return Resource - the device configuration file
	 */
	Resource getDeviceConfigFile(String deviceTypeName, String deviceType, boolean isThunderEnabled);

	/**
	 * This method is used to upload the device configuration file
	 * 
	 * @param file - the device configuration file
	 * @return boolean - true if the device config file is uploaded successfully
	 *         false - if the device config file is not uploaded successfully
	 */
	boolean uploadDeviceConfigFile(MultipartFile file, boolean isThunderEnabled);

	/**
	 * This method is used to delete the device configuration file
	 * 
	 * @param deviceConfigFileName - the device configuration file name
	 * @return boolean - true if the device config file is deleted successfully
	 *         false - if the device config file is not deleted
	 */
	boolean deleteDeviceConfigFile(String deviceConfigFileName, boolean isThunderEnabled);

}
