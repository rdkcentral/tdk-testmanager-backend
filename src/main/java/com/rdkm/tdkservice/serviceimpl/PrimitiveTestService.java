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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.rdkm.tdkservice.dto.ParameterValueDTO;
import com.rdkm.tdkservice.dto.PrimitiveTestCreateDTO;
import com.rdkm.tdkservice.dto.PrimitiveTestDTO;
import com.rdkm.tdkservice.dto.PrimitiveTestNameAndIdDTO;
import com.rdkm.tdkservice.dto.PrimitiveTestParameterDTO;
import com.rdkm.tdkservice.dto.PrimitiveTestUpdateDTO;
import com.rdkm.tdkservice.enums.ParameterDataType;
import com.rdkm.tdkservice.exception.DeleteFailedException;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
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
import com.rdkm.tdkservice.service.IPrimitiveTestService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.MapperUtils;

/**
 * The PrimitiveTestService class is used to perform the operations on primitive
 * test.
 */

@Service
public class PrimitiveTestService implements IPrimitiveTestService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PrimitiveTestService.class);

	@Autowired
	ModuleRepository moduleRepository;

	@Autowired
	FunctionRepository functionRepository;

	@Autowired
	ParameterRepository parameterRepository;

	@Autowired
	PrimitiveTestParameterRepository primitiveTestParameterRepository;

	@Autowired
	PrimitiveTestRepository primitiveTestRepository;

	/**
	 * This method is used to create the primitive test
	 *
	 * @param primitiveTestDTO - PrimitiveTestCreateDTO object
	 * @return boolean - true if primitive test is created successfully
	 */

	@Override
	public boolean createPrimitiveTest(PrimitiveTestCreateDTO primitiveTestDTO) {
		LOGGER.info("Creating primitive test: " + primitiveTestDTO.toString());
		if (primitiveTestRepository.existsByName(primitiveTestDTO.getPrimitiveTestname())) {
			LOGGER.info("Primitive test already exists with the same name: " + primitiveTestDTO.getPrimitiveTestname());
			throw new ResourceAlreadyExistsException(Constants.PRIMITIVE_TEST_NAME,
					primitiveTestDTO.getPrimitiveTestname());
		}
		Module module = moduleRepository.findByName(primitiveTestDTO.getPrimitiveTestModuleName());
		if (module == null) {
			LOGGER.error("Module not found with the name: " + primitiveTestDTO.getPrimitiveTestModuleName());
			throw new ResourceNotFoundException(Constants.MODULE_NAME, primitiveTestDTO.getPrimitiveTestModuleName());
		}
		Function function = functionRepository.findByName(primitiveTestDTO.getPrimitiveTestfunctionName());
		if (function == null) {
			LOGGER.error("Function not found with the name: " + primitiveTestDTO.getPrimitiveTestfunctionName());
			throw new ResourceNotFoundException(Constants.FUNCTION_NAME,
					primitiveTestDTO.getPrimitiveTestfunctionName());
		}
		try {

			PrimitiveTest primitiveTest = new PrimitiveTest();
			primitiveTest.setName(primitiveTestDTO.getPrimitiveTestname());
			primitiveTest.setModule(module);
			primitiveTest.setFunction(function);
			primitiveTestRepository.save(primitiveTest);

			// if the primitive test with this module and function contains parameters,then
			// it save the parameter value in table primitive test parameter
			// Here we will compare parameter list and matches with the parameter value DTO
			// parameters and then save the values in primitive test tables
			List<ParameterValueDTO> primitiveTestParameterList = primitiveTestDTO.getPrimitiveTestParameters();
			List<Parameter> parameter = parameterRepository.findByFunction(function);
			if (parameter != null && primitiveTestParameterList != null) {
				for (Parameter param : parameter) {
					for (ParameterValueDTO parameterValueDTO : primitiveTestParameterList) {
						if (param.getName().equals(parameterValueDTO.getParameterName())) {
							PrimitiveTestParameter primitiveTestParameter = new PrimitiveTestParameter();
							primitiveTestParameter.setPrimitiveTest(primitiveTest);

							primitiveTestParameter.setParameterName(param.getName());
							primitiveTestParameter.setParameterType(param.getParameterDataType().toString());
							primitiveTestParameter.setParameterRange(param.getRangeVal());
							primitiveTestParameter.setParameterValue(parameterValueDTO.getParameterValue());
							primitiveTestParameterRepository.save(primitiveTestParameter);
						}
					}
				}
				LOGGER.info("Primitive test parameters saved successfully");
			}
		} catch (Exception e) {
			LOGGER.error("Error in saving primitive test data: " + e.getMessage());
			return false;
		}
		LOGGER.info("Primitive test created successfully");
		return true;
	}

	/**
	 * This method is used to delete the primitive test
	 *
	 * @param id - Integer
	 */

	@Override
	public void deleteById(UUID id) {
		LOGGER.info("Deleting primitive test with id: " + id);
		PrimitiveTest primitiveTest = primitiveTestRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.PRIMITIVE_TEST_ID, id.toString()));
		try {
			primitiveTestRepository.delete(primitiveTest);
		} catch (DataIntegrityViolationException e) {
			LOGGER.error("Error in deleting primitive test data: " + e.getMessage());
			throw new DeleteFailedException();
		}
		LOGGER.info("Primitive test deleted successfully");
	}

	/**
	 * This method is used to get the primitive test details by id
	 *
	 * @param id - Integer
	 * @return PrimitiveTestDTO - primitive test details
	 */

	@Override
	public PrimitiveTestDTO getPrimitiveTestDetailsById(UUID id) {
		LOGGER.info("Finding primitive test with id: " + id);
		PrimitiveTest primitiveTest = primitiveTestRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.PRIMITIVE_TEST_ID, id.toString()));
		List<Parameter> parameters = parameterRepository.findByFunction(primitiveTest.getFunction());
		List<PrimitiveTestParameter> primitiveTestParameters = primitiveTestParameterRepository
				.findByPrimitiveTest(primitiveTest);
		List<PrimitiveTestParameterDTO> primitiveTestParameterDTOs = MapperUtils
				.convertPrimitiveTestParameterToDTO(primitiveTestParameters);

		if (primitiveTestParameters != null) {

			// Create a set of parameter names that are already in the DTO list
			Set<String> existingParameterNames = primitiveTestParameterDTOs.stream()
					.map(PrimitiveTestParameterDTO::getParameterName).collect(Collectors.toSet());

			// Iterate over all parameters and add those not present in the DTO list

			// Here we are getting complete details of a primitive test including its
			// parameters
			// if any addition parameters are added to parameter table which is not present
			// in primitive test parameter table
			// we will set its value as an empty string and return the dto
			// while updating this field will comes along with other parameters field to
			// update

			for (Parameter parameter : parameters) {
				if (!existingParameterNames.contains(parameter.getName())) {
					PrimitiveTestParameterDTO primitiveTestParameterDTO = new PrimitiveTestParameterDTO();
					primitiveTestParameterDTO.setParameterName(parameter.getName());
					primitiveTestParameterDTO.setParameterValue(""); // Set value as empty string
					primitiveTestParameterDTO.setParameterrangevalue(parameter.getRangeVal());
					primitiveTestParameterDTO.setParameterType(parameter.getParameterDataType().toString());
					primitiveTestParameterDTOs.add(primitiveTestParameterDTO);
				}
			}
		}
		PrimitiveTestDTO primitiveTestDTO = new PrimitiveTestDTO();
		primitiveTestDTO.setPrimitiveTestId(primitiveTest.getId());
		primitiveTestDTO.setPrimitiveTestName(primitiveTest.getName());
		primitiveTestDTO.setPrimitivetestModule(primitiveTest.getModule().getName());
		primitiveTestDTO.setPrimitiveTestfunction(primitiveTest.getFunction().getName());
		if (primitiveTestParameterDTOs != null) {
			primitiveTestDTO.setPrimitiveTestParameters(primitiveTestParameterDTOs);
		}
		LOGGER.info("Primitive test found  " + primitiveTestDTO.toString());

		return primitiveTestDTO;
	}

	/**
	 * This method is used to update the primitive test
	 *
	 * @param primitiveTestDTO - PrimitiveTestUpdateDTO object
	 * @return boolean - true if primitive test is updated successfully
	 * 
	 */

	@Override
	public boolean updatePrimitiveTest(PrimitiveTestUpdateDTO primitiveTestDTO) {
		LOGGER.info("Updating primitive test: " + primitiveTestDTO.toString());
		PrimitiveTest primitiveTest = primitiveTestRepository.findById(primitiveTestDTO.getPrimitiveTestId())
				.orElseThrow(() -> new ResourceNotFoundException(Constants.PRIMITIVE_TEST_ID,
						primitiveTestDTO.getPrimitiveTestId().toString()));
		try {

			List<PrimitiveTestParameter> primitiveTestParameters = primitiveTestParameterRepository
					.findByPrimitiveTest(primitiveTest);
			List<ParameterValueDTO> primitiveTestParameterList = primitiveTestDTO.getPrimitiveTestParameters();

			// Create a map of parameter names to PrimitiveTestParameters for easy lookup
			Map<String, PrimitiveTestParameter> parameterMap = primitiveTestParameters.stream()
					.collect(Collectors.toMap(p -> p.getParameterName(), p -> p));

			for (ParameterValueDTO parameterValueDTO : primitiveTestParameterList) {
				PrimitiveTestParameter primitiveTestParameter = parameterMap.get(parameterValueDTO.getParameterName());
				if (primitiveTestParameter != null) {
					// If the parameter is present, update the value
					primitiveTestParameter.setParameterValue(parameterValueDTO.getParameterValue());
					primitiveTestParameterRepository.save(primitiveTestParameter);
				} else {
					// If the parameter is not present, create a new PrimitiveTestParameter
					Parameter parameter = parameterRepository.findByName(parameterValueDTO.getParameterName());
					PrimitiveTestParameter primitiveTestParameterDetails = new PrimitiveTestParameter();
					primitiveTestParameterDetails.setPrimitiveTest(primitiveTest);
					primitiveTestParameterDetails.setParameterName(parameterValueDTO.getParameterName());
					primitiveTestParameterDetails.setParameterValue(parameterValueDTO.getParameterValue());
					primitiveTestParameterDetails.setParameterType(parameter.getParameterDataType().toString());
					primitiveTestParameterDetails.setParameterRange(parameter.getRangeVal());
					primitiveTestParameterRepository.save(primitiveTestParameterDetails);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error in updating primitive test data: " + e.getMessage());
			return false;
		}
		LOGGER.info("Primitive Test Updated Successfully");
		return true;
	}

	/**
	 * This method is used to get the primitive test details by module name
	 *
	 * @param moduleName - String
	 * @return List<PrimitiveTestNameAndIdDTO> - list of primitive test details
	 * 
	 */
	@Override
	public List<PrimitiveTestNameAndIdDTO> getPrimitiveTestDetailsByModuleName(String moduleName) {
		LOGGER.info("Finding primitive test with module name: " + moduleName);
		Module module = moduleRepository.findByName(moduleName);
		if (module == null) {
			LOGGER.error("Module not found with the name: " + moduleName);
			throw new ResourceNotFoundException(Constants.MODULE_NAME, moduleName);
		}
		List<PrimitiveTest> primitiveTests = primitiveTestRepository.findByModule(module);
		if (primitiveTests == null || primitiveTests.isEmpty()) {
			LOGGER.error("Primitive test not found with module name: " + moduleName);
			return null;
		}
		List<PrimitiveTestNameAndIdDTO> primitiveTestNameAndId = new ArrayList<>();
		for (PrimitiveTest primitiveTest : primitiveTests) {
			PrimitiveTestNameAndIdDTO primitiveTestDTO = new PrimitiveTestNameAndIdDTO();
			primitiveTestDTO.setPrimitiveTestId(primitiveTest.getId());
			primitiveTestDTO.setPrimitiveTestName(primitiveTest.getName());
			primitiveTestNameAndId.add(primitiveTestDTO);

		}
		LOGGER.info("Primitive test found  " + primitiveTestNameAndId.toString());
		return primitiveTestNameAndId;
	}

	/**
	 * This method is used to get the primitive test details by module name
	 *
	 * @param moduleName - String
	 * @return List<PrimitiveTestDTO> - list of primitive test details
	 * 
	 */

	@Override
	public List<PrimitiveTestDTO> findAllByModuleName(String moduleName) {

		LOGGER.info("Finding primitive test with module name: " + moduleName);
		Module module = moduleRepository.findByName(moduleName);
		if (module == null) {
			LOGGER.error("Module not found with the name: " + moduleName);
			throw new ResourceNotFoundException(Constants.MODULE_NAME, moduleName);
		}
		List<PrimitiveTest> primitiveTests = primitiveTestRepository.findByModule(module);
		if (primitiveTests == null || primitiveTests.isEmpty()) {
			LOGGER.error("Primitive test not found with module name: " + moduleName);
			throw new ResourceNotFoundException(Constants.PRIMITIVE_TEST_WITH_MODULE_NAME, module.getName());
		}

		List<PrimitiveTestDTO> primitiveTestDTOs = new ArrayList<>();
		for (PrimitiveTest primitiveTest : primitiveTests) {
			PrimitiveTestDTO primitiveTestDTO = new PrimitiveTestDTO();
			primitiveTestDTO.setPrimitiveTestId(primitiveTest.getId());
			primitiveTestDTO.setPrimitiveTestName(primitiveTest.getName());
			primitiveTestDTO.setPrimitivetestModule(primitiveTest.getModule().getName());
			primitiveTestDTO.setPrimitiveTestfunction(primitiveTest.getFunction().getName());
			List<PrimitiveTestParameter> primitiveTestParameters = primitiveTestParameterRepository
					.findByPrimitiveTest(primitiveTest);
			List<PrimitiveTestParameterDTO> primitiveTestParameterDTOs = MapperUtils
					.convertPrimitiveTestParameterToDTO(primitiveTestParameters);
			if (primitiveTestParameterDTOs != null) {
				primitiveTestDTO.setPrimitiveTestParameters(primitiveTestParameterDTOs);
			}

			primitiveTestDTOs.add(primitiveTestDTO);
		}
		LOGGER.info("Primitive test found  " + primitiveTestDTOs.toString());
		return primitiveTestDTOs;
	}

	/**
	 * This method is used to get the primitive test details by module name
	 *
	 * @param testName - String -primitive test name
	 * @return String - list of primitive test details
	 */
	@Override
	public JSONObject getJson(String testName) {
		LOGGER.info("Finding primitive test with module name: " + testName);
		PrimitiveTest primitiveTest = this.getPrimitiveTest(testName);

		try {
			return this.getJsonData(primitiveTest, null);
		} catch (JSONException e) {
			LOGGER.error("Error in getting primitive test data: " + e.getMessage());
		}
		return null;
	}

	/**
	 * This method is used to get the primitive test details by module name
	 *
	 * @param primitiveTestName - String
	 * @return List<PrimitiveTestDTO> - list of primitive test details
	 */
	public PrimitiveTest getPrimitiveTest(String primitiveTestName) {
		LOGGER.info("Finding primitive test with module name: " + primitiveTestName);
		PrimitiveTest primitiveTest = primitiveTestRepository.findByName(primitiveTestName);
		if (primitiveTest == null) {
			LOGGER.error("Primitive test not found with  name: " + primitiveTestName);
			throw new ResourceNotFoundException(Constants.PRIMITIVE_TEST_WITH_MODULE_NAME, primitiveTestName);
		}
		return primitiveTest;
	}

	/**
	 * This method is used to get the primitive test details as json data in the
	 * format required by python script
	 *
	 * @param primitiveTest - PrimitiveTest - primitive test object
	 * @param idValue       - String - id value
	 * @return JSONObject - list of primitive test details
	 */
	private JSONObject getJsonData(PrimitiveTest primitiveTest, String idValue) throws JSONException {

		LOGGER.info(
				"Going to get the primitive test data" + (primitiveTest != null ? primitiveTest.getName() : "null"));
		JSONObject outData = new JSONObject();

		if (primitiveTest != null && primitiveTest.getFunction() != null
				&& primitiveTest.getFunction().getName() != null) {
			outData.put("module", primitiveTest.getModule().getName().trim());
			outData.put("method", primitiveTest.getFunction().getName().trim());
			JSONObject paramsObj = new JSONObject();
			List<PrimitiveTestParameter> parameters = primitiveTest.getParameters();

			for (PrimitiveTestParameter parameter : parameters) {
				String paramName = parameter.getParameterName();
				String paramValue = parameter.getParameterValue();
				String parameterType = parameter.getParameterType();

				if ((ParameterDataType.INTEGER.toString()).equals(parameterType)) {
					if (!paramValue.isEmpty()) {
						paramsObj.put(paramName, Integer.parseInt(paramValue));
					} else {
						// Handle the case where paramValue is empty
						paramsObj.put(paramName, 0); // or any default value
					}
				} else if ((ParameterDataType.DOUBLE.toString()).equals(parameterType)) {
					if (!paramValue.isEmpty()) {
						paramsObj.put(paramName, Double.parseDouble(paramValue));
					} else {
						// Handle the case where paramValue is empty
						paramsObj.put(paramName, 0.0); // or any default value
					}
				} else if ((ParameterDataType.FLOAT.toString()).equals(parameterType)) {
					if (!paramValue.isEmpty()) {
						paramsObj.put(paramName, Float.parseFloat(paramValue));
					} else {
						// Handle the case where paramValue is empty
						paramsObj.put(paramName, 0.0f); // or any default value
					}
				} else {
					paramsObj.put(paramName, paramValue.trim());
				}

			}

			outData.put("params", paramsObj);
		}

		return outData;
	}

}
