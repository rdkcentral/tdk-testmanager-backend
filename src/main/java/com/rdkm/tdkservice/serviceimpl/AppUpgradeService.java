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

package com.rdkm.tdkservice.serviceimpl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.dto.AppUpgradeResponseDTO;
import com.rdkm.tdkservice.dto.CategoryChangeDTO;
import com.rdkm.tdkservice.dto.DeploymentLogsDTO;
import com.rdkm.tdkservice.dto.EntityDataDTO;
import com.rdkm.tdkservice.dto.EntityListMetadataDTO;
import com.rdkm.tdkservice.dto.EntityListResponseDTO;
import com.rdkm.tdkservice.dto.FunctionDataDTO;
import com.rdkm.tdkservice.dto.ModuleDataDTO;
import com.rdkm.tdkservice.dto.WarGenerationMetadata;
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

	private final Map<String, WarGenerationMetadata> warGenerationExecutions = new ConcurrentHashMap<>();

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
	 * Generates a DTO containing entity names that were created or updated after
	 * the specified date. This method retrieves all entities (DeviceType, OEM, SOC,
	 * Module, Function, Parameter, PrimitiveTest, Script, TestSuite) created or
	 * updated after the given timestamp and returns them in DTO format organized by
	 * category with separate sections for new and updated data.
	 *
	 * @param since the Instant timestamp; only entities created or updated after
	 *              this time are included
	 * @return EntityListResponseDTO containing categorized lists of entity names
	 *         grouped by category with new/updated data
	 * @throws TDKServiceException if there's an error generating the DTO
	 */
	public EntityListResponseDTO generateEntityListJsonByCreatedDate(Instant since) {
		LOGGER.info("Generating entity list DTO for entities created or updated after: {}", since);

		try {
			// Create metadata
			EntityListMetadataDTO metadata = new EntityListMetadataDTO();
			metadata.setGeneratedAt(Instant.now().toString());
			metadata.setSinceDate(since.toString());
			metadata.setDescription("List of entities created or updated after specified date, organized by category");

			// Get all simple entities (deviceType, oem, soc, script, testSuite)
			Map<String, Map<String, Map<String, List<String>>>> changesByCategory = new HashMap<>();
			processEntityChanges(changesByCategory, since);

			// Build hierarchical module data (module -> functions -> parameters, module ->
			// primitiveTests)
			Map<String, Map<String, List<ModuleDataDTO>>> moduleDataByCategory = buildAllModuleData(since);

			// Merge all categories from both maps
			Set<String> allCategories = new HashSet<>();
			allCategories.addAll(changesByCategory.keySet());
			allCategories.addAll(moduleDataByCategory.keySet());

			// Convert to DTO structure
			List<CategoryChangeDTO> changes = new ArrayList<>();
			for (String category : allCategories) {
				Map<String, Map<String, List<String>>> entityData = changesByCategory.getOrDefault(category,
						new HashMap<>());
				Map<String, List<ModuleDataDTO>> moduleData = moduleDataByCategory.getOrDefault(category,
						new HashMap<>());

				// Create EntityDataDTO for new data
				EntityDataDTO newData = createEntityDataDTO(entityData, "new",
						moduleData.getOrDefault("new", new ArrayList<>()));

				// Create EntityDataDTO for updated data
				EntityDataDTO updatedData = createEntityDataDTO(entityData, "updated",
						moduleData.getOrDefault("updated", new ArrayList<>()));

				// Create CategoryChangeDTO
				CategoryChangeDTO categoryChange = new CategoryChangeDTO();
				categoryChange.setCategory(category);
				categoryChange.setNewData(newData);
				categoryChange.setUpdatedData(updatedData);

				changes.add(categoryChange);
			}

			// Create the main response DTO
			EntityListResponseDTO response = new EntityListResponseDTO();
			response.setMetadata(metadata);
			response.setChanges(changes);

			LOGGER.info("Successfully generated entity list DTO for entities created or updated after: {}", since);
			return response;

		} catch (Exception e) {
			LOGGER.error("Unexpected error generating entity list DTO", e);
			throw new TDKServiceException("Failed to generate entity list: " + e.getMessage());
		}
	}

	/**
	 * Builds hierarchical module data for all categories and change types.
	 * Groups changed modules, functions, parameters, and primitiveTests into
	 * a Module -> Function -> Parameter hierarchy.
	 * 
	 * If any function, parameter, or primitiveTest changes, its parent module
	 * will appear in the moduleData section.
	 *
	 * @param since the Instant timestamp; only entities changed after this time
	 *              are included
	 * @return map of category -> changeType -> list of ModuleDataDTO
	 */
	private Map<String, Map<String, List<ModuleDataDTO>>> buildAllModuleData(Instant since) {
		Map<String, Map<String, Map<String, ModuleDataDTO>>> workingMap = new HashMap<>();

		// 1. Process changed modules
		List<Module> changedModules = moduleRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);
		for (Module m : changedModules) {
			if (m.getName() == null)
				continue;
			String category = normalizeCategory(m.getCategory() != null ? m.getCategory().name() : null);
			String changeType = m.getCreatedDate().isAfter(since) ? "new" : "updated";
			getOrCreateModuleData(workingMap, category, changeType, m.getName());
		}

		// 2. Process changed functions — group under their parent module
		List<Function> changedFunctions = functionRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);
		for (Function f : changedFunctions) {
			if (f.getName() == null || f.getModule() == null)
				continue;
			String moduleName = f.getModule().getName();
			String category = normalizeCategory(f.getCategory() != null ? f.getCategory().name() : null);
			String changeType = f.getCreatedDate().isAfter(since) ? "new" : "updated";
			ModuleDataDTO moduleDTO = getOrCreateModuleData(workingMap, category, changeType, moduleName);

			boolean funcExists = moduleDTO.getFunctionData().stream()
					.anyMatch(fd -> fd.getFunctionName().equals(f.getName()));
			if (!funcExists) {
				FunctionDataDTO funcDTO = new FunctionDataDTO();
				funcDTO.setFunctionName(f.getName());
				funcDTO.setParameterNames(new ArrayList<>());
				moduleDTO.getFunctionData().add(funcDTO);
			}
		}

		// 3. Process changed parameters — group under their parent function and module
		List<Parameter> changedParameters = parameterRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);
		for (Parameter p : changedParameters) {
			if (p.getName() == null || p.getFunction() == null)
				continue;
			Function func = p.getFunction();
			if (func.getModule() == null)
				continue;
			String moduleName = func.getModule().getName();
			String rawCategory = func.getCategory() != null ? func.getCategory().name() : null;
			String category = normalizeCategory(rawCategory);
			String changeType = p.getCreatedDate().isAfter(since) ? "new" : "updated";
			ModuleDataDTO moduleDTO = getOrCreateModuleData(workingMap, category, changeType, moduleName);

			// Find or create function entry within this module
			FunctionDataDTO funcDTO = moduleDTO.getFunctionData().stream()
					.filter(fd -> fd.getFunctionName().equals(func.getName()))
					.findFirst()
					.orElseGet(() -> {
						FunctionDataDTO newFuncDTO = new FunctionDataDTO();
						newFuncDTO.setFunctionName(func.getName());
						newFuncDTO.setParameterNames(new ArrayList<>());
						moduleDTO.getFunctionData().add(newFuncDTO);
						return newFuncDTO;
					});
			if (!funcDTO.getParameterNames().contains(p.getName())) {
				funcDTO.getParameterNames().add(p.getName());
			}
		}

		// 4. Process changed primitiveTests — group under their parent module
		List<PrimitiveTest> changedPTs = primitiveTestRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);
		for (PrimitiveTest pt : changedPTs) {
			if (pt.getName() == null || pt.getModule() == null)
				continue;
			String moduleName = pt.getModule().getName();
			String rawCategory = pt.getModule().getCategory() != null ? pt.getModule().getCategory().name() : null;
			String category = normalizeCategory(rawCategory);
			String changeType = pt.getCreatedDate().isAfter(since) ? "new" : "updated";
			ModuleDataDTO moduleDTO = getOrCreateModuleData(workingMap, category, changeType, moduleName);

			if (!moduleDTO.getPrimitiveTestNames().contains(pt.getName())) {
				moduleDTO.getPrimitiveTestNames().add(pt.getName());
			}
		}

		List<PrimitiveTestParameter> changedPTParams = primitiveTestParameterRepository
				.findByCreatedDateAfterOrUpdatedAtAfter(since, since);
		for (PrimitiveTestParameter ptp : changedPTParams) {
			PrimitiveTest pt = ptp.getPrimitiveTest();
			if (pt == null || pt.getName() == null || pt.getModule() == null)
				continue;

			// Skip if the primitiveTest itself is new (already handled above)
			if (pt.getCreatedDate().isAfter(since))
				continue;

			String moduleName = pt.getModule().getName();
			String rawCategory = pt.getModule().getCategory() != null ? pt.getModule().getCategory().name() : null;
			String category = normalizeCategory(rawCategory);
			ModuleDataDTO moduleDTO = getOrCreateModuleData(workingMap, category, "updated", moduleName);

			if (!moduleDTO.getPrimitiveTestNames().contains(pt.getName())) {
				moduleDTO.getPrimitiveTestNames().add(pt.getName());
			}
		}

		// Convert working map to result format
		Map<String, Map<String, List<ModuleDataDTO>>> result = new HashMap<>();
		for (Map.Entry<String, Map<String, Map<String, ModuleDataDTO>>> catEntry : workingMap.entrySet()) {
			Map<String, List<ModuleDataDTO>> changeTypeMap = new HashMap<>();
			for (Map.Entry<String, Map<String, ModuleDataDTO>> ctEntry : catEntry.getValue().entrySet()) {
				changeTypeMap.put(ctEntry.getKey(), new ArrayList<>(ctEntry.getValue().values()));
			}
			result.put(catEntry.getKey(), changeTypeMap);
		}

		return result;
	}

	/**
	 * Gets or creates a ModuleDataDTO in the working map for the given
	 * category, change type, and module name.
	 */
	private ModuleDataDTO getOrCreateModuleData(
			Map<String, Map<String, Map<String, ModuleDataDTO>>> workingMap,
			String category, String changeType, String moduleName) {
		return workingMap
				.computeIfAbsent(category, k -> new HashMap<>())
				.computeIfAbsent(changeType, k -> new LinkedHashMap<>())
				.computeIfAbsent(moduleName, name -> {
					ModuleDataDTO dto = new ModuleDataDTO();
					dto.setModuleName(name);
					dto.setFunctionData(new ArrayList<>());
					dto.setPrimitiveTestNames(new ArrayList<>());
					return dto;
				});
	}

	/**
	 * Creates an EntityDataDTO from entity data for a specific change type
	 * 
	 * @param entityData the map containing entity type to change type to names
	 * @param changeType the change type ("new" or "updated")
	 * @param moduleData the hierarchical module data for this category and change
	 *                   type
	 * @return EntityDataDTO with populated data
	 */
	private EntityDataDTO createEntityDataDTO(Map<String, Map<String, List<String>>> entityData, String changeType,
			List<ModuleDataDTO> moduleData) {
		EntityDataDTO dto = new EntityDataDTO();

		// Helper method to get list or default message
		java.util.function.Function<String, List<String>> getEntityList = (entityType) -> {
			Map<String, List<String>> changeTypes = entityData.get(entityType);
			if (changeTypes != null && changeTypes.containsKey(changeType)) {
				List<String> entityList = changeTypes.get(changeType);
				return entityList.isEmpty() ? List.of("No changes") : entityList;
			}
			return List.of("No changes");
		};

		dto.setDeviceType(getEntityList.apply("deviceType"));
		dto.setOem(getEntityList.apply("oem"));
		dto.setSoc(getEntityList.apply("soc"));
		dto.setModuleData(moduleData);
		dto.setScript(getEntityList.apply("script"));
		dto.setTestSuite(getEntityList.apply("testSuite"));

		return dto;
	}

	/**
	 * Normalizes category names to standard RDKV/RDKB format
	 * 
	 * @param rawCategory the raw category string
	 * @return normalized category (RDKV, RDKB, or UNCATEGORIZED)
	 */
	private String normalizeCategory(String rawCategory) {
		if (rawCategory == null || rawCategory.trim().isEmpty()) {
			return "UNCATEGORIZED";
		}

		String category = rawCategory.toUpperCase().trim();

		// Normalize RDKV variants
		if (category.equals("RDKV") || category.equals("RDKV_RDKSERVICE") || category.startsWith("RDKV")) {
			return "RDKV";
		}

		// Normalize RDKB variants
		if (category.equals("RDKB") || category.startsWith("RDKB")) {
			return "RDKB";
		}

		// All other categories are considered uncategorized
		return "UNCATEGORIZED";
	}

	/**
	 * Processes simple entity changes and organizes them by category, entity type,
	 * and change type (new/updated). Module-related entities are handled separately
	 * via buildAllModuleData().
	 */
	private void processEntityChanges(Map<String, Map<String, Map<String, List<String>>>> changesByCategory,
			Instant since) {
		// Process DeviceType entities
		processDeviceTypeChanges(changesByCategory, since);

		// Process OEM entities
		processOemChanges(changesByCategory, since);

		// Process SOC entities
		processSocChanges(changesByCategory, since);

		// Process Script entities
		processScriptChanges(changesByCategory, since);

		// Process TestSuite entities
		processTestSuiteChanges(changesByCategory, since);
	}

	/**
	 * Processes DeviceType entity changes and organizes them by category and change
	 * type (new/updated)
	 */
	private void processDeviceTypeChanges(Map<String, Map<String, Map<String, List<String>>>> changesByCategory,
			Instant since) {
		List<DeviceType> deviceTypes = deviceTypeRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);

		for (DeviceType dt : deviceTypes) {
			if (dt.getName() != null) {
				String rawCategory = dt.getCategory() != null ? dt.getCategory().toString() : null;
				String category = normalizeCategory(rawCategory);
				boolean isNew = dt.getCreatedDate().isAfter(since);
				String changeType = isNew ? "new" : "updated";

				addToChanges(changesByCategory, category, "deviceType", changeType, dt.getName());
			}
		}
	}

	/**
	 * Processes Oem entity changes and organizes them by category and change type
	 * (new/updated)
	 */
	private void processOemChanges(Map<String, Map<String, Map<String, List<String>>>> changesByCategory,
			Instant since) {
		List<Oem> oems = oemRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);

		for (Oem oem : oems) {
			if (oem.getName() != null) {
				String rawCategory = oem.getCategory() != null ? oem.getCategory().toString() : null;
				String category = normalizeCategory(rawCategory);
				boolean isNew = oem.getCreatedDate().isAfter(since);
				String changeType = isNew ? "new" : "updated";

				addToChanges(changesByCategory, category, "oem", changeType, oem.getName());
			}
		}
	}

	/**
	 * Processes Soc entity changes and organizes them by category and change type
	 * (new/updated)
	 */
	private void processSocChanges(Map<String, Map<String, Map<String, List<String>>>> changesByCategory,
			Instant since) {
		List<Soc> socs = socRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);

		for (Soc soc : socs) {
			if (soc.getName() != null) {
				String rawCategory = soc.getCategory() != null ? soc.getCategory().toString() : null;
				String category = normalizeCategory(rawCategory);
				boolean isNew = soc.getCreatedDate().isAfter(since);
				String changeType = isNew ? "new" : "updated";

				addToChanges(changesByCategory, category, "soc", changeType, soc.getName());
			}
		}
	}

	/**
	 * Processes Module entity changes and organizes them by category and change
	 * type (new/updated)
	 */
	private void processModuleChanges(Map<String, Map<String, Map<String, List<String>>>> changesByCategory,
			Instant since) {
		List<Module> modules = moduleRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);

		for (Module module : modules) {
			if (module.getName() != null) {
				String rawCategory = module.getCategory() != null ? module.getCategory().name() : null;
				String category = normalizeCategory(rawCategory);
				boolean isNew = module.getCreatedDate().isAfter(since);
				String changeType = isNew ? "new" : "updated";

				addToChanges(changesByCategory, category, "module", changeType, module.getName());
			}
		}
	}

	/**
	 * Processes Function entity changes and organizes them by category and change
	 * type (new/updated)
	 */
	private void processFunctionChanges(Map<String, Map<String, Map<String, List<String>>>> changesByCategory,
			Instant since) {
		List<Function> functions = functionRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);

		for (Function function : functions) {
			if (function.getName() != null) {
				String rawCategory = function.getCategory() != null ? function.getCategory().name() : null;
				String category = normalizeCategory(rawCategory);
				boolean isNew = function.getCreatedDate().isAfter(since);
				String changeType = isNew ? "new" : "updated";

				addToChanges(changesByCategory, category, "function", changeType, function.getName());
			}
		}
	}

	/**
	 * Processes Parameter entity changes and organizes them by category and change
	 * type (new/updated)
	 */
	private void processParameterChanges(Map<String, Map<String, Map<String, List<String>>>> changesByCategory,
			Instant since) {
		List<Parameter> parameters = parameterRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);

		for (Parameter parameter : parameters) {
			if (parameter.getName() != null) {
				// Use function's category if available, otherwise use parameter data type
				String rawCategory = null;
				if (parameter.getFunction() != null && parameter.getFunction().getCategory() != null) {
					rawCategory = parameter.getFunction().getCategory().name();
				} else if (parameter.getParameterDataType() != null) {
					rawCategory = parameter.getParameterDataType().name();
				}

				String category = normalizeCategory(rawCategory);
				boolean isNew = parameter.getCreatedDate().isAfter(since);
				String changeType = isNew ? "new" : "updated";

				addToChanges(changesByCategory, category, "parameter", changeType, parameter.getName());
			}
		}
	}

	/**
	 * Processes PrimitiveTest entity changes and organizes them by category and
	 * change type (new/updated)
	 */
	private void processPrimitiveTestChanges(Map<String, Map<String, Map<String, List<String>>>> changesByCategory,
			Instant since) {
		List<PrimitiveTest> primitiveTests = primitiveTestRepository.findByCreatedDateAfterOrUpdatedAtAfter(since,
				since);

		for (PrimitiveTest pt : primitiveTests) {
			if (pt.getName() != null) {
				String rawCategory = pt.getModule() != null && pt.getModule().getCategory() != null
						? pt.getModule().getCategory().name()
						: null;
				String category = normalizeCategory(rawCategory);
				boolean isNew = pt.getCreatedDate().isAfter(since);
				String changeType = isNew ? "new" : "updated";

				addToChanges(changesByCategory, category, "primitiveTest", changeType, pt.getName());
			}
		}
	}

	/**
	 * Processes Script entity changes and organizes them by category and change
	 * type (new/updated)
	 */
	private void processScriptChanges(Map<String, Map<String, Map<String, List<String>>>> changesByCategory,
			Instant since) {
		List<Script> scripts = scriptRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);

		for (Script script : scripts) {
			if (script.getName() != null) {
				String rawCategory = script.getCategory() != null ? script.getCategory().name() : null;
				String category = normalizeCategory(rawCategory);
				boolean isNew = script.getCreatedDate().isAfter(since);
				String changeType = isNew ? "new" : "updated";

				addToChanges(changesByCategory, category, "script", changeType, script.getName());
			}
		}
	}

	/**
	 * Processes TestSuite entity changes and organizes them by category and change
	 * type (new/updated)
	 */
	private void processTestSuiteChanges(Map<String, Map<String, Map<String, List<String>>>> changesByCategory,
			Instant since) {
		List<TestSuite> testSuites = testSuiteRepository.findByCreatedDateAfterOrUpdatedAtAfter(since, since);

		// Filter out TestSuites whose name matches any existing Module name
		List<String> moduleNames = moduleRepository.findAll().stream().map(Module::getName)
				.filter(java.util.Objects::nonNull).toList();

		for (TestSuite ts : testSuites) {
			if (ts.getName() != null && !moduleNames.contains(ts.getName())) {
				String rawCategory = ts.getCategory() != null ? ts.getCategory().name() : null;
				String category = normalizeCategory(rawCategory);
				boolean isNew = ts.getCreatedDate().isAfter(since);
				String changeType = isNew ? "new" : "updated";

				addToChanges(changesByCategory, category, "testSuite", changeType, ts.getName());
			}
		}
	}

	/**
	 * Adds an entity name to the changes map under the specified category, entity
	 * type, and change type.
	 * 
	 * @param changesByCategory
	 * @param category
	 * @param entityType
	 * @param changeType
	 * @param entityName
	 */
	private void addToChanges(Map<String, Map<String, Map<String, List<String>>>> changesByCategory, String category,
			String entityType, String changeType, String entityName) {
		changesByCategory.computeIfAbsent(category, k -> new HashMap<>())
				.computeIfAbsent(entityType, k -> new HashMap<>()).computeIfAbsent(changeType, k -> new ArrayList<>())
				.add(entityName);
	}

	/**
	 * Generates and writes entity list JSON to a file for entities created after
	 * the specified date. This method creates a JSON file containing all entities
	 * (DeviceType, OEM, SOC, Module, Function, Parameter, PrimitiveTest, Script,
	 * TestSuite) that were created after the given timestamp, organized by entity
	 * type and category.
	 *
	 * @param since    the Instant timestamp; only entities created after this time
	 *                 are included
	 * @param filePath the path to the output JSON file
	 * @throws IOException if an I/O error occurs during file writing
	 */
	public void writeEntityListJsonToFile(Instant since, String filePath) throws IOException {
		LOGGER.info("Writing entity list JSON to file: {}", filePath);

		try {
			EntityListResponseDTO responseDTO = generateEntityListJsonByCreatedDate(since);

			ObjectMapper objectMapper = new ObjectMapper();
			String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseDTO);

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
				writer.write(json);
			}

			LOGGER.info("Successfully wrote entity list JSON to file: {}", filePath);

		} catch (JsonProcessingException e) {
			LOGGER.error("Error serializing entity list DTO to JSON", e);
			throw new TDKServiceException("Failed to serialize entity list DTO: " + e.getMessage());
		} catch (IOException e) {
			LOGGER.error("Error writing entity list JSON to file", e);
			throw new TDKServiceException("Failed to write entity list JSON: " + e.getMessage());
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
		List<String> moduleNames = moduleRepository.findAll().stream().map(m -> m.getName())
				.filter(java.util.Objects::nonNull).toList();
		testSuites = testSuites.stream().filter(ts -> ts.getName() == null || !moduleNames.contains(ts.getName()))
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
			String appUrl = commonService.getConfigProperty(new File(tmConfigFilePath), Constants.TM_URL);
			String healthCheckUrl = appUrl + "actuator/health";
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
			commandBuilder.append("cp ").append(appUpgradeFilePath).append("/").append(Constants.APP_UPGRADE_WAR_FILE)
					.append(" /opt/tomcat/webapps/ && ");
			commandBuilder.append("cp ").append(appUpgradeFilePath).append("/app_upgrade_config_backup.sh")
					.append(" /opt/tomcat/webapps/ && ");

			commandBuilder.append("chmod 777 /opt/tomcat/webapps/").append(Constants.APP_UPGRADE_WAR_FILE)
					.append(" && ");
			commandBuilder.append("chmod 777 /opt/tomcat/webapps/app_upgrade_config_backup.sh && ");

			commandBuilder.append("cd /opt/tomcat/webapps && nohup ./").append(Constants.APP_UPGRADE_WAR_FILE)
					.append(" ").append(backupDir).append(" ").append(uploadLocation).append(" ").append(healthCheckUrl)
					.append(" > /dev/null 2>&1 &");
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

	/**
	 * Initiates WAR (Web Application Archive) generation for a specific release
	 * tag.
	 * 
	 * This method validates the release tag format, creates a unique execution ID,
	 * reads configuration properties, and sets up the necessary metadata for WAR
	 * generation.
	 * The actual generation process is prepared but not executed by this method.
	 * 
	 * @param releaseTag The release tag for which to generate the WAR file. Must
	 *                   follow
	 *                   the format "TDK_M" followed by exactly 3 digits (e.g.,
	 *                   "TDK_M123").
	 * 
	 * @return A unique execution ID (UUID) that can be used to track the WAR
	 *         generation process.
	 * 
	 * @throws UserInputException  If the releaseTag is null or doesn't match the
	 *                             required format
	 *                             "^TDK_M\\d{3}$"
	 * @throws TDKServiceException If unable to read the upgrade file location from
	 *                             the tm.config file
	 */
	@Override
	public String executeWarGeneration(String releaseTag) {
		LOGGER.info("Initiating WAR generation for release tag: {}", releaseTag);

		// Validate releaseTag: must start with TDK_M followed by 3 digits
		if (releaseTag == null || !releaseTag.matches("^TDK_M\\d{3}$")) {
			LOGGER.error("Invalid release tag format: {}", releaseTag);
			throw new UserInputException(
					"Release tag must start with 'TDK_M' followed by a 3-digit number (e.g., TDK_M123)");
		}

		String executionId = UUID.randomUUID().toString();

		// Read the upgrade file location from tm.config
		String tmConfigFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
				+ Constants.TM_CONFIG_FILE;
		String baseUpgradeLocation = commonService.getConfigProperty(new File(tmConfigFilePath),
				Constants.UPGRADE_FILE_LOCATION);
		if (baseUpgradeLocation == null || baseUpgradeLocation.isEmpty()) {
			LOGGER.error("Unable to read upgrade.fileLocation from tm.config");
			throw new TDKServiceException("Unable to read upgrade file location from config");
		}

		String timestampDirName = "NewRelease_" + new SimpleDateFormat("yy_MM_dd_HH_mm_ss").format(new Date());
		String upgradeDir = baseUpgradeLocation + Constants.FILE_PATH_SEPERATOR + timestampDirName;

		WarGenerationMetadata metadata = new WarGenerationMetadata();
		metadata.setExecutionId(executionId);
		metadata.setReleaseTag(releaseTag);
		metadata.setStatus("PENDING");

		// Get script path from fileStore
		String scriptPath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + "war_generation.sh";
		metadata.setScriptPath(scriptPath);
		metadata.setUpgradeDir(upgradeDir);
		metadata.setCreatedAt(Instant.now());

		warGenerationExecutions.put(executionId, metadata);

		LOGGER.info("Created WAR generation execution ID: {} for release tag: {}", executionId, releaseTag);
		return executionId;
	}

	/**
	 * Asynchronously streams WAR generation logs to a Server-Sent Events (SSE)
	 * emitter.
	 * 
	 * This method executes a WAR generation script and streams the output in
	 * real-time
	 * to the client via SSE. It also writes all logs to a timestamped log file for
	 * persistence.
	 * 
	 * @param executionId The unique identifier for the WAR generation execution.
	 *                    Used to retrieve metadata from warGenerationExecutions
	 *                    map.
	 * @param emitter     The SseEmitter instance used to stream real-time events
	 *                    and logs
	 *                    to the client browser.
	 * 
	 * @throws IOException          If there's an error creating the log directory
	 *                              or file,
	 *                              or reading from the process input stream.
	 * @throws InterruptedException If the process execution is interrupted.
	 * 
	 * 
	 */
	@Override
	@Async
	public void streamWarGenerationLogs(String executionId, SseEmitter emitter) {
		WarGenerationMetadata metadata = warGenerationExecutions.get(executionId);
		if (metadata == null) {
			sendSseError(emitter, "Execution not found");
			return;
		}
		metadata.setStatus("RUNNING");

		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		String logDirPath = "/mnt/WarGeneration/logs";
		String logFilePath = logDirPath + "/" + timestamp + "_WarGeneration.log";

		// Ensure log directory exists
		try {
			Files.createDirectories(Paths.get(logDirPath));
		} catch (IOException e) {
			LOGGER.error("Failed to create log directory: {}", e.getMessage());
			sendSseError(emitter, "Failed to create log directory");
			return;
		}

		Process process = null;
		try (BufferedWriter logFileWriter = new BufferedWriter(new FileWriter(logFilePath))) {
			sendSseEvent(emitter, "status",
					Map.of("message", "Starting WAR generation for release: " + metadata.getReleaseTag()));

			String scriptPath = metadata.getScriptPath();
			String releaseTag = metadata.getReleaseTag();
			String upgradeDirPath = metadata.getUpgradeDir();

			ProcessBuilder processBuilder = new ProcessBuilder("bash", scriptPath, releaseTag, upgradeDirPath)
					.directory(new File(new File(scriptPath).getParent()))
					.redirectErrorStream(true);

			LOGGER.info("Executing command: bash {} {} {}", scriptPath, releaseTag, upgradeDirPath);

			process = processBuilder.start();
			metadata.setProcess(process);

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					LOGGER.debug("WAR Gen Log [{}]: {}", line);
					sendSseEvent(emitter, "log", Map.of("message", line));
					logFileWriter.write(line);
					logFileWriter.newLine();
					logFileWriter.flush();
				}
			}

			int exitCode = process.waitFor();
			metadata.setExitCode(exitCode);

			if (exitCode == 0) {
				metadata.setStatus("COMPLETED");
				sendSseEvent(emitter, "complete", Map.of(
						"status", "SUCCESS",
						"exitCode", exitCode,
						"message", "WAR generation completed successfully",
						"releaseTag", metadata.getReleaseTag(),
						"upgradeDir", metadata.getUpgradeDir()));
				LOGGER.info("WAR generation completed successfully for execution: {}", executionId);
			} else {
				metadata.setStatus("FAILED");
				sendSseEvent(emitter, "complete", Map.of(
						"status", "FAILED",
						"exitCode", exitCode,
						"message", "WAR generation failed with exit code: " + exitCode,
						"releaseTag", metadata.getReleaseTag()));
				LOGGER.error("WAR generation failed for execution: {} with exit code: {}", executionId, exitCode);
			}
		} catch (InterruptedException e) {
			handleProcessException(metadata, emitter, "INTERRUPTED", "WAR generation was interrupted", e);
		} catch (Exception e) {
			handleProcessException(metadata, emitter, "ERROR", "Error: " + e.getMessage(), e);
		} finally {
			cleanupExecution(executionId);
			emitter.complete();
			if (process != null && process.isAlive()) {
				process.destroy();
				try {
					if (!process.waitFor(5, TimeUnit.SECONDS)) {
						process.destroyForcibly();
					}
				} catch (InterruptedException e) {
					process.destroyForcibly();
				}
			}

		}
	}

	/**
	 * Removes and cleans up metadata for a specific WAR generation execution.
	 * 
	 * @param executionId the unique identifier of the execution to clean up
	 */
	private void cleanupExecution(String executionId) {
		WarGenerationMetadata metadata = warGenerationExecutions.remove(executionId);
		if (metadata != null) {
			LOGGER.info("Cleaned up metadata for execution: {}", executionId);
		}
	}

	/**
	 * Performs weekly cleanup of WAR generation metadata to prevent memory leaks
	 * and
	 * maintain optimal performance.
	 * 
	 */
	@Scheduled(cron = "0 0 2 * * SUN") // Every Sunday at 2 AM
	public void weeklyCleanup() {
		LOGGER.info("Starting weekly cleanup of WAR generation metadata");

		int cleanedCount = 0;

		Iterator<Map.Entry<String, WarGenerationMetadata>> iterator = warGenerationExecutions.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<String, WarGenerationMetadata> entry = iterator.next();
			WarGenerationMetadata metadata = entry.getValue();

			cleanupProcess(entry.getKey(), metadata);
			iterator.remove();
			cleanedCount++;
		}

		LOGGER.info("Weekly cleanup completed. Removed {} old entries", cleanedCount);
	}

	/**
	 * Cleans up and terminates the process associated with a specific execution.
	 * 
	 * This method safely terminates any running process linked to the given
	 * execution ID
	 * by first attempting a graceful shutdown, then forcing termination if
	 * necessary.
	 * 
	 * @param executionId the unique identifier for the execution whose process
	 *                    should be cleaned up
	 * @param metadata    the war generation metadata containing the process
	 *                    reference to be terminateds
	 */
	private void cleanupProcess(String executionId, WarGenerationMetadata metadata) {
		// Clean up any associated Process (extracted from your current implementation)
		Process process = metadata.getProcess();
		if (process != null && process.isAlive()) {
			LOGGER.warn("Force terminating process for execution: {}", executionId);
			try {
				process.destroy();
				if (!process.waitFor(5, TimeUnit.SECONDS)) {
					process.destroyForcibly();
				}
			} catch (InterruptedException e) {
				process.destroyForcibly();
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Sends a Server-Sent Event (SSE) to the client through the provided emitter.
	 * 
	 * @param emitter   the SseEmitter instance used to send the event to the client
	 * @param eventName the name of the SSE event to be sent
	 * @param data      a map containing the event data to be transmitted as
	 *                  key-value pairs
	 * 
	 * @throws IOException if an error occurs while sending the SSE event (handled
	 *                     internally)
	 */
	private void sendSseEvent(SseEmitter emitter, String eventName, Map<String, Object> data) {
		try {
			emitter.send(SseEmitter.event().name(eventName).data(data));
		} catch (IOException e) {
			LOGGER.error("Error sending SSE event '{}': {}", eventName, e.getMessage());
		}
	}

	/**
	 * Sends an error message to the client via Server-Sent Events (SSE) and
	 * completes the emitter.
	 * This method creates an error event with the provided message and immediately
	 * closes
	 * the SSE connection after sending the error.
	 *
	 * @param emitter the SseEmitter instance used to send the error event to the
	 *                client
	 * @param message the error message to be sent to the client
	 */
	private void sendSseError(SseEmitter emitter, String message) {
		sendSseEvent(emitter, "error", Map.of("message", message));
		emitter.complete();
	}

	/**
	 * Handles exceptions that occur during the WAR generation process by logging
	 * the error,
	 * updating the metadata status, and sending an SSE event to notify the client.
	 *
	 * @param metadata the WAR generation metadata object that tracks the execution
	 *                 state;
	 *                 may be null if metadata is not available
	 * @param emitter  the Server-Sent Events emitter used to send real-time updates
	 *                 to the client
	 * @param status   the error status string to be set in metadata and sent to the
	 *                 client
	 * @param message  the error message describing what went wrong
	 * @param e        the exception that was caught and needs to be handled
	 */
	private void handleProcessException(WarGenerationMetadata metadata, SseEmitter emitter, String status,
			String message, Exception e) {
		LOGGER.error("{} for WAR generation ID: {}", status, metadata != null ? metadata.getExecutionId() : "unknown",
				e);
		if (metadata != null) {
			metadata.setStatus(status);
			metadata.setError(message);
		}
		sendSseEvent(emitter, "error", Map.of("status", status, "message", message));

	}
}
