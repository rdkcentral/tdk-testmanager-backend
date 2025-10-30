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

package com.rdkm.tdkservice.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import liquibase.integration.spring.SpringLiquibase;

/**
 * Liquibase configuration for manual migration control
 */
@Configuration
public class LiquibaseConfig {

    @Autowired
    private DataSource dataSource;

    @Value("${spring.liquibase.change-log:classpath:db/changelogs/changelog-master.xml}")
    private String changeLog;

    @Value("${spring.liquibase.database-change-log-table:databasechangelog}")
    private String databaseChangeLogTable;

    @Value("${spring.liquibase.database-change-log-lock-table:databasechangeloglock}")
    private String databaseChangeLogLockTable;

    /**
     * Creates SpringLiquibase bean for manual execution
     * This bean is created but doesn't run automatically due to
     * spring.liquibase.enabled=false
     */
    @Bean
    public SpringLiquibase liquibase() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(changeLog);
        liquibase.setDatabaseChangeLogTable(databaseChangeLogTable);
        liquibase.setDatabaseChangeLogLockTable(databaseChangeLogLockTable);
        liquibase.setShouldRun(false); // Prevent automatic execution
        return liquibase;
    }
}
