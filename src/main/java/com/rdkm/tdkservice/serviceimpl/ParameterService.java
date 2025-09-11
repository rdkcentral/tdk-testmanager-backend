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
import com.rdkm.tdkservice.enums.ParameterDataType;
import com.rdkm.tdkservice.exception.DeleteFailedException;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.model.Function;
import com.rdkm.tdkservice.model.Parameter;
import com.rdkm.tdkservice.repository.FunctionRepository;
import com.rdkm.tdkservice.repository.ParameterRepository;
import com.rdkm.tdkservice.service.IParameterService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.MapperUtils;
import com.rdkm.tdkservice.util.Utils;

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

		Function function = functionRepository.findByName(parameterCreateDTO.getFunction());
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
	public boolean updateParameter(ParameterDTO parameterDTO) {
		LOGGER.info("Updating parameter : {}", parameterDTO);

		Parameter parameter = parameterRepository.findById(parameterDTO.getId()).orElse(null);
		if (parameter == null) {
			LOGGER.error("Parameter not found: {}", parameterDTO);
			throw new ResourceNotFoundException(Constants.PARAMETER_NAME, parameterDTO.toString());
		}

		Function function = functionRepository.findByName(parameterDTO.getFunction());
		if (function == null) {
			LOGGER.error("Function not found: {}", parameterDTO.getFunction());
			throw new ResourceNotFoundException(Constants.FUNCTION_NAME, parameterDTO.getFunction());
		}

		if (!Utils.isEmpty(parameterDTO.getParameterName())) {
			Parameter newParameter = parameterRepository.findByNameAndFunction(parameterDTO.getParameterName(),
					function);
			if (newParameter != null && parameterDTO.getParameterName().equalsIgnoreCase(parameter.getName())) {
				parameter.setName(parameterDTO.getParameterName());
			} else {
				if (parameterRepository.existsByNameAndFunction(parameterDTO.getParameterName(), function)) {
					LOGGER.info("Parameter already exists with the same name: " + parameterDTO.getParameterName());
					throw new ResourceAlreadyExistsException(Constants.PARAMETER_NAME, parameterDTO.getParameterName());
				} else {
					parameter.setName(parameterDTO.getParameterName());
				}
			}
		}

		if (!Utils.isEmpty(parameterDTO.getFunction())) {
			parameter.setFunction(function);
		}

		if (parameterDTO.getParameterDataType() != null) {
			parameter.setParameterDataType(parameterDTO.getParameterDataType());
		}

		if (!Utils.isEmpty(parameterDTO.getParameterRangeVal())) {
			parameter.setRangeVal(parameterDTO.getParameterRangeVal());
		}
		try {
			parameterRepository.save(parameter);
			LOGGER.info("Parameter updated successfully: {}", parameter);
			return true;
		} catch (Exception e) {
			LOGGER.error("Failed to update parameter: {}", parameterDTO, e);
			return false;
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
	 * Deletes a parameter type by its ID.
	 *
	 * @param id the ID of the parameter
	 * @return true if the parameter was deleted successfully, false otherwise
	 */
	@Override
	public boolean deleteParameter(UUID id) {
		LOGGER.info("Deleting parameter by ID: {}", id);
		parameterRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException("ParameterType not found for this id :: ", id.toString()));
		try {
			parameterRepository.deleteById(id);
			LOGGER.info("Parameter deleted successfully: {}", id);
			return true;
		} catch (DataIntegrityViolationException e) {
			LOGGER.error("Error occurred while deleting deviceType with id: " + id, e);
			throw new DeleteFailedException();
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
	public List<ParameterDTO> findAllParametersByFunction(String functionName) {
		LOGGER.info("Fetching all parameters for function: {}", functionName);
		Function function = functionRepository.findByName(functionName);
		if (function == null) {
			throw new ResourceNotFoundException("Function", functionName);
		}
		List<Parameter> parameters = parameterRepository.findAllByFunctionId(function.getId());
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