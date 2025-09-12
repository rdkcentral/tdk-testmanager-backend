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

import static com.rdkm.tdkservice.util.Constants.DEVICE_FILE_EXTENSION_ZIP;
import static com.rdkm.tdkservice.util.Constants.DEVICE_XML_FILE_EXTENSION;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.rdkm.tdkservice.dto.DeviceCreateDTO;
import com.rdkm.tdkservice.dto.DeviceResponseDTO;
import com.rdkm.tdkservice.dto.DeviceStatusResponseDTO;
import com.rdkm.tdkservice.dto.DeviceUpdateDTO;
import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.enums.DeviceStatus;
import com.rdkm.tdkservice.exception.DeleteFailedException;
import com.rdkm.tdkservice.exception.MandatoryFieldException;
import com.rdkm.tdkservice.exception.ResourceAlreadyExistsException;
import com.rdkm.tdkservice.exception.ResourceNotFoundException;
import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.exception.UserInputException;
import com.rdkm.tdkservice.model.Device;
import com.rdkm.tdkservice.model.DeviceType;
import com.rdkm.tdkservice.model.Oem;
import com.rdkm.tdkservice.model.Soc;
import com.rdkm.tdkservice.model.UserGroup;
import com.rdkm.tdkservice.repository.DeviceRepositroy;
import com.rdkm.tdkservice.repository.DeviceTypeRepository;
import com.rdkm.tdkservice.repository.OemRepository;
import com.rdkm.tdkservice.repository.SocRepository;
import com.rdkm.tdkservice.repository.UserGroupRepository;
import com.rdkm.tdkservice.service.IDeviceService;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.service.utilservices.ScriptExecutorService;
import com.rdkm.tdkservice.util.Constants;
import com.rdkm.tdkservice.util.MapperUtils;
import com.rdkm.tdkservice.util.Utils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@Service
public class DeviceService implements IDeviceService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeviceService.class);

	@Autowired
	private DeviceRepositroy deviceRepository;

	@Autowired
	private DeviceTypeRepository deviceTypeRepository;

	@Autowired
	private OemRepository oemRepository;

	@Autowired
	private SocRepository socRepository;

	@Autowired
	private UserGroupRepository userGroupRepository;

	@Autowired
	CommonService commonService;

	@Autowired
	DeviceStatusService deviceStatusService;

	@Autowired
	private ScriptExecutorService scriptExecutorService;

	/**
	 * This method is used to create a new device. It receives a POST request at the
	 * "/createDevice" endpoint with a DeviceDTO object in the request body. The
	 * DeviceDTO object should contain the necessary information for creating a new
	 * device.
	 *
	 * @param deviceCreateDTO The request object containing the details of the
	 *                        device.
	 * @return A ResponseEntity containing the created device if successful, or an
	 *         error message if unsuccessful.
	 */
	public boolean createDevice(DeviceCreateDTO deviceCreateDTO) {
		LOGGER.info("Going to create Device");
		// name,Ip and mac are unique fields
		if (deviceRepository.existsByName(deviceCreateDTO.getDeviceName())) {
			LOGGER.info("Device with the same DeviceName already exists");
			throw new ResourceAlreadyExistsException("DeviceName: ", deviceCreateDTO.getDeviceName());
		}
		if (deviceRepository.existsByIp(deviceCreateDTO.getDeviceIp())) {
			LOGGER.info("Device with the same deviceIp already exists");
			throw new ResourceAlreadyExistsException("DeviceIp: ", deviceCreateDTO.getDeviceIp());
		}
		if (deviceRepository.existsByMacId(deviceCreateDTO.getMacId())) {
			LOGGER.info("Device with the same macid already exists");
			throw new ResourceAlreadyExistsException("MacId: ", deviceCreateDTO.getMacId());
		}

		Device device = MapperUtils.populateDeviceDTO(deviceCreateDTO);
		// call setPropetietsCreateDTO meeethod here
		setDevicePropertiesFromCreateDTO(device, deviceCreateDTO);
		// save device return true if save success otherwise return false
		Device savedDevice = deviceRepository.save(device);

		// save device return true if save success otherwise return false
		return savedDevice != null;

	}

	/**
	 * This method is used to update an existing device. It receives a POST request
	 * at the "/updateDevice" endpoint with a DeviceUpdateDTO object in the request
	 * body. The DeviceUpdateDTO object should contain the necessary information for
	 * updating an existing device.
	 *
	 * @param deviceUpdateDTO The request object containing the details of the
	 *                        device to be updated.
	 * @return A ResponseEntity containing the updated device if successful, or an
	 *         error message if unsuccessful.
	 */
	@Override
	public boolean updateDevice(DeviceUpdateDTO deviceUpdateDTO) {
		LOGGER.info("Going to update Device with id: " + deviceUpdateDTO.getId());
		Device device = deviceRepository.findById(deviceUpdateDTO.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Device Id", deviceUpdateDTO.getId().toString()));
		if (!Utils.isEmpty(deviceUpdateDTO.getDeviceIp())) {
			if ((deviceRepository.existsByIp(deviceUpdateDTO.getDeviceIp()))
					&& !(deviceUpdateDTO.getDeviceIp().equals(device.getIp()))) {
				LOGGER.info("Device with the same deviceIp already exists");
				throw new ResourceAlreadyExistsException("deviceIp: ", deviceUpdateDTO.getDeviceIp());
			} else {
				device.setIp(deviceUpdateDTO.getDeviceIp());
			}
		}

		if (!Utils.isEmpty(deviceUpdateDTO.getDeviceName())) {
			Device newDeviceType = deviceRepository.findByName(deviceUpdateDTO.getDeviceName());
			if (newDeviceType != null && deviceUpdateDTO.getDeviceName().equalsIgnoreCase(device.getName())) {
				device.setName(deviceUpdateDTO.getDeviceName());
			} else {
				if (deviceRepository.existsByName(deviceUpdateDTO.getDeviceName())) {
					LOGGER.info("Device Type already exists with the same name: " + deviceUpdateDTO.getDeviceName());
					throw new ResourceAlreadyExistsException("DeviceName: ", deviceUpdateDTO.getDeviceName());
				} else {
					device.setName(deviceUpdateDTO.getDeviceName());
				}
			}
		}
		if (!Utils.isEmpty(deviceUpdateDTO.getMacId())) {
			if ((deviceRepository.existsByMacId(deviceUpdateDTO.getMacId()))
					&& !(deviceUpdateDTO.getMacId().equals(device.getMacId()))) {
				LOGGER.info("Device with the same macid already exists");
				throw new ResourceAlreadyExistsException("MacId: ", deviceUpdateDTO.getMacId());
			} else {
				device.setMacId(deviceUpdateDTO.getMacId());
			}
		}
		MapperUtils.updateDeviceProperties(device, deviceUpdateDTO);
		setDevicePropertiesFromUpdateDTO(device, deviceUpdateDTO);
		try {
			deviceRepository.save(device);
		} catch (Exception e) {
			LOGGER.error("Error occurred while updating Device with id: " + deviceUpdateDTO.getId(), e);
			throw new TDKServiceException("Error occurred while updating Device with id: " + deviceUpdateDTO.getId());
		}
		return true;
	}

	/**
	 * This method is used to find all devices. It receives a GET request at the
	 * "/findAll" endpoint. It then returns a list of all devices.
	 *
	 * @return A List containing all devices.
	 */
	@Override
	public List<DeviceResponseDTO> getAllDeviceDetails() {
		LOGGER.info("Going to fetch all devices");
		List<Device> devices = null;
		try {
			devices = deviceRepository.findAll();
		} catch (Exception e) {
			LOGGER.error("Error occurred while fetching all devices", e);
		}
		if (devices.isEmpty()) {
			return Collections.emptyList();
		}
		return devices.stream().map(this::convertToDeviceDTO).collect(Collectors.toList());
	}

	/**
	 * This method is used to find a device by its ID. It receives a GET request at
	 * the "/findDeviceById/{id}" endpoint with the ID of the device to be found. It
	 * then returns the device with the given ID.
	 *
	 * @param id The ID of the device to be found.
	 * @return A Device object containing the details of the device with the given
	 *         ID.
	 */
	@Override
	public DeviceResponseDTO findDeviceById(UUID id) {
		LOGGER.info("Executing find Device by id method with id: " + id);
		Device device = deviceRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Device Id", id.toString()));
		DeviceResponseDTO deviceDTO = null;
		try {
			deviceDTO = this.convertToDeviceDTO(device);
		} catch (Exception e) {
			LOGGER.error("Error occurred while fetching Device with id: " + id, e);
		}
		return deviceDTO;
	}

	/**
	 * This method is used to delete a device by its ID. It receives a DELETE
	 * request at the "/delete/{id}" endpoint with the ID of the device to be
	 * deleted. It then deletes the device with the given ID.
	 *
	 * @param id The ID of the device to be deleted.
	 * @return A ResponseEntity containing a success message if the device is
	 *         deleted successfully, or an error message if the device is not found.
	 */
	@Override
	public boolean deleteDeviceById(UUID id) {
		LOGGER.info("Going to delete Device with id: " + id);
		try {
			// Then, delete the device
			deviceRepository.deleteById(id);
			return true;
		} catch (DataIntegrityViolationException e) {
			LOGGER.error("Error occurred while deleting Device with id: " + id, e);
			throw new DeleteFailedException();
		}

	}

	/**
	 * This method is used to retrieve all devices by category.
	 *
	 * @param category The category of the devices to retrieve.
	 * @return A List containing all devices with the given category.
	 */
	@Override
	public List<DeviceResponseDTO> getAllDeviceDetailsByCategory(String category) {
		LOGGER.info("Starting getAllDeviceDetailsByCategory method with category: {}", category);
		List<Device> devices = new ArrayList<>();
		Category categoryEnum = commonService.validateCategory(category);
		try {
			devices = deviceRepository.findByCategory(categoryEnum);
		} catch (Exception e) {
			LOGGER.error("Error occurred while fetching devices by category: " + category, e);
		}
		if (devices != null && !devices.isEmpty()) {
			LOGGER.info("Found {} devices", devices.size());
			return devices.stream().map(this::convertToDeviceDTO).collect(Collectors.toList());
		} else {
			LOGGER.warn("No devices found for category: " + category);
			return Collections.emptyList();
		}
	}

	/**
	 * This method is used to convert a Device object to a DeviceDTO object.
	 *
	 * @param device The Device object to convert.
	 * @return A DeviceDTO object containing the details of the Device object.
	 */
	private DeviceResponseDTO convertToDeviceDTO(Device device) {
		LOGGER.trace("Converting Device to DeviceDTO");
		DeviceResponseDTO deviceDTO = MapperUtils.convertToDeviceDTO(device);
		return deviceDTO;
	}

	/**
	 * This method is used to parse an XML file for device details.
	 *
	 * @param file The XML file to parse.
	 */
	@Override
	public boolean parseXMLForDevice(MultipartFile file) {
		LOGGER.info("Parsing XML file for device details");
		validateFile(file);
		Document doc = null;
		try {
			doc = getDocumentFromXMLFile(file);

		} catch (ParserConfigurationException | SAXException | IOException e) {
			LOGGER.error("Error parsing XML file", e);
			throw new TDKServiceException("Error parsing XML file" + e.getMessage());
		}

		if (doc == null) {
			LOGGER.error("Document is null");
			return false;
		}

		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();

		NodeList nList = doc.getElementsByTagName("device");

		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			LOGGER.info("\nCurrent Element :" + nNode.getNodeName());
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				DeviceCreateDTO deviceDTO = createDeviceDTOFromElement(eElement);
				validateAndCreateDevice(deviceDTO, validator);
			}
		}
		return true;

	}

	/**
	 * This method is used to create a DeviceCreateDTO object from an XML element.
	 *
	 * @param eElement The XML element to create the DeviceCreateDTO object from.
	 * @return A DeviceCreateDTO object containing the details of the XML element.
	 */
	private DeviceCreateDTO createDeviceDTOFromElement(Element eElement) {
		LOGGER.info("Creating DeviceDTO from Element");
		DeviceCreateDTO deviceDTO = new DeviceCreateDTO();

		// Set the category of the device from the XML element
		deviceDTO.setCategory(getNodeTextContent(eElement, Constants.XML_TAG_CATEGORY));

		// If the category is RDKB, set the STB name and IP from the gateway name and IP
		if (deviceDTO.getCategory().equalsIgnoreCase(Category.RDKB.getName())) {
			deviceDTO.setDeviceName(getNodeTextContent(eElement, Constants.XML_TAG_GATEWAY_NAME));
			deviceDTO.setDeviceIp(getNodeTextContent(eElement, Constants.XML_TAG_GATEWAY_IP));
		}
		// If the category is RDKC, set the STB name and IP from the camera name and IP
		else if (deviceDTO.getCategory().equalsIgnoreCase(Category.RDKC.getName())) {
			deviceDTO.setDeviceName(getNodeTextContent(eElement, Constants.XML_TAG_CAMERA_NAME));
			deviceDTO.setDeviceIp(getNodeTextContent(eElement, Constants.XML_TAG_CAMERA_IP));
		}

		// Set the device name, IP, MAC ID, device type name, oem name, SoC
		// name,
		// thunder enabled status, thunder port, recorder ID, and gateway device name
		// from the XML element
		deviceDTO.setDeviceName(getNodeTextContent(eElement, Constants.XML_TAG_DEVICE_NAME));
		deviceDTO.setDeviceIp(getNodeTextContent(eElement, Constants.XML_TAG_DEVICE_IP));
		deviceDTO.setMacId(getNodeTextContent(eElement, Constants.XML_TAG_MAC_ADDR));
		deviceDTO.setDeviceTypeName(getNodeTextContent(eElement, Constants.XML_TAG_Device_TYPE));
		deviceDTO.setOemName(getNodeTextContent(eElement, Constants.XML_TAG_OEM));
		deviceDTO.setSocName(getNodeTextContent(eElement, Constants.XML_TAG_SOC));
		deviceDTO.setThunderEnabled(
				Boolean.parseBoolean(getNodeTextContent(eElement, Constants.XML_TAG_IS_THUNDER_ENABLED)));
		deviceDTO.setThunderPort(getNodeTextContent(eElement, Constants.XML_TAG_THUNDER_PORT));

		return deviceDTO;
	}

	/**
	 * This method is used to get the text content of a node in an XML element.
	 *
	 * @param eElement The XML element containing the node.
	 * @param tagName  The name of the node to get the text content of.
	 * @return A String containing the text content of the node.
	 */
	private String getNodeTextContent(Element eElement, String tagName) {
		Node node = eElement.getElementsByTagName(tagName).item(0);
		return node != null ? node.getTextContent() : null;
	}

	/**
	 * This method is used to validate the device details and create a new device.
	 *
	 * @param deviceDTO The DeviceDTO object containing the details of the device.
	 * @param validator The Validator object to validate the device details.
	 */
	private void validateAndCreateDevice(DeviceCreateDTO deviceDTO, Validator validator) {
		Set<ConstraintViolation<DeviceCreateDTO>> violations = validator.validate(deviceDTO);
		if (!violations.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (ConstraintViolation<DeviceCreateDTO> violation : violations) {
				sb.append(violation.getMessage()).append("\n");
			}
			throw new IllegalArgumentException("Validation errors: \n" + sb.toString());
		}

		createDevice(deviceDTO);
	}

	/**
	 * This method is used to download the device details in XML format.
	 *
	 * @param name The name of the device to download the details for.
	 * @return A String containing the device details in XML format.
	 */
	public String downloadDeviceXML(String name) {
		Device device = deviceRepository.findByName(name);
		try {
			Document doc = createDeviceXMLDocument(device);
			return convertDocumentToString(doc);
		} catch (Exception e) {
			throw new RuntimeException("Error generating XML", e);
		}
	}

	/**
	 * This method is used to create an XML document from a Device object.
	 *
	 * @param device The Device object to create the XML document from.
	 * @return A Document object containing the XML document.
	 * @throws ParserConfigurationException If a DocumentBuilder cannot be created.
	 */
	private void setDevicePropertiesFromCreateDTO(Device device, DeviceCreateDTO deviceDTO) {
		// Set common properties

		if (deviceRepository.existsByIp(deviceDTO.getDeviceIp())) {
			LOGGER.info("Device with the same deviceIp already exists");
			throw new ResourceAlreadyExistsException("DeviceIp: ", deviceDTO.getDeviceIp());
		}

		// Check if a device with the same deviceName already exists
		if (deviceRepository.existsByName(deviceDTO.getDeviceName())) {
			LOGGER.info("Device with the same DeviceName already exists");
			throw new ResourceAlreadyExistsException("DeviceName: ", deviceDTO.getDeviceName());
		}

		// Check if a device with the same macid already exists
		if (deviceRepository.existsByMacId(deviceDTO.getMacId())) {
			LOGGER.info("Device with the same macid already exists");
			throw new ResourceAlreadyExistsException("MacId: ", deviceDTO.getMacId());
		}

		Category category = Category.valueOf(deviceDTO.getCategory().toUpperCase());
		if (category != null) {
			device.setCategory(category);
		} else {
			throw new ResourceNotFoundException("Category not found", deviceDTO.getCategory());
		}

		// Set DeviceType
		DeviceType deviceType = deviceTypeRepository.findByNameAndCategory(deviceDTO.getDeviceTypeName(), category);
		if (deviceType != null) {
			device.setDeviceType(deviceType);
		} else {
			throw new ResourceNotFoundException("DeviceType: ", deviceDTO.getDeviceTypeName());
		}

		// Set OEM
		Oem oem = oemRepository.findByNameAndCategory(deviceDTO.getOemName(), category);
		if (oem != null) {
			device.setOem(oem);
		}
		// Set Soc
		Soc soc = socRepository.findByNameAndCategory(deviceDTO.getSocName(), category);
		if (soc != null) {
			device.setSoc(soc);
		}

		// Set UserGroup
		UserGroup userGroup = userGroupRepository.findByName(deviceDTO.getUserGroupName());
		if (userGroup != null) {
			device.setUserGroup(userGroup);
		}

		// Check if thunder is enabled and port is set
		if (device.isThunderEnabled() && (device.getThunderPort() == null || device.getThunderPort().isEmpty())) {
			LOGGER.info("ThunderPort should not be null or empty");
			throw new MandatoryFieldException(" ThunderPort should not be null or empty");
		}

	}

	/**
	 * This method is used to set the properties of a Device object from a
	 * DeviceUpdateDTO object.
	 *
	 * @param device          The Device object to set the properties for.
	 * @param deviceUpdateDTO The DeviceUpdateDTO object containing the properties
	 *                        to set.
	 */
	private void setDevicePropertiesFromUpdateDTO(Device device, DeviceUpdateDTO deviceUpdateDTO) {

		Category categoryValue = Category.getCategory(deviceUpdateDTO.getCategory());
		if (!Utils.isEmpty(deviceUpdateDTO.getDeviceTypeName())) {
			DeviceType deviceType = deviceTypeRepository.findByNameAndCategory(deviceUpdateDTO.getDeviceTypeName(),
					categoryValue);
			if (deviceType != null) {
				device.setDeviceType(deviceType);
			} else {
				throw new ResourceNotFoundException("DeviceType: ", deviceUpdateDTO.getDeviceTypeName());
			}
		}

		if (!Utils.isEmpty(deviceUpdateDTO.getOemName())) {
			Oem oem = oemRepository.findByNameAndCategory(deviceUpdateDTO.getOemName(), categoryValue);
			if (oem != null) {
				device.setOem(oem);
			}
		}

		if (!Utils.isEmpty(deviceUpdateDTO.getSocName())) {
			Soc soc = socRepository.findByNameAndCategory(deviceUpdateDTO.getSocName(), categoryValue);
			if (soc != null) {
				device.setSoc(soc);
			}
		}
		UserGroup userGroup = userGroupRepository.findByName(deviceUpdateDTO.getUserGroupName());
		if (null != userGroup)
			device.setUserGroup(userGroup);
		// Check if thunder is enabled and port is set
		if (device.isThunderEnabled()) {
			if (deviceUpdateDTO.getThunderPort() != null && !deviceUpdateDTO.getThunderPort().isEmpty()) {
				device.setThunderPort(deviceUpdateDTO.getThunderPort());
			} else if (device.getThunderPort() == null || device.getThunderPort().isEmpty()) {
				LOGGER.info("ThunderPort should not be null or empty");
				throw new MandatoryFieldException(" ThunderPort should not be null or empty");
			}
		}

	}

	/**
	 * This method is used to get a Document object from an XML file.
	 *
	 * @param file The XML file to get the Document object from.
	 * @return A Document object containing the contents of the XML file.
	 * @throws ParserConfigurationException If a DocumentBuilder cannot be created.
	 * @throws SAXException                 If an error occurs while parsing the XML
	 *                                      file.
	 * @throws IOException                  If an error occurs while reading the XML
	 *                                      file.
	 */
	private Document getDocumentFromXMLFile(MultipartFile file)
			throws ParserConfigurationException, SAXException, IOException {
		String xmlData = new String(file.getBytes(), StandardCharsets.UTF_8);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xmlData));
		return dBuilder.parse(is);
	}

	/**
	 * This method is used to create an XML document for a device.
	 *
	 * @param device The device for which to create the XML document.
	 * @return A Document object containing the device details in XML format.
	 */
	private Document createDeviceXMLDocument(Device device) throws ParserConfigurationException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.newDocument();

		Element rootElement = doc.createElement(Constants.XML_TAG_ROOT);
		doc.appendChild(rootElement);

		Element deviceElement = doc.createElement(Constants.XML_TAG_DEVICE);
		rootElement.appendChild(deviceElement);

		// Add comments to the XML document
		String nameTag = selectTag(device, Constants.XML_TAG_GATEWAY_NAME, Constants.XML_TAG_CAMERA_NAME,
				Constants.XML_TAG_DEVICE_NAME);
		appendElement(deviceElement, nameTag, device.getName(), "  Unique name for the STB");

		String ipTag = selectTag(device, Constants.XML_TAG_GATEWAY_IP, Constants.XML_TAG_CAMERA_IP,
				Constants.XML_TAG_DEVICE_IP);
		appendElement(deviceElement, ipTag, device.getIp(), " Unique IP for the STB");

		appendElement(deviceElement, Constants.XML_TAG_MAC_ADDR, device.getMacId(), " Mac Addr for the STB");
		appendElement(deviceElement, Constants.XML_TAG_IS_THUNDER_ENABLED, String.valueOf(device.isThunderEnabled()),
				" Is Thunder enabled for STB");
		appendElement(deviceElement, Constants.XML_TAG_THUNDER_PORT, device.getThunderPort(),
				" Thunder port for thunder devices");
		appendElement(deviceElement, Constants.XML_TAG_Device_TYPE, device.getDeviceType().getName(),
				" device type for STB");
		if (device.getOem() != null && device.getOem().getName() != null) {
			appendElement(deviceElement, Constants.XML_TAG_OEM, device.getOem().getName(), " oem for the STB");
		} else {
			appendElement(deviceElement, Constants.XML_TAG_OEM, "", " oem for the STB");
		}

		if (device.getSoc() != null && device.getSoc().getName() != null) {
			appendElement(deviceElement, Constants.XML_TAG_SOC, device.getSoc().getName(), " SoC for the STB");
		} else {
			appendElement(deviceElement, Constants.XML_TAG_SOC, "", " SoC for the STB");
		}
		appendElement(deviceElement, Constants.XML_TAG_CATEGORY, device.getCategory().toString(),
				" Category for the STB");
		return doc;
	}

	/**
	 * This method is used to append an element to a parent element.
	 *
	 * @param parent      The parent element to append the new element to.
	 * @param tagName     The name of the new element to append.
	 * @param textContent The text content of the new element.
	 */
	private void appendElement(Element parent, String tagName, String textContent, String commentText) {
		if (textContent != null && !textContent.isEmpty()) {
			Document doc = parent.getOwnerDocument();
			Comment comment = doc.createComment(commentText);
			parent.appendChild(comment);
			Element element = doc.createElement(tagName);
			element.appendChild(doc.createTextNode(textContent));
			parent.appendChild(element);
		}
	}

	/**
	 * This method is used to select a tag based on the device category.
	 *
	 * @param device     The device for which to select the tag.
	 * @param gatewayTag The tag to select if the device is a gateway.
	 * @param cameraTag  The tag to select if the device is a camera.
	 * @param defaultTag The default tag to select if the device is neither a
	 *                   gateway nor a camera.
	 * @return A String containing the selected tag.
	 */
	private String selectTag(Device device, String gatewayTag, String cameraTag, String defaultTag) {
		if (device.getCategory().getName().equalsIgnoreCase(Category.RDKB.getName())) {
			return gatewayTag;
		} else if (device.getCategory().getName().equalsIgnoreCase(Category.RDKC.getName())) {
			return cameraTag;
		} else {
			return defaultTag;
		}
	}

	/**
	 * This method is used to convert a Document object to a String.
	 *
	 * @param doc The Document object to convert.
	 * @return A String containing the contents of the Document object.
	 * @throws TransformerException If an error occurs while transforming the
	 *                              Document object.
	 */
	private String convertDocumentToString(Document doc) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		// Set standalone to yes
		DOMSource source = new DOMSource(doc);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		transformer.transform(source, result);

		return writer.toString();
	}

	/**
	 * This method is used to generate an XML file for a device.
	 *
	 * @param device The device for which to generate the XML file.
	 * @return A String containing the XML content for the device.
	 * @throws Exception If an error occurs while generating the XML content.
	 */
	public String generateXMLForDevice(Device device) throws Exception {
		Document doc = createDeviceXMLDocument(device);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(doc), new StreamResult(writer));
		return writer.getBuffer().toString();
	}

	/**
	 *
	 * @param category This is the category of the Devices to be downloaded.
	 * @return
	 * @throws Exception
	 */
	@Override
	public Path downloadAllDevicesByCategory(String category) {
		LOGGER.info("Downloading all devices for category: {}", category);
		List<Device> devices = deviceRepository.findAllByCategory(Category.valueOf(category.toUpperCase()));
		Path zipFilePath = Paths.get("devices_" + category + DEVICE_FILE_EXTENSION_ZIP);
		try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
			// Add each device to the zip file
			for (Device device : devices) {
				// Create an XML document for the device
				Document doc = createDeviceXMLDocument(device);
				String xmlString = convertDocumentToString(doc);
				// Add the XML content to the zip file
				ZipEntry zipEntry = new ZipEntry(device.getName() + DEVICE_XML_FILE_EXTENSION);
				zipOut.putNextEntry(zipEntry);
				// Write the XML content to the zip file
				byte[] bytes = xmlString.getBytes();
				zipOut.write(bytes, 0, bytes.length);
				zipOut.closeEntry();
			}
		} catch (IOException | TransformerException | ParserConfigurationException e) {
			LOGGER.error("Error downloading devices for category: " + category, e);
			throw new TDKServiceException("Error downloading devices for category: " + category);
		}

		return zipFilePath;
	}

	/**
	 * Validates the uploaded file.
	 *
	 * @param file the uploaded file
	 */
	private void validateFile(MultipartFile file) {
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

	/**
	 * This method is used to get the status of all the devices in the given
	 * category
	 * 
	 * @param category- category of the devices say RDKV, RDKB, RDKC
	 * @return List of device status response DTOs
	 */
	public List<DeviceStatusResponseDTO> getAllDeviceStatus(String category) {
		LOGGER.debug("Fetching device statuses for category: " + category);
		List<DeviceStatusResponseDTO> deviceStatusResponseDTOs = new ArrayList<>();
		try {
			List<Device> devices = deviceRepository.findByCategory(Category.getCategory(category));
			if (devices == null || devices.isEmpty()) {
				LOGGER.warn("No devices found for category: " + category);
				return deviceStatusResponseDTOs;
			}
			for (Device device : devices) {
				DeviceStatusResponseDTO deviceStatusResponseDTO = new DeviceStatusResponseDTO();
				deviceStatusResponseDTO.setDeviceName(device.getName());
				deviceStatusResponseDTO.setStatus(device.getDeviceStatus().toString());
				deviceStatusResponseDTO.setIp(device.getIp());
				deviceStatusResponseDTO.setMacAddress(device.getMacId());
				deviceStatusResponseDTO.setDeviceType(device.getDeviceType().getName());
				deviceStatusResponseDTO.setThunderEnabled(device.isThunderEnabled());
				deviceStatusResponseDTOs.add(deviceStatusResponseDTO);
			}
		} catch (IllegalArgumentException e) {
			LOGGER.error("Invalid category: " + category, e);
			throw new ResourceNotFoundException(Constants.CATEGORY, category);
		} catch (Exception e) {
			LOGGER.error("An unexpected error occurred while fetching device statuses", e);
			throw new TDKServiceException("An unexpected error occurred while fetching device statuses");
		}
		return deviceStatusResponseDTOs;
	}

	/**
	 * This method is used to get the device details for the given device IP
	 * 
	 * @param deviceIp- IP of the device
	 * @return JSON object containing the device details
	 */
	@Override
	public String getDeviceDetails(String deviceIp) {
		JSONObject devicDetailsJSON = new JSONObject();

		if (deviceIp == null) {
			LOGGER.error("Device IP is null");
			throw new UserInputException("Device IP cannot be null");
		}
		Device device = deviceRepository.findByIp(deviceIp);
		try {
			if (device != null) {
				devicDetailsJSON.put("status", "SUCCESS");
				devicDetailsJSON.put("devicename", device.getName());
				devicDetailsJSON.put("mac", device.getMacId());
				devicDetailsJSON.put("category", device.getCategory().getName());
				devicDetailsJSON.put("boxtype", device.getDeviceType().getName());
			} else {
				devicDetailsJSON.put("status", "FAILURE");
				devicDetailsJSON.put("remarks", "No valid device found with provided data");
			}

		} catch (JSONException e) {

		}
		return devicDetailsJSON.toString();
	}

	/**
	 * This method is used to get the thunder device ports for the given device IP
	 * 
	 * @param deviceIp- IP of the device
	 * @return JSON object containing the thunder device ports
	 */
	@Override
	public String getThunderDevicePorts(String deviceIp) {
		JSONObject thunderDevicePortsJSON = new JSONObject();
		try {
			if (deviceIp == null) {
				LOGGER.error("Device IP is null");
				throw new UserInputException("Device IP cannot be null");

			}
			Device device = deviceRepository.findByIp(deviceIp);
			if (device != null) {
				if (device.isThunderEnabled()) {
					if (device.getThunderPort() == null || device.getThunderPort().isEmpty()) {
						thunderDevicePortsJSON.put("Result", "Thunder port is not configured");
					} else {
						thunderDevicePortsJSON.put("thunderPort", device.getThunderPort());

					}
				} else {
					thunderDevicePortsJSON.put("Result", "Device is not thunder enabled");

				}

			} else {
				thunderDevicePortsJSON.put("Result", "Device not found");
			}
		} catch (JSONException e) {
			LOGGER.error("Error occurred while creating JSON object", e);
			throw new TDKServiceException("Error occurred while creating JSON object");

		}
		return thunderDevicePortsJSON.toString();

	}

	/**
	 * This method is used to get the device type for the given device IP
	 * 
	 * @param deviceIp- IP of the device
	 * @return JSON object containing the device type
	 */
	@Override
	public String getDeviceType(String deviceIp) {
		JSONObject deviceTypeJSON = new JSONObject();
		Device device = deviceRepository.findByIp(deviceIp);
		try {
			if (device != null) {
				deviceTypeJSON.put("deviceip", deviceIp);
				deviceTypeJSON.put("status", "SUCCESS");
				deviceTypeJSON.put("boxtype", device.getDeviceType());
			} else {
				deviceTypeJSON.put("status", "FAILURE");
				deviceTypeJSON.put("remarks", "No valid device found with provided data");

			}
		} catch (JSONException e) {
			LOGGER.error("");
		}

		return deviceTypeJSON.toString();
	}

	/**
	 * This method is used to get the device details for the given device IP
	 * 
	 * @param deviceIp- IP of the device
	 * @return JSON object containing the device details
	 */
	public List<DeviceResponseDTO> getDevicesByCategoryAndThunderStatus(String category, Boolean isThunderEnabled) {
		LOGGER.info("Fetching devices for category: {} with Thunder status: {}", category, isThunderEnabled);
		List<Device> devices;
		if (isThunderEnabled != null) {
			devices = deviceRepository.findByCategoryAndIsThunderEnabled(Category.valueOf(category.toUpperCase()),
					isThunderEnabled);
		} else {
			devices = deviceRepository.findByCategory(Category.valueOf(category.toUpperCase()));
		}
		if (devices.isEmpty()) {
			LOGGER.warn("No devices found for category: {} with Thunder status: {}", category, isThunderEnabled);
			return Collections.emptyList();
		}

		devices = getFreeDevices(devices);
		if (devices.isEmpty()) {
			LOGGER.warn("No free devices found for category: {} with Thunder status: {}", category, isThunderEnabled);
			return Collections.emptyList();
		}
		return devices.stream().map(device -> {
			DeviceResponseDTO dto = new DeviceResponseDTO();
			dto.setDeviceName(device.getName());
			dto.setDeviceIp(device.getIp());
			dto.setMacId(device.getMacId());
			dto.setId(device.getId());
			return dto;
		}).collect(Collectors.toList());
	}

	/**
	 * This method is used to get the free devices from the list of devices.
	 * 
	 * @param devices List of devices
	 * @return List of free devices
	 */
	private List<Device> getFreeDevices(List<Device> devices) {
		List<Device> freeDevices = new ArrayList<>();
		for (Device device : devices) {
			if (device.getDeviceStatus().equals(DeviceStatus.FREE)) {
				freeDevices.add(device);
			}
		}
		if (freeDevices.isEmpty()) {
			LOGGER.warn("No free devices found");
		}
		return freeDevices;

	}

	/**
	 * This method is used to set the thunder enabled status for the device with the
	 * given id.
	 * 
	 * @param id The ID of the device to set the thunder enabled status for.
	 * 
	 */
	@Override
	public boolean toggleThunderEnabledstatus(String deviceIP) {
		LOGGER.info("Setting Thunder enabled status for device with ip: {}", deviceIP);
		Device device = deviceRepository.findByIp(deviceIP);
		if (device == null) {
			LOGGER.error("Device not found with IP: {}", deviceIP);
			throw new ResourceNotFoundException("Device ", deviceIP);
		}

		device.setThunderEnabled(!device.isThunderEnabled());
		deviceRepository.save(device);

		DeviceStatus deviceStatus = deviceStatusService.fetchDeviceStatus(device);
		device.setDeviceStatus(deviceStatus);
		deviceRepository.save(device);
		LOGGER.info("Completed setting Thunder enabled status for device with ip: {}", deviceIP);
		return device.isThunderEnabled();
	}

	/**
	 * This method is used to get the device details for the given device IP
	 * 
	 * @param deviceIp- IP of the device
	 * @return JSON object containing the device details
	 */
	@Override
	public DeviceStatusResponseDTO getDeviceStatus(String deviceIp) {
		LOGGER.info("Fetching device status for device with IP: {}", deviceIp);
		Device device = deviceRepository.findByIp(deviceIp);
		if (device == null) {
			LOGGER.error("Device not found with IP: {}", deviceIp);
			throw new ResourceNotFoundException("Device IP", deviceIp);
		}
		DeviceStatusResponseDTO deviceStatusResponseDTO = new DeviceStatusResponseDTO();
		deviceStatusResponseDTO.setDeviceName(device.getName());
		deviceStatusResponseDTO.setStatus(device.getDeviceStatus().toString());
		deviceStatusResponseDTO.setIp(device.getIp());
		deviceStatusResponseDTO.setMacAddress(device.getMacId());
		deviceStatusResponseDTO.setDeviceType(device.getDeviceType().getName());
		deviceStatusResponseDTO.setThunderEnabled(device.isThunderEnabled());
		LOGGER.info("Fetching device status for device : {} with status {}", device.getName(),
				device.getDeviceStatus());
		return deviceStatusResponseDTO;

	}

	/**
	 * Updates the status of all devices by category and retrieves the updated list
	 * of devices.
	 *
	 * This method performs the following steps: 1. Calls the
	 * `updateAllDeviceStatusByCategory` method from `DeviceStatusService` to update
	 * the status of all devices belonging to the specified category. 2. Retrieves
	 * and returns the updated list of devices in the specified category from the
	 * `DeviceRepositroy`.
	 *
	 * @param category The category of devices to update and retrieve. This
	 *                 parameter is of type {@link Category}.
	 * @return A list of updated {@link Device} objects belonging to the specified
	 *         category.
	 */
	public List<DeviceStatusResponseDTO> updateAndGetAllDeviceStatus(String category) {
		Category catgoryValue = commonService.validateCategory(category);

		// Update the status of all devices in the specified category
		deviceStatusService.updateAllDeviceStatusByCategory(catgoryValue);

		// Retrieve and return the updated list of devices in the specified category
		return this.getAllDeviceStatus(category);
	}

	/**
	 * This method is used to find the device details by its name using SSH.
	 * 
	 * @param deviceName The name of the device to find the details for.
	 * @return A String containing the device details.
	 */
	@Override
	public String findDeviceDetailsByName(String deviceName) {
		LOGGER.info("Fetching device details for device with name: {}", deviceName);
		Device device = deviceRepository.findByName(deviceName);
		if (device == null) {
			LOGGER.error("Device not found with name: {}", deviceName);
			throw new ResourceNotFoundException("Device Name", deviceName);
		}
		String deviceIp = device.getIp();
		String sshOptions = "-o StrictHostKeyChecking=no";
		String sshPass = "sshpass";
		String password = ""; // Your password here
		String user = "root";
		String command = "cat /version.txt";
		String[] executeScriptCommand = { sshPass, "-p", password, "ssh", sshOptions, user + "@" + deviceIp, command };
		LOGGER.info("Executing command: {}", String.join(" ", executeScriptCommand));
		String output = null;
		try {
			output = scriptExecutorService.executeScript(executeScriptCommand, 30);
			return output;
		} catch (Exception e) {
			LOGGER.error("Error executing SSH command for device '{}': {}", deviceName, e.getMessage());
			throw new TDKServiceException("Failed to retrieve device details: " + e.getMessage());
		}

	};
}
