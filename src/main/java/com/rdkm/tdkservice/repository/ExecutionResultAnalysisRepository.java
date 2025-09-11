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
package com.rdkm.tdkservice.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rdkm.tdkservice.model.ExecutionResult;
import com.rdkm.tdkservice.model.ExecutionResultAnalysis;

/**
 * Repository interface for accessing and managing ExecutionResultAnalysis
 * entities. This interface extends JpaRepository to provide CRUD operations and
 * custom query methods.
 */
@Repository
public interface ExecutionResultAnalysisRepository extends JpaRepository<ExecutionResultAnalysis, UUID> {

	/**
	 * Finds an ExecutionResultAnalysis entity by the associated ExecutionResult.
	 *
	 * @param executionResult the ExecutionResult entity to find the analysis for
	 * @return the ExecutionResultAnalysis entity associated with the given
	 *         ExecutionResult
	 */
	ExecutionResultAnalysis findByExecutionResult(ExecutionResult executionResult);

}
