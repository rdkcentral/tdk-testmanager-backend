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

import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.enums.ExecutionOverallResultStatus;
import com.rdkm.tdkservice.enums.ExecutionProgressStatus;
import com.rdkm.tdkservice.enums.ExecutionType;
import com.rdkm.tdkservice.model.Execution;

/**
 * Repository interface for Execution entity. This interface provides methods
 * for CRUD operations on Execution entity objects and also custom methods to
 * filter
 */
@Repository
public interface ExecutionRepository extends JpaRepository<Execution, UUID> {

	/**
	 * This method is used to find the execution by name.
	 * 
	 * @param name
	 * @return Execution
	 * 
	 */
	Execution findByName(String name);

	/**
	 * This method is used to check the existence of the execution by name.
	 * 
	 * @param executionName
	 * @return boolean - true if the execution exists, false otherwise
	 */
	boolean existsByName(String executionName);

	/**
	 * This method is used to find the execution by name and category.
	 * 
	 * @param name
	 * @param category
	 * @return Execution
	 */
	Page<Execution> findByCategory(Category category, Pageable pageable);

	/**
	 * This method is used to find the execution by device name
	 * 
	 * @param deviceName - name of the device
	 * @param pageable   - pageable object
	 * @return Pagination for execution
	 */
	@Query("SELECT ed.execution FROM ExecutionDevice ed WHERE ed.device LIKE %:deviceName%")
	Page<Execution> findByDeviceName(String deviceName, Pageable pageable);

	/**
	 * This method is used to search the execution by script test suite name and
	 * category.
	 * 
	 * @param scriptTestSuite - script test suite name or part of it
	 * @param category        - category of the execution
	 * @param pageable        - pageable object
	 * @return Pagination for execution
	 */
	Page<Execution> findByscripttestSuiteNameContainingAndCategory(String scriptTestSuite, Category category,
			Pageable pageable);

	/**
	 * This method is used to search the execution by name and category.
	 * 
	 * @param scriptTestSuite - script test suite name or part of it
	 * @param category        - category of the execution
	 * @param pageable        - pageable object
	 * @return Pagination for execution
	 */
	Page<Execution> findByNameContainingAndCategory(String scriptTestSuite, Category category, Pageable pageable);

	/**
	 * This method is used to find the execution by user
	 * 
	 * @param user     - user object
	 * @param pageable - pageable object
	 * @return Pagination for execution
	 */
	Page<Execution> findByUserAndCategory(String user, Category category, Pageable pageable);

	/**
	 * Retrieves all Executions filtered by execution progress status.
	 *
	 * @param progressStatus the progress status of the execution
	 * @return a list of filtered Executions
	 */
	List<Execution> findByExecutionStatus(ExecutionProgressStatus executionProgressStats);

	/**
	 * Retrieves all Executions with the given ExecutionStatus created within the
	 * last 10 days.
	 *
	 * @param executionStatus the progress status of the execution
	 * @param tenDaysAgo      the date 10 days before the current date
	 * @param now             the current date
	 * @return a list of filtered Executions
	 */
	@Query("SELECT ex FROM Execution ex WHERE ex.executionStatus = :executionStatus AND ex.createdDate BETWEEN :start AND :now")
	List<Execution> findExecutionListInDateRange(@Param("executionStatus") ExecutionProgressStatus executionStatus,
			@Param("start") Instant start, @Param("now") Instant now);

	/**
	 * This method is used to get execution list between date ranges
	 * 
	 * @param fromDate - start {@link Date}
	 * @param toDate   - end {@link Date}
	 * @return List of {@link Execution}
	 */
	@Query("SELECT ex From  Execution ex WHERE ex.createdDate BETWEEN :fromDate AND :toDate")
	List<Execution> executionListInDateRange(Instant fromDate, Instant toDate);

	/**
	 * Retrieves a list of Executions filtered by category, date range, and result
	 * status.
	 *
	 * @param category the category of the execution
	 * @param fromDate the start date of the execution
	 * @param toDate   the end date of the execution
	 * @param results  the list of result statuses to filter by
	 * @param pageable the pagination information
	 * @return a list of filtered Executions
	 */
	@Query("SELECT ex FROM Execution ex WHERE ex.category = :category AND ex.createdDate BETWEEN :fromDate AND :toDate AND ex.result IN (:results) ORDER BY ex.createdDate DESC")
	List<Execution> getExecutionListByFilter(Category category, Instant fromDate, Instant toDate,
			List<ExecutionOverallResultStatus> results, Pageable pageable);

	/**
	 * Retrieves a list of Executions filtered by category, date range, result
	 * status, and execution type.
	 *
	 * @param category      the category of the execution
	 * @param fromDate      the start date of the execution
	 * @param toDate        the end date of the execution
	 * @param results       the list of result statuses to filter by
	 * @param executionType the type of the execution
	 * @param pageable      the pagination information
	 * @return a list of filtered Executions
	 */
	@Query("SELECT ex FROM Execution ex WHERE ex.category = :category AND ex.createdDate BETWEEN :fromDate AND :toDate AND ex.result IN (:results) AND ex.executionType = :executionType  ORDER BY ex.createdDate DESC")
	List<Execution> getExecutionListByFilterWithExecutionType(Category category, Instant fromDate, Instant toDate,
			List<ExecutionOverallResultStatus> results, ExecutionType executionType, Pageable pageable);

	/**
	 * Retrieves a list of Executions filtered by category, date range, result
	 * status, execution type, and script test suite name.
	 *
	 * @param category            the category of the execution
	 * @param fromDate            the start date of the execution
	 * @param toDate              the end date of the execution
	 * @param results             the list of result statuses to filter by
	 * @param executionType       the type of the execution
	 * @param scripttestSuiteName the name of the script test suite
	 * @param pageable            the pagination information
	 * @return a list of filtered Executions
	 */
	@Query("SELECT ex FROM Execution ex WHERE ex.category = :category AND ex.createdDate BETWEEN :fromDate AND :toDate AND ex.result IN (:results) AND ex.executionType = :executionType  AND ex.scripttestSuiteName= :scripttestSuiteName   ORDER BY ex.createdDate DESC")
	List<Execution> getExecutionListByFilterWithExecutionTypeAndSuitescript(Category category, Instant fromDate,
			Instant toDate, List<ExecutionOverallResultStatus> results, ExecutionType executionType,
			String scripttestSuiteName, Pageable pageable);

	/**
	 * Finds executions with the specified status that were created after the given
	 * date.
	 * 
	 * @param status      the execution status to filter by
	 * @param createdDate the date to compare against
	 * @return a list of matching executions
	 */
	List<Execution> findByExecutionStatusAndCreatedDateAfter(ExecutionProgressStatus status, Instant createdDate);

	/**
	 * Finds executions with the specified status that were created before the given
	 * date.
	 * 
	 * @param status      the execution status to filter by
	 * @param createdDate the date to compare against
	 * @return a list of matching executions
	 */
	List<Execution> findByExecutionStatusAndCreatedDateBefore(ExecutionProgressStatus status, Instant createdDate);

}
