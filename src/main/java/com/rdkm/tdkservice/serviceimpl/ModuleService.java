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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rdkm.tdkservice.dto.ModuleCreateDTO;
import com.rdkm.tdkservice.dto.ModuleDTO;
import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.enums.ParameterDataType;
import com.rdkm.tdkservice.enums.TestGroup;
import com.rdkm.tdkservice.exception.DeleteFailedException;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.exception.UserInputException;
import com.rdkm.tdkservice.model.Function;
import com.rdkm.tdkservice.model.Module;
import com.rdkm.tdkservice.model.Parameter;
import com.rdkm.tdkservice.model.PrimitiveTest;
import com.rdkm.tdkservice.model.PrimitiveTestParameter;
import com.rdkm.tdkservice.repository.FunctionRepository;
import com.rdkm.tdkservice.repository.ModuleRepository;
import com.rdkm.tdkservice.repository.ParameterRepository;
import com.rdkm.tdkservice.repository.PrimitiveTestParameterRepository;
import com.rdkm.tdkservice.repository.PrimitiveTestRepository;
import com.rdkm.tdkservice.repository.UserGroupRepository;
import com.rdkm.tdkservice.service.IModuleService;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.MapperUtils;
import com.rdkm.tdkservice.util.Utils;

/**
 * Service implementation for managing module details.
 */
@Service
public class ModuleService implements IModuleService {

	@Autowired
	private ModuleRepository moduleRepository;

	@Autowired
	private UserGroupRepository userGroupRepository;

	@Autowired
	PrimitiveTestRepository primitiveTestRepository;

	@Autowired
	FunctionRepository functionRepository;

	@Autowired
	PrimitiveTestParameterRepository primitiveTestParameterRepository;

	@Autowired
	ParameterRepository parameterRepository;

	@Autowired
	CommonService commonService;

	private static final Logger LOGGER = LoggerFactory.getLogger(ModuleService.class);

	/**
	 * Saves a new module.
	 *
	 * @param moduleDTO the data transfer object containing the module details
	 * @return true if the module was saved successfully, false otherwise
	 */
	@Override
	public boolean saveModule(ModuleCreateDTO moduleDTO) {
		if (moduleRepository.existsByName(moduleDTO.getModuleName())) {
			LOGGER.error("Module with name {} already exists", moduleDTO.getModuleName());
			throw new ResourceAlreadyExistsException(Constants.MODULE_NAME, moduleDTO.getModuleName());
		}

		Module module = MapperUtils.toModuleEntity(moduleDTO);

		// Check if isThunderEnabled is true
		if (moduleDTO.isModuleThunderEnabled()) {
			module.setCategory(Category.RDKV_RDKSERVICE);
		} else {
			Category category = commonService.validateCategory(moduleDTO.getModuleCategory());
			module.setCategory(category);
		}
		try {
			moduleRepository.save(module);
		} catch (Exception e) {
			LOGGER.error("Error saving module: {}", e.getMessage());
			return false;
		}

		return module != null && module.getId() != null && module.getId() != null;

	}

	/**
	 * Updates an existing module.
	 *
	 * @param moduleDTO the data transfer object containing the updated module
	 *                  details
	 * @return true if the module was updated successfully, false otherwise
	 */
	@Override
	public boolean updateModule(ModuleDTO moduleDTO) {
		Module existingModule = moduleRepository.findById(moduleDTO.getId()).orElse(null);
		if (existingModule == null) {
			LOGGER.error("Module with ID {} not found", moduleDTO.getId());
			throw new ResourceNotFoundException(Constants.MODULE_NAME, moduleDTO.getId().toString());
		}

		if (!Utils.isEmpty(moduleDTO.getModuleName())) {
			Module newModule = moduleRepository.findByName(moduleDTO.getModuleName());
			if (newModule != null && moduleDTO.getModuleName().equalsIgnoreCase(existingModule.getName())) {
				existingModule.setName(moduleDTO.getModuleName());
			} else {

				if (moduleRepository.existsByName(moduleDTO.getModuleName())) {
					LOGGER.info("Module already exists with the same name: " + moduleDTO.getModuleName());
					throw new ResourceAlreadyExistsException(Constants.MODULE_NAME, moduleDTO.getModuleName());
				} else {
					existingModule.setName(moduleDTO.getModuleName());
				}
			}
		}

		// Check if isThunderEnabled is true
		if (moduleDTO.isModuleThunderEnabled()) {
			existingModule.setCategory(Category.RDKV_RDKSERVICE);
		}

		MapperUtils.updateModuleProperties(existingModule, moduleDTO);

		try {
			moduleRepository.save(existingModule);
			return true;
		} catch (Exception e) {
			LOGGER.error("Failed to update module: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Finds all modules.
	 *
	 * @return a list of data transfer objects containing the details of all modules
	 */
	@Override
	public List<ModuleDTO> findAllModules() {
		LOGGER.info("Going to fetch all modules");
		List<Module> modules;
		try {
			modules = moduleRepository.findAll();
		} catch (Exception e) {
			LOGGER.error("Error occurred while fetching all modules", e);
			return Collections.emptyList();
		}
		if (modules.isEmpty()) {
			return Collections.emptyList();
		}
		return modules.stream().map(MapperUtils::convertToModuleDTO).collect(Collectors.toList());
	}

	/**
	 * Finds a module by its ID.
	 *
	 * @param id the ID of the module
	 * @return the data transfer object containing the details of the module, or
	 *         null if not found
	 */
	@Override
	public ModuleDTO findModuleById(UUID id) {
		LOGGER.info("Going to fetch module with ID: {}", id);
		if (!moduleRepository.existsById(id)) {
			LOGGER.error("Module with ID {} not found", id);
			throw new ResourceNotFoundException("Module id :: ", id.toString());
		}
		Module module = moduleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Module not found for this id :: ", id.toString()));
		return MapperUtils.convertToModuleDTO(module);
	}

	/**
	 * Finds all modules by category, For RDKV category, it fetches modules
	 * associated with both RDKV and RDKV_RDKSERVICE categories.
	 *
	 * @param category the category of the module
	 * @return a list of data transfer objects containing the details of all modules
	 */
	@Override
	public List<ModuleDTO> findAllByCategory(String category) {
		LOGGER.info("Going to fetch all modules by category: {}", category);
		Category categoryEnum = commonService.validateCategory(category);
		List<Module> modules;
		if (Category.RDKV.name().equals(category)) {
			modules = moduleRepository.findAllByCategoryIn(Arrays.asList(Category.RDKV, Category.RDKV_RDKSERVICE));
		} else {
			categoryEnum = commonService.validateCategory(category);
			modules = moduleRepository.findAllByCategory(categoryEnum);
		}
		if (modules.isEmpty()) {
			return Collections.emptyList();
		}
		return modules.stream().map(MapperUtils::convertToModuleDTO).collect(Collectors.toList());
	}

	/**
	 * Deletes a module by its ID.
	 *
	 * @param id the ID of the module
	 * @return true if the module was deleted successfully, false otherwise
	 */

	@Override
	public boolean deleteModule(UUID id) {
		LOGGER.info("Going to delete module with ID: {}", id);
		Module module = moduleRepository.findById(id).orElse(null);
		if (module == null) {
			LOGGER.error("Module with ID {} not found", id);
			throw new ResourceNotFoundException(Constants.MODULE_NAME, id.toString());
		}
		try {
			moduleRepository.deleteById(id);
		} catch (DataIntegrityViolationException e) {
			LOGGER.error("Error deleting module: {}", e.getMessage());
			throw new DeleteFailedException();
		}
		LOGGER.info("Module deleted successfully: {}", id);
		return true;
	}

	@Override
	public List<String> findAllTestGroupsFromEnum() {
		LOGGER.info("Fetching all test groups from enum");
		return Arrays.stream(TestGroup.values()).map(TestGroup::name).collect(Collectors.toList());
	}

	/**
	 * Finds all modules names by category.
	 *
	 * @param category the category of the module
	 * @return a list of all modules names by category
	 */
	@Override
	public List<String> findAllModuleNameByCategory(String category) {
		LOGGER.info("Going to fetch all modules by category: {}", category);
		List<Module> modules = null;
		if (Category.RDKV.name().equals(category)) {
			modules = moduleRepository.findAllByCategoryIn(Arrays.asList(Category.RDKV, Category.RDKV_RDKSERVICE));
		} else {
			modules = moduleRepository.findAllByCategory(Category.valueOf(category));
		}

		Utils.checkCategoryValid(category);
		if (modules.isEmpty()) {
			return Collections.emptyList();
		}

		return modules.stream().map(Module::getName).collect(Collectors.toList());
	}

	/**
	 * Parses and saves the XML file.
	 *
	 * @param file the XML file to be parsed
	 * @throws Exception if an error occurs while parsing the XML file
	 */
	@Override
	public boolean parseAndSaveXml(MultipartFile file) {
		LOGGER.info("Parsing and saving the XML file");
		validateFile(file);
		try {
			Document doc = parseFileToDocument(file);
			Module module = processModule(doc);
			LOGGER.info("Module saved successfully");
			return true;
		} catch (SAXException e) {
			LOGGER.error("Invalid XML file format" + e.getMessage());
			throw new UserInputException("Invalid XML file format.");
		} catch (Exception e) {
			LOGGER.error("Error reading the XML file" + e.getMessage());
			throw new TDKServiceException("Error reading the XML file.");
		}
	}

	/**
	 * Generates the XML file.
	 *
	 * @param moduleName the name of the module
	 * @return the XML content of the module
	 * @throws Exception if an error occurs while generating the XML file
	 */
	@Override
	public String generateXML(String moduleName) {
		Module module = moduleRepository.findByName(moduleName);
		if (module == null) {
			LOGGER.error("Module with name {} not found", moduleName);
			throw new ResourceNotFoundException(Constants.MODULE_NAME, moduleName);
		}
		try {
			Document doc = createDocument();
			Element rootElement = createRootElement(doc);
			Element moduleElement = createModuleElement(doc, module);
			rootElement.appendChild(moduleElement);

			appendFunctionsToModuleElement(doc, moduleElement, module);
			// appendParametersToModuleElement(doc, moduleElement, module);
			appendPrimitiveTestsToModuleElement(doc, moduleElement, module);
			return convertDocumentToXmlString(doc);
		} catch (ParserConfigurationException e) {
			LOGGER.error("Error creating the XML document");
			throw new TDKServiceException("Error creating the XML document.");
		} catch (TransformerException e) {
			LOGGER.error("Error converting the XML document to string");
			throw new TDKServiceException("Error converting the XML document to string.");
		}

	}

	/**
	 * Downloads all modules as a ZIP file.
	 *
	 * @param category the category of the modules to be downloaded
	 * @return the ZIP file containing all modules
	 * @throws Exception if an error occurs while downloading the modules as a ZIP
	 *                   file
	 */
	@Override
	public ByteArrayResource downloadModulesAsZip(String category) {
		LOGGER.info("Downloading all modules as a ZIP file");
		Utils.checkCategoryValid(category);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(baos);

		Category categoryEnum = commonService.validateCategory(category);
		// Fetch all modules by category
		List<Module> modules = moduleRepository.findAllByCategory(categoryEnum);

		if (modules.isEmpty()) {
			LOGGER.error("No modules found for category {}", category);
			throw new ResourceNotFoundException(Constants.MODULE_NAME, category);
		}

		try {

			for (Module module : modules) {
				// Generate XML content for each module
				String xmlContent = generateXML(module.getName());

				// Create a ZIP entry for each module
				ZipEntry zipEntry = new ZipEntry(module.getName() + Constants.XML_EXTENSION);
				zos.putNextEntry(zipEntry);

				// Write XML content to the ZIP entry
				zos.write(xmlContent.getBytes());
				zos.closeEntry();
			}

			zos.finish();

			return new ByteArrayResource(baos.toByteArray());

		} catch (Exception e) {
			LOGGER.error("Error occurred while fetching all modules", e);
			throw new TDKServiceException("Error occurred while fetching all modules");
		}

	}

	/**
	 * Validates the uploaded file.
	 *
	 * @param file the uploaded file
	 */
	private void validateFile(MultipartFile file) {
		String fileName = file.getOriginalFilename();
		if (fileName == null || !fileName.endsWith(Constants.XML_EXTENSION)) {
			LOGGER.error("The uploaded file must have a .xml extension {}", fileName);
			throw new UserInputException("The uploaded file must be a .xml file.");
		}
		if (file.isEmpty()) {
			LOGGER.error("The uploaded file is empty");
			throw new UserInputException("The uploaded file is empty.");
		}
	}

	/**
	 * Parses the uploaded file to a Document object.
	 *
	 * @param file the uploaded file
	 * @return the Document object
	 * @throws SAXException                 if an error occurs while parsing the
	 *                                      file
	 * @throws IOException                  if an error occurs while reading the
	 *                                      file
	 * @throws ParserConfigurationException if an error occurs while parsing the
	 *                                      file
	 */
	private Document parseFileToDocument(MultipartFile file)
			throws SAXException, IOException, ParserConfigurationException {
		LOGGER.info("Parsing the XML file to Document");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(file.getInputStream());
	}

	/**
	 * Processes the module details from the XML file.
	 *
	 * @param doc the Document object
	 * @return the Module object
	 */
	private Module processModule(Document doc) {
		LOGGER.info("Inside processModule method");
		Element root = doc.getDocumentElement();
		Element moduleElement = (Element) root.getElementsByTagName(Constants.XML_MODULE_TAG).item(0);
		if (moduleElement == null) {
			LOGGER.error("Module element is missing");
			throw new UserInputException("Module element is missing from the XML.");
		}
		// Process and save the module data
		Module module = processModuleElement(moduleElement);

		// Process and save the functions associated with the module
		processFunctions(moduleElement, module);
		processPrimitiveTests(moduleElement, module);

		return module;
	}

	/**
	 * Processes the primitive tests for a given module from the XML
	 * It creates new primitive test if not existing, updated if
	 * there is a change via comparison
	 * 
	 * @param moduleElement
	 * @param module
	 */
	private void processPrimitiveTests(Element moduleElement, Module module) {
		LOGGER.info("Processing primitive tests for module: {}", module.getName());
		if (moduleElement == null)
			return;

		NodeList primitiveTestNodes = moduleElement.getElementsByTagName(Constants.XML_PRIMITIVE_TEST);

		// 1. Collect all PrimitiveTest names from XML
		Set<String> xmlPrimitiveTestNames = new HashSet<>();
		for (int i = 0; i < primitiveTestNodes.getLength(); i++) {
			Element ptElement = (Element) primitiveTestNodes.item(i);
			String testName = getTextContent(ptElement, Constants.NAME);
			xmlPrimitiveTestNames.add(testName);
		}

		// 2. Map existing primitive tests by name for quick lookup
		List<PrimitiveTest> existingPrimitiveTests = primitiveTestRepository.findByModule(module);
		Map<String, PrimitiveTest> primitiveTestMap = existingPrimitiveTests.stream()
				.collect(Collectors.toMap(PrimitiveTest::getName, pt -> pt));

		// 3. Process (add/update) the PrimitiveTests present in XML as before
		for (int i = 0; i < primitiveTestNodes.getLength(); i++) {
			Element ptElement = (Element) primitiveTestNodes.item(i);
			String testId = getTextContent(ptElement, Constants.ID);
			String testName = getTextContent(ptElement, Constants.NAME);
			String functionName = getTextContent(ptElement, Constants.XML_PARAMETER_FUN_NAME);

			// Find function by name (if present)
			Function function = null;
			if (functionName != null && !functionName.isEmpty()) {
				function = functionRepository.findByName(functionName);
			}

			PrimitiveTest primitiveTest = primitiveTestMap.get(testName);

			try {
				if (primitiveTest == null) {
					// Create new PrimitiveTest
					primitiveTest = new PrimitiveTest();
					primitiveTest.setName(testName);
					primitiveTest.setModule(module);
					primitiveTest.setFunction(function);
					primitiveTest.setId(UUID.fromString(testId));
					primitiveTest = primitiveTestRepository.save(primitiveTest);
					LOGGER.info("Created new PrimitiveTest: {}", testName);
				} else {
					// Update only if there are changes
					boolean changed = false;
					if (!Objects.equals(primitiveTest.getFunction().getName(), function.getName())) {
						primitiveTest.setFunction(function);
						changed = true;
					}
					// ...add more field comparisons as needed...
					if (changed) {
						primitiveTestRepository.save(primitiveTest);
						LOGGER.info("Updated PrimitiveTest: {}", testName);
					}
				}
			} catch (IllegalArgumentException e) {
				LOGGER.error("Error in saving the Primitive test data from the XML {}", e.getMessage());
				throw new UserInputException(
						"Error in saving the Primitive test data from the XML, Please check the Primitive test data");
			}

			// Process parameters for this primitive test
			processPrimitiveTestParameters(ptElement, primitiveTest);
		}
	}

	/**
	 * Processes parameters for a given PrimitiveTest.
	 * - Adds new parameters if not present.
	 * - Updates existing ones only if there are changes.
	 */
	private void processPrimitiveTestParameters(Element ptElement, PrimitiveTest primitiveTest) {
		NodeList paramNodes = ptElement.getElementsByTagName(Constants.XML_PRIMITIVE_TEST_PARAMETER);

		// 1. Map existing parameters by name for quick lookup
		List<PrimitiveTestParameter> existingParams = primitiveTestParameterRepository
				.findByPrimitiveTest(primitiveTest);
		Map<String, PrimitiveTestParameter> paramMap = existingParams.stream()
				.collect(Collectors.toMap(p -> p.getParameterName(), p -> p));

		// 2. Process each parameter in the XML
		for (int j = 0; j < paramNodes.getLength(); j++) {
			Element paramElement = (Element) paramNodes.item(j);

			String paramName = getTextContent(paramElement, Constants.XML_PARAMETER_NAME);
			String paramType = getTextContent(paramElement, Constants.XML_PARAMETER_TYPE);
			String paramRange = getTextContent(paramElement, Constants.XML_PARAMETER_RANGE);
			String paramValue = paramElement.getElementsByTagName(Constants.XML_PARAMETER_VALUE).getLength() > 0
					? getTextContent(paramElement, Constants.XML_PARAMETER_VALUE)
					: null;
			String paramId = getTextContent(paramElement, Constants.ID);

			PrimitiveTestParameter param = paramMap.get(paramName);

			try {

				if (param == null) {
					// Create new parameter
					param = new PrimitiveTestParameter();
					param.setParameterName(paramName);
					param.setParameterType(paramType);
					param.setParameterRange(paramRange);
					param.setParameterValue(paramValue);
					param.setPrimitiveTest(primitiveTest);
					param.setId(UUID.fromString(paramId));
					primitiveTestParameterRepository.save(param);
					LOGGER.info("Created new PrimitiveTestParameter: {}", paramName);
				} else {
					// Update only if there are changes
					boolean changed = false;
					if (!Objects.equals(param.getParameterType(), paramType)) {
						param.setParameterType(paramType);
						changed = true;
					}
					if (!Objects.equals(param.getParameterRange(), paramRange)) {
						param.setParameterRange(paramRange);
						changed = true;
					}
					if (!Objects.equals(param.getParameterValue(), paramValue)) {
						param.setParameterValue(paramValue);
						changed = true;
					}

					if (changed) {
						primitiveTestParameterRepository.save(param);
						LOGGER.info("Updated PrimitiveTestParameter: {}", paramName);
					}
				}

			} catch (IllegalArgumentException e) {
				LOGGER.error("Error in saving the Primitive test parameter data from the XML {}", e.getMessage());
				throw new UserInputException(
						"Error in saving the Primitive test parameter data from the XML, Please check the Primitive test parameter data for the primitive test "
								+ primitiveTest.getName());
			}

		}
	}

	/**
	 * Processes the module element from the XML file.
	 *
	 * @param moduleElement the module element
	 * @return the Module object
	 */
	private Module processModuleElement(Element moduleElement) {
		LOGGER.info("Inside processModuleElement method");

		// 1. All the mandatory fields are checked here and data is read from XML
		String moduleName = getTextContent(moduleElement, Constants.XML_MODULE_NAME);
		if (moduleName == null || moduleName.isEmpty()) {
			LOGGER.error("Module name is required");
			throw new UserInputException("Module name is required in the XML.");
		}

		Category category = Category.getCategory(getTextContent(moduleElement, Constants.XML_TAG_CATEGORY));
		if (category == null) {
			LOGGER.error("Invalid category");
			throw new UserInputException("Invalid category added in the XML.");
		}

		String moduleID = getTextContent(moduleElement, Constants.XML_MODULE_ID);
		if (moduleID == null || moduleID.isEmpty()) {
			LOGGER.error("Module ID is required");
			throw new UserInputException("Module ID is required in the XML.");
		}

		// Execution timeout can be empty
		int executionTimeOut = Integer.parseInt(getTextContent(moduleElement, Constants.XML_TAG_EXECUTION_TIME_OUT));

		TestGroup testGroup = TestGroup.getTestGroup(getTextContent(moduleElement, Constants.XML_TAG_TEST_GROUP));
		if (testGroup == null) {
			LOGGER.error("Test Group Data is not there or invalid");
			throw new UserInputException("Test Group data is not there or invalid");
		}

		Set<String> logFileNames = getListContent(moduleElement, Constants.XML_TAG_LOG_FILE_NAMES);
		Set<String> crashFileNames = getListContent(moduleElement, Constants.XML_TAG_CRASH_FILE_NAMES);

		// 2. Check if the module already exists , then update the existing one
		// if not, create a new module
		Module module = moduleRepository.findByName(moduleName);

		try {
			if (module == null) {
				module = new Module();
				module.setName(moduleName);
				module.setId(UUID.fromString(moduleID));
				module.setCategory(category);
				module.setExecutionTime(executionTimeOut);
				module.setTestGroup(testGroup);
				module.setLogFileNames(logFileNames);
				module.setCrashLogFiles(crashFileNames);
				module = moduleRepository.save(module);
				moduleRepository.flush();
			} else {
				// If the module exists, update its details only if there is a change
				updateModuleIfChanged(module, executionTimeOut, category, testGroup, logFileNames, crashFileNames);
			}

		} catch (IllegalArgumentException | DataIntegrityViolationException e) {
			LOGGER.error("Error processing module XML and saving", e);
			throw new UserInputException("Error processing module element: " + e.getMessage());
		} catch (Exception e) {
			LOGGER.error("Unexpected error processing module XML and saving", e);
			throw new TDKServiceException("Unexpected error processing module element");
		}
		return module;

	}

	/**
	 * Updates the module details if any changes are detected for the xml upload.
	 * 
	 * @param module
	 * @param executionTimeOut
	 * @param category
	 * @param testGroup
	 * @param logFileNames
	 * @param crashFileNames
	 */
	private void updateModuleIfChanged(Module module, int executionTimeOut, Category category, TestGroup testGroup,
			Set<String> logFileNames, Set<String> crashFileNames) {
		// ...move the update logic here, return true if any field was updated...
		boolean isUpdated = false;
		if (module.getExecutionTime() != executionTimeOut) {
			module.setExecutionTime(executionTimeOut);
			isUpdated = true;
		}

		if (module.getCategory() != category) {
			module.setCategory(category);
			isUpdated = true;
		}

		if (module.getTestGroup() != testGroup) {
			module.setTestGroup(testGroup);
			isUpdated = true;
		}

		if (addFileNames(module.getLogFileNames(), logFileNames)) {
			isUpdated = true;
		}

		if (addFileNames(module.getCrashLogFiles(), crashFileNames)) {
			isUpdated = true;
		}

		if (isUpdated) {
			moduleRepository.save(module);
		}
	}

	/*
	 * Processes the functions from the XML file.
	 */
	private void processFunctions(Element moduleElement, Module module) {
		LOGGER.info("Inside processFunctions method");
		NodeList functionNodes = moduleElement.getElementsByTagName(Constants.XML_FUNCTION);

		// Existing functions in the module, if the module is new it will return empty
		// set
		Set<Function> existingFunctions = module.getFunctions();

		// Create a map of existing functions for quick lookup
		Map<String, Function> functionMap = existingFunctions.stream()
				.collect(Collectors.toMap(Function::getName, Function -> Function));

		for (int i = 0; i < functionNodes.getLength(); i++) {
			Element functionElement = (Element) functionNodes.item(i);
			String functionName = functionElement.getAttribute(Constants.NAME);
			String functionId = functionElement.getAttribute(Constants.ID);
			if (functionName == null || functionName.isEmpty()) {
				LOGGER.error("Function name is required");
				throw new UserInputException("A function name is missing in the XML.");

			}

			// Check if the function already exists, if not create a new one
			Function function = functionMap.get(functionName);
			try {
				if (function == null) {
					function = new Function();
					function.setName(functionName);
					function.setId(UUID.fromString(functionId));

					function.setModule(module);
					function = functionRepository.save(function);
					functionRepository.flush();

					// Process parameters for the current function
					NodeList parameterNodes = functionElement.getElementsByTagName(Constants.XML_PARAMETER);
					if (null == parameterNodes || parameterNodes.getLength() == 0) {
						LOGGER.warn("No parameters found for function: {}", function.getName());
					} else {
						processParameters(parameterNodes, function);
					}
				}
			} catch (IllegalArgumentException | DataIntegrityViolationException e) {
				LOGGER.error("Error processing module XML and saving", e);
				throw new UserInputException("Error processing function elements in XML: ");
			} catch (Exception e) {
				LOGGER.error("Unexpected error processing module XML and saving", e);
				throw new TDKServiceException("Unexpected error processing module element");
			}

		}

	}

	/*
	 * Processes the parameters from the XML file.
	 */
	private void processParameters(NodeList parameterNodes, Function function) {
		LOGGER.info("Inside processParameters method");

		function = functionRepository.findById(function.getId()).orElse(null);
		// 2. Parameters existing for the given function
		Set<Parameter> parameters = function.getParameters();

		if (parameters == null || parameters.isEmpty()) {
			LOGGER.warn("No parameters found for function: {}", function.getName());
			// return;
		}

		for (int i = 0; i < parameterNodes.getLength(); i++) {
			Element parameterElement = (Element) parameterNodes.item(i);

			String parameterName = parameterElement.getAttribute(Constants.XML_PARAMETER_NAME);
			String parameterDataType = parameterElement.getAttribute(Constants.XML_PARAMETER_TYPE);
			String rangeVal = parameterElement.getAttribute(Constants.XML_PARAMETER_RANGE);
			String paramID = parameterElement.getAttribute(Constants.ID);

			this.validateParameterFields(parameterName, function, paramID, parameterDataType, rangeVal);

			// Check if the parameter already exists, if not create a new one
			Parameter parameter = parameters.stream().filter(p -> p.getName().equals(parameterName)).findFirst()
					.orElse(null);
			if (parameter == null) {

				parameter = new Parameter();
				parameter.setName(parameterName);
				parameter.setId(UUID.fromString(paramID));
				parameter.setParameterDataType(
						ParameterDataType.valueOf(parameterElement.getAttribute(Constants.XML_PARAMETER_TYPE)));
				parameter.setRangeVal(parameterElement.getAttribute(Constants.XML_PARAMETER_RANGE));
				parameter.setFunction(function);

				try {
					parameterRepository.save(parameter);
				} catch (IllegalArgumentException | DataIntegrityViolationException e) {
					LOGGER.error("Failed to save parameter: {}  for function: {}",
							parameterName, function.getName());
					throw new UserInputException("Failed to save parameter: " + parameterName + " , check the XML");
				} catch (Exception e) {
					LOGGER.error("Unexpected error processing module XML and saving", e);
					throw new TDKServiceException("Unexpected error processing module element");
				}

			}
		}
	}

	/**
	 * Validate the fields of a parameter while creating XML based data upload
	 * 
	 * @param parameterName     - name of the parameter
	 * @param function          - function object
	 * @param paramID           - ID of the parameter
	 * @param parameterDataType - data type of the parameter
	 * @param rangeVal          - range value of the parameter
	 */
	private void validateParameterFields(String parameterName, Function function,
			String paramID, String parameterDataType, String rangeVal) {
		if (parameterName == null || parameterName.isEmpty()) {
			throw new UserInputException("Parameter name is required.");
		}

		if (paramID == null || paramID.isEmpty()) {
			throw new UserInputException("Parameter ID is required.");
		}

		if (function == null) {
			throw new UserInputException("Function is required for parameter creation.");
		}
		if (parameterDataType == null || parameterDataType.isEmpty()) {
			throw new UserInputException("Parameter data type is required.");
		}
		try {
			ParameterDataType.valueOf(parameterDataType);
		} catch (IllegalArgumentException e) {
			throw new UserInputException("Invalid parameter data type: " + parameterDataType);
		}
		if (rangeVal == null || rangeVal.isEmpty()) {
			throw new UserInputException("Parameter range value is required.");
		}
	}

	/*
	 * Creates a new Document object.
	 */
	private Document createDocument() throws ParserConfigurationException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		return dBuilder.newDocument();
	}

	/*
	 * Creates the root element of the XML
	 */

	private Element createRootElement(Document doc) {
		Element rootElement = doc.createElement(Constants.XML);
		doc.appendChild(rootElement);
		return rootElement;
	}

	/*
	 * Creates a new module element.
	 */
	private Element createModuleElement(Document doc, Module module) {
		Element moduleElement = doc.createElement(Constants.XML_MODULE_TAG);
		addComment(doc, moduleElement, "Module Id");
		moduleElement.appendChild(
				createElementWithTextContent(doc, Constants.XML_MODULE_ID, module.getId().toString()));

		addComment(doc, moduleElement, Constants.MODULE_NAME);
		moduleElement.appendChild(createElementWithTextContent(doc, Constants.XML_MODULE_NAME, module.getName()));

		addComment(doc, moduleElement, "Category of the module");
		moduleElement.appendChild(
				createElementWithTextContent(doc, Constants.XML_TAG_CATEGORY, module.getCategory().toString()));

		addComment(doc, moduleElement, "Execution time out of module");
		moduleElement.appendChild(createElementWithTextContent(doc, Constants.XML_TAG_EXECUTION_TIME_OUT,
				String.valueOf(module.getExecutionTime())));

		addComment(doc, moduleElement, "Test Group of the module");
		moduleElement.appendChild(
				createElementWithTextContent(doc, Constants.XML_TAG_TEST_GROUP, module.getTestGroup().getName()));

		addComment(doc, moduleElement, "Logs File Names of module");
		moduleElement.appendChild(
				createElementWithListContent(doc, Constants.XML_TAG_LOG_FILE_NAMES, module.getLogFileNames()));

		addComment(doc, moduleElement, "Crash File Names of the module ");
		moduleElement.appendChild(
				createElementWithListContent(doc, Constants.XML_TAG_CRASH_FILE_NAMES, module.getCrashLogFiles()));

		return moduleElement;
	}

	/*
	 * Creates an element with text content.
	 */

	private Element createElementWithTextContent(Document doc, String name, String value) {
		Element element = doc.createElement(name);
		element.appendChild(doc.createTextNode(value));
		return element;
	}

	/*
	 * Creates an element with list content.
	 */

	private Element createElementWithListContent(Document doc, String name, Set<String> values) {
		Element element = doc.createElement(name);
		String formattedValues = "[" + String.join(",", values) + "]";
		element.appendChild(doc.createTextNode(formattedValues));
		return element;
	}

	/*
	 * Appends the functions to the module element.
	 */
	private void appendFunctionsToModuleElement(Document doc, Element moduleElement, Module module) {
		addComment(doc, moduleElement, "Total functions corresponding modules");
		Element functionsElement = doc.createElement(Constants.XML_FUNCTIONS);
		for (Function function : module.getFunctions()) {
			Element functionElement = doc.createElement(Constants.XML_FUNCTION);
			functionElement.setAttribute(Constants.NAME, function.getName());
			functionElement.setAttribute(Constants.ID, function.getId().toString());
			functionsElement.appendChild(functionElement);

			Element parametersElement = doc.createElement(Constants.XML_PARAMETERS);
			Set<Parameter> parameters = function.getParameters();
			if (null != parameters) {
				// Iterate over each parameter to create XML elements
				for (Parameter parameter : parameters) {
					Element parameterElement = doc.createElement(Constants.XML_PARAMETER);
					parameterElement.setAttribute(Constants.ID, parameter.getId().toString());
					parameterElement.setAttribute(Constants.XML_PARAMETER_NAME, parameter.getName());
					parameterElement.setAttribute(Constants.XML_PARAMETER_TYPE,
							parameter.getParameterDataType().toString());
					parameterElement.setAttribute(Constants.XML_PARAMETER_RANGE, parameter.getRangeVal());
					parametersElement.appendChild(parameterElement);
				}
			}
			functionElement.appendChild(parametersElement);
		}
		moduleElement.appendChild(functionsElement);
	}

	/*
	 * Appends the primitive tests (and their parameters) to the module element.
	 * doc - the XML document
	 * moduleElement - the module element
	 * module - the module object
	 */
	private void appendPrimitiveTestsToModuleElement(Document doc, Element moduleElement, Module module) {
		addComment(doc, moduleElement, "Primitive tests associated with this module");
		Element primitiveTestsElement = doc.createElement(Constants.XML_PRIMITIVE_TESTS);
		List<PrimitiveTest> primitiveTests = primitiveTestRepository.findByModule(module);
		if (primitiveTests != null) {
			for (PrimitiveTest primitiveTest : primitiveTests) {
				Element primitiveTestElement = doc.createElement(Constants.XML_PRIMITIVE_TEST);
				// Add PrimitiveTest fields
				primitiveTestElement
						.appendChild(createElementWithTextContent(doc, Constants.ID, primitiveTest.getId().toString()));
				primitiveTestElement
						.appendChild(createElementWithTextContent(doc, Constants.NAME, primitiveTest.getName()));
				if (primitiveTest.getFunction() != null) {
					primitiveTestElement.appendChild(
							createElementWithTextContent(doc, Constants.XML_PARAMETER_FUN_NAME,
									primitiveTest.getFunction().getName()));
				}

				// Add parameters
				Element parametersElement = doc.createElement(Constants.XML_PRIMITIVE_TEST_PARAMETERS);
				List<PrimitiveTestParameter> primitiveTestParameterList = primitiveTestParameterRepository
						.findByPrimitiveTest(primitiveTest);
				if (primitiveTestParameterList != null) {
					for (PrimitiveTestParameter param : primitiveTestParameterList) {
						Element paramElement = doc.createElement(Constants.XML_PRIMITIVE_TEST_PARAMETER);
						paramElement
								.appendChild(createElementWithTextContent(doc, Constants.ID, param.getId().toString()));
						paramElement.appendChild(
								createElementWithTextContent(doc, Constants.XML_PARAMETER_NAME,
										param.getParameterName()));
						paramElement.appendChild(
								createElementWithTextContent(doc, Constants.XML_PARAMETER_TYPE,
										param.getParameterType()));
						paramElement.appendChild(
								createElementWithTextContent(doc, Constants.XML_PARAMETER_RANGE,
										param.getParameterRange()));
						if (param.getParameterValue() != null) {
							paramElement.appendChild(
									createElementWithTextContent(doc, Constants.XML_PARAMETER_VALUE,
											param.getParameterValue()));
						}
						parametersElement.appendChild(paramElement);
					}
				}
				primitiveTestElement.appendChild(parametersElement);
				primitiveTestsElement.appendChild(primitiveTestElement);
			}
		}
		moduleElement.appendChild(primitiveTestsElement);
	}

	/*
	 * Appends the parameters to the module element.
	 */
	private void appendParametersToModuleElement(Document doc, Element moduleElement, Module module) {
		addComment(doc, moduleElement, "Total parameters corresponding module");
		Element parametersElement = doc.createElement(Constants.XML_PARAMETERS);

		Set<Function> functions = module.getFunctions();
		// Iterate over each function to get its parameters
		if (null != functions) {
			for (Function function : functions) {
				Set<Parameter> parameters = function.getParameters();
				if (null != parameters) {
					// Iterate over each parameter to create XML elements
					for (Parameter parameter : parameters) {
						Element parameterElement = doc.createElement(Constants.XML_PARAMETER);
						parameterElement.setAttribute(Constants.ID, parameter.getId().toString());
						parameterElement.setAttribute(Constants.XML_PARAMETER_FUN_NAME, function.getName());
						parameterElement.setAttribute(Constants.XML_PARAMETER_NAME, parameter.getName());
						parameterElement.setAttribute(Constants.XML_PARAMETER_TYPE,
								parameter.getParameterDataType().toString());
						parameterElement.setAttribute(Constants.XML_PARAMETER_RANGE, parameter.getRangeVal());
						parametersElement.appendChild(parameterElement);
					}
				}

			}

		}

		moduleElement.appendChild(parametersElement);
	}

	/*
	 * Converts the Document object to an XML string.
	 */
	private String convertDocumentToXmlString(Document doc) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, Constants.YES);
		DOMSource source = new DOMSource(doc);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		transformer.transform(source, result);
		return writer.toString();
	}

	/*
	 * Updated the filenames if needed
	 * 
	 * @param existingFileNames
	 * 
	 * @param newFileNames
	 */
	private boolean addFileNames(Set<String> existingFileNames, Set<String> newFileNames) {
		// update only happens if the sets are actually different, preventing
		// unnecessary updates and timestamp changes.
		boolean updated = false;
		if (existingFileNames == null && newFileNames != null) {
			existingFileNames = new HashSet<>(newFileNames);
			updated = true;
		} else if (existingFileNames != null && newFileNames != null && !existingFileNames.equals(newFileNames)) {
			existingFileNames.clear();
			existingFileNames.addAll(newFileNames);
			updated = true;
		}
		return updated;
		// If both are null or both are equal, do nothing (no update)

	}

	/*
	 * Gets the text content of a tag from an XML element.
	 */
	private String getTextContent(Element element, String tagName) {
		return element.getElementsByTagName(tagName).item(0).getTextContent();
	}

	/*
	 * Gets the list content from an XML element.
	 */
	private Set<String> getListContent(Element element, String tagName) {
		// Get the text content from the tag
		String content = element.getElementsByTagName(tagName).item(0).getTextContent();

		// Remove the brackets if present
		content = content.replaceAll("[\\[\\]]", "");

		// Split the content by commas and convert to a Set
		String[] items = content.split(Constants.COMMA_SEPARATOR);
		return new HashSet<>(Arrays.asList(items));
	}

	/*
	 * Adds a comment to an XML element.
	 */
	private void addComment(Document doc, Element parent, String commentText) {
		Comment comment = doc.createComment(" " + commentText + " ");
		parent.appendChild(comment);
	}

	/**
	 * Finds all module names by category.
	 *
	 * @param category the category of the module
	 * @return a list of all module names
	 */
	@Override
	public List<String> findAllModuleNamesBySubCategory(String category) {
		LOGGER.info("Going to fetch all modules by category: {}", category);
		Category categoryEnum = commonService.validateCategory(category);
		List<Module> modules = moduleRepository.findAllByCategory(categoryEnum);
		if (modules.isEmpty()) {
			return Collections.emptyList();
		}
		return modules.stream().map(Module::getName).collect(Collectors.toList());
	}

}