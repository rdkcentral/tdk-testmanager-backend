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

import com.rdkm.tdkservice.dto.DeviceTypeCreateDTO;
import com.rdkm.tdkservice.dto.DeviceTypeDTO;
import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.enums.DeviceTypeCategory;
import com.rdkm.tdkservice.exception.DeleteFailedException;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.model.DeviceType;
import com.rdkm.tdkservice.model.UserGroup;
import com.rdkm.tdkservice.repository.DeviceRepositroy;
import com.rdkm.tdkservice.repository.DeviceTypeRepository;
import com.rdkm.tdkservice.repository.UserGroupRepository;
import com.rdkm.tdkservice.service.IDeviceTypeService;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.MapperUtils;
import com.rdkm.tdkservice.util.Utils;

/**
 * This class provides the implementation for the oemService interface. It
 * provides methods to perform CRUD operations on deviceType entities.
 */
@Service
public class DeviceTypeService implements IDeviceTypeService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeviceTypeService.class);

	@Autowired
	DeviceTypeRepository deviceTypeRepository;

	@Autowired
	DeviceRepositroy deviceRepository;

	@Autowired
	UserGroupRepository userGroupRepository;

	@Autowired
	CommonService commonService;

	/**
	 * This method is used to create a new deviceType.
	 * 
	 * @param deviceTypeDTO This is the request object containing the details of the
	 *                      deviceType to be created.
	 * @return boolean This returns true if the deviceType was created successfully,
	 *         false otherwise.
	 */
	@Override
	public boolean createDeviceType(DeviceTypeCreateDTO deviceTypeDTO) {
		LOGGER.info("Going to create DeviceType");
		Category category = Category.getCategory(deviceTypeDTO.getDeviceTypeCategory());
		if (deviceTypeRepository.existsByNameAndCategory(deviceTypeDTO.getDeviceTypeName(), category)) {
			LOGGER.error("Device type already exists with the same name: " + deviceTypeDTO.getDeviceTypeName());
			throw new ResourceAlreadyExistsException(Constants.DEVICE_TYPE, deviceTypeDTO.getDeviceTypeName());
		}

		DeviceType deviceType = new DeviceType();
		deviceType.setName(deviceTypeDTO.getDeviceTypeName());

		DeviceTypeCategory deviceTypeCategory = DeviceTypeCategory.getDeviceTypeCategory(deviceTypeDTO.getDeviceType());
		if (null == deviceTypeCategory) {
			throw new ResourceNotFoundException(Constants.DEVICE_TYPE_TYPE, deviceTypeDTO.getDeviceType());
		} else {
			deviceType.setType(deviceTypeCategory);
		}

		if (deviceTypeDTO.getDeviceTypeCategory() != null) {
			deviceType.setCategory(category);
		}

		UserGroup userGroup = userGroupRepository.findByName(deviceTypeDTO.getDeviceTypeUserGroup());
		deviceType.setUserGroup(userGroup);

		try {
			deviceType = deviceTypeRepository.save(deviceType);

		} catch (Exception e) {
			LOGGER.error("Error occurred while creating Device Type", e);
			return false;
		}
		LOGGER.info("DeviceType creation completed");
		return deviceType != null && deviceType.getId() != null;
	}

	/**
	 * This method is used to retrieve all DeviceTypes.
	 * 
	 * @return List<DeviceTypeDTO> This returns a list of all DeviceTypes.
	 */
	@Override
	public List<DeviceTypeDTO> getAllDeviceTypes() {
		LOGGER.info("Going to fetch all Device types");
		List<DeviceType> deviceTypes = deviceTypeRepository.findAll();
		if (deviceTypes.isEmpty()) {
			return null;
		}
		return deviceTypes.stream().map(this::convertToDeviceTypeDTO).collect(Collectors.toList());
	}

	/**
	 * This method is used to delete a DeviceType by its id.
	 * 
	 * @param id This is the id of the DeviceType to be deleted.
	 */
	@Override
	public void deleteById(UUID id) {
		DeviceType deviceType = deviceTypeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.DEVICE_TYPE_ID, id.toString()));
		try {
			deviceTypeRepository.deleteById(deviceType.getId());
		} catch (DataIntegrityViolationException e) {
			LOGGER.error("Error occurred while deleting deviceType with id: " + id, e);
			throw new DeleteFailedException();
		}

	}

	/**
	 * This method is used to find a DeviceType by its id.
	 * 
	 * @param id This is the id of the DeviceType to be found.
	 * @return DeviceTypeRequest This returns the DeviceType.
	 */
	@Override
	public DeviceTypeDTO findById(UUID id) {
		LOGGER.info("Executing find DeviceType by id method with id: " + id);
		DeviceType deviceType = deviceTypeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.DEVICE_TYPE_ID, id.toString()));
		DeviceTypeDTO userDTO = null;
		try {
			userDTO = this.convertToDeviceTypeDTO(deviceType);
		} catch (Exception e) {
			LOGGER.error("Error occurred while fetching DeviceType with id: " + id, e);
		}
		return userDTO;

	}

	/**
	 * This method is used to update a DeviceType.
	 * 
	 * @param deviceTypeUpdateDTO This is the request object containing the updated
	 *                            details of the DeviceType.
	 * @param id                  This is the id of the DeviceType to be updated.
	 * @return deviceTypeUpdateDTO This returns the updated DeviceType.
	 */
	@Override
	public DeviceTypeDTO updateDeviceType(DeviceTypeDTO deviceTypeUpdateDTO) {
		Category category = Category.getCategory(deviceTypeUpdateDTO.getDeviceTypeCategory());
		DeviceType deviceType = deviceTypeRepository.findById(deviceTypeUpdateDTO.getDeviceTypeId())
				.orElseThrow(() -> new ResourceNotFoundException(Constants.DEVICE_TYPE_ID,
						deviceTypeUpdateDTO.getDeviceTypeId().toString()));
		if (!Utils.isEmpty(deviceTypeUpdateDTO.getDeviceTypeName())) {
			DeviceType newDeviceType = deviceTypeRepository
					.findByNameAndCategory(deviceTypeUpdateDTO.getDeviceTypeName(), category);
			if (newDeviceType != null
					&& deviceTypeUpdateDTO.getDeviceTypeName().equalsIgnoreCase(deviceType.getName())) {
				deviceType.setName(deviceTypeUpdateDTO.getDeviceTypeName());
			} else {
				if (deviceTypeRepository.existsByNameAndCategory(deviceTypeUpdateDTO.getDeviceTypeName(), category)) {
					LOGGER.info("Device Type already exists with the same name: "
							+ deviceTypeUpdateDTO.getDeviceTypeName());
					throw new ResourceAlreadyExistsException(Constants.DEVICE_TYPE,
							deviceTypeUpdateDTO.getDeviceTypeName());
				} else {
					deviceType.setName(deviceTypeUpdateDTO.getDeviceTypeName());
				}
			}
		}

		if (deviceTypeUpdateDTO.getDeviceType() != null) {
			deviceType.setType(DeviceTypeCategory.getDeviceTypeCategory(deviceTypeUpdateDTO.getDeviceType()));
		}
		if (deviceTypeUpdateDTO.getDeviceTypeCategory() != null) {
			deviceType.setCategory(category);
		}
		try {
			deviceType = deviceTypeRepository.save(deviceType);

		} catch (Exception e) {
			LOGGER.error("Error occurred while updating DeviceType", e);
			throw new RuntimeException("Error occurred while updating DeviceType", e);
		}
		return MapperUtils.convertToDeviceTypeUpdateDTO(deviceType);

	}

	/**
	 * This method is used to retrieve all DeviceTypes by category.
	 * 
	 * @param category This is the category of the DeviceTypes to be retrieved.
	 * @return List<DeviceTypeDTO> This returns a list of DeviceTypes.
	 */

	@Override
	public List<DeviceTypeDTO> getDeviceTypesByCategory(String category) {
		LOGGER.info("Going to fetch DeviceType  by category: " + category);
		Category categoryEnum = commonService.validateCategory(category);
		List<DeviceType> deviceTypes = deviceTypeRepository.findByCategory(categoryEnum);
		if (deviceTypes.isEmpty()) {
			return null;
		}
		return deviceTypes.stream().map(this::convertToDeviceTypeDTO).collect(Collectors.toList());
	}

	/**
	 * This method is used to retrieve all DeviceTypes by category.
	 * 
	 * @param category This is the category of the DeviceTypes to be retrieved.
	 * @return List<String> This returns a list of DeviceTypes.
	 */
	@Override
	public List<String> getDeviceTypeNameByCategory(String category) {
		LOGGER.info("Going to fetch DeviceType names by category: " + category);
		Category categoryEnum = commonService.validateCategory(category);
		List<DeviceType> deviceTypes = deviceTypeRepository.findByCategory(categoryEnum);
		if (deviceTypes.isEmpty()) {
			return null;
		}
		return deviceTypes.stream().map(DeviceType::getName).collect(Collectors.toList());
	}

	/**
	 * This method is used to convert DeviceType to DeviceTypeDTO.
	 * 
	 * @param deviceType the device type to convert
	 * @return DeviceTypeDTO the converted DeviceType
	 */
	private DeviceTypeDTO convertToDeviceTypeDTO(DeviceType deviceType) {
		LOGGER.trace("Converting DeviceTypeDTO to DeviceType");
		return MapperUtils.convertToDeviceTypeDTO(deviceType);

	}

}
