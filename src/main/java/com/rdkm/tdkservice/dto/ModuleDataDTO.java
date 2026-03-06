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

import java.util.List;

import lombok.Data;

/**
 * DTO for entity data containing all entity types with their lists
 */
@Data
public class ModuleDataDTO {

    /**
     * Module name
     */
    private String moduleName;

    /**
     * List of function data containing function names and parameters for this
     * module
     */
    private List<FunctionDataDTO> functionData;

    /**
     * List of primitive test names
     */
    private List<String> primitiveTestNames;

}