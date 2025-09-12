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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.rdkm.tdkservice.serviceimpl;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.enums.DeviceStatus;
import com.rdkm.tdkservice.model.Device;
import com.rdkm.tdkservice.repository.DeviceRepositroy;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.service.utilservices.InetUtilityService;
import com.rdkm.tdkservice.service.utilservices.ScriptExecutorService;
import com.rdkm.tdkservice.util.Constants;

/**
 * This class is used to update the status of the device
 */
@Service
public class DeviceStatusService {

	public static final Logger LOGGER = LoggerFactory.getLogger(DeviceStatusService.class);

	@Autowired
	ScriptExecutorService scriptExecutorService;

	@Autowired
	CommonService commonService;

	@Autowired
	private DeviceRepositroy deviceRepository;

	@Autowired
	private DeviceConfigService deviceConfigService;

	/**
	 * This method updates the status for all devices at a fixed rate. It fetches
	 * the list of all devices from the repository, attempts to update their status,
	 * and saves the updated status back to the repository.
	 * 
	 * @return boolean - true if the status update was successful for all devices,
	 *         false otherwise
	 */
	@Scheduled(initialDelay = 5000, fixedDelay = 5000)
	public void updateAllDeviceStatus() {
		LOGGER.debug("Updating status for all devices");
		// The status of BUSY, HANG devices should be checked first, as the
		// BUSY, HANG states of the TDK enabled devices are set from
		// the device itself for TDK enabled devices
		List<Device> devices = deviceRepository.findAllSortedByDeviceStatus();
		for (Device device : devices) {
			try {
				// Check if the device still exists in the repository
				if (!deviceRepository.existsById(device.getId())) {
					LOGGER.warn("Device not found: " + device.getName());
					continue;
				}
				DeviceStatus deviceStatus = fetchDeviceStatus(device);
				if (deviceStatus != null) {

					// If the device is on use from the script, then the status is
					// not set
					if (device.getDeviceStatus().equals(DeviceStatus.IN_USE)) {
						continue;
					} else {
						// If the device is not is use, then the current status should
						// be updated in the periodic update
						device.setDeviceStatus(deviceStatus);
					}

					deviceRepository.save(device);
				} else {

					LOGGER.warn("Failed to fetch status for device: " + device.getName());
				}
			} catch (Exception e) {
				LOGGER.error("Error updating status for device: " + device.getName(), e);
			}
		}
		LOGGER.debug("Status updated for all devices");

	}

	/**
	 * Updates the status of all devices filtered by category.
	 *
	 * @param category - the category of devices to update
	 */
	public void updateAllDeviceStatusByCategory(Category category) {
		LOGGER.debug("Updating status for all devices in category: " + category);
		List<Device> devices = deviceRepository.findAllSortedByDeviceStatus().stream()
				.filter(device -> device.getCategory().equals(category)).toList();
		for (Device device : devices) {
			try {
				if (!deviceRepository.existsById(device.getId())) {
					LOGGER.warn("Device not found: " + device.getName());
					continue;
				}
				DeviceStatus deviceStatus = fetchDeviceStatus(device);
				if (deviceStatus != null) {
					if (!device.getDeviceStatus().equals(DeviceStatus.IN_USE)) {
						device.setDeviceStatus(deviceStatus);
					}
					deviceRepository.save(device);
				} else {
					LOGGER.warn("Failed to fetch status for device: " + device.getName());
				}
			} catch (Exception e) {
				LOGGER.error("Error updating status for device: " + device.getName(), e);
			}
		}
		LOGGER.debug("Status updated for all devices in category: " + category);
	}

	/**
	 * This method is used to fetch the status of the device
	 * 
	 * @param device- device for which the status needs to be fetched
	 * @return DeviceStatus- status of the device
	 */
	public DeviceStatus fetchDeviceStatus(Device device) {
		LOGGER.debug("Fetching device status for device: " + device.getName());
		String[] scriptExecutionCommand = null;
		// Get the python command from the execution configuration
		String pythonCommand = commonService.getPythonCommandFromConfig();

		String deviceStatusOutput = null;
		if (device.isThunderEnabled()) {
			String pythonScriptPath = this.getAbsolutePath("callthunderdevicestatus_cmndline.py");
			if (pythonScriptPath == null) {
				LOGGER.error(" Thunder device status Script file not found ");
				return null;
			}
			scriptExecutionCommand = new String[] {
					pythonCommand,
					pythonScriptPath,
					device.getIp(),
					device.getThunderPort(),
					deviceConfigService.getDeviceConfigFileName(device.getName(), device.getDeviceType().getName(),
							device.isThunderEnabled()), // Fetches the config file name
					device.getMacId() != null ? device.getMacId() : null // Adds MAC ID or null
			};

		} else {
			String pythonScriptPath = this.getAbsolutePath("calldevicestatus_cmndline.py");
			if (pythonScriptPath == null) {
				LOGGER.error(" Thunder device status Script file not found ");
				return null;
			}
			String hostIPAddress;
			// Check if the device is RDKV and has an IPv6 address, then fetch the host IPv6
			// and by default fetch the host IPv4
			if (device.getCategory() == Category.RDKV && InetUtilityService.isIPv6Address(device.getIp())) {
				hostIPAddress = commonService.getHostIpAddress(Constants.IPV6);
			} else {
				hostIPAddress = commonService.getHostIpAddress(Constants.IPV4);
			}
			scriptExecutionCommand = new String[] { pythonCommand, pythonScriptPath, device.getIp(), device.getPort(),
					hostIPAddress, device.getName() };

		}

		// Wait time of 5 seconds. If the device status is not obtained after 5 seconds,
		// then the device status fetching is failed, the status checker itself has
		// the logic to return the status as NOT_FOUND in case of inaccesibility
		// So not handling that here
		deviceStatusOutput = scriptExecutorService.executeScript(scriptExecutionCommand, 5);

		DeviceStatus deviceStatus = getDeviceStatusFromOutput(deviceStatusOutput);
		LOGGER.debug("Device status fetched for device: " + device.getName() + " is: " + deviceStatus.toString());
		return deviceStatus;
	}

	/**
	 * This method is used to get the absolute path of the script file
	 * 
	 * @param scriptFileName - name of the script file
	 * @return String - absolute path of the script file
	 */
	private String getAbsolutePath(String scriptFileName) {
		LOGGER.debug("Getting absolute path of the script file: " + scriptFileName);
		String thunderStatusFilePath = AppConfig.getBaselocation() + "/" + scriptFileName;
		File statusCheckerFile = new File(thunderStatusFilePath);
		if (!statusCheckerFile.exists()) {
			LOGGER.error("Script file not found at location: " + thunderStatusFilePath);
			return null;
		}
		String pythonScriptPath = statusCheckerFile.getAbsolutePath();
		LOGGER.debug("Absolute path of the script file: " + pythonScriptPath);
		return pythonScriptPath;
	}

	/**
	 * This method is used to get the device status from the output of the device
	 * status script
	 * 
	 * @param deviceStatusOutput - output of the device status script
	 * @return DeviceStatus - status of the device
	 */
	public DeviceStatus getDeviceStatusFromOutput(String deviceStatusOutput) {
		LOGGER.debug("Mapping device status from output of the device status script: " + deviceStatusOutput);
		DeviceStatus deviceStatus;
		if (deviceStatusOutput == null) {
			return DeviceStatus.NOT_FOUND;
		}
		switch (deviceStatusOutput.trim()) {
			case Constants.BUSY:
				deviceStatus = DeviceStatus.BUSY;
				break;
			case Constants.FREE:
				deviceStatus = DeviceStatus.FREE;
				break;
			case Constants.NOT_FOUND:
				deviceStatus = DeviceStatus.NOT_FOUND;
				break;
			case Constants.HANG:
				deviceStatus = DeviceStatus.HANG;
				break;
			case Constants.TDK_DISABLED:
				deviceStatus = DeviceStatus.TDK_DISABLED;
				break;
			default:
				deviceStatus = DeviceStatus.NOT_FOUND;
				break;
		}
		LOGGER.debug("Device status mapped to: " + deviceStatus);
		return deviceStatus;
	}

	/**
	 * This method is used to set the status of the device
	 * 
	 * @param deviceStatus - status to be set
	 * @param device       - device for which the status needs to be set
	 * @return boolean - true if the status was set successfully
	 */
	public boolean setDeviceStatus(DeviceStatus deviceStatus, String device) {
		try {
			// Fetch the device from the repository
			Device existingDevice = deviceRepository.findByName(device);
			if (existingDevice == null) {
				LOGGER.warn("Device not found");
				return false;
			}

			// Update the device status
			existingDevice.setDeviceStatus(deviceStatus);

			// Save the updated device back to the repository
			deviceRepository.save(existingDevice);
			LOGGER.debug("Device status updated successfully for device: " + device);
			return true;
		} catch (Exception e) {
			LOGGER.error("Error updating device status for device: " + device, e);
			return false;
		}

	}

	/**
	 * This method is used to fetch and update the status of the device
	 * 
	 * @param device - device for which the status needs to be fetched and updated
	 * @return boolean - true if the status was fetched and updated successfully
	 */
	public boolean fetchAndUpdateDeviceStatus(Device device) {
		LOGGER.debug("Fetching and updating status for device: " + device.getName());
		try {
			// Fetch the device status
			DeviceStatus deviceStatus = fetchDeviceStatus(device);
			if (deviceStatus != null) {
				// Update the device status in the database
				return setDeviceStatus(deviceStatus, device.getName());
			} else {
				LOGGER.warn("Failed to fetch status for device: " + device.getName());
				return false;
			}
		} catch (Exception e) {
			LOGGER.error("Error fetching and updating status for device: " + device.getName(), e);
			return false;
		}
	}

}
