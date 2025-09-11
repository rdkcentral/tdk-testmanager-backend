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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.rdkm.tdkservice.dto.FunctionCreateDTO;
import com.rdkm.tdkservice.dto.FunctionDTO;
import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.exception.DeleteFailedException;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.model.Function;
import com.rdkm.tdkservice.model.Module;
import com.rdkm.tdkservice.model.Parameter;
import com.rdkm.tdkservice.repository.FunctionRepository;
import com.rdkm.tdkservice.repository.ModuleRepository;
import com.rdkm.tdkservice.repository.ParameterRepository;
import com.rdkm.tdkservice.service.IFunctionService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.MapperUtils;
import com.rdkm.tdkservice.util.Utils;

/**
 * Service implementation for managing function details.
 */
@Service
public class FunctionService implements IFunctionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(FunctionService.class);

	@Autowired
	private FunctionRepository functionRepository;

	@Autowired
	private ModuleRepository moduleRepository;

	@Autowired
	private ParameterRepository parameterRepository;

	/**
	 * Creates a new function.
	 *
	 * @param functionCreateDTO the data transfer object containing the function
	 *                          details
	 * @return true if the function was created successfully, false otherwise
	 */
	@Override
	public boolean createFunction(FunctionCreateDTO functionCreateDTO) {

		LOGGER.info("Creating new function: {}", functionCreateDTO);

		Module module = moduleRepository.findByName(functionCreateDTO.getModuleName());
		if (functionRepository.existsByName(functionCreateDTO.getFunctionName())) {
			LOGGER.error("Function with name {} already exists", functionCreateDTO.getFunctionName());
			throw new ResourceAlreadyExistsException(Constants.FUNCTION_NAME, functionCreateDTO.getFunctionName());
		}

		if (module == null) {
			LOGGER.error("Module not found: {}", functionCreateDTO.getModuleName());
			throw new ResourceNotFoundException(Constants.MODULE_NAME, functionCreateDTO.getModuleName());
		}
		Category category = Category.getCategory(functionCreateDTO.getFunctionCategory());
		if (null == category) {
			throw new ResourceNotFoundException(Constants.CATEGORY, functionCreateDTO.getFunctionCategory());
		}
		Function function = new Function();

		MapperUtils.mapCreateDTOToEntity(function, functionCreateDTO, module);
		try {
			functionRepository.save(function);
		} catch (Exception e) {
			LOGGER.error("Failed to create function: {}", functionCreateDTO, e);
			return false;
		}
		return function != null && function.getId() != null && function.getId() != null;
	}

	/**
	 * Updates an existing function.
	 *
	 * @param functionDTO the data transfer object containing the updated function
	 *                    details
	 * @return true if the function was updated successfully, false otherwise
	 */
	@Override
	public boolean updateFunction(FunctionDTO functionDTO) {
		LOGGER.info("Updating function: {}", functionDTO);
		Function function = functionRepository.findById(functionDTO.getId()).orElse(null);
		if (function == null) {
			LOGGER.error("Function not found: {}", functionDTO.getId());
			throw new ResourceNotFoundException(Constants.FUNCTION_NAME, functionDTO.getFunctionName().toString());
		}

		if (!Utils.isEmpty(functionDTO.getModuleName())) {
			Module module = moduleRepository.findByName(functionDTO.getModuleName());
			if (module == null) {
				LOGGER.error("Module not found: {}", functionDTO.getModuleName());
				throw new ResourceNotFoundException(Constants.MODULE_NAME, functionDTO.getModuleName());
			}
			function.setModule(module);
		}

		if (!Utils.isEmpty(functionDTO.getFunctionName())) {
			Function newFunction = functionRepository.findByName(functionDTO.getFunctionName());
			if (newFunction != null && functionDTO.getFunctionName().equalsIgnoreCase(function.getName())) {
				function.setName(functionDTO.getFunctionName());
			} else {
				if (functionRepository.existsByName(functionDTO.getFunctionName())) {
					LOGGER.info("Function already exists with the same name: " + functionDTO.getFunctionName());
					throw new ResourceAlreadyExistsException(Constants.FUNCTION_NAME, functionDTO.getFunctionName());
				} else {
					function.setName(functionDTO.getFunctionName());
				}
			}
		}

		if (!Utils.isEmpty(functionDTO.getFunctionCategory())) {
			Category category = Category.getCategory(functionDTO.getFunctionCategory());
			if (category == null) {
				throw new ResourceNotFoundException(Constants.CATEGORY, functionDTO.getFunctionCategory());
			}
			function.setCategory(category);
		}

		try {
			functionRepository.save(function);
			LOGGER.info("Function updated successfully: {}", function);
			return true;
		} catch (Exception e) {
			LOGGER.error("Failed to update function: {}", functionDTO, e);
			return false;
		}
	}

	/**
	 * Finds all functions.
	 *
	 * @return a list of data transfer objects containing the details of all
	 *         functions
	 */
	@Override
	public List<FunctionDTO> findAllFunctions() {
		LOGGER.info("Retrieving all functions");
		List<Function> functions = functionRepository.findAll();
		List<FunctionDTO> functionDTOs = functions.stream().map(MapperUtils::convertToFunctionDTO)
				.collect(Collectors.toList());
		LOGGER.info("Retrieved {} functions", functionDTOs.size());
		return functionDTOs;
	}

	/**
	 * Finds a function by its ID.
	 *
	 * @param id the ID of the function
	 * @return the data transfer object containing the details of the function, or
	 *         null if not found
	 */
	@Override
	public FunctionDTO findFunctionById(UUID id) {
		LOGGER.info("Retrieving function by ID: {}", id);
		Function function = functionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Function not found for this id :: ", id.toString()));
		FunctionDTO functionDTO = MapperUtils.convertToFunctionDTO(function);
		LOGGER.info("Retrieved function: {}", functionDTO);
		return functionDTO;
	}

	/**
	 * Deletes a function by its ID.
	 *
	 * @param id the ID of the function
	 * @return true if the function was deleted successfully, false otherwise
	 */
	@Override
	public void deleteFunction(UUID id) {
		LOGGER.info("Deleting function by ID: {}", id);
		if (!functionRepository.existsById(id)) {
			LOGGER.error("Function not found for ID: {}", id);
			throw new ResourceNotFoundException("Function id :: ", id.toString());
		}
		try {
			List<Parameter> parameters = parameterRepository.findAllByFunctionId(id);
			for (Parameter parameter : parameters) {
				parameterRepository.deleteById(parameter.getId());
				LOGGER.info("Deleted parameter with ID: {}", parameter.getId());
			}
			functionRepository.deleteById(id);
			LOGGER.info("Function deleted successfully: {}", id);
		} catch (DataIntegrityViolationException e) {
			LOGGER.error("Failed to delete function by ID: {}", id, e);
			throw new DeleteFailedException();
		}
	}

	/**
	 * Finds all functions by their category.
	 *
	 * @param category the category of the functions
	 * @return a list of data transfer objects containing the details of all
	 *         functions in the specified category
	 */
	@Override
	public List<FunctionDTO> findAllByCategory(String category) {
		LOGGER.info("Retrieving all functions by category: {}", category);
		Utils.checkCategoryValid(category);
		List<Function> functions = functionRepository.findAllByCategory(Category.getCategory(category));
		List<FunctionDTO> functionDTOs = functions.stream().map(MapperUtils::convertToFunctionDTO)
				.collect(Collectors.toList());
		LOGGER.info("Retrieved {} functions by category {}", functionDTOs.size(), category);
		return functionDTOs;
	}

	/**
	 * Finds all functions by module.
	 *
	 * @param moduleName the name of the module
	 * @return a list of data transfer objects containing the details of all
	 *         functions in the specified module
	 */
	@Override
	public List<FunctionDTO> findAllFunctionsByModule(String moduleName) {
		LOGGER.info("Fetching all functions for module: {}", moduleName);
		Module module = moduleRepository.findByName(moduleName);
		if (module == null) {
			throw new ResourceNotFoundException("Module", moduleName);
		}else {
			LOGGER.info("Module found: {}", module.getName());
		}
		List<Function> functions = functionRepository.findAllByModuleId(module.getId());
		return functions.stream().map(MapperUtils::convertToFunctionDTO).collect(Collectors.toList());
	}

	/**
	 * Finds all function names by module.
	 *
	 * @param ModueName the name of the module
	 * @return a list of all function names
	 */
	@Override
	public List<String> findAllFunctionNameByModule(String ModueName) {
		LOGGER.info("Retrieving all functions by category: {}", ModueName);
		Module module = moduleRepository.findByName(ModueName);
		if (module == null) {
			throw new ResourceNotFoundException(Constants.MODULE_NAME, ModueName);
		}
		List<Function> functions = functionRepository.findAllByModule(module);
		List<String> functionNames = functions.stream().map(Function::getName).collect(Collectors.toList());
		LOGGER.info("Retrieved function names: {}", functionNames);
		return functionNames;
	}
}