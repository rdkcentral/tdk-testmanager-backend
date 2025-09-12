package com.rdkm.tdkservice.service;

import java.util.List;
import java.util.UUID;

import com.rdkm.tdkservice.dto.ParameterCreateDTO;
import com.rdkm.tdkservice.dto.ParameterDTO;
import com.rdkm.tdkservice.enums.ParameterDataType;

/**
 * Service interface for managing parameter type details.
 */
public interface IParameterService {

	/**
	 * Creates a new parameter type.
	 *
	 * @param parameterTypeCreateDTO the data transfer object containing the
	 *                               parameter type details
	 * @return true if the parameter type was created successfully, false otherwise
	 */
	public boolean createParameter(ParameterCreateDTO parameterTypeCreateDTO);

	/**
	 * Updates an existing parameter type.
	 *
	 * @param parameterDTO the data transfer object containing the updated parameter
	 *                     type details
	 * @return true if the parameter type was updated successfully, false otherwise
	 */
	public boolean updateParameter(ParameterDTO parameterDTO);

	/**
	 * Finds all parameter types.
	 *
	 * @return a list of data transfer objects containing the details of all
	 *         parameter types
	 */
	public List<ParameterDTO> findAllParameters();

	/**
	 * Finds a parameter type by its ID.
	 *
	 * @param id the ID of the parameter type
	 * @return the data transfer object containing the details of the parameter
	 *         type, or null if not found
	 */
	public ParameterDTO findParameterById(UUID id);

	/**
	 * Deletes a parameter type by its ID.
	 *
	 * @param id the ID of the parameter type
	 * @return true if the parameter type was deleted successfully, false otherwise
	 */
	public boolean deleteParameter(UUID id);

	/**
	 * Finds all parameters by their function.
	 *
	 * @param functionName the name of the function
	 * @return a list of data transfer objects containing the details of all
	 *         parameters for the specified function
	 */
	public List<ParameterDTO> findAllParametersByFunction(String functionName);

	/**
	 * Finds all parameter enums.
	 *
	 * @return a list of parameter enums
	 */
	public List<ParameterDataType> getAllParameterEnums();
}