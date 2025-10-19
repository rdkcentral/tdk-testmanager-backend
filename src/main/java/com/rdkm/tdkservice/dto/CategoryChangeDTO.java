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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for category changes containing entity data organized by type and change
 * status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryChangeDTO {

    /**
     * The category of entities (RDKV, RDKB, or UNCATEGORIZED)
     */
    private String category;

    /**
     * Entity data for newly created entities in this category
     */
    private EntityDataDTO newData;

    /**
     * Entity data for updated entities in this category
     */
    private EntityDataDTO updatedData;
}