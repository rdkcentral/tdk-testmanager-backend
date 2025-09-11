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

import com.rdkm.tdkservice.model.Execution;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface IExportExcelService {

	/**
	 * Generates an Excel report for the given execution.
	 *
	 * @param execution the execution for which the report is generated
	 * @return a byte array representing the generated Excel report
	 */
	public byte[] generateExcelReport(Execution execution);

	/**
	 * Retrieves the execution by its ID.
	 *
	 * @param executionId the UUID of the execution
	 * @return the execution with the specified ID
	 */
	Execution getExecutionById(UUID executionId);

	/**
	 * Generates a combined Excel report for the given list of execution IDs.
	 *
	 * @param executionIds the list of UUIDs of the executions
	 * @return a byte array representing the generated combined Excel report
	 */
	byte[] generateCombinedExcelReport(List<UUID> executionIds);

	/**
	 * Generates a raw report for the given execution ID.
	 *
	 * @param executionId the UUID of the execution
	 * @return a byte array representing the generated raw report
	 */
	byte[] generateRawReport(UUID executionId);

	/**
	 * Generates an XML report for the given execution.
	 *
	 * @param execution the execution for which the report is generated
	 * @return a byte array representing the generated XML report
	 * @throws ParserConfigurationException if a parser configuration error occurs
	 * @throws TransformerException         if a transformer error occurs
	 */
	byte[] generateXmlReport(Execution execution) throws ParserConfigurationException, TransformerException;

	/**
	 * Generates a ZIP file containing the execution results for the given execution
	 * ID.
	 *
	 * @param executionId the UUID of the execution
	 * @return a byte array representing the generated ZIP file
	 * @throws IOException if an I/O error occurs
	 */
	byte[] generateExecutionResultsZip(UUID executionId);

	/**
	 * Generates a ZIP file containing the failure scripts results for the given
	 * execution ID.
	 *
	 * @param executionId the UUID of the execution
	 * @return a byte array representing the generated ZIP file
	 * @throws IOException if an I/O error occurs
	 */
	byte[] generateExecutionFailureScriptsResultsZip(UUID executionId);

	/**
	 * Generates an Excel report comparing the specified executions.
	 *
	 * @param baseExecId   the UUID of the base execution to compare against
	 * @param executionIds a list of UUIDs of the executions to be compared
	 * @return a ByteArrayInputStream containing the generated Excel report
	 * @throws IOException if an I/O error occurs during report generation
	 */
	ByteArrayInputStream generateComparisonExcelReport(UUID baseExecId, List<UUID> executionIds);

	/**
	 * Generates an Excel report comparing the specified executions by their names.
	 *
	 * @param baseExecName       the name of the base execution to compare against
	 * @param executionNames     a list of names of the executions to be compared
	 * @return a ByteArrayInputStream containing the generated Excel report
	 */
	ByteArrayInputStream generateComparisonExcelReportByNames(String baseExecName, List<String> executionNames);
}
