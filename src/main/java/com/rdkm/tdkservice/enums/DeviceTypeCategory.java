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
package com.rdkm.tdkservice.enums;

/*
 * Enum for DeviceTypeCategory
 */
public enum DeviceTypeCategory {

	CLIENT("CLIENT");

	private String name;

	DeviceTypeCategory(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	/**
	 * Get Category by name
	 * 
	 * @param name
	 * @return
	 */
	public static DeviceTypeCategory getDeviceTypeCategory(String name) {
		for (DeviceTypeCategory deviceTypeEnum : DeviceTypeCategory.values()) {
			if (deviceTypeEnum.getName().equals(name)) {
				return deviceTypeEnum;
			}
		}
		return null;
	}

	/**
	 * Get default theme
	 * 
	 * @return
	 */
	public static DeviceTypeCategory getDefaultCategory() {
		return CLIENT;
	}

}
