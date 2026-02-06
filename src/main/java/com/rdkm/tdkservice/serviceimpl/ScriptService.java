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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.dto.PreConditionDTO;
import com.rdkm.tdkservice.dto.ScriptCreateDTO;
import com.rdkm.tdkservice.dto.ScriptDTO;
import com.rdkm.tdkservice.dto.ScriptDetailsResponse;
import com.rdkm.tdkservice.dto.ScriptListDTO;
import com.rdkm.tdkservice.dto.ScriptModuleDTO;
import com.rdkm.tdkservice.dto.TestStepCreateDTO;
import com.rdkm.tdkservice.dto.TestStepDTO;
import com.rdkm.tdkservice.dto.TestSuiteCreateDTO;
import com.rdkm.tdkservice.dto.TestSuiteDTO;
import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.exception.DeleteFailedException;
import com.rdkm.tdkservice.exception.MandatoryFieldException;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.exception.UserInputException;
import com.rdkm.tdkservice.model.DeviceType;
import com.rdkm.tdkservice.model.Module;
import com.rdkm.tdkservice.model.PreCondition;
import com.rdkm.tdkservice.model.PrimitiveTest;
import com.rdkm.tdkservice.model.Script;
import com.rdkm.tdkservice.model.ScriptTestSuite;
import com.rdkm.tdkservice.model.TestStep;
import com.rdkm.tdkservice.model.TestSuite;
import com.rdkm.tdkservice.model.UserGroup;
import com.rdkm.tdkservice.repository.DeviceTypeRepository;
import com.rdkm.tdkservice.repository.ModuleRepository;
import com.rdkm.tdkservice.repository.PreConditionRepository;
import com.rdkm.tdkservice.repository.PrimitiveTestRepository;
import com.rdkm.tdkservice.repository.ScriptRepository;
import com.rdkm.tdkservice.repository.ScriptTestSuiteRepository;
import com.rdkm.tdkservice.repository.TestStepRepository;
import com.rdkm.tdkservice.repository.TestSuiteRepository;
import com.rdkm.tdkservice.repository.UserGroupRepository;
import com.rdkm.tdkservice.service.IScriptService;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.MapperUtils;
import com.rdkm.tdkservice.util.Utils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Service for scripts. This class is used to provide the service to save the
 * script file and the script details. The script file will be saved in the
 * location based on the module and category. The script details will be saved
 * in the database.
 */
@Service
public class ScriptService implements IScriptService {

	public static final Logger LOGGER = LoggerFactory.getLogger(ScriptService.class);

	@Autowired
	ScriptRepository scriptRepository;

	@Autowired
	DeviceTypeRepository deviceTypeRepository;

	@Autowired
	PrimitiveTestRepository primitiveTestRepository;

	@Autowired
	private UserGroupRepository userGroupRepository;

	@Autowired
	private ModuleRepository moduleRepository;

	@Autowired
	TestSuiteRepository testSuiteRepository;

	@Autowired
	ScriptTestSuiteRepository scriptTestSuiteRepository;

	@Autowired
	private PreConditionRepository preConditionRepository;

	@Autowired
	private TestStepRepository testStepRepository;

	@Autowired
	CommonService commonService;

	@Autowired
	TestSuiteService testSuiteService;

	/**
	 * Save the script file and the script details freshly from UI or from API call.
	 * The script file will be saved in the location based on the module and
	 * category. The script details will be saved in the database.
	 */
	@Override
	public boolean saveScript(MultipartFile scriptFile, ScriptCreateDTO scriptCreateDTO) {
		return this.saveNewScript(scriptFile, scriptCreateDTO, null);
	}

	/**
	 * Save the script file and the script details freshly from UI or from API call.
	 * The script file will be saved in the location based on the module and
	 * category. The script details will be saved in the database.
	 * 
	 * @param scriptFile      - the script file to be saved
	 * @param scriptCreateDTO - the script creation DTO
	 * @param ID              - the ID of the script (optional), needed only for XML
	 *                        based file transfer
	 * @return true if the script was saved successfully, false otherwise
	 */
	private boolean saveNewScript(MultipartFile scriptFile, ScriptCreateDTO scriptCreateDTO, String ID) {
		LOGGER.info("Saving script file: " + scriptFile.getOriginalFilename() + " for scriptdetails: "
				+ scriptCreateDTO.toString());
		// Check if the script already exists with the same name or not in the database
		this.checkIfScriptExists(scriptCreateDTO);

		// Convert DTO to Entity, this is a custom method
		Script script = MapperUtils.convertToScriptEntity(scriptCreateDTO);
		if (ID != null) {
			script.setId(UUID.fromString(ID));
		}

		// Get the primitive test based on the primitive test name
		PrimitiveTest primitiveTest = primitiveTestRepository.findByName(scriptCreateDTO.getPrimitiveTestName());
		if (null != primitiveTest) {
			script.setPrimitiveTest(primitiveTest);
		} else {
			LOGGER.error("Primitive test not found with the name: " + scriptCreateDTO.getPrimitiveTestName());
			throw new ResourceNotFoundException(Constants.PRIMITIVE_TEST_NAME, scriptCreateDTO.getPrimitiveTestName());
		}

		// Set user group in the script
		UserGroup userGroup = userGroupRepository.findByName(scriptCreateDTO.getUserGroup());
		if (userGroup != null) {
			script.setUserGroup(userGroup);
		}

		// Get the module based on the primitive test
		Module module = primitiveTest.getModule();
		if (module == null) {
			LOGGER.error("Module not found for the primitive test: " + primitiveTest.getName());
			throw new TDKServiceException("Module not found for the primitive test: " + primitiveTest.getName());
		}
		script.setModule(module);
		// TODO: Need to implement later
		// if (module.getExecutionTime() < script.getExecutionTimeOut()) {
		// LOGGER.error("Script execution time out cannot be greater than module
		// execution time");
		// throw new UserInputException("Script execution time out cannot be greater
		// than module execution time");
		// }

		// Set the category based on the module
		Category category = this.getCategoryBasedOnModule(module);
		script.setCategory(category);

		// Get script location based on the module and category
		String scriptLocation = this.getScriptLocation(module, category);

		// Validate the script file before saving
		this.validateScriptFile(scriptFile, scriptCreateDTO.getName(), scriptLocation);

		// Save the script file
		this.saveScriptFile(scriptFile, scriptLocation);

		// Save the script details in the database after the python file is saved
		script.setScriptLocation(scriptLocation);

		// If the file is saved successfully, save the script details
		Script savedScript = null;
		try {
			script.setScriptLocation(scriptLocation);
			// Set the device types in the script entity
			if (scriptCreateDTO.getDeviceTypes() != null && !scriptCreateDTO.getDeviceTypes().isEmpty()) {
				List<DeviceType> deviceTypes = this.getScriptDevicetypes(scriptCreateDTO.getDeviceTypes(), category);
				if (null != deviceTypes & !deviceTypes.isEmpty()) {
					script.setDeviceTypes(deviceTypes);

				}
			}
			savedScript = scriptRepository.save(script);

		} catch (ResourceNotFoundException e) {
			// Let ResourceNotFoundException propagate to the global exception handler
			LOGGER.error("Device type doesnt exist " + e.getMessage());
			throw e;
		} catch (Exception e) {
			LOGGER.error("Error saving script with device types: " + e.getMessage());
			e.printStackTrace();
		}

		return null != savedScript;

	}

	/**
	 * This method is used to update the script.
	 * 
	 * @param scriptFile      - the script file
	 * @param scriptUpdateDTO - the script update dto
	 * @return - true if the script is updated successfully, false otherwise
	 */
	@Override
	public boolean updateScript(MultipartFile scriptFile, ScriptDTO scriptUpdateDTO) {

		LOGGER.info("Updating script : " + scriptUpdateDTO.toString());

		// Check if the script ID is present in the database or not
		Script script = scriptRepository.findById(scriptUpdateDTO.getId()).orElseThrow(
				() -> new ResourceNotFoundException(Constants.SCRIPT_ID, scriptUpdateDTO.getId().toString()));

		boolean hasEntityChanges = checkIfEntityChangesExist(scriptUpdateDTO, script);
		if (!hasEntityChanges) {
			this.updateScriptFileOnly(scriptFile, scriptUpdateDTO, script);
			return true;
		}
		// Check if the script name is being updated and if it already exists in the
		// database
		if (!Utils.isEmpty(scriptUpdateDTO.getName())) {
			Script newScript = scriptRepository.findByName(scriptUpdateDTO.getName());
			if (newScript != null && scriptUpdateDTO.getName().equalsIgnoreCase(script.getName())) {
				script.setName(scriptUpdateDTO.getName());
			} else {
				if (scriptRepository.existsByName(scriptUpdateDTO.getName())) {
					LOGGER.info("Module already exists with the same name: " + scriptUpdateDTO.getName());
					throw new ResourceAlreadyExistsException(Constants.SCRIPT, scriptUpdateDTO.getName());
				} else {
					script.setName(scriptUpdateDTO.getName());
				}
			}
		}

		return this.updateTheGivenScriptAndFile(scriptFile, scriptUpdateDTO, script);

	}

	/**
	 * Updates the given script and its file. For the update functionality via XML,
	 * the script object will be passed here. For update via UI or Rest API, the
	 * script id will be used to identify the script to be updated.
	 *
	 * @param scriptFile      - the script file to be updated
	 * @param scriptUpdateDTO - the script update DTO
	 * @param script          - the script entity to be updated
	 * @return true if the script was updated successfully, false otherwise
	 */
	private boolean updateScriptFromXML(MultipartFile scriptFile, ScriptDTO scriptUpdateDTO, Script script) {
		return this.updateTheGivenScriptAndFile(scriptFile, scriptUpdateDTO, script);
	}

	/**
	 * Updates the given script and its file. The logic for updating the script is
	 * common to the source of the update (XML, UI, REST API).
	 *
	 * @param scriptFile      - the script file to be updated
	 * @param scriptUpdateDTO - the script update DTO
	 * @param script          - the script entity to be updated
	 * @return true if the script was updated successfully, false otherwise
	 */
	private boolean updateTheGivenScriptAndFile(MultipartFile scriptFile, ScriptDTO scriptUpdateDTO, Script script) {

		// Get the primitive test based on the primitive test name
		PrimitiveTest primitiveTest = primitiveTestRepository.findByName(scriptUpdateDTO.getPrimitiveTestName());
		if (null != primitiveTest) {
			script.setPrimitiveTest(primitiveTest);
		} else {
			LOGGER.error("Primitive test not found with the name: " + scriptUpdateDTO.getPrimitiveTestName());
			throw new ResourceNotFoundException(Constants.PRIMITIVE_TEST_NAME, scriptUpdateDTO.getPrimitiveTestName());
		}

		// Get the module based on the primitive test
		Module module = primitiveTest.getModule();
		if (module == null) {
			LOGGER.error("Module not found for the primitive test: " + primitiveTest.getName());
			throw new TDKServiceException("Module not found for the primitive test: " + primitiveTest.getName());
		}
		script.setModule(module);
		// TODO: Need to implement later
		// if (module.getExecutionTime() < scriptUpdateDTO.getExecutionTimeOut()) {
		// LOGGER.error("Script execution time out cannot be greater than module
		// execution time");
		// throw new UserInputException("Script execution time out cannot be greater
		// than module execution time");
		// }

		// Set the category based on the module
		Category category = this.getCategoryBasedOnModule(module);
		script.setCategory(category);

		// Get script location based on the module and category
		String scriptLocation = this.getScriptLocation(module, category);
		if (scriptLocation != script.getScriptLocation()) {
			this.deleteScriptFile(script.getName(), script.getScriptLocation());
			script.setScriptLocation(scriptLocation);
		}
		// Updating the script entity with the updated script details
		script = MapperUtils.updateScript(script, scriptUpdateDTO);

		// Update the primitive test in the script entity
		if (scriptUpdateDTO.getPreConditions() != null && !scriptUpdateDTO.getPreConditions().isEmpty()) {
			this.updatePreConditions(script, scriptUpdateDTO.getPreConditions());
		} else {
			LOGGER.info("No preconditions to update for the script: " + script.getName());
		}

		// Update the test steps
		if (scriptUpdateDTO.getTestSteps() != null && !scriptUpdateDTO.getTestSteps().isEmpty()) {
			updateTestSteps(script, scriptUpdateDTO.getTestSteps());
		} else {
			LOGGER.info("No test steps to update for the script: " + script.getName());
		}

		// If the script file is updated, validate and save the new script file
		if (!scriptFile.isEmpty()) {
			this.validateScriptFile(scriptFile, script.getName(), script.getScriptLocation());
			// This will replave the existing file with the new file
			this.saveScriptFile(scriptFile, script.getScriptLocation());
		}

		// Set devicetypes in the script entity if the devicetypes are updated
		if (null != scriptUpdateDTO.getDeviceTypes() || !scriptUpdateDTO.getDeviceTypes().isEmpty()) {
			List<DeviceType> deviceType = this.getScriptDevicetypes(scriptUpdateDTO.getDeviceTypes(),
					script.getCategory());
			script.setDeviceTypes(deviceType);
		}

		Script updatedScript = scriptRepository.save(script);
		return (null != updatedScript);
	}

	/**
	 * Updates the test steps in the script. Clears existing test steps and adds new
	 * ones.
	 * 
	 * @param script       the script to be updated
	 * @param testStepDTOs the list of test step DTOs containing the updated details
	 */
	private void updateTestSteps(Script script, List<TestStepDTO> testStepDTOs) {
		// Remove all existing test steps
		script.getTestSteps().clear();

		// Add new test steps from DTOs
		for (TestStepDTO testStepDTO : testStepDTOs) {
			TestStep testStep = new TestStep();
			testStep.setStepName(testStepDTO.getStepName());
			testStep.setStepDescription(testStepDTO.getStepDescription());
			testStep.setExpectedResult(testStepDTO.getExpectedResult());
			testStep.setScript(script);
			script.getTestSteps().add(testStep);
		}
	}

	/**
	 * Updates the preconditions in the script. *
	 * 
	 * @param script           - the script
	 * @param preConditionDTOs - the list of precondition DTOs
	 */
	private void updatePreConditions(Script script, List<PreConditionDTO> preConditionDTOs) {
		// Remove all existing preconditions
		script.getPreConditions().clear();

		// Add new preconditions from DTOs
		for (PreConditionDTO preConditionDTO : preConditionDTOs) {
			PreCondition preCondition = new PreCondition();
			preCondition.setPreConditionDescription(preConditionDTO.getPreConditionDetails());
			preCondition.setScript(script);
			script.getPreConditions().add(preCondition);
		}
	}

	/**
	 * Checks if any entity changes exist between the DTO and existing script. This
	 * is a helper method that returns boolean instead of throwing exception.
	 * 
	 * @param scriptUpdateDTO - the script update DTO with new values
	 * @param existingScript  - the existing script entity from database
	 * @return true if any entity changes are detected, false otherwise
	 */
	private boolean checkIfEntityChangesExist(ScriptDTO scriptUpdateDTO, Script existingScript) {
		return hasNameChanged(scriptUpdateDTO, existingScript)
				|| hasPrimitiveTestChanged(scriptUpdateDTO, existingScript)
				|| hasSynopsisChanged(scriptUpdateDTO, existingScript)
				|| hasExecutionTimeoutChanged(scriptUpdateDTO, existingScript)
				|| hasLongDurationChanged(scriptUpdateDTO, existingScript)
				|| hasSkipExecutionChanged(scriptUpdateDTO, existingScript)
				|| hasTestIdChanged(scriptUpdateDTO, existingScript)
				|| hasObjectiveChanged(scriptUpdateDTO, existingScript)
				|| hasPriorityChanged(scriptUpdateDTO, existingScript)
				|| hasReleaseVersionChanged(scriptUpdateDTO, existingScript)
				|| hasDeviceTypesChanged(scriptUpdateDTO, existingScript)
				|| hasPreConditionsChanged(scriptUpdateDTO, existingScript)
				|| hasTestStepsChanged(scriptUpdateDTO, existingScript);
	}

	/**
	 * Updates only the script file for an existing script without modifying other
	 * script properties.
	 * This method handles the validation and replacement of the script file if a
	 * new file is provided.
	 *
	 * @param scriptFile      the new script file to upload; if empty, no update is
	 *                        performed
	 * @param scriptUpdateDTO the DTO containing script update information
	 *                        (currently unused in this method)
	 * @param script          the existing script entity containing name and
	 *                        location information for validation
	 * @throws RuntimeException if script file validation fails
	 * @throws IOException      if file saving operation fails
	 */
	private void updateScriptFileOnly(MultipartFile scriptFile, ScriptDTO scriptUpdateDTO, Script script) {
		// If the script file is updated, validate and save the new script file
		if (!scriptFile.isEmpty()) {
			this.validateScriptFile(scriptFile, script.getName(), script.getScriptLocation());
			// This will replave the existing file with the new file
			this.saveScriptFile(scriptFile, script.getScriptLocation());
		}

	}

	/**
	 * This method is used to delete the script.
	 * 
	 * @param scriptId - the script
	 * @return - true if the script is deleted successfully, false otherwise
	 */
	@Override
	public boolean deleteScript(UUID scriptId) {
		LOGGER.info("Deleting script: " + scriptId.toString());
		Script script = scriptRepository.findById(scriptId)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.SCRIPT_ID, scriptId.toString()));
		try {
			scriptRepository.delete(script);
			this.deleteScriptFile(script.getName(), script.getScriptLocation());
			LOGGER.info("Script deleted successfully: " + scriptId.toString());
			return true;
		} catch (DataIntegrityViolationException e) {
			LOGGER.error("Error deleting script: " + e.getMessage());
			throw new DeleteFailedException();
		} catch (Exception e) {
			LOGGER.error("Error deleting script: " + e.getMessage());
		}
		return false;
	}

	/**
	 * This method is used to get the list of scripts based on the category.
	 * 
	 */
	@Override
	public List<ScriptListDTO> findAllScriptsByCategory(String categoryName) {
		LOGGER.debug("Getting all scripts based on the category: " + categoryName);
		Category category = commonService.validateCategory(categoryName);
		List<Script> scripts = scriptRepository.findAllByCategory(category);
		List<ScriptListDTO> scriptListDTO = new ArrayList<>();
		for (Script script : scripts) {
			ScriptListDTO scriptDTO = MapperUtils.convertToScriptListDTO(script);
			scriptListDTO.add(scriptDTO);
			LOGGER.debug("Script: " + script.getName() + " added to the list");
		}
		LOGGER.debug("Returning all scripts based on the category: " + categoryName);
		return scriptListDTO;

	}

	/**
	 * This method is used to get the list of scripts based on the module.
	 * 
	 * @param moduleName - the module name
	 * @return - the list of scripts based on the module
	 */
	@Override
	public List<ScriptListDTO> findAllScriptsByModule(String moduleName) {
		LOGGER.debug("Getting all scripts based on the module: " + moduleName);
		Module module = moduleRepository.findByName(moduleName);
		if (module == null) {
			LOGGER.error("Module not found with the name: " + moduleName);
			throw new ResourceNotFoundException(Constants.MODULE, moduleName);
		}

		List<Script> scripts = scriptRepository.findAllByModule(module);
		List<ScriptListDTO> scriptListDTO = new ArrayList<>();
		for (Script script : scripts) {
			ScriptListDTO scriptDTO = MapperUtils.convertToScriptListDTO(script);
			scriptListDTO.add(scriptDTO);
			LOGGER.debug("Script: " + script.getName() + " added to the list");
		}

		return scriptListDTO;
	}

	/**
	 * This method is used to get all the scripts based on the module with the
	 * category
	 * 
	 * @param category - the category
	 * @return - the list of scripts based on the module with the category
	 */
	@Override
	public List<ScriptModuleDTO> findAllScriptByModuleWithCategory(String category) {
		LOGGER.info("Getting all scripts based on the module");

		// Get the category based on the category name
		Category categoryValue = commonService.validateCategory(category);

		// Get all the modules based on the category
		List<Module> modules;

		if (Category.RDKV.equals(categoryValue)) {
			modules = moduleRepository.findAllByCategoryIn(Arrays.asList(Category.RDKV, Category.RDKV_RDKSERVICE));
		} else {
			modules = moduleRepository.findAllByCategory(categoryValue);
		}

		List<ScriptModuleDTO> scriptModuleDTOList = new ArrayList<>();
		// If no modules are found for the category, then throw an exception
		if (modules.isEmpty()) {
			LOGGER.error("No modules found for the category: " + category);
			return null;
		}

		for (Module module : modules) {
			List<ScriptListDTO> scripts = this.findAllScriptsByModule(module.getName());
			ScriptModuleDTO moduleDTO = new ScriptModuleDTO();
			moduleDTO.setModuleId(module.getId());
			moduleDTO.setModuleName(module.getName());
			moduleDTO.setScripts(scripts);
			moduleDTO.setTestGroupName(module.getTestGroup().getName());
			if (!moduleDTO.getScripts().isEmpty()) {
				scriptModuleDTOList.add(moduleDTO);
			}
			LOGGER.info("Module: " + module.getName() + " added to the list");
		}
		return scriptModuleDTOList;
	}

	/**
	 * This method is used to get the script details by testscriptId.
	 * 
	 * @param scriptId - the script id
	 * @return - the script
	 */
	public ScriptDTO findScriptById(UUID scriptId) {
		LOGGER.info("Getting script details by scriptId: " + scriptId);
		Script script = scriptRepository.findById(scriptId)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.SCRIPT_ID, scriptId.toString()));
		ScriptDTO scriptDTO = MapperUtils.convertToScriptDTO(script);
		if (null != script.getDeviceTypes()) {
			scriptDTO.setDeviceTypes(commonService.getDeviceTypesAsStringList(script.getDeviceTypes()));
		}

		// Get the script file based on the script name
		File scriptFile = getPythonFile(script.getName());
		if (!scriptFile.exists()) {
			LOGGER.error("Script file not found: " + script.getName());
			throw new ResourceNotFoundException("Script file", script.getName());
		}
		try {
			// Option 1b: Store script content as base64 encoded string (if necessary)
			String scriptContentBase64 = Files.readString(scriptFile.toPath(), StandardCharsets.UTF_8);
			scriptDTO.setScriptContent(scriptContentBase64);
			return scriptDTO;
		} catch (Exception e) {
			LOGGER.error("Error reading script file: " + e.getMessage());
			throw new TDKServiceException("Error reading script file: " + e.getMessage());
		}
	}

	/**
	 * This method is used to get the script details by testScriptName.
	 * 
	 * @param testScriptName - the module name
	 * @return - the script
	 */

	@Override
	public ByteArrayInputStream testCaseToExcel(String testScriptName) {
		LOGGER.info("Received request to download test case as excel for test script: " + testScriptName);
		Script script = scriptRepository.findByName(testScriptName);
		if (script == null) {
			LOGGER.error("Test script not found with the name: " + testScriptName);
			throw new ResourceNotFoundException(Constants.SCRIPT_NAME, testScriptName);
		}
		List<Script> scripts = new ArrayList<>();
		scripts.add(script);
		return commonService.createExcelFromTestCasesDetailsInScript(scripts, "TEST_CASE_" + testScriptName);
	}

	/**
	 * This method is used to get the script details by module.
	 * 
	 * @param moduleName - the module name
	 * @return - the script
	 */

	@Override
	public ByteArrayInputStream testCaseToExcelByModule(String moduleName) {
		LOGGER.info("Received request to download test case as excel for module: " + moduleName);

		Module module = moduleRepository.findByName(moduleName);
		if (module == null) {
			LOGGER.error("Module not found with the name: " + moduleName);
			throw new ResourceNotFoundException(Constants.MODULE, moduleName);
		}
		List<Script> script = scriptRepository.findAllByModule(module);
		return commonService.createExcelFromTestCasesDetailsInScript(script, "TEST_CASE_" + moduleName);
	}

	/**
	 * Check if the script already exists with the same name or not in the database
	 * 
	 * @param scriptCreateDTO - the script details
	 */
	private void checkIfScriptExists(ScriptCreateDTO scriptCreateDTO) {
		if (scriptRepository.existsByName(scriptCreateDTO.getName())) {
			LOGGER.error("Script already exists with the same name: " + scriptCreateDTO.getName());
			throw new ResourceAlreadyExistsException(Constants.SCRIPT_NAME, scriptCreateDTO.getName());
		}
	}

	/**
	 * Save the script file in the location based on the module and category
	 * 
	 * @param scriptFile     - the script file
	 * @param scriptLocation - the script location
	 */
	private void saveScriptFile(MultipartFile scriptFile, String scriptLocation) {
		try {
			MultipartFile fileWithHeader = commonService.addHeader(scriptFile);
			Path uploadPath = Paths.get(AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + scriptLocation
					+ Constants.FILE_PATH_SEPERATOR);
			if (!Files.exists(uploadPath)) {
				Files.createDirectories(uploadPath);
			}
			Path filePath = uploadPath.resolve(fileWithHeader.getOriginalFilename());
			Files.copy(fileWithHeader.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.info("File uploaded successfully: {}", fileWithHeader.getOriginalFilename());
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Error saving file: " + e.getMessage());
			throw new TDKServiceException("Error saving file: " + e.getMessage());
		} catch (Exception ex) {
			ex.printStackTrace();
			LOGGER.error("Error saving file: ", ex.getMessage());
			throw new TDKServiceException("Error saving file: " + ex.getMessage());

		}
	}

	/**
	 * Delete the script file from the location
	 * 
	 * @param scriptName     - the script name
	 * @param scriptLocation - the script location
	 */
	private void deleteScriptFile(String scriptName, String scriptLocation) {
		try {
			Path uploadPath = Paths.get(AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + scriptLocation
					+ Constants.FILE_PATH_SEPERATOR);
			if (!Files.exists(uploadPath)) {
				Files.createDirectories(uploadPath);
			}
			Path filePath = uploadPath.resolve(scriptName + Constants.PYTHON_FILE_EXTENSION);
			LOGGER.info("Deleting file: " + filePath.toString());
			if (!Files.exists(filePath)) {
				LOGGER.error("File not found: " + filePath.toString());
				return;
			}
			Files.delete(filePath);
			LOGGER.info("File deleted successfully: {}", scriptName);
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Error deleting file: " + e.getMessage());
			throw new TDKServiceException("Error deleting file: " + e.getMessage());
		} catch (Exception ex) {
			ex.printStackTrace();
			LOGGER.error("Error deleting file: ", ex.getMessage());
			throw new TDKServiceException("Error deleting file: " + ex.getMessage());

		}
	}

	/**
	 * Validate the script file before saving it in the location
	 * 
	 * @param scriptFile     - the script file
	 * @param scriptName     - the script details
	 * @param scriptLocation - the script location
	 */
	private void validateScriptFile(MultipartFile scriptFile, String scriptName, String scriptLocation) {
		if (scriptFile.isEmpty()) {
			LOGGER.error("Script file is empty");
			throw new UserInputException("Script file is empty");
		}
		if (!(scriptName + Constants.PYTHON_FILE_EXTENSION).equals(scriptFile.getOriginalFilename())) {
			LOGGER.error("Script name and file name are not same");
			throw new UserInputException("Script name and Script file name are not the same");
		}
		commonService.validatePythonFile(scriptFile);
	}

	/**
	 * Get the list of deviceType based on the deviceType name
	 * 
	 * @param deviceTypes - list of deviceType names
	 * @return deviceTypes - list of deviceTypes
	 */
	private List<DeviceType> getScriptDevicetypes(List<String> deviceTypes, Category category) {
		List<DeviceType> deviceTypeList = new ArrayList<>();
		for (String deviceTypeName : deviceTypes) {
			DeviceType deviceType;
			if (category.equals(Category.RDKV_RDKSERVICE)) {
				deviceType = deviceTypeRepository.findByNameAndCategory(deviceTypeName, Category.RDKV);
			} else {
				deviceType = deviceTypeRepository.findByNameAndCategory(deviceTypeName, category);
			}
			if (null != deviceType) {
				deviceTypeList.add(deviceType);
			} else {
				LOGGER.error("deviceType not found with the name: " + deviceTypeName);
				throw new ResourceNotFoundException(Constants.DEVICE_TYPE, deviceTypeName);
			}
		}
		return deviceTypeList;
	}

	/**
	 * Get the script location path without base location based on the module and
	 * category
	 * 
	 * @param module   the module
	 * @param category the category
	 * @return filePath - script location path without t
	 */
	private String getScriptLocation(Module module, Category category) {
		LOGGER.info("Getting script location based on module and category: " + module.getName() + " - "
				+ category.getName());
		String filePath = "";
		String categoryFolderName = commonService.getFolderBasedOnCategory(category);
		String moduleFolderName = module.getName();
		String testGroupFolderName = commonService.getFolderBasedOnModuleType(module.getTestGroup());
		if (!Utils.isEmpty(categoryFolderName) && !Utils.isEmpty(moduleFolderName)
				&& !Utils.isEmpty(testGroupFolderName)) {
			filePath = String.format("%s/%s/%s", categoryFolderName, testGroupFolderName, moduleFolderName);
		} else {
			LOGGER.error("Invalid folder names: categoryFolderName: " + categoryFolderName + " moduleFolderName: "
					+ moduleFolderName + " testGroupFolderName: " + testGroupFolderName);
			throw new TDKServiceException("Invalid folder names: categoryFolderName: " + categoryFolderName
					+ " moduleFolderName: " + moduleFolderName + " testGroupFolderName: " + testGroupFolderName);
		}
		return filePath;

	}

	/**
	 * Get the category based on the module
	 * 
	 * @param module the primitive test
	 * @return category - RDKV, RDKB, RDKC, RDKV_RDKSERVICE
	 */
	private Category getCategoryBasedOnModule(Module module) {
		LOGGER.info("Getting category based on module: " + module.getName());
		if (module.getCategory().equals(Category.RDKV_RDKSERVICE)) {
			return Category.RDKV_RDKSERVICE;
		} else if (module.getCategory().equals(Category.RDKV)) {
			return Category.RDKV;
		} else if (module.getCategory().equals(Category.RDKB)) {
			return Category.RDKB;
		} else if (module.getCategory().equals(Category.RDKC)) {
			return Category.RDKC;
		} else {
			throw new TDKServiceException("Category is not found for the module: " + module.getName());
		}
	}

	/**
	 * Generate a ZIP file containing a Python script and an XML file with the test
	 * case details
	 * 
	 * @param scriptName - the script name
	 * @return - the ZIP file as a byte array
	 * @throws IOException - the IO exception
	 */

	@Override
	public byte[] generateScriptZip(String scriptName) {
		String xmlContent;
		File getPythonScriptFile;
		xmlContent = generateTestCaseXml(scriptName);
		getPythonScriptFile = getPythonFile(scriptName);

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
			ZipEntry xmlEntry = new ZipEntry(scriptName + Constants.XML_FILE_EXTENSION);
			zipOutputStream.putNextEntry(xmlEntry);
			zipOutputStream.write(xmlContent.getBytes());
			zipOutputStream.closeEntry();

			ZipEntry scriptEntry = new ZipEntry(getPythonScriptFile.getName());
			zipOutputStream.putNextEntry(scriptEntry);
			Files.copy(getPythonScriptFile.toPath(), zipOutputStream);
			zipOutputStream.closeEntry();
		} catch (FileNotFoundException e) {
			LOGGER.error("File not found: " + e.getMessage());
			throw new TDKServiceException("File not found: " + e.getMessage());
		} catch (ZipException e) {
			LOGGER.error("Error processing zip file: " + e.getMessage());
			throw new TDKServiceException("Error processing zip file: " + e.getMessage());
		} catch (IOException e) {
			LOGGER.error("IO error: " + e.getMessage());
			throw new TDKServiceException("IO error: " + e.getMessage());
		}

		return byteArrayOutputStream.toByteArray();
	}

	/**
	 * Get the python file based on the script name
	 * 
	 * @param scriptName - the script name
	 * @return - the python file
	 */

	public File getPythonFile(String scriptName) {
		Script testCase = scriptRepository.findByName(scriptName);
		if (testCase == null) {
			LOGGER.error("Test script not found with the name: " + scriptName);
			throw new ResourceNotFoundException(Constants.SCRIPT_NAME, scriptName);
		}
		Path scriptFilePath = Paths
				.get(AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + testCase.getScriptLocation()
						+ Constants.FILE_PATH_SEPERATOR + scriptName + Constants.PYTHON_FILE_EXTENSION);
		File scriptFile = scriptFilePath.toFile();
		if (!scriptFile.exists()) {
			throw new ResourceNotFoundException("Script file", scriptName);
		}
		return scriptFile;

	}

	/**
	 * Generate test case XML based on the test script name
	 * 
	 * @param scriptName - the script name
	 * @return - the test case XML
	 */

	public String generateTestCaseXml(String scriptName) {

		Script script = scriptRepository.findByName(scriptName);
		if (script == null) {
			LOGGER.error("Test script not found with the name: " + scriptName);
			throw new ResourceNotFoundException(Constants.SCRIPT_NAME, scriptName);
		}
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			var doc = docBuilder.newDocument();
			var rootElement = doc.createElement(Constants.XML);
			doc.appendChild(rootElement);

			createElement(doc, rootElement, "name", script.getName());
			createElement(doc, rootElement, "script_id", script.getId().toString());

			createElement(doc, rootElement, "primitive_test_name", script.getPrimitiveTest().getName());
			createElement(doc, rootElement, "script_location", script.getScriptLocation());
			createElement(doc, rootElement, "module", script.getModule().getName());
			createElement(doc, rootElement, "synopsis", script.getSynopsis());

			String executionTime = String.valueOf(script.getExecutionTimeOut());
			if (executionTime != null && !executionTime.isEmpty()) {
				createElement(doc, rootElement, "execution_time", executionTime);
			}
			String longDuration = String.valueOf(script.isLongDuration());
			if (longDuration != null && !longDuration.isEmpty()) {
				createElement(doc, rootElement, "long_duration", longDuration);
			}
			String skip = String.valueOf(script.isSkipExecution());
			if (skip != null && !skip.isEmpty()) {
				createElement(doc, rootElement, "skip", skip);
			}

			createElement(doc, rootElement, "test_case_id", script.getTestId());
			createElement(doc, rootElement, "test_objective", script.getObjective());

			// Create <box_types> element and add <box_type> child elements
			Element deviceTypesElement = doc.createElement("device_types");
			rootElement.appendChild(deviceTypesElement);

			// Loop through the list of box types and create <box_type> elements
			for (DeviceType deviceType : script.getDeviceTypes()) {
				createElement(doc, deviceTypesElement, "device_type", deviceType.getName());
			}
			Element preConditionElement = doc.createElement("pre_conditions");
			rootElement.appendChild(preConditionElement);
			for (PreCondition preCondition : script.getPreConditions()) {
				createElement(doc, rootElement, "pre_condition", preCondition.getPreConditionDescription());
			}
			Element testStepElements = doc.createElement("test_steps");
			rootElement.appendChild(testStepElements);
			for (TestStep testStep : script.getTestSteps()) {
				Element testStepElement = doc.createElement("test_step");
				testStepElements.appendChild(testStepElement);
				createElement(doc, testStepElement, "step_name", testStep.getStepName());
				createElement(doc, testStepElement, "step_description", testStep.getStepDescription());
				createElement(doc, testStepElement, "expected_result", testStep.getExpectedResult());
			}

			createElement(doc, rootElement, "priority", script.getPriority());
			createElement(doc, rootElement, "release_version", script.getReleaseVersion());

			// Convert the document to a String
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, Constants.YES);
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, Constants.NO);
			transformer.setOutputProperty(OutputKeys.METHOD, Constants.XML);

			DOMSource domSource = new DOMSource(doc);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			StreamResult result = new StreamResult(outputStream);
			transformer.transform(domSource, result);
			return outputStream.toString();
		} catch (Exception e) {
			LOGGER.error("Error generating test case XML: " + e.getMessage());
			throw new TDKServiceException("Error generating test case XML: " + e.getMessage());
		}

	}

	/**
	 * Upload a ZIP or TAR.GZ file containing a Python script and an XML file with
	 * the script
	 * details
	 * 
	 * @param file - the ZIP or TAR.GZ file
	 * @return true if the file is uploaded successfully, false otherwise
	 * @throws IOException
	 */
	@Override
	public boolean uploadZipFile(MultipartFile file) throws IOException {
		LOGGER.info("Uploading archive file: " + file.getOriginalFilename());

		String fileName = file.getOriginalFilename();
		boolean isZipFile = false;
		boolean isTarGzFile = false;

		// Validate file type
		if ((file.getContentType() != null && file.getContentType().equals("application/zip"))
				|| (fileName != null && fileName.endsWith(Constants.ZIP_EXTENSION))) {
			isZipFile = true;
		} else if ((file.getContentType() != null &&
				(file.getContentType().equals("application/gzip") ||
						file.getContentType().equals("application/x-gzip") ||
						file.getContentType().equals("application/x-tar") ||
						file.getContentType().equals("application/x-compressed-tar")))
				|| (fileName != null &&
						(fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")))) {
			isTarGzFile = true;
		} else {
			LOGGER.error("Only ZIP and TAR.GZ files are allowed");
			throw new UserInputException("Only ZIP and TAR.GZ files are allowed");
		}

		MultipartFile pythonFile = null;
		ScriptCreateDTO scriptCreateDTO = null;
		String scriptId = null;

		if (isZipFile) {
			// Handle ZIP file processing
			File tempZipFile = File.createTempFile("uploaded-", Constants.ZIP_EXTENSION);
			file.transferTo(tempZipFile);

			try (ZipFile zip = new ZipFile(tempZipFile)) {
				boolean[] fileExists = validateAndExtractFromZip(zip);
				if (!fileExists[0] || !fileExists[1]) {
					LOGGER.error("Both Python and XML files are required in the archive file.");
					throw new UserInputException("Both Python and XML files are required in the archive file.");
				}

				Object[] extractedFiles = extractFilesFromZip(zip);
				pythonFile = (MultipartFile) extractedFiles[0];
				scriptCreateDTO = (ScriptCreateDTO) extractedFiles[1];
				scriptId = (String) extractedFiles[2];

			} finally {
				tempZipFile.delete();
			}

		} else if (isTarGzFile) {
			// Handle TAR.GZ file processing
			try (InputStream fileInputStream = file.getInputStream();
					GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
					TarArchiveInputStream tarInputStream = new TarArchiveInputStream(gzipInputStream)) {

				boolean[] fileExists = validateAndExtractFromTar(tarInputStream);
				if (!fileExists[0] || !fileExists[1]) {
					LOGGER.error("Both Python and XML files are required in the archive file.");
					throw new UserInputException("Both Python and XML files are required in the archive file.");
				}

				// Reset stream for actual extraction
				try (InputStream fileInputStream2 = file.getInputStream();
						GZIPInputStream gzipInputStream2 = new GZIPInputStream(fileInputStream2);
						TarArchiveInputStream tarInputStream2 = new TarArchiveInputStream(gzipInputStream2)) {

					Object[] extractedFiles = extractFilesFromTar(tarInputStream2);
					pythonFile = (MultipartFile) extractedFiles[0];
					scriptCreateDTO = (ScriptCreateDTO) extractedFiles[1];
					scriptId = (String) extractedFiles[2];
				}
			}
		}

		// Process the extracted files (common logic for both ZIP and TAR.GZ)
		if (pythonFile != null && scriptCreateDTO != null) {
			String scriptName = scriptCreateDTO.getName();
			Script script = scriptRepository.findByName(scriptName);
			boolean saveOrUpdateScript = false;

			if (script == null) {
				LOGGER.info("Script going to be saved as new: " + scriptName);
				saveOrUpdateScript = saveNewScript(pythonFile, scriptCreateDTO, scriptId);
			} else {
				LOGGER.info("Script going to be updated: " + scriptName);
				ScriptDTO scriptDTO = MapperUtils.convertToScriptDTOForXMLUpdate(scriptCreateDTO, script);
				saveOrUpdateScript = updateScriptFromXML(pythonFile, scriptDTO, script);
			}

			LOGGER.info("Script processed successfully");
			return saveOrUpdateScript;
		} else {
			LOGGER.error(
					"Error processing the archive file, either the XML or Python file was not processed correctly.");
			throw new TDKServiceException("Error processing the archive file: XML or Python file processing failed.");
		}
	}

	/**
	 * Validates and extracts information from a ZIP file to check for required file
	 * types.
	 * 
	 * This method iterates through all entries in the provided ZIP file and checks
	 * for the
	 * presence of Python (.py) and XML files based on their file extensions.
	 * 
	 * @param zip the ZipFile to validate and extract information from
	 * @return a boolean array where the first element indicates if at least one
	 *         Python file
	 *         exists in the ZIP, and the second element indicates if at least one
	 *         XML file
	 *         exists in the ZIP
	 */
	private boolean[] validateAndExtractFromZip(ZipFile zip) {
		boolean pythonFileExists = false;
		boolean xmlFileExists = false;

		Enumeration<? extends ZipEntry> entries = zip.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			if (entry.getName().endsWith(Constants.PYTHON_FILE_EXTENSION)) {
				pythonFileExists = true;
			}
			if (entry.getName().endsWith(Constants.XML_FILE_EXTENSION)) {
				xmlFileExists = true;
			}
		}

		return new boolean[] { pythonFileExists, xmlFileExists };
	}

	/**
	 * Validates and extracts information from a TAR.GZ file to check for required
	 * file types.
	 * 
	 * This method iterates through all entries in the provided TAR.GZ input stream
	 * and checks for the
	 * presence of Python (.py) and XML files based on their file extensions.
	 * 
	 * @param tarInputStream the TarArchiveInputStream to validate and extract
	 *                       information from
	 * @return a boolean array where the first element indicates if at least one
	 *         Python file
	 *         exists in the TAR.GZ, and the second element indicates if at least
	 *         one XML file
	 *         exists in the TAR.GZ
	 * @throws IOException if an I/O error occurs while reading the TAR.GZ input
	 *                     stream
	 */
	private boolean[] validateAndExtractFromTar(TarArchiveInputStream tarInputStream) throws IOException {
		boolean pythonFileExists = false;
		boolean xmlFileExists = false;

		TarArchiveEntry entry;
		while ((entry = tarInputStream.getNextTarEntry()) != null) {
			if (entry.getName().endsWith(Constants.PYTHON_FILE_EXTENSION)) {
				pythonFileExists = true;
			}
			if (entry.getName().endsWith(Constants.XML_FILE_EXTENSION)) {
				xmlFileExists = true;
			}
		}

		return new boolean[] { pythonFileExists, xmlFileExists };
	}

	/**
	 * Extracts the Python and XML files from a ZIP file.
	 * 
	 * @param zip the ZipFile to extract files from
	 * @return an array containing the extracted Python MultipartFile,
	 *         ScriptCreateDTO, and script ID
	 * @throws IOException if an I/O error occurs while reading the ZIP file
	 */
	private Object[] extractFilesFromZip(ZipFile zip) throws IOException {
		MultipartFile pythonFile = null;
		ScriptCreateDTO scriptCreateDTO = null;
		String scriptId = null;

		Enumeration<? extends ZipEntry> entries = zip.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();

			if (entry.getName().endsWith(Constants.PYTHON_FILE_EXTENSION)) {
				try (InputStream pyInputStream = zip.getInputStream(entry)) {
					String[] parts = entry.getName().split("/");
					String fileName = parts[parts.length - 1];
					pythonFile = convertScriptFileToMultipartFile(pyInputStream, fileName);
				}
			} else if (entry.getName().endsWith(Constants.XML_FILE_EXTENSION)) {
				try (InputStream xmlInputStream = zip.getInputStream(entry)) {
					byte[] xmlBytes = xmlInputStream.readAllBytes();
					scriptId = extractScriptIDFromXml(new ByteArrayInputStream(xmlBytes));
					scriptCreateDTO = convertXmlToScriptCreateDTO(new ByteArrayInputStream(xmlBytes));
				}
			}
		}

		return new Object[] { pythonFile, scriptCreateDTO, scriptId };
	}

	/**
	 * Extracts the Python and XML files from a TAR.GZ input stream.
	 * 
	 * @param tarInputStream the TarArchiveInputStream to extract files from
	 * @return an array containing the extracted Python MultipartFile,
	 *         ScriptCreateDTO, and script ID
	 * @throws IOException if an I/O error occurs while reading the TAR.GZ input
	 *                     stream
	 */
	private Object[] extractFilesFromTar(TarArchiveInputStream tarInputStream) throws IOException {
		MultipartFile pythonFile = null;
		ScriptCreateDTO scriptCreateDTO = null;
		String scriptId = null;

		TarArchiveEntry entry;
		while ((entry = tarInputStream.getNextTarEntry()) != null) {
			if (entry.isFile()) {
				if (entry.getName().endsWith(Constants.PYTHON_FILE_EXTENSION)) {
					String[] parts = entry.getName().split("/");
					String fileName = parts[parts.length - 1];

					// Read all bytes from the tar entry properly
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					byte[] buffer = new byte[8192];
					int bytesRead;
					while ((bytesRead = tarInputStream.read(buffer)) != -1) {
						outputStream.write(buffer, 0, bytesRead);
					}
					byte[] fileContent = outputStream.toByteArray();

					pythonFile = convertScriptFileToMultipartFile(
							new ByteArrayInputStream(fileContent), fileName);

				} else if (entry.getName().endsWith(Constants.XML_FILE_EXTENSION)) {
					// Read all bytes from the tar entry properly
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					byte[] buffer = new byte[8192];
					int bytesRead;
					while ((bytesRead = tarInputStream.read(buffer)) != -1) {
						outputStream.write(buffer, 0, bytesRead);
					}
					byte[] xmlBytes = outputStream.toByteArray();

					scriptId = extractScriptIDFromXml(new ByteArrayInputStream(xmlBytes));
					scriptCreateDTO = convertXmlToScriptCreateDTO(new ByteArrayInputStream(xmlBytes));
				}
			}
		}

		return new Object[] { pythonFile, scriptCreateDTO, scriptId };
	}

	/**
	 * Extracts the Script ID from the XML input stream.
	 * 
	 * @param xmlInputStream the XML input stream
	 * @return the Script ID
	 */
	public String extractScriptIDFromXml(InputStream xmlInputStream) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(xmlInputStream);
			document.getDocumentElement().normalize();

			NodeList idNodes = document.getElementsByTagName("script_id");
			if (idNodes.getLength() > 0) {
				return idNodes.item(0).getTextContent();
			} else {
				throw new IllegalArgumentException("Script ID not found in XML.");
			}
		} catch (Exception e) {
			// Log the error and rethrow as a runtime exception or handle as needed
			LOGGER.error("Error extracting Script ID from XML: {}", e.getMessage());
			throw new UserInputException("Failed to extract Script ID from XML.");
		}
	}

	/**
	 * Convert the XML file to ScriptCreateDTO
	 * 
	 * @param xmlInputStream - the XML input stream
	 * @return - the ScriptCreateDTO
	 */
	private ScriptCreateDTO convertXmlToScriptCreateDTO(InputStream xmlInputStream) {

		ScriptCreateDTO testCase = null;
		// Parse XML
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(xmlInputStream);
			ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
			Validator validator = validatorFactory.getValidator();

			// Normalize XML structure
			document.getDocumentElement().normalize();

			// Extract fields from XML
			NodeList nodeList = document.getElementsByTagName(Constants.XML);
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);

				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element) node;

					testCase = new ScriptCreateDTO();
					testCase.setName(getElementValue(element, "name"));
					testCase.setPrimitiveTestName(getElementValue(element, "primitive_test_name"));
					testCase.setExecutionTimeOut(Integer.parseInt(getElementValue(element, "execution_time")));
					testCase.setLongDuration(Boolean.parseBoolean(getElementValue(element, "long_duration")));
					testCase.setSkipExecution(Boolean.parseBoolean(getElementValue(element, "skip")));
					testCase.setSynopsis(getElementValue(element, "synopsis"));
					testCase.setTestId(getElementValue(element, "test_case_id"));
					testCase.setObjective(getElementValue(element, "test_objective"));

					List<String> preConditionList = new ArrayList<>();
					NodeList preConditions = element.getElementsByTagName("pre_condition");
					for (int j = 0; j < preConditions.getLength(); j++) {
						preConditionList.add(preConditions.item(j).getTextContent());
					}
					testCase.setPreConditions(preConditionList);

					List<TestStepCreateDTO> testSteps = new ArrayList<>();
					NodeList testStepsNodes = element.getElementsByTagName("test_step");
					for (int j = 0; j < testStepsNodes.getLength(); j++) {
						Element testStepElement = (Element) testStepsNodes.item(j);
						TestStepCreateDTO testStep = new TestStepCreateDTO();
						testStep.setStepName(getElementValue(testStepElement, "step_name"));
						testStep.setStepDescription(getElementValue(testStepElement, "step_description"));
						testStep.setExpectedResult(getElementValue(testStepElement, "expected_result"));
						testSteps.add(testStep);
					}
					testCase.setTestSteps(testSteps);

					testCase.setPriority(getElementValue(element, "priority"));
					testCase.setReleaseVersion(getElementValue(element, "release_version"));

					// Handle box_types
					List<String> deviceType = new ArrayList<>();
					NodeList deviceTypeNodes = element.getElementsByTagName("device_type");
					for (int j = 0; j < deviceTypeNodes.getLength(); j++) {
						deviceType.add(deviceTypeNodes.item(j).getTextContent());
					}
					testCase.setDeviceTypes(deviceType);
					validateAndCreateScript(testCase, validator);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error parsing XML and saving to database: " + e.getMessage());
			throw new UserInputException("Error parsing XML and saving to database: " + e.getMessage());
		}
		return testCase;

	}

	/**
	 * Validate the script details before saving
	 * 
	 * @param scriptCreateDTO - the script details
	 * @param validator       - the validator
	 */
	private void validateAndCreateScript(ScriptCreateDTO scriptCreateDTO, Validator validator) {
		Set<ConstraintViolation<ScriptCreateDTO>> violations = validator.validate(scriptCreateDTO);
		if (!violations.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (ConstraintViolation<ScriptCreateDTO> violation : violations) {
				sb.append(violation.getMessage()).append(",");
			}
			LOGGER.error("Validation errors: \n" + sb.toString());
			throw new IllegalArgumentException("Validation errors: " + sb.toString());
		}

	}

	/**
	 * Convert the script file to MultipartFile
	 * 
	 * @param inputStream - the input stream
	 * @param fileName    - the file name
	 * @return - the MultipartFile
	 * @throws IOException
	 */
	private MultipartFile convertScriptFileToMultipartFile(InputStream inputStream, String fileName) {
		try {
			LOGGER.info("Converting script file to MultipartFile: " + fileName);
			// Read the input stream and store the content in a byte array
			byte[] fileContent = inputStream.readAllBytes();

			// Create a new MultipartFile (MockMultipartFile in this case)
			return new MockMultipartFile(fileName, fileName, Constants.PYTHON_CONTENT, fileContent);
		} catch (Exception e) {
			LOGGER.error("Error converting script file to MultipartFile: " + e.getMessage());
			throw new TDKServiceException("Error converting script file to MultipartFile: " + e.getMessage());
		}
	}

	/**
	 * Create an element with the tag name and text content
	 * 
	 * @param doc         - the document
	 * @param parent      - the parent element
	 * @param tagName     - the tag name
	 * @param textContent - the text content
	 */
	private void createElement(Document doc, Element parent, String tagName, String textContent) {
		Element element = doc.createElement(tagName);
		element.setTextContent(textContent);
		parent.appendChild(element);
	}

	/**
	 * Get the element value based on the tag name
	 * 
	 * @param parent  - the parent element
	 * @param tagName - the tag name
	 * @return - the element value
	 */
	private String getElementValue(Element parent, String tagName) {
		NodeList nodeList = parent.getElementsByTagName(tagName);
		if (nodeList.getLength() > 0) {
			return nodeList.item(0).getTextContent();
		}
		return null;
	}

	/**
	 * This method is used to get the script template details by primitiveTestName.
	 *
	 * @param primitiveTestName - the primitive test name
	 * @return - the script
	 */
	@Override
	public String scriptTemplate(String primitiveTestName) {

		StringBuilder scriptBuilder = new StringBuilder();

		if (primitiveTestName == null || primitiveTestName.isEmpty()) {
			throw new MandatoryFieldException("Primitive test name cannot be null or empty");
		}

		PrimitiveTest primitiveTest = primitiveTestRepository.findByName(primitiveTestName);
		if (primitiveTest == null) {
			throw new ResourceNotFoundException("Primitive test name: ", primitiveTestName);
		}

		scriptBuilder.append("# use tdklib library,which provides a wrapper for tdk testcase script \r\n")
				.append("import tdklib; \r\n\r\n").append("#Test component to be tested\r\n")
				.append("obj = tdklib.TDKScriptingLibrary(\"").append(primitiveTest.getModule().getName())
				.append("\",\"1\");\r\n\r\n").append("#IP and Port of device type, No need to change,\r\n")
				.append("#This will be replaced with corresponding DUT Ip and port while executing script\r\n")
				.append("ip = <ipaddress>\r\n").append("port = <port>\r\n")
				.append("obj.configureTestCase(ip,port,'');\r\n\r\n")
				.append("#Get the result of connection with test component and DUT\r\n")
				.append("result = obj.getLoadModuleResult();\r\n")
				.append("print(\"[LIB LOAD STATUS]  :  %s\" %result);\r\n\r\n")
				.append("#Prmitive test case which associated to this Script\r\n")
				.append("tdkTestObj = obj.createTestStep('").append(primitiveTestName).append("');\r\n\r\n")
				.append("#Execute the test case in DUT\r\n").append("tdkTestObj.executeTestCase(\"\");\r\n\r\n")
				.append("#Get the result of execution\r\n").append("result = tdkTestObj.getResult();\r\n")
				.append("print(\"[TEST EXECUTION RESULT] : %s\" %result);\r\n\r\n")
				.append("#Set the result status of execution\r\n")
				.append("tdkTestObj.setResultStatus(\"none\");\r\n\r\n").append("obj.unloadModule(\"")
				.append(primitiveTest.getModule().getName()).append("\");");

		return scriptBuilder.toString();
	}

	/**
	 * This method is used to get the Excel of scripts by module based on the
	 * category.
	 * 
	 * @param categoryName - the category name
	 */
	@Override
	public ByteArrayInputStream testCaseToExcelByCategory(String category) {
		LOGGER.info("Received request to download test case as excel for module and category: " + category);
		// Validate the category
		Category categoryValue = commonService.validateCategory(category);
		// Get all the modules based on the category
		List<Module> modules;
		if (Category.RDKV.equals(categoryValue)) {
			modules = moduleRepository.findAllByCategoryIn(Arrays.asList(Category.RDKV, Category.RDKV_RDKSERVICE));
		} else {
			modules = moduleRepository.findAllByCategory(categoryValue);
		}
		if (modules.isEmpty() || modules == null) {
			LOGGER.error("No modules found for the category: " + category);
			throw new ResourceNotFoundException("Modules for", category);
		}

		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
			for (Module module : modules) {
				try {
					ByteArrayInputStream byteArrayInputStream = testCaseToExcelByModule(module.getName());
					ZipEntry zipEntry = new ZipEntry(module.getName() + Constants.EXCEL_FILE_EXTENSION);
					zipOutputStream.putNextEntry(zipEntry);
					byte[] bytes = byteArrayInputStream.readAllBytes();
					zipOutputStream.write(bytes);
					zipOutputStream.closeEntry();
				} catch (Exception e) {
					LOGGER.error("Error creating ZIP entry for module: " + module.getName(), e);
					throw new TDKServiceException("Error creating ZIP entry for module: " + e.getMessage());
				}
			}

			zipOutputStream.finish();
			return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

		} catch (Exception e) {
			LOGGER.error("Error creating ZIP file: " + e.getMessage(), e);
			throw new TDKServiceException("Error creating ZIP file: " + e.getMessage());
		}
	}

	/**
	 * This method is used to get all the scripts based on the module with the
	 * category
	 * 
	 * @param category - the category
	 * @return - the list of scripts based on the module with the category
	 */
	public List<ScriptModuleDTO> findAllScriptByModuleWithCategoryWise(String category) {
		LOGGER.info("Getting all scripts based on the module");

		// Get the category based on the category name
		Category categoryValue = commonService.validateCategory(category);

		// Get all the modules based on the category
		List<Module> modules = moduleRepository.findAllByCategory(categoryValue);

		List<ScriptModuleDTO> scriptModuleDTOList = new ArrayList<>();
		// If no modules are found for the category, then throw an exceptiontttt
		if (modules.isEmpty()) {
			LOGGER.error("No modules found for the category: " + category);
			return null;
		}

		for (Module module : modules) {
			List<ScriptListDTO> scripts = this.findAllScriptsByModule(module.getName());
			ScriptModuleDTO moduleDTO = new ScriptModuleDTO();
			moduleDTO.setModuleId(module.getId());
			moduleDTO.setModuleName(module.getName());
			moduleDTO.setScripts(scripts);
			moduleDTO.setTestGroupName(module.getTestGroup().getName());
			if (!moduleDTO.getScripts().isEmpty()) {
				scriptModuleDTOList.add(moduleDTO);
			}
			LOGGER.info("Module: " + module.getName() + " added to the list");
		}
		return scriptModuleDTOList;
	}

	/*
	 * This method is used to create default test suite for existing module and also
	 * to update the test suite if it already exists with the new scripts
	 */
	@Override
	public void defaultTestSuiteCreationForExistingModule() {
		String[] categories = { "RDKV", "RDKB", "RDKV_RDKSERVICE" };

		for (String category : categories) {

			List<ScriptModuleDTO> scripts = findAllScriptByModuleWithCategoryWise(category);
			if (scripts != null && !scripts.isEmpty()) {
				for (ScriptModuleDTO scriptModuleDTO : scripts) {

					TestSuiteCreateDTO testSuiteCerateDTO = new TestSuiteCreateDTO();
					testSuiteCerateDTO.setName(scriptModuleDTO.getModuleName());
					testSuiteCerateDTO.setDescription("TestSuite For " + scriptModuleDTO.getModuleName());
					testSuiteCerateDTO.setCategory(category);
					testSuiteCerateDTO.setScripts(scriptModuleDTO.getScripts());
					TestSuite testSuite = testSuiteRepository.findByName(testSuiteCerateDTO.getName());
					if (testSuite == null) {
						testSuiteService.createTestSuite(testSuiteCerateDTO);
					} else {
						// Check if script count differs before updating
						int existingScriptCount = testSuite.getScriptTestSuite().size();
						int currentScriptCount = scriptModuleDTO.getScripts().size();

						if (existingScriptCount != currentScriptCount) {
							TestSuiteDTO testSuiteDTO = new TestSuiteDTO();
							testSuiteDTO.setId(testSuite.getId());
							testSuiteDTO.setName(scriptModuleDTO.getModuleName());
							testSuiteDTO.setDescription(testSuite.getDescription());
							testSuiteDTO.setCategory(category);
							testSuiteDTO.setScripts(scriptModuleDTO.getScripts());
							testSuiteService.updateTestSuite(testSuiteDTO);
							LOGGER.info("Test suite updated for module: {} - Script count changed from {} to {}",
									scriptModuleDTO.getModuleName(), existingScriptCount, currentScriptCount);
						} else {
							LOGGER.info(
									"Test suite for module: {} already has the same script count ({}), skipping update",
									scriptModuleDTO.getModuleName(), existingScriptCount);
						}
					}

				}
			}
		}
	}

	/**
	 * This method is used to get the list of script names based on the category
	 *
	 * @param category - the category
	 * @return - the list of script names
	 */
	public List<ScriptDetailsResponse> getListofScriptNamesByCategory(String category, boolean isThunderEnabled) {
		LOGGER.info("Fetching script names for category: {} with Thunder enabled: {}", category, isThunderEnabled);

		if (!category.equalsIgnoreCase("RDKV") && !category.equalsIgnoreCase("RDKB")
				&& !category.equalsIgnoreCase("RDKC")) {
			LOGGER.error("Invalid category: {}", category);
			throw new UserInputException("Invalid category: " + category);
		}
		List<Script> scripts = new ArrayList<>();

		if (isThunderEnabled) {
			if (!category.equalsIgnoreCase(Category.RDKV.name())) {
				LOGGER.error("The category {} cannot be thunder enabled", category);
				throw new UserInputException("The category " + category + " cannot be thunder enabled");
			}
			scripts = scriptRepository.findAllByCategory(Category.RDKV_RDKSERVICE);
		} else {
			if (category.equalsIgnoreCase(Category.RDKV.name())) {
				scripts = scriptRepository.findAllByCategory(Category.valueOf(category));
			} else if (category.equalsIgnoreCase(Category.RDKB.name())) {
				scripts = scriptRepository.findAllByCategory(Category.valueOf(category));
			} else if (category.equalsIgnoreCase(Category.RDKC.name())) {
				scripts = scriptRepository.findAllByCategory(Category.valueOf(category));
			}
		}

		// Sort scripts by module name
		scripts.sort(Comparator.comparing(script -> script.getModule().getName()));

		return scripts.stream().map(MapperUtils::convertToScriptDetailsResponse).collect(Collectors.toList());
	}

	/**
	 * This method is used to get the module for the given script name
	 * 
	 * @param scriptName - the script
	 * @return - the module - the module entity
	 */
	@Override
	public Module getModuleByScriptName(String scriptName) {
		LOGGER.info("Getting module by script name: " + scriptName);
		Script script = scriptRepository.findByName(scriptName);
		if (script == null) {
			LOGGER.error("Script not found with the name: " + scriptName);
			return null;
		}
		return script.getModule();
	}

	/**
	 * This method is used to create a markdown file for the given script name
	 * 
	 * @param scriptNme - the script name
	 * @return - the markdown file
	 */

	@Override
	public ByteArrayInputStream createMarkdownFile(String scriptNme) {
		Script testCase = scriptRepository.findByName(scriptNme);
		return generateMarkdownFile(testCase);
	}

	/**
	 * This method is used to create a markdown file for the given script ID
	 * 
	 * @param scriptId - the script ID
	 * @return - the markdown file
	 */
	@Override
	public ByteArrayInputStream createMarkdownFilebyScriptId(UUID scriptId) {
		Script testCase = scriptRepository.findById(scriptId)
				.orElseThrow(() -> new ResourceNotFoundException("Script not found with id: ", scriptId.toString()));
		return generateMarkdownFile(testCase);
	}

	/**
	 * This method is used to generate a markdown file for the given script
	 * 
	 * @param script - the script
	 * @return - the markdown file
	 */
	private ByteArrayInputStream generateMarkdownFile(Script script) {

		LOGGER.info("Generating markdown file for script: {}", script.getName());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		StringBuilder writer = new StringBuilder();
		writer.append("## TestCase ID\n").append(script.getTestId()).append("\n").append("## TestCase Name\n")
				.append(script.getName()).append("\n").append("<a name=\"head.TOC\"></a>\n## Table Of Contents\n")
				.append("- [Objective](#head.Objective)\n").append("- [Precondition](#head.Precondition)\n")
				.append("- [Test Steps](#head.TestSteps)\n").append("- [Test Attributes](#head.Attributes)\n\n")
				.append("<a name=\"head.Objective\"></a>\n## Objective\n").append(script.getObjective()).append("\n\n")
				.append("<a name=\"head.Precondition\"></a>\n## Preconditions\n")
				.append("|#|Conditions|\n|-|----------|\n");

		List<PreCondition> preConditions = script.getPreConditions();
		for (int i = 0; i < preConditions.size(); i++) {
			writer.append("|").append(i + 1).append("|").append(preConditions.get(i).getPreConditionDescription())
					.append("|\n");
		}

		writer.append("\n<a name=\"head.TestSteps\"></a>\n## Test Steps\n\n").append(
				"|#|StepName | Step Description| Expected Result|\n|-|---------|-----------------|----------------|\n");

		List<TestStep> testSteps = script.getTestSteps();
		for (int i = 0; i < testSteps.size(); i++) {
			TestStep step = testSteps.get(i);
			writer.append("| ").append(i + 1).append(" | ").append(step.getStepName()).append(" | ")
					.append(step.getStepDescription()).append(" | ").append(step.getExpectedResult()).append(" |\n");
		}

		writer.append("\n<a name=\"head.Attributes\"></a>\n## Test Attributes\n\n").append("**Supported Models** : ");
		List<DeviceType> deviceTypes = script.getDeviceTypes();
		for (int i = 0; i < deviceTypes.size(); i++) {
			writer.append(deviceTypes.get(i).getName());
			if (i < deviceTypes.size() - 1) {
				writer.append(", ");
			}
		}

		writer.append("\n\n**Estimated duration** : ").append(script.getExecutionTimeOut())
				.append("\n\n**Priority** : ").append(script.getPriority()).append("\n\n**Release Version** : ")
				.append(script.getReleaseVersion())
				.append("<div align=\"right\"><sup>[Go To Top](#head.TOC)</sup></div>\n");

		try {
			// Write the content to the output stream
			out.write(writer.toString().getBytes(StandardCharsets.UTF_8));
			out.flush();
		} catch (IOException e) {
			LOGGER.error("Error generating markdown file: " + e.getMessage());
			throw new TDKServiceException("Error generating markdown file: " + e.getMessage());
		}
		return new ByteArrayInputStream(out.toByteArray());
	}

	/**
	 * This method is used to get the list of scripts based on the script group.
	 *
	 * @param scriptGroup - the script group name (test suite name)
	 * @return - the list of scripts based on the script group
	 */
	@Override
	public List<ScriptListDTO> findAllScriptsByTestSuite(String testSuite) {
		LOGGER.info("Getting all scripts based on the test suite: " + testSuite);

		// Find the test suite by name (test suite acts as script group)
		TestSuite testSuiteObj = testSuiteRepository.findByName(testSuite);
		if (testSuiteObj == null) {
			LOGGER.error("Test suite not found with the name: " + testSuite);
			throw new ResourceNotFoundException("Test suite", testSuite);
		}

		// Get scripts from the test suite
		List<ScriptTestSuite> scriptTestSuites = testSuiteObj.getScriptTestSuite();
		List<ScriptListDTO> scriptListDTO = new ArrayList<>();

		for (ScriptTestSuite scriptTestSuite : scriptTestSuites) {
			Script script = scriptTestSuite.getScript();
			ScriptListDTO scriptDTO = MapperUtils.convertToScriptListDTO(script);
			scriptListDTO.add(scriptDTO);
			LOGGER.info("Script: " + script.getName() + " added to the list for test suite: " + testSuite);
		}

		LOGGER.info("Returning all scripts based on the test suite: " + testSuite);
		return scriptListDTO;
	}

	/**
	 * This method is used to get the module execution time.
	 *
	 * @param moduleName - the module name
	 * @return - the module execution time in seconds
	 */
	@Override
	public Integer getModuleScriptTimeout(String moduleName) {
		LOGGER.info("Getting module execution time for module: " + moduleName);

		Module module = moduleRepository.findByName(moduleName);
		if (module == null) {
			LOGGER.error("Module not found with the name: " + moduleName);
			throw new ResourceNotFoundException(Constants.MODULE, moduleName);
		}

		Integer timeout = module.getExecutionTime();
		LOGGER.info("Module execution time for module '" + moduleName + "': " + timeout + " seconds");
		return timeout;
	}

	/**
	 * This method is used to dowload the all the MD files in ZIP format
	 * 
	 * @return ByteArrayInputStream containing the ZIP
	 */
	@Override
	public ByteArrayInputStream downloadAllMarkdownFilesZip() {
		List<Script> scripts = scriptRepository.findAll();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(baos)) {
			for (Script script : scripts) {
				ByteArrayInputStream mdStream = generateMarkdownFile(script);
				ZipEntry entry = new ZipEntry(script.getName() + ".md");
				zos.putNextEntry(entry);
				byte[] buffer = mdStream.readAllBytes();
				zos.write(buffer);
				zos.closeEntry();
			}
			zos.finish();
		} catch (IOException e) {
			LOGGER.error("Error generating markdown ZIP: " + e.getMessage());
			throw new TDKServiceException("Error generating markdown ZIP: " + e.getMessage());
		}
		return new ByteArrayInputStream(baos.toByteArray());
	}

	/**
	 * Download all markdown files by category, organized by module folders in a
	 * ZIP.
	 * 
	 * @param category the category name
	 * @return ByteArrayInputStream containing the ZIP
	 */
	@Override
	public ByteArrayInputStream downloadMarkdownByCategoryZip(String category) {
		Category cat = commonService.validateCategory(category);
		List<Module> modules = moduleRepository.findAllByCategory(cat);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(baos)) {
			for (Module module : modules) {
				List<Script> scripts = scriptRepository.findAllByModule(module);
				for (Script script : scripts) {
					ByteArrayInputStream mdStream = generateMarkdownFile(script);
					String entryName = module.getName() + "/" + script.getName() + ".md";
					ZipEntry entry = new ZipEntry(entryName);
					zos.putNextEntry(entry);
					byte[] buffer = mdStream.readAllBytes();
					zos.write(buffer);
					zos.closeEntry();
				}
			}
			zos.finish();
		} catch (IOException e) {
			LOGGER.error("Error generating markdown ZIP by category: " + e.getMessage());
			throw new TDKServiceException("Error generating markdown ZIP by category: " + e.getMessage());
		}
		return new ByteArrayInputStream(baos.toByteArray());
	}

	/**
	 * Generate a tar.gz file containing a Python script and an XML file with the
	 * test
	 * case details
	 * 
	 * @param scriptName - the script name
	 * @return - the tar.gz file as a byte array
	 */
	@Override
	public byte[] generateScriptTarGz(String scriptName) {
		String xmlContent;
		File getPythonScriptFile;
		xmlContent = generateTestCaseXml(scriptName);
		getPythonScriptFile = getPythonFile(scriptName);

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (GzipCompressorOutputStream gzipOut = new GzipCompressorOutputStream(byteArrayOutputStream);
				TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(gzipOut)) {

			// Add XML file to tar
			byte[] xmlBytes = xmlContent.getBytes(StandardCharsets.UTF_8);
			TarArchiveEntry xmlEntry = new TarArchiveEntry(scriptName + Constants.XML_FILE_EXTENSION);
			xmlEntry.setSize(xmlBytes.length);
			tarOutputStream.putArchiveEntry(xmlEntry);
			tarOutputStream.write(xmlBytes);
			tarOutputStream.closeArchiveEntry();

			// Add Python script file to tar
			byte[] scriptBytes = Files.readAllBytes(getPythonScriptFile.toPath());
			TarArchiveEntry scriptEntry = new TarArchiveEntry(getPythonScriptFile.getName());
			scriptEntry.setSize(scriptBytes.length);
			tarOutputStream.putArchiveEntry(scriptEntry);
			tarOutputStream.write(scriptBytes);
			tarOutputStream.closeArchiveEntry();

			tarOutputStream.finish();
		} catch (FileNotFoundException e) {
			LOGGER.error("File not found: " + e.getMessage());
			throw new TDKServiceException("File not found: " + e.getMessage());
		} catch (IOException e) {
			LOGGER.error("IO error: " + e.getMessage());
			throw new TDKServiceException("IO error: " + e.getMessage());
		}

		return byteArrayOutputStream.toByteArray();
	}

	/**
	 * Checks if the name of a script has been changed by comparing the DTO name
	 * with the existing script name.
	 * 
	 * @param dto      the ScriptDTO containing the potentially new name
	 * @param existing the existing Script entity to compare against
	 * @return true if the DTO contains a non-null name that differs from the
	 *         existing script's name, false otherwise
	 */
	private boolean hasNameChanged(ScriptDTO dto, Script existing) {
		return dto.getName() != null && !dto.getName().equals(existing.getName());
	}

	/**
	 * Checks if the primitive test name has changed between the DTO and existing
	 * script.
	 * 
	 * @param dto      the ScriptDTO containing the new primitive test name
	 * @param existing the existing Script entity to compare against
	 * @return true if the primitive test name in the DTO is not null and differs
	 *         from the existing script's primitive test name, false otherwise
	 */
	private boolean hasPrimitiveTestChanged(ScriptDTO dto, Script existing) {
		return dto.getPrimitiveTestName() != null
				&& !dto.getPrimitiveTestName().equals(existing.getPrimitiveTest().getName());
	}

	/**
	 * Checks if the synopsis field has changed between the provided DTO and
	 * existing entity.
	 * 
	 * @param dto      the ScriptDTO containing the new synopsis value to compare
	 * @param existing the existing Script entity with the current synopsis value
	 * @return true if the DTO contains a non-null synopsis that differs from the
	 *         existing synopsis,
	 *         false if the DTO synopsis is null or matches the existing synopsis
	 */
	private boolean hasSynopsisChanged(ScriptDTO dto, Script existing) {
		return dto.getSynopsis() != null && !dto.getSynopsis().equals(existing.getSynopsis());
	}

	/**
	 * Checks if the execution timeout value has changed between the provided
	 * ScriptDTO and existing Script.
	 * 
	 * @param dto      the ScriptDTO containing the new execution timeout value
	 * @param existing the existing Script entity to compare against
	 * @return true if the existing script has no timeout set (null) or if the
	 *         timeout values differ,
	 *         false if both timeout values are equal and non-null
	 */
	private boolean hasExecutionTimeoutChanged(ScriptDTO dto, Script existing) {
		Integer existingTimeout = existing.getExecutionTimeOut();
		return existingTimeout == null || dto.getExecutionTimeOut() != existing.getExecutionTimeOut();
	}

	/**
	 * Checks if the long duration flag has changed between the DTO and existing
	 * entity.
	 * 
	 * @param dto      the ScriptDTO containing the updated long duration value
	 * @param existing the existing Script entity to compare against
	 * @return true if the long duration flag has changed, false otherwise
	 */
	private boolean hasLongDurationChanged(ScriptDTO dto, Script existing) {
		return dto.isLongDuration() != existing.isLongDuration();
	}

	/**
	 * Checks if the skip execution flag has changed between the provided DTO and
	 * existing entity.
	 * 
	 * @param dto      the ScriptDTO containing the new skip execution value
	 * @param existing the existing Script entity to compare against
	 * @return true if the skip execution flags are different, false if they are the
	 *         same
	 */
	private boolean hasSkipExecutionChanged(ScriptDTO dto, Script existing) {
		return dto.isSkipExecution() != existing.isSkipExecution();
	}

	/**
	 * Checks if the test ID has been modified between the provided ScriptDTO and
	 * existing Script entity.
	 * 
	 * @param dto      the ScriptDTO containing potentially updated test ID
	 *                 information
	 * @param existing the existing Script entity to compare against
	 * @return true if the DTO contains a non-null test ID that differs from the
	 *         existing entity's test ID,
	 *         false if the test ID is null in the DTO or if both test IDs are equal
	 */
	private boolean hasTestIdChanged(ScriptDTO dto, Script existing) {
		return dto.getTestId() != null && !dto.getTestId().equals(existing.getTestId());
	}

	/**
	 * Checks if the objective field has changed between the provided DTO and
	 * existing entity.
	 * 
	 * @param dto      the ScriptDTO containing the new objective value
	 * @param existing the existing Script entity to compare against
	 * @return true if the DTO's objective is not null and differs from the existing
	 *         objective, false otherwise
	 */
	private boolean hasObjectiveChanged(ScriptDTO dto, Script existing) {
		return dto.getObjective() != null && !dto.getObjective().equals(existing.getObjective());
	}

	/**
	 * Checks if the priority value has changed between the provided DTO and
	 * existing entity.
	 * 
	 * @param dto      the ScriptDTO containing the new priority value to compare
	 * @param existing the existing Script entity with the current priority value
	 * @return true if the DTO has a non-null priority that differs from the
	 *         existing priority,
	 *         false if the DTO priority is null or equals the existing priority
	 */
	private boolean hasPriorityChanged(ScriptDTO dto, Script existing) {
		return dto.getPriority() != null && !dto.getPriority().equals(existing.getPriority());
	}

	/**
	 * Checks if the release version has been modified between the DTO and existing
	 * entity.
	 * 
	 * @param dto      the ScriptDTO containing the potentially updated release
	 *                 version
	 * @param existing the existing Script entity to compare against
	 * @return true if the DTO has a non-null release version that differs from the
	 *         existing entity's release version,
	 *         false otherwise
	 */
	private boolean hasReleaseVersionChanged(ScriptDTO dto, Script existing) {
		return dto.getReleaseVersion() != null && !dto.getReleaseVersion().equals(existing.getReleaseVersion());
	}

	/**
	 * Checks if the device types have changed between the existing script and the
	 * provided DTO.
	 * 
	 * @param dto      the ScriptDTO containing the new device types to compare
	 * @param existing the existing Script entity with current device types
	 * @return true if the device types have changed, false if they are the same or
	 *         if the DTO device types are null/empty
	 */
	private boolean hasDeviceTypesChanged(ScriptDTO dto, Script existing) {
		if (dto.getDeviceTypes() == null || dto.getDeviceTypes().isEmpty()) {
			return false;
		}

		List<String> existingDeviceTypeNames = existing.getDeviceTypes().stream().map(DeviceType::getName).sorted()
				.collect(Collectors.toList());

		List<String> newDeviceTypeNames = dto.getDeviceTypes().stream().sorted().collect(Collectors.toList());

		return !existingDeviceTypeNames.equals(newDeviceTypeNames);
	}

	/**
	 * Checks if the pre-conditions in the provided ScriptDTO have changed compared
	 * to the existing Script.
	 * 
	 * @param dto      the ScriptDTO containing the new pre-conditions to compare
	 * @param existing the existing Script entity with current pre-conditions
	 * @return true if pre-conditions have changed (different size or different
	 *         content),
	 *         false if they are the same or if the DTO has no pre-conditions
	 */
	private boolean hasPreConditionsChanged(ScriptDTO dto, Script existing) {
		if (dto.getPreConditions() == null || dto.getPreConditions().isEmpty()) {
			return false;
		}

		if (existing.getPreConditions().size() != dto.getPreConditions().size()) {
			return true;
		}

		List<String> existingPreConditions = existing.getPreConditions().stream()
				.map(PreCondition::getPreConditionDescription).sorted().collect(Collectors.toList());

		List<String> newPreConditions = dto.getPreConditions().stream().map(PreConditionDTO::getPreConditionDetails)
				.sorted().collect(Collectors.toList());
		return !existingPreConditions.equals(newPreConditions);
	}

	/**
	 * Checks if the test steps in the provided ScriptDTO have changed compared to
	 * the existing Script.
	 * 
	 * This method performs a deep comparison of test steps between a DTO and an
	 * existing entity,
	 * checking for differences in size, step names, descriptions, and expected
	 * results.
	 * 
	 * @param dto      the ScriptDTO containing the new test steps to compare
	 * @param existing the existing Script entity with current test steps
	 * @return true if any test steps have changed (size difference, name,
	 *         description, or expected result),
	 *         false if test steps are identical or if the DTO has no test steps
	 */
	private boolean hasTestStepsChanged(ScriptDTO dto, Script existing) {
		if (dto.getTestSteps() == null || dto.getTestSteps().isEmpty()) {
			return false;
		}

		if (existing.getTestSteps().size() != dto.getTestSteps().size()) {
			return true;
		}

		// Compare test steps
		for (int i = 0; i < dto.getTestSteps().size(); i++) {
			TestStepDTO newStep = dto.getTestSteps().get(i);
			TestStep existingStep = existing.getTestSteps().get(i);

			if (!newStep.getStepName().equals(existingStep.getStepName())
					|| !newStep.getStepDescription().equals(existingStep.getStepDescription())
					|| !newStep.getExpectedResult().equals(existingStep.getExpectedResult())) {
				return true;
			}
		}

		return false;
	}

	
	/**
	 * Generate a ZIP file containing multiple scripts. Each script will be in its
	 * own folder with both Python and XML files.
	 * 
	 * @param scriptNames - the list of script names
	 * @return - the ZIP file as a byte array
	 */
	@Override
	public byte[] generateBulkScriptZip(List<String> scriptNames) {
		LOGGER.info("Generating bulk script ZIP for {} scripts", scriptNames.size());

		if (scriptNames == null || scriptNames.isEmpty()) {
			throw new UserInputException("Script names list cannot be empty");
		}

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
			int foundScriptsCount = 0;
			for (String scriptName : scriptNames) {
				try {
					LOGGER.info("Processing script: {}", scriptName);

					// Get script from database
					Script script = scriptRepository.findByName(scriptName);
					if (script == null) {
						LOGGER.warn("Script not found: {}. Skipping...", scriptName);
						continue;
					}
					foundScriptsCount++;
					// Create folder for this script inside the ZIP
					String scriptFolder = scriptName + "/";

					// Add XML file
					String xmlContent = generateTestCaseXml(scriptName);
					ZipEntry xmlEntry = new ZipEntry(scriptFolder + scriptName + Constants.XML_FILE_EXTENSION);
					zipOutputStream.putNextEntry(xmlEntry);
					zipOutputStream.write(xmlContent.getBytes(StandardCharsets.UTF_8));
					zipOutputStream.closeEntry();

					// Add Python file
					File pythonFile = getPythonFile(scriptName);
					if (pythonFile.exists()) {
						ZipEntry pythonEntry = new ZipEntry(scriptFolder + pythonFile.getName());
						zipOutputStream.putNextEntry(pythonEntry);
						Files.copy(pythonFile.toPath(), zipOutputStream);
						zipOutputStream.closeEntry();
					} else {
						LOGGER.warn("Python file not found for script: {}", scriptName);
					}

					LOGGER.info("Successfully added script {} to ZIP", scriptName);

				} catch (Exception e) {
					LOGGER.error("Error processing script {}: {}", scriptName, e.getMessage());
					// Continue with next script instead of failing entire operation
				}
			}
			if (foundScriptsCount == 0) {
				LOGGER.error("No scripts found in the database for the provided script names: {}", scriptNames);
				throw new UserInputException("No scripts found in the database for the provided script names");
			}
			zipOutputStream.finish();
			LOGGER.info("Successfully generated bulk script ZIP");

		} catch (IOException e) {
			LOGGER.error("Error creating bulk script ZIP: {}", e.getMessage());
			throw new TDKServiceException("Error creating bulk script ZIP: " + e.getMessage());
		}

		return byteArrayOutputStream.toByteArray();
	}

	/**
	 * Generate a TAR.GZ file containing multiple scripts. Each script will be in
	 * its own folder with both Python and XML files.
	 * 
	 * @param scriptNames - the list of script names
	 * @return - the TAR.GZ file as a byte array
	 */
	@Override
	public byte[] generateBulkScriptTarGz(List<String> scriptNames) {
		LOGGER.info("Generating bulk script TAR.GZ for {} scripts", scriptNames.size());

		if (scriptNames == null || scriptNames.isEmpty()) {
			throw new UserInputException("Script names list cannot be empty");
		}

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		try (GzipCompressorOutputStream gzipOutputStream = new GzipCompressorOutputStream(byteArrayOutputStream);
				TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(gzipOutputStream)) {

			// Set to support long file names and set proper format
			tarOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			tarOutputStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

			int foundScriptsCount = 0;

			for (String scriptName : scriptNames) {
				try {
					LOGGER.info("Processing script: {}", scriptName);

					// Get script from database
					Script script = scriptRepository.findByName(scriptName);
					if (script == null) {
						LOGGER.warn("Script not found: {}. Skipping...", scriptName);
						continue;
					}
					foundScriptsCount++;
					// Create folder for this script inside the TAR
					String scriptFolder = scriptName + "/";

					// Add XML file
					String xmlContent = generateTestCaseXml(scriptName);
					byte[] xmlBytes = xmlContent.getBytes(StandardCharsets.UTF_8);

					String xmlFileName = scriptFolder + scriptName + Constants.XML_FILE_EXTENSION;
					TarArchiveEntry xmlEntry = new TarArchiveEntry(xmlFileName);
					xmlEntry.setSize(xmlBytes.length);
					xmlEntry.setModTime(System.currentTimeMillis());
					tarOutputStream.putArchiveEntry(xmlEntry);
					tarOutputStream.write(xmlBytes);
					tarOutputStream.closeArchiveEntry();

					// Add Python file
					File pythonFile = getPythonFile(scriptName);
					if (pythonFile.exists()) {
						byte[] pythonBytes = Files.readAllBytes(pythonFile.toPath());
						String pythonFileName = scriptFolder + pythonFile.getName();

						TarArchiveEntry pythonEntry = new TarArchiveEntry(pythonFileName);
						pythonEntry.setSize(pythonBytes.length);
						pythonEntry.setModTime(pythonFile.lastModified());
						tarOutputStream.putArchiveEntry(pythonEntry);
						tarOutputStream.write(pythonBytes);
						tarOutputStream.closeArchiveEntry();
					} else {
						LOGGER.warn("Python file not found for script: {}", scriptName);
					}

					LOGGER.info("Successfully added script {} to TAR.GZ", scriptName);

				} catch (Exception e) {
					LOGGER.error("Error processing script {}: {}", scriptName, e.getMessage());
					// Continue with next script instead of failing entire operation
				}
			}
			if (foundScriptsCount == 0) {
				LOGGER.error("No scripts found in the database for the provided script names: {}", scriptNames);
				throw new UserInputException("No scripts found in the database for the provided script names");
			}
			tarOutputStream.finish();
			LOGGER.info("Successfully generated bulk script TAR.GZ");

		} catch (IOException e) {
			LOGGER.error("Error creating bulk script TAR.GZ: {}", e.getMessage());
			throw new TDKServiceException("Error creating bulk script TAR.GZ: " + e.getMessage());
		}

		return byteArrayOutputStream.toByteArray();
	}

	/**
	 * Process a bulk script archive (ZIP or TAR.GZ) and extract scripts for
	 * processing.
	 * 
	 * @param archiveFile the uploaded archive file
	 * @return true if at least one script was processed successfully, false
	 *         otherwise
	 */
	@Override
	public boolean processBulkScriptArchive(MultipartFile archiveFile) {
		LOGGER.info("Processing bulk script archive: {}", archiveFile.getOriginalFilename());

		String filename = archiveFile.getOriginalFilename();
		if (filename == null) {
			throw new TDKServiceException("Archive filename cannot be null");
		}

		try {
			if (filename.toLowerCase().endsWith(".zip")) {
				return processBulkZipArchive(archiveFile);
			} else if (filename.toLowerCase().endsWith(".tar.gz") || filename.toLowerCase().endsWith(".tar")) {
				return processBulkTarArchive(archiveFile);
			} else {
				throw new TDKServiceException("Unsupported archive format: " + filename);
			}
		} catch (Exception e) {
			LOGGER.error("Error processing bulk archive: {}", e.getMessage());
			throw new TDKServiceException("Failed to process archive: " + e.getMessage());
		}
	}

	/**
	 * Update ZIP processing to use the Map approach
	 * 
	 * @param zipFile the uploaded ZIP file
	 * @return true if at least one script was processed successfully, false
	 *         otherwise
	 * @throws IOException if an error occurs while processing the ZIP file
	 */
	private boolean processBulkZipArchive(MultipartFile zipFile) throws IOException {
		LOGGER.info("Processing ZIP archive for bulk scripts");

		int processedScripts = 0;
		File tempZipFile = createTempFile(zipFile);

		try (ZipFile zip = new ZipFile(tempZipFile)) {
			Map<String, Map<String, Object>> scriptDataMap = extractScriptsFromZipAsMap(zip);
			processedScripts = processExtractedScriptsFromMap(scriptDataMap);

			LOGGER.info("Processed {} out of {} scripts from ZIP", processedScripts, scriptDataMap.size());
			return processedScripts > 0;

		} finally {
			tempZipFile.delete();
		}
	}

	/**
	 * Extract scripts from ZIP file using Map approach
	 * 
	 * @param zip the ZIP file to extract scripts from
	 * @return a map containing script data organized by script name
	 * @throws IOException if an error occurs while reading the ZIP file
	 */
	private Map<String, Map<String, Object>> extractScriptsFromZipAsMap(ZipFile zip) throws IOException {
		Map<String, Map<String, Object>> scriptDataMap = new HashMap<>();

		Enumeration<? extends ZipEntry> entries = zip.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();

			if (entry.isDirectory()) {
				continue;
			}

			extractZipEntryDataAsMap(zip, entry, scriptDataMap);
		}

		LOGGER.info("Extracted {} scripts from ZIP archive", scriptDataMap.size());
		return scriptDataMap;
	}

	/**
	 * Processes a single ZIP entry and extracts script-related data into the
	 * provided map.
	 * 
	 * This method reads the content of a ZIP entry and organizes it by script
	 * name.
	 * It handles XML and Python files specifically, storing their content and
	 * metadata
	 * in the provided script data map.
	 * 
	 * @param zip           the ZIP file to read entries from
	 * @param entry         the current ZIP entry being processed
	 * @param scriptDataMap a map that stores script data organized by script name,
	 *                      where each script can contain xmlContent, pythonContent,
	 *                      pythonFileName, and scriptName
	 */
	private void extractZipEntryDataAsMap(ZipFile zip, ZipEntry entry, Map<String, Map<String, Object>> scriptDataMap) {
		try {
			String entryPath = entry.getName();
			String[] pathParts = entryPath.split("/");

			if (pathParts.length < 2) {
				return; // Skip files not in folders
			}

			String scriptName = pathParts[0];
			String fileName = pathParts[pathParts.length - 1];

			Map<String, Object> scriptData = scriptDataMap.computeIfAbsent(scriptName, k -> new HashMap<>());
			scriptData.put("scriptName", scriptName);

			try (InputStream entryStream = zip.getInputStream(entry)) {
				if (fileName.endsWith(Constants.XML_FILE_EXTENSION)) {
					scriptData.put("xmlContent", entryStream.readAllBytes());
				} else if (fileName.endsWith(Constants.PYTHON_FILE_EXTENSION)) {
					scriptData.put("pythonContent", entryStream.readAllBytes());
					scriptData.put("pythonFileName", fileName);
				}
			}
		} catch (Exception e) {
			LOGGER.warn("Error processing ZIP entry {}: {}", entry.getName(), e.getMessage());
		}
	}

	/**
	 * Update TAR processing to use the same Map approach
	 * 
	 * @param tarFile the uploaded TAR file
	 * @return true if at least one script was processed successfully, false
	 *         otherwise
	 * @throws IOException if an error occurs while processing the TAR file
	 */
	private boolean processBulkTarArchive(MultipartFile tarFile) throws IOException {
		LOGGER.info("Processing TAR archive for bulk scripts");

		int processedScripts = 0;
		File tempTarFile = createTempFile(tarFile);

		try {

			Map<String, Map<String, Object>> scriptDataMap = extractScriptsFromTar(
					tempTarFile, tarFile.getOriginalFilename(), tarFile.getContentType());
			processedScripts = processExtractedScriptsFromMap(scriptDataMap);

			LOGGER.info("Processed {} out of {} scripts from TAR", processedScripts, scriptDataMap.size());
			return processedScripts > 0;

		} catch (Exception e) {
			LOGGER.error("Error processing TAR archive: {}", e.getMessage(), e);
			throw new TDKServiceException("Error processing TAR archive: " + e.getMessage());
		} finally {
			tempTarFile.delete();
		}
	}

	/**
	 * Extract scripts from TAR file using Map approach
	 * 
	 * @param tarFile          the TAR file to extract scripts from
	 * @param originalFilename the original filename of the uploaded file
	 * @param contentType      the content type of the uploaded file
	 * @return a map containing script data organized by script name
	 * @throws IOException if an error occurs while reading the TAR file
	 */
	private Map<String, Map<String, Object>> extractScriptsFromTar(File tarFile, String originalFilename,
			String contentType) throws IOException {
		Map<String, Map<String, Object>> scriptDataMap = new HashMap<>();

		try (FileInputStream fileInputStream = new FileInputStream(tarFile);
				InputStream inputStream = isGzipContent(originalFilename, contentType)
						? new GzipCompressorInputStream(fileInputStream)
						: fileInputStream;
				TarArchiveInputStream tarInputStream = new TarArchiveInputStream(inputStream)) {

			TarArchiveEntry entry;
			while ((entry = tarInputStream.getNextTarEntry()) != null) {
				if (entry.isFile()) {
					processTarEntry(tarInputStream, entry, scriptDataMap);
				}
			}

			LOGGER.info("Extracted {} scripts from TAR archive", scriptDataMap.size());
			return scriptDataMap;
		}
	}

	/**
	 * Determines if the content is GZIP compressed based on filename and content
	 * type.
	 * 
	 * @param originalFilename the original filename of the uploaded file
	 * @param contentType      the content type of the uploaded file
	 * @return true if the content is GZIP compressed, false otherwise
	 */
	private boolean isGzipContent(String originalFilename, String contentType) {
		// Check content type
		if (contentType != null && (contentType.contains("gzip") || contentType.contains("application/gzip"))) {
			return true;
		}

		// Check filename extension
		if (originalFilename != null) {
			String fileName = originalFilename.toLowerCase();
			return fileName.endsWith(".gz") || fileName.endsWith(".tgz");
		}

		return false;
	}

	/**
	 * Processes a single entry from a TAR archive and extracts script-related data.
	 * 
	 * This method reads the content of a TAR archive entry and organizes it by
	 * script name.
	 * It handles XML and Python files specifically, storing their content and
	 * metadata
	 * in the provided script data map.
	 * 
	 * @param tarInputStream the TAR archive input stream to read entry content from
	 * @param entry          the current TAR archive entry being processed
	 * @param scriptDataMap  a map that stores script data organized by script name,
	 *                       where each script can contain xmlContent,
	 *                       pythonContent,
	 *                       pythonFileName, and scriptName
	 * @throws IOException if an error occurs while reading the entry content from
	 *                     the stream
	 * 
	 */
	private void processTarEntry(TarArchiveInputStream tarInputStream, TarArchiveEntry entry,
			Map<String, Map<String, Object>> scriptDataMap) throws IOException {
		String entryPath = entry.getName();
		String[] pathParts = entryPath.split("/");

		if (pathParts.length < 2) {
			return; // Skip files not in folders
		}

		String scriptName = pathParts[0];
		String fileName = pathParts[pathParts.length - 1];

		Map<String, Object> scriptData = scriptDataMap.computeIfAbsent(scriptName, k -> new HashMap<>());
		scriptData.put("scriptName", scriptName);

		// Read entry content
		byte[] content = new byte[(int) entry.getSize()];
		int totalRead = 0;
		while (totalRead < content.length) {
			int bytesRead = tarInputStream.read(content, totalRead, content.length - totalRead);
			if (bytesRead == -1) {
				break;
			}
			totalRead += bytesRead;
		}

		if (fileName.endsWith(Constants.XML_FILE_EXTENSION)) {
			scriptData.put("xmlContent", content);
		} else if (fileName.endsWith(Constants.PYTHON_FILE_EXTENSION)) {
			scriptData.put("pythonContent", content);
			scriptData.put("pythonFileName", fileName);
		}
	}

	/**
	 * Creates a temporary file from the provided MultipartFile.
	 * The temporary file is created with a prefix "bulk_upload_" and suffix ".tmp".
	 * The file is automatically marked for deletion when the JVM exits.
	 *
	 * @param multipartFile the MultipartFile to be written to the temporary file
	 * @return a File object representing the created temporary file
	 */
	private File createTempFile(MultipartFile multipartFile) throws IOException {
		File tempFile = File.createTempFile("bulk_upload_", ".tmp");
		multipartFile.transferTo(tempFile);
		tempFile.deleteOnExit();
		return tempFile;
	}

	/**
	 * Processes multiple script data entries extracted from a map structure.
	 * 
	 * @param scriptDataMap a map containing script data where each value represents
	 *                      a map of script attributes including "scriptName"
	 * @return the total number of scripts that were successfully processed
	 *
	 */
	private int processExtractedScriptsFromMap(Map<String, Map<String, Object>> scriptDataMap) {
		int processedCount = 0;

		for (Map<String, Object> scriptData : scriptDataMap.values()) {
			try {
				if (processIndividualScriptFromMap(scriptData)) {
					processedCount++;
					String scriptName = (String) scriptData.get("scriptName");
					LOGGER.info("Successfully processed script: {}", scriptName);
				}
			} catch (Exception e) {
				String scriptName = (String) scriptData.get("scriptName");
				LOGGER.warn("Failed to process script '{}': {}", scriptName, e.getMessage());
			}
		}

		return processedCount;
	}

	/**
	 * Processes an individual script from a map containing script data and saves or
	 * updates it in the repository.
	 * 
	 * This method extracts script information from the provided map, validates the
	 * required data,
	 * and either creates a new script or updates an existing one based on whether a
	 * script with
	 * the same name already exists in the repository.
	 * 
	 * @param scriptData A map containing script data with the following expected
	 *                   keys:
	 *                   - "scriptName" (String): The name of the script
	 *                   - "xmlContent" (byte[]): The XML content of the script
	 *                   - "pythonContent" (byte[]): The Python script content as
	 *                   bytes
	 *                   - "pythonFileName" (String): The filename for the Python
	 *                   script
	 * 
	 * @return true if the script was successfully processed and saved/updated,
	 *         false otherwise.
	 *         Returns false if any required data is missing or if an error occurs
	 *         during processing.
	 */
	private boolean processIndividualScriptFromMap(Map<String, Object> scriptData) {
		String scriptName = (String) scriptData.get("scriptName");
		byte[] xmlContent = (byte[]) scriptData.get("xmlContent");
		byte[] pythonContent = (byte[]) scriptData.get("pythonContent");
		String pythonFileName = (String) scriptData.get("pythonFileName");

		if (xmlContent == null || pythonContent == null || pythonFileName == null) {
			LOGGER.warn("Incomplete script data for: {}", scriptName);
			return false;
		}

		try {
			// Reuse existing methods
			ScriptCreateDTO scriptCreateDTO = convertXmlToScriptCreateDTO(
					new ByteArrayInputStream(xmlContent));

			MultipartFile pythonFile = convertScriptFileToMultipartFile(
					new ByteArrayInputStream(pythonContent),
					pythonFileName);

			// Check if script exists and save/update
			Script existingScript = scriptRepository.findByName(scriptName);
			if (existingScript != null) {
				ScriptDTO scriptDTO = MapperUtils.convertToScriptDTOForXMLUpdate(scriptCreateDTO, existingScript);
				return updateScriptFromXML(pythonFile, scriptDTO, existingScript);
			} else {
				return saveScript(pythonFile, scriptCreateDTO);
			}

		} catch (Exception e) {
			LOGGER.error("Error processing script '{}': {}", scriptName, e.getMessage());
			return false;
		}
	}
}
