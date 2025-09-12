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

package com.rdkm.tdkservice.serviceimpl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.dto.AppUpgradeResponseDTO;
import com.rdkm.tdkservice.dto.DeploymentLogsDTO;
import com.rdkm.tdkservice.dto.WarUploadResponseDTO;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.exception.UserInputException;
import com.rdkm.tdkservice.model.DeviceType;
import com.rdkm.tdkservice.model.Function;
import com.rdkm.tdkservice.model.Module;
import com.rdkm.tdkservice.model.Oem;
import com.rdkm.tdkservice.model.Parameter;
import com.rdkm.tdkservice.model.PreCondition;
import com.rdkm.tdkservice.model.PrimitiveTest;
import com.rdkm.tdkservice.model.PrimitiveTestParameter;
import com.rdkm.tdkservice.model.Script;
import com.rdkm.tdkservice.model.ScriptTestSuite;
import com.rdkm.tdkservice.model.Soc;
import com.rdkm.tdkservice.model.TestStep;
import com.rdkm.tdkservice.model.TestSuite;
import com.rdkm.tdkservice.repository.DeviceTypeRepository;
import com.rdkm.tdkservice.repository.FunctionRepository;
import com.rdkm.tdkservice.repository.ModuleRepository;
import com.rdkm.tdkservice.repository.OemRepository;
import com.rdkm.tdkservice.repository.ParameterRepository;
import com.rdkm.tdkservice.repository.PrimitiveTestParameterRepository;
import com.rdkm.tdkservice.repository.PrimitiveTestRepository;
import com.rdkm.tdkservice.repository.ScriptRepository;
import com.rdkm.tdkservice.repository.SocRepository;
import com.rdkm.tdkservice.repository.TestSuiteRepository;
import com.rdkm.tdkservice.service.IAppUpgradeService;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.service.utilservices.ScriptExecutorService;
import com.rdkm.tdkservice.util.Constants;

/**
 * Service for implementing the methods related to app upgrade and data
 * migration
 */
@Service
public class AppUpgradeService implements IAppUpgradeService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AppUpgradeService.class);
	/**
	 * DeviceTypeRepository bean for accessing device type data.
	 */
	@Autowired
	private DeviceTypeRepository deviceTypeRepository;

	@Autowired
	private OemRepository oemRepository;

	@Autowired
	private SocRepository socRepository;

	@Autowired
	private ModuleRepository moduleRepository;

	@Autowired
	private FunctionRepository functionRepository;

	@Autowired
	private ParameterRepository parameterRepository;

	@Autowired
	private PrimitiveTestRepository primitiveTestRepository;

	@Autowired
	private PrimitiveTestParameterRepository primitiveTestParameterRepository;

	@Autowired
	private ScriptRepository scriptRepository;

	@Autowired
	private TestSuiteRepository testSuiteRepository;

	@Autowired
	private CommonService commonService;

	@Autowired
	private ScriptExecutorService scriptExecutorService;

	DateTimeFormatter SQL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
			.withZone(ZoneId.of("UTC"));

	/**
	 * Generates and writes change-only SQL statements for all supported entities
	 * (DeviceType, Oem, Soc, Module, Function, Parameter, PrimitiveTest,
	 * PrimitiveTestParameter, Script, PreCondition, TestStep, ScriptDeviceType,
	 * TestSuite) to the specified file.
	 *
	 * For each entity, only records created or updated since the given timestamp
	 * ('since') are included. For entities with child/mapping tables (e.g.,
	 * TestSuite and ScriptTestSuite), the method deletes all existing mappings for
	 * changed parents and re-inserts the current mappings.
	 *
	 * This method is typically used for migration or release scenarios where only
	 * (changes since a specific time) needs to be exported.
	 * 
	 * This change SQL can be directly applied or applied with liquibase
	 *
	 * @param since    the Instant timestamp; only entities changed after this time
	 *                 are included
	 * @param filePath the path to the output SQL file
	 * @throws IOException if an I/O error occurs during file writing
	 */
	public void writeAppUpgradeSqlToFile(Instant since, String filePath) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
			writeDeviceTypeSql(writer, since);
			writeOemSql(writer, since);
			writeSocSql(writer, since);
			writeModuleSql(writer, since);
			writeFunctionSql(writer, since);
			writeParameterSql(writer, since);
			writePrimitiveTestSql(writer, since);
			writePrimitiveTestParameterSql(writer, since);
			writeScriptSql(writer, since);
			writePreConditionSql(writer, since);
			writeTestStepSql(writer, since);
			writeScriptDeviceTypeSql(writer, since);
			writeTestSuiteSql(writer, since);
		} catch (IOException e) {
			LOGGER.error("Error writing app upgrade SQL to file", e);
			throw new TDKServiceException("Failed to write app upgrade SQL: " + e.getMessage());
		}
	}

	/**
	 * Writes SQL statements for PrimitiveTest changes to the specified file.
	 *
	 * @param writer the BufferedWriter to write SQL statements to
	 * @param since  the Instant timestamp to filter changes
	 * @throws IOException if an I/O error occurs
	 */
	private void writePrimitiveTestSql(BufferedWriter writer, Instant since) throws IOException {
		List<PrimitiveTest> modifiedPrimitiveTests = primitiveTestRepository
				.findByCreatedDateAfterOrUpdatedAtAfter(since, since);
		for (PrimitiveTest modifiedPrimitiveTest : modifiedPrimitiveTests) {
			String id = String.format("'%s'", modifiedPrimitiveTest.getId().toString());
			String moduleId = modifiedPrimitiveTest.getModule() != null
					? String.format("'%s'", modifiedPrimitiveTest.getModule().getId().toString())
					: "NULL";
			String functionId = modifiedPrimitiveTest.getFunction() != null
					? String.format("'%s'", modifiedPrimitiveTest.getFunction().getId().toString())
					: "NULL";
			String userGroupId = modifiedPrimitiveTest.getUserGroup() != null
					? String.format("'%s'", modifiedPrimitiveTest.getUserGroup().getId().toString())
					: "NULL";
			String name = modifiedPrimitiveTest.getName() != null ? modifiedPrimitiveTest.getName().replace("'", "''")
					: "";
			if (modifiedPrimitiveTest.getCreatedDate().isAfter(since)) {
				writer.write(String.format(
						"INSERT INTO primitive_test (id, name, module_id, function_id, user_group_id , created_date, updated_at) VALUES (%s, '%s', %s, %s, %s, '%s', '%s');\n",
						id, name, moduleId, functionId, userGroupId,
						SQL_DATE_FORMAT.format(modifiedPrimitiveTest.getCreatedDate()),
						SQL_DATE_FORMAT.format(modifiedPrimitiveTest.getUpdatedAt())));
			} else if (modifiedPrimitiveTest.getUpdatedAt().isAfter(since)) {
				writer.write(String.format(
						"UPDATE primitive_test SET name = '%s', module_id = %s, function_id = %s, user_group_id  = %s, updated_at = '%s' WHERE id = %s;\n",
						name, moduleId, functionId, userGroupId,
						SQL_DATE_FORMAT.format(modifiedPrimitiveTest.getUpdatedAt()), id));
			}
		}
	}

	/**
	 * Writes SQL statements for PrimitiveTestParameter changes to the specified
	 * file.
	 *
	 * @param writer the BufferedWriter to write SQL statements to
	 * @param since  the Instant timestamp to filter changes
	 * @throws IOException if an I/O error occurs
	 */
	private void writePrimitiveTestParameterSql(BufferedWriter writer, Instant since) throws IOException {
		List<PrimitiveTestParameter> params = primitiveTestParameterRepository
				.findByCreatedDateAfterOrUpdatedAtAfter(since, since);
		for (PrimitiveTestParameter param : params) {
			String id = String.format("'%s'", param.getId().toString());
			String primitiveTestId = param.getPrimitiveTest() != null
					? String.format("'%s'", param.getPrimitiveTest().getId().toString())
					: "NULL";
			String parameterName = param.getParameterName() != null ? param.getParameterName().replace("'", "''") : "";
			String parameterType = param.getParameterType() != null ? param.getParameterType().replace("'", "''") : "";
			String parameterRange = param.getParameterRange() != null ? param.getParameterRange().replace("'", "''")
					: "";
			String parameterValue = param.getParameterValue() != null ? param.getParameterValue().replace("'", "''")
					: "";
			if (param.getCreatedDate().isAfter(since)) {
				writer.write(String.format(
						"INSERT INTO primitive_test_parameter (id, primitivetest_id, parametername, parametertype, parameterrange, parametervalue, created_date, updated_at) VALUES (%s, %s, '%s', '%s', '%s', '%s', '%s', '%s');\n",
						id, primitiveTestId, parameterName, parameterType, parameterRange, parameterValue,
						SQL_DATE_FORMAT.format(param.getCreatedDate()), SQL_DATE_FORMAT.format(param.getUpdatedAt())));
			} else if (param.getUpdatedAt().isAfter(since)) {
				writer.write(String.format(
						"UPDATE primitive_test_parameter SET primitivetest_id = %s, parametername = '%s', parametertype = '%s', parameterrange = '%s', parametervalue = '%s', updated_at = '%s' WHERE id = %s;\n",
						primitiveTestId, parameterName, parameterType, parameterRange, parameterValue,
						SQL_DATE_FORMAT.format(param.getUpdatedAt()), id));
			}
		}
	}

	/**
	 * Writes SQL statements for device type changes to the specified file.
	 * 
	 * @param writer the BufferedWriter to write SQL statements to
	 * @param since  the Instant timestamp to filter changes
	 * @throws IOException if an I/O error occurs
	 */
	private void writeDeviceTypeSql(BufferedWriter writer, Instant since) throws IOException {
		List<DeviceType> deviceTypes = deviceTypeRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);
		for (DeviceType dt : deviceTypes) {
			String id = String.format("'%s'", dt.getId().toString());
			String userGroupId = dt.getUserGroup() != null ? String.format("'%s'", dt.getUserGroup().getId().toString())
					: "NULL";
			if (dt.getCreatedDate().isAfter(since)) {
				writer.write(String.format(
						"INSERT INTO device_type (id, name, type, user_group_id, category, created_date, updated_at) VALUES (%s, '%s', '%s', %s, '%s', '%s', '%s');\n",
						id, dt.getName(), dt.getType(), userGroupId, dt.getCategory(),
						SQL_DATE_FORMAT.format(dt.getCreatedDate()), SQL_DATE_FORMAT.format(dt.getUpdatedAt())));
			} else if (dt.getUpdatedAt().isAfter(since)) {
				writer.write(String.format(
						"UPDATE device_type SET type = '%s', user_group_id = %s, updated_at = '%s' WHERE id = %s;\n",
						dt.getType(), userGroupId, SQL_DATE_FORMAT.format(dt.getUpdatedAt()), id));
			}
		}
	}

	/**
	 * Writes SQL statements for OEM changes to the specified file.
	 * 
	 * @param writer the BufferedWriter to write SQL statements to
	 * @param since  the Instant timestamp to filter changes
	 * @throws IOException if an I/O error occurs
	 */
	private void writeOemSql(BufferedWriter writer, Instant since) throws IOException {
		List<Oem> oems = oemRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);
		for (Oem oem : oems) {
			String id = String.format("'%s'", oem.getId().toString());
			String userGroupId = oem.getUserGroup() != null
					? String.format("'%s'", oem.getUserGroup().getId().toString())
					: "NULL";
			if (oem.getCreatedDate().isAfter(since)) {
				writer.write(String.format(
						"INSERT INTO oem (id, name, user_group_id, category, created_date, updated_at) VALUES (%s, '%s', %s, '%s', '%s', '%s');\n",
						id, oem.getName(), userGroupId, oem.getCategory(), SQL_DATE_FORMAT.format(oem.getCreatedDate()),
						SQL_DATE_FORMAT.format(oem.getUpdatedAt())));
			} else if (oem.getUpdatedAt().isAfter(since)) {
				writer.write(String.format(
						"UPDATE oem SET user_group_id = %s, category = '%s', updated_at = '%s' WHERE id = %s;\n",
						userGroupId, oem.getCategory(), SQL_DATE_FORMAT.format(oem.getUpdatedAt()), id));
			}
		}
	}

	/**
	 * Writes SQL statements for SoC changes to the specified file.
	 * 
	 * @param writer the BufferedWriter to write SQL statements to
	 * @param since  the Instant timestamp to filter changes
	 * @throws IOException if an I/O error occurs
	 */
	private void writeSocSql(BufferedWriter writer, Instant since) throws IOException {
		List<Soc> socs = socRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);
		for (Soc soc : socs) {
			String id = String.format("'%s'", soc.getId().toString());
			String userGroupId = soc.getUserGroup() != null
					? String.format("'%s'", soc.getUserGroup().getId().toString())
					: "NULL";
			if (soc.getCreatedDate().isAfter(since)) {
				writer.write(String.format(
						"INSERT INTO soc (id, name, user_group_id, category, created_date, updated_at) VALUES (%s, '%s', %s, '%s', '%s', '%s');\n",
						id, soc.getName(), userGroupId, soc.getCategory(), SQL_DATE_FORMAT.format(soc.getCreatedDate()),
						SQL_DATE_FORMAT.format(soc.getUpdatedAt())));
			} else if (soc.getUpdatedAt().isAfter(since)) {
				writer.write(String.format(
						"UPDATE soc SET user_group_id = %s, category = '%s', updated_at = '%s' WHERE id = %s;\n",
						userGroupId, soc.getCategory(), SQL_DATE_FORMAT.format(soc.getUpdatedAt()), id));
			}
		}
	}

	/**
	 * Writes SQL statements for Module changes to the specified file.
	 *
	 * @param writer the BufferedWriter to write SQL statements to
	 * @param since  the Instant timestamp to filter changes
	 * @throws IOException if an I/O error occurs
	 */
	private void writeModuleSql(BufferedWriter writer, Instant since) throws IOException {
		List<Module> modules = moduleRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);
		for (Module module : modules) {
			String id = String.format("'%s'", module.getId().toString());
			String userGroupId = module.getUserGroup() != null
					? String.format("'%s'", module.getUserGroup().getId().toString())
					: "NULL";
			String testGroup = module.getTestGroup() != null ? module.getTestGroup().name() : "NULL";
			String category = module.getCategory() != null ? module.getCategory().name() : "NULL";
			boolean isInsert = module.getCreatedDate().isAfter(since);
			if (isInsert) {
				writer.write(String.format(
						"INSERT INTO module (id, name, test_group, execution_time, user_group_id, category, created_date, updated_at) VALUES (%s, '%s', '%s', %s, %s, '%s', '%s', '%s');\n",
						id, module.getName(), testGroup,
						module.getExecutionTime() != null ? module.getExecutionTime().toString() : "NULL", userGroupId,
						category, SQL_DATE_FORMAT.format(module.getCreatedDate()),
						SQL_DATE_FORMAT.format(module.getUpdatedAt())));
			} else if (module.getUpdatedAt().isAfter(since)) {
				writer.write(String.format(
						"UPDATE module SET name = '%s', test_group = '%s', execution_time = %s, user_group_id = %s, category = '%s', updated_at = '%s' WHERE id = %s;\n",
						module.getName(), testGroup,
						module.getExecutionTime() != null ? module.getExecutionTime().toString() : "NULL", userGroupId,
						category, SQL_DATE_FORMAT.format(module.getUpdatedAt()), id));
				// Delete all log/crash paths for this module before re-inserting
				writer.write(String.format("DELETE FROM module_log_file_paths WHERE module_id = %s;\n", id));
				writer.write(String.format("DELETE FROM module_crash_log_paths WHERE module_id = %s;\n", id));
			}
			// Insert log file paths
			if (module.getLogFileNames() != null) {
				for (String logPath : module.getLogFileNames()) {
					writer.write(String.format(
							"INSERT INTO module_log_file_paths (module_id, log_file_path) VALUES (%s, '%s');\n", id,
							logPath.replace("'", "''")));
				}
			}
			// Insert crash log file paths
			if (module.getCrashLogFiles() != null) {
				for (String crashPath : module.getCrashLogFiles()) {
					writer.write(String.format(
							"INSERT INTO module_crash_log_paths (module_id, crash_log_path) VALUES (%s, '%s');\n", id,
							crashPath.replace("'", "''")));
				}
			}
		}
	}

	/**
	 * Writes SQL statements for Function changes to the specified file.
	 *
	 * @param writer the BufferedWriter to write SQL statements to
	 * @param since  the Instant timestamp to filter changes
	 * @throws IOException if an I/O error occurs
	 */
	private void writeFunctionSql(BufferedWriter writer, Instant since) throws IOException {
		List<Function> functions = functionRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);
		for (Function function : functions) {
			String id = String.format("'%s'", function.getId().toString());
			String moduleId = function.getModule() != null
					? String.format("'%s'", function.getModule().getId().toString())
					: "NULL";
			String category = function.getCategory() != null ? function.getCategory().name() : "NULL";
			if (function.getCreatedDate().isAfter(since)) {
				writer.write(String.format(
						"INSERT INTO functions (id, name, module_id, category, created_date, updated_at) VALUES (%s, '%s', %s, '%s', '%s', '%s');\n",
						id, function.getName(), moduleId, category, SQL_DATE_FORMAT.format(function.getCreatedDate()),
						SQL_DATE_FORMAT.format(function.getUpdatedAt())));
			} else if (function.getUpdatedAt().isAfter(since)) {
				writer.write(String.format(
						"UPDATE functions SET name = '%s', module_id = %s, category = '%s', updated_at = '%s' WHERE id = %s;\n",
						function.getName(), moduleId, category, SQL_DATE_FORMAT.format(function.getUpdatedAt()), id));
			}
		}
	}

	/**
	 * Writes SQL statements for Parameter changes to the specified file.
	 *
	 * @param writer the BufferedWriter to write SQL statements to
	 * @param since  the Instant timestamp to filter changes
	 * @throws IOException if an I/O error occurs
	 */
	private void writeParameterSql(BufferedWriter writer, Instant since) throws IOException {
		List<Parameter> parameters = parameterRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);
		for (Parameter parameter : parameters) {
			String id = String.format("'%s'", parameter.getId().toString());
			String functionId = parameter.getFunction() != null
					? String.format("'%s'", parameter.getFunction().getId().toString())
					: "NULL";
			String name = parameter.getName() != null ? parameter.getName().replace("'", "''") : "";
			String dataType = parameter.getParameterDataType() != null ? parameter.getParameterDataType().name()
					: "NULL";
			String rangeVal = parameter.getRangeVal() != null ? parameter.getRangeVal().replace("'", "''") : "";
			if (parameter.getCreatedDate().isAfter(since)) {
				writer.write(String.format(
						"INSERT INTO parameter (id, name, parameter_data_type, range_val, function_id, created_date, updated_at) VALUES (%s, '%s', '%s', '%s', %s, '%s', '%s');\n",
						id, name, dataType, rangeVal, functionId, SQL_DATE_FORMAT.format(parameter.getCreatedDate()),
						SQL_DATE_FORMAT.format(parameter.getUpdatedAt())));
			} else if (parameter.getUpdatedAt().isAfter(since)) {
				writer.write(String.format(
						"UPDATE parameter SET name = '%s', parameter_data_type = '%s', range_val = '%s', function_id = %s, updated_at = '%s' WHERE id = %s;\n",
						name, dataType, rangeVal, functionId, SQL_DATE_FORMAT.format(parameter.getUpdatedAt()), id));
			}
		}
	}

	/**
	 * Writes change-only SQL statements for Script entities to the specified file.
	 *
	 * For each Script, only records created or updated since the given timestamp
	 * ('since') are included. For new scripts, an INSERT statement is generated.
	 * For updated scripts, an UPDATE statement is generated.
	 *
	 * All relevant fields are included in the SQL, including created_date and
	 * updated_at, formatted as 'yyyy-MM-dd HH:mm:ss.SSSSSS' in UTC.
	 *
	 * This method is typically used for migration or upgrade scenarios where only
	 * the delta (changes since a specific time) needs to be exported.
	 *
	 * @param writer the BufferedWriter to write SQL statements to
	 * @param since  the Instant timestamp; only entities changed after this time
	 *               are included
	 * @throws IOException if an I/O error occurs during file writing
	 */
	private void writeScriptSql(BufferedWriter writer, Instant since) throws IOException {
		List<Script> modifiedScripts = scriptRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);
		for (Script script : modifiedScripts) {
			String id = String.format("'%s'", script.getId().toString());
			String name = script.getName() != null ? script.getName().replace("'", "''") : "";
			String synopsis = script.getSynopsis() != null ? script.getSynopsis().replace("'", "''") : "";
			String testId = script.getTestId() != null ? script.getTestId().replace("'", "''") : "";
			String objective = script.getObjective() != null ? script.getObjective().replace("'", "''") : "";
			String priority = script.getPriority() != null ? script.getPriority().replace("'", "''") : "";
			String releaseVersion = script.getReleaseVersion() != null ? script.getReleaseVersion().replace("'", "''")
					: "";
			String scriptLocation = script.getScriptLocation() != null ? script.getScriptLocation().replace("'", "''")
					: "";
			String skipRemarks = script.getSkipRemarks() != null ? script.getSkipRemarks().replace("'", "''") : "";
			String category = script.getCategory() != null ? script.getCategory().name() : "NULL";
			String primitiveTestId = script.getPrimitiveTest() != null
					? String.format("'%s'", script.getPrimitiveTest().getId().toString())
					: "NULL";
			String moduleId = script.getModule() != null ? String.format("'%s'", script.getModule().getId().toString())
					: "NULL";
			String userGroupId = script.getUserGroup() != null
					? String.format("'%s'", script.getUserGroup().getId().toString())
					: "NULL";
			int executionTimeOut = script.getExecutionTimeOut();
			boolean isLongDuration = script.isLongDuration();
			boolean skipExecution = script.isSkipExecution();
			if (script.getCreatedDate().isAfter(since)) {
				writer.write(String.format(
						"INSERT INTO script (id, name, synopsis, execution_time_out, is_long_duration, category, primitive_test_id, module_id, user_group_id, skip_execution, skip_remarks, script_location, test_id, objective, priority, release_version, created_date, updated_at) VALUES (%s, '%s', '%s', %d, %b, '%s', %s, %s, %s, %b, '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');\n",
						id, name, synopsis, executionTimeOut, isLongDuration, category, primitiveTestId, moduleId,
						userGroupId, skipExecution, skipRemarks, scriptLocation, testId, objective, priority,
						releaseVersion, SQL_DATE_FORMAT.format(script.getCreatedDate()),
						SQL_DATE_FORMAT.format(script.getUpdatedAt())));
			} else if (script.getUpdatedAt().isAfter(since)) {
				writer.write(String.format(
						"UPDATE script SET name = '%s', synopsis = '%s', execution_time_out = %d, is_long_duration = %b, category = '%s', primitive_test_id = %s, module_id = %s, user_group_id = %s, skip_execution = %b, skip_remarks = '%s', script_location = '%s', test_id = '%s', objective = '%s', priority = '%s', release_version = '%s', updated_at = '%s' WHERE id = %s;\n",
						name, synopsis, executionTimeOut, isLongDuration, category, primitiveTestId, moduleId,
						userGroupId, skipExecution, skipRemarks, scriptLocation, testId, objective, priority,
						releaseVersion, SQL_DATE_FORMAT.format(script.getUpdatedAt()), id));
			}
		}

	}

	/**
	 * Writes SQL statements for PreCondition changes to the specified file.
	 *
	 * Logic: Find scripts with any PreCondition changed since 'since'. This will
	 * include both new scripts and existing scripts with changed preconditions. If
	 * we update the preconditions, we need to delete the old ones and insert the
	 * new ones. We can't just update them like module table because the
	 * preconditions can change completely; a precondition can be removed and a new
	 * one can be added, so we need to delete all preconditions for the script and
	 * insert the new ones.
	 *
	 * @param writer the BufferedWriter to write SQL statements to
	 * @param since  the Instant timestamp to filter changes
	 * @throws IOException if an I/O error occurs
	 */
	private void writePreConditionSql(BufferedWriter writer, Instant since) throws IOException {

		// Find scripts with preconditions changed since 'since' either created or
		// updated
		List<Script> scriptsWithChangedPreConditions = scriptRepository.findScriptsWithPreConditionChangedSince(since);
		for (Script script : scriptsWithChangedPreConditions) {
			System.out.println("Processing script: " + script.getName() + " with ID: " + script.getId());
			String scriptId = String.format("'%s'", script.getId().toString());
			// Delete all preconditions for this script
			writer.write(String.format("DELETE FROM pre_condition WHERE script_id = %s;\n", scriptId));
			// Insert all current preconditions for this script
			for (PreCondition pc : script.getPreConditions()) {
				String id = String.format("'%s'", pc.getId().toString());
				String preConditionDescription = pc.getPreConditionDescription() != null
						? pc.getPreConditionDescription().replace("'", "''")
						: "";
				writer.write(String.format(
						"INSERT INTO pre_condition (id, pre_condition_description, script_id, created_date, updated_at) VALUES (%s, '%s', %s, '%s', '%s');\n",
						id, preConditionDescription, scriptId, SQL_DATE_FORMAT.format(pc.getCreatedDate()),
						SQL_DATE_FORMAT.format(pc.getUpdatedAt())));
			}
		}
	}

	/**
	 * Writes SQL statements for TestStep changes to the specified file.
	 *
	 * Logic: Find scripts with any TestStep changed since 'since'. This will
	 * include both new scripts and existing scripts with changed test steps. If we
	 * update the test steps, we need to delete the old ones and insert the new
	 * ones. We can't just update them because the test steps can change completely;
	 * a test step can be removed and a new one can be added, so we need to delete
	 * all test steps for the script and insert the new ones.
	 *
	 * @param writer the BufferedWriter to write SQL statements to
	 * @param since  the Instant timestamp to filter changes
	 * @throws IOException if an I/O error occurs
	 */
	private void writeTestStepSql(BufferedWriter writer, Instant since) throws IOException {
		// Find scripts with test steps changed since 'since' either created or updated
		List<Script> scriptsWithChangedTestSteps = scriptRepository.findScriptsWithTestStepChangedSince(since);
		for (Script script : scriptsWithChangedTestSteps) {
			String scriptId = String.format("'%s'", script.getId().toString());
			// Delete all test steps for this script
			writer.write(String.format("DELETE FROM test_step WHERE script_id = %s;\n", scriptId));
			// Insert all current test steps for this script
			for (TestStep ts : script.getTestSteps()) {
				String id = String.format("'%s'", ts.getId().toString());
				String stepName = ts.getStepName() != null ? ts.getStepName().replace("'", "''") : "";
				String stepDescription = ts.getStepDescription() != null ? ts.getStepDescription().replace("'", "''")
						: "";
				String expectedResult = ts.getExpectedResult() != null ? ts.getExpectedResult().replace("'", "''") : "";
				writer.write(String.format(
						"INSERT INTO test_step (id, step_name, step_description, expected_result, script_id, created_date, updated_at) VALUES (%s, '%s', '%s', '%s', %s, '%s', '%s');\n",
						id, stepName, stepDescription, expectedResult, scriptId,
						SQL_DATE_FORMAT.format(ts.getCreatedDate()), SQL_DATE_FORMAT.format(ts.getUpdatedAt())));
			}
		}
	}

	/**
	 * Writes SQL statements for ScriptDeviceType changes to the specified file.
	 * 
	 * Logic : Find the scripts that have been created or updated since 'since'. For
	 * each script, delete all existing device type associations. Then, insert the
	 * current device type associations for that script. This is necessary because
	 * device type associations can change completely (e.g., a device type can be
	 * removed and a new one can be added), so we need to delete all existing
	 * associations and insert the new ones.
	 * 
	 * @param writer
	 * @param since
	 * @throws IOException
	 */
	private void writeScriptDeviceTypeSql(BufferedWriter writer, Instant since) throws IOException {
		// Find scripts changed since 'since' (created or updated)
		List<Script> changedScripts = scriptRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);
		for (Script script : changedScripts) {
			String scriptId = String.format("'%s'", script.getId().toString());
			// Delete all device type associations for this script
			writer.write(String.format("DELETE FROM script_device_type WHERE script_id = %s;\n", scriptId));
			// Insert all current device type associations for this script
			for (DeviceType dt : script.getDeviceTypes()) {
				String deviceTypeId = String.format("'%s'", dt.getId().toString());
				writer.write(
						String.format("INSERT INTO script_device_type (script_id, device_type_id) VALUES (%s, %s);\n",
								scriptId, deviceTypeId));
			}
		}
	}

	/**
	 * Writes change-only SQL statements for TestSuite entities and their
	 * ScriptTestSuite mappings to the specified file. Logic : For each TestSuite,
	 * only records created or updated since the given timestamp ('since') are
	 * included. For new TestSuites, an INSERT statement is generated. For updated
	 * TestSuites an UPDATE statement is generated. In addition, all existing
	 * ScriptTestSuite mappings for the TestSuite are deleted, and the current
	 * mappings are re-inserted for changed TestSuites both created and updated.
	 */
	private void writeTestSuiteSql(BufferedWriter writer, Instant since) throws IOException {
		List<TestSuite> testSuites = testSuiteRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);
		// Remove all TestSuites whose name matches any existing Module name
		List<String> moduleNames = moduleRepository.findAll().stream()
				.map(m -> m.getName())
				.filter(java.util.Objects::nonNull)
				.toList();
		testSuites = testSuites.stream()
				.filter(ts -> ts.getName() == null || !moduleNames.contains(ts.getName()))
				.toList();
		for (TestSuite ts : testSuites) {
			String id = String.format("'%s'", ts.getId().toString());
			String name = ts.getName() != null ? ts.getName().replace("'", "''") : "";
			String description = ts.getDescription() != null ? ts.getDescription().replace("'", "''") : "";
			String category = ts.getCategory() != null ? ts.getCategory().name() : "NULL";
			String userGroupId = ts.getUserGroup() != null ? String.format("'%s'", ts.getUserGroup().getId().toString())
					: "NULL";
			if (ts.getCreatedDate().isAfter(since)) {
				writer.write(String.format(
						"INSERT INTO test_suite (id, name, description, category, user_group_id, created_date, updated_at) VALUES (%s, '%s', '%s', '%s', %s, '%s', '%s');\n",
						id, name, description, category, userGroupId, SQL_DATE_FORMAT.format(ts.getCreatedDate()),
						SQL_DATE_FORMAT.format(ts.getUpdatedAt())));
			} else if (ts.getUpdatedAt().isAfter(since)) {
				writer.write(String.format(
						"UPDATE test_suite SET name = '%s', description = '%s', category = '%s', user_group_id = %s, updated_at = '%s' WHERE id = %s;\n",
						name, description, category, userGroupId, SQL_DATE_FORMAT.format(ts.getUpdatedAt()), id));
			}

			// Always delete and re-insert ScriptTestSuite mappings for changed TestSuite
			writer.write(String.format("DELETE FROM script_test_suite WHERE test_suite_id = %s;\n", id));
			List<ScriptTestSuite> mappings = ts.getScriptTestSuite();
			for (ScriptTestSuite sts : mappings) {
				String scriptId = String.format("'%s'", sts.getScript().getId().toString());
				int scriptOrder = sts.getScriptOrder();
				String stsId = String.format("'%s'", sts.getId().toString());
				writer.write(String.format(
						"INSERT INTO script_test_suite (id, script_id, test_suite_id, script_order, created_date, updated_at) VALUES (%s, %s, %s, %d, '%s', '%s');\n",
						stsId, scriptId, id, scriptOrder, SQL_DATE_FORMAT.format(sts.getCreatedDate()),
						SQL_DATE_FORMAT.format(sts.getUpdatedAt())));
			}
		}
	}

	/**
	 * Uploads a WAR file for application upgrade
	 * 
	 * @param uploadFile the WAR file to be uploaded
	 * @return Response containing upload status and details
	 */
	@Override
	public WarUploadResponseDTO uploadWar(MultipartFile uploadFile) {
		LOGGER.info("Inside Service layer. Going to Upload the backend war file.");
		String fileName = uploadFile.getOriginalFilename();
		this.validateWarFile(fileName);

		try {
			// Read the upgrade file location from tm.config
			String tmConfigFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
					+ Constants.TM_CONFIG_FILE;
			String baseUpgradeLocation = commonService.getConfigProperty(new File(tmConfigFilePath),
					Constants.UPGRADE_FILE_LOCATION);
			if (baseUpgradeLocation == null || baseUpgradeLocation.isEmpty()) {
				LOGGER.error("Unable to read upgrade.fileLocation from tm.config");
				throw new TDKServiceException("Unable to read upgrade file location from config");
			} // Create directory with timestamp
			String timestampDirName = "NewRelease_" + new SimpleDateFormat("yy_MM_dd_HH_mm_ss").format(new Date());
			String targetDir = baseUpgradeLocation + Constants.FILE_PATH_SEPERATOR + timestampDirName;

			// Create directory if it doesn't exist
			Path dirPath = Paths.get(targetDir);
			Files.createDirectories(dirPath);

			// Rename to tdkservice.war if it's a .war file and not already named
			// tdkservice.war
			String targetFileName = fileName;
			if (fileName != null && fileName.toLowerCase().endsWith(".war")
					&& !fileName.equalsIgnoreCase("tdkservice.war")) {
				targetFileName = "tdkservice.war";
				LOGGER.info("Renaming war file from {} to {}", fileName, targetFileName);
			}

			// Copy the file to the target directory with the new name
			Path targetPath = Paths.get(targetDir, targetFileName);
			Files.copy(uploadFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

			LOGGER.info("File uploaded successfully to {}", targetPath);

			// Create and return the response
			WarUploadResponseDTO response = new WarUploadResponseDTO();
			response.setSatusCode(200);
			response.setFileLocation(targetPath.getParent().toString());
			return response;

		} catch (Exception e) {
			LOGGER.error("Error while uploading war file", e);
			throw new TDKServiceException("Error while uploading War file: " + e.getMessage());
		}
	}

	/**
	 * Validates the uploaded WAR file name
	 * 
	 * @param uploadFile the name of the uploaded file
	 * @throws UserInputException if the file name is invalid
	 */
	private void validateWarFile(String uploadFile) {
		if (uploadFile == null || uploadFile.isEmpty()) {
			LOGGER.error("Invalid file name");
			throw new UserInputException("File name cannot be empty");
		}
		if (!uploadFile.toLowerCase().endsWith(".war")) {
			LOGGER.error("Invalid file type. Only .war files are allowed");
			throw new UserInputException("Only .war files are allowed");
		}
	}

	/**
	 * Upgrades the application using the uploaded WAR file
	 * 
	 * @param uploadLocation Path of the uploaded WAR file
	 * @param backupLocation Optional backup location, if null default location will
	 *                       be used
	 * @return Response containing status and backup location
	 */
	@Override
	public AppUpgradeResponseDTO upgradeApplication(String uploadLocation, String backupLocation) {
		LOGGER.info("Inside Service layer. Going to upgrade the application");
		LOGGER.info("Upload location: {}, Backup location: {}", uploadLocation, backupLocation);

		if (uploadLocation == null || uploadLocation.isEmpty()) {
			LOGGER.error("Upload location cannot be empty");
			throw new UserInputException("Upload location cannot be empty");
		}

		// Validate upload location file exists
		File uploadFile = new File(uploadLocation);
		if (!uploadFile.exists()) {
			LOGGER.error("Upload file does not exist at: {}", uploadLocation);
			throw new UserInputException("Upload file does not exist");
		}

		try {
			// Read the current app version for backup folder name
			String tmConfigFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
					+ Constants.TM_CONFIG_FILE;
			String currentVersion = commonService.getConfigProperty(new File(tmConfigFilePath), Constants.APP_VERSION);
			// If backup location is not provided, use default
			if (backupLocation == null || backupLocation.isEmpty()) {
				backupLocation = Constants.DEFAULT_BACKUP_LOCATION;
				LOGGER.info("Using default backup location: {}", backupLocation);
			}

			// Create backup directory with timestamp
			String timestamp = new SimpleDateFormat("yy_MM_dd_HH_mm_ss").format(new Date());
			String backupDirName = Constants.BACKUP_PREFIX + currentVersion + "_" + timestamp;
			String backupDir = backupLocation + Constants.FILE_PATH_SEPERATOR + backupDirName;

			String appUpgradeShellScriptFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
					+ "appDeploymentScripts" + Constants.FILE_PATH_SEPERATOR + Constants.APP_UPGRADE_WAR_FILE;
			File appUpgradeShellScriptFile = new File(appUpgradeShellScriptFilePath);
			String appUpgradeFilePath = appUpgradeShellScriptFile.getParent();
			String appUpgradeFileName = appUpgradeShellScriptFile.getName();

			StringBuilder commandBuilder = new StringBuilder();
			commandBuilder.append("cd ").append(appUpgradeFilePath).append(" && chmod 777 ./")
					.append(Constants.APP_UPGRADE_WAR_FILE).append(" && nohup ./").append(appUpgradeFileName)
					.append(" ").append(backupDir).append(" ").append(uploadLocation).append(" > /dev/null 2>&1 &");

			String[] command = { "sh", "-c", commandBuilder.toString() };

			LOGGER.info("ShellScript execute command: " + Arrays.toString(command));

			new Thread(() -> {
				scriptExecutorService.executeScript(command, 120);
			}).start();

			// Prepare and return the response
			AppUpgradeResponseDTO response = new AppUpgradeResponseDTO();
			response.setStatusCode(200);
			response.setBackupLocation(backupDir);
			response.setMessage("Application upgrade process initiated successfully. Backup created at " + backupDir);
			return response;

		} catch (Exception e) {
			LOGGER.error("Error during application upgrade", e);
			throw new TDKServiceException("Error during application upgrade: " + e.getMessage());
		}
	}

	/**
	 * Fetches the latest deployment logs from the backup folder
	 * 
	 * @param backupLocation Optional backup location, if null default location will
	 *                       be used
	 * @return Response containing log content and details
	 */
	@Override
	public DeploymentLogsDTO getLatestDeploymentLogs() {
		LOGGER.info("Inside Service layer. Going to fetch the latest deployment logs");
		String deploymentLogLocation = Constants.DEPLOYMENT_LOG_PATH;

		try (Stream<Path> files = Files.list(Paths.get(deploymentLogLocation))) {
			Optional<Path> lastFilePath = files.filter(Files::isRegularFile)
					.max(Comparator.comparingLong(p -> p.toFile().lastModified()));

			if (lastFilePath.isPresent()) {
				Path logFile = lastFilePath.get();
				String logContent = Files.readString(logFile, StandardCharsets.UTF_8);

				DeploymentLogsDTO response = new DeploymentLogsDTO();
				response.setStatusCode(200);
				response.setLogFilePath(logFile.getFileName().toString());
				response.setLogContent(logContent);
				return response;
			} else {
				DeploymentLogsDTO response = new DeploymentLogsDTO();
				response.setStatusCode(404);
				response.setLogContent("No deployment log files found.");
				return response;
			}
		} catch (Exception e) {
			LOGGER.error("Error fetching deployment logs", e);
			throw new TDKServiceException("Error fetching deployment logs: " + e.getMessage());
		}
	}

}
