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
package com.rdkm.tdkservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdkm.tdkservice.response.DataResponse;
import com.rdkm.tdkservice.service.IVersionService;
import com.rdkm.tdkservice.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * The VersionController class handles version-related operations in the
 * application. It provides endpoints to retrieve application version
 * information.
 * 
 * The class is annotated with @RestController, which means it's a special type
 * of Controller that includes @Controller and @ResponseBody annotations. It's
 * used to handle HTTP requests and the response is automatically serialized
 * into JSON and passed back into the HttpResponse.
 */
@RestController
@CrossOrigin
@RequestMapping("/api/v1/version/")
public class VersionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionController.class);

    @Autowired
    private IVersionService versionService;

    /**
     * This method is used to get the application version
     * The version is populated from pom.xml during build process
     * 
     * @return String - the application version
     */
    @Operation(summary = "API to get the application version", description = "This API returns the application version populated from pom.xml during build")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the application version")
    @ApiResponse(responseCode = "500", description = "Internal Server Error")
    @GetMapping("/getTDKServiceVersion")
    public ResponseEntity<DataResponse> getAppVersion() {
        LOGGER.info("Inside getAppVersion method");
        String appVersion = versionService.getAppVersion();
        LOGGER.info("App version is: " + appVersion);
        if (null != appVersion) {
            LOGGER.info("App version found successfully");
            return ResponseUtils.getSuccessDataResponse("App version fetched successfully", appVersion);
        } else {
            LOGGER.error("App version not found");
            return ResponseUtils.getNotFoundDataResponse("Version Unavailable", appVersion);
        }
    }

    /**
     * This method is used to get the TDK core version from version.json file
     * 
     * @return String - the TDK core version
     */
    @Operation(summary = "API to get the TDK core version", description = "This API returns the TDK core version from version.json file")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the TDK core version")
    @ApiResponse(responseCode = "500", description = "Internal Server Error")
    @GetMapping("/getTdkCoreVersion")
    public ResponseEntity<DataResponse> getTdkCoreVersion() {
        LOGGER.info("Inside getTdkCoreVersion method");
        String tdkCoreVersion = versionService.getTdkCoreVersion();
        LOGGER.info("TDK core version is: " + tdkCoreVersion);

        if (tdkCoreVersion != null && !"Version Unavailable".equals(tdkCoreVersion)) {
            LOGGER.info("TDK core version found successfully");
            return ResponseUtils.getSuccessDataResponse("TDK core version fetched successfully", tdkCoreVersion);
        } else {
            LOGGER.error("TDK core version not found");
            return ResponseUtils.getNotFoundDataResponse("Version Unavailable", tdkCoreVersion);
        }
    }

    /**
     * This method is used to get the TDK Broadband version from
     * testscriptsRDKB/version.json file
     * 
     * @return String - the TDK Broadband version
     */
    @Operation(summary = "API to get the TDK Broadband version", description = "This API returns the TDK Broadband version from testscriptsRDKB/version.json file")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the TDK Broadband version")
    @ApiResponse(responseCode = "500", description = "Internal Server Error")
    @GetMapping("/getTdkBroadbandScriptVersion")
    public ResponseEntity<DataResponse> getTdkBroadbandVersion() {
        LOGGER.info("Inside getTdkBroadbandVersion method");
        String tdkBroadbandVersion = versionService.getTdkBroadbandVersion();
        LOGGER.info("TDK Broadband version is: " + tdkBroadbandVersion);

        if (tdkBroadbandVersion != null && !"Version Unavailable".equals(tdkBroadbandVersion)) {
            LOGGER.info("TDK Broadband version found successfully");
            return ResponseUtils.getSuccessDataResponse("TDK Broadband version fetched successfully",
                    tdkBroadbandVersion);
        } else {
            LOGGER.error("TDK Broadband version not found");
            return ResponseUtils.getNotFoundDataResponse("Version Unavailable", tdkBroadbandVersion);
        }
    }
}