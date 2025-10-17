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
package com.rdkm.tdkci;

import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

/**
 * Main entry point for the TDK CI Spring Boot application.
 * <p>
 * This class is annotated with
 * {@link org.springframework.boot.autoconfigure.SpringBootApplication}
 * to enable auto-configuration, component scanning, and configuration
 * properties support.
 * <p>
 * The {@link io.swagger.v3.oas.annotations.OpenAPIDefinition} annotation
 * provides OpenAPI metadata
 * for API documentation, including title, version, and description.
 * <p>
 */
@OpenAPIDefinition(info = @Info(title = "TDK CI", version = "1.0", description = "TDK CI APIs"))
@SpringBootApplication
public class TdkCIApplication {

	public static void main(String[] args) {
		SpringApplication.run(TdkCIApplication.class, args);
	}

}
