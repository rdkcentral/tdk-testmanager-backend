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

import java.io.IOException;
import java.time.Instant;

import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.dto.AppUpgradeResponseDTO;
import com.rdkm.tdkservice.dto.DeploymentLogsDTO;
import com.rdkm.tdkservice.dto.WarUploadResponseDTO;

/**
 * Service interface for handling application upgrades This interface defines
 * methods for uploading WAR files and upgrading the application.
 */
public interface IAppUpgradeService {

	/**
	 * Generates and writes change-only SQL statements for all supported entities
	 * (DeviceType, Oem, Soc, Module, Function, Parameter, PrimitiveTest,
	 * PrimitiveTestParameter, Script, PreCondition, TestStep, ScriptDeviceType,
	 * TestSuite) to the specified file.
	 *
	 * For each entity, only records created or updated since the given timestamp
	 * ('since') are included. For entities with child/mapping tables (e.g.,
	 * TestSuite and ScriptTestSuite), the method deletes all existing mappings for
	 * changed parents and re-inserts the current mappings.
	 *
	 * This method is typically used for migration or release scenarios where only
	 * (changes since a specific time) needs to be exported.
	 * 
	 * This change SQL can be directly applied or applied with liquibase
	 *
	 * @param since    the Instant timestamp; only entities changed after this time
	 *                 are included
	 * @param filePath the path to the output SQL file
	 * @throws IOException if an I/O error occurs during file writing
	 */
	void writeAppUpgradeSqlToFile(Instant since, String filePath) throws IOException;

	/**
	 * Uploads a WAR file for application upgrade
	 * 
	 * @param uploadFile the WAR file to be uploaded
	 * @return Response containing upload status and details
	 */
	WarUploadResponseDTO uploadWar(MultipartFile uploadFile);

	/**
	 * Upgrades the application using the uploaded WAR file
	 * 
	 * @param uploadLocation Path of the uploaded WAR file
	 * @param backupLocation Optional backup location, if null default location will
	 *                       be used
	 * @return Response containing status and backup location
	 */
	AppUpgradeResponseDTO upgradeApplication(String uploadLocation, String backupLocation);

	/**
	 * Fetches the latest deployment logs from the backup folder
	 * 
	 * @return Response containing log content and details
	 */
	DeploymentLogsDTO getLatestDeploymentLogs();

}
