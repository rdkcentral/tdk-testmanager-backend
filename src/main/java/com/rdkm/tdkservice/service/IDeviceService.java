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
package com.rdkm.tdkservice.service;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.rdkm.tdkservice.dto.DeviceCreateDTO;
import com.rdkm.tdkservice.dto.DeviceResponseDTO;
import com.rdkm.tdkservice.dto.DeviceStatusResponseDTO;
import com.rdkm.tdkservice.dto.DeviceUpdateDTO;

public interface IDeviceService {
	/**
	 * This method is used to create a new Device.
	 *
	 * @param deviceDto This is the request object containing the details of the
	 *                  Device to be created.
	 * @return boolean This returns true if the Device was created successfully,
	 *         false otherwise.
	 */
	public boolean createDevice(DeviceCreateDTO deviceDto);

	/**
	 * This method is used to update a Device.
	 *
	 * @param deviceUpdateDTO This is the request object containing the updated
	 *                        details of the Device.
	 * @return Device This returns the updated Device.
	 */
	public boolean updateDevice(DeviceUpdateDTO deviceUpdateDTO);

	/**
	 * This method is used to retrieve all Devices.
	 *
	 * @return List<Device> This returns a list of all Devices.
	 */
	public List<DeviceResponseDTO> getAllDeviceDetails();

	/**
	 * This method is used to retrieve all Devices by Category.
	 *
	 * @return List<Device> This returns a list of all Devices.
	 */
	public List<DeviceResponseDTO> getAllDeviceDetailsByCategory(String category);

	/**
	 * This method is used to find a Device by its id.
	 *
	 * @param id This is the id of the Device to be found.
	 * @return Device This returns the found Device.
	 */
	public DeviceResponseDTO findDeviceById(UUID id);

	/**
	 * This method is used to delete a Device by its id.
	 *
	 * @param id This is the id of the Device to be deleted.
	 */
	public boolean deleteDeviceById(UUID id);

	/**
	 * This method is used to parse the Device XML.
	 *
	 * @param file This is the file containing the Device XML.
	 */
	public boolean parseXMLForDevice(MultipartFile file);

	/**
	 * This method is used to download the Device XML.
	 *
	 * @param name This is the stbName of the Device to be downloaded.
	 * @return String This returns the Device XML.
	 */
	public String downloadDeviceXML(String name);

	/**
	 * This method is used to download all Devices by Category.
	 *
	 * @param category This is the category of the Devices to be downloaded.
	 * @return String This returns the Device XML.
	 */
	public Path downloadAllDevicesByCategory(String category);

	/**
	 * This method is used to get the status of all the devices in the given
	 * category
	 * 
	 * @param category- category of the devices say RDKV, RDKB, RDKC
	 * @return List of device status response DTOs
	 */
	public List<DeviceStatusResponseDTO> getAllDeviceStatus(String category);

	/**
	 * This method is used to get the details of a device
	 * 
	 * @param deviceIp- IP of the device
	 * @return String-
	 */
	public String getDeviceDetails(String deviceIp);

	/**
	 * This method is used to get the Thunder ports of a device
	 * 
	 * @param deviceIp- IP of the device
	 * @return String-
	 */
	String getThunderDevicePorts(String deviceIp);

	/**
	 * This method is used to get the device type of a device
	 * 
	 * @param deviceIp- IP of the device
	 */
	String getDeviceType(String deviceIp);

	/**
	 * This method is used to get the devices by category and thunder status
	 * 
	 * @param category-         category of the devices say RDKV, RDKB, RDKC
	 * @param isThunderEnabled- true if thunder is enabled, false otherwise
	 * @return List of device response DTOs
	 */
	public List<DeviceResponseDTO> getDevicesByCategoryAndThunderStatus(String category, Boolean isThunderEnabled);

	/**
	 * This method is used to set the thunder enabled status for the device with the
	 * given id.
	 * 
	 * @param id The ID of the device to set the thunder enabled status for.
	 * 
	 */
	boolean toggleThunderEnabledstatus(String deviceIP);

	/**
	 * This method is used to get the device status
	 * 
	 * @param deviceIp - IP of the device
	 * @return DeviceResponse DTO - device status
	 */
	public DeviceStatusResponseDTO getDeviceStatus(String deviceIp);

	/**
	 * This method is used to update the status of all devices in the given category
	 * 
	 * @param category - RDKV, RDKB
	 * @return List<Device> - List of devices with updated status
	 */
	public List<DeviceStatusResponseDTO> updateAndGetAllDeviceStatus(String category);
	
	/**
	 * This method is used to find device details by its name.
	 * 
	 * @param deviceName This is the name of the Device to be found.
	 * @return String This returns the found Device details in String format.
	 */
	String findDeviceDetailsByName(String deviceName);

}
