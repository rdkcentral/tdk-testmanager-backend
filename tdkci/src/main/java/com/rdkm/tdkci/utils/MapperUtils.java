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
package com.rdkm.tdkci.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rdkm.tdkci.dto.DeviceDTO;
import com.rdkm.tdkci.dto.XconfDTO;
import com.rdkm.tdkci.model.Device;
import com.rdkm.tdkci.model.XconfConfig;

/*
 * Utility class for mapping between entity models and DTOs.
 */
public class MapperUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(MapperUtils.class);

	/**
	 * Converts an {@link XconfConfig} object to an {@link XconfDTO} object.
	 * <p>
	 * This method maps the properties of the given {@code XconfConfig} instance
	 * to a new {@code XconfDTO} instance. It sets the ID, device type name,
	 * configuration name, configuration ID, and configuration description.
	 * </p>
	 *
	 * @param xconfConfig the {@code XconfConfig} object to be converted
	 * @return a new {@code XconfDTO} object with mapped properties from
	 *         {@code xconfConfig}
	 */
	public static XconfDTO convertToXconfDTO(XconfConfig xconfConfig) {
		LOGGER.info("Mapping XconfConfig to XconfDTO: " + xconfConfig);
		XconfDTO xconfDTO = new XconfDTO();
		xconfDTO.setId(xconfConfig.getId());
		xconfDTO.setXconfDeviceTypeName(xconfConfig.getName());
		xconfDTO.setXconfConfigName(xconfConfig.getXconfigName());
		xconfDTO.setXconfConfigId(xconfConfig.getXconfigId());
		xconfDTO.setXconfConfigDescription(xconfConfig.getXconfigDescription());
		return xconfDTO;
	}

	/**
	 * Converts a {@link Device} entity to a {@link DeviceDTO} object.
	 * <p>
	 * This method maps all relevant fields from the given {@code Device} instance
	 * to a new {@code DeviceDTO}, including device identification, network details,
	 * category, status, file extension, upgrade requirement, port, image
	 * information,
	 * suites, test scripts, image prefixes, and Xconf model name.
	 * </p>
	 *
	 * @param device the {@link Device} object to be converted
	 * @return a populated {@link DeviceDTO} instance with data from the given
	 *         device
	 */
	public static DeviceDTO convertToDeviceDTO(Device device) {
		LOGGER.info("Mapping Device to DeviceDTO: " + device);
		DeviceDTO deviceDTO = new DeviceDTO();
		deviceDTO.setId(device.getId());
		deviceDTO.setDeviceName(device.getName());
		deviceDTO.setDeviceIp(device.getIp());
		deviceDTO.setDeviceMac(device.getMacAddress());
		deviceDTO.setDeviceCategory(device.getCategory().name());
		deviceDTO.setDeviceStatus(device.getDeviceStatus().name());
		deviceDTO.setDeviceFileExtension(device.getFileExtension());
		deviceDTO.setDeviceUpgradeRequired(device.isUpgradeRequired());
		deviceDTO.setDevicePort(device.getPort());
		deviceDTO.setLastUpdatedImageName(device.getLastUpdatedImageName());
		deviceDTO.setDeviceSuites(device.getDeviceSuites());
		deviceDTO.setDeviceTestScripts(device.getDeviceTestScripts());
		deviceDTO.setImagePrefixes(device.getImagePrefixes());
		deviceDTO.setXconfModelName(device.getXconfConfig().getName());
		return deviceDTO;
	}
}
