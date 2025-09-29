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

package com.rdkm.tdkservice.serviceimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.rdkm.tdkservice.service.ILiquibaseService;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;

/**
 * Service implementation for Liquibase operations using SpringLiquibase
 * This approach avoids deprecated API calls by using the Spring integration
 */
@Service
public class LiquibaseService implements ILiquibaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiquibaseService.class);

    @Autowired
    private SpringLiquibase springLiquibase;

    @Value("classpath:db/data-recovery.sql")
    private Resource recoveryScript;

    /**
     * Executes pending Liquibase migrations using SpringLiquibase
     */
    @Override
    public String executeMigrations() throws LiquibaseException {
        LOGGER.info("Starting Liquibase migration execution using SpringLiquibase");

        try {
            // Use SpringLiquibase's afterPropertiesSet to trigger migration
            springLiquibase.setShouldRun(true);
            springLiquibase.afterPropertiesSet();
            springLiquibase.setShouldRun(false); // Reset to prevent automatic runs

            LOGGER.info("Liquibase migration completed successfully");
            return "Liquibase migration completed successfully";

        } catch (Exception e) {
            LOGGER.error("Error during Liquibase migration: {}", e.getMessage(), e);
            throw new LiquibaseException("Migration failed: " + e.getMessage(), e);
        }
    }

}
