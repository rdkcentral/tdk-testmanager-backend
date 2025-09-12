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
package com.rdkm.tdkservice.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.rdkm.tdkservice.dto.*;
import com.rdkm.tdkservice.enums.*;
import com.rdkm.tdkservice.model.*;
import com.rdkm.tdkservice.model.Module;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rdkm.tdkservice.exception.ResourceNotFoundException;

/**
 * This class is used to populate the DTO objects from the model objects.
 */
public class MapperUtils {

	private static ModelMapper modelMapper = new ModelMapper();

	private static final Logger LOGGER = LoggerFactory.getLogger(MapperUtils.class);

	/**
	 * This method is used to populate the UserDTO object from the User object.
	 * 
	 * @param user This is the User object.
	 * @return UserDTO This returns the UserDTO object populated from the User
	 *         object.
	 */
	public static UserDTO populateUserDTO(User user) {
		if (user == null) {
			return null;
		}
		UserDTO userDTO = new UserDTO();
		userDTO.setUserId(user.getId());
		userDTO.setUserName(user.getUsername());
		userDTO.setUserEmail(user.getEmail());
		userDTO.setUserDisplayName(user.getDisplayName());
		userDTO.setUserThemeName(user.getTheme() != null ? user.getTheme().name() : null);
		userDTO.setUserGroupName(user.getUserGroup() != null ? user.getUserGroup().getName() : null);
		userDTO.setUserRoleName(user.getUserRole() != null ? user.getUserRole().getName() : null);
		userDTO.setUserStatus(user.getStatus());
		userDTO.setUserCategory(user.getCategory().name());
		return userDTO;
	}

	/**
	 * This method is used to convert the deviceType object to deviceTypeDTO object.
	 * 
	 * @param deviceType This is the deviceType object.
	 * @return deviceType This returns the deviceTypeDTO object converted from the
	 *         deviceType object.
	 */
	public static DeviceTypeDTO convertToDeviceTypeDTO(DeviceType deviceType) {
		DeviceTypeDTO deviceTypeDTO = new DeviceTypeDTO();
		deviceTypeDTO.setDeviceTypeId(deviceType.getId());
		deviceTypeDTO.setDeviceTypeName(deviceType.getName());
		deviceTypeDTO.setDeviceType(deviceType.getType().name());
		deviceTypeDTO.setDeviceTypeCategory(deviceType.getCategory().name());
		LOGGER.info("Device Type DTO: {}", deviceTypeDTO);
		return deviceTypeDTO;
	}

	/**
	 * This method is used to convert the UserGroup object to UserGroupDTO object.
	 * 
	 * @param userGroup This is the UserGroup object.
	 * @return UserGroupDTO This returns the UserGroupDTO object converted from the
	 *         UserGroup object.
	 */
	public static UserGroupDTO convertToUserGroupDTO(UserGroup userGroup) {
		UserGroupDTO userGroupDTO = modelMapper.map(userGroup, UserGroupDTO.class);
		LOGGER.info("User Group DTO: {}", userGroupDTO);
		return userGroupDTO;
	}

	/**
	 * This method is used to convert the oem object to oemDTO object.
	 * 
	 * @param oem This is the oem object.
	 * @return oemDTO This returns the oemDTO object converted from the oem object.
	 */
	public static OemDTO convertToOemDTO(Oem oem) {
		OemDTO oemDTO = modelMapper.map(oem, OemDTO.class);

		LOGGER.info("oem DTO: {}", oemDTO);
		return oemDTO;
	}

	/**
	 * This method is used to convert the SocVendor object to SocVendorDTO object.
	 * 
	 * @param soc This is the SocVendor object.
	 * @return SocVendorDTO This returns the SocVendorDTO object converted from the
	 *         SocVendor object.
	 */

	public static SocDTO convertToSocDTO(Soc soc) {
		SocDTO socDTO = modelMapper.map(soc, SocDTO.class);
		LOGGER.info("Soc DTO: {}", socDTO);
		return socDTO;
	}

	/**
	 * This method is used to convert the UserRole object to UserRoleDTO object.
	 * 
	 * @param userRole This is the UserRole object.
	 * @return UserRoleDTO This returns the UserRoleDTO object converted from the
	 *         UserRole object.
	 */
	public static UserRoleDTO convertToUserRoleDTO(UserRole userRole) {
		UserRoleDTO userRoleDTO = modelMapper.map(userRole, UserRoleDTO.class);
		LOGGER.info("User Role DTO: {}", userRoleDTO);
		return userRoleDTO;
	}

	/**
	 * This method is used to convert the Device object to DeviceDTO object.
	 *
	 * @param deviceCreateDTO This is the Device object.
	 * @return DeviceDTO This returns the DeviceDTO object converted from the Device
	 *         object.
	 */

	public static Device populateDeviceDTO(DeviceCreateDTO deviceCreateDTO) {

		Device device = new Device();
		device.setIp(deviceCreateDTO.getDeviceIp());
		device.setName(deviceCreateDTO.getDeviceName());
		if (null != deviceCreateDTO.getDevicePort() && !deviceCreateDTO.getDevicePort().isEmpty()) {
			Utils.validateInteger(deviceCreateDTO.getDevicePort(), "Device port");
			device.setPort(deviceCreateDTO.getDevicePort());
		}

		if (null != deviceCreateDTO.getStatusPort() && !deviceCreateDTO.getStatusPort().isEmpty()) {
			Utils.validateInteger(deviceCreateDTO.getStatusPort(), "Status port");
			device.setStatusPort(deviceCreateDTO.getStatusPort());
		}
		if (null != deviceCreateDTO.getAgentMonitorPort() && !deviceCreateDTO.getAgentMonitorPort().isEmpty()) {
			Utils.validateInteger(deviceCreateDTO.getAgentMonitorPort(), "Agent port");
			device.setAgentMonitorPort(deviceCreateDTO.getAgentMonitorPort());
		}
		if (null != deviceCreateDTO.getLogTransferPort() && !deviceCreateDTO.getLogTransferPort().isEmpty()) {
			Utils.validateInteger(deviceCreateDTO.getLogTransferPort(), "Log transfer port");
			device.setLogTransferPort(deviceCreateDTO.getLogTransferPort());
		}

		device.setMacId(deviceCreateDTO.getMacId());
		// device.setDeviceStatus(deviceCreateDTO.getDevicestatus());
		device.setThunderPort(deviceCreateDTO.getThunderPort());
		device.setThunderEnabled(deviceCreateDTO.isThunderEnabled());
		// isDevicePortsConfigured
		device.setDevicePortsConfigured(deviceCreateDTO.isDevicePortsConfigured());

		return device;
	}

	/**
	 * This method is used to convert the Device object to UpdateDeviceDTO object.
	 *
	 * @param device This is the Device object.
	 * @return DeviceDTO This returns the DeviceDTO object converted from the Device
	 *         object.
	 */
	public static void updateDeviceProperties(Device device, DeviceUpdateDTO deviceUpdateDTO) {
		if (!Utils.isEmpty(deviceUpdateDTO.getDevicePort()))
			device.setPort(deviceUpdateDTO.getDevicePort());
		if (!Utils.isEmpty(deviceUpdateDTO.getStatusPort()))
			device.setStatusPort(deviceUpdateDTO.getStatusPort());
		if (!Utils.isEmpty(deviceUpdateDTO.getAgentMonitorPort()))
			device.setAgentMonitorPort(deviceUpdateDTO.getAgentMonitorPort());
		if (!Utils.isEmpty(deviceUpdateDTO.getLogTransferPort()))
			device.setLogTransferPort(deviceUpdateDTO.getLogTransferPort());
		if (!Utils.isEmpty(deviceUpdateDTO.getThunderPort()))
			device.setThunderPort(deviceUpdateDTO.getThunderPort());
		device.setThunderEnabled(deviceUpdateDTO.isThunderEnabled());
		// isDevicePortsConfigured
		device.setDevicePortsConfigured(deviceUpdateDTO.isDevicePortsConfigured());

		if (!Utils.isEmpty(deviceUpdateDTO.getCategory())) {
			Category category = Category.valueOf(deviceUpdateDTO.getCategory().toUpperCase());
			if (category != null) {
				device.setCategory(category);
			} else {
				throw new ResourceNotFoundException("Category not found", deviceUpdateDTO.getCategory());
			}
		}
		// set device type for update case

	}

	/**
	 * This method is used to convert the deviceType object to deviceTypeUpdateDTO
	 * object.
	 * 
	 * @param deviceType This is the deviceType object.
	 * @return DeviceTypeUpdateDTO This returns the DeviceTypeUpdateDTO object
	 *         converted from the deviceType object.
	 */
	public static DeviceTypeDTO convertToDeviceTypeUpdateDTO(DeviceType deviceType) {
		DeviceTypeDTO deviceTypeUpdateDTO = new DeviceTypeDTO();
		deviceTypeUpdateDTO.setDeviceTypeName(deviceType.getName());
		deviceTypeUpdateDTO.setDeviceType(deviceType.getType().getName());
		deviceTypeUpdateDTO.setDeviceTypeCategory(deviceType.getCategory().name());
		LOGGER.info("device type Update DTO: {}", deviceTypeUpdateDTO);
		return deviceTypeUpdateDTO;
	}

	/**
	 * This method is used to convert the oem object to OemUpdateDTO object.
	 * 
	 * @param oem This is the oem object.
	 * @return OemUpdateDTO This returns the OemDTO object converted from the oem
	 *         object.
	 */
	public static OemDTO convertToOemUpdateDTO(Oem oem) {
		OemDTO oemUpdateDTO = new OemDTO();
		oemUpdateDTO.setOemName(oem.getName());
		oemUpdateDTO.setOemCategory(oem.getCategory().name());
		LOGGER.info("oem Update DTO: {}", oem);
		return oemUpdateDTO;
	}

	/**
	 * This method is used to convert the Soc object to socUpdateDTO object.
	 * 
	 * @param soc This is the SocVendor object.
	 * @return soc This returns the soc object converted from the soc object.
	 */
	public static SocDTO convertToSocUpdateDTO(Soc soc) {
		SocDTO socUpdateDTO = new SocDTO();
		socUpdateDTO.setSocName(soc.getName());
		socUpdateDTO.setSocCategory(soc.getCategory().name());
		LOGGER.info("Soc  Update DTO: {}", soc);
		return socUpdateDTO;

	}

	/**
	 * This method is used to convert the Device object to DeviceDTO object.
	 *
	 * @param device This is the Device object.
	 * @return DeviceDTO This returns the DeviceDTO object converted from the Device
	 *         object.
	 */

	public static DeviceResponseDTO convertToDeviceDTO(Device device) {
		modelMapper.typeMap(Device.class, DeviceResponseDTO.class).addMappings(mapper -> {
			mapper.map(src -> src.getName(), DeviceResponseDTO::setDeviceName);
		});
		return modelMapper.map(device, DeviceResponseDTO.class);
	}

	/**
	 * Converts a ModuleCreateDTO object to a Module entity.
	 *
	 * @param dto       the data transfer object containing the module details
	 * @param userGroup the user group associated with the module
	 * @return the Module entity populated with the details from the DTO
	 */
	public static Module toModuleEntity(ModuleCreateDTO dto) {
		Module module = new Module();
		module.setName(dto.getModuleName());
		TestGroup testGroup = TestGroup.valueOf(dto.getTestGroup());
		module.setTestGroup(testGroup);
		module.setExecutionTime(dto.getExecutionTime());
		module.setLogFileNames(dto.getModuleLogFileNames());
		module.setCrashLogFiles(dto.getModuleCrashLogFiles());
		return module;
	}

	/**
	 * This method is used to convert the Module object to ModuleDTO object.
	 *
	 * @param module This is the Module object.
	 * @return ModuleDTO This returns the ModuleDTO object converted from the Module
	 *         object.
	 */
	public static void updateModuleProperties(Module module, ModuleDTO moduleDTO) {
		if (moduleDTO.getExecutionTime() != null)
			module.setExecutionTime(moduleDTO.getExecutionTime());
		if (moduleDTO.getModuleLogFileNames() != null)
			module.setLogFileNames(moduleDTO.getModuleLogFileNames());
		if (moduleDTO.getModuleCrashLogFiles() != null)
			module.setCrashLogFiles(moduleDTO.getModuleCrashLogFiles());
		if (moduleDTO.getTestGroup() != null && !moduleDTO.getTestGroup().isEmpty()) {
			TestGroup testGroup = TestGroup.valueOf(moduleDTO.getTestGroup());
			module.setTestGroup(testGroup);
		}
		if (!moduleDTO.isModuleThunderEnabled()) {
			Category category = Category.getCategory(moduleDTO.getModuleCategory());
			if (null == category) {
				throw new ResourceNotFoundException(Constants.CATEGORY, moduleDTO.getModuleCategory());
			} else {
				module.setCategory(category);
			}
		}
	}

	/**
	 * Converts a Module entity to a ModuleDTO object.
	 *
	 * @param module the Module entity to be converted
	 * @return the ModuleDTO object populated with the details from the Module
	 *         entity
	 */
	public static ModuleDTO convertToModuleDTO(Module module) {

		ModuleDTO moduleDTO = new ModuleDTO();
		moduleDTO.setId(module.getId());
		moduleDTO.setModuleName(module.getName());
		moduleDTO.setTestGroup(module.getTestGroup() != null ? module.getTestGroup().name() : null);
		moduleDTO.setExecutionTime(module.getExecutionTime());
		moduleDTO.setModuleLogFileNames(module.getLogFileNames());
		moduleDTO.setModuleCrashLogFiles(module.getCrashLogFiles());
		moduleDTO.setModuleCategory(module.getCategory().name());
		if (module.getCategory() == Category.RDKV_RDKSERVICE) {
			moduleDTO.setModuleThunderEnabled(true);
		}
		return moduleDTO;
	}

	/**
	 * Converts a Function entity to a FunctionDTO object.
	 *
	 * @param function the Function entity to be converted
	 * @return the FunctionDTO object populated with the details from the Function
	 *         entity
	 */
	public static FunctionDTO convertToFunctionDTO(Function function) {
		FunctionDTO functionDTO = new FunctionDTO();
		functionDTO.setId(function.getId());
		functionDTO.setFunctionName(function.getName());
		functionDTO.setModuleName(function.getModule() != null ? function.getModule().getName() : null);
		functionDTO.setFunctionCategory(function.getCategory().name());
		return functionDTO;
	}

	/**
	 * Converts a ParameterType entity to a ParameterTypeDTO object.
	 *
	 * @param parameter the ParameterType entity to be converted
	 * @return the ParameterTypeDTO object populated with the details from the
	 *         ParameterType entity
	 */
	public static ParameterDTO convertToParameterTypeDTO(Parameter parameter) {
		ParameterDTO parameterDTO = new ParameterDTO();
		parameterDTO.setId(parameter.getId());
		parameterDTO.setParameterName(parameter.getName());
		parameterDTO.setParameterDataType(parameter.getParameterDataType());
		parameterDTO.setParameterRangeVal(parameter.getRangeVal());
		parameterDTO.setFunction(parameter.getFunction() != null ? parameter.getFunction().getName() : null);
		return parameterDTO;
	}

	/**
	 * Maps the data from FunctionCreateDTO to Function entity.
	 *
	 * @param function          the function entity to be updated
	 * @param functionCreateDTO the data transfer object containing the function
	 *                          details
	 * @param module            the module entity associated with the function
	 */
	public static void mapCreateDTOToEntity(Function function, FunctionCreateDTO functionCreateDTO, Module module) {
		function.setName(functionCreateDTO.getFunctionName());
		function.setModule(module);
		function.setCategory(Category.valueOf(functionCreateDTO.getFunctionCategory()));
	}

	/**
	 * Maps the data from ParameterTypeCreateDTO to ParameterType entity.
	 *
	 * @param parameter          the parameter type entity to be updated
	 * @param parameterCreateDTO the data transfer object containing the parameter
	 *                           type details
	 * @param function           the function entity associated with the parameter
	 *                           type
	 */
	public static void mapDTOCreateParameterTypeToEntity(Parameter parameter, ParameterCreateDTO parameterCreateDTO,
			Function function) {
		parameter.setName(parameterCreateDTO.getParameterName());
		parameter.setParameterDataType(parameterCreateDTO.getParameterDataType());
		parameter.setRangeVal(parameterCreateDTO.getParameterRangeVal());
		parameter.setFunction(function);
	}

	/**
	 * Maps the data from ParameterTypeDTO to ParameterType entity.
	 *
	 * @param parameter    the parameter type entity to be updated
	 * @param parameterDTO the data transfer object containing the parameter type
	 *                     details
	 */
	public static void mapDTOToEntity(Parameter parameter, ParameterDTO parameterDTO) {
		parameter.setName(parameterDTO.getParameterName());
		parameter.setParameterDataType(parameterDTO.getParameterDataType());
		parameter.setRangeVal(parameterDTO.getParameterRangeVal());
	}

	/**
	 * This method is used to convert the Function object to FunctionDTO object.
	 *
	 * @param primitiveTestParameters This is the Function object.
	 * @return FunctionDTO This returns the FunctionDTO object converted from the
	 *         Function object.
	 */

	public static List<PrimitiveTestParameterDTO> convertPrimitiveTestParameterToDTO(
			List<PrimitiveTestParameter> primitiveTestParameters) {
		LOGGER.info("Converting primitive test parameters to DTO");
		List<PrimitiveTestParameterDTO> primitiveTestParameterDTOs = new ArrayList<>();
		for (PrimitiveTestParameter primitiveTestParameter : primitiveTestParameters) {
			PrimitiveTestParameterDTO primitiveTestParameterDTO = new PrimitiveTestParameterDTO();
			primitiveTestParameterDTO.setParameterName(primitiveTestParameter.getParameterName());
			primitiveTestParameterDTO.setParameterValue(primitiveTestParameter.getParameterValue());
			primitiveTestParameterDTO.setParameterrangevalue(primitiveTestParameter.getParameterRange());
			primitiveTestParameterDTO.setParameterType(primitiveTestParameter.getParameterType().toString());
			primitiveTestParameterDTOs.add(primitiveTestParameterDTO);
		}
		LOGGER.info("Primitive test parameters converted to DTO:" + primitiveTestParameterDTOs.toString());
		return primitiveTestParameterDTOs;
	}

	/**
	 * This method is used to convert the ScriptCreateDTO to Script entity
	 * 
	 * @param scriptCreateDTO ScriptCreateDTO
	 * @return script Script
	 */
	public static Script convertToScriptEntity(ScriptCreateDTO scriptCreateDTO) {
		Script script = new Script();
		LOGGER.info("Converting ScriptDTO to ScriptEntity");
		script.setName(scriptCreateDTO.getName());
		script.setSynopsis(scriptCreateDTO.getSynopsis());
		script.setExecutionTimeOut(scriptCreateDTO.getExecutionTimeOut());
		script.setLongDuration(scriptCreateDTO.isLongDuration());
		script.setSkipExecution(scriptCreateDTO.isSkipExecution());
		if (scriptCreateDTO.isSkipExecution()) {
			script.setSkipRemarks(scriptCreateDTO.getSkipRemarks());
		}

		script.setTestId(scriptCreateDTO.getTestId());
		script.setObjective(scriptCreateDTO.getObjective());
		List<PreCondition> preConditionList = new ArrayList<>();
		for (String preCondition : scriptCreateDTO.getPreConditions()) {
			PreCondition preConditionObj = new PreCondition();
			preConditionObj.setPreConditionDescription(preCondition);
			preConditionObj.setScript(script);
			preConditionList.add(preConditionObj);

		}
		script.setPreConditions(preConditionList);
		List<TestStep> testSteps = new ArrayList<>();

		for (TestStepCreateDTO testStep : scriptCreateDTO.getTestSteps()) {
			TestStep testStepObj = new TestStep();
			testStepObj.setStepName(testStep.getStepName());
			testStepObj.setStepDescription(testStep.getStepDescription());
			testStepObj.setExpectedResult(testStep.getExpectedResult());
			testStepObj.setScript(script);
			testSteps.add(testStepObj);
		}
		script.setTestSteps(testSteps);

		script.setPriority(scriptCreateDTO.getPriority());
		script.setReleaseVersion(scriptCreateDTO.getReleaseVersion());

		return script;
	}

	/*
	 * Support method to script update operation
	 * 
	 * @param scriptUpdateDTO ScriptDTO
	 * 
	 * @param script Script
	 * 
	 * @return script Script
	 */
	public static Script updateScript(Script script, ScriptDTO scriptUpdateDTO) {
		LOGGER.info("Updating the script entity with the properties available in the script update DTO");

		if (!Utils.isEmpty(scriptUpdateDTO.getSynopsis())) {
			script.setSynopsis(scriptUpdateDTO.getSynopsis());
		}
		script.setExecutionTimeOut(scriptUpdateDTO.getExecutionTimeOut());
		script.setLongDuration(scriptUpdateDTO.isLongDuration());
		script.setSkipExecution(scriptUpdateDTO.isSkipExecution());
		if (scriptUpdateDTO.isSkipExecution()) {
			if (!Utils.isEmpty(scriptUpdateDTO.getSkipRemarks())) {
				script.setSkipRemarks(scriptUpdateDTO.getSkipRemarks());
			}
		}

		// Update the test case documentation details
		if (!Utils.isEmpty(scriptUpdateDTO.getTestId())) {
			script.setTestId(scriptUpdateDTO.getTestId());
		}
		if (!Utils.isEmpty(scriptUpdateDTO.getObjective())) {
			script.setObjective(scriptUpdateDTO.getObjective());
		}

		if (!Utils.isEmpty(scriptUpdateDTO.getPriority())) {
			script.setPriority(scriptUpdateDTO.getPriority());
		}

		if (!Utils.isEmpty(scriptUpdateDTO.getReleaseVersion())) {
			script.setReleaseVersion(scriptUpdateDTO.getReleaseVersion());
		}

		LOGGER.info("Updated the script entity with the properties available in the script update DTO");
		return script;

	}

	/**
	 * This method is used to convert the Script entity to ScriptListDTO
	 * 
	 * @param script Script entity
	 * @return scriptListDTO ScriptListDTO
	 */
	public static ScriptListDTO convertToScriptListDTO(Script script) {
		LOGGER.debug("Converting the script entity to script list DTO");
		ScriptListDTO scriptListDTO = new ScriptListDTO();
		scriptListDTO.setId(script.getId());
		scriptListDTO.setName(script.getName());
		LOGGER.debug("Converted the script entity to script list DTO:" + scriptListDTO.toString());
		return scriptListDTO;
	}

	/**
	 * Converts a ScriptCreateDTO and Script entity to a ScriptDTO for XML update.
	 * 
	 * @param createDTO the ScriptCreateDTO
	 * @param script    the Script entity
	 * @return the ScriptDTO representation of the script entity
	 */
	public static ScriptDTO convertToScriptDTOForXMLUpdate(ScriptCreateDTO createDTO, Script script) {
		ScriptDTO dto = new ScriptDTO();

		dto.setName(createDTO.getName());
		dto.setSynopsis(createDTO.getSynopsis());
		dto.setExecutionTimeOut(createDTO.getExecutionTimeOut());
		dto.setLongDuration(createDTO.isLongDuration());
		dto.setPrimitiveTestName(createDTO.getPrimitiveTestName());
		dto.setDeviceTypes(createDTO.getDeviceTypes());
		dto.setSkipExecution(createDTO.isSkipExecution());
		dto.setSkipRemarks(createDTO.getSkipRemarks());
		dto.setTestId(createDTO.getTestId());
		dto.setObjective(createDTO.getObjective());
		dto.setPriority(createDTO.getPriority());
		dto.setReleaseVersion(createDTO.getReleaseVersion());

		// Convert preConditions (List<String> to List<PreConditionDTO>)
		if (createDTO.getPreConditions() != null) {
			List<PreConditionDTO> preConditionDTOs = createDTO.getPreConditions().stream()
					.map(detail -> {
						PreConditionDTO preConditionDTO = new PreConditionDTO();
						preConditionDTO.setPreConditionDetails(detail);
						return preConditionDTO;
					})
					.collect(Collectors.toList());
			dto.setPreConditions(preConditionDTOs);
		}

		// Convert testSteps (List<TestStepCreateDTO> to List<TestStepDTO>)
		if (createDTO.getTestSteps() != null) {
			List<TestStepDTO> testStepDTOs = createDTO.getTestSteps().stream()
					.map(ts -> {
						TestStepDTO testStepDTO = new TestStepDTO();
						testStepDTO.setStepDescription(ts.getStepDescription());
						testStepDTO.setStepName(ts.getStepName());
						testStepDTO.setExpectedResult(ts.getExpectedResult());
						// Add other fields if needed
						return testStepDTO;
					})
					.collect(Collectors.toList());
			dto.setTestSteps(testStepDTOs);
		}

		return dto;
	}

	/**
	 * This method is used to convert the Script entity to ScriptDTO
	 * 
	 * @param script Script entity
	 * @return scriptDTO ScriptDTO
	 */
	public static ScriptDTO convertToScriptDTO(Script script) {
		LOGGER.info("Converting the script entity to script DTO");
		ScriptDTO scriptDTO = new ScriptDTO();
		scriptDTO.setId(script.getId());

		if (script.getPrimitiveTest() != null) {
			scriptDTO.setPrimitiveTestName(script.getPrimitiveTest().getName());
		}

		scriptDTO.setName(script.getName());
		scriptDTO.setSynopsis(script.getSynopsis());
		scriptDTO.setModuleName(script.getModule().getName());
		scriptDTO.setExecutionTimeOut(script.getExecutionTimeOut());
		scriptDTO.setLongDuration(script.isLongDuration());
		scriptDTO.setSkipExecution(script.isSkipExecution());
		scriptDTO.setSkipRemarks(script.getSkipRemarks());
		scriptDTO.setTestId(script.getTestId());
		scriptDTO.setObjective(script.getObjective());

		List<PreConditionDTO> preConditionList = new ArrayList<>();
		for (PreCondition preCondition : script.getPreConditions()) {
			PreConditionDTO preConditionObj = new PreConditionDTO();
			preConditionObj.setPreConditionId(preCondition.getId());
			preConditionObj.setPreConditionDetails(preCondition.getPreConditionDescription());
			preConditionList.add(preConditionObj);
		}
		scriptDTO.setPreConditions(preConditionList);
		List<TestStepDTO> testSteps = new ArrayList<>();
		for (TestStep testStep : script.getTestSteps()) {
			TestStepDTO testStepObj = new TestStepDTO();
			testStepObj.setTestStepId(testStep.getId());
			testStepObj.setStepName(testStep.getStepName());
			testStepObj.setStepDescription(testStep.getStepDescription());
			testStepObj.setExpectedResult(testStep.getExpectedResult());
			testSteps.add(testStepObj);
		}
		scriptDTO.setTestSteps(testSteps);
		scriptDTO.setPriority(script.getPriority());

		scriptDTO.setReleaseVersion(script.getReleaseVersion());

		LOGGER.info("Converted the script entity to script DTO:" + scriptDTO.toString());
		return scriptDTO;

	}

	/**
	 * This method is used to convert the ScriptTestSuite map list to ScriptListDTO
	 * list
	 * 
	 * @param testSuiteCreateDTO
	 * @return scriptListDTOList ScriptListDTO list
	 */
	public static List<ScriptListDTO> getScriptList(List<ScriptTestSuite> scriptTestSuiteList) {
		List<ScriptListDTO> scriptList = new ArrayList<>();
		for (ScriptTestSuite scriptTestSuite : scriptTestSuiteList) {
			ScriptListDTO scriptListDTO = new ScriptListDTO();
			scriptListDTO.setId(scriptTestSuite.getScript().getId());
			scriptListDTO.setName(scriptTestSuite.getScript().getName());
			scriptList.add(scriptListDTO);
		}
		return scriptList;
	}

	/**
	 * This method is used to convert the TestSuite entity to TestSuiteDTO
	 * 
	 * @param testSuite TestSuite entity
	 * @return testSuite TestSuiteDTO
	 */
	public static TestSuiteDTO convertToTestSuiteDTO(TestSuite testSuite) {
		TestSuiteDTO testSuiteDTO = new TestSuiteDTO();
		testSuiteDTO.setId(testSuite.getId());
		testSuiteDTO.setName(testSuite.getName());
		testSuiteDTO.setDescription(testSuite.getDescription());
		testSuiteDTO.setCategory(testSuite.getCategory().toString());
		return testSuiteDTO;
	}

	/**
	 * This method is used to convert the Script entity to ScriptListDTO
	 * 
	 * @param script Script entity
	 * @return scriptListDTO ScriptListDTO
	 */
	public static List<ScriptListDTO> getScriptListDTOFromScriptList(List<Script> script) {
		LOGGER.info("Converting the script entity to script list DTO");
		List<ScriptListDTO> scriptListDTOList = new ArrayList<>();
		for (Script scriptObj : script) {
			ScriptListDTO scriptListDTO = new ScriptListDTO();
			scriptListDTO.setId(scriptObj.getId());
			scriptListDTO.setName(scriptObj.getName());
			scriptListDTOList.add(scriptListDTO);
		}
		return scriptListDTOList;

	}

	/**
	 * This method is used to convert the ExecutionScheduleDTO to ExecutionSchedule
	 * 
	 * @param executionScheduleDTO ExecutionScheduleDTO
	 * @return ExecutionSchedule ExecutionSchedule
	 */
	public static ExecutionSchedule convertToExecutionSchedule(ExecutionScheduleDTO executionScheduleDTO,
			String cronExpression) {
		ExecutionSchedule executionSchedule = new ExecutionSchedule();
		ExecutionTriggerDTO executionTriggerDTO = executionScheduleDTO.getExecutionTriggerDTO();
		List<String> deviceList = executionTriggerDTO.getDeviceList();
		List<String> scriptList = executionTriggerDTO.getScriptList();
		List<String> testSuiteList = executionTriggerDTO.getTestSuite();

		// Setting up execution related data
		if (null != deviceList && !deviceList.isEmpty()) {
			executionSchedule.setDevice(deviceList.get(0));
		}

		if ((null != scriptList) && !scriptList.isEmpty()) {
			String scriptListString = String.join(",", scriptList);
			executionSchedule.setScriptList(scriptListString);
		}

		if ((null != testSuiteList) && !testSuiteList.isEmpty()) {
			String testSuiteString = String.join(",", testSuiteList);
			executionSchedule.setTestSuite(testSuiteString);
		}

		executionSchedule.setTestType(executionTriggerDTO.getTestType());
		executionSchedule.setUser(executionTriggerDTO.getUser());
		if (Utils.isEmpty(executionTriggerDTO.getExecutionName())) {
			executionSchedule.setExecutionName(Constants.JOB + Utils.getTimeStampInUTCForExecutionName());
		} else {
			executionSchedule.setExecutionName(Constants.JOB + executionTriggerDTO.getExecutionName());

		}
		executionSchedule.setRepeatCount(executionTriggerDTO.getRepeatCount());
		executionSchedule.setRerunOnFailure(executionTriggerDTO.isRerunOnFailure());
		executionSchedule.setDeviceLogsNeeded(executionTriggerDTO.isDeviceLogsNeeded());
		executionSchedule.setPerformanceLogsNeeded(executionTriggerDTO.isPerformanceLogsNeeded());
		executionSchedule.setDiagnosticLogsNeeded(executionTriggerDTO.isDiagnosticLogsNeeded());
		String categoryValue = executionScheduleDTO.getExecutionTriggerDTO().getCategory();
		Category category = Category.valueOf(categoryValue.toUpperCase());
		executionSchedule.setCategory(category);
		executionSchedule.setCiCallBackUrl(executionTriggerDTO.getCiCallBackUrl());
		executionSchedule.setCiImageVersion(executionTriggerDTO.getCiImageVersion());
		executionSchedule.setIndividualRepeatExecution(executionTriggerDTO.isIndividualRepeatExecution());

		// Execution schedule related data
		executionSchedule.setScheduleType(executionScheduleDTO.getScheduleType());
		executionSchedule.setScheduleStatus(ScheduleStatus.SCHEDULED);
		executionSchedule.setExecutionTime(executionScheduleDTO.getExecutionTime());
		executionSchedule.setCronExpression(cronExpression);
		executionSchedule.setCronEndTime(executionScheduleDTO.getCronEndTime());
		executionSchedule.setCronStartTime(executionScheduleDTO.getCronStartTime());
		executionSchedule.setCronQuery(executionScheduleDTO.getCronQuery());

		return executionSchedule;
	}

	/**
	 * This method is used to convert script to ScriptDetailsResponse
	 * 
	 * @param script
	 * @return
	 */
	public static ScriptDetailsResponse convertToScriptDetailsResponse(Script script) {
		ScriptDetailsResponse response = new ScriptDetailsResponse();
		response.setId(script.getId());
		response.setScriptName(script.getName());

		return response;
	}

	/**
	 * This method is used to convert the TestSuite entity to
	 * TestSuiteDetailsResponse
	 *
	 * @param testSuite TestSuite entity
	 * @return testSuite TestSuiteDetailsResponse
	 */
	public static TestSuiteDetailsResponse convertToTestSuiteDetailsResponse(TestSuite testSuite) {
		TestSuiteDetailsResponse response = new TestSuiteDetailsResponse();
		response.setId(testSuite.getId());
		response.setTestSuiteName(testSuite.getName());
		response.setScriptCount(testSuite.getScriptTestSuite().size());
		return response;
	}

	/**
	 * This method is used to convert the ExecutionResultDTO to ExecutionResult
	 *
	 * @param executionResultDTO ExecutionResult
	 **/
	public static ExecutionResult convertToExecutionResult(ExecutionResultDTO executionResultDTO) {
		ExecutionResult executionResult = new ExecutionResult();
		executionResult.setId(executionResultDTO.getExecutionResultID());
		executionResult.setScript(executionResultDTO.getName());
		executionResult.setResult(ExecutionResultStatus.valueOf(executionResultDTO.getStatus()));
		return executionResult;
	}
}
