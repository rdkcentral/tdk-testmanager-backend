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

import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.response.PackageResponse;

/**
 * Interface for managing packages on a device.
 */
public interface IPackageManagerService {

	/**
	 * Creates a package of the specified type for the given device.
	 *
	 * @param type   the type of the package to create
	 * @param device the device for which the package is being created
	 * @return a response containing details about the created package
	 */
	PackageResponse createPackage(String type, String device);

	/**
	 * Retrieves a list of available packages for the given device.
	 *
	 * @param device the device for which to retrieve available packages
	 * @return a list of available package names
	 */
	List<String> getAvailablePackages(String type, String device);

	/**
	 * Uploads a package file to the specified device.
	 *
	 * @param uploadFile the package file to upload
	 * @param device     the device to which the package file is being uploaded
	 * @return true if the upload was successful, false otherwise
	 */
	boolean uploadPackage(String type, MultipartFile uploadFile, String device);

	/**
	 * Installs a package on the specified device.
	 *
	 * @param device      the device on which to install the package
	 * @param packageName the name of the package to install
	 * @return a string indicating the result of the installation
	 */
	PackageResponse installPackage(String type, String device, String packageName);

	/**
	 * Installs a generic package on the specified device.
	 *
	 * @param device      the device on which to install the package
	 * @param packageName the name of the package to install
	 * @return a string indicating the result of the installation
	 */
	boolean uploadGenericPackage(String type, MultipartFile uploadFile, String device);

}
