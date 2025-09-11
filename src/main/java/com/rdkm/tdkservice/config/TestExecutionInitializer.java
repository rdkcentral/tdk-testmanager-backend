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
*
http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.rdkm.tdkservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.rdkm.tdkservice.serviceimpl.ExecutionService;

/**
 * This class is used to initialize the stopped test execution after the
 * application is fully started.
 */
@Component
public class TestExecutionInitializer {

	@Autowired
	ExecutionService executionService;

	private static final Logger LOGGER = LoggerFactory.getLogger(TestExecutionInitializer.class);

	/**
	 * Runs only once when the application is fully started. This method re-runs the
	 * stopped executions after a restart.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void onExecutionStartup() {
		LOGGER.info("Re-running stopped executions after application restart...");
		executionService.reRunTheStoppedExecutionsAfterRestart();
		LOGGER.info("Completed re-running of the executions after restart.");
	}
}
