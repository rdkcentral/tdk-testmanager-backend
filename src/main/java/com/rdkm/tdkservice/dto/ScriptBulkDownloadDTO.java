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

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Data Transfer Object for downloading multiple scripts.
 */
@Data
public class ScriptBulkDownloadDTO {

    /**
     * The list of script names to download.
     */
    @NotEmpty(message = "Script names list cannot be empty")
    private List<String> scriptNames;

    /**
     * The format for the download (zip or tar).
     * Defaults to "zip" if not specified.
     */
    @Pattern(regexp = "^(zip|tar)$", message = "Format must be either 'zip' or 'tar'")
    private String format = "zip";
}