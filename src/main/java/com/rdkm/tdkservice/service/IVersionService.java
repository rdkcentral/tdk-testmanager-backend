/*
* If not stated otherwise in this file or this component's LICENSE file the
* following copyright and licenses apply:
*
* Copyright 2025 RDK Management
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

/**
 * The IVersionService interface defines the contract for version-related
 * operations.
 * It provides methods to retrieve different types of version information from
 * the application.
 */
public interface IVersionService {

    /**
     * This method is used to get the app version from application properties
     * The version is automatically populated from pom.xml during build
     * 
     * @return String - returns the app version
     */
    String getAppVersion();

    /**
     * This method is used to get the TDK core version from version.json file
     * 
     * @return String - returns the TDK core version or "Version Unavailable"
     */
    String getTdkCoreVersion();

    /**
     * This method is used to get the TDK Broadband version from
     * testscriptsRDKB/version.json file
     * 
     * @return String - returns the TDK Broadband version or "Version Unavailable"
     */
    String getTdkBroadbandVersion();
}