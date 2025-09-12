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
import com.rdkm.tdkservice.model.TestSuite;

/**
 * Repository interface for test suite
 */
@Repository
public interface TestSuiteRepository extends JpaRepository<TestSuite, UUID> {

	/**
	 * Check if test suite exists by name
	 * 
	 * @param name - name of the script
	 * @return boolean if script exists
	 */
	boolean existsByName(String name);

	/**
	 * Find all test suites by category
	 * 
	 * @param category - category of the test suite
	 * @return list of script
	 */
	List<TestSuite> findAllByCategory(Category category);

	/**
	 * Find test suite by name
	 * 
	 * @param testSuiteName - name of the test suite
	 * @return - test suite
	 */
	TestSuite findByName(String testSuiteName);

	/**
	 * Find all test suites by category in
	 * 
	 * @param asList - list of categories
	 * @return list of {@link com.rdkm.tdkservice.model.TestSuite}
	 */
	List<TestSuite> findAllByCategoryIn(List<Category> asList);

	/**
	 * Find all test suites created or updated after the specified dates.
	 *
	 * @param createdDate - the date to compare against for creation
	 * @param updatedAt   - the date to compare against for updates
	 * @return list of {@link com.rdkm.tdkservice.model.TestSuite}
	 */
	List<TestSuite> findByCreatedDateAfterOrUpdatedAtAfter(Instant createdDate, Instant updatedAt);

}
