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

import java.util.List;
import java.util.UUID;

import org.json.JSONObject;

import com.rdkm.tdkservice.dto.PrimitiveTestCreateDTO;
import com.rdkm.tdkservice.dto.PrimitiveTestDTO;
import com.rdkm.tdkservice.dto.PrimitiveTestNameAndIdDTO;
import com.rdkm.tdkservice.dto.PrimitiveTestUpdateDTO;

/**
 * The IPrimitiveTestService interface provides the methods to create, update,
 * delete and get the primitive test details.
 */
public interface IPrimitiveTestService {

	/**
	 * This method is used to create the primitive test.
	 * 
	 * @param primitiveTestDTO
	 * @return boolean
	 */
	boolean createPrimitiveTest(PrimitiveTestCreateDTO primitiveTestDTO);

	/**
	 * This method is used to update the primitive test.
	 * 
	 * @param primitiveTestDTO
	 * @return boolean
	 */
	boolean updatePrimitiveTest(PrimitiveTestUpdateDTO primitiveTestDTO);

	/**
	 * This method is used to delete the primitive test by id.
	 * 
	 * @param id
	 */
	void deleteById(UUID id);

	/**
	 * This method is used to get the primitive test details by id.
	 * 
	 * @param id
	 * @return Primitive TestDTO
	 */
	PrimitiveTestDTO getPrimitiveTestDetailsById(UUID id);

	/**
	 * This method is used to get the primitive test details by module name.
	 * 
	 * @param moduleName
	 * @return List<PrimitiveTestNameAndIdDTO>
	 */
	List<PrimitiveTestNameAndIdDTO> getPrimitiveTestDetailsByModuleName(String moduleName);

	/**
	 * This method is used to get the primitive test details by module name.
	 * 
	 * @param moduleName
	 * @return List<PrimitiveTestDTO>
	 * 
	 */
	List<PrimitiveTestDTO> findAllByModuleName(String moduleName);

	/**
	 * This method is used to get the primitive test details by testName.
	 * 
	 * @param testName
	 * @return JSONObject
	 */
	JSONObject getJson(String testName);

}
