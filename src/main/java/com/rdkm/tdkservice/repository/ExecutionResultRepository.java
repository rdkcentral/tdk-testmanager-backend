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

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rdkm.tdkservice.enums.ExecutionResultStatus;
import com.rdkm.tdkservice.model.Execution;
import com.rdkm.tdkservice.model.ExecutionResult;

/**
 * Represents the execution result repository.
 */
@Repository
public interface ExecutionResultRepository extends JpaRepository<ExecutionResult, UUID> {

	/**
	 * Find the execution result by execution.
	 * 
	 * @param execution the execution
	 * @return the list of execution result
	 */
	List<ExecutionResult> findByExecution(Execution execution);

	/**
	 * Find the top 5 execution result by script order by date of execution desc.
	 * 
	 * @param script   the script
	 * @param pageable the pageable
	 * @return the list of execution result
	 */
	@Query("SELECT er FROM ExecutionResult er WHERE er.script = :script AND er.result IN ('SUCCESS', 'FAILURE') ORDER BY er.dateOfExecution DESC")
	List<ExecutionResult> findTop5ByScriptOrderByDateOfExecutionDesc(@Param("script") String script, Pageable pageable);

	/**
	 * Find the execution result by execution and result.
	 * 
	 * @param execution the execution
	 * @param failure   the failure
	 * @return the list of execution result
	 */
	List<ExecutionResult> findByExecutionAndResult(Execution execution, ExecutionResultStatus failure);

	/**
	 * Find the execution result by execution and script.
	 *
	 * @param executionId the execution
	 * @return the list of execution result
	 */
	@Query("SELECT DISTINCT s.module.name FROM ExecutionResult er JOIN Script s ON er.script = s.name WHERE er.execution.id = :executionId")
	List<String> findDistinctModuleNamesByExecutionId(@Param("executionId") UUID executionId);

	/**
	 * Count the execution result by execution and script.
	 *
	 * @param executionId the execution ID
	 * @param moduleName  the module name
	 * @return the count of execution result
	 */
	@Query("SELECT COUNT(er) FROM ExecutionResult er JOIN Script s ON er.script = s.name WHERE er.execution.id = :executionId AND s.module.name = :moduleName")
	int countByExecutionIdAndScriptModuleName(@Param("executionId") UUID executionId, @Param("moduleName") String moduleName);

	/**
	 * Count the execution result by execution, script and result.
	 *
	 * @param executionId the execution ID
	 * @param moduleName  the module name
	 * @param result      the result
	 * @return the count of execution result
	 */
	@Query("SELECT COUNT(er) FROM ExecutionResult er JOIN Script s ON er.script = s.name WHERE er.execution.id = :executionId AND s.module.name = :moduleName AND er.result = :result")
	int countByExecutionIdAndScriptModuleNameAndResult(@Param("executionId") UUID executionId, @Param("moduleName") String moduleName, @Param("result") ExecutionResultStatus result);

}
