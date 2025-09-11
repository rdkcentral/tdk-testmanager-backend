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

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * This configuration class is designed to enable the asynchronous processing in
 * the application.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

	/**
	 * Creates a ThreadPoolTaskExecutor bean that is used to execute tasks
	 * asynchronously. Mainly for the execution triggers.
	 * 
	 * @return Executor instance configured with a thread pool.
	 */
	@Bean(name = "taskExecutor")
	public Executor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		executor.setCorePoolSize(50); // Start with 50 threads immediately
		executor.setMaxPoolSize(100); // Allow it to scale up to 100 threads if needed
		executor.setQueueCapacity(10); // Allow only 10 tasks to queue before new threads are created (up to
										// maxPoolSize)

		executor.setThreadNamePrefix("Automation-");
		executor.initialize();
		return executor;
	}
}
