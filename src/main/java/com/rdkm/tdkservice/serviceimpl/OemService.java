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

import com.rdkm.tdkservice.dto.OemCreateDTO;
import com.rdkm.tdkservice.dto.OemDTO;
import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.exception.DeleteFailedException;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.model.Oem;
import com.rdkm.tdkservice.model.UserGroup;
import com.rdkm.tdkservice.repository.OemRepository;
import com.rdkm.tdkservice.repository.UserGroupRepository;
import com.rdkm.tdkservice.service.IOemService;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.MapperUtils;
import com.rdkm.tdkservice.util.Utils;

/**
 * This class provides the implementation for the OemService interface. It
 * provides methods to perform CRUD operations on OemService entities.
 */

@Service
public class OemService implements IOemService {

	private static final Logger LOGGER = LoggerFactory.getLogger(IOemService.class);

	@Autowired
	OemRepository oemRepository;

	@Autowired
	UserGroupRepository userGroupRepository;

	@Autowired
	CommonService commonService;

	/**
	 * This method is used to create a new oem.
	 * 
	 * @param oemDTO This is the request object containing the details of the oem to
	 *               be created.
	 * @return boolean This returns true if the oem was created successfully, false
	 *         otherwise.
	 */
	@Override
	public boolean createOem(OemCreateDTO oemDTO) {
		LOGGER.info("Going to create oemDTO with name: " + oemDTO.toString());

		Category category = commonService.validateCategory(oemDTO.getOemCategory());
		
		if (oemRepository.existsByNameAndCategory(oemDTO.getOemName(), category)) {
			LOGGER.info("oem already exists with the same name: " + oemDTO.getOemName());
			throw new ResourceAlreadyExistsException(Constants.OEM_NAME, oemDTO.getOemName());
		}
		Oem oem = new Oem();
		oem.setName(oemDTO.getOemName());
		UserGroup userGroup = userGroupRepository.findByName(oemDTO.getOemUserGroup());
		oem.setUserGroup(userGroup);
		oem.setCategory(category);

		try {
			oem = oemRepository.save(oem);
		} catch (Exception e) {
			LOGGER.error("Error occurred while creating oem", e);
			return false;
		}

		return oem != null && oem.getId() != null;

	}

	/**
	 * This method is used to retrieve all oems.
	 * 
	 * @return List<oemRequest> This returns a list of all oems.
	 */
	@Override
	public List<OemDTO> getAllOem() {
		LOGGER.info("Going to get all oems");
		List<Oem> oems = oemRepository.findAll();
		if (oems.isEmpty() || oems == null) {
			return null;
		}
		return oems.stream().map(MapperUtils::convertToOemDTO).collect(Collectors.toList());

	}

	/**
	 * This method is used to delete a oem by its id.
	 * 
	 * @param id This is the id of the oem to be deleted.
	 */
	@Override
	public void deleteOem(UUID id) {
		if (!oemRepository.existsById(id)) {
			LOGGER.info("No oem found with id: " + id);
			throw new ResourceNotFoundException(Constants.OEM_ID, id.toString());
		}
		try {
			oemRepository.deleteById(id);
		} catch (DataIntegrityViolationException e) {
			LOGGER.error("Error occurred while deleting oem with id: " + id, e);
			throw new DeleteFailedException();
		}
	}

	/**
	 * This method is used to find a oem by its id.
	 * 
	 * @param id This is the id of the oem to be found.
	 * @return oem This returns the found oem.
	 */
	@Override
	public OemDTO findById(UUID id) {
		LOGGER.info("Going to find oem with id: " + id);
		Oem oem = oemRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.OEM_ID, id.toString()));
		return MapperUtils.convertToOemDTO(oem);

	}

	/**
	 * This method is used to update a oem.
	 * 
	 * @param oemUpdateDTO This is the request object containing the updated details
	 *                     of the oem.
	 * @param id           This is the id of the oem to be updated.
	 * @return OemEMRequest This returns the updated oem.
	 */
	@Override
	public OemDTO updateOem(OemDTO oemUpdateDTO) {
		LOGGER.info("Going to update oem with id: " + oemUpdateDTO.getOemId());
		Category categoryValue = Category.getCategory(oemUpdateDTO.getOemCategory());

		Oem oem = oemRepository.findById(oemUpdateDTO.getOemId())
				.orElseThrow(() -> new ResourceNotFoundException(Constants.OEM_ID, oemUpdateDTO.getOemId().toString()));

		if (!Utils.isEmpty(oemUpdateDTO.getOemName())) {
			Oem newOem = oemRepository.findByNameAndCategory(oemUpdateDTO.getOemName(), categoryValue);
			if (newOem != null && oem.getName().equalsIgnoreCase(oemUpdateDTO.getOemName())) {
				newOem.setName(oemUpdateDTO.getOemName());
			} else {
				if (oemRepository.existsByNameAndCategory(oemUpdateDTO.getOemName(), categoryValue)) {
					LOGGER.info("Soc already exists with the same name: " + oemUpdateDTO.getOemName());
					throw new ResourceAlreadyExistsException(Constants.OEM_NAME, oemUpdateDTO.getOemName());
				} else {
					oem.setName(oemUpdateDTO.getOemName());
				}
			}
		}
		if (!Utils.isEmpty(oemUpdateDTO.getOemCategory())) {
			oem.setCategory(Category.getCategory(oemUpdateDTO.getOemCategory()));
		}

		try {
			oem = oemRepository.save(oem);
		} catch (Exception e) {
			LOGGER.error("Error occurred while updating oem with id: " + oemUpdateDTO.getOemId(), e);
		}
		LOGGER.info("oem updated successfully with id: " + oemUpdateDTO.getOemId());

		return MapperUtils.convertToOemUpdateDTO(oem);

	}

	/**
	 * This method is used to retrieve all oem DTO by category.
	 * 
	 * @param category This is the category of the oem to be retrieved.
	 * @return List<OemRequest> This returns a list of oem.
	 */

	@Override
	public List<OemDTO> getOemsByCategory(String category) {
		LOGGER.info("Going to get all oems by category: " + category);
		List<Oem> oems = oemRepository.findByCategory(Category.getCategory(category));
		if (oems.isEmpty() || oems == null) {
			return null;
		}
		return oems.stream().map(MapperUtils::convertToOemDTO).collect(Collectors.toList());
	}

	/**
	 * This method is used to retrieve all oem names list by category.
	 * 
	 * @param category This is the category of the oems to be retrieved.
	 * @return List<String> This returns a list of Oems.
	 */

	@Override
	public List<String> getOemListByCategory(String category) {
		LOGGER.info("Going to get all oems by category: " + category);
		List<Oem> oems = oemRepository.findByCategory(Category.getCategory(category));
		if (oems.isEmpty() || oems == null) {
			return null;
		}
		return oems.stream().map(Oem::getName).collect(Collectors.toList());
	}

}
