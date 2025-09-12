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

package com.rdkm.tdkservice.service;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.dto.TestSuiteCreateDTO;
import com.rdkm.tdkservice.dto.TestSuiteCustomDTO;
import com.rdkm.tdkservice.dto.TestSuiteDTO;
import com.rdkm.tdkservice.dto.TestSuiteDetailsResponse;

/**
 * The ITestSuiteService interface is used to provide the services for the test
 * suites.
 */
public interface ITestSuiteService {

	/**
	 * Create the test suite.
	 * 
	 * @param testSuiteCreateDTO - the test suite create dto
	 * @return the boolean
	 */
	boolean createTestSuite(TestSuiteCreateDTO testSuiteCreateDTO);

	/**
	 * Update the test suite.
	 * 
	 * @param testSuiteCreateDTO - the test suite create dto
	 * @return the boolean - Status of update
	 */
	boolean updateTestSuite(TestSuiteDTO testSuiteDTO);

	/**
	 * Delete the test suite.
	 * 
	 * @param id - the test suite id
	 * @return
	 */
	boolean deleteTestSuite(UUID id);

	/**
	 * Find the test suite by id.
	 * 
	 * @param id - the test suite id
	 * @return the test suite dto
	 */
	TestSuiteDTO findTestSuiteById(UUID id);

	/**
	 * This method is used to find all the test suites.
	 * 
	 * @param category - the category
	 * @return the list of test suites
	 */
	List<TestSuiteDTO> findAllTestSuiteByCategory(String category);

	/**
	 * This method is used to get the test case details of each in test suite as
	 * excel
	 * 
	 * @param id - the test suite id
	 * @return the test cases in test suite as excel
	 */
	ByteArrayInputStream getTestCasesInTestSuiteAsExcel(String testSuiteName);

	/**
	 * This method is used to create the custom test suite.
	 * 
	 * @param testSuiteCustomDTO - the test suite custom dto
	 * @return the boolean - Status of custom test suite creation
	 */
	boolean createCustomTestSuite(TestSuiteCustomDTO testSuiteCustomDTO);

	/**
	 * This method is used to download the test suite as XML
	 * 
	 * @param testSuite - the test suite
	 * @return the test suite as XML
	 */
	ByteArrayInputStream downloadTestSuiteAsXML(String testSuite);

	/**
	 * This method is used to upload the test suite as XML
	 * 
	 * @param scriptFile - the script file
	 * @return the boolean - Status of upload
	 */

	boolean uploadTestSuiteAsXML(MultipartFile scriptFile);

	/**
	 * This method is used to get the test case details of each in test suite
	 *
	 * @param moduleName - the moduleName
	 * @return the scripsin test suite
	 */
	String updateTestSuiteByModuleNameAndCategory(String moduleName, String Category);

	/**
	 * This method is used to download all the test suite as XML
	 * 
	 * @param category - the category
	 * @return the test suite as XML
	 */
	ByteArrayInputStream downloadAllTestSuiteAsXML(String category);

	/**
	 *
	 * @param category
	 * @param isThunderEnabled
	 * @return
	 */
	public List<TestSuiteDetailsResponse> getListofTestSuiteNamesByCategory(String category, boolean isThunderEnabled);

	/**
	 * This method is used to get the list of all test suites with details.
	 * 
	 * @return List<TestSuiteDetailsResponse> - List of test suites with details
	 */
	List<TestSuiteDetailsResponse> getTestSuiteList();

}
