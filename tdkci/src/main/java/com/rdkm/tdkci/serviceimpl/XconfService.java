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
package com.rdkm.tdkci.serviceimpl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.rdkm.tdkci.dto.XconfCreateDTO;
import com.rdkm.tdkci.dto.XconfDTO;
import com.rdkm.tdkci.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkci.exception.ResourceNotFoundException;
import com.rdkm.tdkci.model.XconfConfig;
import com.rdkm.tdkci.repository.XconfRepository;
import com.rdkm.tdkci.service.IXconfService;
import com.rdkm.tdkci.utils.MapperUtils;
import com.rdkm.tdkci.utils.Utils;
import com.rdkm.tdkci.exception.DeleteFailedException;

/**
 * Service implementation class for managing xconf configurations.
 */
@Service
public class XconfService implements IXconfService {

	@Autowired
	private XconfRepository xconfRepository;

	private static final Logger LOGGER = LoggerFactory.getLogger(XconfService.class);

	/**
	 * Creates a new Xconf configuration based on the provided
	 * {@link XconfCreateDTO} request.
	 * <p>
	 * This method performs the following steps:
	 * <ul>
	 * <li>Validates the device type category in the request.</li>
	 * <li>Checks for existing XconfConfig by ID, device type name, and config name
	 * to prevent duplicates.</li>
	 * <li>If no duplicates are found, creates and saves a new {@link XconfConfig}
	 * entity.</li>
	 * </ul>
	 * If any duplicate is found, a {@link ResourceAlreadyExistsException} is
	 * thrown.
	 * If creation fails due to other exceptions, the method logs the error and
	 * returns {@code false}.
	 *
	 * @param xconfRequest the DTO containing Xconf configuration details to be
	 *                     created
	 * @return {@code true} if the Xconf configuration is created successfully,
	 *         {@code false} otherwise
	 * @throws ResourceAlreadyExistsException if a configuration with the same ID,
	 *                                        device type name, or config name
	 *                                        already exists
	 */
	@Override
	public boolean createXconf(XconfCreateDTO xconfRequest) {
		LOGGER.info("xconfRequest: " + xconfRequest);
		Utils.checkCategoryValid(xconfRequest.getXconfDeviceTypeName());
		if (xconfRepository.existsByXconfigId(xconfRequest.getXconfConfigId())) {
			LOGGER.error("XconfConfig with ID {} already exists", xconfRequest.getXconfConfigId());
			throw new ResourceAlreadyExistsException("XconfConfig ID", xconfRequest.getXconfConfigId());
		}
		if (xconfRepository.findByName(xconfRequest.getXconfDeviceTypeName()) != null) {
			LOGGER.error("XconfConfig with name {} already exists", xconfRequest.getXconfDeviceTypeName());
			throw new ResourceAlreadyExistsException("XconfConfig Name", xconfRequest.getXconfDeviceTypeName());
		}

		if (xconfRepository.existsByXconfigName(xconfRequest.getXconfConfigName())) {
			LOGGER.error("XconfConfig with name {} already exists", xconfRequest.getXconfConfigName());
			throw new ResourceAlreadyExistsException("XconfConfig Name", xconfRequest.getXconfConfigName());
		}

		try {
			XconfConfig xconfConfig = new XconfConfig();
			xconfConfig.setXconfigId(xconfRequest.getXconfConfigId());
			xconfConfig.setXconfigName(xconfRequest.getXconfConfigName());
			xconfConfig.setXconfigDescription(xconfRequest.getXconfConfigDescription());
			xconfConfig.setName(xconfRequest.getXconfDeviceTypeName());
			xconfRepository.save(xconfConfig);
			LOGGER.info("Xconf created successfully");
			return true;
		} catch (Exception e) {
			LOGGER.error("Failed to create Xconf: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Retrieves all Xconf configurations from the repository.
	 * <p>
	 * This method fetches all {@link XconfConfig} entities, converts them to
	 * {@link XconfDTO} objects,
	 * and returns them as a list. If no configurations are found, it logs a warning
	 * and returns {@code null}.
	 *
	 * @return a list of {@link XconfDTO} representing all Xconf configurations, or
	 *         {@code null} if none are found
	 */
	@Override
	public List<XconfDTO> findAllXconfConfigurations() {
		LOGGER.info("Fetching all xconf configurations");
		List<XconfConfig> xconfConfigs = xconfRepository.findAll();
		if (xconfConfigs.isEmpty()) {
			LOGGER.warn("No Xconf configurations found");
			return null;
		}
		return xconfConfigs.stream().map(MapperUtils::convertToXconfDTO).collect(Collectors.toList());
	}

	/**
	 * Retrieves a list of all Xconf configuration names.
	 * <p>
	 * This method fetches all XconfConfig entities from the repository and extracts
	 * their names.
	 * If no configurations are found, it logs a warning and returns {@code null}.
	 *
	 * @return a list of Xconf configuration names, or {@code null} if none are
	 *         found
	 */
	@Override
	public List<String> findXconfNames() {
		LOGGER.info("Fetching all xconf names");
		List<XconfConfig> xconfConfigs = xconfRepository.findAll();
		if (xconfConfigs.isEmpty()) {
			LOGGER.warn("No Xconf configurations found");
			return null;
		}
		return xconfConfigs.stream().map(XconfConfig::getName).collect(Collectors.toList());
	}

	/**
	 * Deletes the XconfConfig entity with the specified UUID.
	 * <p>
	 * Logs the deletion attempt and checks if the entity exists before deletion.
	 * If the entity does not exist, throws a {@link ResourceNotFoundException}.
	 * If a data integrity violation occurs during deletion, throws a
	 * {@link DeleteFailedException}.
	 *
	 * @param id the UUID of the XconfConfig to delete
	 * @return {@code true} if the deletion was successful
	 * @throws ResourceNotFoundException if the XconfConfig with the given ID does
	 *                                   not exist
	 * @throws DeleteFailedException     if an error occurs during deletion due to
	 *                                   data integrity violation
	 */
	@Override
	public boolean deleteXconfById(UUID id) {
		LOGGER.info("Deleting xconf with id: " + id);
		if (!xconfRepository.existsById(id)) {
			LOGGER.error("XconfConfig with ID {} does not exist", id);
			throw new ResourceNotFoundException("XconfConfig ID", id.toString());
		}
		try {
			xconfRepository.deleteById(id);
			LOGGER.info("Xconf deleted successfully");
			return true;
		} catch (DataIntegrityViolationException e) {
			LOGGER.error("Error occurred while deleting xconf with id: " + id, e);
			throw new DeleteFailedException();
		}
	}

	/**
	 * Updates an existing XconfConfig entity with the provided details.
	 * <p>
	 * The method performs the following steps:
	 * <ul>
	 * <li>Logs the update request.</li>
	 * <li>Validates the device type category.</li>
	 * <li>Retrieves the existing XconfConfig by ID, throwing
	 * {@link ResourceNotFoundException} if not found.</li>
	 * <li>Checks for uniqueness of the configuration name and ID, throwing
	 * {@link ResourceAlreadyExistsException} if duplicates are found.</li>
	 * <li>Updates the configuration name, ID, device type name, and description if
	 * provided and valid.</li>
	 * <li>Saves the updated entity to the repository.</li>
	 * <li>Logs success or failure and returns a boolean indicating the result.</li>
	 * </ul>
	 *
	 * @param xconfUpdateRequest the DTO containing updated XconfConfig details
	 * @return {@code true} if the update was successful, {@code false} otherwise
	 * @throws ResourceNotFoundException      if the XconfConfig with the specified
	 *                                        ID does not exist
	 * @throws ResourceAlreadyExistsException if the configuration name or ID
	 *                                        already exists for another entity
	 */
	@Override
	public boolean updateXconf(XconfDTO xconfUpdateRequest) {
		LOGGER.info("xconfUpdateRequest: " + xconfUpdateRequest);
		Utils.checkCategoryValid(xconfUpdateRequest.getXconfDeviceTypeName());
		XconfConfig existingXconf = xconfRepository.findById(xconfUpdateRequest.getId()).orElseThrow(() -> {
			LOGGER.error("XconfConfig with ID {} does not exist", xconfUpdateRequest.getId());
			return new ResourceNotFoundException("XconfConfig ID", xconfUpdateRequest.getId().toString());
		});
		if (!Utils.isEmpty(xconfUpdateRequest.getXconfConfigName())) {
			if (xconfRepository.existsByXconfigName(xconfUpdateRequest.getXconfConfigName())
					&& !existingXconf.getXconfigName().equals(xconfUpdateRequest.getXconfConfigName())) {
				LOGGER.error("XconfConfig with name {} already exists", xconfUpdateRequest.getXconfConfigName());
				throw new ResourceAlreadyExistsException("XconfConfig Name", xconfUpdateRequest.getXconfConfigName());
			} else {
				existingXconf.setXconfigName(xconfUpdateRequest.getXconfConfigName());
			}
		}
		if (!Utils.isEmpty(xconfUpdateRequest.getXconfConfigId())) {
			if (xconfRepository.existsByXconfigId(xconfUpdateRequest.getXconfConfigId())
					&& !existingXconf.getXconfigId().equals(xconfUpdateRequest.getXconfConfigId())) {
				LOGGER.error("XconfConfig with ID {} already exists", xconfUpdateRequest.getXconfConfigId());
				throw new ResourceAlreadyExistsException("XconfConfig ID", xconfUpdateRequest.getXconfConfigId());
			} else {
				existingXconf.setXconfigId(xconfUpdateRequest.getXconfConfigId());
			}
		}
		if (!Utils.isEmpty(xconfUpdateRequest.getXconfDeviceTypeName())) {
			XconfConfig xconfByName = xconfRepository.findByName(xconfUpdateRequest.getXconfDeviceTypeName());
			if (xconfByName != null && !xconfByName.getId().equals(xconfUpdateRequest.getId())) {
				LOGGER.error("XconfConfig with name {} already exists", xconfUpdateRequest.getXconfDeviceTypeName());
				throw new ResourceAlreadyExistsException("XconfConfig Name",
						xconfUpdateRequest.getXconfDeviceTypeName());
			} else {
				existingXconf.setName(xconfUpdateRequest.getXconfDeviceTypeName());
			}
		}
		if (!Utils.isEmpty(xconfUpdateRequest.getXconfConfigDescription())) {
			existingXconf.setXconfigDescription(xconfUpdateRequest.getXconfConfigDescription());
		}
		try {
			xconfRepository.save(existingXconf);
			LOGGER.info("Xconf updated successfully");
			return true;

		} catch (Exception e) {
			LOGGER.error("Failed to update Xconf: " + e.getMessage());
			return false;
		}
	}

}
