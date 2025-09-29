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
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.rdkm.tdkservice.service;

/**
 * Service interface for data recovery operations.
 * 
 */
public interface IDataRecoveryService {

    /**
     * Executes data recovery when there is a failure during Liquibase migration.
     * This method is typically called when a Liquibase migration fails to restore
     * the database to a consistent state.
     */
    void executeDataRecovery();

}