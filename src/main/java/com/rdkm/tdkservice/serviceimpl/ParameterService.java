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

import static com.rdkm.tdkservice.util.MapperUtils.mapDTOCreateParameterTypeToEntity;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.rdkm.tdkservice.dto.ParameterCreateDTO;
import com.rdkm.tdkservice.dto.ParameterDTO;
import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.enums.ParameterDataType;
import com.rdkm.tdkservice.exception.DeleteFailedException;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.model.Function;
import com.rdkm.tdkservice.model.Parameter;
import com.rdkm.tdkservice.model.PrimitiveTest;
import com.rdkm.tdkservice.model.PrimitiveTestParameter;
import com.rdkm.tdkservice.repository.FunctionRepository;
import com.rdkm.tdkservice.repository.ParameterRepository;
import com.rdkm.tdkservice.repository.PrimitiveTestParameterRepository;
import com.rdkm.tdkservice.repository.PrimitiveTestRepository;
import com.rdkm.tdkservice.service.IParameterService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.MapperUtils;
import com.rdkm.tdkservice.util.Utils;

import jakarta.transaction.Transactional;

/**
 * Service implementation for managing parameter types.
 */
@Service
public class ParameterService implements IParameterService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ParameterService.class);

	@Autowired
	private ParameterRepository parameterRepository;

	@Autowired
	private FunctionRepository functionRepository;

	@Autowired
	private PrimitiveTestRepository primitiveTestRepository;

	@Autowired
	private PrimitiveTestParameterRepository primitiveTestParameterRepository;

	/**
	 * Creates a new parameter type.
	 *
	 * @param parameterCreateDTO the data transfer object containing the parameter
	 *                           type details
	 * @return true if the parameter type was created successfully, false otherwise
	 */
	@Override
	public boolean createParameter(ParameterCreateDTO parameterCreateDTO) {
		LOGGER.info("Creating new parameter : {}", parameterCreateDTO);
		Parameter parameter = new Parameter();
		Category category = Category.getCategory(parameterCreateDTO.getParameterCategory());
		Function function = functionRepository.findByNameAndCategory(parameterCreateDTO.getFunction(), category);
		if (function == null) {
			throw new ResourceNotFoundException(Constants.FUNCTION_NAME, parameterCreateDTO.getFunction());
		}

		if (parameterRepository.existsByNameAndFunction(parameterCreateDTO.getParameterName(), function)) {
			LOGGER.error("Parameter with name {} already exists", parameterCreateDTO.getParameterName());
			throw new ResourceAlreadyExistsException(Constants.PARAMETER_NAME, parameterCreateDTO.getParameterName());
		}
		mapDTOCreateParameterTypeToEntity(parameter, parameterCreateDTO, function);
		parameterRepository.save(parameter);

		LOGGER.info("Parameter  created successfully: {}", parameter);
		try {
			parameterRepository.save(parameter);
			LOGGER.info("Parameter created successfully: {}", parameter);
		} catch (Exception e) {
			LOGGER.error("Failed to create parameter: {}", parameterCreateDTO, e);
			return false;
		}
		return parameter != null && parameter.getId() != null && parameter.getId() != null;
	}

	/**
	 * Updates an existing parameter.
	 *
	 * @param parameterDTO the data transfer object containing the updated parameter
	 *                     details
	 * @return true if the parameter type was updated successfully, false otherwise
	 */

	@Override
	@Transactional
	public boolean updateParameter(ParameterDTO parameterDTO) {
		LOGGER.info("Updating parameter with ID: {}", parameterDTO.getId());

		// Find existing parameter
		Parameter parameter = parameterRepository.findById(parameterDTO.getId()).orElseThrow(() -> {
			LOGGER.error("Parameter not found with ID: {}", parameterDTO.getId());
			return new ResourceNotFoundException(Constants.PARAMETER_NAME, parameterDTO.getId().toString());
		});

		// Find function by name and category
		Category category = Category.getCategory(parameterDTO.getParameterCategory());
		Function function = functionRepository.findByNameAndCategory(parameterDTO.getFunction(), category);
		if (function == null) {
			LOGGER.error("Function not found: {} in category: {}", parameterDTO.getFunction(), category);
			throw new ResourceNotFoundException(Constants.FUNCTION_NAME, parameterDTO.getFunction());
		}

		// Store original name for reference in primitive test parameters update
		String originalParameterName = parameter.getName();
		LOGGER.debug("Original parameter name: {}", originalParameterName);

		// Update parameter properties
		updateParameterProperties(parameter, parameterDTO, function);

		// Update associated primitive test parameters
		updateRelatedPrimitiveTestParameters(function, originalParameterName, parameterDTO);

		// Save updated parameter
		parameterRepository.save(parameter);
		LOGGER.info("Parameter updated successfully: {}", parameter);
		return true;
	}

	/**
	 * Updates the properties of a parameter entity based on the DTO values.
	 * 
	 * @param parameter
	 * @param parameterDTO
	 * @param function
	 * 
	 * @return void
	 */
	private void updateParameterProperties(Parameter parameter, ParameterDTO parameterDTO, Function function) {
		// Update parameter name if provided
		if (!Utils.isEmpty(parameterDTO.getParameterName())
				&& !parameterDTO.getParameterName().equals(parameter.getName())) {

			// Check if another parameter with the same name exists
			if (parameterRepository.existsByNameAndFunction(parameterDTO.getParameterName(), function)) {
				LOGGER.error("Parameter already exists with name: {} for function: {}", parameterDTO.getParameterName(),
						function.getName());
				throw new ResourceAlreadyExistsException(Constants.PARAMETER_NAME, parameterDTO.getParameterName());
			}

			parameter.setName(parameterDTO.getParameterName());
		}

		// Update function reference if provided
		if (!Utils.isEmpty(parameterDTO.getFunction())) {
			parameter.setFunction(function);
		}

		// Update data type if provided
		if (parameterDTO.getParameterDataType() != null) {
			parameter.setParameterDataType(parameterDTO.getParameterDataType());
		}

		// Update range value if provided
		if (!Utils.isEmpty(parameterDTO.getParameterRangeVal())) {
			parameter.setRangeVal(parameterDTO.getParameterRangeVal());
		}
	}

	/**
	 * Updates primitive test parameters associated with the parameter being
	 * updated.
	 * 
	 * @param function
	 * @param originalParameterName
	 * @param parameterDTO
	 * 
	 * @return void
	 */
	private void updateRelatedPrimitiveTestParameters(Function function, String originalParameterName,
			ParameterDTO parameterDTO) {
		LOGGER.debug("Updating primitive test parameters for function: {} with original parameter name: {}",
				function.getName(), originalParameterName);

		List<PrimitiveTest> primitiveTests = primitiveTestRepository.findByFunction(function);
		for (PrimitiveTest primitiveTest : primitiveTests) {
			PrimitiveTestParameter primitiveParam = primitiveTestParameterRepository
					.findByPrimitiveTestAndParameterName(primitiveTest, originalParameterName);

			if (primitiveParam != null) {
				LOGGER.debug("Updating PrimitiveTestParameter ID: {} for test: {}", primitiveParam.getId(),
						primitiveTest.getName());

				// Update parameter name if provided
				if (!Utils.isEmpty(parameterDTO.getParameterName())) {
					primitiveParam.setParameterName(parameterDTO.getParameterName());
				}

				// Update parameter range if provided
				if (!Utils.isEmpty(parameterDTO.getParameterRangeVal())) {
					primitiveParam.setParameterRange(parameterDTO.getParameterRangeVal());
				}

				// Update parameter type if provided
				if (parameterDTO.getParameterDataType() != null) {
					primitiveParam.setParameterType(parameterDTO.getParameterDataType().toString());
				}

				primitiveTestParameterRepository.save(primitiveParam);
			}
		}
	}

	/**
	 * Finds all parameter.
	 *
	 * @return a list of data transfer objects containing the details of all
	 *         parameter
	 */
	@Override
	public List<ParameterDTO> findAllParameters() {
		LOGGER.info("Retrieving all parameter ");
		List<Parameter> parameters = parameterRepository.findAll();
		List<ParameterDTO> parameterDTOs = parameters.stream().map(MapperUtils::convertToParameterTypeDTO)
				.collect(Collectors.toList());
		LOGGER.info("Retrieved {} parameter", parameterDTOs.size());
		return parameterDTOs;
	}

	/**
	 * Finds a parameter type by its ID.
	 *
	 * @param id the ID of the parameter
	 * @return the data transfer object containing the details of the parameter
	 *         type, or null if not found
	 */
	@Override
	public ParameterDTO findParameterById(UUID id) {
		LOGGER.info("Retrieving parameter by ID: {}", id);
		try {
			Parameter parameter = parameterRepository.findById(id).orElseThrow(
					() -> new ResourceNotFoundException("ParameterType not found for this id :: ", id.toString()));
			ParameterDTO parameterDTO = MapperUtils.convertToParameterTypeDTO(parameter);
			LOGGER.info("Retrieved parameter: {}", parameterDTO);
			return parameterDTO;
		} catch (ResourceNotFoundException e) {
			LOGGER.error("Parameter not found for ID: {}", id);
			throw e;
		}
	}

	/**
	 * Deletes a parameter type by its ID along with all associated primitive test
	 * parameters.
	 *
	 * @param id the ID of the parameter
	 * @return true if the parameter was deleted successfully
	 * @throws ResourceNotFoundException if parameter is not found
	 * @throws DeleteFailedException     if deletion fails due to constraints
	 */
	@Override
	@Transactional
	public boolean deleteParameter(UUID id) {
		LOGGER.info("Deleting parameter by ID: {}", id);

		// Find parameter first to get its details
		Parameter parameter = parameterRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException("ParameterType not found for this id :: ", id.toString()));

		// Find and delete all associated primitive test parameters
		try {
			Function function = parameter.getFunction();
			String parameterName = parameter.getName();

			LOGGER.info("Finding and deleting primitive test parameters for parameter: {}", parameterName);
			List<PrimitiveTest> primitiveTests = primitiveTestRepository.findByFunction(function);

			int deletedCount = 0;
			for (PrimitiveTest primitiveTest : primitiveTests) {
				PrimitiveTestParameter primitiveParam = primitiveTestParameterRepository
						.findByPrimitiveTestAndParameterName(primitiveTest, parameterName);

				if (primitiveParam != null) {
					LOGGER.debug("Deleting PrimitiveTestParameter ID: {} for test: {}", primitiveParam.getId(),
							primitiveTest.getName());
					primitiveTestParameterRepository.delete(primitiveParam);
					deletedCount++;
				}
			}

			LOGGER.info("Deleted {} associated primitive test parameters", deletedCount);

			// Now delete the parameter itself
			parameterRepository.deleteById(id);
			LOGGER.info("Parameter deleted successfully: {}", id);
			return true;
		} catch (DataIntegrityViolationException e) {
			LOGGER.error("Error occurred while deleting parameter with id: " + id, e);
			throw new DeleteFailedException();
		} catch (Exception e) {
			LOGGER.error("Unexpected error occurred while deleting parameter with id: " + id, e);
			throw new TDKServiceException("Failed to delete parameter with id: " + id);
		}
	}

	/**
	 * Finds all parameters by their function.
	 *
	 * @param functionName the name of the function
	 * @return a list of data transfer objects containing the details of all
	 *         parameters
	 */
	@Override
	public List<ParameterDTO> findAllParametersByFunction(String functionName, String categoryName) {
		LOGGER.info("Fetching all parameters for function: {}", functionName);
		Category category = Category.getCategory(categoryName);
		if (null == category) {
			throw new ResourceNotFoundException(Constants.CATEGORY, categoryName);
		}
		Function function = functionRepository.findByNameAndCategory(functionName, category);
		if (function == null) {
			throw new ResourceNotFoundException("Function", functionName);
		}
		List<Parameter> parameters = parameterRepository.findByFunction(function);
		return parameters.stream().map(MapperUtils::convertToParameterTypeDTO).collect(Collectors.toList());
	}

	/**
	 * Finds all parameter enums.
	 *
	 * @return a list of parameter enums
	 */
	@Override
	public List<ParameterDataType> getAllParameterEnums() {
		LOGGER.info("Fetching all parameter enums");
		return Arrays.asList(ParameterDataType.values());
	}
}