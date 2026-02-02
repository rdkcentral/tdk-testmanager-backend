/*
* If not stated otherwise in this file or this component's LICENSE file the
* following copyright and licenses apply:
*
* Copyright 2026 RDK Management
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
package com.rdkm.tdkservice.dto;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.Data;

/**
 * Data Transfer Object representing metadata for WAR (Web Application Archive)
 * generation process.
 * This class encapsulates all the necessary information to track and monitor
 * the status
 * of a WAR file generation operation.
 */
@Data
public class WarGenerationMetadata {

    /**
     * Unique identifier for the execution instance of the WAR generation process.
     */
    private String executionId;

    /**
     * File system path to the script that performs the WAR generation.
     */
    private String scriptPath;

    /**
     * Version tag or release identifier associated with the WAR being generated.
     */
    private String releaseTag;

    /**
     * Current status of the WAR generation process (e.g., "RUNNING", "COMPLETED",
     * "FAILED").
     */
    private String status;

    /**
     * Exit code returned by the WAR generation process. Typically 0 indicates
     * success,
     * while non-zero values indicate various error conditions.
     */
    private Integer exitCode;

    /**
     * Error message or description if the WAR generation process encountered any
     * issues.
     */
    private String error;

    /**
     * Directory path where upgrade-related files are stored during the generation
     * process.
     */
    private String upgradeDir;

    /**
     * Reference to the actual system process executing the WAR generation.
     * Used for process control and monitoring.
     */
    private Process process;

    /**
     * Timestamp indicating when this WAR generation metadata instance was created.
     */
    private Instant createdAt;

    /**
     * Thread-safe list containing log output lines from the WAR generation process.
     * Uses CopyOnWriteArrayList to ensure thread safety during concurrent
     * read/write operations.
     */
    private final List<String> logLines = new CopyOnWriteArrayList<>();
}
