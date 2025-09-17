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
package com.rdkm.tdkservice.config;

import java.util.concurrent.Executor;
import java.io.File;
import org.springframework.beans.factory.annotation.Autowired;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.util.Constants;

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

	@Autowired
	private CommonService commonService;

	/**
	 * Creates a ThreadPoolTaskExecutor bean that is used to execute tasks
	 * asynchronously. Mainly for the execution triggers.
	 * 
	 * @return Executor instance configured with a thread pool.
	 */
	@Bean(name = "taskExecutor")
	public Executor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		// Default values
		int corePoolSize = 50;
		int maxPoolSize = 100;
		int queueCapacity = 10;

		try {
			String configFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
					+ Constants.TM_CONFIG_FILE;
			File configFile = new File(configFilePath);
			String corePoolSizeStr = commonService.getConfigProperty(configFile, "threadpool.corePoolSize");
			String maxPoolSizeStr = commonService.getConfigProperty(configFile, "threadpool.maxPoolSize");
			String queueCapacityStr = commonService.getConfigProperty(configFile, "threadpool.queueCapacity");
			if (corePoolSizeStr != null)
				corePoolSize = Integer.parseInt(corePoolSizeStr);
			if (maxPoolSizeStr != null)
				maxPoolSize = Integer.parseInt(maxPoolSizeStr);
			if (queueCapacityStr != null)
				queueCapacity = Integer.parseInt(queueCapacityStr);
		} catch (Exception e) {
			System.err.println("Could not load thread pool config from tm.config, using defaults: " + e.getMessage());
		}

		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix("Automation-");
		executor.initialize();
		return executor;
	}
}
