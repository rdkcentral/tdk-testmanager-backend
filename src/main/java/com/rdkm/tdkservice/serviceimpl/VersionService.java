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
package com.rdkm.tdkservice.serviceimpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.service.IVersionService;

/**
 * The VersionService class handles version-related business logic.
 * It provides methods to retrieve application version information.
 */
@Service
public class VersionService implements IVersionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionService.class);
    private static final String VERSION_UNAVAILABLE = "Version Unavailable";

    @Value("${info.app.version:unknown}")
    private String applicationVersion;

    /**
     * This method is used to get the app version from application properties
     * The version is automatically populated from pom.xml during build
     * 
     * @return String - returns the app version
     */
    public String getAppVersion() {
        LOGGER.info("Getting the current app version from application properties");

        if (applicationVersion == null || "unknown".equals(applicationVersion)) {
            LOGGER.warn("Application version not available, falling back to manifest");
        }

        LOGGER.info("Successfully retrieved app version: {}", applicationVersion);
        return applicationVersion;
    }

    /**
     * This method is used to get the TDK core version from version.json file
     * 
     * @return String - returns the TDK core version or "Version Unavailable"
     */
    public String getTdkCoreVersion() {
        LOGGER.info("Getting TDK core version from version.json file");
        return getVersionFromJsonFile("version.json", "TDK core");
    }

    /**
     * This method is used to get the TDK Broadband version from
     * testscriptsRDKB/version.json file
     * 
     * @return String - returns the TDK Broadband version or "Version Unavailable"
     */
    public String getTdkBroadbandVersion() {
        LOGGER.info("Getting TDK Broadband version from testscriptsRDKB/version.json file");
        return getVersionFromJsonFile("testscriptsRDKB" + File.separator + "version.json", "TDK Broadband");
    }

    /**
     * Private method to read version from a JSON file
     * 
     * @param relativeFilePath - relative path to the version.json file from base
     *                         location
     * @param versionType      - type of version being read (for logging purposes)
     * @return String - returns the version or "Version Unavailable"
     */
    private String getVersionFromJsonFile(String relativeFilePath, String versionType) {
        try {
            String versionFilePath = AppConfig.getBaselocation() + File.separator + relativeFilePath;
            File versionFile = new File(versionFilePath);

            if (!versionFile.exists()) {
                LOGGER.warn("{} version.json file not found at: {}", versionType, versionFilePath);
                return VERSION_UNAVAILABLE;
            }

            String jsonContent = new String(Files.readAllBytes(versionFile.toPath()));
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonContent);

            JsonNode versionNode = jsonNode.get("version");
            if (versionNode == null || versionNode.isNull()) {
                LOGGER.warn("Version key not found in {} version.json file", versionType);
                return VERSION_UNAVAILABLE;
            }

            String version = versionNode.asText();
            LOGGER.info("Successfully retrieved {} version: {}", versionType, version);
            return version;

        } catch (IOException e) {
            LOGGER.error("Error reading {} version.json file", versionType, e);
            return VERSION_UNAVAILABLE;
        } catch (Exception e) {
            LOGGER.error("Unexpected error while getting {} version", versionType, e);
            return VERSION_UNAVAILABLE;
        }
    }

}