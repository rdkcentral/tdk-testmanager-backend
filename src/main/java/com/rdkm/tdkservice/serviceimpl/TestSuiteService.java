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
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rdkm.tdkservice.dto.ScriptListDTO;
import com.rdkm.tdkservice.dto.TestSuiteCreateDTO;
import com.rdkm.tdkservice.dto.TestSuiteCustomDTO;
import com.rdkm.tdkservice.dto.TestSuiteDTO;
import com.rdkm.tdkservice.dto.TestSuiteDetailsResponse;
import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.exception.DeleteFailedException;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.exception.UserInputException;
import com.rdkm.tdkservice.model.DeviceType;
import com.rdkm.tdkservice.model.Module;
import com.rdkm.tdkservice.model.Script;
import com.rdkm.tdkservice.model.ScriptTestSuite;
import com.rdkm.tdkservice.model.TestSuite;
import com.rdkm.tdkservice.model.UserGroup;
import com.rdkm.tdkservice.repository.DeviceTypeRepository;
import com.rdkm.tdkservice.repository.ModuleRepository;
import com.rdkm.tdkservice.repository.ScriptRepository;
import com.rdkm.tdkservice.repository.ScriptTestSuiteRepository;
import com.rdkm.tdkservice.repository.TestSuiteRepository;
import com.rdkm.tdkservice.service.ITestSuiteService;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.MapperUtils;
import com.rdkm.tdkservice.util.Utils;

/**
 * The TestSuiteService class is used to provide the services for the test
 * suite. It implements the ITestSuiteService interface which contains the
 * methods for the test suite operations.
 */
@Service
public class TestSuiteService implements ITestSuiteService {

	public static final Logger LOGGER = LoggerFactory.getLogger(TestSuiteService.class);

	@Autowired
	CommonService commonService;

	@Autowired
	ScriptRepository scriptRepository;

	@Autowired
	TestSuiteRepository testSuiteRepository;

	@Autowired
	ScriptTestSuiteRepository scriptTestSuiteRepository;

	@Autowired
	ModuleRepository moduleRepository;

	@Autowired
	DeviceTypeRepository deviceTypeRepository;

	/**
	 * Create the test suite.
	 * 
	 * @param TestSuiteCreateDTO - the test suite create dto
	 * @return the boolean
	 */
	@Override
	public boolean createTestSuite(TestSuiteCreateDTO testSuiteCreateDTO) {
		LOGGER.info("Creating test suite" + testSuiteCreateDTO.getName());
		TestSuite testSuite = new TestSuite();

		// Check if the test suite already exists with the same name or not in the
		// database. If it exists, throw a Resource Already exists exception
		this.checkIfTestSuiteExists(testSuiteCreateDTO.getName());
		testSuite.setName(testSuiteCreateDTO.getName());
		testSuite.setDescription(testSuiteCreateDTO.getDescription());

		// Validates category and sets it to test suite
		Category category = commonService.validateCategory(testSuiteCreateDTO.getCategory());
		testSuite.setCategory(category);

		// Validates user group and sets it to test suite
		UserGroup userGroup = commonService.validateUserGroup(testSuiteCreateDTO.getUserGroup());
		testSuite.setUserGroup(userGroup);

		/// Validates scripts and sets it to test suite
		List<ScriptListDTO> scriptList = testSuiteCreateDTO.getScripts();
		if (scriptList == null || scriptList.isEmpty() || scriptList.size() == 0) {
			LOGGER.error("Scripts list is empty");
			throw new UserInputException(
					"There is no script info associated with the given test suite. Please provide the script info.");
		}

		// Save the test suite and script list
		try {
			testSuiteRepository.save(testSuite);
			saveScriptList(scriptList, category, testSuite);
		} catch (Exception e) {
			LOGGER.error("Error while saving test suite", e);
			throw new TDKServiceException("Error while saving test suite");
		}
		return true;

	}

	/**
	 * This method is used to create the custom test suite.
	 * 
	 * @param TestSuiteCustomDTO - the test suite custom dto
	 * @return the boolean - Status of custom test suite creation
	 */
	@Override
	public boolean updateTestSuite(TestSuiteDTO testSuiteDTO) {
		LOGGER.info("Updating test suite" + testSuiteDTO.getName());

		// Check if the script ID is present in the database or not
		TestSuite testSuite = testSuiteRepository.findById(testSuiteDTO.getId())
				.orElseThrow(() -> new ResourceNotFoundException(Constants.SCRIPT_ID, testSuiteDTO.getId().toString()));

		// TODO : Revisit this if test suite name can be changed or not
		if (!Utils.isEmpty(testSuiteDTO.getName())) {
			TestSuite newTestSuite = testSuiteRepository.findByName(testSuiteDTO.getName());
			if (newTestSuite != null && testSuiteDTO.getName().equalsIgnoreCase(testSuite.getName())) {
				testSuite.setName(testSuiteDTO.getName());
			} else {

				if (testSuiteRepository.existsByName(testSuiteDTO.getName())) {
					LOGGER.info("Test Suite already exists with the same name: " + testSuiteDTO.getName());
					throw new ResourceAlreadyExistsException(Constants.TEST_SUITE, testSuiteDTO.getName());
				} else {
					testSuite.setName(testSuiteDTO.getName());
				}
			}
		}

		return this.updateTheGivenTestSuite(testSuiteDTO, testSuite);
	}

	/**
	 * Updates a test suite from an XML input stream.
	 * 
	 * @param testSuiteDTO the test suite DTO
	 * @return true if the update was successful, false otherwise
	 */
	public boolean updateTestSuiteFromXML(TestSuiteDTO testSuiteDTO) {
		// Extract the Test Suite ID from the XML input stream
		TestSuite testSuite = testSuiteRepository.findByName(testSuiteDTO.getName());
		return this.updateTheGivenTestSuite(testSuiteDTO, testSuite);
	}

	/**
	 * Updates the given test suite.
	 * 
	 * @param testSuiteDTO the test suite DTO
	 * @param testSuite    the test suite
	 * @return true if the update was successful, false otherwise
	 */
	public boolean updateTheGivenTestSuite(TestSuiteDTO testSuiteDTO, TestSuite testSuite) {

		if (!(Utils.isEmpty(testSuiteDTO.getDescription()))
				&& !(testSuiteDTO.getDescription().equals(testSuite.getDescription()))) {
			testSuite.setDescription(testSuiteDTO.getDescription());
		}

		// Category update is conceptually not allowed for existing test suites, so we
		// ignore it, as the scripts are already linked to the test suite with a
		// specific category.

		// Explicitly set the updatedAt timestamp and save the entity to ensure
		// the updated time is persisted immediately, especially when changes
		// are made to the script list (Script TestSuite table)only that may not trigger
		// automatic
		// JPA updates in the testsuite table.
		testSuite.setUpdatedAt(Instant.now());
		testSuiteRepository.save(testSuite);

		try {
			if (testSuiteDTO.getScripts() != null) {
				List<ScriptListDTO> scriptsList = testSuiteDTO.getScripts();
				// Delete all the existing test suite mappings
				scriptTestSuiteRepository.deleteByTestSuite(testSuite);
				saveScriptList(scriptsList, testSuite.getCategory(), testSuite);
			}
		} catch (Exception e) {
			LOGGER.error("Error while updating test suite", e);
			throw new TDKServiceException("Error while updating test suite");
		}

		return true;

	}

	/**
	 * This method is used to create the custom test suite.
	 * 
	 * @param TestSuiteCustomDTO - the test suite custom dto
	 * @return the boolean - Status of custom test suite creation
	 */
	@Override
	public boolean createCustomTestSuite(TestSuiteCustomDTO testSuiteCustomDTO) {
		LOGGER.info("Creating custom test suite" + testSuiteCustomDTO.getTestSuiteName());

		Category category = commonService.validateCategory(testSuiteCustomDTO.getCategory());

		/**
		 * Check if the test suite already exists with the same name or not in the
		 * database. If it exists, throw a Resource Already exists exception
		 */
		this.checkIfTestSuiteExists(testSuiteCustomDTO.getTestSuiteName());

		TestSuite testSuite = new TestSuite();
		testSuite.setName(testSuiteCustomDTO.getTestSuiteName());
		testSuite.setDescription(testSuiteCustomDTO.getDescription());
		testSuite.setCategory(category);
		UserGroup userGroup = commonService.validateUserGroup(testSuiteCustomDTO.getUserGroup());
		testSuite.setUserGroup(userGroup);

		// Get the list of modules from the test suite custom DTO
		List<String> modulesList = testSuiteCustomDTO.getModules();
		if (modulesList == null || modulesList.isEmpty() || modulesList.size() == 0) {
			LOGGER.error("Modules list is empty");
			throw new UserInputException(
					"There is no module info associated with the given test suite. Please provide the module info.");
		}

		// Get the list of scripts from the modules
		List<ScriptListDTO> scriptList = new ArrayList<ScriptListDTO>();
		for (String moduleName : modulesList) {
			Module module = moduleRepository.findByName(moduleName);
			if (module == null) {
				LOGGER.error("Module is not available with the name " + moduleName);
				// if the module is not available in the database, then throw an exception
				throw new ResourceNotFoundException(Constants.MODULE, moduleName);
			}

			List<Script> scriptsList = scriptRepository.findAllByModuleAndIsLongDuration(module,
					testSuiteCustomDTO.isLongDurationScripts());
			if (scriptsList == null || scriptsList.isEmpty() || scriptsList.size() == 0) {
				LOGGER.error("Scripts list is empty for the module  " + module.getName());
				// if no scripts are available for the module, then continue with the next
				// module in the list
				continue;
			}

			DeviceType boxtype;
			if (category.equals(Category.RDKV_RDKSERVICE)) {
				boxtype = deviceTypeRepository.findByNameAndCategory(testSuiteCustomDTO.getDeviceType(), Category.RDKV);
			} else {
				boxtype = deviceTypeRepository.findByNameAndCategory(testSuiteCustomDTO.getDeviceType(), category);
			}
			if (boxtype == null) {
				LOGGER.error("DeviceType is not available with the name " + testSuiteCustomDTO.getDeviceType());
				// if the device type is not available in the database, then throw an exception
				throw new ResourceNotFoundException(Constants.DEVICE_TYPE, testSuiteCustomDTO.getDeviceType());
			}

			// Get the list of script with matching box type
			List<Script> scriptsListWithDeviceType = getScriptsWithMatchingDeviceType(scriptsList, boxtype);

			// Convert the list of scripts to list of script list DTO, and add it to the
			// scriptlist
			scriptList.addAll(MapperUtils.getScriptListDTOFromScriptList(scriptsListWithDeviceType));

		}

		if (scriptList == null || scriptList.isEmpty() || scriptList.size() == 0) {
			throw new UserInputException(
					"Custom test suite creation failed, there is no scripts available that satisfy this condition");
		}

		try {
			testSuiteRepository.save(testSuite);
			this.saveScriptList(scriptList, category, testSuite);

		} catch (Exception e) {
			LOGGER.error("Error while saving test suite", e);
			throw new TDKServiceException("Error while saving test suite");
		}
		return true;

	}

	/**
	 * Get the list of script with matching box type
	 * 
	 * @param scriptList - the list of scripts
	 * @param boxType    - the box type
	 * @return - the list of script with matching box
	 */
	private List<Script> getScriptsWithMatchingDeviceType(List<Script> scriptList, DeviceType boxType) {
		LOGGER.info("Getting the list of script with matching box type");
		List<Script> scriptListWithDeviceType = new ArrayList<Script>();
		for (Script script : scriptList) {
			List<DeviceType> boxTypes = script.getDeviceTypes();
			if (boxTypes.contains(boxType)) {
				scriptListWithDeviceType.add(script);
			}
		}
		LOGGER.info("Got the list of script with matching box type");
		return scriptListWithDeviceType;

	}

	/**
	 * Delete the test suite.
	 * 
	 * @param id - the test suite id
	 * @return
	 */
	@Override
	public boolean deleteTestSuite(UUID id) {
		LOGGER.info("Deleting test suite with id: " + id);
		// get ScriptTestSuite by test suite id
		TestSuite testSuite = testSuiteRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.TEST_SUITE, id.toString()));
		try {
			// Delete all the script test suite mapping for the test suite
			scriptTestSuiteRepository.deleteByTestSuite(testSuite);
			// loop through the script test suite list and delete each script test suite
			testSuiteRepository.delete(testSuite);
		} catch (DataIntegrityViolationException e) {
			LOGGER.error("Error in deleting TestSuite data: " + e.getMessage());
			throw new DeleteFailedException();
		}

		LOGGER.info("Test suite deleted successfully");
		return true;

	}

	/**
	 * Find the test suite by id.
	 * 
	 * @param id - the test suite id
	 * @return the test suite dto
	 */
	@Override
	public TestSuiteDTO findTestSuiteById(UUID testSuiteId) {
		LOGGER.info("Finding test suite with id: " + testSuiteId);
		TestSuite testSuite = testSuiteRepository.findById(testSuiteId)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.TEST_SUITE_ID, testSuiteId.toString()));
		TestSuiteDTO testSuiteDTO = this.convertTestSuiteToTestSuiteDTO(testSuite);
		return testSuiteDTO;
	}

	/**
	 * This method is used to find all the test suites.
	 * 
	 * @param category - the category
	 * @return the list of test suites
	 */
	@Override
	public List<TestSuiteDTO> findAllTestSuiteByCategory(String categoryName) {
		LOGGER.info("Finding all test suites by category" + categoryName);
		Category category = commonService.validateCategory(categoryName);
		List<TestSuite> testSuiteList = testSuiteRepository.findAllByCategory(category);
		if (Category.RDKV.equals(category)) {
			testSuiteList = testSuiteRepository
					.findAllByCategoryIn(Arrays.asList(Category.RDKV, Category.RDKV_RDKSERVICE));
		} else {
			testSuiteList = testSuiteRepository.findAllByCategory(category);
		}

		List<TestSuiteDTO> testSuiteDTOList = new ArrayList<TestSuiteDTO>();
		if (testSuiteList != null && !testSuiteList.isEmpty()) {
			for (TestSuite testSuite : testSuiteList) {
				TestSuiteDTO testSuiteDTO = this.convertTestSuiteToTestSuiteDTO(testSuite);
				if (null != testSuiteDTO) {
					testSuiteDTOList.add(testSuiteDTO);
				}
			}
		} else {
			LOGGER.error("No test suites found for the category: " + category);
			throw new ResourceNotFoundException(Constants.TEST_SUITE, category.toString());
		}
		return testSuiteDTOList;
	}

	/**
	 * This method is used to get the test case details of each in test suite as
	 * excel
	 * 
	 * @param id - the test suite id
	 * @return the test cases in test suite as excel
	 */
	@Override
	public ByteArrayInputStream getTestCasesInTestSuiteAsExcel(String testSuiteName) {
		LOGGER.info("Received request to download test case as excel for the test suite: with id " + testSuiteName);

		TestSuite testSuite = testSuiteRepository.findByName(testSuiteName);
		if (testSuite == null) {
			LOGGER.error("Test suite not found with the name: " + testSuiteName);
			throw new ResourceNotFoundException(Constants.TEST_SUITE, testSuiteName);
		}

		List<ScriptTestSuite> scriptTestSuiteList = scriptTestSuiteRepository.findAllByTestSuite(testSuite);
		// get All the script in the test suite in the list
		List<Script> scriptList = new ArrayList<Script>();
		for (ScriptTestSuite scriptTestSuite : scriptTestSuiteList) {
			scriptList.add(scriptTestSuite.getScript());
		}
		return commonService.createExcelFromTestCasesDetailsInScript(scriptList, "TEST_CASE_" + testSuite.getName());

	}

	/**
	 * Check if the script already exists with the same name or not in the database
	 * 
	 * @param scriptCreateDTO - the script details
	 */
	private void checkIfTestSuiteExists(String testSuiteName) {
		if (testSuiteRepository.existsByName(testSuiteName)) {
			LOGGER.error("Test suite already exists with the same name: " + testSuiteName);
			throw new ResourceAlreadyExistsException(Constants.TEST_SUITE, testSuiteName);
		}
	}

	/**
	 * Get the list of script test suite
	 * 
	 * @param scriptsList - the list
	 * @return - the list of script test suite
	 */
	private void saveScriptList(List<ScriptListDTO> scriptsList, Category testSuiteCategory, TestSuite testSuite) {
		for (int i = 0; i < scriptsList.size(); i++) {
			ScriptTestSuite scriptTestSuite = new ScriptTestSuite();
			UUID scriptId = scriptsList.get(i).getId();
			String scriptName = scriptsList.get(i).getName();
			// If any script id sent is not there in the database, then error is thrown
			Optional<Script> scriptOptional = scriptRepository.findById(scriptId);
			if (scriptOptional.isEmpty()) {
				LOGGER.error("Script is not available with the id " + scriptId + " and script name" + scriptName);
				continue;
			}
			Script script = scriptOptional.get();
			if (null == script || (script.getCategory() != testSuiteCategory)) {
				// If the script is not available in the database, then then do not add it to
				// the
				// test suite
				if (null == script)
					LOGGER.error("Script is not available with the id " + scriptId + " and script name" + scriptName);
				// If the script is available in the database, but the category is different
				// than
				// the test suite, then do not add it to the test suite
				if (script.getCategory() != testSuiteCategory)
					LOGGER.error("Category of the script added " + scriptName + " is different than the test suite");

				continue;
			}

			scriptTestSuite.setScript(script);
			scriptTestSuite.setTestSuite(testSuite);
			scriptTestSuite.setScriptOrder(i);
			scriptTestSuiteRepository.save(scriptTestSuite);
			LOGGER.debug("Script added to test suite: " + scriptName);
		}
	}

	/**
	 * Convert the test suite to test suite DTO
	 * 
	 * @param TestSuite
	 * @return
	 */
	private TestSuiteDTO convertTestSuiteToTestSuiteDTO(TestSuite testSuite) {
		LOGGER.info("Converting test suite to test suite DTO");
		TestSuiteDTO testSuiteDTO = MapperUtils.convertToTestSuiteDTO(testSuite);
		List<ScriptTestSuite> scriptTestSuiteList = scriptTestSuiteRepository
				.findByTestSuiteOrderByScriptOrderAsc(testSuite);
		List<ScriptListDTO> scriptList = MapperUtils.getScriptList(scriptTestSuiteList);
		testSuiteDTO.setScripts(scriptList);
		LOGGER.info("Test suite converted to test suite DTO");
		return testSuiteDTO;
	}

	/**
	 * This method is used to download the test suite as XML
	 * 
	 * @param TestSuite - the test suite
	 * @return the test suite as XML
	 */
	@Override
	public ByteArrayInputStream downloadTestSuiteAsXML(String testSuite) {
		LOGGER.info("Downloading test suite as XML for the test suite: " + testSuite);
		TestSuite testSuiteObj = testSuiteRepository.findByName(testSuite);
		if (testSuiteObj == null) {
			LOGGER.error("Test suite not found with the name: " + testSuite);
			throw new ResourceNotFoundException(Constants.TEST_SUITE, testSuite);
		}
		String categoryName = testSuiteObj.getCategory().name();
		String testSuiteDesc = testSuiteObj.getDescription();
		List<ScriptTestSuite> scriptTestSuiteList = scriptTestSuiteRepository.findAllByTestSuite(testSuiteObj);
		if (scriptTestSuiteList.isEmpty()) {
			LOGGER.error("No scripts found for the test suite: " + testSuite);
			throw new ResourceNotFoundException(Constants.TEST_SUITE, testSuite);
		}
		// Sort by scriptOrder to preserve order in XML
		scriptTestSuiteList.sort(Comparator.comparingInt(ScriptTestSuite::getScriptOrder));
		List<Script> scriptList = new ArrayList<>();
		for (ScriptTestSuite scriptTestSuite : scriptTestSuiteList) {
			scriptList.add(scriptTestSuite.getScript());
		}
		return downloadTestSuiteXML(scriptList, categoryName, testSuiteDesc);

	}

	/**
	 * This method is used to upload the test suite as XML
	 * 
	 * @param scriptFile - the test suite file
	 * @return - true if the test suite is uploaded successfully, false otherwise
	 */
	@Override
	public boolean uploadTestSuiteAsXML(MultipartFile testSuiteXMLFile) {
		// Validate the uploaded file
		validateFile(testSuiteXMLFile);
		String testSuiteName = testSuiteXMLFile.getOriginalFilename().replace(Constants.XML_EXTENSION,
				Constants.EMPTY_STRING);
		try {
			InputStream xmlInputStream = testSuiteXMLFile.getInputStream();
			return uploadTestSuiteXml(xmlInputStream, testSuiteName);
		} catch (Exception e) {
			LOGGER.error("Error while uploading test suite from XML file", e);
			throw new TDKServiceException("Error while uploading test suite from XML file");
		}
	}

	/**
	 * This method is used to upload the test suite from XML input stream
	 * 
	 * @param xmlInputStream - the XML input stream
	 * @param testSuiteName  - the test suite name
	 * @return - true if the test suite is uploaded successfully, false otherwise
	 */
	public boolean uploadTestSuiteXml(InputStream xmlInputStream, String testSuiteName) {
		LOGGER.info("Uploading test suite from XML for the test suite: " + testSuiteName);
		try {
			// Parse XML
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(xmlInputStream);

			// Normalize XML structure
			document.getDocumentElement().normalize();

			// Extract category
			NodeList categoryList = document.getElementsByTagName("category");
			String category = categoryList.item(0).getTextContent();

			String desc;
			NodeList description = document.getElementsByTagName("description");
			if (null != description && description.getLength() > 0) {
				desc = description.item(0).getTextContent();
			} else {
				desc = "Test suite for " + testSuiteName;

			}

			List<ScriptListDTO> scriptListDTO = new ArrayList<>();

			// Extract scripts
			NodeList scriptNodes = document.getElementsByTagName("script_name");
			for (int i = 0; i < scriptNodes.getLength(); i++) {
				String scriptName = scriptNodes.item(i).getTextContent();

				// Check if script already exists in the database
				Script existingScript = scriptRepository.findByName(scriptName);
				if (existingScript != null) {
					ScriptListDTO scriptDTO = MapperUtils.convertToScriptListDTO(existingScript);
					scriptListDTO.add(scriptDTO);
				}
			}

			TestSuite testSuite = testSuiteRepository.findByName(testSuiteName);
			if (testSuite == null) {

				TestSuiteCreateDTO testSuiteCreateDTO = new TestSuiteCreateDTO();
				testSuiteCreateDTO.setName(testSuiteName);
				testSuiteCreateDTO.setDescription(desc);
				testSuiteCreateDTO.setCategory(category);
				testSuiteCreateDTO.setScripts(scriptListDTO);
				this.createTestSuite(testSuiteCreateDTO);
				return true;
			} else {
				TestSuiteDTO testSuiteDTO = new TestSuiteDTO();
				testSuiteDTO.setId(testSuite.getId());
				testSuiteDTO.setName(testSuiteName);
				testSuiteDTO.setDescription(desc);
				testSuiteDTO.setCategory(category);
				testSuiteDTO.setScripts(scriptListDTO);

				this.updateTheGivenTestSuite(testSuiteDTO, testSuite);
				return true;

			}

		} catch (Exception e) {
			LOGGER.error("Error while uploading script names in test suite", e);
			throw new TDKServiceException("Error while uploading script names in script");
		}
	}

	/**
	 * This method is used to download the test suite as XML
	 * 
	 * @param scriptList   - the list of scripts
	 * @param categoryName - the category name
	 * @return the test suite as XML
	 */
	public ByteArrayInputStream downloadTestSuiteXML(List<Script> scriptList, String categoryName, String description) {
		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder;
		try {
			documentBuilder = documentFactory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();

			// Create root element <xml>
			Element root = document.createElement(Constants.XML);
			document.appendChild(root);

			// Create <test_suite> element
			Element testSuiteElement = document.createElement("test_suite");
			root.appendChild(testSuiteElement);

			// Create <category> element and append it to <test_suite>
			Element categoryElement = document.createElement("category");
			categoryElement.appendChild(document.createTextNode(categoryName));
			testSuiteElement.appendChild(categoryElement);

			Element descriptionElement = document.createElement("description");
			descriptionElement.appendChild(document.createTextNode(description));
			testSuiteElement.appendChild(descriptionElement);

			// Create <scripts> element and append it to <test_suite>
			Element scriptElement = document.createElement("scripts");
			testSuiteElement.appendChild(scriptElement);

			// Iterate over the script list and add each script name to <scripts>
			for (Script script : scriptList) {
				Element scriptName = document.createElement("script_name");
				scriptName.appendChild(document.createTextNode(script.getName()));
				scriptElement.appendChild(scriptName);
			}
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, Constants.YES);
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, Constants.NO);
			transformer.setOutputProperty(OutputKeys.METHOD, Constants.XML);
			DOMSource domSource = new DOMSource(document);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			StreamResult streamResult = new StreamResult(out);
			transformer.transform(domSource, streamResult);
			return new ByteArrayInputStream(out.toByteArray());
		} catch (ParserConfigurationException | TransformerException e) {
			LOGGER.error("Error creating XML from script names: " + e.getMessage());
			throw new TDKServiceException("Error creating XML from script names: " + e.getMessage());
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
	 * This method is used to update the test suite by module name and category.
	 *
	 * @param moduleName - the module name
	 * @param category   - the category
	 * @return the test suite details DTO
	 */
	@Override
	public String updateTestSuiteByModuleNameAndCategory(String moduleName, String category) {
		LOGGER.info("Updating test suite for module: {}", moduleName);

		// Validate module and retrieve it
		Module module = moduleRepository.findByName(moduleName);
		if (module == null) {
			LOGGER.error("Module not found with the name: {}", moduleName);
			throw new ResourceNotFoundException(Constants.MODULE, moduleName);
		}
		LOGGER.debug("Module found: {}", module);

		// Validate category
		Category validatedCategory = commonService.validateCategory(category);
		LOGGER.debug("Category validated: {}", validatedCategory);

		// Retrieve or create the TestSuite for the module
		TestSuite testSuite = testSuiteRepository.findByName(moduleName);
		boolean isNewTestSuite = false;
		if (testSuite == null) {
			testSuite = new TestSuite();
			testSuite.setName(moduleName);
			testSuite.setDescription(moduleName + "_testSuite");
			testSuite.setCategory(validatedCategory);
			testSuite.setUserGroup(module.getUserGroup());
			LOGGER.debug("Created new TestSuite: {}", testSuite);
			isNewTestSuite = true;
		} else {
			LOGGER.debug("Found existing TestSuite: {}", testSuite);
		}

		// Retrieve all scripts for the module
		List<Script> moduleScripts = scriptRepository.findAllByModule(module);
		if (moduleScripts.isEmpty()) {
			LOGGER.error("No scripts found for the module: {}", moduleName);
			throw new ResourceNotFoundException(Constants.SCRIPT, moduleName);
		}
		LOGGER.debug("Module scripts retrieved: {}", moduleScripts);

		// Clear the existing scriptTestSuite collection
		testSuite.getScriptTestSuite().clear();

		// Add the new scripts to the scriptTestSuite collection
		for (Script script : moduleScripts) {
			ScriptTestSuite scriptTestSuite = new ScriptTestSuite();
			scriptTestSuite.setScript(script);
			scriptTestSuite.setTestSuite(testSuite);
			testSuite.getScriptTestSuite().add(scriptTestSuite);
		}

		// Save the updated test suite
		testSuiteRepository.save(testSuite);
		LOGGER.debug("Updated TestSuite with new scripts: {}", testSuite);

		// Return success message
		return isNewTestSuite ? "Test suite created successfully" : "Test suite updated successfully";
	}

	/**
	 * This method is used to download all the test suite as XML
	 * 
	 * @param category - the category
	 * @return the test suite as XML
	 */
	public ByteArrayInputStream downloadAllTestSuiteAsXML(String category) {
		LOGGER.info("Downloading all test suites as XML for the category: " + category);
		Category categoryObj = commonService.validateCategory(category);
		List<TestSuite> testSuiteObj;
		if (Category.RDKV.equals(categoryObj)) {
			testSuiteObj = testSuiteRepository
					.findAllByCategoryIn(Arrays.asList(Category.RDKV, Category.RDKV_RDKSERVICE));
		} else {
			testSuiteObj = testSuiteRepository.findAllByCategory(categoryObj);
		}
		if (testSuiteObj == null || testSuiteObj.isEmpty()) {
			LOGGER.error("No test suites found for the category: " + category);
			throw new ResourceNotFoundException(Constants.TEST_SUITE, category);
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ZipOutputStream zipOut = new ZipOutputStream(out);

		for (TestSuite testSuite : testSuiteObj) {
			try {
				ByteArrayInputStream testSuiteXML = downloadTestSuiteAsXML(testSuite.getName());
				ZipEntry zipEntry = new ZipEntry(testSuite.getName() + ".xml");
				zipOut.putNextEntry(zipEntry);
				byte[] bytes = testSuiteXML.readAllBytes();
				zipOut.write(bytes, 0, bytes.length);
				zipOut.closeEntry();
			} catch (ResourceNotFoundException e) {
				LOGGER.error("Test suite not found: " + testSuite.getName() + ". Skipping.");
				// Continue with next test suite
			} catch (Exception e) {
				LOGGER.error("Error while creating zip file for test suite: " + testSuite.getName(), e);
				// Continue with next test suite
			}
		}

		try {
			zipOut.close();
		} catch (Exception e) {
			LOGGER.error("Error while closing zip output stream");
			throw new TDKServiceException("Error while closing zip output stream");
		}

		return new ByteArrayInputStream(out.toByteArray());
	}

	/**
	 *
	 * @param category
	 * @param isThunderEnabled
	 * @return
	 */
	public List<TestSuiteDetailsResponse> getListofTestSuiteNamesByCategory(String category, boolean isThunderEnabled) {
		LOGGER.info("Fetching test suite names for category: {} with Thunder enabled: {}", category, isThunderEnabled);

		if (!category.equalsIgnoreCase("RDKV") && !category.equalsIgnoreCase("RDKB")
				&& !category.equalsIgnoreCase("RDKC")) {
			LOGGER.error("Invalid category: {}", category);
			throw new UserInputException("Invalid category: " + category);
		}

		List<TestSuite> testSuites = new ArrayList<>();

		if (isThunderEnabled) {
			if (!category.equalsIgnoreCase(Category.RDKV.name())) {
				LOGGER.error("The category {} cannot be thunder enabled", category);
				throw new UserInputException("The category " + category + " cannot be thunder enabled");
			}
			testSuites = testSuiteRepository.findAllByCategory(Category.valueOf(Category.RDKV_RDKSERVICE.name()));
		} else {
			if (category.equalsIgnoreCase(Category.RDKV.name())) {
				testSuites = testSuiteRepository.findAllByCategory(Category.valueOf(category));
			} else if (category.equalsIgnoreCase(Category.RDKB.name())) {
				testSuites = testSuiteRepository.findAllByCategory(Category.valueOf(category));
			} else if (category.equalsIgnoreCase(Category.RDKC.name())) {
				testSuites = testSuiteRepository.findAllByCategory(Category.valueOf(category));
			}
		}

		// Sort alphabetically by test suite name
		testSuites.sort(Comparator.comparing(TestSuite::getName, String.CASE_INSENSITIVE_ORDER));

		return testSuites.stream().map(MapperUtils::convertToTestSuiteDetailsResponse).collect(Collectors.toList());
	}

	/**
	 * This method is used to get the list of all test suites
	 * 
	 * @return the list of test suites
	 */
	@Override
	public List<TestSuiteDetailsResponse> getTestSuiteList() {
		LOGGER.info("Fetching all test suite names");
		List<TestSuite> testSuites = testSuiteRepository.findAll();
		// Sort alphabetically by test suite name
		testSuites.sort(Comparator.comparing(TestSuite::getName, String.CASE_INSENSITIVE_ORDER));

		return testSuites.stream().map(MapperUtils::convertToTestSuiteDetailsResponse).collect(Collectors.toList());
	}

	/**
	 * This method is used to download custom/non-module test suites as XML
	 * (test suites whose names don't match any module name)
	 * 
	 * @param category - the category
	 * @return the custom test suites as XML in a zip file
	 */
	@Override
	public ByteArrayInputStream downloadCustomTestSuiteAsXML(String category) {
		LOGGER.info("Downloading custom (non-module) test suites as XML for the category: " + category);

		List<TestSuite> customTestSuites = this.getCustomTestSuitesByCategory(category);
		// Create ZIP file with custom test suites
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ZipOutputStream zipOut = new ZipOutputStream(out);
		for (TestSuite testSuite : customTestSuites) {
			try {
				ByteArrayInputStream testSuiteXML = downloadTestSuiteAsXML(testSuite.getName());
				ZipEntry zipEntry = new ZipEntry(testSuite.getName() + ".xml");
				zipOut.putNextEntry(zipEntry);
				byte[] bytes = testSuiteXML.readAllBytes();
				zipOut.write(bytes, 0, bytes.length);
				zipOut.closeEntry();
				LOGGER.debug("Added test suite {} to zip", testSuite.getName());
			} catch (ResourceNotFoundException e) {
				LOGGER.warn("Test suite not found: {}. Skipping.", testSuite.getName());
			} catch (Exception e) {
				LOGGER.error("Error while creating zip file for test suite: {}", testSuite.getName(), e);
			}
		}

		try {
			zipOut.close();
		} catch (Exception e) {
			LOGGER.error("Error while closing zip output stream", e);
			throw new TDKServiceException("Error while closing zip output stream");
		}

		LOGGER.info("Successfully downloaded custom test suites zip file for category {}", category);
		return new ByteArrayInputStream(out.toByteArray());
	}

	/**
	 * This method is used to download custom/non-module test suites as TAR.GZ
	 * (test suites whose names don't match any module name)
	 * 
	 * @param category - the category
	 * @return the custom test suites as TAR.GZ
	 */
	@Override
	public ByteArrayInputStream downloadCustomTestSuiteAsTarGz(String category) {
		LOGGER.info("Downloading custom (non-module) test suites as TAR.GZ for the category: " + category);

		List<TestSuite> customTestSuites = this.getCustomTestSuitesByCategory(category);
		// Create TAR.GZ file with custom test suites
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (GZIPOutputStream gzipOut = new GZIPOutputStream(out);
				TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzipOut)) {

			// Set long file mode for tar archives (supports filenames > 100 chars)
			tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			tarOut.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

			for (TestSuite testSuite : customTestSuites) {
				try {
					ByteArrayInputStream testSuiteXML = downloadTestSuiteAsXML(testSuite.getName());
					byte[] bytes = testSuiteXML.readAllBytes();

					// Create tar entry
					String entryName = testSuite.getName() + ".xml";
					TarArchiveEntry tarEntry = new TarArchiveEntry(entryName);
					tarEntry.setSize(bytes.length);
					tarEntry.setModTime(System.currentTimeMillis());

					tarOut.putArchiveEntry(tarEntry);
					tarOut.write(bytes);
					tarOut.closeArchiveEntry();
					LOGGER.debug("Added test suite {} to tar.gz", testSuite.getName());
				} catch (ResourceNotFoundException e) {
					LOGGER.warn("Test suite not found: {}. Skipping.", testSuite.getName());
				} catch (Exception e) {
					LOGGER.error("Error while creating tar.gz file for test suite: {}", testSuite.getName(), e);
				}
			}

			tarOut.finish();

		} catch (Exception e) {
			LOGGER.error("Error while closing tar.gz output stream", e);
			throw new TDKServiceException("Error while creating tar.gz archive");
		}

		LOGGER.info("Successfully downloaded custom test suites as TAR.GZ for category {}", category);
		return new ByteArrayInputStream(out.toByteArray());
	}

	/**
	 * This method is used to get the list of custom/non-module test suites
	 * (test suites whose names don't match any module name)
	 * 
	 * @param category - the category
	 * @return the list of custom test suites
	 */
	public List<TestSuite> getCustomTestSuitesByCategory(String category) {
		LOGGER.info("Fetching custom (non-module) test suites for the category: " + category);
		Category categoryObj = commonService.validateCategory(category);

		// Get all test suites for the category
		List<TestSuite> testSuiteList;
		if (Category.RDKV.equals(categoryObj)) {
			testSuiteList = testSuiteRepository
					.findAllByCategoryIn(Arrays.asList(Category.RDKV, Category.RDKV_RDKSERVICE));
		} else {
			testSuiteList = testSuiteRepository.findAllByCategory(categoryObj);
		}

		if (testSuiteList == null || testSuiteList.isEmpty()) {
			LOGGER.error("No test suites found for the category: " + category);
			throw new ResourceNotFoundException("Custom test suite for category ", category);
		}

		// Get all module names for the category
		List<Module> modules;
		if (Category.RDKV.equals(categoryObj)) {
			modules = moduleRepository.findAllByCategoryIn(Arrays.asList(Category.RDKV, Category.RDKV_RDKSERVICE));
		} else {
			modules = moduleRepository.findAllByCategory(categoryObj);
		}

		// Create a set of module names for faster lookup
		Set<String> moduleNames = modules.stream()
				.map(Module::getName)
				.collect(Collectors.toSet());

		LOGGER.info("Found {} modules for category {}", moduleNames.size(), category);

		// Filter test suites to exclude those whose names match module names
		List<TestSuite> customTestSuites = testSuiteList.stream()
				.filter(testSuite -> !moduleNames.contains(testSuite.getName()))
				.collect(Collectors.toList());

		if (customTestSuites.isEmpty()) {
			LOGGER.error("No custom test suites found for the category: " + category);
			throw new ResourceNotFoundException("Custom Test Suites for", category);
		}

		LOGGER.info("Found {} custom test suites (out of {} total) for category {}",
				customTestSuites.size(), testSuiteList.size(), category);
		return customTestSuites;
	}

	/**
	 * This method is used to upload multiple test suites from an archive file (ZIP
	 * or TAR.GZ)
	 * 
	 * @param archiveFile - the archive file containing multiple test suite XML
	 *                    files
	 * @return String - Summary message of the upload operation
	 */
	@Override
	public String uploadAllTestSuitesFromArchive(MultipartFile archiveFile) {
		LOGGER.info("Uploading all test suites from archive file: {}", archiveFile.getOriginalFilename());

		// Validate the uploaded file
		validateArchiveFile(archiveFile);

		String fileName = archiveFile.getOriginalFilename();
		int successCount = 0;
		int failureCount = 0;

		try {
			// Determine file type and process accordingly
			if (fileName.endsWith(".zip")) {
				LOGGER.info("Processing ZIP archive");
				Map<String, Integer> result = processZipArchive(archiveFile);
				successCount = result.get("success");
				failureCount = result.get("failure");
			} else if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
				LOGGER.info("Processing TAR.GZ archive");
				Map<String, Integer> result = processTarGzArchive(archiveFile);
				successCount = result.get("success");
				failureCount = result.get("failure");
			} else {
				throw new UserInputException("Unsupported file format. Only ZIP and TAR.GZ files are supported.");
			}

		} catch (Exception e) {
			LOGGER.error("Error while uploading test suites from archive", e);
			throw new TDKServiceException("Error while uploading test suites from archive: " + e.getMessage());
		}

		String message = String.format(
				"Upload completed. Success: %d, Failed: %d",
				successCount, failureCount);
		LOGGER.info(message);

		return message;
	}

	/**
	 * Process ZIP archive and upload test suites
	 * 
	 * @param inputStream - the input stream of the ZIP file
	 * @return Map with success, failure, and skipped counts
	 * @throws Exception
	 */
	private Map<String, Integer> processZipArchive(MultipartFile archiveFile) throws Exception {
		int successCount = 0;
		int failureCount = 0;
		InputStream inputStream = archiveFile.getInputStream();
		try (ZipInputStream zipIn = new ZipInputStream(inputStream)) {
			ZipEntry entry;

			while ((entry = zipIn.getNextEntry()) != null) {
				if (!entry.isDirectory() && entry.getName().endsWith(".xml")) {
					LOGGER.debug("Processing entry: {}", entry.getName());

					try {
						// Read the XML content
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						byte[] buffer = new byte[1024];
						int len;
						while ((len = zipIn.read(buffer)) > 0) {
							out.write(buffer, 0, len);
						}

						// Process the test suite XML
						String testSuiteName = extractTestSuiteNameFromFileName(entry.getName());
						ByteArrayInputStream xmlInputStream = new ByteArrayInputStream(out.toByteArray());
						boolean result = uploadTestSuiteXml(xmlInputStream, testSuiteName);

						if (result) {
							successCount++;
						} else {
							failureCount++;
						}

					} catch (Exception e) {
						LOGGER.error("Error processing entry: {}", entry.getName(), e);
						failureCount++;
					}
				}
				zipIn.closeEntry();
			}
		}

		Map<String, Integer> result = new HashMap<>();
		result.put("success", successCount);
		result.put("failure", failureCount);

		return result;
	}

	/**
	 * Process TAR.GZ archive and upload test suites
	 * 
	 * @param inputStream - the input stream of the TAR.GZ file
	 * @return Map with success, failure, and skipped counts
	 * @throws Exception
	 */
	private Map<String, Integer> processTarGzArchive(MultipartFile archiveFile) throws Exception {
		int successCount = 0;
		int failureCount = 0;
		InputStream inputStream = archiveFile.getInputStream();
		try (GZIPInputStream gzipIn = new GZIPInputStream(inputStream);
				TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {

			TarArchiveEntry entry;

			while ((entry = tarIn.getNextTarEntry()) != null) {
				if (!entry.isDirectory() && entry.getName().endsWith(".xml")) {
					LOGGER.debug("Processing entry: {}", entry.getName());

					try {
						// Read the XML content
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						byte[] buffer = new byte[1024];
						int len;
						while ((len = tarIn.read(buffer)) > 0) {
							out.write(buffer, 0, len);
						}

						// Process the test suite XML
						String testSuiteName = extractTestSuiteNameFromFileName(entry.getName());
						ByteArrayInputStream xmlInputStream = new ByteArrayInputStream(out.toByteArray());
						boolean result = uploadTestSuiteXml(xmlInputStream, testSuiteName);

						if (result) {
							successCount++;
						} else {
							failureCount++;
						}

					} catch (Exception e) {
						LOGGER.error("Error processing entry: {}", entry.getName(), e);
						failureCount++;
					}
				}
			}
		}

		Map<String, Integer> result = new HashMap<>();
		result.put("success", successCount);
		result.put("failure", failureCount);

		return result;
	}

	/**
	 * Extract test suite name from file name
	 * 
	 * @param fileName - the file name with path
	 * @return String - the test suite name without extension
	 */
	private String extractTestSuiteNameFromFileName(String fileName) {
		// Remove path and extension
		String name = fileName;

		// Remove directory path if present
		if (name.contains("/")) {
			name = name.substring(name.lastIndexOf("/") + 1);
		}
		if (name.contains("\\")) {
			name = name.substring(name.lastIndexOf("\\") + 1);
		}

		// Remove .xml extension
		if (name.endsWith(".xml")) {
			name = name.substring(0, name.length() - 4);
		}

		return name;
	}

	/**
	 * Validates the uploaded archive file
	 *
	 * @param file the uploaded archive file
	 */
	private void validateArchiveFile(MultipartFile file) {
		String fileName = file.getOriginalFilename();

		if (fileName == null) {
			LOGGER.error("File name is null");
			throw new UserInputException("File name cannot be null.");
		}

		if (!fileName.endsWith(".zip") && !fileName.endsWith(".tar.gz") && !fileName.endsWith(".tgz")) {
			LOGGER.error("Invalid file format: {}", fileName);
			throw new UserInputException("The uploaded file must be a .zip, .tar.gz, or .tgz file.");
		}

		if (file.isEmpty()) {
			LOGGER.error("The uploaded file is empty");
			throw new UserInputException("The uploaded file is empty.");
		}

	}

}
