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

import java.io.IOException;
import java.time.Instant;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.rdkm.tdkservice.dto.AppUpgradeResponseDTO;
import com.rdkm.tdkservice.dto.DeploymentLogsDTO;
import com.rdkm.tdkservice.dto.EntityListResponseDTO;
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
	 * Generates a JSON list of entity names that were created after the specified
	 * date.
	 * This method retrieves all entities (DeviceType, OEM, SOC, Module, Function,
	 * Parameter, PrimitiveTest, Script, TestSuite) created after the given
	 * timestamp
	 * and returns them in DTO format organized by entity type and category.
	 *
	 * @param since the Instant timestamp; only entities created after this time are
	 *              included
	 * @return EntityListResponseDTO containing categorized lists of entity names
	 */
	EntityListResponseDTO generateEntityListJsonByCreatedDate(Instant since);

	/**
	 * Generates and writes entity list JSON to a file for entities created after
	 * the specified date.
	 * This method creates a JSON file containing all entities (DeviceType, OEM,
	 * SOC, Module,
	 * Function, Parameter, PrimitiveTest, Script, TestSuite) that were created
	 * after the
	 * given timestamp, organized by entity type and category.
	 *
	 * @param since    the Instant timestamp; only entities created after this time
	 *                 are included
	 * @param filePath the path to the output JSON file
	 * @throws IOException if an I/O error occurs during file writing
	 */
	void writeEntityListJsonToFile(Instant since, String filePath) throws IOException;

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

	/**
	 * Executes the WAR generation script (clone_repo.sh) with the provided release
	 * tag
	 * and returns an execution ID for tracking
	 * 
	 * @param releaseTag the release tag to pass to the clone script
	 * @return execution ID for tracking the script execution
	 */
	String executeWarGeneration(String releaseTag);

	/**
	 * Streams WAR generation logs via Server-Sent Events
	 * 
	 * @param executionId the execution ID to stream logs for
	 * @param emitter     the SSE emitter for streaming
	 */
	void streamWarGenerationLogs(String executionId, SseEmitter emitter);

	/**
	 * Performs weekly cleanup of WAR generation metadata
	 * This method is executed every Sunday at 2 AM via scheduled task
	 */
	void weeklyCleanup();

}
