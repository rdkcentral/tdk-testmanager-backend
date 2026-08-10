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

import java.util.ArrayList;

import lombok.Data;

/**
 * CIRequestDTO is a Data Transfer Object that represents a request for a CI
 * (Continuous Integration) service.
 * It contains information about the service, its status, start time, initiator,
 * duration, and results.
 */
@Data
public class ResultDTO {

    /**
     * The CI job ID associated with this request.
     */
    public String ciJobId;

    /**
     * The name of the file associated with this request.
     */
    public String buildFileName;

    /**
     * The status code of the CI request.
     */
    public int statusCode;

    /**
     * The current status of the CI service.
     */
    public String status;

    /**
     * A list of results from the CI service.
     */
    public ArrayList<DetailedResultDTO> result;

}
