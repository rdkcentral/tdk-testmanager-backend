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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.model.ExecutionSchedule;

/**
 * Execution schedule repository interface
 */
@Repository
public interface ExecutionScheduleRepository extends JpaRepository<ExecutionSchedule, UUID> {

	/**
	 * This method is used to check the existence of the execution by name.
	 * 
	 * @param executionName
	 * @return boolean - true if the execution exists, false otherwise
	 */
	boolean existsByExecutionName(String executionName);

	/**
	 * This method is to get the list of all the executions based on category
	 * 
	 * @param category - Category say RDKV, RDKB, RDKC
	 * @return list of Execution Schedules
	 */
	List<ExecutionSchedule> findAllByCategory(Category category);

	/**
	 * This method is used to check the existence of the execution by name 
	 * and device.
	 * @param executionTime
	 * @param string
	 * @return
	 */
	boolean existsByExecutionTimeAndDevice(Instant executionTime, String string);
}