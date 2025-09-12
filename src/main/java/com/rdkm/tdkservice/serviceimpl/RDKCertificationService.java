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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jayway.jsonpath.internal.Utils;
import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.exception.UserInputException;
import com.rdkm.tdkservice.service.IRDKCertificationService;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.util.Constants;

/**
 * Service class for RDK Certification
 */
@Service
public class RDKCertificationService implements IRDKCertificationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RDKCertificationService.class);

	@Autowired
	CommonService commonService;

	/**
	 * Method to create or upload a config file
	 * 
	 * @param file
	 * @return boolean
	 */
	@Override
	public boolean createOrUploadConfigFile(MultipartFile file) {
		LOGGER.info("Inside createOrUpdateConfigFile method with fileName: {}", file.getOriginalFilename());
		commonService.validatePythonFile(file);
		Path testVariableConfig = Paths.get(
				AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + Constants.RDK_CERTIFICATION_FILE_LIST);
		if (!Files.exists(testVariableConfig)) {
			try {
				Files.createFile(testVariableConfig);
			} catch (IOException e) {
				LOGGER.error("Error creating config file: " + e.getMessage());
				throw new TDKServiceException("Error creating config file: " + e.getMessage());
			}
		}
		try {
			MultipartFile fileWithHeader = commonService.addHeader(file);
			Path uploadPath = Paths.get(AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR);

			if (isFileNameExists(
					file.getOriginalFilename().replace(Constants.PYTHON_FILE_EXTENSION, Constants.EMPTY_STRING),
					testVariableConfig.toString())) {
				LOGGER.info("Config file already exists, updating the file");
			} else {
				addToConfig(file.getOriginalFilename(), testVariableConfig.toString());
			}

			Path filePath = uploadPath.resolve(fileWithHeader.getOriginalFilename());
			Files.copy(fileWithHeader.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.info("File uploaded successfully: {}", fileWithHeader.getOriginalFilename());
			return true;
		} catch (IOException e) {
			LOGGER.error("Error saving file: " + e.getMessage());
			throw new TDKServiceException("Error saving file: " + e.getMessage());
		}
	}

	/**
	 * Checks if a given file name exists in the specified configuration file.
	 *
	 * @param fileName       the name of the file to check for existence
	 * @param configFilePath the path to the configuration file containing the list
	 *                       of file names
	 * @return true if the file name exists in the configuration file, false
	 *         otherwise
	 * @throws IOException if an I/O error occurs reading from the configuration
	 *                     file
	 */
	private static boolean isFileNameExists(String fileName, String configFilePath) throws IOException {
		List<String> existingFileNames = readFile(configFilePath);
		for (String existingFileName : existingFileNames) {

			if (existingFileName.trim().equals(fileName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Reads a file from the specified file path and returns a list of lines that do
	 * not start with a '#' character.
	 *
	 * @param filePath the path of the file to be read
	 * @return a list of lines from the file that do not start with a '#' character
	 * @throws IOException if an I/O error occurs reading from the file
	 */
	private static List<String> readFile(String filePath) throws IOException {
		List<String> lines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.startsWith("#") && !(Utils.isEmpty(line))) {
					lines.add(line.trim());
				}
			}
		}
		return lines;
	}

	/**
	 * Adds the specified file name to the configuration file.
	 *
	 * @param fileName       the name of the file to be added to the configuration
	 * @param configFilePath the path to the configuration file
	 * @throws IOException if an I/O error occurs
	 */
	private static void addToConfig(String fileName, String configFilePath) throws IOException {

		try (FileWriter fw = new FileWriter(configFilePath, true); BufferedWriter bw = new BufferedWriter(fw)) {
			bw.newLine();
			bw.write(fileName.replace(Constants.PYTHON_FILE_EXTENSION, Constants.EMPTY_STRING));
		}

	}

	/**
	 * Method to download a config file
	 * 
	 * @param fileName
	 * @return Resource
	 */
	@Override
	public Resource downloadConfigFile(String fileName) {
		LOGGER.info("Inside downloadConfigFile method with configFileName: {}", fileName);
		String configFileLocation = AppConfig.getRealPath() + Constants.BASE_FILESTORE_DIR
				+ Constants.FILE_PATH_SEPERATOR;
		Path path = null;
		Path pythonFilepath = Paths.get(configFileLocation).resolve(fileName + Constants.PYTHON_FILE_EXTENSION);
		Path configFilePath = Paths.get(configFileLocation).resolve(fileName + Constants.CONFIG_FILE_EXTENSION);

		if (Files.exists(pythonFilepath)) {
			path = pythonFilepath;
		} else if (Files.exists(configFilePath)) {
			path = configFilePath;
		}
		if (!Files.exists(path)) {
			LOGGER.error("Python config file not found: {}", fileName);
			throw new ResourceNotFoundException("Python Config file", fileName);
		}
		Resource resource = null;
		try {
			resource = new UrlResource(path.toUri());
		} catch (MalformedURLException e) {
			LOGGER.error("Python config file not found: {}", fileName);
		}
		// Loads the resource and checks if it exists
		if (null != resource && !resource.exists()) {
			LOGGER.error("Python config file not found: {}", fileName);
			return null;
		}
		return resource;
	}

	/**
	 * Method to get all config file names
	 * 
	 * @return List<String>
	 */
	@Override
	public List<String> getAllConfigFileNames() {
		LOGGER.info("Inside getAllConfigFileNames method");
		List<String> configFileNames = new ArrayList<>();
		try {
			Path testVariableConfig = Paths.get(AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
					+ Constants.RDK_CERTIFICATION_FILE_LIST);

			if (!Files.exists(testVariableConfig)) {
				try {
					Files.createFile(testVariableConfig);
				} catch (IOException e) {
					LOGGER.error("Error creating config file: " + e.getMessage());
					throw new TDKServiceException("Error creating config file: " + e.getMessage());
				}
			}
			configFileNames = readFile(testVariableConfig.toString());

		} catch (Exception e) {
			LOGGER.error("Error in getting config file names: " + e.getMessage());
			throw new TDKServiceException("Error in getting config file names: " + e.getMessage());
		}
		return configFileNames;
	}

	/**
	 * Method to get the content of the config file
	 * 
	 * @param fileName
	 * @return String
	 * 
	 */
	@Override
	public String getConfigFileContent(String fileName) {
		LOGGER.info("Inside getConfigFileContent method with fileName: {}", fileName);
		String fileContent = "";
		try {
			String pyFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + fileName
					+ Constants.PYTHON_FILE_EXTENSION;
			File pyFile = new File(pyFilePath);

			// Try with Config extension if Python file doesn't exist
			String configFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + fileName + ".config";
			File configFile = new File(configFilePath);

			// Check which file exists and read its content
			if (pyFile.exists()) {
				fileContent = new String(Files.readAllBytes(pyFile.toPath()));
				LOGGER.info("Reading content from Python file: {}", pyFilePath);
			} else if (configFile.exists()) {
				fileContent = new String(Files.readAllBytes(configFile.toPath()));
				LOGGER.info("Reading content from Config file: {}", configFilePath);
			} else {
				throw new UserInputException("The configuration file with name " + fileName
						+ " not found with either .py or .config extension");
			}
		} catch (Exception e) {
			LOGGER.error("Error in getting config file content: " + e.getMessage());
			throw new TDKServiceException("Error in getting config file content: " + e.getMessage());
		}
		return fileContent;
	}

	/**
	 * Method to delete a config file
	 * 
	 * @param fileName
	 * @return boolean
	 */
	@Override
	public boolean deleteConfigFile(String fileName) {
		LOGGER.info("Inside deleteConfigFile method with fileName: {}", fileName);
		try {
			Path testVariableConfig = Paths.get(AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
					+ Constants.RDK_CERTIFICATION_FILE_LIST);
			deleteFromTestVariableFile(fileName, testVariableConfig.toString());
			String pythonFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + fileName
					+ Constants.PYTHON_FILE_EXTENSION;
			File pythonFile = new File(pythonFilePath);

			String configFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + fileName + Constants.CONFIG_FILE_EXTENSION;
			File configFile = new File(configFilePath);
			if (pythonFile.exists()) {
				pythonFile.delete();
				LOGGER.info("Config file deleted successfully");
				return true;
			} else if (configFile.exists()) {
				configFile.delete();
				LOGGER.info("Config file deleted successfully");
				return true;
			} else {
				LOGGER.error("Config file not found for the name: {}", fileName);
				return false;
			}
		} catch (Exception e) {
			LOGGER.error("Error in deleting config file: " + e.getMessage());
			throw new TDKServiceException("Error in deleting config file: " + e.getMessage());
		}

	}

	/**
	 * Deletes a specific line from a file.
	 *
	 * @param fileName the name of the file to be deleted from the list of lines in
	 *                 the file
	 * @param path     the path to the file from which the line should be deleted
	 * @throws TDKServiceException if an I/O error occurs while reading or writing
	 *                             the file
	 */
	private void deleteFromTestVariableFile(String fileName, String path) {
		try {
			// Read all lines from the file
			List<String> lines = Files.readAllLines(Paths.get(path));
			List<String> updatedLines = new ArrayList<>();

			// Iterate through lines and keep only those not matching the line to remove
			for (String line : lines) {
				if (!line.trim().equals(fileName.trim())) {
					updatedLines.add(line);
				}
			}

			// Write the updated lines back to the file
			Files.write(Paths.get(path), updatedLines);
		} catch (IOException e) {
			LOGGER.error("Error deleting file from config: " + e.getMessage());
			throw new TDKServiceException("Error deleting file from config: " + e.getMessage());
		}

	}

	/**
	 * Updates the configuration file with the provided MultipartFile.
	 * 
	 * @param file the MultipartFile to be uploaded and updated
	 * @return true if the file is successfully uploaded and updated, false
	 *         otherwise
	 * @throws ResourceNotFoundException if the configuration file does not exist
	 * @throws TDKServiceException       if there is an error saving the file
	 */
	@Override
	public boolean updateConfigFile(MultipartFile file) {
		LOGGER.info("Inside updateConfigFile method with fileName: {}", file.getOriginalFilename());
		commonService.validatePythonFile(file);
		try {
			MultipartFile fileWithHeader = commonService.addHeader(file);
			Path testVariableConfig = Paths.get(AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
					+ Constants.RDK_CERTIFICATION_FILE_LIST);

			String baseFileName = file.getOriginalFilename().replace(Constants.PYTHON_FILE_EXTENSION,
					Constants.EMPTY_STRING);
			// Check if file exists in the config list
			if (!isFileNameExists(baseFileName, testVariableConfig.toString())) {
				throw new ResourceNotFoundException("Config file", file.getOriginalFilename());
			}

			// Determine the existing extension (.py or .config)
			String extension = Constants.PYTHON_FILE_EXTENSION; // Default extension

			// Check if file with .py extension exists
			File pyFile = new File(AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + baseFileName
					+ Constants.PYTHON_FILE_EXTENSION);
			// Check if file with .config extension exists
			File configFile = new File(
					AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + baseFileName + ".config");

			if (configFile.exists()) {
				extension = ".config";
				LOGGER.info("Found existing file with .config extension, will use this extension");
			} else if (pyFile.exists()) {
				LOGGER.info("Found existing file with .py extension, will use this extension");
			} else {
				LOGGER.info("No existing file found, using default .py extension");
			}
			// Save with the determined extension
			Path uploadPath = Paths.get(AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR);
			Path filePath = uploadPath.resolve(baseFileName + extension);
			Files.copy(fileWithHeader.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.info("File uploaded successfully: {}", fileWithHeader.getOriginalFilename());
			return true;

		} catch (ResourceNotFoundException e) {
			LOGGER.error("Error saving file: " + e.getMessage());
			throw e;
		} catch (IOException e) {
			LOGGER.error("Error saving file: " + e.getMessage());
			throw new TDKServiceException("Error saving file: " + e.getMessage());
		} catch (Exception ex) {
			LOGGER.error("Error saving file: ", ex.getMessage());
			throw new TDKServiceException("Error saving file: " + ex.getMessage());

		}
	}
}
