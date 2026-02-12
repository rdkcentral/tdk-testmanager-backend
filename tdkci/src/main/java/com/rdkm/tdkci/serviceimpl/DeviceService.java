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
package com.rdkm.tdkci.serviceimpl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rdkm.tdkci.dto.DeviceCreateDTO;
import com.rdkm.tdkci.dto.DeviceDTO;
import com.rdkm.tdkci.enums.Category;
import com.rdkm.tdkci.enums.DeviceStatus;
import com.rdkm.tdkci.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkci.exception.ResourceNotFoundException;
import com.rdkm.tdkci.exception.UserInputException;
import com.rdkm.tdkci.model.Device;
import com.rdkm.tdkci.model.XconfConfig;
import com.rdkm.tdkci.repository.DeviceRepository;
import com.rdkm.tdkci.repository.XconfRepository;
import com.rdkm.tdkci.service.IDeviceService;
import com.rdkm.tdkci.utils.MapperUtils;
import com.rdkm.tdkci.utils.Utils;

/**
 * Service implementation class for managing devices.
 */
@Service
public class DeviceService implements IDeviceService {

	@Autowired
	private DeviceRepository deviceRepository;

	@Autowired
	private XconfRepository xconfRepository;

	private static final Logger LOGGER = LoggerFactory.getLogger(DeviceService.class);

	/**
	 * Creates a new device based on the provided {@link DeviceCreateDTO} request.
	 * <p>
	 * Validates the device category, checks for existing devices with the same
	 * name, IP, or MAC address, and ensures the referenced XconfConfig exists.
	 * Populates a new {@link Device} entity with the request data, including
	 * suites, test scripts, image prefixes, file extension, last updated image
	 * name, port, upgrade requirement, and associated XconfConfig. Saves the device
	 * to the repository.
	 * </p>
	 *
	 * @param deviceRequest the DTO containing device creation details
	 * @return {@code true} if the device was created successfully, {@code false}
	 *         otherwise
	 * @throws ResourceAlreadyExistsException if a device with the same name, IP, or
	 *                                        MAC address already exists
	 * @throws ResourceNotFoundException      if the specified XconfConfig does not
	 *                                        exist
	 * @throws UserInputException             if both device suites and test scripts
	 *                                        are empty
	 */
	@Override
	public boolean createDevice(DeviceCreateDTO deviceRequest) {
		LOGGER.info("DeviceRequest: " + deviceRequest);
		Utils.checkCategoryValid(deviceRequest.getDeviceCategory());
		if (deviceRepository.existsByName(deviceRequest.getDeviceName())) {
			LOGGER.error("Device with name {} already exists", deviceRequest.getDeviceName());
			throw new ResourceAlreadyExistsException("Device Name", deviceRequest.getDeviceName());
		}

		if (deviceRepository.existsByIp(deviceRequest.getDeviceIp())) {
			LOGGER.error("Device with IP {} already exists", deviceRequest.getDeviceIp());
			throw new ResourceAlreadyExistsException("Device IP", deviceRequest.getDeviceIp());
		}

		if (deviceRepository.existsByMacAddress(deviceRequest.getDeviceMac())) {
			LOGGER.error("Device with MAC Address {} already exists", deviceRequest.getDeviceMac());
			throw new ResourceAlreadyExistsException("Device MAC Address", deviceRequest.getDeviceMac());
		}
		XconfConfig xconf = xconfRepository.findByName(deviceRequest.getXconfConfig());
		if (xconf == null) {
			LOGGER.error("XconfConfig with name {} does not exist", deviceRequest.getXconfConfig());
			throw new ResourceNotFoundException("XconfConfig Name", deviceRequest.getXconfConfig());
		}
		try {
			Device device = new Device();
			device.setName(deviceRequest.getDeviceName());
			device.setIp(deviceRequest.getDeviceIp());
			device.setMacAddress(deviceRequest.getDeviceMac());
			device.setDeviceStatus(DeviceStatus.FREE);

			if (deviceRequest.getDeviceSuites() != null && !deviceRequest.getDeviceSuites().isEmpty()) {
				device.setDeviceSuites(deviceRequest.getDeviceSuites());
			} else if (deviceRequest.getDeviceTestScripts() != null
					&& !deviceRequest.getDeviceTestScripts().isEmpty()) {
				device.setDeviceTestScripts(deviceRequest.getDeviceTestScripts());
			} else {
				LOGGER.error("Device suites and test scripts are empty for device: " + deviceRequest.getDeviceName());
				throw new UserInputException(
						"Device suites and test scripts cannot be empty, Either one of it should not be empty");
			}
			if (deviceRequest.getImagePrefixes() != null && !deviceRequest.getImagePrefixes().isEmpty()) {
				device.setImagePrefixes(deviceRequest.getImagePrefixes());
			}
			device.setCategory(Category.valueOf(deviceRequest.getDeviceCategory()));
			if (deviceRequest.getDeviceFileExtension() != null && !deviceRequest.getDeviceFileExtension().isEmpty()) {
				device.setFileExtension(deviceRequest.getDeviceFileExtension());
			}
			if (deviceRequest.getLastUpdatedImageName() != null && !deviceRequest.getLastUpdatedImageName().isEmpty()) {
				device.setLastUpdatedImageName(deviceRequest.getLastUpdatedImageName());
			}
			if (deviceRequest.getDevicePort() != null && !deviceRequest.getDevicePort().isEmpty()) {
				device.setPort(deviceRequest.getDevicePort());
			}
			device.setUpgradeRequired(deviceRequest.isDeviceUpgradeRequired());
			device.setXconfConfig(xconf);
			deviceRepository.save(device);
			return true;
		} catch (Exception e) {
			LOGGER.error("Failed to create Device: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Retrieves all devices from the repository and converts them to DeviceDTO
	 * objects.
	 * <p>
	 * This method fetches all {@link Device} entities from the data source. If no
	 * devices are found, it logs a warning and returns {@code null}. Otherwise, it
	 * maps each {@link Device} to a {@link DeviceDTO} using
	 * {@link MapperUtils#convertToDeviceDTO(Device)} and returns the resulting
	 * list.
	 *
	 * @return a list of {@link DeviceDTO} objects representing all devices, or
	 *         {@code null} if no devices are found.
	 */
	@Override
	public List<DeviceDTO> getAllDevices() {
		LOGGER.info("Fetching all devices from the repository");
		List<Device> devices = deviceRepository.findAll();
		if (devices.isEmpty()) {
			LOGGER.warn("No devices found in the repository");
			return null;
		}
		return devices.stream().map(MapperUtils::convertToDeviceDTO).collect(Collectors.toList());

	}

	/**
	 * Retrieves the names of all devices from the repository.
	 * <p>
	 * This method fetches all {@link Device} entities from the repository and
	 * extracts their names. If no devices are found, it logs a warning and returns
	 * {@code null}.
	 * </p>
	 *
	 * @return a list of device names, or {@code null} if no devices are found
	 */
	@Override
	public List<String> getAllDeviceNames() {
		LOGGER.info("Fetching all device names from the repository");
		List<Device> devices = deviceRepository.findAll();
		if (devices.isEmpty()) {
			LOGGER.warn("No devices found in the repository");
			return null;
		}
		return devices.stream().map(Device::getName).collect(Collectors.toList());
	}

	/**
	 * Deletes a device by its unique identifier.
	 * <p>
	 * This method attempts to delete a device from the repository using the
	 * provided UUID. If the device does not exist, a
	 * {@link ResourceAlreadyExistsException} is thrown. Logs the deletion process
	 * and returns {@code true} if the deletion is successful, otherwise logs the
	 * error and returns {@code false}.
	 *
	 * @param id the UUID of the device to be deleted
	 * @return {@code true} if the device was deleted successfully, {@code false}
	 *         otherwise
	 * @throws ResourceAlreadyExistsException if the device with the specified ID
	 *                                        does not exist
	 */
	@Override
	public boolean deleteDeviceById(UUID id) {
		LOGGER.info("Deleting device with id: " + id);
		if (!deviceRepository.existsById(id)) {
			LOGGER.error("Device with ID {} does not exist", id);
			throw new ResourceAlreadyExistsException("Device ID", id.toString());
		}
		try {
			deviceRepository.deleteById(id);
			LOGGER.info("Device deleted successfully");
			return true;
		} catch (Exception e) {
			LOGGER.error("Failed to delete Device: " + e.getMessage());
			return false;
		}

	}

	/**
	 * Updates an existing device with the provided details.
	 * <p>
	 * This method performs the following operations:
	 * <ul>
	 * <li>Validates the device category.</li>
	 * <li>Retrieves the existing device by its ID, throwing
	 * {@link ResourceNotFoundException} if not found.</li>
	 * <li>Checks for uniqueness of device name, IP, and MAC address, throwing
	 * {@link ResourceAlreadyExistsException} if duplicates are found.</li>
	 * <li>Updates device properties such as name, IP, MAC address, XconfConfig,
	 * category, status, file extension, port, upgrade requirement, last updated
	 * image name, suites, test scripts, and image prefixes.</li>
	 * <li>Ensures that either device suites or test scripts are provided, throwing
	 * {@link UserInputException} if both are empty.</li>
	 * <li>Saves the updated device to the repository.</li>
	 * </ul>
	 * Logs relevant information and errors during the update process.
	 *
	 * @param deviceUpdateRequest the {@link DeviceDTO} containing updated device
	 *                            information
	 * @return {@code true} if the device was updated successfully, {@code false}
	 *         otherwise
	 * @throws ResourceNotFoundException      if the device or related XconfConfig
	 *                                        does not exist
	 * @throws ResourceAlreadyExistsException if the device name, IP, or MAC address
	 *                                        already exists
	 * @throws UserInputException             if both device suites and test scripts
	 *                                        are empty
	 */
	@Override
	public boolean updateDevice(DeviceDTO deviceUpdateRequest) {
		LOGGER.info("Updating device with id: " + deviceUpdateRequest.getId());
		Utils.checkCategoryValid(deviceUpdateRequest.getDeviceCategory());
		Device existingDevice = deviceRepository.findById(deviceUpdateRequest.getId()).orElseThrow(() -> {
			LOGGER.error("Device with ID {} does not exist", deviceUpdateRequest.getId());
			return new ResourceNotFoundException("Device ID", deviceUpdateRequest.getId().toString());
		});

		// Corner case while updateing when any other device has the same name except
		// the current device update
		if (!Utils.isEmpty(deviceUpdateRequest.getDeviceName())) {
			if (deviceRepository.existsByName(deviceUpdateRequest.getDeviceName())
					&& !existingDevice.getName().equals(deviceUpdateRequest.getDeviceName())) {
				LOGGER.error("Device with name {} already exists", deviceUpdateRequest.getDeviceName());
				throw new ResourceAlreadyExistsException("Device Name", deviceUpdateRequest.getDeviceName());
			} else {
				existingDevice.setName(deviceUpdateRequest.getDeviceName());
			}
		}
		if (!Utils.isEmpty(deviceUpdateRequest.getDeviceIp())) {
			if (deviceRepository.existsByIp(deviceUpdateRequest.getDeviceIp())
					&& !existingDevice.getIp().equals(deviceUpdateRequest.getDeviceIp())) {
				LOGGER.error("Device with IP {} already exists", deviceUpdateRequest.getDeviceIp());
				throw new ResourceAlreadyExistsException("Device IP", deviceUpdateRequest.getDeviceIp());
			} else {
				existingDevice.setIp(deviceUpdateRequest.getDeviceIp());
			}
		}
		if (!Utils.isEmpty(deviceUpdateRequest.getDeviceMac())) {
			if (deviceRepository.existsByMacAddress(deviceUpdateRequest.getDeviceMac())
					&& !existingDevice.getMacAddress().equals(deviceUpdateRequest.getDeviceMac())) {
				LOGGER.error("Device with MAC Address {} already exists", deviceUpdateRequest.getDeviceMac());
				throw new ResourceAlreadyExistsException("Device MAC Address", deviceUpdateRequest.getDeviceMac());
			} else {
				existingDevice.setMacAddress(deviceUpdateRequest.getDeviceMac());
			}
		}

		if (!Utils.isEmpty(deviceUpdateRequest.getXconfConfig())) {
			XconfConfig xconf = xconfRepository.findByName(deviceUpdateRequest.getXconfConfig());
			if (xconf == null) {
				LOGGER.error("XconfConfig with name {} does not exist", deviceUpdateRequest.getXconfConfig());
				throw new ResourceNotFoundException("XconfConfig Name", deviceUpdateRequest.getXconfConfig());
			}
			existingDevice.setXconfConfig(xconf);
		}
		if (!Utils.isEmpty(deviceUpdateRequest.getDeviceCategory())) {
			existingDevice.setCategory(Category.valueOf(deviceUpdateRequest.getDeviceCategory()));
		}
		if (!Utils.isEmpty(deviceUpdateRequest.getDeviceStatus())) {
			existingDevice.setDeviceStatus(DeviceStatus.valueOf(deviceUpdateRequest.getDeviceStatus()));
		}
		if (!Utils.isEmpty(deviceUpdateRequest.getDeviceFileExtension())) {
			existingDevice.setFileExtension(deviceUpdateRequest.getDeviceFileExtension());
		}
		if (!Utils.isEmpty(deviceUpdateRequest.getDevicePort())) {
			existingDevice.setPort(deviceUpdateRequest.getDevicePort());
		}
		existingDevice.setUpgradeRequired(deviceUpdateRequest.isDeviceUpgradeRequired());
		if (!Utils.isEmpty(deviceUpdateRequest.getLastUpdatedImageName())) {
			existingDevice.setLastUpdatedImageName(deviceUpdateRequest.getLastUpdatedImageName());
		}
		if (deviceUpdateRequest.getDeviceSuites() != null && !deviceUpdateRequest.getDeviceSuites().isEmpty()
				&& (deviceUpdateRequest.getDeviceTestScripts() == null
						|| deviceUpdateRequest.getDeviceTestScripts().isEmpty())) {
			existingDevice.setDeviceSuites(deviceUpdateRequest.getDeviceSuites());
			existingDevice.setDeviceTestScripts(null);
		} else if (deviceUpdateRequest.getDeviceTestScripts() != null
				&& !deviceUpdateRequest.getDeviceTestScripts().isEmpty()) {
			existingDevice.setDeviceTestScripts(deviceUpdateRequest.getDeviceTestScripts());
			existingDevice.setDeviceSuites(null);
		} else {
			LOGGER.error("Device suites and test scripts are empty for device: " + deviceUpdateRequest.getDeviceName());
			throw new UserInputException("Device suites and test scripts cannot be empty");
		}
		if (deviceUpdateRequest.getImagePrefixes() != null && !deviceUpdateRequest.getImagePrefixes().isEmpty()) {
			existingDevice.setImagePrefixes(deviceUpdateRequest.getImagePrefixes());
		}
		if (!Utils.isEmpty(deviceUpdateRequest.getLastUpdatedImageName())) {
			existingDevice.setLastUpdatedImageName(deviceUpdateRequest.getLastUpdatedImageName());
		}

		try {
			deviceRepository.save(existingDevice);
			LOGGER.info("Device updated successfully");
			return true;
		} catch (Exception e) {
			LOGGER.error("Failed to update Device: " + e.getMessage());
			return false;
		}
	}

}
