
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
package com.rdkm.tdkservice.dto;

import java.util.List;

import lombok.Data;

/**
 * DTO for entity data containing all entity types with their lists
 */
@Data
public class EntityDataDTO {

    /**
     * List of device type names (e.g., STB, TV, Gateway)
     */
    private List<String> deviceType;

    /**
     * List of Original Equipment Manufacturer (OEM) names
     */
    private List<String> oem;

    /**
     * List of System on Chip (SoC) names and identifiers
     */
    private List<String> soc;

    /**
     * List of test module names organized by functionality
     */
    private List<String> module;

    /**
     * List of test function names within modules
     */
    private List<String> function;

    /**
     * List of test parameter names used in test execution
     */
    private List<String> parameter;

    /**
     * List of primitive test names representing basic test operations
     */
    private List<String> primitiveTest;

    /**
     * List of test script names for automated test execution
     */
    private List<String> script;

    /**
     * List of test suite names containing grouped test scripts
     */
    private List<String> testSuite;
}