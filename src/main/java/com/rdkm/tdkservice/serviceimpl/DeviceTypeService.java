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
package com.rdkm.tdkservice.serviceimpl;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.rdkm.tdkservice.dto.DeviceTypeCreateDTO;
import com.rdkm.tdkservice.dto.DeviceTypeDTO;
import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.enums.DeviceTypeCategory;
import com.rdkm.tdkservice.exception.DeleteFailedException;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.exception.UserInputException;
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
	public boolean createDeviceType(DeviceTypeCreateDTO deviceTypeDTO, boolean exceptionFlag) {
		LOGGER.info("Going to create DeviceType");
		DeviceType deviceType = new DeviceType();
		Category category = Category.getCategory(deviceTypeDTO.getDeviceTypeCategory());
		boolean existsByNameAndCategoryFlag = deviceTypeRepository
				.existsByNameAndCategory(deviceTypeDTO.getDeviceTypeName(), category);
		if (existsByNameAndCategoryFlag && exceptionFlag) {
			LOGGER.error("Device type already exists with the same name: " + deviceTypeDTO.getDeviceTypeName());
			throw new ResourceAlreadyExistsException(Constants.DEVICE_TYPE, deviceTypeDTO.getDeviceTypeName());
		}
		if (existsByNameAndCategoryFlag && !exceptionFlag) {
			return false;
		}
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

	/**
	 * Downloads all device types by category as a single XML file.
	 * 
	 * @param category The category of the device types to download.
	 * @return String containing XML content of all device types.
	 */
	@Override
	public String downloadAllDeviceTypesXML(String category) {
		LOGGER.info("Downloading all device types for category: {}", category);
		Category categoryEnum = commonService.validateCategory(category);
		List<DeviceType> deviceTypes = deviceTypeRepository.findByCategory(categoryEnum);
		if (deviceTypes.isEmpty()) {
			throw new ResourceNotFoundException(Constants.DEVICE_TYPE, category);
		}
		try {
			Document doc = createDeviceTypesXMLDocument(deviceTypes);
			return convertDocumentToString(doc);
		} catch (Exception e) {
			LOGGER.error("Error generating device types XML for category: " + category, e);
			throw new TDKServiceException("Error generating device types XML for category: " + category);
		}
	}

	/**
	 * Parses an uploaded XML file and creates device types from it (bulk import).
	 * 
	 * @param file The XML file containing device type definitions.
	 * @return boolean true if the device types were created successfully.
	 */
	@Override
	public boolean parseXMLForDeviceType(MultipartFile file) {
		LOGGER.info("Parsing XML file for device type details");
		validateXMLFile(file);
		Document doc;
		try {
			String xmlData = new String(file.getBytes(), StandardCharsets.UTF_8);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			dbFactory.setXIncludeAware(false);
			dbFactory.setExpandEntityReferences(false);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(xmlData));
			doc = dBuilder.parse(is);
		} catch (Exception e) {
			LOGGER.error("Error parsing XML file", e);
			throw new TDKServiceException("Error parsing XML file: " + e.getMessage());
		}

		NodeList nList = doc.getElementsByTagName("deviceType");
		if (nList.getLength() == 0) {
			LOGGER.error("No deviceType elements found in the XML file");
			throw new UserInputException("No deviceType elements found in the XML file.");
		}

		for (int i = 0; i < nList.getLength(); i++) {
			Node nNode = nList.item(i);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				DeviceTypeCreateDTO dto = new DeviceTypeCreateDTO();
				dto.setDeviceTypeName(getNodeTextContent(eElement, "name"));
				dto.setDeviceType(getNodeTextContent(eElement, "type"));
				dto.setDeviceTypeCategory(getNodeTextContent(eElement, "category"));
				createDeviceType(dto, false);
			}
		}
		return true;
	}

	/**
	 * Creates an XML document containing all device types.
	 */
	private Document createDeviceTypesXMLDocument(List<DeviceType> deviceTypes) throws ParserConfigurationException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.newDocument();

		Element rootElement = doc.createElement("deviceTypes");
		doc.appendChild(rootElement);

		for (DeviceType deviceType : deviceTypes) {
			Element dtElement = doc.createElement("deviceType");
			rootElement.appendChild(dtElement);

			Element nameEl = doc.createElement("name");
			nameEl.setTextContent(deviceType.getName());
			dtElement.appendChild(nameEl);

			Element typeEl = doc.createElement("type");
			typeEl.setTextContent(deviceType.getType() != null ? deviceType.getType().getName() : "");
			dtElement.appendChild(typeEl);

			Element categoryEl = doc.createElement("category");
			categoryEl.setTextContent(deviceType.getCategory() != null ? deviceType.getCategory().getName() : "");
			dtElement.appendChild(categoryEl);
		}

		return doc;
	}

	/**
	 * Converts a Document to its String representation.
	 */
	private String convertDocumentToString(Document doc) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		transformer.transform(source, result);
		return writer.toString();
	}

	/**
	 * Gets the text content of a node in an XML element.
	 */
	private String getNodeTextContent(Element eElement, String tagName) {
		Node node = eElement.getElementsByTagName(tagName).item(0);
		return node != null ? node.getTextContent() : null;
	}

	/**
	 * Validates the uploaded XML file.
	 */
	private void validateXMLFile(MultipartFile file) {
		String fileName = file.getOriginalFilename();
		if (fileName == null || !fileName.endsWith(Constants.XML_EXTENSION)) {
			LOGGER.error("The uploaded file must have a .xml extension {}", fileName);
			throw new UserInputException("The uploaded file must be a .xml file.");
		}
		if (file.isEmpty()) {
			LOGGER.error("The uploaded file is empty");
			throw new UserInputException("The uploaded file is empty.");
		}
	}

}
