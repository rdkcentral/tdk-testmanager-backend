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

import com.rdkm.tdkservice.dto.SocCreateDTO;
import com.rdkm.tdkservice.dto.SocDTO;
import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.exception.DeleteFailedException;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.model.Soc;
import com.rdkm.tdkservice.model.UserGroup;
import com.rdkm.tdkservice.repository.SocRepository;
import com.rdkm.tdkservice.repository.UserGroupRepository;
import com.rdkm.tdkservice.service.ISocService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.MapperUtils;
import com.rdkm.tdkservice.util.Utils;

/**
 * This class represents the service implementation for managing SOC vendors. It
 * provides methods for creating a new SOC vendor, retrieving all SOC vendors,
 * deleting a SOC vendor, and updating a SOC vendor.
 */
@Service
public class SocService implements ISocService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SocService.class);
	@Autowired
	SocRepository socRepository;

	@Autowired
	UserGroupRepository userGroupRepository;

	/**
	 * Creates a new SOC vendor.
	 *
	 * @param socDTO The request object containing the details of the SOC vendor.
	 * @return true if the SOC is created successfully, false otherwise.
	 * @throws ResourceAlreadyExistsException if a SOC with the same name already
	 *                                        exists.
	 */
	@Override
	public boolean createSoc(SocCreateDTO socDTO) {
		LOGGER.info("socDTO: " + socDTO);
		Category categoryValue = Category.getCategory(socDTO.getSocCategory());
		if (socRepository.existsByNameAndCategory(socDTO.getSocName(), categoryValue)) {
			LOGGER.info("Soc already exists with the same name: " + socDTO.getSocName());
			throw new ResourceAlreadyExistsException(Constants.SOC_NAME, socDTO.getSocName());
		}
		Soc soc = new Soc();
		soc.setName(socDTO.getSocName());
		soc.setCategory(Category.getCategory(socDTO.getSocCategory()));
		try {
			soc = socRepository.save(soc);
		} catch (Exception e) {
			LOGGER.error("Error while saving Soc: " + e.getMessage());
			return false;
		}

		return soc != null && soc.getId() != null;

	}

	/**
	 * Retrieves all SOC's.
	 *
	 * @return a list of all SOC's.
	 */
	@Override
	public List<SocDTO> findAll() {
		LOGGER.info("Going to fetch all the socs");
		List<Soc> socVendors = socRepository.findAll();
		if (socVendors == null || socVendors.isEmpty()) {
			LOGGER.info("No soc found");
			return null;
		}
		return socVendors.stream().map(MapperUtils::convertToSocDTO).collect(Collectors.toList());

	}

	/**
	 * Deletes a SOC vendor by ID.
	 *
	 * @param id the ID of the SOC to delete
	 * @throws ResourceNotFoundException if the SOC with the provided ID does not
	 *                                   exist.
	 */
	@Override
	public void deleteSoc(UUID id) {
		if (!socRepository.existsById(id)) {
			LOGGER.info("Soc  with id: " + id + " does not exist");
			throw new ResourceNotFoundException(Constants.SOC_ID, id.toString());
		}
		try {
			socRepository.deleteById(id);
		} catch (DataIntegrityViolationException e) {
			LOGGER.error("Error while deleting Soc: " + e.getMessage());
			throw new DeleteFailedException();
		}
	}

	/**
	 * Retrieves a SOC by its ID.
	 *
	 * @param id the ID of the SOC to retrieve
	 * @return the SOC vendor if found, or a NOT_FOUND status with an error message
	 *         if not found
	 */
	@Override
	public SocDTO findById(UUID id) {
		LOGGER.info("Going to fetch SocVendor with id: " + id);
		Soc soc = socRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.SOC_ID, id.toString()));
		return MapperUtils.convertToSocDTO(soc);

	}

	/**
	 * Updates a SOC vendor based on the provided SocVendorUpdateDTO.
	 *
	 * @param socUpdateDTO The DTO containing the updated details of the SOC vendor.
	 * @param id           The ID of the SOC to be updated.
	 * @return SocUpdateDTO The DTO representation of the updated SOC vendor object.
	 * @throws ResourceNotFoundException If the SOC vendor with the provided ID does
	 *                                   not exist.
	 */
	@Override
	public SocDTO updateSoc(SocDTO socUpdateDTO) {
		LOGGER.info("Going to update SocVendor with id: " + socUpdateDTO.getSocId().toString());
		Category categoryValue = Category.getCategory(socUpdateDTO.getSocCategory());
		Soc soc = socRepository.findById(socUpdateDTO.getSocId())
				.orElseThrow(() -> new ResourceNotFoundException(Constants.SOC_ID, socUpdateDTO.getSocId().toString()));

		if (!Utils.isEmpty(socUpdateDTO.getSocName())) {
			Soc newSoc = socRepository.findByNameAndCategory(socUpdateDTO.getSocName(), categoryValue);
			if (newSoc != null && soc.getName().equalsIgnoreCase(socUpdateDTO.getSocName())) {
				soc.setName(socUpdateDTO.getSocName());
			} else {
				if (socRepository.existsByNameAndCategory(socUpdateDTO.getSocName(), categoryValue)) {
					LOGGER.info("Soc already exists with the same name: " + socUpdateDTO.getSocName());
					throw new ResourceAlreadyExistsException(Constants.SOC_NAME, socUpdateDTO.getSocName());
				} else {
					soc.setName(socUpdateDTO.getSocName());
				}
			}
		}

		if (socUpdateDTO.getSocCategory() != null) {
			soc.setCategory(categoryValue);
		}
		try {
			soc = socRepository.save(soc);
		} catch (Exception e) {
			LOGGER.error("Error while saving Soc: " + e.getMessage());
			throw new RuntimeException(
					"Error occurred while updating Soc with id: " + socUpdateDTO.getSocId().toString(), e);
		}

		return MapperUtils.convertToSocUpdateDTO(soc);

	}

	/**
	 * Retrieves all SOC DTO by category.
	 *
	 * @param category the category of the SOC to retrieve
	 * @return a list of all SOC with the specified category
	 */

	@Override
	public List<SocDTO> getSOCsByCategory(String category) {
		LOGGER.info("Going to fetch soc  by category: " + category);
		Category categoryName = Category.getCategory(category);
		if (null == categoryName) {
			throw new ResourceNotFoundException(Constants.CATEGORY, category);
		}
		List<Soc> socVendors = socRepository.findByCategory(categoryName);
		if (socVendors == null || socVendors.isEmpty()) {
			LOGGER.info("No socs found with category: " + category);
			return null;
		}
		return socVendors.stream().map(MapperUtils::convertToSocDTO).collect(Collectors.toList());
	}

	/**
	 * Retrieves all SOC names by category.
	 *
	 * @param category the category of the SOC vendors to retrieve
	 * @return a list of all SOC with the specified category
	 */

	@Override
	public List<String> getSOCsListByCategory(String category) {
		LOGGER.info("Going to fetch socs by category: " + category);
		Category categoryName = Category.getCategory(category);
		if (null == categoryName) {
			throw new ResourceNotFoundException(Constants.CATEGORY, category);
		}
		List<Soc> socs = socRepository.findByCategory(categoryName);
		if (socs == null || socs.isEmpty()) {
			LOGGER.info("No soc  found with category: " + category);
			return null;
		}
		return socs.stream().map(Soc::getName).collect(Collectors.toList());
	}
}
